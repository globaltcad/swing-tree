package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.Tuple
import sprouts.Val
import sprouts.Var
import swingtree.threading.EventProcessor
import utility.LogSpy
import utility.SwingTreeTestConfigurator

import javax.swing.JTree
import javax.swing.tree.TreePath

import static swingtree.TreeSpecFileSystem.selectedRows
import static swingtree.TreeSpecFileSystem.visibleRows

@Title("Selecting and Editing in a Bound Tree")
@Narrative("""

    The two things a user does to a tree, beyond opening and closing branches, are selecting
    a node and changing one. Both are handled the same way everything else in SwingTree is:
    by binding a property, not by registering a listener and copying values around by hand.

    **Selection** is a property holding a **path of ids** — the ids leading from the root
    down to the selected node — kept in sync in both directions. A selection is a *position*
    in the tree, and a node value cannot name a position: the same value may sit in several
    places at once, and a tree only asks its ids to be unique among siblings. A path has
    nothing to be ambiguous about, needs no search to resolve, and carries no node data which
    could contradict the tree. The empty path means nothing is selected.

    **Editing** is where the lens at the heart of this API becomes visible. Renaming a node
    nine levels down cannot mutate anything: it has to produce a brand new root value with
    every ancestor along the path rebuilt and everything off the path shared. That is exactly
    what a `children(getter, wither)` rule declares, and the chain of those rules from the
    root down to the edited node is what carries the edit home.

    The rule to remember throughout: **a getter alone is read only, a getter together with a
    wither is a lens and therefore two way.** These scenarios are mostly about what follows
    from that one sentence.

    The file system they build on is a sealed interface with two cases, which in Java 21
    would read:

    ```java
    public sealed interface FsNode extends HasId<String> { String name(); }
    public record Dir( String id, String name, Tuple<FsNode> entries ) implements FsNode {}
    public record Doc( String id, String name, String body )           implements FsNode {}
    ```

""")
@Subject([UIForTree, TreeNodeConf, TreeSelectionDelegate])
class Tree_Selection_And_Editing_Spec extends Specification
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

    def 'The selected position is a property holding a path of ids.'()
    {
        reportInfo """
            One binding replaces the whole dance a plain `JTree` demands: a selection
            listener writing into the model, and a second piece of code walking the rows to
            put a selection back when the model changes elsewhere.

            Note what the property holds. **A selection is a position, and only a path names
            a position** — the ids leading from the root down to the selected node, the
            root's own id first. A node value could not do it: the same value may sit in
            several places at once, which the next scenario makes concrete.
        """
        given : 'A file system, and a property for wherever the selection is.'
            var fileSystem = Var.of(FsNode, new Dir("r", "root", Tuple.of(FsNode,
                                        new Doc("a", "notes.txt"),
                                        new Doc("b", "todo.md")
                                    )))
            var selected = Var.of(Tuple.of(String))
        and : 'A tree binding the two together.'
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }).text({ Dir d -> d.name() }) })
                        .nodesOf(Doc, { it.text({ Doc d -> d.name() }) })
                    })
                    .withSelection(selected)
                    .get(JTree)

        when : 'The user clicks the second document.'
            UI.runNow({ tree.setSelectionRow(2) })

        then : 'The property holds the path leading to it.'
            selected.get().toList() == ["r", "b"]

        when : 'The application selects the other document by naming its path.'
            UI.runNow({ selected.set(Tuple.of(String, ["r", "a"])) })

        then : 'The tree moved its selection there.'
            selectedRows(tree) == ["notes.txt"]
    }

    def 'The same node value in two places is two different selections.'()
    {
        reportInfo """
            This is the scenario a node-valued selection could not express, and the reason
            the property holds a path.

            A tree only requires its ids to be unique **among siblings**, so two folders may
            each hold a `notes.txt` with the very same id. Those are two different rows, and
            the two paths leading to them say so exactly. Nothing is searched for, so nothing
            can resolve to the wrong one.
        """
        given : 'A structure in which the same id is deliberately reused in two folders.'
            var fileSystem = Var.of(FsNode, new Dir("r", "root", Tuple.of(FsNode,
                                        new Dir("first",  "first", Tuple.of(FsNode,  new Doc("notes", "notes.txt"))),
                                        new Dir("second", "second", Tuple.of(FsNode, new Doc("notes", "notes.txt")))
                                    )))
            var selected = Var.of(Tuple.of(String))
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }).text({ Dir d -> d.name() }) })
                        .nodesOf(Doc, { it.text({ Doc d -> d.name() }) })
                    })
                    .withSelection(selected)
                    .withInitialExpansionDepth(3)
                    .get(JTree)

        expect : 'Both documents are on screen, and they look identical.'
            visibleRows(tree) == [
                "root",
                "    first",
                "        notes.txt",
                "    second",
                "        notes.txt",
            ]

        when : 'The application asks for the one inside the SECOND folder.'
            UI.runNow({ selected.set(Tuple.of(String, ["r", "second", "notes"])) })

        then : 'That is the row which got selected, and not its identical twin.'
            UI.runAndGet({ tree.getSelectionRows() as List }) == [4]

        when : 'The user clicks the one inside the FIRST folder instead.'
            UI.runNow({ tree.setSelectionRow(2) })

        then : 'The selection stays where they clicked...'
            UI.runAndGet({ tree.getSelectionRows() as List }) == [2]
        and : '...and the property says which of the two it is.'
            selected.get().toList() == ["r", "first", "notes"]
    }

    def 'A path outlives an edit to the node it names.'()
    {
        reportInfo """
            A path is made of ids, and an id is exactly the thing an edit leaves alone. So a
            selection made before a rename is still the same selection afterwards, without
            the property having to be rewritten and without the tree having to guess.
        """
        given : 'A tree with a bound selection.'
            var fileSystem = Var.of(FsNode, new Dir("r", "root", Tuple.of(FsNode,
                                        new Dir("a", "src", Tuple.of(FsNode, new Doc("a1", "App.java"))),
                                        new Doc("b", "README.md")
                                    )))
            var selected = Var.of(Tuple.of(String))
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }, { Dir d, Tuple<FsNode> e -> d.withEntries(e) })
                                          .text({ Dir d -> d.name() }) })
                        .nodesOf(Doc, { it.text({ Doc d -> d.name() }) })
                    })
                    .withSelection(selected)
                    .get(JTree)

        when : 'We select the folder by its path.'
            UI.runNow({ selected.set(Tuple.of(String, ["r", "a"])) })

        then : 'It is selected.'
            selectedRows(tree) == ["src"]

        when : 'That very folder is then renamed, which replaces it with a different value.'
            UI.runNow({ fileSystem.update({ Dir root ->
                root.withEntries(root.entries().setAt(0, ((Dir) root.entries().get(0)).withName("sources")))
            }) })

        then : 'It is still the selected row, because renaming it did not move it.'
            visibleRows(tree) == ["root", "    sources", "    README.md"]
            selectedRows(tree) == ["sources"]
        and : 'And the path in the property never had to change.'
            selected.get().toList() == ["r", "a"]
    }

    def 'The empty path means nothing is selected.'()
    {
        reportInfo """
            "Nothing is selected" is an ordinary value of the property rather than a null to
            handle: a path of no ids leads nowhere, which is precisely what an empty
            selection is. This is also why the root being selected is representable at all —
            that is a path of exactly one id.
        """
        given : 'A tree with something selected in it.'
            var fileSystem = Var.of(FsNode, new Dir("r", "root", Tuple.of(FsNode, new Doc("a", "notes.txt"))))
            var selected = Var.of(Tuple.of(String))
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }).text({ Dir d -> d.name() }) })
                        .nodesOf(Doc, { it.text({ Doc d -> d.name() }) })
                    })
                    .withSelection(selected)
                    .get(JTree)
            UI.runNow({ tree.setSelectionRow(1) })

        expect : 'The document is selected.'
            selectedRows(tree) == ["notes.txt"]

        when : 'The application empties the path.'
            UI.runNow({ selected.set(Tuple.of(String)) })

        then : 'The tree has no selection any more.'
            selectedRows(tree) == []

        when : 'The application names the root itself, which is a path of one id.'
            UI.runNow({ selected.set(Tuple.of(String, ["r"])) })

        then : 'The root is selected, which an empty path could never have expressed.'
            selectedRows(tree) == ["root"]
    }

    def 'Several selected positions bind to a property holding a tuple of paths.'()
    {
        reportInfo """
            Which overload you call is what says whether the user may select one node or
            several: there is no separate selection mode to set. Binding a tuple of paths
            puts the tree into multiple selection mode and keeps the whole selection in the
            property.
        """
        given : 'A tree binding every selected position to one property.'
            var fileSystem = Var.of(FsNode, new Dir("r", "root", Tuple.of(FsNode,
                                        new Doc("a", "one.txt"),
                                        new Doc("b", "two.txt"),
                                        new Doc("c", "three.txt")
                                    )))
            var selected = Var.of(Tuple.of(Tuple.classTyped(String)))
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }).text({ Dir d -> d.name() }) })
                        .nodesOf(Doc, { it.text({ Doc d -> d.name() }) })
                    })
                    .withSelectionPaths(selected)
                    .get(JTree)

        when : 'The user picks the first and the last document.'
            UI.runNow({ tree.setSelectionRows([1, 3] as int[]) })

        then : 'Both paths are in the property, in the order the tree reports them.'
            selected.get().toList().collect({ Tuple<String> p -> p.toList() }) == [["r", "a"], ["r", "c"]]

        when : 'The application replaces the selection with the middle document alone.'
            UI.runNow({ selected.set(Tuple.of(Tuple.classTyped(String), [Tuple.of(String, ["r", "b"])])) })

        then : 'That is what is selected in the tree.'
            selectedRows(tree) == ["two.txt"]
    }

    def 'A path is resolved back to its node through the configuration.'()
    {
        reportInfo """
            A path says *where*, not *what*. When a view needs the node itself — for a detail
            pane, or a breadcrumb trail — it asks the tree's own configuration, which is the
            thing that knows where children live.

            `nodesAlong(..)` walks the whole chain and `nodeAt(..)` takes its last step. Both
            answer emptily for a path which no longer leads anywhere, so a stale selection
            never produces a half-resolved trail.
        """
        given : 'The configuration a tree is built from, kept so it can answer questions later.'
            var fileSystem = Var.of(FsNode, new Dir("r", "root", Tuple.of(FsNode,
                                        new Dir("a", "src", Tuple.of(FsNode, new Doc("a1", "App.java")))
                                    )))
            var conf = TreeConf.of(FsNode, String)
                            .nodesOf(Dir, { it.children({ Dir d -> d.entries() }).text({ Dir d -> d.name() }) })
                            .nodesOf(Doc, { it.text({ Doc d -> d.name() }) })

        when : 'We ask what sits at a path.'
            var node = conf.nodeAt(fileSystem.get(), Tuple.of(String, ["r", "a", "a1"]))
        then : 'It is the node that path names.'
            node.get().name() == "App.java"

        when : 'We ask for the whole chain leading to it.'
            var trail = conf.nodesAlong(fileSystem.get(), Tuple.of(String, ["r", "a", "a1"]))
        then : 'It reads as a breadcrumb.'
            trail.toList().collect({ FsNode n -> n.name() }) == ["root", "src", "App.java"]

        when : 'We ask for a path which names nothing.'
            var missing = conf.nodesAlong(fileSystem.get(), Tuple.of(String, ["r", "a", "gone"]))
        then : 'Nothing comes back, rather than a partial trail.'
            missing.isEmpty()
            !conf.nodeAt(fileSystem.get(), Tuple.of(String, ["r", "a", "gone"])).isPresent()
    }

    def 'A selection action speaks in your own node values, and in paths.'()
    {
        reportInfo """
            `onSelection(..)` is for the cases a bound property does not cover, such as
            opening a document when it is picked. The delegate it receives answers both
            questions a selection raises: *what* is selected, in your own node types, and
            *which position* is selected, as a path of ids.

            Reaching into the tree's own path objects would not help you here anyway: a bound
            tree keeps internal handles in them, so `getLastSelectedPathComponent()` does not
            return one of your nodes. The delegate exists so you never need it.
        """
        given : 'A tree recording what each selection change told it.'
            var fileSystem = Var.of(FsNode, new Dir("r", "root", Tuple.of(FsNode,
                                        new Dir("a", "src", Tuple.of(FsNode, new Doc("a1", "App.java")))
                                    )))
            var seen = []
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }).text({ Dir d -> d.name() }) })
                        .nodesOf(Doc, { it.text({ Doc d -> d.name() }) })
                    })
                    .onSelection({ it ->
                        seen << [ lead : it.lead().map({ FsNode n -> n.name() }).orElse("nothing"),
                                  names: it.pathToLead().toList().collect({ FsNode n -> n.name() }),
                                  path : it.leadPath().toList(),
                                  count: it.selection().size() ]
                    })
                    .get(JTree)
            UI.runNow({ tree.expandRow(1) })

        when : 'The user selects the document two levels down.'
            UI.runNow({ tree.setSelectionRow(2) })

        then : 'The action was told what was selected...'
            seen.size() == 1
            seen[0].lead  == "App.java"
            seen[0].names == ["root", "src", "App.java"]
            seen[0].count == 1
        and : '...and exactly which position it was.'
            seen[0].path == ["r", "a", "a1"]
    }

    def 'A selection action can go straight on to edit what was selected.'()
    {
        reportInfo """
            The delegate also hands over a **lens onto the selected node**: a `Var` focused on
            that one node, reaching all the way up into the single property the tree is bound
            to. Writing through it produces a new root value, exactly as an edit made in the
            tree itself would.

            This is the same mechanism the whole API rests on, surfaced where an action can
            reach it. It is empty when nothing is selected, or when the tree was bound read
            only, because there would then be nothing to write into.
        """
        given : 'A tree whose selection action renames whatever document gets picked.'
            var fileSystem = Var.of(FsNode, new Dir("r", "root", Tuple.of(FsNode,
                                        new Dir("a", "src", Tuple.of(FsNode, new Doc("a1", "App.java")))
                                    )))
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }, { Dir d, Tuple<FsNode> e -> d.withEntries(e) })
                                          .text({ Dir d -> d.name() }) })
                        .nodesOf(Doc, { it.text({ Doc d -> d.name() }) })
                    })
                    .onSelection({ it ->
                        it.property().ifPresent({ Var<FsNode> node ->
                            node.update({ FsNode n -> n instanceof Doc ? ((Doc) n).withName("Selected.java") : n })
                        })
                    })
                    .get(JTree)
            UI.runNow({ tree.expandRow(1) })

        when : 'The user selects the document two levels down.'
            UI.runNow({ tree.setSelectionRow(2) })

        then : 'The write went through the lens into the one bound property.'
            visibleRows(tree) == ["root", "    src", "        Selected.java"]
        and : 'And what came out is a new root value carrying the change.'
            ((Doc) ((Dir) fileSystem.get().entries().get(0)).entries().get(0)).name() == "Selected.java"
    }

    def 'Renaming a node writes one new root value back into the bound property.'()
    {
        reportInfo """
            An in place rename is what a tree offers when a node type declares a text rule
            *with a wither*. The edited text is handed to that wither, and the resulting node
            travels back up through every `children(getter, wither)` rule above it until it
            arrives as a new value of the single bound property.

            The tree drives this through the standard Swing editing path, which is why the
            scenario below calls `valueForPathChanged(..)`: that is the method a finished
            edit lands in.
        """
        given : 'A tree three levels deep, bound to a mutable property.'
            var fileSystem = Var.of(FsNode, new Dir("r", "root", Tuple.of(FsNode,
                                        new Dir("a", "src", Tuple.of(FsNode, new Doc("a1", "App.java"))),
                                        new Dir("b", "docs", Tuple.of(FsNode))
                                    )))
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }, { Dir d, Tuple<FsNode> e -> d.withEntries(e) })
                                          .text({ Dir d -> d.name() }, { Dir d, String t -> d.withName(t) }) })
                        .nodesOf(Doc, { it.text({ Doc d -> d.name() }, { Doc d, String t -> d.withName(t) }) })
                    })
                    .get(JTree)
            UI.runNow({ tree.expandRow(1) })
        and : 'We hold on to the root value as it is before the edit.'
            var before = fileSystem.get()

        when : 'The user finishes renaming the deepest document.'
            UI.runNow({ tree.getModel().valueForPathChanged(tree.getPathForRow(2), "Application.java") })

        then : 'The rename is in the property...'
            ((Doc) ((Dir) fileSystem.get().entries().get(0)).entries().get(0)).name() == "Application.java"
        and : '...and on screen.'
            visibleRows(tree) == ["root", "    src", "        Application.java", "    docs"]
        and : 'The value that was there before was not mutated, because it could not be.'
            ((Doc) ((Dir) before.entries().get(0)).entries().get(0)).name() == "App.java"
    }

    def 'Everything off the edited path is shared, not copied.'()
    {
        reportInfo """
            This is why an edit in a large tree is cheap. Rebuilding the path from the root
            down to the edited node is unavoidable: those nodes really did change, because
            each of them now holds a different child. Everything else does not change, and a
            persistent collection therefore hands back the very same objects.

            The assertions below are deliberately about **reference** identity. It is the same
            property the update walk exploits to skip untouched subtrees, seen from the other
            side.
        """
        given : 'A tree with two sibling folders, one of which we are about to edit.'
            var fileSystem = Var.of(FsNode, new Dir("r", "root", Tuple.of(FsNode,
                                        new Dir("a", "edited", Tuple.of(FsNode,    new Doc("a1", "one.txt"))),
                                        new Dir("b", "untouched", Tuple.of(FsNode, new Doc("b1", "two.txt")))
                                    )))
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }, { Dir d, Tuple<FsNode> e -> d.withEntries(e) })
                                          .text({ Dir d -> d.name() }, { Dir d, String t -> d.withName(t) }) })
                        .nodesOf(Doc, { it.text({ Doc d -> d.name() }, { Doc d, String t -> d.withName(t) }) })
                    })
                    .get(JTree)
            UI.runNow({ tree.expandRow(1) })
        and : 'We remember both folders as they are now.'
            var editedBefore    = fileSystem.get().entries().get(0)
            var untouchedBefore = fileSystem.get().entries().get(1)

        when : 'A document inside the first folder is renamed.'
            UI.runNow({ tree.getModel().valueForPathChanged(tree.getPathForRow(2), "ONE.txt") })

        then : 'The folder holding it is a new value, because it now holds a different document.'
            !fileSystem.get().entries().get(0).is(editedBefore)
        and : 'The other folder is not a copy of what it was, it is the exact same object.'
            fileSystem.get().entries().get(1).is(untouchedBefore)
    }

    def 'A read only branch rule drops edits coming from below it.'()
    {
        reportInfo """
            An edit travels home through the chain of `children(getter, wither)` rules above
            it. A branch declared with only a getter has no wither, so there is no way to put
            a changed child back into it, and the write stops there.

            That is a deliberate outcome rather than an error: declaring a read only rule is
            how you say a part of the structure is not the user's to rearrange. SwingTree logs
            the dropped write once, naming the type of the branch which refused it — once for
            the whole tree, not once per keystroke, because a user who holds a key down in a
            branch that will not take the result should not fill the log with it.
        """
        given : 'A tree in which directories expose their entries read only.'
            var log = LogSpy.attach()
            var fileSystem = Var.of(FsNode, new Dir("r", "root", Tuple.of(FsNode,
                                        new Dir("a", "src", Tuple.of(FsNode, new Doc("a1", "App.java")))
                                    )))
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() })                                // getter only
                                          .text({ Dir d -> d.name() }, { Dir d, String t -> d.withName(t) }) })
                        .nodesOf(Doc, { it.text({ Doc d -> d.name() }, { Doc d, String t -> d.withName(t) }) })
                    })
                    .get(JTree)
            UI.runNow({ tree.expandRow(1) })

        when : 'The user tries to rename a document below that branch, three times over.'
            UI.runNow({
                tree.getModel().valueForPathChanged(tree.getPathForRow(2), "Application.java")
                tree.getModel().valueForPathChanged(tree.getPathForRow(2), "Program.java")
                tree.getModel().valueForPathChanged(tree.getPathForRow(2), "Main.java")
            })

        then : 'Nothing changed, because there was no way back up through the read only branch.'
            ((Doc) ((Dir) fileSystem.get().entries().get(0)).entries().get(0)).name() == "App.java"
            visibleRows(tree) == ["root", "    src", "        App.java"]
        and : 'And it was said exactly once, naming the branch type which refused the write.'
            log.warnings().findAll { it.contains("read only 'children(getter)'") }.size() == 1
            log.warnings().any { it.contains("'Dir'") }

        when : 'The root itself is renamed, which needs no branch wither at all.'
            UI.runNow({ tree.getModel().valueForPathChanged(tree.getPathForRow(0), "workspace") })

        then : 'That edit does land, because nothing stood between it and the property.'
            fileSystem.get().name() == "workspace"

        cleanup :
            log.detach()
    }

    def 'A node type without a text wither cannot be renamed, even in an editable tree.'()
    {
        reportInfo """
            Editability is decided per node type, by whether that type declared how edited
            text gets back into it. A tree may therefore let the user rename its folders while
            leaving its documents alone, which is a common thing to want and needs no
            predicate anywhere.
        """
        given : 'A tree where only directories declare a text wither.'
            var fileSystem = Var.of(FsNode, new Dir("r", "root", Tuple.of(FsNode, new Doc("a", "notes.txt"))))
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }, { Dir d, Tuple<FsNode> e -> d.withEntries(e) })
                                          .text({ Dir d -> d.name() }, { Dir d, String t -> d.withName(t) }) })
                        .nodesOf(Doc, { it.text({ Doc d -> d.name() }) })                                    // getter only
                    })
                    .get(JTree)

        when : 'An edit is attempted on the document.'
            UI.runNow({ tree.getModel().valueForPathChanged(tree.getPathForRow(1), "renamed.txt") })

        then : 'It is ignored, because the document never said how to take it.'
            visibleRows(tree) == ["root", "    notes.txt"]

        when : 'The same edit is attempted on the directory.'
            UI.runNow({ tree.getModel().valueForPathChanged(tree.getPathForRow(0), "workspace") })

        then : 'It lands, because the directory did.'
            visibleRows(tree) == ["workspace", "    notes.txt"]
    }

    def 'An editor is offered for the node the edit is aimed at, not for the selected one.'()
    {
        reportInfo """
            Whether a node may be renamed is a question about *that* node, so it is asked of
            the node an edit is aimed at rather than of whatever happens to be selected at the
            time. A plain `JTree` lets `startEditingAtPath(..)` open an editor on a tree with
            no selection at all, and a bound tree does too.
        """
        given : 'A tree in which directories can be renamed.'
            var fileSystem = Var.of(FsNode, new Dir("r", "root", Tuple.of(FsNode, new Doc("a", "notes.txt"))))
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }, { Dir d, Tuple<FsNode> e -> d.withEntries(e) })
                                          .text({ Dir d -> d.name() }, { Dir d, String t -> d.withName(t) }) })
                        .nodesOf(Doc, { it.text({ Doc d -> d.name() }, { Doc d, String t -> d.withName(t) }) })
                    })
                    .get(JTree)

        when : 'An edit is started on a row while nothing at all is selected.'
            var editing = UI.runAndGet({
                tree.setSize(220, 120)
                tree.doLayout()
                tree.expandRow(0)
                tree.clearSelection()
                tree.startEditingAtPath(tree.getPathForRow(1))
                return [ tree.isEditing(), tree.getEditingPath() == tree.getPathForRow(1) ]
            })

        then : 'The editor opened, and on that very row.'
            editing == [true, true]

        cleanup :
            UI.runNow({ tree.stopEditing() })
    }

    def 'A tree bound to a read only property refuses edits outright.'()
    {
        reportInfo """
            The last of the conditions. Even with a full set of withers, there is nothing to
            write into when the bound property is read only, and SwingTree says so rather than
            quietly discarding the edit.
        """
        given : 'The same rules as an editable tree, but bound through the read only overload.'
            FsNode structure = new Dir("r", "root", Tuple.of(FsNode, new Doc("a", "notes.txt")))
            var tree =
                    UI.tree(FsNode, Val.of(structure), { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }, { Dir d, Tuple<FsNode> e -> d.withEntries(e) })
                                          .text({ Dir d -> d.name() }, { Dir d, String t -> d.withName(t) }) })
                        .nodesOf(Doc, { it.text({ Doc d -> d.name() }, { Doc d, String t -> d.withName(t) }) })
                    })
                    .get(JTree)

        when : 'An edit is attempted anyway.'
            UI.runNow({ tree.getModel().valueForPathChanged(new TreePath(tree.getModel().getRoot()), "workspace") })

        then : 'Nothing changed, and the tree never offered an editor in the first place.'
            visibleRows(tree) == ["root", "    notes.txt"]
            !tree.isEditable()
    }
}
