package swingtree;

import org.jspecify.annotations.Nullable;

import javax.swing.tree.TreePath;
import java.util.Arrays;

/**
 *  The handle a {@link javax.swing.JTree} holds inside its
 *  {@link javax.swing.tree.TreePath}s when it is bound to a property.
 *  <p>
 *  Its identity is the <b>path of ids</b> leading to the node, never the node's content.
 *  A {@link javax.swing.tree.TreePath} compares by the nodes it contains, and value objects
 *  compare by content, so identifying a node by its value would make renaming one leaf
 *  invalidate every path in the tree at once and collapse everything the user had opened.
 *  <p>
 *  A forest has one handle more than it has nodes: {@link #ofForest()} builds the invisible
 *  one sitting above the top level, whose id path is empty because no id names it and whose
 *  value is {@code null} because it wraps no node of the user's. Its children then get id
 *  paths of length one, and everything below them follows without a special case.
 *  <p>
 *  Value, parent and index ride along in mutable fields written only by
 *  {@link PropertyTreeModel} and only on the UI thread. They take no part in
 *  {@link #equals(Object)}: two handles for the same path <i>are</i> the same node,
 *  whatever either of them currently holds.
 */
final class TreeNodeRef
{
    private static final Object[] NO_IDS = new Object[0];

    private final Object[]              _idPath;
    private final int                   _hash;
    private final @Nullable TreeNodeRef _parent;
    private final int                   _depth;
    private @Nullable Object            _value; // UI thread owned, never part of identity.
    private int                         _index;

    static TreeNodeRef ofRoot( @Nullable Object value, Object id ) {
        return new TreeNodeRef(new Object[]{id}, null, value, 0);
    }

    static TreeNodeRef ofForest() {
        return new TreeNodeRef(NO_IDS, null, null, 0);
    }

    private TreeNodeRef( Object[] idPath, @Nullable TreeNodeRef parent, @Nullable Object value, int index ) {
        _idPath = idPath;
        _hash   = Arrays.hashCode(idPath);
        _parent = parent;
        _depth  = ( parent == null ? 0 : parent._depth + 1 );
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

    /** Shared by reference with every caller, so it must never be mutated. */
    Object[] idPath() {
        return _idPath;
    }

    /**
     *  Walked along the parent chain rather than sized from the id path, because a forest
     *  handle contributes a component to every path below it without contributing an id.
     */
    TreePath path() {
        Object[] chain = new Object[_depth + 1];
        TreeNodeRef current = this;
        for ( int i = _depth; i >= 0; i-- ) {
            chain[i] = current;
            current = current._parent;
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
     *  Forwards to the node, because
     *  {@link javax.swing.JTree#convertValueToText(Object, boolean, boolean, boolean, int, boolean)}
     *  falls back to this when no renderer has anything better to say.
     */
    @Override
    public String toString() {
        return String.valueOf(_value);
    }
}
