package swingtree;

import org.jspecify.annotations.Nullable;
import sprouts.Tuple;
import swingtree.api.model.TableData;
import swingtree.threading.DecoupledEventProcessor;
import swingtree.threading.EventProcessor;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *  A {@link javax.swing.table.TableModel} base class which makes SwingTree tables
 *  thread safe by following the same snapshot protocol as the combo box models
 *  (see {@link AbstractComboModel}).
 *  <p>
 *  This class is deliberately package private: SwingTree favours composition over
 *  inheritance, which is why a user supplied data source is always <i>wrapped</i> by
 *  one of the concrete models in this package (see {@link BasicTableModelAdapter})
 *  instead of the user being asked to extend anything.
 *  <p>
 *  The problem this solves: a plain lambda or collection based table model reads
 *  its application thread owned data source live, whenever the UI thread (the AWT
 *  Event Dispatch Thread) paints the table. Under
 *  {@link EventProcessor#DECOUPLED}, the application thread may mutate that data
 *  source at the very same moment, which produces torn rows, phantom nulls or
 *  index exceptions in the middle of painting.
 *  <p>
 *  The fix is <b>adaptive</b>:
 *  <ul>
 *      <li>Under the single threaded {@link EventProcessor#COUPLED} /
 *          {@link EventProcessor#COUPLED_STRICT} processors, the model reads its
 *          data source <em>live</em> (through the {@code _liveXxx} accessors),
 *          exactly like before, with zero added overhead.</li>
 *      <li>Under {@link EventProcessor#DECOUPLED}, the model maintains a UI thread
 *          owned {@link TableData}. All UI thread reads are served from that
 *          immutable snapshot. When the data changes (see {@link #refresh()}),
 *          a fresh snapshot is taken <em>on the application thread</em> (which
 *          owns the data) and then published to the UI thread, where it is
 *          swapped in and the appropriate table events are fired.</li>
 *  </ul>
 *  Which of the two behaviours is active is decided by the {@link EventProcessor}
 *  handed to this model at install time through {@link #_setEventProcessor(EventProcessor)}.
 */
abstract class AbstractSnapshotTableModel extends AbstractTableModel
{
    /**
     *  The UI thread owned snapshot of the table contents. It is {@code null}
     *  in the coupled case (live reads) and non-null in the decoupled case.
     *  It is {@code volatile} because it is written by the UI thread and,
     *  during {@link #_setEventProcessor(EventProcessor)}, possibly read across
     *  the thread that installs the model.
     */
    private volatile @Nullable TableData _snapshot = null;
    private volatile EventProcessor _eventProcessor = EventProcessor.COUPLED;

    /**
     *  Gives this model the {@link EventProcessor} of the UI declaration it is
     *  installed in. If that processor is decoupled, an initial snapshot is taken
     *  right away so that the UI thread never reads the live data source under
     *  the decoupled protocol.
     *
     * @param eventProcessor The event processor of the enclosing UI declaration.
     */
    final void _setEventProcessor( EventProcessor eventProcessor ) {
        _eventProcessor = Objects.requireNonNull(eventProcessor);
        if ( _isDecoupled() && _snapshot == null )
            _snapshot = takeLiveSnapshot();
    }

    /** Tells if the app and UI threads are decoupled. @return True if the current event processor decouples the application thread from the UI thread. */
    protected final boolean _isDecoupled() {
        return _eventProcessor instanceof DecoupledEventProcessor;
    }

    /** Hands a task over to the UI thread of the current {@link EventProcessor}. */
    protected final void _publishToUIThread( Runnable uiTask ) {
        _eventProcessor.registerUIEvent(uiTask);
    }

    /** Hands a task over to the application thread of the current {@link EventProcessor}. */
    protected final void _publishToAppThread( Runnable appTask ) {
        _eventProcessor.registerAppEvent(appTask);
    }

    /**
     *  Directly exposes the current UI thread owned snapshot, or {@code null}
     *  if the model is currently reading its data source live (coupled mode).
     *  @return The current snapshot, or {@code null}.
     */
    protected final @Nullable TableData _currentSnapshot() {
        return _snapshot;
    }

    /** Swaps in a new UI thread owned snapshot. Must be called on the UI thread. */
    protected final void _setSnapshot( @Nullable TableData snapshot ) {
        _snapshot = snapshot;
    }

    /**
     *  Refreshes the table from its data source. This is the entry point that the
     *  update bindings ({@code updateOn} / {@code updateTableOn}) call when the
     *  data source signalled a change.
     *  <ul>
     *      <li>Decoupled: a fresh snapshot is taken on the calling (application)
     *          thread, then swapped in and fired on the UI thread. If this is
     *          called by the UI thread instead (because the update signal was
     *          raised in UI code), then the snapshot is first handed to the
     *          application thread, which owns the data source.</li>
     *      <li>Coupled: the model reads live, so it merely fires the change
     *          events on the UI thread.</li>
     *  </ul>
     */
    void refresh() {
        if ( _isDecoupled() ) {
            if ( UI.thisIsUIThread() ) {
                // The data source belongs to the application thread, so we do not
                // read it here. Note that this does not wait for that thread.
                _publishToAppThread(this::refresh);
                return;
            }
            TableData next = takeLiveSnapshot();
            _publishToUIThread(() -> {
                _snapshot = next;
                _fireEverythingChanged();
            });
        } else {
            _publishToUIThread(this::_fireEverythingChanged);
        }
    }

    /**
     *  Fires a table structure change followed by a table data change,
     *  which is the most thorough (if blunt) way to tell a {@link javax.swing.JTable}
     *  to rebuild itself completely. Subclasses with finer grained change
     *  information (like a {@code Tuple} based model) fire more targeted events instead.
     */
    protected final void _fireEverythingChanged() {
        fireTableStructureChanged();
        fireTableDataChanged();
    }

    /**
     *  Builds a fresh {@link TableData} by reading the live data source of
     *  this model through the {@code _liveXxx} accessors. This is called on the
     *  thread that owns the data (the application thread under the decoupled
     *  protocol). Subclasses whose data source is already immutable (like a
     *  {@link Tuple}) may override this to avoid the per-cell copy.
     *  <p>
     *  Note that the {@code _liveXxx} accessors already speak in {@code (row, column)}
     *  terms (a column major data source transposes in its accessors), which is why
     *  the snapshot taken here is always {@link UI.ListData#ROW_MAJOR}.
     *
     * @return A new immutable snapshot of the current table contents.
     */
    @SuppressWarnings("unchecked")
    protected TableData takeLiveSnapshot() {
        int cols = Math.max(0, _liveColumnCount());
        int rows = Math.max(0, _liveRowCount());
        List<@Nullable String> names   = new ArrayList<>(cols);
        List<Class<?>>         classes = new ArrayList<>(cols);
        for ( int c = 0; c < cols; c++ ) {
            names.add(_liveColumnName(c));
            classes.add(_liveColumnClass(c));
        }
        List<Tuple<@Nullable Object>> rowTuples = new ArrayList<>(rows);
        for ( int r = 0; r < rows; r++ ) {
            List<@Nullable Object> cells = new ArrayList<>(cols);
            for ( int c = 0; c < cols; c++ )
                cells.add(_liveValueAt(r, c));
            rowTuples.add(Tuple.ofNullable(Object.class, cells));
        }
        /*
            Note that the dimensions of the result are derived from what we hand over
            here: the column metadata has one entry per column, which is what keeps a
            table with columns but no rows as wide as it should be.
         */
        return TableData.of(
            UI.ListData.ROW_MAJOR,
            Tuple.ofNullable(String.class, names),
            Tuple.of((Class<Class<?>>)(Class<?>) Class.class, classes),
            Tuple.of((Class<Tuple<@Nullable Object>>)(Class<?>) Tuple.class, rowTuples)
        );
    }

    // ---- TableModel reads: snapshot when present, otherwise live. ----

    @Override public int getRowCount() {
        TableData snap = _snapshot;
        return snap == null ? Math.max(0, _liveRowCount()) : snap.getRowCount();
    }

    @Override public int getColumnCount() {
        TableData snap = _snapshot;
        return snap == null ? Math.max(0, _liveColumnCount()) : snap.getColumnCount();
    }

    @Override public @Nullable Object getValueAt( int rowIndex, int columnIndex ) {
        TableData snap = _snapshot;
        return snap == null ? _liveValueAt(rowIndex, columnIndex) : snap.getValueAt(rowIndex, columnIndex);
    }

    @Override public String getColumnName( int columnIndex ) {
        TableData snap = _snapshot;
        String name = snap == null ? _liveColumnName(columnIndex) : snap.getColumnName(columnIndex);
        return name == null ? super.getColumnName(columnIndex) : name;
    }

    @Override public Class<?> getColumnClass( int columnIndex ) {
        TableData snap = _snapshot;
        return snap == null ? _liveColumnClass(columnIndex) : snap.getColumnClass(columnIndex);
    }

    @Override public boolean isCellEditable( int rowIndex, int columnIndex ) {
        // Editability is a per-cell predicate consulted by the JTable on the UI
        // thread only when the user initiates editing, not during bulk painting,
        // so we let it read live (it typically reads near static configuration).
        return _liveCellEditable(rowIndex, columnIndex);
    }

    @Override public void setValueAt( @Nullable Object value, int rowIndex, int columnIndex ) {
        if ( _isDecoupled() ) {
            /*
                A cell edit made by the user is applied to the UI thread owned snapshot
                right away, so that the table does not flicker back to the old value
                while the application thread catches up. We only do this for an edit
                which the data source will actually accept, and only on the UI thread,
                which is the thread owning both the snapshot and the events below.
                Any other write (a programmatic one, say) simply travels to the data
                source, whose change signal then refreshes the snapshot as usual.
             */
            TableData snap = _snapshot;
            if ( snap != null && UI.thisIsUIThread() && _liveCellEditable(rowIndex, columnIndex) ) {
                TableData next = snap.setCellAt(rowIndex, columnIndex, value);
                if ( next != snap ) {
                    _snapshot = next;
                    fireTableCellUpdated(rowIndex, columnIndex);
                }
            }
            _publishToAppThread(() -> _liveSetValueAt(value, rowIndex, columnIndex));
        } else {
            _liveSetValueAt(value, rowIndex, columnIndex);
        }
    }

    // ---- The live data source accessors implemented by concrete models. ----

    /** Reads the row count live. @return The current row count read straight from the data source. */
    protected abstract int _liveRowCount();
    /** Reads the column count live. @return The current column count read straight from the data source. */
    protected abstract int _liveColumnCount();
    /** Reads a cell value live. @return The current cell value read straight from the data source. */
    protected abstract @Nullable Object _liveValueAt( int rowIndex, int columnIndex );
    /** Reads a column name live. @return The current column name read straight from the data source, or {@code null} for a default. */
    protected abstract @Nullable String _liveColumnName( int columnIndex );
    /** Reads a column class live. @return The current column class read straight from the data source. */
    protected abstract Class<?> _liveColumnClass( int columnIndex );
    /** Reads a cell's editability live. @return Whether the given cell is currently editable according to the data source. */
    protected abstract boolean _liveCellEditable( int rowIndex, int columnIndex );
    /** Writes a user edit straight into the data source (invoked on the application thread when decoupled). */
    protected abstract void _liveSetValueAt( @Nullable Object value, int rowIndex, int columnIndex );
}
