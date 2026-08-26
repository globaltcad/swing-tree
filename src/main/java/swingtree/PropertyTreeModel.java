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
 *  <p>
 *  This is the tree analogue of {@code PropertyTableModel}, and it rests on the same
 *  observation: because the bound value is deeply immutable, the property value <em>is</em>
 *  the UI thread owned snapshot. Nothing is copied to hand it between the application thread
 *  and the AWT Event Dispatch Thread, and the Event Dispatch Thread never reads application
 *  thread owned mutable state.
 *  <p>
 *  Two design decisions are worth knowing about:
 *  <ul>
 *      <li><b>The nodes this model hands to the {@link JTree} are {@link TreeNodeRef} handles,
 *      not the user's own values.</b> A handle takes its identity from the path of ids down
 *      to the node, which is what keeps expanded paths and the selection alive across an edit
 *      (see {@link TreeNodeRef} for why value identity cannot do that). Everything that wants
 *      the real value asks {@link #valueOf(Object)}.</li>
 *      <li><b>An update costs what is on screen, not what exists.</b> When the root property
 *      changes, this model walks only the currently expanded region, and stops at every
 *      subtree whose value is reference identical to the one it had before. Persistent,
 *      structurally shared data makes that comparison true for everything the change did not
 *      touch, so editing one node in a tree of two hundred thousand visits a handful of them.</li>
 *  </ul>
 *  A structural change under an expanded node (an insertion, a removal, a reordering) is
 *  currently answered with a structure change event plus a capture and restore of the
 *  expanded paths and the selection, which is correct and, thanks to the id based handles,
 *  invisible to the user.
 *
 * @param <N> The common node type of the tree.
 */
final class PropertyTreeModel<I, N> implements TreeModel
{
    private static final Logger log = LoggerFactory.getLogger(PropertyTreeModel.class);

    /**
     *  Beyond this many materialised node handles the canonical map is dropped and rebuilt
     *  lazily. Dropping it is always safe, because handle identity is the path of ids and
     *  {@link #valueOf(Object)} falls back to the handle's own value, so a stale handle the
     *  {@link JTree} still holds keeps resolving correctly.
     */
    private static final int MAX_CANONICAL_REFS = 100_000;

    private final Val<N>                        _root;
    private final @Nullable Var<N>              _writableRoot;
    private final TreeConf<I, N>                   _conf;
    private final Map<Class<?>, Object>         _ruleCache;
    private final EventProcessor                _eventProcessor;
    private final EventListenerList             _listeners = new EventListenerList();
    private final Map<TreeNodeRef, TreeNodeRef> _canonical = new HashMap<>();

    private @Nullable WeakReference<JTree> _tree;
    private @Nullable Class<I>             _derivedIdType;
    private @Nullable TreeNodeRef          _rootRef;
    private @Nullable N                    _snapshot;

    PropertyTreeModel( Val<N> root, TreeConf<I, N> conf, boolean writable, EventProcessor eventProcessor ) {
        _root           = Objects.requireNonNull(root);
        _conf           = Objects.requireNonNull(conf);
        _eventProcessor = Objects.requireNonNull(eventProcessor);
        _ruleCache      = conf.newRuleCache();
        _writableRoot   = ( writable && root instanceof Var && root.isMutable() ) ? (Var<N>) root : null;
        _snapshot       = root.orElseNull();
        _rootRef        = _newRootRef();
    }

    /**
     *  Tells this model which tree renders it, which it needs for exactly two things:
     *  asking what is currently expanded (so an update can skip everything that is not),
     *  and restoring expansion and selection after a structural change.
     */
    void attachTo( JTree tree ) {
        _tree = new WeakReference<>(tree);
    }

    TreeConf<I, N> conf() {
        return _conf;
    }

    /**
     *  Tells whether an edit made in the tree actually has somewhere to go. This is not the
     *  same question as which {@code UI.tree(..)} overload was used: a property may be
     *  handed over through a mutable overload and still refuse writes, so the tree asks
     *  here rather than assuming, and does not offer the user an editor it could not honour.
     */
    boolean isWritable() {
        return _writableRoot != null;
    }

    /**
     *  Resolves the user's own node value behind whatever the {@link JTree} handed us.
     *  Renderers, editors and event handlers all go through here rather than casting,
     *  because a handle the tree kept across a dropped canonical map still answers correctly.
     */
    @Nullable Object valueOf( @Nullable Object node ) {
        if ( !(node instanceof TreeNodeRef) )
            return node;
        TreeNodeRef canonical = _canonical.get(node);
        return ( canonical != null ? canonical.value() : ((TreeNodeRef) node).value() );
    }

    @Nullable TreeNodeConf<N, ?> ruleOf( @Nullable Object node ) {
        return _conf.ruleFor(valueOf(node), _ruleCache);
    }

    /**
     *  A writable property focused on one single node of the tree, which is what makes an
     *  edit anywhere in the structure produce one new root value. Returns an empty optional
     *  when the tree was bound read only, because there is then nothing to write into.
     */
    @Nullable Var<N> propertyFor( @Nullable Object node ) {
        Var<N> writable = _writableRoot;
        if ( writable == null || !(node instanceof TreeNodeRef) )
            return null;
        TreeNodeRef ref = (TreeNodeRef) node;
        return writable.zoomTo(
            new TreePathLens<>(_conf, _ruleCache, ref.idPath(), _conf.nodeType().cast(valueOf(ref)))
        );
    }

    // ---------------------------------------------------------------- TreeModel

    @Override
    public Object getRoot() {
        TreeNodeRef rootRef = _rootRef;
        if ( rootRef == null ) {
            rootRef = _newRootRef();
            _rootRef = rootRef;
        }
        return rootRef;
    }

    @Override
    public int getChildCount( Object parent ) {
        return _childrenOf(parent).size();
    }

    @Override
    public Object getChild( Object parent, int index ) {
        Tuple<Object> children = _childrenOf(parent);
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
        Tuple<Object> children = _childrenOf(parent);
        for ( int i = 0; i < children.size(); i++ )
            if ( Objects.equals(_conf.idOf(children.get(i)), wantedId) )
                return i;
        return -1;
    }

    @Override
    public boolean isLeaf( Object node ) {
        Object value = valueOf(node);
        TreeNodeConf<N, ?> rule = _conf.ruleFor(value, _ruleCache);
        if ( rule == null )
            return true;
        Boolean declared = rule.declaredLeafState();
        if ( declared != null )
            return declared;
        if ( !rule.hasChildrenRule() )
            return true;
        // A declared children rule makes a node a branch, empty or not, unless told otherwise:
        return _conf.leafWhenEmpty() && _childrenOf(node).isEmpty();
    }

    /**
     *  Receives an in place rename from the tree's cell editor and writes it back into the
     *  bound property, through the lens focused on the edited node. The edit therefore
     *  produces exactly one new root value with everything off the edited path shared.
     */
    @Override
    public void valueForPathChanged( TreePath path, Object newValue ) {
        Object last = path.getLastPathComponent();
        Object value = valueOf(last);
        TreeNodeConf<N, ?> rule = _conf.ruleFor(value, _ruleCache);
        Var<N> writable = _writableRoot;
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
        TreePathLens<I, N> lens = new TreePathLens<>(
            _conf, _ruleCache, ((TreeNodeRef) last).idPath(), _conf.nodeType().cast(value)
        );
        _eventProcessor.registerAppEvent(
            () -> writable.update(From.VIEW, root -> lens.wither(root, renamedNode))
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
     *  Adopts a new root value and tells the {@link JTree} about it in the most targeted way
     *  the change permits. Always runs on the UI thread.
     */
    void applyNewRoot( @Nullable N newRoot ) {
        @Nullable N previous = _snapshot;
        _snapshot = newRoot;
        if ( previous == newRoot )
            return; // Reference identical: nothing anywhere in the tree changed.
        TreeNodeRef rootRef = _rootRef;
        if ( rootRef == null || previous == null || newRoot == null ) {
            _rebuildEverything();
            return;
        }
        if ( !Objects.equals(_conf.idOf(previous), _conf.idOf(newRoot)) ) {
            _rebuildEverything(); // A different root entirely, so no path carries over.
            return;
        }
        /*
            The node change events are collected rather than fired as they are discovered,
            because a walk which ends up meeting a structural change discards them: a
            structure change already tells the tree everything, and firing node changes
            in front of it would only make the tree do work it is about to throw away.
        */
        List<TreeNodeRef> changed = new ArrayList<>();
        boolean synced;
        try {
            synced = _sync(rootRef, previous, newRoot, changed);
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
     *  Walks the changed region of the tree, collecting every node whose content actually
     *  differs, and returns false as soon as it meets a change no node event can express
     *  (a node that changed type, or a branch whose children were inserted, removed or
     *  reordered), which the caller answers with a full rebuild instead.
     */
    private boolean _sync( TreeNodeRef ref, Object oldValue, Object newValue, List<TreeNodeRef> changed ) {
        if ( oldValue == newValue )
            return true; // Structural sharing: this whole subtree is untouched.
        if ( oldValue.getClass() != newValue.getClass() )
            return false; // The node became a different variant of the sum type.

        ref.updateValue(newValue);
        changed.add(ref);

        if ( !_isExpanded(ref) )
            return true; // Nothing below an unexpanded node is on screen, so nothing to do.

        TreeNodeConf<N, ?> rule = _conf.ruleFor(newValue, _ruleCache);
        if ( rule == null || !rule.hasChildrenRule() )
            return true;

        Tuple<Object> oldChildren = rule.childrenOf(oldValue);
        Tuple<Object> newChildren = rule.childrenOf(newValue);
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
            if ( !_sync(_canonicalChild(ref, newChild, i), oldChild, newChild, changed) )
                return false;
        }
        return true;
    }

    /**
     *  The blunt but always correct update: announce a structure change and put the user's
     *  expanded paths and selection back afterwards. Because node identity is a path of ids
     *  rather than node content, both survive verbatim.
     */
    private void _rebuildEverything() {
        JTree tree = _treeOrNull();
        List<Object[]> expanded = _captureExpandedIdPaths(tree);
        List<Object[]> selected = _captureSelectedIdPaths(tree);

        _canonical.clear();
        _rootRef = _newRootRef();

        _fireStructureChanged();

        _restoreExpanded(tree, expanded);
        _restoreSelection(tree, selected);
    }

    // ------------------------------------------------------------------ Helpers

    private TreeNodeRef _newRootRef() {
        @Nullable N snapshot = _snapshot;
        TreeNodeRef rootRef = TreeNodeRef.ofRoot(snapshot, _conf.idOf(snapshot));
        _canonical.put(rootRef, rootRef);
        return rootRef;
    }

    private Tuple<Object> _childrenOf( @Nullable Object node ) {
        Object value = valueOf(node);
        if ( value == null )
            return Tuple.of(Object.class);
        TreeNodeConf<N, ?> rule = _conf.ruleFor(value, _ruleCache);
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

    /**
     *  Returns the one live handle for a child, creating it if this is the first time the
     *  tree asks for it, and refreshing its value and index if it is not.
     */
    private TreeNodeRef _canonicalChild( TreeNodeRef parent, @Nullable Object childValue, int index ) {
        TreeNodeRef probe = parent.child(_conf.idOf(childValue), childValue, index);
        TreeNodeRef existing = _canonical.get(probe);
        if ( existing != null ) {
            existing.updateValue(childValue);
            existing.updateIndex(index);
            return existing;
        }
        if ( _canonical.size() >= MAX_CANONICAL_REFS )
            _canonical.clear();
        _canonical.put(probe, probe);
        return probe;
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

    /**
     *  Turns a path of ids back into a live {@link TreePath}, or {@code null} when the node
     *  it names is no longer in the tree. This is how expansion, selection and any other id
     *  addressed state finds its way back after a structural change.
     */
    @Nullable TreePath pathForIds( Object[] idPath ) {
        TreeNodeRef current = (TreeNodeRef) getRoot();
        if ( idPath.length == 0 || !Objects.equals(current.idPath()[0], idPath[0]) )
            return null;
        for ( int level = 1; level < idPath.length; level++ ) {
            Tuple<Object> children = _childrenOf(current);
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
     *  Turns a selection path — a tuple of ids leading down from the root — into a live
     *  {@link TreePath}, or {@code null} when it names nothing in the current value.
     *  <p>
     *  This is exact. A tuple of ids <i>is</i> the identity of a position in the tree, so
     *  there is nothing to search for and nothing to be ambiguous about, which is why ids
     *  only ever have to be unique among siblings.
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
     *  The identity of the position a {@link TreePath} names: the ids leading down to it,
     *  the root's own id first. This is what a selection binding writes into its property,
     *  and it is taken from the path the user actually clicked rather than reconstructed.
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
        for ( Object id : ids )
            typed.add(idType.cast(id));
        return Tuple.of(idType, typed);
    }

    /**
     *  The element type selection path tuples are built with. A declared id type wins;
     *  otherwise it is derived from the first id this model actually resolves, walking up
     *  out of an anonymous class so that an enum constant with a body reports its enum
     *  rather than the synthetic subclass Java gave it.
     */
    @SuppressWarnings("unchecked")
    Class<I> idType() {
        Class<I> declared = _conf.idType();
        if ( declared != null )
            return declared;
        Class<I> derived = _derivedIdType;
        if ( derived != null )
            return derived;
        @Nullable N snapshot = _snapshot;
        Class<?> found = Object.class;
        if ( snapshot != null ) {
            Object id = _conf.idOf(snapshot);
            if ( id != null ) {
                Class<?> c = id.getClass();
                while ( c.isAnonymousClass() || c.isSynthetic() ) {
                    Class<?> parent = c.getSuperclass();
                    if ( parent == null )
                        break;
                    c = parent;
                }
                found = c;
            }
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

    private void _fireStructureChanged() {
        TreeNodeRef rootRef = (TreeNodeRef) getRoot();
        TreeModelEvent event = new TreeModelEvent(this, rootRef.path(), null, null);
        for ( TreeModelListener listener : _listeners.getListeners(TreeModelListener.class) )
            listener.treeStructureChanged(event);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" +
                    "root="     + _root + ", " +
                    "conf="     + _conf + ", " +
                    "writable=" + ( _writableRoot != null ) +
                "]";
    }
}
