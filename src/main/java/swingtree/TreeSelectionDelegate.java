package swingtree;

import org.jspecify.annotations.Nullable;
import sprouts.Tuple;
import sprouts.Var;

import javax.swing.JTree;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 *  Everything a selection change in a bound {@link JTree} has to say, handed to the
 *  {@link sprouts.Action} registered through
 *  {@link UIForTree#onSelection(sprouts.Action)}.
 *  <p>
 *  It speaks entirely in the user's own node values. The handles a bound tree keeps inside
 *  its {@link TreePath}s are an internal matter and never surface here, which is also why
 *  {@link JTree#getLastSelectedPathComponent()} is not a useful thing to call on a bound tree.
 *  <p>
 *  Note that for the common case of mirroring the selection into a view model there is no
 *  need for this delegate at all: bind a property with
 *  {@link UIForTree#withSelection(Var)} instead and the two stay in sync by themselves.
 *
 * @param <I> The identity type of the nodes, which selection paths are made of.
 * @param <N> The common node type of the tree.
 */
public final class TreeSelectionDelegate<I, N>
{
    /*
        Read once, on the UI thread, in the constructor. An action is delivered on the
        application thread, and the node handles a bound tree keeps are owned by the UI
        thread, so reading them from here later would be a race under DECOUPLED. Values,
        ids and id paths are all immutable, so a snapshot of them needs no copy.
    */
    private final JTree                   _tree;
    private final PropertyTreeModel<I, N> _model;
    private final @Nullable N             _lead;
    private final @Nullable Object[]      _leadIdPath;
    private final Tuple<N>                _selection;
    private final Tuple<N>                _pathToLead;
    private final Tuple<I>                _leadPath;
    private final Tuple<Tuple<I>>         _selectionPaths;

    TreeSelectionDelegate(
        JTree                 tree,
        PropertyTreeModel<I, N> model,
        @Nullable TreePath    leadPath,
        @Nullable TreePath[]  selectedPaths
    ) {
        _tree           = Objects.requireNonNull(tree);
        _model          = Objects.requireNonNull(model);
        TreePath[] paths = ( selectedPaths == null ? new TreePath[0] : selectedPaths );
        Class<N> nodeType = model.conf().nodeType();
        Class<I> idType   = model.idType();

        _lead       = _nodeOf(model, leadPath);
        _leadIdPath = _idPathOf(leadPath);
        _leadPath   = model.idTupleOf(leadPath, idType);

        List<N> selected = new ArrayList<>(paths.length);
        List<Tuple<I>> selectedIds = new ArrayList<>(paths.length);
        for ( TreePath path : paths ) {
            N node = _nodeOf(model, path);
            if ( node != null )
                selected.add(node);
            selectedIds.add(model.idTupleOf(path, idType));
        }
        _selection      = Tuple.of(nodeType, selected);
        _selectionPaths = Tuple.of(Tuple.classTyped(idType), selectedIds);

        List<N> trail = new ArrayList<>();
        if ( leadPath != null )
            for ( Object component : leadPath.getPath() ) {
                Object value = model.valueOf(component);
                if ( value != null )
                    trail.add(nodeType.cast(value));
            }
        _pathToLead = Tuple.of(nodeType, trail);
    }

    private static @Nullable Object[] _idPathOf( @Nullable TreePath path ) {
        if ( path == null )
            return null;
        Object last = path.getLastPathComponent();
        return ( last instanceof TreeNodeRef ? ((TreeNodeRef) last).idPath() : null );
    }

    private static <I, N> @Nullable N _nodeOf( PropertyTreeModel<I, N> model, @Nullable TreePath path ) {
        if ( path == null )
            return null;
        Object value = model.valueOf(path.getLastPathComponent());
        return ( value == null ? null : model.conf().nodeType().cast(value) );
    }

    /**
     *  The tree whose selection changed, in case you need to reach past this delegate.
     *  @return The {@link JTree} this selection belongs to.
     */
    public JTree tree() {
        return _tree;
    }

    /**
     *  The node the user just moved the selection onto, which is empty when the selection
     *  was cleared rather than moved.
     *  @return The node the selection now leads with.
     */
    public Optional<N> lead() {
        return Optional.ofNullable(_lead);
    }

    /**
     *  Every currently selected node, in the order the tree reports them. For a single
     *  selection tree this holds at most one node, and it is empty when nothing is selected.
     *  @return All selected nodes.
     */
    public Tuple<N> selection() {
        return _selection;
    }

    /**
     *  The chain of nodes from the root of the tree down to (and including) the
     *  {@link #lead()} node, which is how you learn where in the structure the selection
     *  landed. It is empty when the selection was cleared.
     *  @return The nodes leading from the root to the selected node.
     */
    public Tuple<N> pathToLead() {
        return _pathToLead;
    }

    /**
     *  The identity of the selected position: the ids leading from the root down to the
     *  {@link #lead()} node, the root's own id first. This is the same value a property
     *  bound with {@link UIForTree#withSelection(sprouts.Var)} receives, and it is empty
     *  when the selection was cleared rather than moved.
     *  <p>
     *  Where {@link #pathToLead()} answers "what is selected", this answers "which position
     *  is selected" — the question a node value cannot answer on its own, because the same
     *  value may sit in several places at once.
     *
     *  @return The ids leading to the selected node.
     */
    public Tuple<I> leadPath() {
        return _leadPath;
    }

    /**
     *  The identity of every selected position, in the order the tree reports them. This is
     *  what a property bound with {@link UIForTree#withSelectionPaths(sprouts.Var)} receives.
     *
     *  @return One path of ids per selected node.
     */
    public Tuple<Tuple<I>> selectionPaths() {
        return _selectionPaths;
    }

    /**
     *  A writable property focused on the selected node, so that an action reacting to a
     *  selection can go straight on to edit what was selected, and the edit lands in the
     *  single root property the tree is bound to.
     *  <p>
     *  It is empty when nothing is selected, or when the tree was bound to a read only
     *  {@link sprouts.Val}, in which case there is nothing to write into.
     *
     *  @return A lens property onto the selected node.
     */
    public Optional<Var<N>> property() {
        Object[] idPath = _leadIdPath;
        if ( idPath == null )
            return Optional.empty();
        return Optional.ofNullable(_model.propertyFor(idPath, _lead));
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" +
                    "lead="      + ( _lead == null ? "none" : String.valueOf(_lead) ) + ", " +
                    "selection=" + _selection.size() + " node(s)" +
                "]";
    }
}
