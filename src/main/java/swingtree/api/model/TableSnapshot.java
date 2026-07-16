package swingtree.api.model;

import org.jspecify.annotations.Nullable;
import sprouts.Tuple;

/**
 *  An immutable, internally consistent view of the contents of a table model.
 *  <p>
 *  In SwingTree's decoupled threading mode, the UI thread (the AWT Event
 *  Dispatch Thread) never reads the application thread owned data source of a
 *  table directly. Instead it reads one of these snapshots, which is published
 *  to it by the application thread. Because a {@link TableSnapshot} is deeply
 *  immutable (its cells are stored in immutable {@link Tuple}s), it can be
 *  handed between the two threads without any locking, and every read within a
 *  single UI thread task is guaranteed to see the same consistent state, even
 *  while the application thread mutates the underlying data source.
 *  <p>
 *  This is the table analogue of the UI thread owned snapshot that the combo
 *  box models maintain (see {@code swingtree.AbstractComboModel}).
 */
public final class TableSnapshot
{
    @SuppressWarnings("unchecked")
    private static final TableSnapshot EMPTY = new TableSnapshot(
            0,
            Tuple.ofNullable(String.class),
            Tuple.of((Class<Class<?>>)(Class<?>) Class.class),
            Tuple.of(Tuple.classTyped(Object.class))
        );

    /**
     *  The shared empty snapshot, representing a table with no rows and no columns.
     *  @return An empty {@link TableSnapshot}.
     */
    public static TableSnapshot empty() {
        return EMPTY;
    }

    /**
     *  Creates a new snapshot from the fully resolved contents of a table.
     *  The supplied metadata tuples are expected to hold exactly one entry per
     *  column (already resolved to what the model would report for that column).
     *
     * @param columnCount   The number of columns of the table.
     * @param columnNames   The resolved column names, one per column.
     * @param columnClasses The resolved column classes, one per column.
     * @param rows          The row major cell values, an immutable tuple of rows,
     *                      each of which is an immutable tuple of cell values.
     * @return A new {@link TableSnapshot}.
     */
    public static TableSnapshot of(
        int                                 columnCount,
        Tuple<@Nullable String>             columnNames,
        Tuple<Class<?>>                     columnClasses,
        Tuple<Tuple<@Nullable Object>>      rows
    ) {
        return new TableSnapshot(columnCount, columnNames, columnClasses, rows);
    }

    private final int                            _columnCount;
    private final Tuple<@Nullable String>        _columnNames;
    private final Tuple<Class<?>>                _columnClasses;
    private final Tuple<Tuple<@Nullable Object>> _rows;

    private TableSnapshot(
        int                                 columnCount,
        Tuple<@Nullable String>             columnNames,
        Tuple<Class<?>>                     columnClasses,
        Tuple<Tuple<@Nullable Object>>      rows
    ) {
        _columnCount   = Math.max(0, columnCount);
        _columnNames   = columnNames;
        _columnClasses = columnClasses;
        _rows          = rows;
    }

    /** Returns the number of rows in this snapshot. @return The row count. */
    public int getRowCount() {
        return _rows.size();
    }

    /** Returns the number of columns in this snapshot. @return The column count. */
    public int getColumnCount() {
        return _columnCount;
    }

    /**
     *  Reads the value of a single cell.
     * @param rowIndex    The row index of the cell.
     * @param columnIndex The column index of the cell.
     * @return The value of the cell, or {@code null} if the indices are out of bounds.
     */
    public @Nullable Object getValueAt( int rowIndex, int columnIndex ) {
        if ( rowIndex < 0 || rowIndex >= _rows.size() )
            return null;
        Tuple<@Nullable Object> row = _rows.get(rowIndex);
        if ( columnIndex < 0 || columnIndex >= row.size() )
            return null;
        return row.get(columnIndex);
    }

    /**
     *  Reads the resolved name of a column.
     * @param columnIndex The column index.
     * @return The column name, or {@code null} if the index is out of bounds.
     */
    public @Nullable String getColumnName( int columnIndex ) {
        if ( columnIndex < 0 || columnIndex >= _columnNames.size() )
            return null;
        return _columnNames.get(columnIndex);
    }

    /**
     *  Reads the resolved class of a column.
     * @param columnIndex The column index.
     * @return The column class, or {@link Object} if the index is out of bounds.
     */
    public Class<?> getColumnClass( int columnIndex ) {
        if ( columnIndex < 0 || columnIndex >= _columnClasses.size() )
            return Object.class;
        return _columnClasses.get(columnIndex);
    }

    /**
     *  The row major cell values of this snapshot.
     *  This is exposed so that models based on a {@link Tuple} of rows can
     *  reuse the very same immutable structure without copying it.
     *  @return The immutable tuple of rows.
     */
    public Tuple<Tuple<@Nullable Object>> rows() {
        return _rows;
    }

    /**
     *  Produces a new snapshot in which a single cell has a new value,
     *  reusing the immutable structure of this snapshot for everything else.
     *  This is used to apply a user edit to the UI thread owned snapshot right
     *  away, before the edit is handed over to the application thread.
     *
     * @param rowIndex    The row index of the cell to change.
     * @param columnIndex The column index of the cell to change.
     * @param value       The new value of the cell.
     * @return A new {@link TableSnapshot} with the changed cell,
     *         or this snapshot unchanged if the indices are out of bounds.
     */
    public TableSnapshot withValueAt( int rowIndex, int columnIndex, @Nullable Object value ) {
        if ( rowIndex < 0 || rowIndex >= _rows.size() )
            return this;
        Tuple<@Nullable Object> row = _rows.get(rowIndex);
        if ( columnIndex < 0 || columnIndex >= row.size() )
            return this;
        Tuple<@Nullable Object> newRow = row.setAt(columnIndex, value);
        return new TableSnapshot(_columnCount, _columnNames, _columnClasses, _rows.setAt(rowIndex, newRow));
    }
}
