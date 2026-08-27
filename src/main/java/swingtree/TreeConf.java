package swingtree;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sprouts.Association;
import sprouts.HasId;
import sprouts.Pair;
import sprouts.Tuple;
import swingtree.api.Configurator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 *  Describes the shape of a tree: which node types have children, where those children
 *  live inside the node, how a node is labelled, and what identifies it.
 *  <p>
 *  A {@link javax.swing.JTree} in SwingTree is bound to a single property holding a
 *  deeply immutable, nested data structure. This class is how you tell the tree which
 *  parts of that structure to zoom into:
 *  <pre>{@code
 *  public sealed interface FsNode extends HasId<UUID> { String name(); }
 *  public record Dir( UUID id, String name, Tuple<FsNode> entries ) implements FsNode {}
 *  public record Doc( UUID id, String name, String body )           implements FsNode {}
 *
 *  UI.tree(fileSystem, conf -> conf
 *      .nodesOf(Dir.class, dir -> dir.children(Dir::entries).text(Dir::name))
 *      .nodesOf(Doc.class, doc -> doc.text(Doc::name))
 *  );
 *  }</pre>
 *  Each {@link #nodesOf(Class, Configurator)} block covers one node type and reads like one
 *  {@code case} of the {@code switch} you would otherwise write by hand, which is why this
 *  API pairs so naturally with a sealed interface based sum type.
 *  <p>
 *  <b>On identity.</b> A tree of value objects has a problem a list of them does not:
 *  {@link javax.swing.JTree} keys expansion and selection on {@link javax.swing.tree.TreePath},
 *  which compares nodes with {@link Object#equals(Object)}. Records compare by content, so
 *  editing one leaf would invalidate every path in the tree at once. SwingTree therefore
 *  identifies a node by its <i>path of ids</i>. Nodes implementing {@link HasId} supply that
 *  id for free; for types you do not own, declare one with {@link #idOf(Function)}. Ids only
 *  need to be unique <i>among siblings</i>, because the path disambiguates the rest.
 *  <p>
 *  A configuration describes node types, not the shape of the top level, so the very same
 *  value binds a single rooted tree through {@link UI#tree(sprouts.Var, TreeConf)} and a
 *  forest of them through {@link UI#trees(sprouts.Var, TreeConf)}.
 *  <p>
 *  Instances of this class are immutable values, so every method returns a new
 *  instance instead of modifying the receiver.
 *
 * @param <I> The identity type of the nodes, which selection paths are made of.
 * @param <N> The common node type of the tree, typically a sealed interface.
 */
public final class TreeConf<I, N>
{
    private static final Logger log = LoggerFactory.getLogger(TreeConf.class);

    private final Class<N>                                    _nodeType;
    private final @Nullable Class<I>                          _idType;
    private final Association<Class<?>, TreeNodeConf<N, ?>>   _rules;
    private final @Nullable Function<N, I>                    _idGetter;
    private final boolean                                     _leafWhenEmpty;

    /*
        Caches whose content is fully determined by the immutable fields above. Concurrent
        because a lens resolves rules on the application thread while the tree paints rows
        on the Event Dispatch Thread.
    */
    private final Map<Class<?>, Object> _ruleMemo     = new ConcurrentHashMap<>();
    private final Set<Class<?>>         _warnedAbout  = ConcurrentHashMap.newKeySet();

    /**
     *  Declares the shape of a tree whose nodes carry their own identity through
     *  {@link HasId}, which is the usual case. The result is an ordinary immutable value:
     *  bind it to a tree with {@link UI#tree(sprouts.Var, TreeConf)}, and ask it questions
     *  about paths with {@link #nodeAt(Object, Tuple)} and {@link #nodesAlong(Object, Tuple)}.
     *  <pre>{@code
     *  TreeConf<String, Packed> shape = TreeConf.of(Packed.class)
     *          .nodesOf(Box.class,  it -> it.children(Box::contents).text(Box::label))
     *          .nodesOf(Item.class, it -> it.text(Item::label));
     *  }</pre>
     *
     * @param nodeType The common supertype of every node in the tree.
     * @param <I> The identity type of the nodes, taken from the {@link HasId} bound.
     * @param <N> The common node type of the tree.
     * @return A new, empty tree configuration.
     */
    public static <I, N extends HasId<I>> TreeConf<I, N> of( Class<N> nodeType ) {
        Objects.requireNonNull(nodeType, "nodeType");
        return _of(nodeType, null);
    }

    /**
     *  Declares the shape of a tree whose node types cannot implement {@link HasId}, by
     *  naming the identity type explicitly. Their identity is then declared with
     *  {@link #idOf(Function)}.
     *
     * @param nodeType The common supertype of every node in the tree.
     * @param idType The type of the node identities, which selection paths are made of.
     * @param <I> The identity type of the nodes.
     * @param <N> The common node type of the tree.
     * @return A new, empty tree configuration.
     */
    public static <I, N> TreeConf<I, N> of( Class<N> nodeType, Class<I> idType ) {
        Objects.requireNonNull(nodeType, "nodeType");
        Objects.requireNonNull(idType, "idType");
        return _of(nodeType, idType);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static <I, N> TreeConf<I, N> _of( Class<N> nodeType, @Nullable Class<I> idType ) {
        return new TreeConf<>(
            nodeType,
            idType,
            (Association) Association.betweenLinked(Class.class, TreeNodeConf.class),
            null,
            false
        );
    }

    private TreeConf(
        Class<N>                                  nodeType,
        @Nullable Class<I>                        idType,
        Association<Class<?>, TreeNodeConf<N, ?>> rules,
        @Nullable Function<N, I>                  idGetter,
        boolean                                   leafWhenEmpty
    ) {
        _nodeType      = Objects.requireNonNull(nodeType);
        _idType        = idType;
        _rules         = Objects.requireNonNull(rules);
        _idGetter      = idGetter;
        _leafWhenEmpty = leafWhenEmpty;
    }

    /**
     *  Declares how nodes of the given type behave: whether they have children and where,
     *  how they are labelled, and what icon they carry. Declaring a block for a type that
     *  already has one replaces the previous block.
     *  <pre>{@code
     *  UI.tree(fileSystem, conf -> conf
     *      .nodesOf(Dir.class, dir -> dir
     *          .children(Dir::entries, Dir::withEntries)
     *          .text(Dir::name, Dir::withName)
     *      )
     *  );
     *  }</pre>
     *  A node whose type matches no block at all is treated as a leaf labelled by its
     *  {@link Object#toString()}, and SwingTree logs a warning naming the type, because
     *  that is almost always a forgotten case rather than an intent.
     *  <p>
     *  Where several blocks match a node, the most specific one wins and is used <i>on its
     *  own</i>: a block is chosen, never merged with a more general one, in the same way one
     *  {@code case} of a {@code switch} does not continue into another. A block which wants
     *  the label of a catch-all block above it therefore has to declare that label too.
     *
     * @param type The concrete node type this block applies to.
     * @param conf Configures the behaviour of nodes of that type.
     * @param <B> The concrete node type.
     * @return An updated configuration.
     */
    public <B extends N> TreeConf<I, N> nodesOf( Class<B> type, Configurator<TreeNodeConf<N, B>> conf ) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(conf, "conf");
        TreeNodeConf<N, B> nodeConf = TreeNodeConf.of(type);
        try {
            nodeConf = conf.configure(nodeConf);
        } catch (Exception e) {
            log.error(SwingTree.get().logMarker(),
                    "Failed to configure tree nodes of type '{}'.", type.getSimpleName(), e);
            return this;
        }
        Objects.requireNonNull(nodeConf);
        return new TreeConf<>(_nodeType, _idType, _rules.put(type, nodeConf), _idGetter, _leafWhenEmpty);
    }

    /**
     *  Declares how <i>every</i> node behaves, which is the right thing when the tree is
     *  homogeneous and one rule covers all of it. It is exactly
     *  {@code nodesOf(theNodeType, conf)}, so a more specific block declared for a subtype
     *  wins over it — and, because the winning block is used on its own, replaces it rather
     *  than adding to it. A subtype which wants what is declared here has to restate it.
     *
     * @param conf Configures the behaviour of all nodes.
     * @return An updated configuration.
     */
    public TreeConf<I, N> nodesOf( Configurator<TreeNodeConf<N, N>> conf ) {
        return nodesOf(_nodeType, conf);
    }

    /**
     *  Declares what identifies a node, which is what lets expansion and selection survive
     *  an edit anywhere in the tree. Node types implementing {@link HasId} need no such
     *  declaration; use this one for types you do not own:
     *  <pre>{@code
     *  .idOf( node -> node instanceof Department ? ((Department) node).id() : node )
     *  }</pre>
     *  Ids only have to be unique among the children of one parent.
     *
     * @param id Produces the identity of a node.
     * @return An updated configuration.
     */
    public TreeConf<I, N> idOf( Function<N, I> id ) {
        Objects.requireNonNull(id, "id");
        return new TreeConf<>(_nodeType, _idType, _rules, id, _leafWhenEmpty);
    }

    /**
     *  Decides whether a branch that currently has no children should be drawn as a leaf.
     *  SwingTree defaults to {@code false}, meaning the presence of a {@code children(..)}
     *  rule alone makes a node a branch, so an empty folder still looks like a folder.
     *  Pass {@code true} for the plain {@link javax.swing.JTree} behaviour, where a node
     *  with zero children is a leaf.
     *
     * @param leafWhenEmpty True to draw childless branches as leaves.
     * @return An updated configuration.
     */
    public TreeConf<I, N> leafWhenEmpty( boolean leafWhenEmpty ) {
        return new TreeConf<>(_nodeType, _idType, _rules, _idGetter, leafWhenEmpty);
    }

    /**
     *  Resolves a selection path back to the node it names, which is what a view bound to
     *  a selection needs in order to show anything about what is selected.
     *  <p>
     *  A selection is a {@link Tuple} of ids leading down from the root, because that is
     *  the only thing which identifies a <i>position</i> in a tree. The node living at that
     *  position is a question about the tree, and this is how you ask it.
     *  <p>
     *  Declaring the shape as an ordinary value rather than inline is what makes that
     *  elegant, because the same value then serves twice: it builds the tree, and it answers
     *  the detail pane standing next to the tree.
     *  <pre>{@code
     *  private final Var<FsNode>        fileSystem   = vm.zoomTo(Workspace::files,  Workspace::withFiles);
     *  private final Var<Tuple<String>> selectedPath = vm.zoomTo(Workspace::opened, Workspace::withOpened);
     *
     *  private final TreeConf<String, FsNode> shape =
     *          TreeConf.of(FsNode.class)
     *                  .nodesOf(Dir.class, it -> it.children(Dir::entries).text(Dir::name))
     *                  .nodesOf(Doc.class, it -> it.text(Doc::name));
     *
     *  // "Which node is selected" is a function of the tree and the path, so it is derived:
     *  private final Val<FsNode> selectedNode =
     *          Viewable.of(fileSystem, selectedPath,
     *                  (root, path) -> shape.nodeAt(root, path).orElse(null));
     *
     *  UI.panel("fill")
     *  .add("grow, w 35%",
     *      UI.scrollPane().add(
     *          UI.tree(fileSystem, shape)     // the shape builds the tree...
     *          .withSelection(selectedPath)   // ...which reports a path of ids in here...
     *      )
     *  )
     *  .add("grow, w 65%",
     *      UI.panel("fill").add(selectedNode, this::detailsOf)   // ...which answers this pane
     *  );
     *  }</pre>
     *  Note that no listener appears anywhere above, and nothing reaches into the
     *  {@link javax.swing.JTree}. Clicking a row writes a path into {@code selectedPath}, which
     *  makes {@code selectedNode} a different node, which swaps the view in the right hand
     *  panel — three pure functions of one property each.
     *  <p>
     *  Use {@link #nodeAt(Tuple, Tuple)} instead where the tree was bound with
     *  {@link UI#trees(sprouts.Var, swingtree.api.Configurator)}, whose property holds a
     *  {@link Tuple} of top level nodes rather than one root.
     *  <p>
     *  The result is empty for the empty path (nothing is selected), for a path which no
     *  longer leads anywhere, which is what a path pointing into a since-deleted branch does,
     *  and for a root which is {@code null}, which is what a property holding nothing is.
     *
     * @param root The root value of the tree, which is what the path is relative to,
     *             or {@code null} where the property holding it is empty.
     * @param path The ids leading from the root down to the node, the root's own id first.
     * @return The node at that path, or an empty optional if the path names nothing.
     */
    public Optional<N> nodeAt( @Nullable N root, Tuple<I> path ) {
        Tuple<N> along = nodesAlong(root, path);
        return ( along.isEmpty() ? Optional.empty() : Optional.of(along.last()) );
    }

    /**
     *  Resolves a selection path of a <b>forest</b> back to the node it names, which is the
     *  same question {@link #nodeAt(Object, Tuple)} answers for a single rooted tree. A forest
     *  has no root, so the first id of the path names one of the top level nodes rather than a
     *  container above them, and this overload therefore takes the whole top level.
     *  <p>
     *  It is the detail pane of {@link #nodeAt(Object, Tuple)} with one property widened, and
     *  nothing else about the view changes:
     *  <pre>{@code
     *  private final Var<Tuple<FsNode>> projects     = vm.zoomTo(Workspace::open,   Workspace::withOpen);
     *  private final Var<Tuple<String>> selectedPath = vm.zoomTo(Workspace::opened, Workspace::withOpened);
     *
     *  private final TreeConf<String, FsNode> shape =
     *          TreeConf.of(FsNode.class)
     *                  .nodesOf(Dir.class, it -> it.children(Dir::entries).text(Dir::name))
     *                  .nodesOf(Doc.class, it -> it.text(Doc::name));
     *
     *  // The node type has to be named here, because the property no longer has it:
     *  private final Val<FsNode> selectedNode =
     *          Viewable.of(FsNode.class, projects, selectedPath,
     *                  (roots, path) -> shape.nodeAt(roots, path).orElse(null));
     *
     *  UI.panel("fill")
     *  .add("grow, w 35%",
     *      UI.scrollPane().add(
     *          UI.trees(projects, shape)      // several top level nodes, no root drawn
     *          .withSelection(selectedPath)   // [ "myapp", "src", "App.java" ]
     *      )
     *  )
     *  .add("grow, w 65%",
     *      UI.panel("fill").add(selectedNode, this::detailsOf)
     *  );
     *  }</pre>
     *  The one line worth pausing on is the {@code FsNode.class} in the middle. The three
     *  argument {@code Viewable.of(..)} takes its result type from the first property, which
     *  here holds a {@code Tuple<FsNode>} and not an {@code FsNode}, so the four argument form
     *  is the one a forest needs.
     *
     * @param roots The top level nodes of the forest, which is what the path is relative to.
     * @param path The ids leading from a top level node down to the node, its own id first.
     * @return The node at that path, or an empty optional if the path names nothing.
     */
    public Optional<N> nodeAt( Tuple<N> roots, Tuple<I> path ) {
        Tuple<N> along = nodesAlong(roots, path);
        return ( along.isEmpty() ? Optional.empty() : Optional.of(along.last()) );
    }

    /**
     *  Resolves a selection path to every node along it, the root first and the named node
     *  last, which is what a breadcrumb trail is made of. Where
     *  {@link #nodeAt(Object, Tuple)} answers "what is selected", this answers "how did we get
     *  there", and a bar above the tree is the usual reason to ask:
     *  <pre>{@code
     *  private final Var<FsNode>        fileSystem   = vm.zoomTo(Workspace::files,  Workspace::withFiles);
     *  private final Var<Tuple<String>> selectedPath = vm.zoomTo(Workspace::opened, Workspace::withOpened);
     *
     *  private final TreeConf<String, FsNode> shape =
     *          TreeConf.of(FsNode.class)
     *                  .nodesOf(Dir.class, it -> it.children(Dir::entries).text(Dir::name))
     *                  .nodesOf(Doc.class, it -> it.text(Doc::name));
     *
     *  private final Val<String> breadcrumb =
     *          Viewable.of(String.class, fileSystem, selectedPath,
     *                  (root, path) -> shape.nodesAlong(root, path)
     *                                       .stream().map(FsNode::name)
     *                                       .collect(Collectors.joining("  >  ")));
     *
     *  UI.panel("fill, wrap 1")
     *  .add("growx", UI.label(breadcrumb))   // Workspace  >  src  >  App.java
     *  .add("grow",
     *      UI.scrollPane().add(
     *          UI.tree(fileSystem, shape).withSelection(selectedPath)
     *      )
     *  );
     *  }</pre>
     *  The label reads correctly after a rename anywhere along that trail, because the trail
     *  is resolved from the ids the selection holds every time either property changes,
     *  rather than being a string somebody remembered to rebuild.
     *  <p>
     *  Use {@link #nodesAlong(Tuple, Tuple)} instead where the tree was bound with
     *  {@link UI#trees(sprouts.Var, swingtree.api.Configurator)}.
     *  <p>
     *  The result is empty when the path names nothing, so a partial trail is never
     *  returned: either the whole path resolves or none of it does.
     *
     * @param root The root value of the tree, which is what the path is relative to,
     *             or {@code null} where the property holding it is empty.
     * @param path The ids leading from the root down to the node, the root's own id first.
     * @return Every node from the root down to the named one, or an empty tuple.
     */
    public Tuple<N> nodesAlong( @Nullable N root, Tuple<I> path ) {
        Objects.requireNonNull(path, "path");
        if ( root == null )
            return Tuple.of(_nodeType);
        return nodesAlong(Tuple.of(_nodeType, Collections.singletonList(root)), path);
    }

    /**
     *  Resolves a selection path of a <b>forest</b> to every node along it, the top level node
     *  first and the named node last. It is {@link #nodesAlong(Object, Tuple)} in every respect
     *  but where the trail starts: a forest has no root, so its trails begin one level lower
     *  and the bar has one segment fewer.
     *  <pre>{@code
     *  private final Var<Tuple<FsNode>> projects     = vm.zoomTo(Workspace::open,   Workspace::withOpen);
     *  private final Var<Tuple<String>> selectedPath = vm.zoomTo(Workspace::opened, Workspace::withOpened);
     *
     *  private final TreeConf<String, FsNode> shape =
     *          TreeConf.of(FsNode.class)
     *                  .nodesOf(Dir.class, it -> it.children(Dir::entries).text(Dir::name))
     *                  .nodesOf(Doc.class, it -> it.text(Doc::name));
     *
     *  private final Val<String> breadcrumb =
     *          Viewable.of(String.class, projects, selectedPath,
     *                  (roots, path) -> shape.nodesAlong(roots, path)
     *                                        .stream().map(FsNode::name)
     *                                        .collect(Collectors.joining("  >  ")));
     *
     *  UI.panel("fill, wrap 1")
     *  .add("growx", UI.label(breadcrumb))   // myapp  >  src  >  App.java
     *  .add("grow",
     *      UI.scrollPane().add(
     *          UI.trees(projects, shape).withSelection(selectedPath)
     *      )
     *  );
     *  }</pre>
     *  Compare that comment with the one on {@link #nodesAlong(Object, Tuple)}: the workspace
     *  itself is not in the trail, because in a forest it is not a node at all. Which is the
     *  whole point of binding one — there is no invented container to explain away, either on
     *  screen or inside the paths the application stores.
     *
     * @param roots The top level nodes of the forest, which is what the path is relative to.
     * @param path The ids leading from a top level node down to the node, its own id first.
     * @return Every node from the top level down to the named one, or an empty tuple.
     */
    public Tuple<N> nodesAlong( Tuple<N> roots, Tuple<I> path ) {
        Objects.requireNonNull(roots, "roots");
        Objects.requireNonNull(path, "path");
        Tuple<N> empty = Tuple.of(_nodeType);
        if ( path.isEmpty() )
            return empty;
        int found = _indexOfId(roots, path.get(0));
        if ( found < 0 )
            return empty;
        List<N> along = new ArrayList<>(path.size());
        Object current = roots.get(found);
        along.add(_nodeType.cast(current));
        for ( int level = 1; level < path.size(); level++ ) {
            TreeNodeConf<N, ?> rule = ruleFor(current);
            if ( rule == null || !rule.hasChildrenRule() )
                return empty;
            Tuple<Object> children;
            try {
                children = rule.childrenOf(current);
            } catch (Exception e) {
                log.debug(SwingTree.get().logMarker(), "Failed to walk a tree path.", e);
                return empty;
            }
            found = _indexOfId(children, path.get(level));
            if ( found < 0 )
                return empty;
            current = children.get(found);
            along.add(_nodeType.cast(current));
        }
        return Tuple.of(_nodeType, along);
    }

    private int _indexOfId( Tuple<?> nodes, I id ) {
        for ( int i = 0; i < nodes.size(); i++ )
            if ( Objects.equals(idOf(nodes.get(i)), id) )
                return i;
        return -1;
    }

    Class<N> nodeType() {
        return _nodeType;
    }

    /** Null where no type was declared, in which case {@code PropertyTreeModel} derives one. */
    @Nullable Class<I> idType() {
        return _idType;
    }

    boolean leafWhenEmpty() {
        return _leafWhenEmpty;
    }

    boolean hasRules() {
        return !_rules.isEmpty();
    }

    /** Decides whether the tree gets an in place rename editor at all. */
    boolean hasRenamableRule() {
        for ( TreeNodeConf<N, ?> rule : _rules.values() )
            if ( rule.isRenamable() )
                return true;
        return false;
    }

    /**
     *  A declared id getter wins over {@link HasId}, and a node offering neither falls back
     *  to itself, which still works for a static tree of distinct siblings.
     */
    Object idOf( @Nullable Object node ) {
        if ( node == null )
            return NULL_ID;
        Function<N, I> getter = _idGetter;
        if ( getter != null ) {
            try {
                Object id = getter.apply(_nodeType.cast(node));
                if ( id != null )
                    return id;
            } catch (Exception e) {
                log.debug(SwingTree.get().logMarker(), "Failed to read the id of tree node '{}'.", node, e);
            }
        }
        if ( node instanceof HasId ) {
            Object id = ((HasId<?>) node).id();
            if ( id != null )
                return id;
        }
        return node;
    }

    /**
     *  Among all rules whose type is assignable from the node's type, the one whose type no
     *  other match is assignable from — so a concrete record beats the sealed interface
     *  above it. Where two matches are unrelated, the one declared first wins.
     */
    private @Nullable TreeNodeConf<N, ?> _findRule( Class<?> type ) {
        @Nullable TreeNodeConf<N, ?> best = null;
        for ( Pair<Class<?>, TreeNodeConf<N, ?>> entry : _rules.entrySet() ) {
            if ( !entry.first().isAssignableFrom(type) )
                continue;
            if ( best == null || best.type().isAssignableFrom(entry.first()) )
                best = entry.second();
        }
        return best;
    }

    private static final Object NO_RULE = new Object();
    static final Object NULL_ID = new Object() {
        @Override public String toString() { return "null"; }
    };

    @SuppressWarnings("unchecked")
    @Nullable TreeNodeConf<N, ?> ruleFor( @Nullable Object node ) {
        if ( node == null )
            return null;
        Class<?> type = node.getClass();
        Object cached = _ruleMemo.get(type);
        if ( cached == null ) {
            @Nullable TreeNodeConf<N, ?> found = _findRule(type);
            if ( found == null )
                log.warn(SwingTree.get().logMarker(),
                        "No tree node rule declared for type '{}'. It will be rendered as a leaf " +
                        "labelled by its 'toString()'. Declare one through 'nodesOf({}.class, ..)'.",
                        type.getName(), type.getSimpleName());
            cached = ( found == null ? NO_RULE : found );
            _ruleMemo.put(type, cached);
        }
        return cached == NO_RULE ? null : (TreeNodeConf<N, ?>) cached;
    }

    /**
     *  Tells whether a complaint about the given node type has still to be made,
     *  and remembers that it now has been.
     *
     * @param type The node type the complaint is about.
     * @return True the first time this configuration is asked about that type.
     */
    boolean shouldWarnAbout( Class<?> type ) {
        return _warnedAbout.add(type);
    }

    @Override
    public String toString() {
        StringBuilder types = new StringBuilder();
        for ( Class<?> type : _rules.keySet() ) {
            if ( types.length() > 0 )
                types.append(", ");
            types.append(type.getSimpleName());
        }
        return this.getClass().getSimpleName() + "[" +
                    "nodeType="      + _nodeType.getSimpleName() + ", " +
                    "rules=["        + types + "], " +
                    "idOf="          + ( _idGetter == null ? "byHasId" : "declared" ) + ", " +
                    "leafWhenEmpty=" + _leafWhenEmpty +
                "]";
    }
}
