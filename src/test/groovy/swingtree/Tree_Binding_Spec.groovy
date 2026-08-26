package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.Tuple
import sprouts.Val
import sprouts.Var
import swingtree.api.IconDeclaration
import swingtree.threading.EventProcessor
import utility.LogSpy
import utility.SwingTreeTestConfigurator

import javax.swing.JTree
import javax.swing.ToolTipManager
import java.awt.event.MouseEvent

import static swingtree.TreeSpecFileSystem.dir
import static swingtree.TreeSpecFileSystem.doc
import static swingtree.TreeSpecFileSystem.expandedRows
import static swingtree.TreeSpecFileSystem.visibleRows

@Title("Growing a Tree from a Property")
@Narrative("""

    A `JTree` in SwingTree is not built by assembling mutable nodes. It is bound to a
    **single property holding one deeply immutable, nested value**, together with a small
    configuration saying which parts of that value the tree should zoom into.

    The value is usually a sum type. In Java 21 the file system these scenarios use would
    read like this:

    ```java
    public sealed interface FsNode extends HasId<String> { String name(); }
    public record Dir( String id, String name, Tuple<FsNode> entries ) implements FsNode {}
    public record Doc( String id, String name, String body )           implements FsNode {}
    ```

    And the tree over it like this:

    ```java
    UI.tree(fileSystem, conf -> conf
        .nodesOf(Dir.class, dir -> dir.children(Dir::entries).text(Dir::name))
        .nodesOf(Doc.class, doc -> doc.text(Doc::name))
    );
    ```

    One `nodesOf(..)` block per node type, reading like one `case` of the `switch` you
    would otherwise have written by hand. This document is about that configuration: what
    makes a node a branch or a leaf, where its label and icon come from, what identifies it,
    and how a tree of unrelated types is described.

    Two companion documents carry on from here: `Tree_Update_Spec` is about what happens
    when the bound value changes, and `Tree_Selection_And_Editing_Spec` is about selecting
    and editing in a bound tree.

""")
@Subject([UIForTree, TreeConf, TreeNodeConf])
class Tree_Binding_Spec extends Specification
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

    def 'A tree is one property plus one rule per node type.'()
    {
        reportInfo """
            This is the whole of the basic API. `UI.tree(..)` takes the property holding the
            root of your structure and a configurator declaring, per node type, where that
            type keeps its children and how it is labelled.

            Note what you do *not* write: no `TreeModel`, no `DefaultMutableTreeNode`, no
            loop copying your data into a second, mutable tree. The value you already have
            *is* the tree.
        """
        given : 'A little file system, held in a single property.'
            var fileSystem = Var.of(FsNode, dir("r", "root",
                                        dir("a", "assets", doc("a1", "logo.svg")),
                                        doc("b", "README.md")
                                    ))
        when : 'We describe it as a tree, one rule block per node type.'
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }).text({ Dir d -> d.name() }) })
                        .nodesOf(Doc, { it.text({ Doc d -> d.name() }) })
                    })
                    .get(JTree)

        then : 'The tree shows the root and its two entries, because a bound tree opens its root.'
            visibleRows(tree) == [
                "root",
                "    assets",
                "    README.md",
            ]
        and : 'The root is a branch holding those two entries.'
            !UI.runAndGet({ tree.getModel().isLeaf(tree.getModel().getRoot()) })
            UI.runAndGet({ tree.getModel().getChildCount(tree.getModel().getRoot()) }) == 2
    }

    def 'A node type is a leaf exactly when no rule gives it children.'()
    {
        reportInfo """
            There is no `leaf(..)` declaration to remember, and no "allows children" flag to
            set. Declaring a `children(..)` rule for a type is what makes that type a branch,
            and every type without one is a leaf.

            For a sum type this is exactly the right default: the cases that carry a
            collection of child nodes are the branches, and the cases that do not are the
            leaves. The shape of your data already said so.
        """
        given : 'A tree whose configuration gives directories children, but documents none.'
            var fileSystem = Var.of(FsNode, dir("r", "root",
                                        dir("empty", "empty-folder"),
                                        doc("d", "notes.txt")
                                    ))
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }).text({ Dir d -> d.name() }) })
                        .nodesOf(Doc, { it.text({ Doc d -> d.name() }) })
                    })
                    .get(JTree)
        and : 'We reach for the two children of the root.'
            var model      = tree.getModel()
            var emptyDir   = UI.runAndGet({ model.getChild(model.getRoot(), 0) })
            var document   = UI.runAndGet({ model.getChild(model.getRoot(), 1) })

        expect : 'The document is a leaf, because no rule ever hands it children.'
            UI.runAndGet({ model.isLeaf(document) })
        and : 'The directory is a branch even though it currently holds nothing at all.'
            !UI.runAndGet({ model.isLeaf(emptyDir) })
            UI.runAndGet({ model.getChildCount(emptyDir) }) == 0
    }

    def 'An empty branch stays a branch, unless you ask for the plain Swing behaviour.'()
    {
        reportInfo """
            A plain `JTree` calls any node with zero children a leaf, which draws an empty
            folder as though it were a document. SwingTree defaults the other way round,
            because a folder that happens to be empty is still a folder, and because the
            user has to be able to open it to see things arrive in it later.

            `leafWhenEmpty(true)` restores the plain Swing rule for the whole tree, for the
            rare model where an empty branch really is nothing.
        """
        given : 'A file system whose only entry is an empty directory.'
            var fileSystem = Var.of(FsNode, dir("r", "root", dir("e", "empty-folder")))
        and : 'Two trees over it, differing only in how they treat childless branches.'
            var defaultTree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }).text({ Dir d -> d.name() }) })
                    })
                    .get(JTree)
            var plainSwingTree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .leafWhenEmpty(true)
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }).text({ Dir d -> d.name() }) })
                    })
                    .get(JTree)

        expect : 'SwingTree keeps the empty directory a branch...'
            !UI.runAndGet({ defaultTree.getModel().isLeaf(defaultTree.getPathForRow(1).getLastPathComponent()) })
        and : '...while the opted-in tree calls it a leaf.'
            UI.runAndGet({ plainSwingTree.getModel().isLeaf(plainSwingTree.getPathForRow(1).getLastPathComponent()) })
    }

    def 'A node with no children yet can still be declared a branch, which is how lazy loading works.'()
    {
        reportInfo """
            A tree whose branches are fetched on demand has a third kind of node: one that
            *will* have children, but does not have them yet. It has to draw a handle,
            because expanding it is the gesture that triggers the load.

            `isLeaf(false)` says exactly that. It is the one place the derived default needs
            overriding, and it is what makes a `Pending` case of a sum type behave the way
            the user expects.
        """
        given : 'A file system in which one directory has not been read from disk yet.'
            var fileSystem = Var.of(FsNode, dir("r", "root", dir("lazy", "not-loaded-yet")))
        and : 'A tree which declares the empty directory a branch regardless.'
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }).text({ Dir d -> d.name() }).isLeaf(false) })
                    })
                    .get(JTree)

        expect : 'It is a branch, so the tree draws a handle the user can click.'
            !UI.runAndGet({ tree.getModel().isLeaf(tree.getPathForRow(1).getLastPathComponent()) })
        and : 'Even though there is nothing below it to show.'
            UI.runAndGet({ tree.getModel().getChildCount(tree.getPathForRow(1).getLastPathComponent()) }) == 0
    }

    def 'The label of a node comes from its `text(..)` rule.'()
    {
        reportInfo """
            Without a `text(..)` rule a tree falls back to `toString()`, which is almost
            never what a value object should show a user. Declaring the label as part of the
            node's own rule keeps it next to everything else that is true about that type.

            Note that the two node types here are labelled from *different* fields, which is
            the whole point of declaring the rule per type rather than once for the tree.
        """
        given : 'A tree labelling directories by their name and documents by name and size.'
            var fileSystem = Var.of(FsNode, dir("r", "Projects",
                                        doc("a", "notes.txt", "0123456789")
                                    ))
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }).text({ Dir d -> d.name() }) })
                        .nodesOf(Doc, { it.text({ Doc d -> d.name() + " (" + d.body().length() + " bytes)" }) })
                    })
                    .get(JTree)

        expect : 'Each node is labelled by its own rule.'
            visibleRows(tree) == [
                "Projects",
                "    notes.txt (10 bytes)",
            ]
    }

    def 'Without a text rule the tree falls back to the `toString()` of the node.'()
    {
        reportInfo """
            The fallback exists so that a half-finished tree still shows you something you
            can recognise while you are building it. It is not meant to be shipped: a record
            prints all of its fields, which is rarely a good label.
        """
        given : 'A tree which declares where the children are, but not how to label anything.'
            var fileSystem = Var.of(FsNode, dir("r", "root", doc("a", "notes.txt")))
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }) })
                        .nodesOf(Doc, { it.isLeaf(true) })
                    })
                    .get(JTree)

        expect : 'The nodes are labelled by their own `toString()`.'
            visibleRows(tree) == [
                "Dir(root)",
                "    Doc(notes.txt)",
            ]
    }

    def 'Icons and tool tips are declared right next to the label.'()
    {
        reportInfo """
            An icon is declared as an `IconDeclaration`, the same lightweight value SwingTree
            uses everywhere else, so it is resolved through the shared icon cache and an SVG
            source stays crisp at any UI scale.

            Declaring it per node type is what lets a folder and a document look different
            without a single `if` in a renderer.

            A tool tip needs one thing beyond the renderer to actually appear: a `JTree`
            shows the tips of its renderer only if it is registered with Swing's
            `ToolTipManager`, which it is not by default. A bound tree registers itself, so
            a declared `toolTip(..)` is a tip the user really sees.
        """
        given : 'Two icon declarations, one per node type.'
            IconDeclaration folderIcon = { "img/two_16th_notes.svg" }
            IconDeclaration pageIcon   = { "img/trees.png" }
        and : 'A tree declaring an icon and a tool tip for each type.'
            var fileSystem = Var.of(FsNode, dir("r", "root", doc("a", "notes.txt")))
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() })
                                          .text({ Dir d -> d.name() })
                                          .icon({ Dir d -> folderIcon })
                                          .toolTip({ Dir d -> "A folder holding " + d.entries().size() + " entries" }) })
                        .nodesOf(Doc, { it.text({ Doc d -> d.name() })
                                          .icon({ Doc d -> pageIcon })
                                          .toolTip({ Doc d -> "A document" }) })
                    })
                    .get(JTree)
        and : 'We render the root node the way the tree paints it.'
            var model  = tree.getModel()
            var view   = UI.runAndGet({ tree.getCellRenderer().getTreeCellRendererComponent(
                            tree, model.getRoot(), false, true, false, 0, false
                         ) })

        expect : 'The declared icon reached the cell...'
            view.getIcon() != null
        and : '...and so did the tool tip, which reads off the node it belongs to.'
            view.getToolTipText() == "A folder holding 1 entries"
        and : 'The tree registered itself with the tool tip manager, without which nothing would show.'
            tree.getMouseListeners().any({ it instanceof ToolTipManager })
        and : 'So the tree answers a pointer resting over the root row with that very text.'
            UI.runAndGet({
                tree.setSize(200, 100)
                tree.doLayout()
                var bounds = tree.getRowBounds(0)
                tree.getToolTipText(new MouseEvent(tree, MouseEvent.MOUSE_MOVED, 0, 0,
                                        (int)(bounds.x + 2), (int)(bounds.y + 2), 0, false))
            }) == "A folder holding 1 entries"
    }

    def 'The most specific rule wins, so a general rule can be refined for one type.'()
    {
        reportInfo """
            Rules are looked up by the type of the node, and the most specific declared rule
            wins. So a general rule can carry the types that have nothing special to say,
            while the ones that do get a block of their own.

            Here a rule on the `FsNode` interface labels everything by its name, and a rule on
            `Dir` gives directories their children. Note that the `Dir` block restates the
            label: the winning rule is used whole, it does not inherit from the general one.
            The scenario after next is about exactly that.
        """
        given : 'A tree with a rule for the shared interface and a more specific one for directories.'
            var fileSystem = Var.of(FsNode, dir("r", "root", doc("a", "notes.txt")))
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf({ it.text({ FsNode n -> n.name().toUpperCase() }) })
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }).text({ Dir d -> d.name() }) })
                    })
                    .get(JTree)

        expect : 'Directories use their own rule, documents fall back to the general one.'
            visibleRows(tree) == [
                "root",
                "    NOTES.TXT",
            ]
    }

    def 'A tree of unrelated types needs only its common supertype named.'()
    {
        reportInfo """
            Not every tree is a sum type. A company holds departments, a department holds
            employees, and those are three records with nothing in common but `Object`.

            The rules carry the shape just as well; what the tree needs is the common
            supertype spelled out, which is what the `UI.tree(Class, .., ..)` overload is
            for. Note also that the child collection is free to have a different element
            type from the node type itself, which is what makes a heterogeneous tree work at
            all.
        """
        given : 'Three unrelated value types, one per level of the tree.'
            var company = Var.of(Object, new Company("Globaltcad", Tuple.of(Department,
                                    new Department("d1", "Research", Tuple.of(Employee,
                                        new Employee("e1", "Ada"), new Employee("e2", "Grace"))),
                                    new Department("d2", "Support", Tuple.of(Employee,
                                        new Employee("e3", "Alan")))
                                )))
        when : 'We declare one rule block per level, naming Object as the common node type.'
            var tree =
                    UI.tree(Object, String, company, { conf -> conf
                        .idOf({ Object node -> node instanceof Department ? ((Department) node).id()
                                             : node instanceof Employee   ? ((Employee) node).id()
                                             : "company" })
                        .nodesOf(Company,    { it.children({ Company c -> c.departments() }).text({ Company c -> c.name() }) })
                        .nodesOf(Department, { it.children({ Department d -> d.staff() }).text({ Department d -> d.name() }) })
                        .nodesOf(Employee,   { it.text({ Employee e -> e.name() }) })
                    })
                    .withInitialExpansionDepth(3)
                    .get(JTree)

        then : 'The three levels appear, each labelled by its own rule.'
            visibleRows(tree) == [
                "Globaltcad",
                "    Research",
                "        Ada",
                "        Grace",
                "    Support",
                "        Alan",
            ]
    }

    def 'A node type with no rule at all is treated as a leaf.'()
    {
        reportInfo """
            Java 8, which SwingTree compiles against, cannot check a sealed hierarchy for
            exhaustiveness, so a forgotten `nodesOf(..)` block cannot be a compile error.

            It is not silent either: such a node becomes a leaf labelled by its `toString()`,
            and SwingTree logs a warning naming the type it has no rule for, because a
            missing case is almost always an oversight rather than an intent.
        """
        given : 'A tree which forgets to declare anything about documents.'
            var fileSystem = Var.of(FsNode, dir("r", "root", doc("a", "notes.txt")))
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }).text({ Dir d -> d.name() }) })
                    })
                    .get(JTree)

        expect : 'The undeclared type still shows up, as a leaf labelled by its `toString()`.'
            visibleRows(tree) == [
                "root",
                "    Doc(notes.txt)",
            ]
            UI.runAndGet({ tree.getModel().isLeaf(tree.getPathForRow(1).getLastPathComponent()) })
    }

    def 'Node identity comes from `HasId`, or from an `idOf(..)` rule for types you do not own.'()
    {
        reportInfo """
            A tree of value objects needs an answer to "which node is this?" that survives
            the node's content changing, because that is what keeps expanded paths and the
            selection alive across an edit. Nodes implementing `sprouts.HasId` answer it for
            free.

            For a type you cannot change, `idOf(..)` supplies the answer instead. Note how
            little is required of it: ids only have to be unique **among siblings**, because
            the path leading to a node disambiguates the rest. The two documents below
            deliberately share an id, and the folders above them keep them apart.
        """
        given : 'A structure in which the same id is deliberately reused in two different folders.'
            var fileSystem = Var.of(FsNode, dir("r", "root",
                                        dir("a", "first",  doc("notes", "notes.txt")),
                                        dir("b", "second", doc("notes", "notes.txt"))
                                    ))
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }).text({ Dir d -> d.name() }) })
                        .nodesOf(Doc, { it.text({ Doc d -> d.name() }) })
                    })
                    .withInitialExpansionDepth(3)
                    .get(JTree)

        expect : 'Both folders start out open.'
            expandedRows(tree) == ["root", "first", "second"]

        when : 'The user closes the second folder, leaving only the first one open.'
            UI.runNow({ tree.collapseRow(3) })

        then : 'The two same-named documents were told apart by the folders above them.'
            expandedRows(tree) == ["root", "first"]
            visibleRows(tree) == [
                "root",
                "    first",
                "        notes.txt",
                "    second",
            ]
    }

    def 'Hiding the root turns a single rooted value into what looks like a forest.'()
    {
        reportInfo """
            A property always holds exactly one value, so a bound tree always has exactly one
            root. That is usually not what should be on screen: a file browser shows the
            *contents* of a folder, not the folder itself.

            `withRootVisible(false)` drops that row, and turns the handles on for the top
            level so the user can still open what is now the first visible level.
        """
        given : 'A file system with a root nobody needs to see.'
            var fileSystem = Var.of(FsNode, dir("r", "<workspace>",
                                        dir("a", "src"),
                                        dir("b", "docs")
                                    ))
        when : 'We hide the root.'
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }).text({ Dir d -> d.name() }) })
                    })
                    .withRootVisible(false)
                    .get(JTree)

        then : 'Its children become the top level rows.'
            visibleRows(tree).collect { it.trim() } == ["src", "docs"]
        and : 'And the tree draws handles next to them, so they can still be opened.'
            tree.getShowsRootHandles()
    }

    def 'The initial expansion depth decides how much of the tree is open to begin with.'()
    {
        reportInfo """
            A freshly bound tree opens its root and nothing else, which is the plain Swing
            behaviour. `withInitialExpansionDepth(n)` opens `n` levels below the root
            instead, which is how an outline or a small settings tree presents itself
            usefully on first sight.

            It only shapes the initial view; expansion the user performs afterwards is
            untouched by it.
        """
        given : 'A file system three levels deep.'
            var fileSystem = Var.of(FsNode, dir("r", "root",
                                        dir("a", "src", dir("a1", "main", doc("a11", "App.java"))),
                                        dir("b", "docs")
                                    ))
            var conf = { conf -> conf
                            .nodesOf(Dir, { it.children({ Dir d -> d.entries() }).text({ Dir d -> d.name() }) })
                            .nodesOf(Doc, { it.text({ Doc d -> d.name() }) })
                        }

        when : 'We build one tree with no expansion depth and one which opens two levels.'
            var closedTree = UI.tree(FsNode, fileSystem, conf).get(JTree)
            var openTree   = UI.tree(FsNode, fileSystem, conf).withInitialExpansionDepth(2).get(JTree)

        then : 'The first shows only the root and its immediate entries.'
            visibleRows(closedTree) == [
                "root",
                "    src",
                "    docs",
            ]
        and : 'The second has opened one level further down.'
            visibleRows(openTree) == [
                "root",
                "    src",
                "        main",
                "    docs",
            ]
    }

    def 'A custom cell renderer still sees your own node values, never the tree internals.'()
    {
        reportInfo """
            A bound tree keeps internal handles inside its `TreePath`s, because value
            identity cannot key an expanded path (see `Tree_Update_Spec` for why). Those
            handles are an internal matter, and no renderer ever has to know about them:
            SwingTree unwraps them before your renderer or your `withCells(..)` declaration
            is asked anything.

            So `when(Doc.class)` matches a document, and `cell.entry()` hands you a `Doc`.
        """
        given : 'A tree whose cells are declared through the shared cell builder API.'
            var fileSystem = Var.of(FsNode, dir("r", "root", doc("a", "notes.txt")))
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }) })
                    })
                    .withCells({ it
                        .when(Dir).asText({ cell -> "[" + cell.entry().map({ Dir d -> d.name() }).orElse("?") + "]" })
                        .when(Doc).asText({ cell -> "- " + cell.entry().map({ Doc d -> d.name() }).orElse("?") })
                    })
                    .get(JTree)

        expect : 'Each cell was rendered by the declaration matching its own node type.'
            visibleRows(tree) == [
                "[root]",
                "    - notes.txt",
            ]
    }

    def 'A cell view for one node type leaves every other type its own label.'()
    {
        reportInfo """
            `withCells(..)` and the `text(..)` / `icon(..)` rules are not an either-or. A tree
            usually needs a richer view for exactly one of its node types, and it would be a
            poor trade to have to restate the label of every other type just to get it.

            So a node type no `when(..)` clause covers keeps whatever its own rule says. Only
            the types you actually name here are taken over.
        """
        given : 'A tree whose types are labelled by their rules, and one type given a cell view.'
            var fileSystem = Var.of(FsNode, dir("r", "root",
                                        dir("a", "assets", doc("a1", "logo.svg")),
                                        doc("b", "README.md")
                                    ))
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }).text({ Dir d -> d.name() }) })
                        .nodesOf(Doc, { it.text({ Doc d -> d.name() }) })
                    })
                    .withCells({ it
                        .when(Doc).asText({ cell -> "\u00b7 " + cell.entry().map({ Doc d -> d.name() }).orElse("?") })
                    })
                    .withInitialExpansionDepth(3)
                    .get(JTree)

        expect : 'Documents use the cell view, and directories keep the label of their own rule.'
            visibleRows(tree) == [
                "root",
                "    assets",
                "        \u00b7 logo.svg",
                "    \u00b7 README.md",
            ]
    }

    def 'A more specific rule replaces a general one, it does not extend it.'()
    {
        reportInfo """
            Rules are looked up per node type and the most specific match is the answer, whole.
            It is worth being precise about what that means: the winning block is used *on its
            own*, so a block declared for one type does not inherit the label of a catch-all
            block above it.

            The reason is that a `nodesOf(..)` block reads like one `case` of a `switch`, and a
            `case` does not silently continue into another. If a type needs both the general
            label and its own children, it says both.
        """
        given : 'A catch-all rule labelling everything, and a directory rule declaring only children.'
            var fileSystem = Var.of(FsNode, dir("r", "root", doc("a", "notes.txt")))
            var forgetful =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf({ it.text({ FsNode n -> n.name() }) })
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }) })
                    })
                    .get(JTree)

        expect : 'The directory has no label of its own left, so it falls back to `toString()`.'
            visibleRows(forgetful) == [
                "Dir(root)",
                "    notes.txt",
            ]

        when : 'The directory rule states the label it wants as well.'
            var complete =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf({ it.text({ FsNode n -> n.name() }) })
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }).text({ Dir d -> d.name() }) })
                    })
                    .get(JTree)

        then : 'Both are labelled, each by the rule that won for its own type.'
            visibleRows(complete) == [
                "root",
                "    notes.txt",
            ]
    }

    def 'Two siblings sharing an id are reported rather than silently merged.'()
    {
        reportInfo """
            An id has to be unique among siblings, and only among siblings, because the parent
            is part of the address. Two children of one node sharing an id are genuinely
            indistinguishable to a tree which addresses a node by its path of ids: they
            collapse onto one position, so one of them is drawn twice and the other never.

            Nothing can be done about that from inside the library, but a warning naming the
            offending id beats leaving somebody to work out why a row appeared twice.
        """
        given : 'A folder holding two documents which were given the same id.'
            var log = LogSpy.attach()
            var fileSystem = Var.of(FsNode, dir("r", "root", doc("dup", "first.txt"), doc("dup", "second.txt")))
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }).text({ Dir d -> d.name() }) })
                        .nodesOf(Doc, { it.text({ Doc d -> d.name() }) })
                    })
                    .withInitialExpansionDepth(3)
                    .get(JTree)

        when : 'The tree is asked what it shows.'
            var rows = visibleRows(tree)

        then : 'SwingTree said what went wrong, naming the id the two share.'
            log.warnings().any { it.contains("share the id 'dup'") }
        and : 'And the two really are one position, which is the whole reason for the warning.'
            rows.size() == 3

        cleanup :
            log.detach()
    }

    def 'A read only property yields a tree you can browse but not edit.'()
    {
        reportInfo """
            Two conditions have to hold before a tree lets the user edit anything: the
            property has to be writable, and a node type has to declare a text wither saying
            how the edited text gets back into the node. Either one missing means a tree the
            user can browse and select in, but not change.

            Which overload you reach for is part of that decision, and it is decided by the
            *declared* type of what you pass, not its runtime type. A view model exposing its
            `Var` as a `Val` therefore hands out a genuinely read only tree, rather than one
            that happens to be editable because of what the reference points at.
        """
        given : 'The same structure and the same renaming rules for both trees.'
            FsNode structure = dir("r", "root", doc("a", "notes.txt"))
            var conf = { conf -> conf
                            .nodesOf(Dir, { it.children({ Dir d -> d.entries() }).text({ Dir d -> d.name() }, { Dir d, String t -> d.withName(t) }) })
                            .nodesOf(Doc, { it.text({ Doc d -> d.name() }, { Doc d, String t -> d.withName(t) }) })
                        }

        when : 'We bind one tree to a mutable property and one to a read only one.'
            var editableTree = UI.tree(FsNode, Var.of(FsNode, structure), conf).get(JTree)
            var readOnlyTree = UI.tree(FsNode, Val.of(structure), conf).get(JTree)

        then : 'Both render exactly the same thing.'
            visibleRows(editableTree) == ["root", "    notes.txt"]
            visibleRows(readOnlyTree) == ["root", "    notes.txt"]
        and : 'But only the one bound to a mutable property lets the user start editing.'
            editableTree.isEditable()
            !readOnlyTree.isEditable()
    }

    def 'A tree whose rules declare no wither is not editable either.'()
    {
        reportInfo """
            The second of the two conditions: a mutable property is not on its own enough.
            Without a text wither there is nothing to turn edited text back into a node, so
            SwingTree does not offer the user an editor it could not honour.
        """
        given : 'A mutable property, but rules which only read.'
            var fileSystem = Var.of(FsNode, dir("r", "root", doc("a", "notes.txt")))
        when : 'We bind it with read only text rules.'
            var tree =
                    UI.tree(FsNode, fileSystem, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> d.entries() }).text({ Dir d -> d.name() }) })
                        .nodesOf(Doc, { it.text({ Doc d -> d.name() }) })
                    })
                    .get(JTree)

        then : 'The tree is not editable, because no rule said how an edit would get home.'
            !tree.isEditable()
    }

    /** A company, which knows nothing about departments beyond holding them. */
    static final class Company {
        private final String name; private final Tuple<Department> departments
        Company( String name, Tuple<Department> departments ) { this.name = name; this.departments = departments }
        String name() { return name }
        Tuple<Department> departments() { return departments }
        Company withDepartments( Tuple<Department> d ) { return new Company(name, d) }
    }

    /** A department, holding employees, which are again an unrelated type. */
    static final class Department {
        private final String id; private final String name; private final Tuple<Employee> staff
        Department( String id, String name, Tuple<Employee> staff ) { this.id = id; this.name = name; this.staff = staff }
        String id() { return id }
        String name() { return name }
        Tuple<Employee> staff() { return staff }
        Department withStaff( Tuple<Employee> s ) { return new Department(id, name, s) }
    }

    /** An employee, which is a leaf because no rule gives it children. */
    static final class Employee {
        private final String id; private final String name
        Employee( String id, String name ) { this.id = id; this.name = name }
        String id() { return id }
        String name() { return name }
    }
}
