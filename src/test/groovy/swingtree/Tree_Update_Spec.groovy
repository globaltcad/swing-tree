package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.From
import sprouts.Tuple
import sprouts.Var
import swingtree.threading.EventProcessor
import utility.SwingTreeTestConfigurator

import javax.swing.JTree
import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener

import static swingtree.TreeSpecFileSystem.expandedRows
import static swingtree.TreeSpecFileSystem.selectedRows
import static swingtree.TreeSpecFileSystem.visibleRows

@Title("Keeping a Bound Tree in Sync")
@Narrative("""

    A bound tree follows one property. When that property receives a new value, the tree has
    to change with it, and it has to do so without throwing away what the user has done:
    which branches they opened, and what they had selected.

    That is harder for a tree than for a list, because of how value objects work. A `JTree`
    keys its expanded branches and its selection on `TreePath`, whose equality bottoms out
    in the equality of the nodes it holds. Records compare by content, so renaming one leaf
    produces a new leaf, a new parent, a new grandparent and a new root: **every path in the
    whole tree goes stale at once**, and the tree collapses.

    SwingTree therefore identifies a node by the *path of ids* leading to it, which does not
    change when the node's content does. That is the same idea `HasId` brings to
    `addAll(Var<Tuple<M>>, ..)`, carried from one index up to a whole path.

    The second thing these scenarios are about is cost. "Potentially large" is a real
    requirement for a tree: two hundred thousand nodes of which forty are on screen. So an
    update walks only what is expanded, and stops dead at every subtree whose value is
    reference identical to the one before, which persistent, structurally shared data makes
    true for everything a change did not touch.

    The file system these scenarios build on is a sealed interface with two cases, which in
    Java 21 would read:

    ```java
    public sealed interface FsNode extends HasId<String> { String name(); }
    public record Dir( String id, String name, Tuple<FsNode> entries ) implements FsNode {}
    public record Doc( String id, String name, String body )           implements FsNode {}
    ```

""")
@Subject([UIForTree, TreeConf])
class Tree_Update_Spec extends Specification
{
    def setupSpec() {
        SwingTree.initializeUsing(SwingTreeTestConfigurator.get())
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
        // In this specification we are using the strict event processor,
        // which complains if UI operations are performed off the UI thread.
    }

    def cleanupSpec() {
        SwingTree.clear()
    }

    def 'Assigning a new value to the property changes the tree.'()
    {
        reportInfo """
            There is no `updateTreeOn(..)`, no event to fire and no model to notify. The tree
            listens to the property it was bound to, and a new value is all it takes.

            Note that the update is written as a pure function of the old value, which is how
            a change to an immutable structure is always expressed: `update(..)` is shorthand
            for reading the value, transforming it, and setting the result.
        """
        given : 'A small file system in a property, and a tree bound to it.'
            var fileSystem = Var.of(FsNode, new Dir("r", "root", Tuple.of(FsNode, new Doc("a", "notes.txt"))))
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }).text({ Dir d -> d.name() }) })
                        .nodesOf(Doc, { it.text({ Doc d -> d.name() }) })
                    })
                    .get(JTree)

        expect : 'It shows what the property holds.'
            visibleRows(tree) == ["root", "    notes.txt"]

        when : 'We put a second document into the root.'
            UI.runNow({ fileSystem.update({ Dir d -> d.withEntries(d.entries().add(new Doc("b", "todo.md"))) }) })

        then : 'The tree shows it, without anything else being said.'
            visibleRows(tree) == ["root", "    notes.txt", "    todo.md"]
    }

    def 'Expansion survives an edit anywhere in the tree.'()
    {
        reportInfo """
            This is the scenario the whole identity design exists for.

            The user opens two folders deep down. Then something they are looking at is
            renamed, which in an immutable world means a brand new root value with brand new
            nodes all the way down the edited path. If the tree identified its nodes by their
            content, everything the user had opened would snap shut.

            It does not, because a node is identified by the ids leading to it, and an id is
            exactly the thing an edit leaves alone.
        """
        given : 'A file system three levels deep, bound to a tree.'
            var fileSystem = Var.of(FsNode, new Dir("r", "root", Tuple.of(FsNode,
                                        new Dir("a", "src", Tuple.of(FsNode,
                                            new Dir("a1", "main", Tuple.of(FsNode, new Doc("a11", "App.java"))))),
                                        new Dir("b", "docs", Tuple.of(FsNode, new Doc("b1", "guide.md")))
                                    )))
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }, { Dir d, Tuple<FsNode> e -> d.withEntries(e) })
                                          .text({ Dir d -> d.name() }) })
                        .nodesOf(Doc, { it.text({ Doc d -> d.name() }) })
                    })
                    .get(JTree)

        when : 'The user opens the two folders leading down to the deepest document.'
            UI.runNow({
                tree.expandRow(1)   // src
                tree.expandRow(2)   // main, which only has a row once src is open
            })

        then : 'Both are open, and the deep document is on screen.'
            expandedRows(tree) == ["root", "src", "main"]
            visibleRows(tree) == [
                "root",
                "    src",
                "        main",
                "            App.java",
                "    docs",
            ]

        when : 'The deepest document is renamed, which rebuilds every node above it.'
            UI.runNow({ fileSystem.update({ Dir root ->
                var src  = (Dir) root.entries().get(0)
                var main = (Dir) src.entries().get(0)
                var app  = (Doc) main.entries().get(0)
                root.withEntries(root.entries().setAt(0,
                    src.withEntries(src.entries().setAt(0,
                        main.withEntries(main.entries().setAt(0, app.withName("Application.java")))))))
            }) })

        then : 'The rename is on screen...'
            visibleRows(tree) == [
                "root",
                "    src",
                "        main",
                "            Application.java",
                "    docs",
            ]
        and : '...and both folders the user opened are still open.'
            expandedRows(tree) == ["root", "src", "main"]
    }

    def 'A content edit is announced as a node change, never as a structure change.'()
    {
        reportInfo """
            The distinction matters to a `JTree`. A node change repaints the affected rows; a
            structure change makes it throw away everything it knows about the branch and ask
            again.

            So an edit which leaves the shape of the tree alone must not be announced as a
            structural one. Here we listen to the model directly and watch it say the right
            thing: three node changes, one for each node on the path the edit rebuilt, and no
            structure change at all.
        """
        given : 'A tree with one folder opened by the user.'
            var fileSystem = Var.of(FsNode, new Dir("r", "root", Tuple.of(FsNode,
                                        new Dir("a", "src", Tuple.of(FsNode, new Doc("a1", "App.java"))),
                                        new Dir("b", "docs", Tuple.of(FsNode))
                                    )))
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }).text({ Dir d -> d.name() }) })
                        .nodesOf(Doc, { it.text({ Doc d -> d.name() }) })
                    })
                    .get(JTree)
            UI.runNow({ tree.expandRow(1) })
        and : 'A listener recording everything the tree model announces.'
            var announced = []
            tree.getModel().addTreeModelListener(new TreeModelListener() {
                @Override void treeNodesChanged    ( TreeModelEvent e ) { announced << "changed"   }
                @Override void treeNodesInserted   ( TreeModelEvent e ) { announced << "inserted"  }
                @Override void treeNodesRemoved    ( TreeModelEvent e ) { announced << "removed"   }
                @Override void treeStructureChanged( TreeModelEvent e ) { announced << "structure" }
            })

        when : 'A document two levels down is renamed.'
            UI.runNow({ fileSystem.update({ Dir root ->
                var src = (Dir) root.entries().get(0)
                root.withEntries(root.entries().setAt(0,
                    src.withEntries(src.entries().setAt(0, ((Doc) src.entries().get(0)).withName("Main.java")))))
            }) })

        then : 'Exactly the three nodes the edit rebuilt were announced as changed.'
            announced == ["changed", "changed", "changed"]
        and : 'The tree was never told to rebuild anything.'
            !announced.contains("structure")
    }

    def 'A subtree the change did not touch is not even looked at.'()
    {
        reportInfo """
            This is where immutability pays for itself. A persistent collection shares
            everything a change did not touch, so after an edit inside one folder, the *other*
            folder is not merely equal to what it was, it is the very same object.

            The update walk compares by reference and stops there. Below, the children getter
            counts how often each folder is asked for its entries, and the untouched one is
            never asked at all, even though it is expanded and on screen. That is what makes
            the cost of an edit independent of the size of the tree.
        """
        given : 'A counter recording which folders get asked for their children.'
            var reads = [:].withDefault { 0 }
        and : 'A tree in which two sibling folders are both open.'
            var fileSystem = Var.of(FsNode, new Dir("r", "root", Tuple.of(FsNode,
                                        new Dir("a", "edited", Tuple.of(FsNode,    new Doc("a1", "one.txt"))),
                                        new Dir("b", "untouched", Tuple.of(FsNode, new Doc("b1", "two.txt")))
                                    )))
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> reads[d.name()] = reads[d.name()] + 1; d.entries() },
                                                    { Dir d, Tuple<FsNode> e -> d.withEntries(e) })
                                          .text({ Dir d -> d.name() }) })
                        .nodesOf(Doc, { it.text({ Doc d -> d.name() }) })
                    })
                    .withInitialExpansionDepth(3)
                    .get(JTree)

        expect : 'Both folders are on screen and open.'
            expandedRows(tree) == ["root", "edited", "untouched"]
            visibleRows(tree) == [
                "root",
                "    edited",
                "        one.txt",
                "    untouched",
                "        two.txt",
            ]

        when : 'We forget every read so far and then rename a document in the first folder.'
            reads.clear()
            UI.runNow({ fileSystem.update({ Dir root ->
                var edited = (Dir) root.entries().get(0)
                root.withEntries(root.entries().setAt(0,
                    edited.withEntries(edited.entries().setAt(0, ((Doc) edited.entries().get(0)).withName("ONE.txt")))))
            }) })

        then : 'The rename is on screen.'
            visibleRows(tree).contains("        ONE.txt")
        and : 'The edited folder was walked into...'
            reads["edited"] > 0
        and : '...and the untouched one was never asked for anything, because it is the same object as before.'
            reads["untouched"] == 0
    }

    def 'A change below a collapsed branch costs nothing to apply.'()
    {
        reportInfo """
            The second half of the cost story. Nothing below a collapsed branch is on screen,
            so nothing below it needs an event, and the walk stops at the branch.

            This is what keeps a tree over a genuinely large structure responsive: the work an
            update does is bounded by what the user has actually opened, not by what exists.
        """
        given : 'A counter, and a tree whose one folder the user has not opened.'
            var reads = [:].withDefault { 0 }
            var fileSystem = Var.of(FsNode, new Dir("r", "root", Tuple.of(FsNode,
                                        new Dir("a", "closed", Tuple.of(FsNode, new Doc("a1", "deep.txt")))
                                    )))
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> reads[d.name()] = reads[d.name()] + 1; d.entries() })
                                          .text({ Dir d -> d.name() }) })
                        .nodesOf(Doc, { it.text({ Doc d -> d.name() }) })
                    })
                    .get(JTree)

        expect : 'Only the root and the closed folder are on screen.'
            visibleRows(tree) == ["root", "    closed"]

        when : 'We forget every read and then change something inside the closed folder.'
            reads.clear()
            UI.runNow({ fileSystem.update({ Dir root ->
                var closed = (Dir) root.entries().get(0)
                root.withEntries(root.entries().setAt(0,
                    closed.withEntries(closed.entries().setAt(0, ((Doc) closed.entries().get(0)).withName("changed.txt")))))
            }) })

        then : 'The closed folder was never asked for its children, because they are not on screen.'
            reads["closed"] == 0

        when : 'The user opens it.'
            UI.runNow({ tree.expandRow(1) })

        then : 'The change was there all along, and shows the moment it becomes visible.'
            visibleRows(tree) == ["root", "    closed", "        changed.txt"]
    }

    def 'Adding a node makes it appear, and leaves every open branch open.'()
    {
        reportInfo """
            An insertion changes the shape of a branch, which is a different kind of change
            from an edit: the tree cannot be told "this row now reads differently", it has to
            be told the branch itself is different.

            SwingTree answers that by announcing a structure change and then putting the
            user's expanded branches and selection back. The user notices nothing, because
            the paths being restored are made of ids, which the insertion did not disturb.
        """
        given : 'A tree with two folders.'
            var fileSystem = Var.of(FsNode, new Dir("r", "root", Tuple.of(FsNode,
                                        new Dir("a", "src", Tuple.of(FsNode,  new Doc("a1", "App.java"))),
                                        new Dir("b", "docs", Tuple.of(FsNode, new Doc("b1", "guide.md")))
                                    )))
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }, { Dir d, Tuple<FsNode> e -> d.withEntries(e) })
                                          .text({ Dir d -> d.name() }) })
                        .nodesOf(Doc, { it.text({ Doc d -> d.name() }) })
                    })
                    .get(JTree)
        and : 'Which the user opens both of.'
            UI.runNow({
                tree.expandRow(1)   // src
                tree.expandRow(3)   // docs, one row further down now that src is open
            })

        when : 'A new document is added to the first folder.'
            UI.runNow({ fileSystem.update({ Dir root ->
                var src = (Dir) root.entries().get(0)
                root.withEntries(root.entries().setAt(0, src.withEntries(src.entries().add(new Doc("a2", "Util.java")))))
            }) })

        then : 'It appears where it belongs.'
            visibleRows(tree) == [
                "root",
                "    src",
                "        App.java",
                "        Util.java",
                "    docs",
                "        guide.md",
            ]
        and : 'And both folders the user opened are still open.'
            expandedRows(tree) == ["root", "src", "docs"]
    }

    def 'Removing a node makes it disappear, and its siblings keep their state.'()
    {
        reportInfo """
            The mirror image of an insertion, and the harder direction: removing an entry
            shifts every entry after it up by one. A tree identifying its rows by position
            would now have the wrong branch expanded.

            Identifying them by id means the surviving folder keeps its own state, wherever
            the removal left it sitting.
        """
        given : 'Three folders, of which the user has opened only the last.'
            var fileSystem = Var.of(FsNode, new Dir("r", "root", Tuple.of(FsNode,
                                        new Dir("a", "first", Tuple.of(FsNode,  new Doc("a1", "a.txt"))),
                                        new Dir("b", "second", Tuple.of(FsNode, new Doc("b1", "b.txt"))),
                                        new Dir("c", "third", Tuple.of(FsNode,  new Doc("c1", "c.txt")))
                                    )))
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }, { Dir d, Tuple<FsNode> e -> d.withEntries(e) })
                                          .text({ Dir d -> d.name() }) })
                        .nodesOf(Doc, { it.text({ Doc d -> d.name() }) })
                    })
                    .get(JTree)
            UI.runNow({ tree.expandRow(3) })

        expect : 'Only the third folder shows its contents.'
            visibleRows(tree) == [
                "root",
                "    first",
                "    second",
                "    third",
                "        c.txt",
            ]
            expandedRows(tree) == ["root", "third"]

        when : 'The first folder is removed, shifting the other two up.'
            UI.runNow({ fileSystem.update({ Dir root -> root.withEntries(root.entries().removeAt(0)) }) })

        then : 'It is gone...'
            visibleRows(tree) == [
                "root",
                "    second",
                "    third",
                "        c.txt",
            ]
        and : '...and the folder the user had opened is still the one that is open, despite having moved.'
            expandedRows(tree) == ["root", "third"]
    }

    def 'A node changing to a different variant of the sum type is handled.'()
    {
        reportInfo """
            A sum type makes it possible for a node to become something else entirely: a
            document turning into a directory, or a `Pending` branch turning into a `Loaded`
            one once its contents arrive.

            That is not a content change, it is a different node in the same place, and it may
            well change whether the node is a branch at all. SwingTree answers it the same way
            it answers an insertion, so the rest of the tree keeps its state while the changed
            node picks up its new type's rules.
        """
        given : 'A tree in which the first entry is currently a document.'
            var fileSystem = Var.of(FsNode, new Dir("r", "root", Tuple.of(FsNode,
                                        new Doc("x", "mystery"),
                                        new Dir("b", "docs", Tuple.of(FsNode, new Doc("b1", "guide.md")))
                                    )))
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }, { Dir d, Tuple<FsNode> e -> d.withEntries(e) })
                                          .text({ Dir d -> d.name() }) })
                        .nodesOf(Doc, { it.text({ Doc d -> d.name() }) })
                    })
                    .get(JTree)
            UI.runNow({ tree.expandRow(2) })

        expect : 'The document is a leaf, and the folder below it is open.'
            expandedRows(tree) == ["root", "docs"]
            UI.runAndGet({ tree.getModel().isLeaf(tree.getPathForRow(1).getLastPathComponent()) })

        when : 'That same entry, keeping its id, becomes a directory.'
            UI.runNow({ fileSystem.update({ Dir root ->
                root.withEntries(root.entries().setAt(0, new Dir("x", "mystery", Tuple.of(FsNode, new Doc("x1", "revealed.txt")))))
            }) })

        then : 'It is a branch now, and it can be opened.'
            !UI.runAndGet({ tree.getModel().isLeaf(tree.getPathForRow(1).getLastPathComponent()) })
        and : 'The folder the user had opened elsewhere is untouched by any of it.'
            expandedRows(tree) == ["root", "docs"]

        when : 'The user opens the node that changed its nature.'
            UI.runNow({ tree.expandRow(1) })

        then : 'Its new contents are there.'
            visibleRows(tree) == [
                "root",
                "    mystery",
                "        revealed.txt",
                "    docs",
                "        guide.md",
            ]
    }

    def 'Replacing the whole root rebuilds the tree.'()
    {
        reportInfo """
            Every scenario so far changed something *inside* the bound value. Assigning an
            entirely different root is a different matter: no path from the old tree means
            anything in the new one, so there is nothing to preserve and the tree starts
            over.

            SwingTree tells the two cases apart by the identity of the root, which is why
            this is not something you have to declare.
        """
        given : 'A tree over one file system, with a folder opened.'
            var fileSystem = Var.of(FsNode, new Dir("old", "old-root", Tuple.of(FsNode, new Dir("a", "src", Tuple.of(FsNode, new Doc("a1", "App.java"))))))
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }).text({ Dir d -> d.name() }) })
                        .nodesOf(Doc, { it.text({ Doc d -> d.name() }) })
                    })
                    .get(JTree)
            UI.runNow({ tree.expandRow(1) })

        expect :
            visibleRows(tree) == ["old-root", "    src", "        App.java"]

        when : 'A completely different file system is assigned.'
            UI.runNow({ fileSystem.set(new Dir("new", "new-root", Tuple.of(FsNode, new Doc("z", "readme.txt")))) })

        then : 'The tree shows the new one, from the top.'
            visibleRows(tree) == ["new-root", "    readme.txt"]
    }

    def 'Setting the property to a value that is not actually different does nothing.'()
    {
        reportInfo """
            Sprouts properties do not fire when the value they receive equals the one they
            hold, so an update that computes the same structure back is not an update at all
            and the tree is never disturbed.

            This is worth knowing because it is what makes it safe to recompute state
            liberally: the view does not pay for a change that did not happen.
        """
        given : 'A tree, and a listener counting everything its model announces.'
            var fileSystem = Var.of(FsNode, new Dir("r", "root", Tuple.of(FsNode, new Doc("a", "notes.txt"))))
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }, { Dir d, Tuple<FsNode> e -> d.withEntries(e) })
                                          .text({ Dir d -> d.name() }) })
                        .nodesOf(Doc, { it.text({ Doc d -> d.name() }) })
                    })
                    .get(JTree)
            var announcements = 0
            tree.getModel().addTreeModelListener(new TreeModelListener() {
                @Override void treeNodesChanged    ( TreeModelEvent e ) { announcements++ }
                @Override void treeNodesInserted   ( TreeModelEvent e ) { announcements++ }
                @Override void treeNodesRemoved    ( TreeModelEvent e ) { announcements++ }
                @Override void treeStructureChanged( TreeModelEvent e ) { announcements++ }
            })

        when : 'We rename a document to the name it already has.'
            UI.runNow({ fileSystem.update({ Dir root ->
                root.withEntries(root.entries().setAt(0, ((Doc) root.entries().get(0)).withName("notes.txt")))
            }) })

        then : 'Nothing was announced, because nothing changed.'
            announcements == 0
            visibleRows(tree) == ["root", "    notes.txt"]
    }

    def 'A structural change leaves a bound selection exactly where it was.'()
    {
        reportInfo """
            Restoring the user's selection after a structural change is only half the job. The
            other half is not *lying* about it on the way there.

            A structure change makes the `JTree` drop every selected path before the restore
            can put it back, and it announces that as an ordinary selection event. A binding
            which passed that on would tell the application "nothing is selected any more" on
            every single insertion, and a detail pane listening to it would blink empty and
            fill again each time somebody added a file in a folder nobody was looking at.

            So the write back is muted for the length of the rebuild, and the settled selection
            is compared with the one the rebuild started from: it is announced once at the end
            if it really moved, and not at all if it did not. What reaches the property is what
            is true after the tree has finished moving, and only when it is news.
        """
        given : 'A file system with a document selected somewhere inside it.'
            var fileSystem = Var.of(FsNode, new Dir("r", "root", Tuple.of(FsNode,
                                        new Dir("a", "assets", Tuple.of(FsNode, new Doc("a1", "logo.svg"))),
                                        new Doc("b", "README.md")
                                    )))
            var selection = Var.of(Tuple.of(String))
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }, { Dir d, Tuple<FsNode> e -> d.withEntries(e) })
                                          .text({ Dir d -> d.name() }) })
                        .nodesOf(Doc, { it.text({ Doc d -> d.name() }) })
                    })
                    .withSelection(selection)
                    .withInitialExpansionDepth(3)
                    .get(JTree)
        and : 'A record of every value the selection property ever receives.'
            var seen = []
            selection.onChange(From.ALL, { seen.add(it.currentValue().orElseThrow().toList()) })

        when : 'The logo is selected.'
            UI.runNow({ selection.set(Tuple.of(String, ["r", "a", "a1"])) })
        then : 'The property and the tree agree, and that was one change.'
            selectedRows(tree) == ["logo.svg"]
            seen == [["r", "a", "a1"]]

        when : 'A new file appears somewhere else entirely, which is a structural change.'
            seen.clear()
            UI.runNow({ fileSystem.update({ Dir root -> root.withEntries(root.entries().add(new Doc("c", "CHANGELOG.md"))) }) })

        then : 'It is on screen...'
            visibleRows(tree).contains("    CHANGELOG.md")
        and : '...the logo is still selected...'
            selectedRows(tree) == ["logo.svg"]
        and : '...and the property was never told anything else. No empty selection in between.'
            seen.every { it == ["r", "a", "a1"] }
            !seen.contains([])
            selection.get().toList() == ["r", "a", "a1"]
    }

    def 'Deleting the selected node empties the selection, and says so once.'()
    {
        reportInfo """
            The counterpart to the scenario above: muting the write back during a rebuild must
            not swallow a change that really happened. When the selected node is the one that
            was removed, there is nothing to restore, and the property has to hear about it.
        """
        given : 'A file system with a document selected, which we are about to delete.'
            var fileSystem = Var.of(FsNode, new Dir("r", "root", Tuple.of(FsNode,
                                        new Dir("a", "assets", Tuple.of(FsNode, new Doc("a1", "logo.svg"), new Doc("a2", "icon.svg")))
                                    )))
            var selection = Var.of(Tuple.of(String, "r", "a", "a1"))
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }, { Dir d, Tuple<FsNode> e -> d.withEntries(e) })
                                          .text({ Dir d -> d.name() }) })
                        .nodesOf(Doc, { it.text({ Doc d -> d.name() }) })
                    })
                    .withSelection(selection)
                    .withInitialExpansionDepth(3)
                    .get(JTree)
        expect : 'The logo starts out selected.'
            selectedRows(tree) == ["logo.svg"]

        when : 'The selected document is removed from its folder.'
            UI.runNow({ fileSystem.update({ Dir root ->
                var assets = (Dir) root.entries().get(0)
                root.withEntries(root.entries().setAt(0, assets.withEntries(assets.entries().removeFirst(1))))
            }) })

        then : 'It is gone from the screen, and the property says nothing is selected.'
            visibleRows(tree) == ["root", "    assets", "        icon.svg"]
            selectedRows(tree) == []
            selection.get().isEmpty()
    }

    def 'A property holding nothing is an empty tree, not a tree holding nothing.'()
    {
        reportInfo """
            A property may legitimately hold no value at all: a structure which has not
            finished loading, a document nobody has opened yet. A `TreeModel` says "there is
            no tree" by having no root, and a `JTree` draws no rows for one, which is what
            an empty property should look like.

            What it must *not* look like is a single row reading `null`, which is what
            wrapping the absent value in a node would produce.
        """
        given : 'A property which holds no file system yet.'
            var fileSystem = Var.ofNullable(FsNode, null)
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }).text({ Dir d -> d.name() }) })
                        .nodesOf(Doc, { it.text({ Doc d -> d.name() }) })
                    })
                    .get(JTree)

        expect : 'The tree has no root and shows nothing.'
            UI.runAndGet({ tree.getModel().getRoot() }) == null
            visibleRows(tree) == []

        when : 'The file system arrives.'
            UI.runNow({ fileSystem.set(new Dir("r", "root", Tuple.of(FsNode, new Doc("a", "notes.txt")))) })
        then : 'It is on screen, root opened, exactly as if it had been there all along.'
            visibleRows(tree) == ["root", "    notes.txt"]

        when : 'And it is taken away again.'
            UI.runNow({ fileSystem.set(null) })
        then : 'The tree goes empty rather than showing a node standing in for nothing.'
            UI.runAndGet({ tree.getModel().getRoot() }) == null
            visibleRows(tree) == []
    }
}
