package swingtree;

import org.jspecify.annotations.Nullable;
import sprouts.From;
import sprouts.SequenceChange;
import sprouts.Tuple;
import sprouts.Val;
import sprouts.Var;
import sprouts.Viewable;
import sprouts.impl.SequenceDiff;
import sprouts.impl.SequenceDiffOwner;
import swingtree.api.model.AbstractSnapshotTableModel;
import swingtree.api.model.TableSnapshot;

/**
 *  A thread safe, reactive table model whose rows are modelled by a
 *  {@link Tuple} based property, where the outer {@link Tuple} holds the rows
 *  and each inner {@link Tuple} holds the cells of a row (a row major matrix).
 *  <p>
 *  This is the table analogue of {@code swingtree.TuplePropertyComboModel}:
 *  because a {@link Tuple} is deeply immutable, the two threads can simply
 *  publish it to each other without any copying. The property value <em>is</em>
 *  the UI thread owned snapshot. When the property changes on the application
 *  thread, the new tuple is handed to the UI thread, which swaps it in and
 *  fires the table events.
 *  <p>
 *  Crucially, this model exploits the {@link SequenceDiff} carried by the tuple
 *  (mirroring {@code UIForTabbedPane._updateTabs}): if a change is a direct
 *  successor of the previous one, only the affected rows are refreshed through
 *  targeted {@link #fireTableRowsInserted(int, int)} /
 *  {@link #fireTableRowsDeleted(int, int)} / {@link #fireTableRowsUpdated(int, int)}
 *  events. This keeps updates to large tables cheap (O(changes) instead of a
 *  full rebuild), which is exactly why a {@link Tuple} based source should be
 *  preferred for dynamic, application thread owned table data.
 *
 * @param <E> The common type of the cell values in the table.
 */
final class TuplePropertyTableModel<E extends @Nullable Object> extends AbstractSnapshotTableModel
{
    private final Val<Tuple<Tuple<E>>> _rows;
    private final Viewable<Tuple<Tuple<E>>> _rowsView; // A strong reference keeps the (weakly parented) view and its listener alive.
    private volatile @Nullable SequenceDiff _lastDiff; // Read/written on the UI thread only (plus the initial value from the installing thread).

    TuplePropertyTableModel( Val<Tuple<Tuple<E>>> rows ) {
        _rows = rows;
        Tuple<Tuple<E>> initial = _rowsOrEmpty();
        _lastDiff = _diffOf(initial);
        _setSnapshot(_snapshotOf(initial));
        _rowsView = _rows.view();
        _rowsView.onChange(From.ALL, it -> {
            Tuple<Tuple<E>> newRows = it.currentValue().orElseGet(TuplePropertyTableModel::_emptyRows);
            _publishToUIThread(() -> _applyNewRows(newRows));
        });
    }

    /**
     *  Applies a new rows tuple to the UI thread owned snapshot and fires the
     *  most targeted table events the {@link SequenceDiff} allows. Always runs
     *  on the UI thread.
     */
    private void _applyNewRows( Tuple<Tuple<E>> newRows ) {
        TableSnapshot previous = _currentSnapshot();
        int previousColumnCount = previous == null ? -1 : previous.getColumnCount();

        SequenceDiff diff     = _diffOf(newRows);
        SequenceDiff lastDiff = _lastDiff;
        _lastDiff = diff;

        TableSnapshot next = _snapshotOf(newRows);
        _setSnapshot(next); // Swap first, so that the events below observe the new state.

        boolean columnCountChanged = next.getColumnCount() != previousColumnCount;

        if ( columnCountChanged || diff == null || lastDiff == null || !diff.isDirectSuccessorOf(lastDiff) ) {
            _fireEverythingChanged();
            return;
        }
        int index = diff.index().orElse(0);
        switch ( diff.change() ) {
            case ADD:
                fireTableRowsInserted(index, index + diff.size() - 1);
                break;
            case REMOVE:
                fireTableRowsDeleted(index, index + diff.size() - 1);
                break;
            case SET:
                fireTableRowsUpdated(index, index + diff.size() - 1);
                break;
            default:
                // RETAIN, CLEAR, SORT, DISTINCT, REVERSE, NONE and anything unknown:
                // these either affect unknown ranges or the whole table, so we rebuild.
                _fireEverythingChanged();
        }
    }

    @Override
    protected TableSnapshot takeLiveSnapshot() {
        return _snapshotOf(_rowsOrEmpty());
    }

    private static @Nullable SequenceDiff _diffOf( Tuple<?> rows ) {
        if ( rows instanceof SequenceDiffOwner )
            return ((SequenceDiffOwner) rows).differenceFromPrevious().orElse(null);
        return null;
    }

    @SuppressWarnings("unchecked")
    private TableSnapshot _snapshotOf( Tuple<Tuple<E>> rows ) {
        int columnCount = 0;
        for ( Tuple<E> row : rows )
            columnCount = Math.max(columnCount, row.size());
        // The cell tuples are immutable and only ever read, so this cast is safe:
        Tuple<Tuple<@Nullable Object>> cells = (Tuple<Tuple<@Nullable Object>>)(Tuple<?>) rows;
        return TableSnapshot.of(
            columnCount,
            Tuple.ofNullable(String.class), // No explicit names: the base falls back to default (spreadsheet) names.
            Tuple.of((Class<Class<?>>)(Class<?>) Class.class),
            cells
        );
    }

    private Tuple<Tuple<E>> _rowsOrEmpty() {
        Tuple<Tuple<E>> rows = _rows.orElseNull();
        return rows == null ? _emptyRows() : rows;
    }

    @SuppressWarnings("unchecked")
    private static <E extends @Nullable Object> Tuple<Tuple<E>> _emptyRows() {
        return Tuple.of((Class<Tuple<E>>)(Class<?>) Tuple.class);
    }

    // ---- Live accessors (only used if there is no snapshot, which for this model is never). ----

    @Override protected int _liveRowCount() { return _rowsOrEmpty().size(); }

    @Override protected int _liveColumnCount() {
        int columnCount = 0;
        for ( Tuple<E> row : _rowsOrEmpty() )
            columnCount = Math.max(columnCount, row.size());
        return columnCount;
    }

    @Override protected @Nullable Object _liveValueAt( int rowIndex, int columnIndex ) {
        Tuple<Tuple<E>> rows = _rowsOrEmpty();
        if ( rowIndex < 0 || rowIndex >= rows.size() )
            return null;
        Tuple<E> row = rows.get(rowIndex);
        if ( columnIndex < 0 || columnIndex >= row.size() )
            return null;
        return row.get(columnIndex);
    }

    @Override protected @Nullable String _liveColumnName( int columnIndex ) { return null; }
    @Override protected Class<?> _liveColumnClass( int columnIndex ) { return Object.class; }

    // The tuple based rows describe an immutable matrix, so cells are not editable by default.
    @Override protected boolean _liveCellEditable( int rowIndex, int columnIndex ) { return false; }

    @Override
    @SuppressWarnings("unchecked")
    protected void _liveSetValueAt( @Nullable Object value, int rowIndex, int columnIndex ) {
        if ( !(_rows instanceof Var) )
            return; // A read only source cannot receive edits.
        ((Var<Tuple<Tuple<E>>>) _rows).update(From.VIEW, rows -> {
            if ( rowIndex < 0 || rowIndex >= rows.size() )
                return rows; // The rows changed in the meantime; the edit no longer applies.
            Tuple<E> row = rows.get(rowIndex);
            if ( columnIndex < 0 || columnIndex >= row.size() )
                return rows;
            return rows.setAt(rowIndex, row.setAt(columnIndex, NullUtil.fakeNonNull((E) value)));
        });
    }
}
