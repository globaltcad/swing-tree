package swingtree;

import org.jspecify.annotations.Nullable;
import sprouts.From;
import sprouts.Tuple;
import sprouts.Val;
import sprouts.Var;
import sprouts.Viewable;
import sprouts.impl.SequenceDiff;
import sprouts.impl.SequenceDiffOwner;
import swingtree.api.model.TableSnapshot;

import java.util.Objects;

/**
 *  A thread safe, reactive table model whose contents are modelled by a property
 *  holding an immutable {@link TableSnapshot} value.
 *  <p>
 *  This is the table analogue of {@code swingtree.TuplePropertyComboModel}, and it
 *  is the single model behind every property based table binding: a {@link Tuple}
 *  of {@link Tuple}s of cells is simply
 *  {@linkplain Val#viewAs(Class, java.util.function.Function) viewed}
 *  (or {@linkplain Var#zoomTo(Class, sprouts.Lens) zoomed into}, if the cells are
 *  editable) as a {@link TableSnapshot} property before it reaches this model.
 *  <p>
 *  Because a {@link TableSnapshot} is a deeply immutable value, the two threads can
 *  publish it to each other without any copying: the property value <em>is</em> the
 *  UI thread owned snapshot. When the property changes on the application thread,
 *  the new snapshot is handed to the UI thread, which swaps it in and fires the
 *  table events.
 *  <p>
 *  Crucially, for a row major snapshot this model exploits the {@link SequenceDiff}
 *  carried by the snapshot's {@link TableSnapshot#cells()} (mirroring
 *  {@code UIForTabbedPane._updateTabs}): if a change is a direct successor of the
 *  previous one, only the affected rows are refreshed through targeted
 *  {@link #fireTableRowsInserted(int, int)} / {@link #fireTableRowsDeleted(int, int)} /
 *  {@link #fireTableRowsUpdated(int, int)} events. This keeps updates to large tables
 *  cheap (O(changes) instead of a full rebuild), which is exactly why a row major
 *  source should be preferred for dynamic, application thread owned table data.
 *  (For a column major source the cells tuple describes columns, so a change to it
 *  never maps onto a row range, which is why such a model falls back to refreshing
 *  all rows at once.)
 */
final class SnapshotPropertyTableModel extends AbstractSnapshotTableModel
{
    private final Val<TableSnapshot> _model;
    private final Viewable<TableSnapshot> _modelView; // A strong reference keeps the (weakly parented) view and its listener alive.
    private volatile @Nullable SequenceDiff _lastDiff; // Read/written on the UI thread only (plus the initial value from the installing thread).

    SnapshotPropertyTableModel( Val<TableSnapshot> model ) {
        _model = Objects.requireNonNull(model);
        TableSnapshot initial = _modelOrEmpty();
        _lastDiff = _diffOf(initial);
        _setSnapshot(initial);
        _modelView = _model.view();
        _modelView.onChange(From.ALL, it -> {
            TableSnapshot next = it.currentValue().orElseGet(TableSnapshot::empty);
            _publishToUIThread(() -> _applyNewSnapshot(next));
        });
    }

    /**
     *  Applies a new snapshot and fires the most targeted table events the
     *  {@link SequenceDiff} allows. Always runs on the UI thread.
     */
    private void _applyNewSnapshot( TableSnapshot next ) {
        TableSnapshot previous = _currentSnapshot();

        SequenceDiff diff     = _diffOf(next);
        SequenceDiff lastDiff = _lastDiff;
        _lastDiff = diff;

        if ( next.equals(previous) )
            return; // Nothing about the table contents changed, so there is nothing to fire.

        _setSnapshot(next); // Swap first, so that the events below observe the new state.

        if ( _structureChanged(previous, next) ) {
            _fireEverythingChanged();
            return;
        }
        if ( !next.layout().isRowMajor() ) {
            // The cells tuple describes columns, so its diff says nothing about row ranges.
            fireTableDataChanged();
            return;
        }
        if ( diff == null || lastDiff == null || !diff.isDirectSuccessorOf(lastDiff) ) {
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

    /**
     *  Tells if the columns of the table itself changed, which the {@link javax.swing.JTable}
     *  can only pick up through a structure change (a row event would leave a stale header
     *  or a stale set of column renderers behind).
     */
    private static boolean _structureChanged( @Nullable TableSnapshot previous, TableSnapshot next ) {
        return previous == null
            || next.getColumnCount() != previous.getColumnCount()
            || next.layout().isRowMajor() != previous.layout().isRowMajor()
            || !next.columnNames().equals(previous.columnNames())
            || !next.columnClasses().equals(previous.columnClasses());
    }

    @Override
    protected TableSnapshot takeLiveSnapshot() {
        return _modelOrEmpty();
    }

    private static @Nullable SequenceDiff _diffOf( TableSnapshot snapshot ) {
        Tuple<?> cells = snapshot.cells();
        if ( cells instanceof SequenceDiffOwner )
            return ((SequenceDiffOwner) cells).differenceFromPrevious().orElse(null);
        return null;
    }

    private TableSnapshot _modelOrEmpty() {
        TableSnapshot snapshot = _model.orElseNull();
        return snapshot == null ? TableSnapshot.empty() : snapshot;
    }

    // ---- Live accessors (only used if there is no snapshot, which for this model is never). ----

    @Override protected int _liveRowCount() { return _modelOrEmpty().getRowCount(); }
    @Override protected int _liveColumnCount() { return _modelOrEmpty().getColumnCount(); }
    @Override protected @Nullable Object _liveValueAt( int rowIndex, int columnIndex ) { return _modelOrEmpty().getValueAt(rowIndex, columnIndex); }
    @Override protected @Nullable String _liveColumnName( int columnIndex ) { return _modelOrEmpty().getColumnName(columnIndex); }
    @Override protected Class<?> _liveColumnClass( int columnIndex ) { return _modelOrEmpty().getColumnClass(columnIndex); }

    /**
     *  A snapshot based table is only editable if the layout of its snapshot says so
     *  and if the snapshot actually lives in a mutable {@link Var} which can receive
     *  the edit. (Note that a read only property may well be a {@link Var} instance,
     *  which is why its mutability has to be checked explicitly.)
     */
    @Override protected boolean _liveCellEditable( int rowIndex, int columnIndex ) {
        return _modelOrEmpty().layout().isEditable() && _model instanceof Var && _model.isMutable();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void _liveSetValueAt( @Nullable Object value, int rowIndex, int columnIndex ) {
        if ( !_liveCellEditable(rowIndex, columnIndex) )
            return; // A read only source (or layout) cannot receive edits.
        // Note that 'withValueAt' returns the snapshot unchanged if the indices went
        // out of bounds in the meantime, in which case the edit no longer applies.
        ((Var<TableSnapshot>) _model).update(From.VIEW, snapshot -> snapshot.withValueAt(rowIndex, columnIndex, value));
    }
}
