package swingtree;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sprouts.Action;
import sprouts.From;
import sprouts.Tuple;
import sprouts.Val;
import sprouts.Var;
import swingtree.api.Configurator;
import swingtree.api.IconDeclaration;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.Objects;

/**
 *  A SwingTree builder node designed for configuring {@link JTree} instances.
 *  <p>
 *  The recommended way to get one is {@link UI#tree(Var, Configurator)}, which binds the
 *  tree to a single property holding a deeply immutable, nested data structure and a
 *  {@link TreeConf} saying which parts of it to zoom into:
 *  <pre>{@code
 *  UI.tree(fileSystem, conf -> conf
 *      .nodesOf(Dir.class, dir -> dir
 *          .children(Dir::entries, Dir::withEntries)
 *          .text(Dir::name, Dir::withName)
 *      )
 *      .nodesOf(Doc.class, doc -> doc.text(Doc::name))
 *  )
 *  .withSelection(selectedNode);
 *  }</pre>
 *  Where the data has no single root but several top level nodes,
 *  {@link UI#trees(Var, Configurator)} binds a {@code Var<Tuple<N>>} of them instead. The
 *  builder returned is this same one, and everything below configures both forms alike.
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 *
 * @param <I> The identity type of the nodes, which selection paths are made of.
 * @param <N> The common type of the nodes of the tree managed by this builder.
 * @param <T> The type of the {@link JTree} instance which will be managed by this builder.
 */
public final class UIForTree<I, N, T extends JTree> extends UIForAnySwing<UIForTree<I, N, T>, T>
{
    private static final Logger log = LoggerFactory.getLogger(UIForTree.class);

    private final BuilderState<T>                    _state;
    private final @Nullable PropertyTreeModel<I, N, ?> _model;

    UIForTree( BuilderState<T> state, @Nullable PropertyTreeModel<I, N, ?> model ) {
        _state = Objects.requireNonNull(state);
        _model = model;
    }

    @Override
    protected BuilderState<T> _state() {
        return _state;
    }

    @Override
    protected UIForTree<I, N, T> _newBuilderWithState( BuilderState<T> newState ) {
        return new UIForTree<>(newState, _model);
    }

    /**
     *  Installs a plain Swing {@link TreeModel}, which is the escape hatch for a tree whose
     *  contents genuinely do not live in a property, or which already has a model you want
     *  to keep. A tree installed like this takes no part in any of the property binding this
     *  builder otherwise offers.
     *  <p>
     *  Installing one on a tree built by {@code UI.tree(rootProperty, ..)} replaces the bound
     *  model, which leaves the binding with nothing to drive; SwingTree says so in the log
     *  rather than leaving you to work out why the tree stopped following the property. Build
     *  the tree with {@link UI#of(JTree)} instead when the contents are not a property.
     *
     * @param model The {@link TreeModel} to install.
     * @return This builder node, to allow for method chaining.
     */
    public final UIForTree<I, N, T> withModel( TreeModel model ) {
        Objects.requireNonNull(model, "model");
        PropertyTreeModel<I, N, ?> bound = _model;
        return _with( thisComponent -> {
                    if ( bound != null ) {
                        log.warn(SwingTree.get().logMarker(),
                            "Replacing the bound model of a tree with a plain '{}'. The tree will no " +
                            "longer follow the property it was built from. Use 'UI.of(someTree)' if " +
                            "the contents of this tree are not supposed to live in a property.",
                            model.getClass().getSimpleName());
                        thisComponent.setCellRenderer(new DefaultTreeCellRenderer());
                        thisComponent.setEditable(false);
                    }
                    thisComponent.setModel(model);
                })
                ._this();
    }

    /**
     *  Binds the selected position of the tree to a mutable property holding a
     *  <b>path of ids</b>, which keeps the two in sync in both directions: selecting a node
     *  writes the path leading to it into the property, and assigning a path selects the
     *  node it names, opening every branch above it and scrolling it into view.
     *  <pre>{@code
     *  Var<Tuple<String>> selectedPath = vm.zoomTo(Move::selectedPath, Move::withSelectedPath);
     *
     *  UI.tree(move, conf -> ..).withSelection(selectedPath);
     *  //  nothing selected   ->  Tuple.of(String.class)
     *  //  the root           ->  ["move"]
     *  //  the kettle         ->  ["move", "kitchen", "appliances", "kettle"]
     *  }</pre>
     *  <b>A selection is a position, and only a path names a position.</b> A node value
     *  cannot: the same value may sit in several places at once, and a tree only requires
     *  its ids to be unique <i>among siblings</i>, so two folders may each hold a
     *  {@code notes.txt}. A path of ids has nothing to be ambiguous about, needs no search
     *  to resolve, and carries no data which could contradict the tree.
     *  <p>
     *  The empty tuple means nothing is selected, so there is no null to handle. To get at
     *  the node a path names — for a detail view, say — ask the configuration:
     *  {@link TreeConf#nodeAt(Object, Tuple)} and {@link TreeConf#nodesAlong(Object, Tuple)}.
     *  <p>
     *  On a tree bound through {@link UI#trees(Var, Configurator)} the first id names a top
     *  level node rather than a root, so its paths are one element shorter and no path at all
     *  names the forest. Resolve them with {@link TreeConf#nodeAt(Tuple, Tuple)}.
     *  <p>
     *  Binding a single path this way also puts the tree into single selection mode. Use
     *  {@link #withSelectionPaths(Var)} for a tree the user may select several nodes in.
     *
     * @param selection A property holding the ids leading to the selected node.
     * @return This builder node, to allow for method chaining.
     */
    public final UIForTree<I, N, T> withSelection( Var<Tuple<I>> selection ) {
        Objects.requireNonNull(selection, "selection");
        PropertyTreeModel<I, N, ?> model = _requireBoundModel("withSelection(Var)");
        if ( model == null )
            return _this();
        return _with( thisComponent -> {
                    thisComponent.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
                    Runnable writeBack = () -> {
                        Tuple<I> path = model.idTupleOf(
                                            thisComponent.getSelectionPath(),
                                            _idTypeOf(selection, model)
                                        );
                        _runInApp(() -> selection.set(From.VIEW, path));
                    };
                    thisComponent.addTreeSelectionListener( event -> {
                        if ( model.isRestructuring() )
                            return; // The tree is mid rebuild, so its selection means nothing yet.
                        writeBack.run();
                    });
                    model.onAfterRestructure(writeBack);
                })
                ._withOnShow( selection, (thisComponent, path) -> {
                    _selectPaths(thisComponent, model, _asPaths(path));
                })
                ._with( thisComponent -> {
                    Tuple<I> initial = selection.orElseNull();
                    if ( initial != null && !initial.isEmpty() )
                        _selectPaths(thisComponent, model, _asPaths(initial));
                })
                ._this();
    }

    /**
     *  Binds the selected position of the tree to a read only property holding a path of
     *  ids, so that the tree follows the property but a selection made by the user is not
     *  written back. See {@link #withSelection(Var)} for why a selection is a path.
     *
     * @param selection A read only property holding the ids leading to the selected node.
     * @return This builder node, to allow for method chaining.
     */
    public final UIForTree<I, N, T> withSelection( Val<Tuple<I>> selection ) {
        Objects.requireNonNull(selection, "selection");
        PropertyTreeModel<I, N, ?> model = _requireBoundModel("withSelection(Val)");
        if ( model == null )
            return _this();
        return _with( thisComponent -> {
                    thisComponent.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
                })
                ._withOnShow( selection, (thisComponent, path) -> {
                    _selectPaths(thisComponent, model, _asPaths(path));
                })
                ._with( thisComponent -> {
                    Tuple<I> initial = selection.orElseNull();
                    if ( initial != null && !initial.isEmpty() )
                        _selectPaths(thisComponent, model, _asPaths(initial));
                })
                ._this();
    }

    /**
     *  Binds every selected position of the tree to a mutable property holding one path of
     *  ids per selected node, which also puts the tree into a mode where the user may
     *  select any number of nodes. The two stay in sync in both directions, just like
     *  {@link #withSelection(Var)}.
     *
     * @param selection A property holding one path of ids per selected node.
     * @return This builder node, to allow for method chaining.
     */
    public final UIForTree<I, N, T> withSelectionPaths( Var<Tuple<Tuple<I>>> selection ) {
        Objects.requireNonNull(selection, "selection");
        PropertyTreeModel<I, N, ?> model = _requireBoundModel("withSelectionPaths(Var)");
        if ( model == null )
            return _this();
        return _with( thisComponent -> {
                    thisComponent.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
                    Runnable writeBack = () -> {
                        Class<I> idType = _idTypeOfNested(selection, model);
                        TreePath[] paths = thisComponent.getSelectionPaths();
                        List<Tuple<I>> asIds = new ArrayList<>();
                        if ( paths != null )
                            for ( TreePath path : paths )
                                asIds.add(model.idTupleOf(path, idType));
                        Tuple<Tuple<I>> selected = Tuple.of(Tuple.classTyped(idType), asIds);
                        _runInApp(() -> selection.set(From.VIEW, selected));
                    };
                    thisComponent.addTreeSelectionListener( event -> {
                        if ( model.isRestructuring() )
                            return; // The tree is mid rebuild, so its selection means nothing yet.
                        writeBack.run();
                    });
                    model.onAfterRestructure(writeBack);
                })
                ._withOnShow( selection, (thisComponent, paths) -> {
                    _selectPaths(thisComponent, model, _asPathList(paths));
                })
                ._with( thisComponent -> {
                    Tuple<Tuple<I>> initial = selection.orElseNull();
                    if ( initial != null && !initial.isEmpty() )
                        _selectPaths(thisComponent, model, _asPathList(initial));
                })
                ._this();
    }

    /**
     *  Registers an action which is invoked whenever the selection of the tree changes.
     *  The action receives a {@link TreeSelectionDelegate} speaking in your own node values,
     *  so there is never a reason to reach into the tree's own path objects.
     *  <pre>{@code
     *  UI.tree(fileSystem, conf -> ..)
     *  .onSelection( it -> it.lead().ifPresent(node -> open(node)) );
     *  }</pre>
     *  For the common case of mirroring the selection into a view model, prefer
     *  {@link #withSelection(Var)}, which needs no action at all.
     *
     * @param action The action to invoke when the selection changes.
     * @return This builder node, to allow for method chaining.
     */
    public final UIForTree<I, N, T> onSelection( Action<TreeSelectionDelegate<I, N>> action ) {
        Objects.requireNonNull(action, "action");
        PropertyTreeModel<I, N, ?> model = _requireBoundModel("onSelection(..)");
        if ( model == null )
            return _this();
        return _with( thisComponent -> {
                    Runnable notify = () -> {
                        // Captured on the UI thread, delivered on the application thread:
                        TreeSelectionDelegate<I, N> delegate = new TreeSelectionDelegate<>(
                            thisComponent, model,
                            thisComponent.getSelectionPath(),
                            thisComponent.getSelectionPaths()
                        );
                        _runInApp(() -> {
                            try {
                                action.accept(delegate);
                            } catch (Exception e) {
                                log.error(SwingTree.get().logMarker(),
                                        "Error occurred while processing a tree selection event.", e);
                            }
                        });
                    };
                    thisComponent.addTreeSelectionListener( event -> {
                        if ( model.isRestructuring() )
                            return; // The tree is mid rebuild, so its selection means nothing yet.
                        notify.run();
                    });
                    model.onAfterRestructure(notify);
                })
                ._this();
    }

    /**
     *  Decides whether the root node of the bound structure is drawn as a row of its own.
     *  Hiding it also turns on the handles of the level below, so that what is now the first
     *  visible level can still be opened.
     *  <p>
     *  A tree bound through {@link UI#trees(Var, Configurator)} has no root node, so there is
     *  nothing here to show: showing it is ignored and reported in the log, and hiding it was
     *  already the case.
     *
     * @param rootVisible True to show the root node.
     * @return This builder node, to allow for method chaining.
     */
    public final UIForTree<I, N, T> withRootVisible( boolean rootVisible ) {
        return _with( thisComponent -> {
                    _applyRootVisible(thisComponent, rootVisible, "withRootVisible(boolean)");
                    if ( !rootVisible )
                        thisComponent.setShowsRootHandles(true);
                })
                ._this();
    }

    /**
     *  Binds the visibility of the root node to a property, so that the tree can fold its
     *  root away and back in response to application state. A tree bound through
     *  {@link UI#trees(Var, Configurator)} has no root node, so see
     *  {@link #withRootVisible(boolean)} for what happens there.
     *
     * @param rootVisible A property telling whether the root node should be shown.
     * @return This builder node, to allow for method chaining.
     */
    public final UIForTree<I, N, T> isRootVisibleIf( Val<Boolean> rootVisible ) {
        Objects.requireNonNull(rootVisible, "rootVisible");
        return _withOnShow( rootVisible, (thisComponent, visible) -> {
                    _applyRootVisible(thisComponent, Boolean.TRUE.equals(visible), "isRootVisibleIf(Val)");
                })
                ._with( thisComponent -> {
                    _applyRootVisible(
                        thisComponent, Boolean.TRUE.equals(rootVisible.orElseNull()), "isRootVisibleIf(Val)"
                    );
                })
                ._this();
    }

    private void _applyRootVisible( JTree tree, boolean rootVisible, String method ) {
        PropertyTreeModel<I, N, ?> model = _model;
        if ( rootVisible && model != null && model.isForest() ) {
            log.warn(SwingTree.get().logMarker(),
                "Ignoring '{}' on a tree bound to a tuple of top level nodes, which has no root " +
                "node to show. Bind it through 'UI.tree(rootProperty, ..)' if it should have one.",
                method);
            return;
        }
        tree.setRootVisible(rootVisible);
    }

    /**
     *  Decides whether the handles which expand and collapse a branch are drawn next to the
     *  top level nodes as well. Hiding the root turns these on, because they are then the only
     *  handles there are — and turning them off again on a tree with no visible root leaves
     *  the user nothing to click, so only do that where expansion is driven from code.
     *
     * @param showsRootHandles True to draw handles next to the top level nodes.
     * @return This builder node, to allow for method chaining.
     */
    public final UIForTree<I, N, T> withRootHandlesVisible( boolean showsRootHandles ) {
        return _with( thisComponent -> {
                    thisComponent.setShowsRootHandles(showsRootHandles);
                })
                ._this();
    }

    /**
     *  Sets the height of every row of the tree, in the same scale independent developer
     *  pixels the rest of SwingTree speaks in. Pass {@code 0} to let each row take the
     *  height its own cell view asks for.
     *
     * @param rowHeight The height of a row.
     * @return This builder node, to allow for method chaining.
     */
    public final UIForTree<I, N, T> withRowHeight( int rowHeight ) {
        return _with( thisComponent -> {
                    thisComponent.setRowHeight(rowHeight <= 0 ? rowHeight : UI.scale(rowHeight));
                })
                ._this();
    }

    /**
     *  Expands every branch down to the given depth, once, at the point this builder method
     *  runs, where a depth of {@code 1} opens the topmost visible level, {@code 2} the level
     *  below that as well, and so on. This is a convenience for the initial view only:
     *  expansion the user performs afterwards is untouched by it, and so is everything the
     *  bound property grows later.
     *  <p>
     *  Depth is counted from what is on screen, so a root hidden with
     *  {@link #withRootVisible(boolean)} — and the absent root of a forest — does not count
     *  as a level. Call this <i>after</i> hiding the root, so that it knows.
     *
     * @param depth How many visible levels to expand.
     * @return This builder node, to allow for method chaining.
     */
    public final UIForTree<I, N, T> withInitialExpansionDepth( int depth ) {
        return _with( thisComponent -> {
                    _expandToDepth(thisComponent, depth);
                })
                ._this();
    }

    /**
     *  Use this to build a cell renderer covering several node types at once, through the
     *  same fluent API the list, combo box and table components expose:
     *  <pre>{@code
     *  UI.tree(fileSystem, conf -> ..)
     *  .withCells( it -> it
     *      .when(Dir.class).asText( cell -> cell.entry().map(Dir::name).orElse("") )
     *      .when(Doc.class).asComponent( cell -> myFancyRow(cell) )
     *  );
     *  }</pre>
     *  Note that for a label and an icon there is no need to come here at all: declare them
     *  as part of the node's own rule with {@link TreeNodeConf#text(java.util.function.Function)}
     *  and {@link TreeNodeConf#icon(java.util.function.Function)}. Reach for this method when
     *  a node needs a view a label cannot give it.
     *  <p>
     *  The two mix freely: a node type this builder says nothing about keeps the label, icon
     *  and tool tip declared in its own rule, so covering one type here does not oblige you
     *  to cover the rest.
     *
     * @param renderBuilder Configures the cell renderer.
     * @param <V> The type of the node values being rendered.
     * @return This builder node, to allow for method chaining.
     */
    public final <V extends N> UIForTree<I, N, T> withCells(
        Configurator<CellBuilder<T, V>> renderBuilder
    ) {
        Objects.requireNonNull(renderBuilder, "renderBuilder");
        CellBuilder builder = CellBuilder.forTree(Object.class);
        try {
            builder = renderBuilder.configure(builder);
        } catch (Exception e) {
            log.error(SwingTree.get().logMarker(), "Error while building the cells of a tree.", e);
            return _this();
        }
        Objects.requireNonNull(builder);
        PropertyTreeModel<I, N, ?> model = _model;
        /*
            The tree's own renderer goes in as the fallback, so a node type no 'when(..)'
            clause covers keeps the rules declared for it instead of falling to 'toString()'.
        */
        TreeCellRenderer renderer = builder.getForTree(
                                        model == null ? null : new DefaultBoundRenderer<>(model)
                                    );
        return withCellRenderer(renderer);
    }

    /**
     *  Configures a single cell view used for every node of the tree, which is the shorter
     *  form of {@link #withCells(Configurator)} when there is no need to distinguish between
     *  node types.
     *
     * @param cellConfigurator Configures the cell view.
     * @param <V> The type of the node values being rendered.
     * @return This builder node, to allow for method chaining.
     */
    public final <V extends N> UIForTree<I, N, T> withCell(
        Configurator<CellConf<T, V>> cellConfigurator
    ) {
        Objects.requireNonNull(cellConfigurator, "cellConfigurator");
        return withCells( it -> it.when((Class) Object.class).as(cellConfigurator) );
    }

    /**
     *  Installs a plain Swing {@link TreeCellRenderer}. A bound tree keeps internal handles
     *  inside its paths, so the renderer is wrapped in one which unwraps them first: what
     *  reaches your renderer is always your own node value.
     *  <p>
     *  A renderer installed this way answers for <i>every</i> node, so the {@code text(..)},
     *  {@code icon(..)} and {@code toolTip(..)} rules declared per node type no longer apply.
     *  Use {@link #withCells(Configurator)} to cover only some node types and leave the rest
     *  to their own rules.
     *
     * @param renderer The renderer to paint the nodes with.
     * @return This builder node, to allow for method chaining.
     */
    public final UIForTree<I, N, T> withCellRenderer( TreeCellRenderer renderer ) {
        Objects.requireNonNull(renderer, "renderer");
        PropertyTreeModel<I, N, ?> model = _model;
        return _with( thisComponent -> {
                    thisComponent.setCellRenderer(
                        model == null ? renderer : new UnwrappingRenderer(model, renderer)
                    );
                })
                ._this();
    }

    /**
     *  Installs a plain Swing {@link TreeCellEditor}, replacing the in place rename the
     *  {@link TreeNodeConf#text(java.util.function.Function, java.util.function.BiFunction)}
     *  rule installs by itself.
     *
     * @param editor The editor to edit the nodes with.
     * @return This builder node, to allow for method chaining.
     */
    public final UIForTree<I, N, T> withCellEditor( TreeCellEditor editor ) {
        Objects.requireNonNull(editor, "editor");
        return _with( thisComponent -> {
                    thisComponent.setCellEditor(editor);
                })
                ._this();
    }

    // -------------------------------------------------------------- Internals

    static <I, N, T extends JTree> UIForTree<I, N, T> _bind(
        BuilderState<T> state, Val<N> root, TreeConf<I, N> conf, boolean writable
    ) {
        return _bindTo(state, root, TreeRoots.single(conf.nodeType()), conf, writable);
    }

    static <I, N, T extends JTree> UIForTree<I, N, T> _bindForest(
        BuilderState<T> state, Val<Tuple<N>> roots, TreeConf<I, N> conf, boolean writable
    ) {
        return _bindTo(state, roots, TreeRoots.forest(conf.nodeType()), conf, writable);
    }

    private static <I, N, R, T extends JTree> UIForTree<I, N, T> _bindTo(
        BuilderState<T> state, Val<R> bound, TreeRoots<N, R> roots, TreeConf<I, N> conf, boolean writable
    ) {
        PropertyTreeModel<I, N, R> model = new PropertyTreeModel<>(
            bound, roots, conf, writable, state.eventProcessor()
        );
        UIForTree<I, N, T> builder = new UIForTree<>(state, model);
        return builder
                ._with( thisComponent -> {
                    model.attachTo(thisComponent);
                    thisComponent.setModel(model);
                    thisComponent.setCellRenderer(new DefaultBoundRenderer<>(model));
                    ToolTipManager.sharedInstance().registerComponent(thisComponent);
                    if ( model.isForest() ) {
                        // Nothing sits above the top level, and its handles are the only ones there are:
                        thisComponent.setRootVisible(false);
                        thisComponent.setShowsRootHandles(true);
                    }
                    if ( model.isWritable() && conf.hasRenamableRule() ) {
                        thisComponent.setEditable(true);
                        thisComponent.setCellEditor(new BoundCellEditor<>(
                            thisComponent, (DefaultTreeCellRenderer) thisComponent.getCellRenderer(), model
                        ));
                    }
                })
                ._withOnShow( bound, (thisComponent, newValue) -> {
                    model.applyNewValue(newValue);
                })
                ._this();
    }

    private @Nullable PropertyTreeModel<I, N, ?> _requireBoundModel( String method ) {
        PropertyTreeModel<I, N, ?> model = _model;
        if ( model == null )
            log.warn(SwingTree.get().logMarker(),
                "Ignoring '{}' on a tree which is not bound to a property. Create the tree through " +
                "'UI.tree(rootProperty, conf -> ..)' to be able to use it.", method);
        return model;
    }

    /**
     *  Taken from the value the property already holds, because a {@link Tuple} compares its
     *  element type as part of its equality: a tuple of the wrong type would never compare
     *  equal to what the property holds, so every write would echo back forever.
     */
    private Class<I> _idTypeOf( Val<Tuple<I>> selection, PropertyTreeModel<I, N, ?> model ) {
        Tuple<I> current = selection.orElseNull();
        return ( current == null ? model.idType() : current.type() );
    }

    private Class<I> _idTypeOfNested( Val<Tuple<Tuple<I>>> selection, PropertyTreeModel<I, N, ?> model ) {
        Tuple<Tuple<I>> current = selection.orElseNull();
        if ( current != null && !current.isEmpty() )
            return current.get(0).type();
        return model.idType();
    }

    private List<Tuple<I>> _asPaths( @Nullable Tuple<I> path ) {
        List<Tuple<I>> paths = new ArrayList<>(1);
        if ( path != null && !path.isEmpty() )
            paths.add(path);
        return paths;
    }

    private List<Tuple<I>> _asPathList( @Nullable Tuple<Tuple<I>> paths ) {
        List<Tuple<I>> result = new ArrayList<>();
        if ( paths != null )
            for ( int i = 0; i < paths.size(); i++ ) {
                Tuple<I> path = paths.get(i);
                if ( path != null && !path.isEmpty() )
                    result.add(path);
            }
        return result;
    }

    /**
     *  A path which no longer names anything simply takes no part in the selection,
     *  rather than clearing it.
     */
    private void _selectPaths( JTree tree, PropertyTreeModel<I, N, ?> model, List<Tuple<I>> idPaths ) {
        if ( idPaths.isEmpty() ) {
            tree.clearSelection();
            return;
        }
        List<TreePath> resolved = new ArrayList<>(idPaths.size());
        for ( Tuple<I> idPath : idPaths ) {
            TreePath path = model.pathForIdTuple(idPath);
            if ( path != null )
                resolved.add(path);
        }
        if ( resolved.isEmpty() ) {
            tree.clearSelection();
            return;
        }
        for ( TreePath path : resolved )
            tree.makeVisible(path);
        tree.setSelectionPaths(resolved.toArray(new TreePath[0]));
        tree.scrollPathToVisible(resolved.get(0));
    }

    private static void _expandToDepth( JTree tree, int depth ) {
        if ( depth <= 0 )
            return;
        // A root nobody can see is no level of its own, so it does not count towards the depth:
        int invisibleLevels = ( tree.isRootVisible() ? 0 : 1 );
        // Expanding a row adds rows below it, so the count is re-read on every step:
        for ( int row = 0; row < tree.getRowCount(); row++ ) {
            TreePath path = tree.getPathForRow(row);
            if ( path != null && path.getPathCount() - invisibleLevels <= depth )
                tree.expandRow(row);
        }
    }

    /** Unwraps the node handle and renders the node by the rules declared for its type. */
    private static final class DefaultBoundRenderer<I, N> extends DefaultTreeCellRenderer
    {
        private final PropertyTreeModel<I, N, ?> _model;

        DefaultBoundRenderer( PropertyTreeModel<I, N, ?> model ) {
            _model = model;
        }

        @Override
        public Component getTreeCellRendererComponent(
            JTree tree, Object value, boolean selected,
            boolean expanded, boolean leaf, int row, boolean hasFocus
        ) {
            Object node = _model.valueOf(value);
            TreeNodeConf<N, ?> rule = _model.ruleOf(value);
            String text = null;
            IconDeclaration iconDeclaration = null;
            String toolTip = null;
            if ( rule != null && node != null ) {
                try {
                    text            = rule.textOf(node);
                    iconDeclaration = rule.iconOf(node);
                    toolTip         = rule.toolTipOf(node);
                } catch (Exception e) {
                    log.error(SwingTree.get().logMarker(),
                            "Failed to render a tree node of type '{}'.",
                            node.getClass().getSimpleName(), e);
                }
            }
            if ( text == null )
                text = String.valueOf(node);
            Component view = super.getTreeCellRendererComponent(
                tree, text, selected, expanded, leaf, row, hasFocus
            );
            // After the super call, which picks a leaf/open/closed icon of its own every time:
            if ( iconDeclaration != null ) {
                Icon icon = iconDeclaration.find().orElse(null);
                if ( icon != null )
                    setIcon(icon);
            }
            setToolTipText(toolTip);
            return view;
        }
    }

    /** Keeps a user supplied renderer from ever seeing the internal node handles. */
    private static final class UnwrappingRenderer implements TreeCellRenderer
    {
        private final PropertyTreeModel<?, ?, ?> _model;
        private final TreeCellRenderer       _delegate;

        UnwrappingRenderer( PropertyTreeModel<?, ?, ?> model, TreeCellRenderer delegate ) {
            _model    = model;
            _delegate = delegate;
        }

        @Override
        public Component getTreeCellRendererComponent(
            JTree tree, Object value, boolean selected,
            boolean expanded, boolean leaf, int row, boolean hasFocus
        ) {
            return _delegate.getTreeCellRendererComponent(
                tree, _model.valueOf(value), selected, expanded, leaf, row, hasFocus
            );
        }
    }

    /**
     *  Differs from the plain Swing editor in two places: it opens showing what the node's
     *  {@code text(..)} rule produces rather than its {@code toString()}, and it refuses to
     *  open on a node the user pointed at whose type declared no wither.
     */
    private static final class BoundCellEditor<I, N> extends DefaultTreeCellEditor
    {
        private final PropertyTreeModel<I, N, ?> _model;

        BoundCellEditor( JTree tree, DefaultTreeCellRenderer renderer, PropertyTreeModel<I, N, ?> model ) {
            super(tree, renderer);
            _model = model;
        }

        @Override
        public Component getTreeCellEditorComponent(
            JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row
        ) {
            Object node = _model.valueOf(value);
            TreeNodeConf<N, ?> rule = _model.ruleOf(value);
            String text = ( rule == null || node == null ? null : rule.textOf(node) );
            return super.getTreeCellEditorComponent(
                tree, text == null ? String.valueOf(node) : text, isSelected, expanded, leaf, row
            );
        }

        @Override
        public boolean isCellEditable( EventObject event ) {
            if ( !super.isCellEditable(event) )
                return false;
            TreePath path = _pathTargetedBy(event);
            if ( path == null )
                return true;
            TreeNodeConf<N, ?> rule = _model.ruleOf(path.getLastPathComponent());
            return rule != null && rule.isRenamable();
        }

        private @Nullable TreePath _pathTargetedBy( @Nullable EventObject event ) {
            if ( event instanceof MouseEvent && event.getSource() instanceof JTree ) {
                MouseEvent click = (MouseEvent) event;
                TreePath clicked = ((JTree) event.getSource())
                                        .getPathForLocation(click.getX(), click.getY());
                if ( clicked != null )
                    return clicked;
            }
            return lastPath;
        }
    }
}
