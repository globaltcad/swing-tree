package swingtree;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sprouts.From;
import sprouts.Tuple;
import sprouts.Val;
import sprouts.Var;
import swingtree.threading.EventProcessor;

import javax.swing.JTree;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 *  A thread safe, reactive {@link TreeModel} whose contents are a single property holding a
 *  deeply immutable, nested data structure, traversed according to a {@link TreeConf}.
 *  Because the bound value is deeply immutable, the property value <em>is</em> the UI thread
 *  owned snapshot: nothing is copied to hand it across, and the Event Dispatch Thread never
 *  reads application thread owned mutable state.
 *  <p>
 *  That property holds either one root node or, for a forest, a {@link Tuple} of top level
 *  nodes. {@link TreeRoots} is the only place that difference is expressed; everything below
 *  the top level is the same in both forms.
 *  <p>
 *  Two design decisions are worth knowing about:
 *  <ul>
 *      <li><b>The nodes this model hands to the {@link JTree} are {@link TreeNodeRef} handles,
 *      not the user's own values</b>, which is what keeps expanded paths and the selection
 *      alive across an edit (see {@link TreeNodeRef}). Everything wanting the real value asks
 *      {@link #valueOf(Object)}.</li>
 *      <li><b>An update costs what is on screen, not what exists.</b> {@link #applyNewValue}
 *      walks only the expanded region and stops at every subtree reference identical to the
 *      one it had before, which structural sharing makes true for everything the change did
 *      not touch. A change it cannot express as node events falls back to a structure change
 *      plus a capture and restore of expansion and selection, which the id based handles
 *      make invisible.</li>
 *  </ul>
 *
 * @param <I> The identity type of the nodes, which selection paths are made of.
 * @param <N> The common node type of the tree.
 * @param <R> The type of the value the tree is bound to: one node, or a tuple of them.
 */
final class PropertyTreeModel<I, N, R> implements TreeModel
{
    private static final Logger log = LoggerFactory.getLogger(PropertyTreeModel.class);

    /**
     *  Beyond this many handles the canonical map is dropped and rebuilt lazily. Dropping it
     *  is always safe: identity is the path of ids, and a handle the {@link JTree} still
     *  holds resolves through {@link #valueOf(Object)} either way.
     */
    private static final int MAX_CANONICAL_REFS = 100_000;

    private final Val<R>                        _bound;
    private final @Nullable Var<R>              _writable;
    private final TreeRoots<N, R>               _roots;
    private final TreeConf<I, N>                _conf;
    private final EventProcessor                _eventProcessor;
    private final EventListenerList             _listeners = new EventListenerList();
    private final Map<TreeNodeRef, TreeNodeRef> _canonical = new HashMap<>();
    private final List<Runnable>                _afterRestructure = new ArrayList<>();

    private @Nullable WeakReference<JTree> _tree;
    private @Nullable Class<I>             _derivedIdType;
    private @Nullable TreeNodeRef          _rootRef;
    private @Nullable R                    _snapshot;
    private boolean                        _restructuring          = false;
    private boolean                        _warnedAboutDuplicates  = false;
    private boolean                        _warnedAboutIdType      = false;

    PropertyTreeModel(
        Val<R>          bound,
        TreeRoots<N, R> roots,
        TreeConf<I, N>  conf,
        boolean         writable,
        EventProcessor  eventProcessor
    ) {
        _bound          = Objects.requireNonNull(bound);
        _roots          = Objects.requireNonNull(roots);
        _conf           = Objects.requireNonNull(conf);
        _eventProcessor = Objects.requireNonNull(eventProcessor);
        _writable       = ( writable && bound instanceof Var && bound.isMutable() ) ? (Var<R>) bound : null;
        _snapshot       = bound.orElseNull();
        _rootRef        = _newRootRef();
    }

    void attachTo( JTree tree ) {
        _tree = new WeakReference<>(tree);
    }

    TreeConf<I, N> conf() {
        return _conf;
    }

    /**
     *  Not the same question as which {@code UI.tree(..)} overload was used: a property may
     *  be handed over through a mutable overload and still refuse writes.
     */
    boolean isWritable() {
        return _writable != null;
    }

    /**
     *  A forest has no root of its own: the {@link JTree} is given a synthetic handle above
     *  the top level nodes, which is never drawn and which no path of ids names.
     */
    boolean isForest() {
        return _roots.isForest();
    }

    /** The top level nodes of the tree: one for a single rooted tree, any number for a forest. */
    private Tuple<N> _rootNodes() {
        return _roots.of(_snapshot);
    }

    private static boolean _isForestHandle( @Nullable Object node ) {
        return node instanceof TreeNodeRef && ((TreeNodeRef) node).idPath().length == 0;
    }

    /**
     *  Resolves the user's own node value behind whatever the {@link JTree} handed us.
     *  Everything goes through here rather than casting, because a handle the tree kept
     *  across a dropped canonical map still answers correctly.
     */
    @Nullable Object valueOf( @Nullable Object node ) {
        if ( !(node instanceof TreeNodeRef) )
            return node;
        TreeNodeRef canonical = _canonical.get(node);
        return ( canonical != null ? canonical.value() : ((TreeNodeRef) node).value() );
    }

    @Nullable TreeNodeConf<N, ?> ruleOf( @Nullable Object node ) {
        return _conf.ruleFor(valueOf(node));
    }

    /**
     *  A writable property focused on one single node of the tree, or {@code null} for a
     *  read only tree. It takes a path of ids and a value the caller already holds rather
     *  than a handle, so a delegate can read what it needs on the UI thread and then build
     *  the property from the application thread.
     */
    @Nullable Var<N> propertyFor( Object[] idPath, @Nullable N value ) {
        Var<R> writable = _writable;
        if ( writable == null || idPath.length == 0 )
            return null; // An empty path names the forest itself, which is no node of the user's.
        return writable.zoomTo(new TreePathLens<>(_conf, _roots, idPath, value));
    }

    // ---------------------------------------------------------------- TreeModel

    /**
     *  Null when a single rooted tree's property holds nothing, which a {@link JTree} draws as
     *  no rows at all. Wrapping the absent value in a handle instead would put a row reading
     *  "null" on screen. A forest always has its handle, and an empty forest simply has no
     *  children below it.
     */
    @Override
    public @Nullable Object getRoot() {
        return _rootRef;
    }

    @Override
    public int getChildCount( Object parent ) {
        return _childrenOf(parent).size();
    }

    @Override
    public Object getChild( Object parent, int index ) {
        Tuple<?> children = _childrenOf(parent);
        if ( index < 0 || index >= children.size() )
            throw new IndexOutOfBoundsException(
                "There is no child at index " + index + " below '" + parent + "'."
            );
        return _canonicalChild((TreeNodeRef) parent, children.get(index), index);
    }

    @Override
    public int getIndexOfChild( @Nullable Object parent, @Nullable Object child ) {
        if ( !(parent instanceof TreeNodeRef) || !(child instanceof TreeNodeRef) )
            return -1;
        Object[] childIds = ((TreeNodeRef) child).idPath();
        Object[] parentIds = ((TreeNodeRef) parent).idPath();
        if ( childIds.length != parentIds.length + 1 )
            return -1;
        Object wantedId = childIds[childIds.length - 1];
        Tuple<?> children = _childrenOf(parent);
        for ( int i = 0; i < children.size(); i++ )
            if ( Objects.equals(_conf.idOf(children.get(i)), wantedId) )
                return i;
        return -1;
    }

    @Override
    public boolean isLeaf( Object node ) {
        if ( _isForestHandle(node) )
            return false; // Nothing would be drawn at all if the forest were a leaf.
        Object value = valueOf(node);
        TreeNodeConf<N, ?> rule = _conf.ruleFor(value);
        if ( rule == null )
            return true;
        Boolean declared = rule.declaredLeafState();
        if ( declared != null )
            return declared;
        if ( !rule.hasChildrenRule() )
            return true;
        return _conf.leafWhenEmpty() && _childrenOf(node).isEmpty();
    }

    /** Receives an in place rename from the cell editor and writes it back through a lens. */
    @Override
    public void valueForPathChanged( TreePath path, Object newValue ) {
        Object last = path.getLastPathComponent();
        if ( !(last instanceof TreeNodeRef) )
            return;
        Object value = valueOf(last);
        TreeNodeConf<N, ?> rule = _conf.ruleFor(value);
        Var<R> writable = _writable;
        if ( value == null || rule == null )
            return;
        if ( !rule.isRenamable() ) {
            log.debug(SwingTree.get().logMarker(),
                "Ignoring an edit of a tree node of type '{}', because it declares no text wither.",
                value.getClass().getSimpleName());
            return;
        }
        if ( writable == null ) {
            log.warn(SwingTree.get().logMarker(),
                "Ignoring an edit of a tree node, because the tree is bound to a read only property. " +
                "Bind it to a 'Var' if the user should be able to edit it.");
            return;
        }
        String text = ( newValue == null ? "" : String.valueOf(newValue) );
        Object renamed;
        try {
            renamed = rule.withText(value, text);
        } catch (Exception e) {
            log.error(SwingTree.get().logMarker(), "Failed to apply the edited text '{}' to a tree node.", text, e);
            return;
        }
        N renamedNode = _conf.nodeType().cast(renamed);
        TreePathLens<I, N, R> lens = new TreePathLens<>(
            _conf, _roots, ((TreeNodeRef) last).idPath(), _conf.nodeType().cast(value)
        );
        _eventProcessor.registerAppEvent(
            () -> writable.update(From.VIEW, bound -> lens.wither(bound, renamedNode))
        );
    }

    @Override
    public void addTreeModelListener( TreeModelListener listener ) {
        _listeners.add(TreeModelListener.class, listener);
    }

    @Override
    public void removeTreeModelListener( TreeModelListener listener ) {
        _listeners.remove(TreeModelListener.class, listener);
    }

    // ------------------------------------------------------------ Model updates

    /**
     *  Adopts a new bound value and tells the {@link JTree} about it in the most targeted way
     *  the change permits. Runs on the UI thread.
     */
    void applyNewValue( @Nullable R newValue ) {
        @Nullable R previous = _snapshot;
        _snapshot = newValue;
        if ( previous == newValue )
            return; // Reference identical: nothing anywhere in the tree changed.
        TreeNodeRef rootRef = _rootRef;
        if ( rootRef == null ) {
            _rebuildEverything();
            return;
        }
        /*
            Collected rather than fired as they are discovered, because a walk which ends up
            meeting a structural change discards them: the structure change already tells the
            tree everything, so firing node changes ahead of it is work it will throw away.
        */
        List<TreeNodeRef> changed = new ArrayList<>();
        boolean synced;
        try {
            synced = _syncTopLevel(rootRef, previous, newValue, changed);
        } catch (Exception e) {
            log.debug(SwingTree.get().logMarker(), "Failed to incrementally sync a bound tree.", e);
            synced = false;
        }
        if ( synced ) {
            for ( TreeNodeRef ref : changed )
                _fireNodeChanged(ref);
        } else
            _rebuildEverything();
    }

    /**
     *  A forest handle is the parent of the top level nodes, so a change of the bound value is
     *  a change of its children. A single root has no such parent and is compared as a node —
     *  and a root whose id changed is a different tree, which no path carries over into.
     */
    private boolean _syncTopLevel(
        TreeNodeRef ref, @Nullable R previous, @Nullable R current, List<TreeNodeRef> changed
    ) {
        if ( _roots.isForest() )
            return _syncChildren(ref, _roots.of(previous), _roots.of(current), changed);
        if ( previous == null || current == null )
            return false;
        if ( !Objects.equals(_conf.idOf(previous), _conf.idOf(current)) )
            return false;
        return _syncNode(ref, previous, current, changed);
    }

    /**
     *  Collects every node whose content differs, and returns false as soon as it meets a
     *  change no node event can express — a node which changed type, or a branch whose
     *  children were inserted, removed or reordered — which the caller answers with a rebuild.
     */
    private boolean _syncNode( TreeNodeRef ref, Object oldValue, Object newValue, List<TreeNodeRef> changed ) {
        if ( oldValue == newValue )
            return true; // Structural sharing: this whole subtree is untouched.
        if ( oldValue.getClass() != newValue.getClass() )
            return false; // The node became a different variant of the sum type.

        ref.updateValue(newValue);
        changed.add(ref);

        if ( !_isExpanded(ref) )
            return true; // Nothing below an unexpanded node is on screen, so nothing to do.

        TreeNodeConf<N, ?> rule = _conf.ruleFor(newValue);
        if ( rule == null || !rule.hasChildrenRule() )
            return true;

        return _syncChildren(ref, rule.childrenOf(oldValue), rule.childrenOf(newValue), changed);
    }

    private boolean _syncChildren(
        TreeNodeRef parent, Tuple<?> oldChildren, Tuple<?> newChildren, List<TreeNodeRef> changed
    ) {
        if ( oldChildren == newChildren )
            return true;
        if ( oldChildren.size() != newChildren.size() )
            return false;

        for ( int i = 0; i < newChildren.size(); i++ ) {
            Object oldChild = oldChildren.get(i);
            Object newChild = newChildren.get(i);
            if ( oldChild == newChild )
                continue;
            if ( !Objects.equals(_conf.idOf(oldChild), _conf.idOf(newChild)) )
                return false; // The children were reordered or exchanged.
            if ( !_syncNode(_canonicalChild(parent, newChild, i), oldChild, newChild, changed) )
                return false;
        }
        return true;
    }

    /**
     *  The blunt but always correct update: announce a structure change, then put the
     *  expanded paths and the selection back, which id based identity lets them survive.
     */
    private void _rebuildEverything() {
        JTree tree = _treeOrNull();
        List<Object[]> expanded = _captureExpandedIdPaths(tree);
        List<Object[]> selected = _captureSelectedIdPaths(tree);

        TreeNodeRef previousRoot = _rootRef;
        _canonical.clear();
        _rootRef = _newRootRef();
        TreeNodeRef currentRoot = _rootRef;
        /*
            A root which has just become null leaves nothing to name a path with, so the
            outgoing root's path says "everything below here is gone" instead. An event
            without a path is resolved against the model's current root, which is null too,
            so it would simply be ignored and the rows would stay.
        */
        TreePath announced = ( currentRoot  != null ? currentRoot.path()
                             : previousRoot != null ? previousRoot.path()
                             : null );

        /*
            A structure change makes the JTree drop every selected path before the restore
            below can put them back, and announces that as an ordinary selection event, so a
            bound selection would empty itself and fill again on every insertion. Muting the
            write back for the length of the rebuild and announcing the settled selection
            once afterwards also reports the truth when a selected node really did disappear.
        */
        _restructuring = true;
        try {
            _fireStructureChanged(announced);
            _restoreExpanded(tree, expanded);
            _restoreSelection(tree, selected);
        } finally {
            _restructuring = false;
        }
        if ( !_sameIdPaths(selected, _captureSelectedIdPaths(tree)) )
            _announceSettledSelection();
    }

    private static boolean _sameIdPaths( List<Object[]> before, List<Object[]> after ) {
        if ( before.size() != after.size() )
            return false;
        for ( int i = 0; i < before.size(); i++ )
            if ( !Arrays.equals(before.get(i), after.get(i)) )
                return false;
        return true;
    }

    /**
     *  Runs when a rebuild left the selection somewhere other than where it found it, so a
     *  binding can report that without also reporting the empty selection it passed through.
     */
    void onAfterRestructure( Runnable hook ) {
        _afterRestructure.add(Objects.requireNonNull(hook));
    }

    /**
     *  While true the tree sits between announcing that everything changed and having its
     *  selection put back, so nothing it reports about that selection is true yet.
     */
    boolean isRestructuring() {
        return _restructuring;
    }

    private void _announceSettledSelection() {
        for ( Runnable hook : _afterRestructure )
            try {
                hook.run();
            } catch (Exception e) {
                log.error(SwingTree.get().logMarker(),
                        "Failed to announce the selection of a bound tree after a structural change.", e);
            }
    }

    // ------------------------------------------------------------------ Helpers

    private @Nullable TreeNodeRef _newRootRef() {
        TreeNodeRef rootRef;
        if ( _roots.isForest() )
            rootRef = TreeNodeRef.ofForest(); // Always there, even where the forest is empty.
        else {
            Tuple<N> roots = _rootNodes();
            if ( roots.isEmpty() )
                return null;
            N root = roots.first();
            rootRef = TreeNodeRef.ofRoot(root, _conf.idOf(root));
        }
        _canonical.put(rootRef, rootRef);
        return rootRef;
    }

    private Tuple<?> _childrenOf( @Nullable Object node ) {
        if ( _isForestHandle(node) )
            return _rootNodes(); // The forest has no rule to consult: its children are what is bound.
        Object value = valueOf(node);
        if ( value == null )
            return Tuple.of(Object.class);
        TreeNodeConf<N, ?> rule = _conf.ruleFor(value);
        if ( rule == null || !rule.hasChildrenRule() )
            return Tuple.of(Object.class);
        try {
            return rule.childrenOf(value);
        } catch (Exception e) {
            log.error(SwingTree.get().logMarker(),
                    "Failed to read the children of a tree node of type '{}'.",
                    value.getClass().getSimpleName(), e);
            return Tuple.of(Object.class);
        }
    }

    private TreeNodeRef _canonicalChild( TreeNodeRef parent, @Nullable Object childValue, int index ) {
        TreeNodeRef probe = parent.child(_conf.idOf(childValue), childValue, index);
        TreeNodeRef existing = _canonical.get(probe);
        if ( existing != null ) {
            if ( existing.index() != index )
                _warnAboutDuplicateSiblingIds(childValue, existing.index(), index);
            existing.updateValue(childValue);
            existing.updateIndex(index);
            return existing;
        }
        if ( _canonical.size() >= MAX_CANONICAL_REFS )
            _canonical.clear();
        _canonical.put(probe, probe);
        return probe;
    }

    /**
     *  Two siblings sharing an id collapse onto one handle, so one is drawn twice and the
     *  other never. Nothing can be done about that here, but saying so beats leaving the
     *  user to work out why a row appeared twice.
     */
    private void _warnAboutDuplicateSiblingIds( @Nullable Object childValue, int first, int second ) {
        if ( _warnedAboutDuplicates )
            return;
        _warnedAboutDuplicates = true;
        log.warn(SwingTree.get().logMarker(),
            "Two children at positions {} and {} below the same tree node share the id '{}'. " +
            "A bound tree identifies a node by its path of ids, so the two are indistinguishable " +
            "and only one of them will be shown. Make ids unique among siblings, either through " +
            "'HasId.id()' or through 'TreeConf.idOf(..)'. Node: '{}'.",
            first, second, _conf.idOf(childValue), childValue);
    }

    private @Nullable JTree _treeOrNull() {
        WeakReference<JTree> ref = _tree;
        return ( ref == null ? null : ref.get() );
    }

    private boolean _isExpanded( TreeNodeRef ref ) {
        JTree tree = _treeOrNull();
        if ( tree == null )
            return false;
        if ( ref.parent() == null && !tree.isRootVisible() )
            return true; // An invisible root always shows its children.
        return tree.isExpanded(ref.path());
    }

    private List<Object[]> _captureExpandedIdPaths( @Nullable JTree tree ) {
        List<Object[]> captured = new ArrayList<>();
        TreeNodeRef rootRef = _rootRef;
        if ( tree == null || rootRef == null )
            return captured;
        Enumeration<TreePath> paths = tree.getExpandedDescendants(rootRef.path());
        if ( paths == null )
            return captured;
        while ( paths.hasMoreElements() ) {
            Object last = paths.nextElement().getLastPathComponent();
            if ( last instanceof TreeNodeRef )
                captured.add(((TreeNodeRef) last).idPath());
        }
        // Shallower paths first, because a deep path can only be expanded once its parents are:
        Collections.sort(captured, Comparator.comparingInt(ids -> ids.length));
        return captured;
    }

    private List<Object[]> _captureSelectedIdPaths( @Nullable JTree tree ) {
        List<Object[]> captured = new ArrayList<>();
        if ( tree == null )
            return captured;
        TreePath[] paths = tree.getSelectionPaths();
        if ( paths == null )
            return captured;
        for ( TreePath path : paths ) {
            Object last = path.getLastPathComponent();
            if ( last instanceof TreeNodeRef )
                captured.add(((TreeNodeRef) last).idPath());
        }
        return captured;
    }

    private void _restoreExpanded( @Nullable JTree tree, List<Object[]> idPaths ) {
        if ( tree == null )
            return;
        for ( Object[] idPath : idPaths ) {
            TreePath path = pathForIds(idPath);
            if ( path != null )
                tree.expandPath(path);
        }
    }

    private void _restoreSelection( @Nullable JTree tree, List<Object[]> idPaths ) {
        if ( tree == null || idPaths.isEmpty() )
            return;
        List<TreePath> paths = new ArrayList<>(idPaths.size());
        for ( Object[] idPath : idPaths ) {
            TreePath path = pathForIds(idPath);
            if ( path != null )
                paths.add(path);
        }
        if ( !paths.isEmpty() )
            tree.setSelectionPaths(paths.toArray(new TreePath[0]));
    }

    /** How expansion, selection and any other id addressed state finds its way back. */
    @Nullable TreePath pathForIds( Object[] idPath ) {
        TreeNodeRef current = _rootRef;
        if ( current == null )
            return null;
        // A single root contributes its own id to every path below it; a forest handle none:
        Object[] rootIds = current.idPath();
        if ( idPath.length < rootIds.length )
            return null;
        for ( int level = 0; level < rootIds.length; level++ )
            if ( !Objects.equals(rootIds[level], idPath[level]) )
                return null;
        for ( int level = rootIds.length; level < idPath.length; level++ ) {
            Tuple<?> children = _childrenOf(current);
            int found = -1;
            for ( int i = 0; i < children.size(); i++ )
                if ( Objects.equals(_conf.idOf(children.get(i)), idPath[level]) ) {
                    found = i;
                    break;
                }
            if ( found < 0 )
                return null;
            current = _canonicalChild(current, children.get(found), found);
        }
        return current.path();
    }

    /**
     *  Exact, never a search: a tuple of ids <i>is</i> the identity of a position in the
     *  tree, which is why ids only ever have to be unique among siblings.
     */
    @Nullable TreePath pathForIdTuple( Tuple<I> idPath ) {
        if ( idPath.isEmpty() )
            return null;
        Object[] ids = new Object[idPath.size()];
        for ( int i = 0; i < idPath.size(); i++ )
            ids[i] = idPath.get(i);
        return pathForIds(ids);
    }

    /**
     *  The ids leading down to the position a {@link TreePath} names, taken from the path
     *  the user actually clicked rather than reconstructed.
     *
     * @param path The tree path to describe, or {@code null} for "nothing".
     * @param idType The element type the resulting tuple must carry, because a
     *               {@link Tuple} compares its type as part of its equality.
     */
    Tuple<I> idTupleOf( @Nullable TreePath path, Class<I> idType ) {
        if ( path == null )
            return Tuple.of(idType);
        Object last = path.getLastPathComponent();
        if ( !(last instanceof TreeNodeRef) )
            return Tuple.of(idType);
        Object[] ids = ((TreeNodeRef) last).idPath();
        List<I> typed = new ArrayList<>(ids.length);
        for ( Object id : ids ) {
            if ( !idType.isInstance(id) ) {
                _warnAboutIdType(id, idType);
                return Tuple.of(idType);
            }
            typed.add(idType.cast(id));
        }
        return Tuple.of(idType, typed);
    }

    /**
     *  An id of the wrong type means a node offered no identity and {@link TreeConf#idOf}
     *  fell back to the node itself. Reporting "nothing selected" and saying why beats
     *  throwing out of a Swing listener.
     */
    private void _warnAboutIdType( @Nullable Object id, Class<I> idType ) {
        if ( _warnedAboutIdType )
            return;
        _warnedAboutIdType = true;
        log.warn(SwingTree.get().logMarker(),
            "Cannot report the selected position of a bound tree, because the id '{}' of a node " +
            "on the selected path is not a '{}'. Declare the identity of your node types, either " +
            "through 'HasId.id()' or through 'TreeConf.idOf(..)'.",
            id, idType.getSimpleName());
    }

    /**
     *  The element type selection path tuples are built with. A declared id type wins,
     *  otherwise it is derived from the id of a top level node — walking up out of an
     *  anonymous class, so an enum constant with a body reports its enum rather than the
     *  synthetic subclass Java gave it.
     */
    @SuppressWarnings("unchecked")
    Class<I> idType() {
        Class<I> declared = _conf.idType();
        if ( declared != null )
            return declared;
        Class<I> derived = _derivedIdType;
        if ( derived != null )
            return derived;
        Tuple<N> roots = _rootNodes();
        if ( roots.isEmpty() )
            return (Class<I>) (Class<?>) Object.class;
        Class<?> found = _conf.idOf(roots.first()).getClass();
        while ( found.isAnonymousClass() || found.isSynthetic() ) {
            Class<?> parent = found.getSuperclass();
            if ( parent == null )
                break;
            found = parent;
        }
        _derivedIdType = (Class<I>) found;
        return _derivedIdType;
    }

    // ------------------------------------------------------------------- Events

    private void _fireNodeChanged( TreeNodeRef ref ) {
        TreeNodeRef parent = ref.parent();
        TreeModelEvent event;
        if ( parent == null )
            event = new TreeModelEvent(this, ref.path(), null, null);
        else
            event = new TreeModelEvent(
                this, parent.path(), new int[]{ref.index()}, new Object[]{ref}
            );
        for ( TreeModelListener listener : _listeners.getListeners(TreeModelListener.class) )
            listener.treeNodesChanged(event);
    }

    private void _fireStructureChanged( @Nullable TreePath path ) {
        if ( path == null )
            return; // There was no tree before this change and there is none after it.
        TreeModelEvent event = new TreeModelEvent(this, path, null, null);
        for ( TreeModelListener listener : _listeners.getListeners(TreeModelListener.class) )
            listener.treeStructureChanged(event);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" +
                    "bound="    + _bound + ", " +
                    "roots="    + _roots + ", " +
                    "conf="     + _conf + ", " +
                    "writable=" + ( _writable != null ) +
                "]";
    }
}
