package swingtree;

import org.jspecify.annotations.Nullable;
import sprouts.From;
import sprouts.Tuple;
import sprouts.Val;
import sprouts.Var;
import sprouts.Viewable;
import sprouts.impl.SequenceDiff;
import sprouts.impl.SequenceDiffOwner;
import swingtree.api.model.AbstractSnapshotTableModel;
import swingtree.api.model.TableSnapshot;

import java.util.Objects;

/**
 *  A thread safe, reactive table model whose cells are modelled by a
 *  {@link Tuple} based property, where the outer {@link Tuple} holds the rows
 *  and each inner {@link Tuple} holds the cells of a row (a row major matrix),
 *  or, if the {@link UI.ListData} layout says so, the other way round (the outer
 *  {@link Tuple} holds the columns and each inner {@link Tuple} the cells of a
 *  column).
 *  <p>
 *  This is the table analogue of {@code swingtree.TuplePropertyComboModel}:
 *  because a {@link Tuple} is deeply immutable, the two threads can simply
 *  publish it to each other without any copying. The property value <em>is</em>
 *  the UI thread owned snapshot. When the property changes on the application
 *  thread, the new tuple is handed to the UI thread, which swaps it in and
 *  fires the table events.
 *  <p>
 *  Crucially, for a row major source this model exploits the {@link SequenceDiff}
 *  carried by the tuple (mirroring {@code UIForTabbedPane._updateTabs}): if a
 *  change is a direct successor of the previous one, only the affected rows are
 *  refreshed through targeted {@link #fireTableRowsInserted(int, int)} /
 *  {@link #fireTableRowsDeleted(int, int)} / {@link #fireTableRowsUpdated(int, int)}
 *  events. This keeps updates to large tables cheap (O(changes) instead of a
 *  full rebuild), which is exactly why a row major {@link Tuple} based source
 *  should be preferred for dynamic, application thread owned table data.
 *  (For a column major source the outer tuple describes columns, so a change to
 *  it never maps onto a row range, which is why such a model falls back to
 *  refreshing all rows at once.)
 *
 * @param <E> The common type of the cell values in the table.
 */
final class TuplePropertyTableModel<E extends @Nullable Object> extends AbstractSnapshotTableModel
{
    private final UI.ListData _layout;
    private final Val<Tuple<Tuple<E>>> _cells;
    private final Viewable<Tuple<Tuple<E>>> _cellsView; // A strong reference keeps the (weakly parented) view and its listener alive.
    private volatile @Nullable SequenceDiff _lastDiff; // Read/written on the UI thread only (plus the initial value from the installing thread).

    TuplePropertyTableModel( UI.ListData layout, Val<Tuple<Tuple<E>>> cells ) {
        _layout = Objects.requireNonNull(layout);
        _cells  = Objects.requireNonNull(cells);
        Tuple<Tuple<E>> initial = _cellsOrEmpty();
        _lastDiff = _diffOf(initial);
        _setSnapshot(_snapshotOf(initial));
        _cellsView = _cells.view();
        _cellsView.onChange(From.ALL, it -> {
            Tuple<Tuple<E>> newCells = it.currentValue().orElseGet(TuplePropertyTableModel::_emptyCells);
            _publishToUIThread(() -> _applyNewCells(newCells));
        });
    }

    /**
     *  Applies a new cells tuple to the UI thread owned snapshot and fires the
     *  most targeted table events the {@link SequenceDiff} allows. Always runs
     *  on the UI thread.
     */
    private void _applyNewCells( Tuple<Tuple<E>> newCells ) {
        TableSnapshot previous = _currentSnapshot();

        SequenceDiff diff     = _diffOf(newCells);
        SequenceDiff lastDiff = _lastDiff;
        _lastDiff = diff;

        TableSnapshot next = _snapshotOf(newCells);
        if ( next.equals(previous) )
            return; // Nothing about the table contents changed, so there is nothing to fire.

        _setSnapshot(next); // Swap first, so that the events below observe the new state.

        boolean structureChanged = previous == null || next.getColumnCount() != previous.getColumnCount();

        if ( !_layout.isRowMajor() ) {
            // The outer tuple describes columns, so its diff says nothing about row ranges.
            if ( structureChanged )
                _fireEverythingChanged();
            else
                fireTableDataChanged();
            return;
        }
        if ( structureChanged || diff == null || lastDiff == null || !diff.isDirectSuccessorOf(lastDiff) ) {
            _fireEverythingChanged();
            return;
        }
        int index = diff.index().orElse(0);
        int size  = diff.size();
        if ( size <= 0 ) {
            _fireEverythingChanged();
            return;
        }
        switch ( diff.change() ) {
            case ADD:
                fireTableRowsInserted(index, index + size - 1);
                break;
            case REMOVE:
                fireTableRowsDeleted(index, index + size - 1);
                break;
            case SET:
                fireTableRowsUpdated(index, index + size - 1);
                break;
            default:
                // RETAIN, CLEAR, SORT, DISTINCT, REVERSE, NONE and anything unknown:
                // these either affect unknown ranges or the whole table, so we rebuild.
                _fireEverythingChanged();
        }
    }

    @Override
    protected TableSnapshot takeLiveSnapshot() {
        return _snapshotOf(_cellsOrEmpty());
    }

    private static @Nullable SequenceDiff _diffOf( Tuple<?> cells ) {
        if ( cells instanceof SequenceDiffOwner )
            return ((SequenceDiffOwner) cells).differenceFromPrevious().orElse(null);
        return null;
    }

    @SuppressWarnings("unchecked")
    private TableSnapshot _snapshotOf( Tuple<Tuple<E>> cells ) {
        // The cell tuples are immutable and only ever read, so this cast is safe:
        return TableSnapshot.of(_layout, (Tuple<Tuple<@Nullable Object>>)(Tuple<?>) cells);
    }

    private Tuple<Tuple<E>> _cellsOrEmpty() {
        Tuple<Tuple<E>> cells = _cells.orElseNull();
        return cells == null ? _emptyCells() : cells;
    }

    @SuppressWarnings("unchecked")
    private static <E extends @Nullable Object> Tuple<Tuple<E>> _emptyCells() {
        return Tuple.of((Class<Tuple<E>>)(Class<?>) Tuple.class);
    }

    // ---- Live accessors (only used if there is no snapshot, which for this model is never). ----

    @Override protected int _liveRowCount() {
        return _layout.isRowMajor() ? _cellsOrEmpty().size() : _longestLine();
    }

    @Override protected int _liveColumnCount() {
        return _layout.isRowMajor() ? _longestLine() : _cellsOrEmpty().size();
    }

    private int _longestLine() {
        int longest = 0;
        for ( Tuple<E> line : _cellsOrEmpty() )
            longest = Math.max(longest, line.size());
        return longest;
    }

    @Override protected @Nullable Object _liveValueAt( int rowIndex, int columnIndex ) {
        int majorIndex = _layout.isRowMajor() ? rowIndex    : columnIndex;
        int minorIndex = _layout.isRowMajor() ? columnIndex : rowIndex;
        Tuple<Tuple<E>> cells = _cellsOrEmpty();
        if ( majorIndex < 0 || majorIndex >= cells.size() )
            return null;
        Tuple<E> line = cells.get(majorIndex);
        if ( minorIndex < 0 || minorIndex >= line.size() )
            return null;
        return line.get(minorIndex);
    }

    @Override protected @Nullable String _liveColumnName( int columnIndex ) { return null; }
    @Override protected Class<?> _liveColumnClass( int columnIndex ) { return Object.class; }

    /**
     *  A tuple based table is only editable if its layout says so and if the
     *  cells actually live in a mutable {@link Var} which can receive the edit.
     *  (Note that a read only property may well be a {@link Var} instance,
     *  which is why its mutability has to be checked explicitly.)
     */
    @Override protected boolean _liveCellEditable( int rowIndex, int columnIndex ) {
        return _layout.isEditable() && _cells instanceof Var && _cells.isMutable();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void _liveSetValueAt( @Nullable Object value, int rowIndex, int columnIndex ) {
        if ( !_liveCellEditable(rowIndex, columnIndex) )
            return; // A read only source (or layout) cannot receive edits.
        int majorIndex = _layout.isRowMajor() ? rowIndex    : columnIndex;
        int minorIndex = _layout.isRowMajor() ? columnIndex : rowIndex;
        ((Var<Tuple<Tuple<E>>>) _cells).update(From.VIEW, cells -> {
            if ( majorIndex < 0 || majorIndex >= cells.size() )
                return cells; // The cells changed in the meantime; the edit no longer applies.
            Tuple<E> line = cells.get(majorIndex);
            if ( minorIndex < 0 || minorIndex >= line.size() )
                return cells;
            return cells.setAt(majorIndex, line.setAt(minorIndex, NullUtil.fakeNonNull((E) value)));
        });
    }
}
