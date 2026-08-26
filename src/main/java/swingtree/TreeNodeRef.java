package swingtree;

import org.jspecify.annotations.Nullable;

import javax.swing.tree.TreePath;
import java.util.Arrays;

/**
 *  The handle a {@link javax.swing.JTree} actually holds inside its
 *  {@link javax.swing.tree.TreePath}s when it is bound to a property.
 *  <p>
 *  It exists because of a mismatch between value objects and {@link javax.swing.JTree}:
 *  the tree keys its expanded paths and its selection on {@link javax.swing.tree.TreePath},
 *  whose equality bottoms out in the equality of the nodes it contains. Records compare by
 *  content, so renaming a single leaf would produce a new leaf, a new parent, a new
 *  grandparent and a new root, making every path in the whole tree stale at once and
 *  collapsing everything the user had opened.
 *  <p>
 *  A {@link TreeNodeRef} therefore takes its identity from the <b>path of ids</b> leading to
 *  the node, which does not change when the node's content does. That is the same trick the
 *  {@code TupleLens} behind {@code addAll(Var<Tuple<M>>, ..)} plays with {@link sprouts.HasId},
 *  extended from one index to a whole path.
 *  <p>
 *  The node's current value, its parent and its index ride along in mutable fields, so that
 *  painting a row or firing an event costs a field read rather than a walk down from the
 *  root. Those fields are written only by {@link PropertyTreeModel} and only on the UI
 *  thread, and they deliberately take no part in {@link #equals(Object)}: two handles for
 *  the same path <i>are</i> the same node, whatever either of them currently holds.
 */
final class TreeNodeRef
{
    private final Object[]              _idPath;
    private final int                   _hash;
    private final @Nullable TreeNodeRef _parent;
    private @Nullable Object            _value; // UI thread owned, never part of identity.
    private int                         _index; // Position among the parent's children.

    static TreeNodeRef ofRoot( @Nullable Object value, Object id ) {
        return new TreeNodeRef(new Object[]{id}, null, value, 0);
    }

    private TreeNodeRef( Object[] idPath, @Nullable TreeNodeRef parent, @Nullable Object value, int index ) {
        _idPath = idPath;
        _hash   = Arrays.hashCode(idPath);
        _parent = parent;
        _value  = value;
        _index  = index;
    }

    TreeNodeRef child( Object childId, @Nullable Object childValue, int index ) {
        Object[] childPath = Arrays.copyOf(_idPath, _idPath.length + 1);
        childPath[_idPath.length] = childId;
        return new TreeNodeRef(childPath, this, childValue, index);
    }

    @Nullable Object value() {
        return _value;
    }

    void updateValue( @Nullable Object value ) {
        _value = value;
    }

    @Nullable TreeNodeRef parent() {
        return _parent;
    }

    int index() {
        return _index;
    }

    void updateIndex( int index ) {
        _index = index;
    }

    /**
     *  The ids leading from the root down to this node, the root's own id first.
     *  Never mutated after construction.
     */
    Object[] idPath() {
        return _idPath;
    }

    /** The {@link TreePath} from the root down to this node, which is what tree events speak in. */
    TreePath path() {
        Object[] chain = new Object[_idPath.length];
        TreeNodeRef current = this;
        for ( int i = _idPath.length - 1; i >= 0; i-- ) {
            chain[i] = current;
            TreeNodeRef parent = current.parent();
            if ( parent == null )
                return new TreePath(Arrays.copyOfRange(chain, i, chain.length));
            current = parent;
        }
        return new TreePath(chain);
    }

    @Override
    public boolean equals( @Nullable Object other ) {
        if ( other == this )
            return true;
        if ( !(other instanceof TreeNodeRef) )
            return false;
        TreeNodeRef that = (TreeNodeRef) other;
        return this._hash == that._hash && Arrays.equals(this._idPath, that._idPath);
    }

    @Override
    public int hashCode() {
        return _hash;
    }

    /**
     *  {@link javax.swing.JTree#convertValueToText(Object, boolean, boolean, boolean, int, boolean)}
     *  falls back to this when no renderer has anything better to say, so it forwards to the
     *  node itself instead of printing the handle.
     */
    @Override
    public String toString() {
        return String.valueOf(_value);
    }
}
