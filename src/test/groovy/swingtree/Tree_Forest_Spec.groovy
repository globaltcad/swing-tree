package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.Tuple
import sprouts.Val
import sprouts.Var
import sprouts.Viewable
import swingtree.threading.EventProcessor
import utility.LogSpy
import utility.SwingTreeTestConfigurator

import javax.swing.JTree

import static swingtree.TreeSpecFileSystem.expandedRows
import static swingtree.TreeSpecFileSystem.selectedRows
import static swingtree.TreeSpecFileSystem.visibleRows

@Title("Growing a Forest from a Property")
@Narrative("""

    A great many trees have no root. A workspace holds several open projects, a document
    holds several top level blocks, a store room holds several shelves, a scene holds
    several graphs. Their natural shape is not one value but a tuple of them:

    ```java
    Var<Tuple<FsNode>> projects;
    ```

    `UI.trees(..)` — plural — binds exactly that. It is the same API as `UI.tree(..)` in
    every other respect: one `TreeConf` describing the node types, a `children(..)` rule
    making a type a branch, a wither turning a rule into a two way lens. What differs is
    only the two ends of it. Nothing is drawn above the top level, and a selection path
    starts at a top level node rather than at a root.

    The alternative — and this is what the plural form exists to spare you — is to invent a
    container type for the view:

    ```java
    record Workspace( Tuple<FsNode> roots ) implements FsNode { .. }   // a case the domain does not have
    ```

    That is a permitted subtype every `switch` over `FsNode` in the application then has to
    handle, which is precisely the property a sealed interface was chosen for. And because a
    selection is a path of ids, the invented id ends up inside every selection path the
    application persists. The fiction does not stay at the binding site; it contaminates
    the data.

    Under the hood there is still exactly one `JTree`, because `javax.swing.tree.TreeModel`
    declares `Object getRoot()` and has never had room for more than one. The container root
    still exists — it is simply SwingTree's, held inside the model, never drawn, and never
    named by anything the application can see.

""")
@Subject([UIForTree, TreeConf])
class Tree_Forest_Spec extends Specification
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

    /** The rules every scenario below shares: folders nest and rename, documents rename. */
    private static Closure shape() {
        return { conf -> conf
            .nodesOf(Dir, { it.children({ Dir d -> d.entries() }, { Dir d, Tuple<FsNode> e -> d.withEntries(e) })
                              .text({ Dir d -> d.name() }, { Dir d, String n -> d.withName(n) }) })
            .nodesOf(Doc, { it.text({ Doc d -> d.name() }, { Doc d, String n -> d.withName(n) }) })
        }
    }

    def 'A forest is a property holding a tuple of top level nodes.'()
    {
        reportInfo """
            `UI.trees(..)` takes the property and a configurator, exactly like `UI.tree(..)`
            does. The declaration is the same one; only the property is one dimension wider.

            Note what is *not* in the rules: nothing describes the top level. There is no rule
            for it because there is no node there — the tuple is the top level, and the tree
            reads it straight out of the property.
        """
        given : 'Two projects, held side by side in a single property.'
            var projects = Var.of(Tuple.of(FsNode,
                                new Dir("app", "myapp", Tuple.of(FsNode, new Dir("src", "src", Tuple.of(FsNode, new Doc("main", "App.java"))))),
                                new Dir("doc", "notes", Tuple.of(FsNode, new Doc("readme", "README.md")))
                           ))

        when : 'We grow a forest from it.'
            var tree = UI.trees(projects, shape()).get(JTree)

        then : 'Both projects are top level rows, with nothing above them.'
            visibleRows(tree) == ["myapp", "notes"]
        and : 'The tree draws no root...'
            !UI.runAndGet({ tree.isRootVisible() })
        and : '...and turns on the handles of the top level, which are now the only ones there are.'
            UI.runAndGet({ tree.getShowsRootHandles() })
    }

    def 'The node type is taken from the tuple, which carries it even while empty.'()
    {
        reportInfo """
            `UI.tree(root, ..)` learns the node type from the property. A property holding a
            tuple cannot say the same, because its own type is the tuple's — but a sprouts
            `Tuple` knows what its elements are, and knows it whether or not it holds any.

            So a forest which starts out empty, which is the ordinary case for a workspace on
            first launch, still knows what it is a forest *of*. Where the tuple was built with
            a narrower element type than the tree's nodes, name the node type explicitly with
            `UI.trees(FsNode.class, projects, ..)`.
        """
        given : 'An empty workspace: a property holding a tuple with nothing in it.'
            var projects = Var.of(Tuple.of(FsNode))

        expect : 'The tuple still knows what its elements are.'
            projects.get().type() == FsNode

        when : 'We bind it, and only afterwards put a project in it.'
            var tree = UI.trees(projects, shape()).get(JTree)

        then : 'An empty forest is an empty tree: no rows, and no placeholder row either.'
            visibleRows(tree) == []

        when : 'The workspace fills in.'
            UI.runNow({ projects.set(Tuple.of(FsNode, new Dir("app", "myapp", Tuple.of(FsNode, new Doc("main", "App.java"))))) })

        then : 'So does the tree.'
            visibleRows(tree) == ["myapp"]
    }

    def 'A selection path names a top level node, with no synthetic element in front of it.'()
    {
        reportInfo """
            This is the reason the plural form is worth having. A selection is a path of ids,
            and that path is what gets written into the view model, persisted, asserted on in
            tests and restored on the next launch.

            Wrap the tuple in a container to satisfy `UI.tree(..)` and every one of those
            paths starts with the container's invented id. Bind the tuple directly and the
            path holds exactly the ids the application chose, and nothing else.
        """
        given : 'A forest and a property to mirror its selection into.'
            var projects = Var.of(Tuple.of(FsNode,
                                new Dir("app", "myapp", Tuple.of(FsNode, new Dir("src", "src", Tuple.of(FsNode, new Doc("main", "App.java"))))),
                                new Dir("doc", "notes", Tuple.of(FsNode, new Doc("readme", "README.md")))
                           ))
            var selected = Var.of(Tuple.of(String))
            var tree = UI.trees(projects, shape()).withSelection(selected).get(JTree)

        when : 'We assign a path naming a document three levels down.'
            UI.runNow({ selected.set(Tuple.of(String, "app", "src", "main")) })

        then : 'The tree opened every folder above it and selected the row.'
            selectedRows(tree) == ["App.java"]
            visibleRows(tree) == [
                "myapp",
                "    src",
                "        App.java",
                "notes",
            ]

        when : 'And when the user clicks the other project instead...'
            UI.runNow({ tree.setSelectionRow(3) })

        then : '...what arrives in the property is one id, because a top level node is one level deep.'
            selected.get() == Tuple.of(String, "doc")

        when : 'The selection is cleared.'
            UI.runNow({ tree.clearSelection() })

        then : 'The property says so with the empty path, which is the only special case left.'
            selected.get().isEmpty()
    }

    def 'A top level node added or removed leaves the rest of the forest as it was.'()
    {
        reportInfo """
            Adding a project is a structural change at the very top, which is the one place a
            single rooted tree never has to deal with. The tree still keeps what the user had
            open, because expansion is captured by ids and put back by ids, and the ids of
            everything else did not change.
        """
        given : 'A forest with both of its projects open.'
            var projects = Var.of(Tuple.of(FsNode,
                                new Dir("app", "myapp", Tuple.of(FsNode, new Dir("src", "src", Tuple.of(FsNode, new Doc("main", "App.java"))))),
                                new Dir("doc", "notes", Tuple.of(FsNode, new Doc("readme", "README.md")))
                           ))
            var tree = UI.trees(projects, shape()).withInitialExpansionDepth(1).get(JTree)

        expect : 'Both are open to begin with.'
            expandedRows(tree) == ["myapp", "notes"]

        when : 'A third project is opened, at the end.'
            UI.runNow({ projects.update({ it.add(new Dir("new", "scratch", Tuple.of(FsNode, new Doc("todo", "todo.txt")))) }) })

        then : 'It appears, and the two that were open are still open.'
            visibleRows(tree) == [
                "myapp",
                "    src",
                "notes",
                "    README.md",
                "scratch",
            ]

        when : 'And the first one is closed again.'
            UI.runNow({ projects.update({ it.removeFirst() }) })

        then : 'It is gone, and the others kept their place and their expansion.'
            visibleRows(tree) == [
                "notes",
                "    README.md",
                "scratch",
            ]
    }

    def 'A change inside one project does not so much as look at the others.'()
    {
        reportInfo """
            The cost story of `Tree_Update_Spec` holds at the top level too. The tuple is
            persistent, so replacing one of its elements leaves the others not merely equal to
            what they were but the very same objects, and the update walk compares by
            reference and stops there.

            The counter below records which project gets asked for its entries. After an edit
            inside the first one, the second is never asked at all, though it is expanded and
            on screen.
        """
        given : 'A counter recording which project gets walked into.'
            var reads = [:].withDefault { 0 }
        and : 'A forest whose two projects are both open.'
            var projects = Var.of(Tuple.of(FsNode,
                                new Dir("app", "edited", Tuple.of(FsNode,    new Doc("one", "one.txt"))),
                                new Dir("doc", "untouched", Tuple.of(FsNode, new Doc("two", "two.txt")))
                           ))
            var tree =
                    UI.trees(projects, { conf -> conf
                        .nodesOf(Dir, { it.children({ Dir d -> reads[d.name()] = reads[d.name()] + 1; d.entries() },
                                                    { Dir d, Tuple<FsNode> e -> d.withEntries(e) })
                                          .text({ Dir d -> d.name() }) })
                        .nodesOf(Doc, { it.text({ Doc d -> d.name() }) })
                    })
                    .withInitialExpansionDepth(2)
                    .get(JTree)

        expect : 'Both are on screen and open.'
            visibleRows(tree) == [
                "edited",
                "    one.txt",
                "untouched",
                "    two.txt",
            ]

        when : 'We forget every read so far and then rename a document in the first project.'
            reads.clear()
            UI.runNow({ projects.update({ Tuple<FsNode> roots ->
                var edited = (Dir) roots.get(0)
                roots.setAt(0, edited.withEntries(
                    edited.entries().setAt(0, ((Doc) edited.entries().get(0)).withName("ONE.txt"))
                ))
            }) })

        then : 'The rename is on screen.'
            visibleRows(tree).contains("    ONE.txt")
        and : 'The edited project was walked into...'
            reads["edited"] > 0
        and : '...and the untouched one was never asked for anything, being the same object as before.'
            reads["untouched"] == 0
    }

    def 'An edit deep inside a project comes out the top as one new tuple.'()
    {
        reportInfo """
            The chain of `children(getter, wither)` rules is what carries a change upwards, and
            it does not stop at the top level node: a forest's last step is the tuple itself,
            which the binding rebuilds and writes back into the one property.

            So renaming a document four levels down produces a new document, a new folder, a
            new project and one new tuple — with every other project shared rather than copied.
        """
        given : 'A forest, with an editable rule for every node type.'
            var projects = Var.of(Tuple.of(FsNode,
                                new Dir("app", "myapp", Tuple.of(FsNode, new Dir("src", "src", Tuple.of(FsNode, new Doc("main", "App.java"))))),
                                new Dir("doc", "notes", Tuple.of(FsNode, new Doc("readme", "README.md")))
                           ))
            var untouchedBefore = projects.get().get(1)
            var tree = UI.trees(projects, shape()).withInitialExpansionDepth(3).get(JTree)

        when : 'The user renames the document the third row shows.'
            var path = UI.runAndGet({ tree.getPathForRow(2) })
            UI.runNow({ tree.getModel().valueForPathChanged(path, "Main.java") })

        then : 'The property holds a new tuple, whose first project carries the rename.'
            ((Dir)((Dir) projects.get().get(0)).entries().get(0)).entries().get(0).name() == "Main.java"
        and : 'The other project was not copied on the way through: it is the same object.'
            projects.get().get(1) === untouchedBefore
        and : 'And the tree shows it.'
            visibleRows(tree).contains("        Main.java")
    }

    def 'Renaming a top level node writes straight into the tuple.'()
    {
        reportInfo """
            A top level node is the shortest possible case of the same walk: there is no
            branch above it to rebuild, so the write lands directly in the bound tuple.

            Note that this needs no `children(..)` wither anywhere. A single rooted tree can
            only rename its root because the root *is* the property; a forest can rename any
            of its top level nodes for exactly the same reason.
        """
        given : 'A forest of two projects.'
            var projects = Var.of(Tuple.of(FsNode,
                                new Dir("app", "myapp", Tuple.of(FsNode, new Doc("main", "App.java"))),
                                new Dir("doc", "notes", Tuple.of(FsNode, new Doc("readme", "README.md")))
                           ))
            var tree = UI.trees(projects, shape()).get(JTree)

        when : 'The user renames the second one in place.'
            var path = UI.runAndGet({ tree.getPathForRow(1) })
            UI.runNow({ tree.getModel().valueForPathChanged(path, "scratch") })

        then : 'The tuple in the property holds the renamed project, in its old position.'
            projects.get().get(1).name() == "scratch"
            projects.get().size() == 2
        and : 'And the row reads differently.'
            visibleRows(tree) == ["myapp", "scratch"]
    }

    def 'The initial expansion depth counts what is on screen, not what is in the model.'()
    {
        reportInfo """
            `withInitialExpansionDepth(1)` opens the topmost visible level. For a single rooted
            tree that is the root; for a forest, whose root is never drawn, it is the top level
            nodes. The argument means the same thing in both, which is to say it means what
            the user would count looking at the screen.
        """
        given : 'A forest three levels deep.'
            var projects = Var.of(Tuple.of(FsNode,
                                new Dir("app", "myapp", Tuple.of(FsNode, new Dir("src", "src", Tuple.of(FsNode, new Doc("main", "App.java"))))),
                                new Dir("doc", "notes", Tuple.of(FsNode, new Doc("readme", "README.md")))
                           ))

        when : 'We build one forest closed and one opened a single level.'
            var closed = UI.trees(projects, shape()).get(JTree)
            var opened = UI.trees(projects, shape()).withInitialExpansionDepth(1).get(JTree)

        then : 'The first shows only the projects themselves.'
            visibleRows(closed) == ["myapp", "notes"]
        and : 'The second has opened each of them one level.'
            visibleRows(opened) == [
                "myapp",
                "    src",
                "notes",
                "    README.md",
            ]
    }

    def 'A forest has no root, so asking to show one is refused and reported.'()
    {
        reportInfo """
            `withRootVisible(true)` is meaningful on a tree whose data really has a root. A
            forest has none — the container the `JTree` needs is SwingTree's own, and drawing
            it would put a row on screen naming nothing in the application.

            So the call is ignored rather than obeyed, and SwingTree says so in the log instead
            of leaving somebody to wonder why a builder method did nothing.
        """
        given : 'A forest, and a spy on the log.'
            var log = LogSpy.attach()
            var projects = Var.of(Tuple.of(FsNode, new Dir("app", "myapp", Tuple.of(FsNode, new Doc("main", "App.java")))))

        when : 'We ask for a root row anyway.'
            var tree = UI.trees(projects, shape()).withRootVisible(true).get(JTree)

        then : 'The tree still draws only what the application has.'
            !UI.runAndGet({ tree.isRootVisible() })
            visibleRows(tree) == ["myapp"]
        and : 'And the log names the method that was ignored.'
            log.warnings().any { it.contains("withRootVisible(boolean)") }

        cleanup :
            log.detach()
    }

    def 'The same configuration resolves a forest path back to its nodes.'()
    {
        reportInfo """
            A `TreeConf` describes node types, not the shape of the top level, so one
            configuration value binds a forest and answers questions about a single rooted
            tree alike. What differs is only what a path is relative to, which is why
            `nodeAt(..)` and `nodesAlong(..)` have an overload taking the top level tuple.

            This is how a detail pane learns what is selected: the selection says *where*, and
            the configuration turns that into *what*.
        """
        given : 'A configuration kept as an ordinary value, and a forest to ask about.'
            var shape = TreeConf.of(FsNode)
                            .nodesOf(Dir, { it.children({ Dir d -> d.entries() }).text({ Dir d -> d.name() }) })
                            .nodesOf(Doc, { it.text({ Doc d -> d.name() }) })
            var projects = Var.of(Tuple.of(FsNode,
                                new Dir("app", "myapp", Tuple.of(FsNode, new Dir("src", "src", Tuple.of(FsNode, new Doc("main", "App.java"))))),
                                new Dir("doc", "notes", Tuple.of(FsNode))
                           ))
            var selected = Var.of(Tuple.of(String))

        and : 'A derived property answering "which node is selected", built out of the two.'
            var selectedNode = Viewable.of(String, projects, selected,
                                    { roots, path -> shape.nodeAt(roots, path).map({ it.name() }).orElse("") })

        when : 'A path is selected.'
            UI.runNow({ selected.set(Tuple.of(String, "app", "src", "main")) })

        then : 'The derived property names the node it leads to.'
            selectedNode.get() == "App.java"
        and : 'And the whole trail is what a breadcrumb is made of, starting at the top level node.'
            shape.nodesAlong(projects.get(), selected.get()).toList()*.name() == ["myapp", "src", "App.java"]

        when : 'The selection points somewhere the forest no longer goes.'
            UI.runNow({ selected.set(Tuple.of(String, "app", "gone")) })

        then : 'Both answer emptily rather than returning half a trail.'
            !shape.nodeAt(projects.get(), selected.get()).isPresent()
            shape.nodesAlong(projects.get(), selected.get()).isEmpty()
    }

    def 'Selecting several nodes at once works across the whole forest.'()
    {
        reportInfo """
            Which overload you call is what puts the tree into multiple selection mode, here
            just as for a single rooted tree. The one thing worth noticing is that the paths
            may name nodes in different projects, which is exactly what a container root would
            have made harder to express rather than easier.
        """
        given : 'A forest and a property holding one path per selected node.'
            var projects = Var.of(Tuple.of(FsNode,
                                new Dir("app", "myapp", Tuple.of(FsNode, new Doc("main", "App.java"))),
                                new Dir("doc", "notes", Tuple.of(FsNode, new Doc("readme", "README.md")))
                           ))
            var selected = Var.of(Tuple.of(Tuple.classTyped(String)))
            var tree = UI.trees(projects, shape()).withSelectionPaths(selected).withInitialExpansionDepth(2).get(JTree)

        when : 'We select one document out of each project.'
            UI.runNow({ selected.set(Tuple.of(Tuple.classTyped(String), [
                            Tuple.of(String, "app", "main"),
                            Tuple.of(String, "doc", "readme")
                       ])) })

        then : 'Both rows are selected.'
            selectedRows(tree) == ["App.java", "README.md"]

        when : 'And the user picks a single row instead.'
            UI.runNow({ tree.setSelectionRow(0) })

        then : 'The property holds exactly one path again.'
            selected.get().toList() == [Tuple.of(String, "app")]
    }

    def 'A selection action speaks in your own nodes, and its trail starts at the top level.'()
    {
        reportInfo """
            `onSelection(..)` is the same delegate a single rooted tree hands you. The only
            difference a forest makes is where the trail begins: there is no root node to put
            in front of it, because there is no root node.

            The container the `JTree` holds never appears in it. That is deliberate — it is
            SwingTree's, not yours, and nothing in your own types could describe it.
        """
        given : 'A forest, and somewhere to record what the action was told.'
            var projects = Var.of(Tuple.of(FsNode,
                                new Dir("app", "myapp", Tuple.of(FsNode, new Dir("src", "src", Tuple.of(FsNode, new Doc("main", "App.java"))))),
                                new Dir("doc", "notes", Tuple.of(FsNode))
                           ))
            var trail = []
            var leadPath = null
            var tree =
                    UI.trees(projects, shape())
                    .onSelection({ it -> trail = it.pathToLead().toList()*.name(); leadPath = it.leadPath() })
                    .withInitialExpansionDepth(3)
                    .get(JTree)

        when : 'The user selects the document.'
            UI.runNow({ tree.setSelectionRow(2) })

        then : 'The trail is made of your own node values, top level node first.'
            trail == ["myapp", "src", "App.java"]
        and : 'And the position it reports is the same path a bound property would receive.'
            leadPath == Tuple.of(String, "app", "src", "main")
    }

    def 'A read only forest may be browsed but not edited.'()
    {
        reportInfo """
            The rule is the one `UI.tree(..)` follows, and it is decided by the *declared* type
            of what you pass rather than by what the reference happens to point at. A view
            model exposing its `Var<Tuple<..>>` as a `Val` therefore hands out a genuinely read
            only forest.
        """
        given : 'The same two projects and the same renaming rules for both forests.'
            var roots = Tuple.of(FsNode, new Dir("app", "myapp", Tuple.of(FsNode, new Doc("main", "App.java"))))

        when : 'We bind one forest to a mutable property and one to a read only one.'
            var editable = UI.trees(Var.of(roots), shape()).get(JTree)
            var readOnly = UI.trees(Val.of(roots), shape()).get(JTree)

        then : 'Both draw exactly the same thing.'
            visibleRows(editable) == ["myapp"]
            visibleRows(readOnly) == ["myapp"]
        and : 'But only the one bound to a mutable property lets the user start editing.'
            UI.runAndGet({ editable.isEditable() })
            !UI.runAndGet({ readOnly.isEditable() })
    }

    def 'Two top level nodes sharing an id are reported rather than silently merged.'()
    {
        reportInfo """
            An id has to be unique among siblings, and the top level nodes of a forest are
            siblings like any others — they simply have SwingTree's own container as their
            parent rather than one of yours.

            Two of them sharing an id are one position, so one is drawn twice and the other
            never, and SwingTree names the id rather than leaving somebody to work it out.
        """
        given : 'Two projects which were given the same id.'
            var log = LogSpy.attach()
            var projects = Var.of(Tuple.of(FsNode,
                                new Dir("same", "first", Tuple.of(FsNode,  new Doc("a", "one.txt"))),
                                new Dir("same", "second", Tuple.of(FsNode, new Doc("b", "two.txt")))
                           ))

        when : 'We build a forest out of them and ask what it shows.'
            var tree = UI.trees(projects, shape()).get(JTree)
            var rows = visibleRows(tree)

        then : 'SwingTree said what went wrong, naming the id the two share.'
            log.warnings().any { it.contains("share the id 'same'") }
        and : 'And the two really are one position: two rows showing one and the same project.'
            rows.size() == 2
            rows.toSet().size() == 1

        cleanup :
            log.detach()
    }
}
