package swingtree.api.model;

import org.jspecify.annotations.Nullable;
import sprouts.Tuple;
import swingtree.UI;

import java.util.Objects;

/**
 *  An immutable, internally consistent value based view of the contents of a
 *  table model.
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
 *  A snapshot is a proper value: two snapshots with the same layout, the same
 *  dimensions and the same contents are {@link #equals(Object)} to each other
 *  (and have the same {@link #hashCode()}), which is what allows a model to tell
 *  whether a refresh actually changed anything.
 *  <p>
 *  The {@link UI.ListData} layout of a snapshot determines how its
 *  {@link #cells()} are interpreted: in a {@link UI.ListData#ROW_MAJOR} snapshot
 *  the outer {@link Tuple} holds the rows and each inner {@link Tuple} holds the
 *  cells of a row, whereas in a {@link UI.ListData#COLUMN_MAJOR} snapshot the
 *  outer {@link Tuple} holds the columns and each inner {@link Tuple} holds the
 *  cells of a column. Reading through {@link #getValueAt(int, int)} is always in
 *  {@code (row, column)} terms, irrespective of the layout.
 *  <p>
 *  This is the table analogue of the UI thread owned snapshot that the combo
 *  box models maintain (see {@code swingtree.AbstractComboModel}).
 */
public final class TableSnapshot
{
    @SuppressWarnings("unchecked")
    private static final TableSnapshot EMPTY = new TableSnapshot(
            UI.ListData.ROW_MAJOR,
            0,
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
     *  Creates a new snapshot whose dimensions are derived from the supplied
     *  cells: the major axis of the {@code layout} is as long as the outer
     *  {@link Tuple}, and the minor axis is as long as the longest inner
     *  {@link Tuple} (so a ragged matrix is padded with {@code null} cells).
     *  The columns have no explicit names or classes, which makes a table fall
     *  back to the default (spreadsheet style) names and to {@link Object}.
     *
     * @param layout The {@link UI.ListData} layout describing how {@code cells} is to be interpreted.
     * @param cells  The cell values, an immutable tuple of rows (or columns, see {@code layout}),
     *               each of which is an immutable tuple of cell values.
     * @return A new {@link TableSnapshot}.
     */
    @SuppressWarnings("unchecked")
    public static TableSnapshot of(
        UI.ListData                    layout,
        Tuple<Tuple<@Nullable Object>> cells
    ) {
        Objects.requireNonNull(layout);
        Objects.requireNonNull(cells);
        int majorCount = cells.size();
        int minorCount = 0;
        for ( Tuple<@Nullable Object> line : cells )
            minorCount = Math.max(minorCount, line.size());
        return new TableSnapshot(
            layout,
            layout.isRowMajor() ? majorCount : minorCount,
            layout.isRowMajor() ? minorCount : majorCount,
            Tuple.ofNullable(String.class),
            Tuple.of((Class<Class<?>>)(Class<?>) Class.class),
            cells
        );
    }

    /**
     *  Creates a new snapshot from the fully resolved contents of a table.
     *  The supplied metadata tuples are expected to hold exactly one entry per
     *  column (already resolved to what the model would report for that column),
     *  and the two counts are what the table will report as its dimensions,
     *  irrespective of how many cells the {@code cells} tuple actually holds
     *  (cells outside of it simply read as {@code null}).
     *
     * @param layout        The {@link UI.ListData} layout describing how {@code cells} is to be interpreted.
     * @param rowCount      The number of rows of the table.
     * @param columnCount   The number of columns of the table.
     * @param columnNames   The resolved column names, one per column.
     * @param columnClasses The resolved column classes, one per column.
     * @param cells         The cell values, an immutable tuple of rows (or columns, see {@code layout}),
     *                      each of which is an immutable tuple of cell values.
     * @return A new {@link TableSnapshot}.
     */
    public static TableSnapshot of(
        UI.ListData                    layout,
        int                            rowCount,
        int                            columnCount,
        Tuple<@Nullable String>        columnNames,
        Tuple<Class<?>>                columnClasses,
        Tuple<Tuple<@Nullable Object>> cells
    ) {
        return new TableSnapshot(layout, rowCount, columnCount, columnNames, columnClasses, cells);
    }

    private final UI.ListData                    _layout;
    private final int                            _rowCount;
    private final int                            _columnCount;
    private final Tuple<@Nullable String>        _columnNames;
    private final Tuple<Class<?>>                _columnClasses;
    private final Tuple<Tuple<@Nullable Object>> _cells;

    private TableSnapshot(
        UI.ListData                    layout,
        int                            rowCount,
        int                            columnCount,
        Tuple<@Nullable String>        columnNames,
        Tuple<Class<?>>                columnClasses,
        Tuple<Tuple<@Nullable Object>> cells
    ) {
        _layout        = Objects.requireNonNull(layout);
        _rowCount      = Math.max(0, rowCount);
        _columnCount   = Math.max(0, columnCount);
        _columnNames   = Objects.requireNonNull(columnNames);
        _columnClasses = Objects.requireNonNull(columnClasses);
        _cells         = Objects.requireNonNull(cells);
    }

    /**
     *  The layout describing how the {@link #cells()} of this snapshot are
     *  to be interpreted.
     *  @return The {@link UI.ListData} layout of this snapshot.
     */
    public UI.ListData layout() {
        return _layout;
    }

    /** Returns the number of rows in this snapshot. @return The row count. */
    public int getRowCount() {
        return _rowCount;
    }

    /** Returns the number of columns in this snapshot. @return The column count. */
    public int getColumnCount() {
        return _columnCount;
    }

    /**
     *  Reads the value of a single cell, always in {@code (row, column)} terms,
     *  irrespective of the {@link #layout()} of this snapshot.
     * @param rowIndex    The row index of the cell.
     * @param columnIndex The column index of the cell.
     * @return The value of the cell, or {@code null} if the indices are out of bounds.
     */
    public @Nullable Object getValueAt( int rowIndex, int columnIndex ) {
        if ( rowIndex < 0 || rowIndex >= _rowCount || columnIndex < 0 || columnIndex >= _columnCount )
            return null;
        int majorIndex = _layout.isRowMajor() ? rowIndex    : columnIndex;
        int minorIndex = _layout.isRowMajor() ? columnIndex : rowIndex;
        if ( majorIndex >= _cells.size() )
            return null;
        Tuple<@Nullable Object> line = _cells.get(majorIndex);
        if ( minorIndex >= line.size() )
            return null;
        return line.get(minorIndex);
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
     *  The cell values of this snapshot, in the order described by its
     *  {@link #layout()}. This is exposed so that models based on a {@link Tuple}
     *  of rows can reuse the very same immutable structure without copying it.
     *  @return The immutable tuple of rows (or columns, see {@link #layout()}).
     */
    public Tuple<Tuple<@Nullable Object>> cells() {
        return _cells;
    }

    /**
     *  The column names of this snapshot, where a {@code null} entry means that
     *  the table should fall back to the default (spreadsheet style) column name.
     *  @return The immutable tuple of column names.
     */
    public Tuple<@Nullable String> columnNames() {
        return _columnNames;
    }

    /**
     *  The column classes of this snapshot, which a {@link javax.swing.JTable}
     *  consults to pick a renderer and editor for each column.
     *  @return The immutable tuple of column classes.
     */
    public Tuple<Class<?>> columnClasses() {
        return _columnClasses;
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
        if ( rowIndex < 0 || rowIndex >= _rowCount || columnIndex < 0 || columnIndex >= _columnCount )
            return this;
        int majorIndex = _layout.isRowMajor() ? rowIndex    : columnIndex;
        int minorIndex = _layout.isRowMajor() ? columnIndex : rowIndex;
        if ( majorIndex >= _cells.size() )
            return this;
        Tuple<@Nullable Object> line = _cells.get(majorIndex);
        if ( minorIndex >= line.size() )
            return this;
        if ( Objects.equals(line.get(minorIndex), value) )
            return this;
        Tuple<@Nullable Object> newLine = line.setAt(minorIndex, value);
        return new TableSnapshot(
                    _layout, _rowCount, _columnCount,
                    _columnNames, _columnClasses,
                    _cells.setAt(majorIndex, newLine)
                );
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == this ) return true;
        if ( !(obj instanceof TableSnapshot) ) return false;
        TableSnapshot other = (TableSnapshot) obj;
        return _layout      == other._layout
            && _rowCount    == other._rowCount
            && _columnCount == other._columnCount
            && _columnNames.equals(other._columnNames)
            && _columnClasses.equals(other._columnClasses)
            && _cells.equals(other._cells);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_layout, _rowCount, _columnCount, _columnNames, _columnClasses, _cells);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" +
                    "layout="      + _layout        + ", " +
                    "rowCount="    + _rowCount      + ", " +
                    "columnCount=" + _columnCount   + ", " +
                    "columnNames=" + _columnNames   + ", " +
                    "cells="       + _cells         +
                "]";
    }
}
