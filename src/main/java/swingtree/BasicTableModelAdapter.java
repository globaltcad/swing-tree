package swingtree;

import org.jspecify.annotations.Nullable;
import swingtree.api.model.BasicTableModel;

import java.util.Objects;

/**
 *  Wraps a user supplied {@link BasicTableModel} so that it gains the thread safety
 *  of {@link AbstractSnapshotTableModel} without the user ever having to extend
 *  anything: a {@link BasicTableModel} describes <i>where the data lives</i>, which is
 *  exactly what the {@code _liveXxx} accessors of the base class ask for, so the two
 *  compose directly.
 *  <p>
 *  This is also how the model learns that its data changed: whatever the delegate
 *  signals through {@link BasicTableModel#addTableModelListener(javax.swing.event.TableModelListener)}
 *  (which is what {@link BasicTableModel.Builder#updateOn(sprouts.Observable)} raises)
 *  is translated into a {@link #refresh()}, which re-reads the delegate on the thread
 *  owning it and publishes the result to the UI thread.
 */
final class BasicTableModelAdapter extends AbstractSnapshotTableModel
{
    private final BasicTableModel _delegate;

    BasicTableModelAdapter( BasicTableModel delegate ) {
        _delegate = Objects.requireNonNull(delegate);
        /*
            Note that the delegate holds this adapter strongly for as long as it lives,
            and the adapter is what the JTable holds as its model, so the two share the
            lifetime of the table. A delegate which does not signal changes at all (the
            default) simply never calls back.
         */
        _delegate.addTableModelListener( event -> refresh() );
    }

    @Override protected int _liveRowCount() { return _delegate.getRowCount(); }
    @Override protected int _liveColumnCount() { return _delegate.getColumnCount(); }
    @Override protected @Nullable Object _liveValueAt( int rowIndex, int columnIndex ) { return _delegate.getValueAt(rowIndex, columnIndex); }
    @Override protected @Nullable String _liveColumnName( int columnIndex ) { return _delegate.getColumnName(columnIndex); }
    @Override protected Class<?> _liveColumnClass( int columnIndex ) { return _delegate.getColumnClass(columnIndex); }
    @Override protected boolean _liveCellEditable( int rowIndex, int columnIndex ) { return _delegate.isCellEditable(rowIndex, columnIndex); }
    @Override protected void _liveSetValueAt( @Nullable Object value, int rowIndex, int columnIndex ) { _delegate.setValueAt(NullUtil.fakeNonNull(value), rowIndex, columnIndex); }
}
