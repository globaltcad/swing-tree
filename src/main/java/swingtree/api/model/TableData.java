package swingtree.api.model;

import org.jspecify.annotations.Nullable;
import sprouts.Tuple;
import swingtree.UI;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *  An immutable value describing the entire contents of a table: its cells, the
 *  name and class of every column, the {@link UI.CellOrder} tying the two together,
 *  and whether the user may edit any of it. Hold one of these in a {@link sprouts.Var}
 *  property, bind that property to a table, and you have a complete, thread safe,
 *  reactive table:
 *  <pre>{@code
 *  Var<TableData> data = Var.of(
 *          TableData.of(UI.CellOrder.ROW_MAJOR, "Name", "Age")
 *              .addRow("Alice", 30)
 *              .addRow("Bob",   42)
 *      );
 *
 *  UI.table(data);
 *  }</pre>
 *  Changing the table is then simply a matter of changing the value in the property,
 *  from wherever your business logic happens to live:
 *  <pre>{@code
 *  data.update( it -> it.addRow("Carol", 55) );
 *  }</pre>
 *
 *  <h2>It is a value</h2>
 *
 *  Every method on this class which sounds like it changes something really returns
 *  a <i>new</i> {@link TableData}, leaving the one you called it on untouched. Two
 *  tables with the same cell order, editability, columns and cells are
 *  {@link #equals(Object)} to each
 *  other and have the same {@link #hashCode()}, which is what lets a table tell
 *  whether anything actually changed.
 *  <p>
 *  Do not let the copying scare you: the cells live in immutable {@link Tuple}s,
 *  which share their structure between versions. Adding a row to a thousand row
 *  table does not copy a thousand rows.
 *
 *  <h2>It is thread safe</h2>
 *
 *  Because this is a deeply immutable value, the application thread and the UI
 *  thread (the AWT Event Dispatch Thread) can hand it to each other without any
 *  locking. Under {@link swingtree.threading.EventProcessor#DECOUPLED}, the value
 *  in your property <i>is</i> what the UI thread reads, so every read within a
 *  single UI thread task sees the same consistent table, even while your
 *  application thread is busy building the next version of it.
 *
 *  <h2>It is cheap to update</h2>
 *
 *  A {@link Tuple} remembers how it was derived from its predecessor, and a
 *  {@link UI.CellOrder#ROW_MAJOR} table passes that knowledge straight to the
 *  {@link javax.swing.JTable}: adding, removing or changing a handful of rows only
 *  repaints those rows, rather than rebuilding the whole table. This is why a row
 *  major cell order is the right default for large, frequently changing tables.
 *
 *  <h2>Rows and columns, whatever the cell order</h2>
 *
 *  The {@link #cellOrder()} decides how the {@link #cells()} are <i>stored</i>: in a
 *  {@link UI.CellOrder#ROW_MAJOR} table the outer {@link Tuple} holds the rows and
 *  each inner {@link Tuple} holds the cells of one row, whereas in a
 *  {@link UI.CellOrder#COLUMN_MAJOR} table the outer {@link Tuple} holds the columns.
 *  That is a storage detail: every method on this class speaks in {@code (row, column)}
 *  terms no matter which cell order you picked, so {@link #addRow(Object...)} adds a row
 *  to a column major table just as happily (it is merely a little more work for it).
 */
public final class TableData
{
    @SuppressWarnings("unchecked")
    private static final Class<Tuple<@Nullable Object>> LINE_TYPE = (Class<Tuple<@Nullable Object>>)(Class<?>) Tuple.class;
    @SuppressWarnings("unchecked")
    private static final Class<Class<?>> CLASS_TYPE = (Class<Class<?>>)(Class<?>) Class.class;

    private static final Tuple<@Nullable Object>        EMPTY_LINE    = Tuple.ofNullable(Object.class);
    private static final Tuple<Tuple<@Nullable Object>> EMPTY_LINES   = Tuple.of(LINE_TYPE);
    private static final Tuple<@Nullable String>        EMPTY_NAMES   = Tuple.ofNullable(String.class);
    private static final Tuple<Class<?>>                EMPTY_CLASSES = Tuple.of(CLASS_TYPE);

    private static final TableData EMPTY = new TableData(
            UI.CellOrder.ROW_MAJOR, UI.Editability.READ_ONLY,
            EMPTY_NAMES, EMPTY_CLASSES, EMPTY_LINES
        );

    /**
     *  The shared empty table: no columns, no rows, not editable.
     *  A good starting point for a table you are about to build up.
     *  @return An empty {@link TableData}.
     */
    public static TableData empty() {
        return EMPTY;
    }

    /**
     *  Creates a table which has columns but no rows yet, which is how most tables
     *  start out in life:
     *  <pre>{@code
     *  TableData.of(UI.CellOrder.ROW_MAJOR, "Name", "Age", "City")
     *  }</pre>
     *  The columns are all of type {@link Object} until you say otherwise through
     *  {@link #setColumnClassAt(int, Class)}, and the table is read only until you
     *  say otherwise through {@link #asEditable()}.
     *
     * @param cellOrder   The {@link UI.CellOrder} deciding how the cells of the table
     *                    are stored, see {@link #cellOrder()}.
     * @param columnNames The names of the columns, one per column.
     * @return A new {@link TableData} with the given columns and no rows.
     */
    public static TableData of( UI.CellOrder cellOrder, String... columnNames ) {
        Objects.requireNonNull(cellOrder);
        Objects.requireNonNull(columnNames);
        List<Class<?>> classes = new ArrayList<>(columnNames.length);
        for ( int i = 0; i < columnNames.length; i++ )
            classes.add(Object.class);
        return new TableData(
                    cellOrder,
                    UI.Editability.READ_ONLY,
                    Tuple.ofNullable(String.class, columnNames),
                    Tuple.of(CLASS_TYPE, classes),
                    EMPTY_LINES
                );
    }

    /**
     *  Creates a table from nothing but its cells, whose dimensions are derived from
     *  them: the major axis of the {@code cellOrder} is as long as the outer {@link Tuple},
     *  and the minor axis is as long as the longest inner {@link Tuple} (so a ragged
     *  matrix reads as though it were padded with {@code null} cells).
     *  The columns have no names, which makes a table fall back to the default
     *  (spreadsheet style) names, and they are all of type {@link Object}.
     *  The table is read only until you say otherwise through {@link #asEditable()}.
     *
     * @param cellOrder The {@link UI.CellOrder} describing how {@code cells} is to be interpreted.
     * @param cells     The cell values, an immutable tuple of rows (or columns, see {@code cellOrder}),
     *                  each of which is an immutable tuple of cell values.
     * @return A new {@link TableData}.
     */
    public static TableData of(
        UI.CellOrder                   cellOrder,
        Tuple<Tuple<@Nullable Object>> cells
    ) {
        Objects.requireNonNull(cellOrder);
        Objects.requireNonNull(cells);
        return new TableData(cellOrder, UI.Editability.READ_ONLY, EMPTY_NAMES, EMPTY_CLASSES, cells);
    }

    /**
     *  Creates a fully described table, whose columns carry both a name and a class.
     *  <p>
     *  Note that the dimensions of the table are derived from what you pass here, and
     *  that the column metadata takes part in that: a table with three column names but
     *  rows only two cells wide still has three columns, the last of which reads as
     *  {@code null}. This is what lets a table have columns but no rows.
     *
     * @param cellOrder     The {@link UI.CellOrder} describing how {@code cells} is to be interpreted.
     * @param editability   The {@link UI.Editability} deciding whether the user may edit the cells.
     * @param columnNames   The column names, one per column, where {@code null} means
     *                      "fall back to the default (spreadsheet style) name".
     * @param columnClasses The column classes, one per column, which a {@link javax.swing.JTable}
     *                      consults to pick a renderer and an editor.
     * @param cells         The cell values, an immutable tuple of rows (or columns, see {@code cellOrder}),
     *                      each of which is an immutable tuple of cell values.
     * @return A new {@link TableData}.
     */
    public static TableData of(
        UI.CellOrder                   cellOrder,
        UI.Editability                 editability,
        Tuple<@Nullable String>        columnNames,
        Tuple<Class<?>>                columnClasses,
        Tuple<Tuple<@Nullable Object>> cells
    ) {
        return new TableData(cellOrder, editability, columnNames, columnClasses, cells);
    }

    /**
     *  Builds a single row (or column) out of plain values, which is handy when you
     *  want to hand a prepared row to {@link #addRow(Tuple)} or {@link #setRowsAt(int, Tuple)},
     *  or a prepared column to {@link #addColumn(String, Class, Tuple)}.
     *
     * @param values The cell values of the row.
     * @return An immutable tuple of the given values.
     */
    public static Tuple<@Nullable Object> row( @Nullable Object... values ) {
        Objects.requireNonNull(values);
        return Tuple.ofNullable(Object.class, values);
    }

    private final UI.CellOrder                   _cellOrder;
    private final UI.Editability                 _editability;
    private final Tuple<@Nullable String>        _columnNames;
    private final Tuple<Class<?>>                _columnClasses;
    private final Tuple<Tuple<@Nullable Object>> _cells;
    /*
        The two dimensions are not state of their own, they are a function of the
        three tuples above, precomputed here because a JTable asks for them
        relentlessly while it paints. Deriving them (instead of letting the caller
        supply them) is what keeps a table which grew a row or lost a column
        consistent without anybody having to keep a counter in sync.
     */
    private final int _rowCount;
    private final int _columnCount;

    private TableData(
        UI.CellOrder                   cellOrder,
        UI.Editability                 editability,
        Tuple<@Nullable String>        columnNames,
        Tuple<Class<?>>                columnClasses,
        Tuple<Tuple<@Nullable Object>> cells
    ) {
        _cellOrder     = Objects.requireNonNull(cellOrder);
        _editability   = Objects.requireNonNull(editability);
        _columnNames   = Objects.requireNonNull(columnNames);
        _columnClasses = Objects.requireNonNull(columnClasses);
        _cells         = Objects.requireNonNull(cells);

        int longestLine = 0;
        for ( Tuple<@Nullable Object> line : cells )
            longestLine = Math.max(longestLine, line.size());
        int describedColumns = Math.max(columnNames.size(), columnClasses.size());

        if ( cellOrder.isRowMajor() ) {
            _rowCount    = cells.size();
            _columnCount = Math.max(describedColumns, longestLine);
        } else {
            _rowCount    = longestLine;
            _columnCount = Math.max(describedColumns, cells.size());
        }
    }

    // ---- Reading ----

    /**
     *  The cell order deciding whether the {@link #cells()} of this table are stored
     *  row by row or column by column.
     *  @return The {@link UI.CellOrder} of this table.
     */
    public UI.CellOrder cellOrder() {
        return _cellOrder;
    }

    /**
     *  Tells whether the user is allowed to edit the cells of this table.
     *  @return The {@link UI.Editability} of this table.
     */
    public UI.Editability editability() {
        return _editability;
    }

    /**
     *  Tells whether the user is allowed to edit the cells of this table. Note that a
     *  table also needs to live in a mutable {@link sprouts.Var} before an edit has
     *  anywhere to go.
     *  @return True if the {@link #editability()} of this table permits cell editing.
     */
    public boolean isEditable() {
        return _editability.isEditable();
    }

    /**
     *  Returns the number of rows in this table.
     *  @return The row count.
     */
    public int getRowCount() {
        return _rowCount;
    }

    /**
     *  Returns the number of columns in this table.
     *  @return The column count.
     */
    public int getColumnCount() {
        return _columnCount;
    }

    /**
     *  Tells whether this table has nothing to show, meaning it has no rows,
     *  no columns, or neither.
     *  @return True if this table has no cells to display.
     */
    public boolean isEmpty() {
        return _rowCount == 0 || _columnCount == 0;
    }

    /**
     *  Reads the value of a single cell, always in {@code (row, column)} terms,
     *  irrespective of the {@link #cellOrder()} of this table.
     * @param rowIndex    The row index of the cell.
     * @param columnIndex The column index of the cell.
     * @return The value of the cell, or {@code null} if the indices are out of bounds.
     */
    public @Nullable Object getValueAt( int rowIndex, int columnIndex ) {
        if ( rowIndex < 0 || rowIndex >= _rowCount || columnIndex < 0 || columnIndex >= _columnCount )
            return null;
        return _cellAt(_majorOf(rowIndex, columnIndex), _minorOf(rowIndex, columnIndex));
    }

    /**
     *  Reads an entire row, padded with {@code null} to the width of the table, so
     *  that the returned tuple always has exactly {@link #getColumnCount()} entries.
     * @param rowIndex The index of the row.
     * @return The cells of the row, or an empty tuple if the index is out of bounds.
     */
    public Tuple<@Nullable Object> getRow( int rowIndex ) {
        if ( rowIndex < 0 || rowIndex >= _rowCount )
            return EMPTY_LINE;
        if ( _cellOrder.isRowMajor() )
            return _padTo(_lineAt(rowIndex), _columnCount);
        return _gatherAcrossLines(rowIndex, _columnCount);
    }

    /**
     *  Reads an entire column, padded with {@code null} to the height of the table, so
     *  that the returned tuple always has exactly {@link #getRowCount()} entries.
     * @param columnIndex The index of the column.
     * @return The cells of the column, or an empty tuple if the index is out of bounds.
     */
    public Tuple<@Nullable Object> getColumn( int columnIndex ) {
        if ( columnIndex < 0 || columnIndex >= _columnCount )
            return EMPTY_LINE;
        if ( !_cellOrder.isRowMajor() )
            return _padTo(_lineAt(columnIndex), _rowCount);
        return _gatherAcrossLines(columnIndex, _rowCount);
    }

    /**
     *  Reads the name of a column.
     * @param columnIndex The column index.
     * @return The column name, or {@code null} if the column has no name of its own
     *         (in which case a table falls back to the default, spreadsheet style name).
     */
    public @Nullable String getColumnName( int columnIndex ) {
        if ( columnIndex < 0 || columnIndex >= _columnNames.size() )
            return null;
        return _columnNames.get(columnIndex);
    }

    /**
     *  Reads the class of a column, which a {@link javax.swing.JTable} consults to
     *  pick a renderer and an editor for it.
     * @param columnIndex The column index.
     * @return The column class, or {@link Object} if the column has no class of its own.
     */
    public Class<?> getColumnClass( int columnIndex ) {
        if ( columnIndex < 0 || columnIndex >= _columnClasses.size() )
            return Object.class;
        return _columnClasses.get(columnIndex);
    }

    /**
     *  Looks up a column by name, so that your business logic may address columns
     *  by what they mean rather than by where they happen to sit:
     *  <pre>{@code
     *  data.setCellAt(0, data.indexOfColumn("Age"), 31);
     *  }</pre>
     * @param columnName The name of the column to look for.
     * @return The index of the first column with the given name, or {@code -1} if there is none.
     */
    public int indexOfColumn( @Nullable String columnName ) {
        for ( int i = 0; i < _columnNames.size(); i++ )
            if ( Objects.equals(_columnNames.get(i), columnName) )
                return i;
        return -1;
    }

    /**
     *  The raw cell values of this table, in the order described by its {@link #cellOrder()}.
     *  This is exposed so that a table model can reuse the very same immutable
     *  structure without copying it, and so that you may reach for the full
     *  {@link Tuple} API when this class does not have the operation you need.
     *  @return The immutable tuple of rows (or columns, see {@link #cellOrder()}).
     */
    public Tuple<Tuple<@Nullable Object>> cells() {
        return _cells;
    }

    /**
     *  The column names of this table, where a {@code null} entry means that
     *  the table falls back to the default (spreadsheet style) column name.
     *  @return The immutable tuple of column names.
     */
    public Tuple<@Nullable String> columnNames() {
        return _columnNames;
    }

    /**
     *  The column classes of this table, which a {@link javax.swing.JTable}
     *  consults to pick a renderer and editor for each column.
     *  @return The immutable tuple of column classes.
     */
    public Tuple<Class<?>> columnClasses() {
        return _columnClasses;
    }

    // ---- Changing cells ----

    /**
     *  Produces a new table in which a single cell has a new value, reusing the
     *  immutable structure of this table for everything else.
     *  <p>
     *  Note that this is a targeted change: a row major table hands it to a
     *  {@link javax.swing.JTable} as a single row update, so only that row repaints.
     *
     * @param rowIndex    The row index of the cell to change.
     * @param columnIndex The column index of the cell to change.
     * @param value       The new value of the cell.
     * @return A new {@link TableData} with the changed cell, or this table unchanged
     *         if the indices are out of bounds or the cell already holds that value.
     */
    public TableData setCellAt( int rowIndex, int columnIndex, @Nullable Object value ) {
        if ( rowIndex < 0 || rowIndex >= _rowCount || columnIndex < 0 || columnIndex >= _columnCount )
            return this;
        int majorIndex = _majorOf(rowIndex, columnIndex);
        int minorIndex = _minorOf(rowIndex, columnIndex);
        if ( Objects.equals(_cellAt(majorIndex, minorIndex), value) )
            return this;
        Tuple<@Nullable Object> line = _padTo(_lineAt(majorIndex), minorIndex + 1).setAt(minorIndex, value);
        return _withCells(_padLines(majorIndex + 1).setAt(majorIndex, line));
    }

    // ---- Changing rows ----

    /**
     *  Appends a row to the end of this table, built from plain values:
     *  <pre>{@code
     *  data.addRow("Alice", 30, "Rome")
     *  }</pre>
     * @param values The cell values of the new row, one per column.
     * @return A new {@link TableData} with the additional row.
     */
    public TableData addRow( @Nullable Object... values ) {
        return addRowAt(_rowCount, row(values));
    }

    /**
     *  Appends a row to the end of this table.
     * @param row The cells of the new row, one per column.
     * @return A new {@link TableData} with the additional row.
     */
    public TableData addRow( Tuple<@Nullable Object> row ) {
        return addRowAt(_rowCount, row);
    }

    /**
     *  Inserts a row at the given index, built from plain values.
     * @param rowIndex The index the new row will have.
     * @param values   The cell values of the new row, one per column.
     * @return A new {@link TableData} with the additional row.
     */
    public TableData addRowAt( int rowIndex, @Nullable Object... values ) {
        return addRowAt(rowIndex, row(values));
    }

    /**
     *  Inserts a row at the given index.
     * @param rowIndex The index the new row will have.
     * @param row      The cells of the new row, one per column.
     * @return A new {@link TableData} with the additional row, or this table unchanged
     *         if the index is out of bounds.
     */
    public TableData addRowAt( int rowIndex, Tuple<@Nullable Object> row ) {
        Objects.requireNonNull(row);
        return addRowsAt(rowIndex, Tuple.of(LINE_TYPE, row));
    }

    /**
     *  Appends several rows at once, which is the efficient way to grow a table by
     *  more than one row: a row major table hands this to a {@link javax.swing.JTable}
     *  as a <i>single</i> insertion of a range of rows, rather than one event per row.
     * @param rows The new rows, each holding one cell value per column.
     * @return A new {@link TableData} with the additional rows.
     */
    public TableData addRows( Tuple<Tuple<@Nullable Object>> rows ) {
        return addRowsAt(_rowCount, rows);
    }

    /**
     *  Inserts several rows at once, starting at the given index.
     *  See {@link #addRows(Tuple)} for why you should prefer this over adding
     *  the rows one at a time.
     * @param rowIndex The index the first of the new rows will have.
     * @param rows     The new rows, each holding one cell value per column.
     * @return A new {@link TableData} with the additional rows, or this table unchanged
     *         if the index is out of bounds or there is nothing to add.
     */
    public TableData addRowsAt( int rowIndex, Tuple<Tuple<@Nullable Object>> rows ) {
        Objects.requireNonNull(rows);
        if ( rowIndex < 0 || rowIndex > _rowCount || rows.isEmpty() )
            return this;
        if ( _cellOrder.isRowMajor() )
            return _withCells(_cells.addAllAt(rowIndex, rows));
        return _withCells(_insertMinors(rowIndex, rows, _rowCount));
    }

    /**
     *  Replaces the row at the given index, built from plain values.
     * @param rowIndex The index of the row to replace.
     * @param values   The new cell values of the row, one per column.
     * @return A new {@link TableData} with the replaced row.
     */
    public TableData setRowAt( int rowIndex, @Nullable Object... values ) {
        return setRowAt(rowIndex, row(values));
    }

    /**
     *  Replaces the row at the given index.
     * @param rowIndex The index of the row to replace.
     * @param row      The new cells of the row, one per column.
     * @return A new {@link TableData} with the replaced row, or this table unchanged
     *         if the index is out of bounds.
     */
    public TableData setRowAt( int rowIndex, Tuple<@Nullable Object> row ) {
        Objects.requireNonNull(row);
        return setRowsAt(rowIndex, Tuple.of(LINE_TYPE, row));
    }

    /**
     *  Replaces a whole range of rows at once, starting at the given index, which a
     *  row major table hands to a {@link javax.swing.JTable} as a single update of a
     *  range of rows. This is the operation you want when a batch of your data changed
     *  and you would rather not rebuild the whole table for it.
     * @param rowIndex The index of the first row to replace.
     * @param rows     The new rows, each holding one cell value per column.
     * @return A new {@link TableData} with the replaced rows, or this table unchanged
     *         if the range is out of bounds or there is nothing to replace.
     */
    public TableData setRowsAt( int rowIndex, Tuple<Tuple<@Nullable Object>> rows ) {
        Objects.requireNonNull(rows);
        if ( rowIndex < 0 || rows.isEmpty() || rowIndex + rows.size() > _rowCount )
            return this;
        if ( _cellOrder.isRowMajor() )
            return _withCells(_cells.setAllAt(rowIndex, rows));
        return _withCells(_replaceMinors(rowIndex, rows));
    }

    /**
     *  Removes the row at the given index.
     * @param rowIndex The index of the row to remove.
     * @return A new {@link TableData} without that row, or this table unchanged
     *         if the index is out of bounds.
     */
    public TableData removeRowAt( int rowIndex ) {
        return removeRowsAt(rowIndex, 1);
    }

    /**
     *  Removes a whole range of rows at once, which a row major table hands to a
     *  {@link javax.swing.JTable} as a single deletion of a range of rows.
     * @param rowIndex The index of the first row to remove.
     * @param count    The number of rows to remove.
     * @return A new {@link TableData} without those rows, or this table unchanged
     *         if the range is out of bounds or there is nothing to remove.
     */
    public TableData removeRowsAt( int rowIndex, int count ) {
        if ( rowIndex < 0 || count <= 0 || rowIndex + count > _rowCount )
            return this;
        if ( _cellOrder.isRowMajor() )
            return _withCells(_cells.removeAt(rowIndex, count));
        return _withCells(_removeMinors(rowIndex, count));
    }

    /**
     *  Removes every row, while keeping the columns of this table exactly as they are.
     *  Reach for {@link #empty()} instead if you want to be rid of the columns too.
     * @return A new {@link TableData} with the same columns but no rows.
     */
    public TableData removeAllRows() {
        return removeRowsAt(0, _rowCount);
    }

    // ---- Changing columns ----

    /**
     *  Appends a column to the right of this table, with a name, a class and its cells:
     *  <pre>{@code
     *  data.addColumn("City", String.class, TableData.row("Rome", "Oslo"))
     *  }</pre>
     * @param columnName  The name of the new column.
     * @param columnClass The class of the new column.
     * @param values      The cells of the new column, one per row.
     * @return A new {@link TableData} with the additional column.
     */
    public TableData addColumn( @Nullable String columnName, Class<?> columnClass, Tuple<@Nullable Object> values ) {
        return addColumnAt(_columnCount, columnName, columnClass, values);
    }

    /**
     *  Inserts a column at the given index, with a name, a class and its cells.
     * @param columnIndex The index the new column will have.
     * @param columnName  The name of the new column.
     * @param columnClass The class of the new column.
     * @param values      The cells of the new column, one per row.
     * @return A new {@link TableData} with the additional column, or this table
     *         unchanged if the index is out of bounds.
     */
    public TableData addColumnAt(
        int                     columnIndex,
        @Nullable String        columnName,
        Class<?>                columnClass,
        Tuple<@Nullable Object> values
    ) {
        Objects.requireNonNull(columnClass);
        Objects.requireNonNull(values);
        if ( columnIndex < 0 || columnIndex > _columnCount )
            return this;
        Tuple<@Nullable String> names   = _padNamesTo(_columnCount).addAt(columnIndex, columnName);
        Tuple<Class<?>>         classes = _padClassesTo(_columnCount).addAt(columnIndex, columnClass);
        Tuple<Tuple<@Nullable Object>> cells =
                _cellOrder.isRowMajor()
                    ? _insertMinors(columnIndex, Tuple.of(LINE_TYPE, values), _columnCount)
                    // Metadata may describe more columns than the cells fill, so the
                    // cells are padded up to the insertion point first - clamping the
                    // index instead would land the new cells under the wrong column.
                    : _padLines(columnIndex).addAllAt(columnIndex, Tuple.of(LINE_TYPE, values));
        return new TableData(_cellOrder, _editability, names, classes, cells);
    }

    /**
     *  Replaces the cells of the column at the given index, leaving its name and
     *  class alone.
     * @param columnIndex The index of the column to replace.
     * @param values      The new cells of the column, one per row.
     * @return A new {@link TableData} with the replaced column, or this table
     *         unchanged if the index is out of bounds.
     */
    public TableData setColumnAt( int columnIndex, Tuple<@Nullable Object> values ) {
        Objects.requireNonNull(values);
        if ( columnIndex < 0 || columnIndex >= _columnCount )
            return this;
        if ( !_cellOrder.isRowMajor() )
            return _withCells(_padLines(columnIndex + 1).setAt(columnIndex, values));
        return _withCells(_replaceMinors(columnIndex, Tuple.of(LINE_TYPE, values)));
    }

    /**
     *  Removes the column at the given index, along with its name and class.
     * @param columnIndex The index of the column to remove.
     * @return A new {@link TableData} without that column, or this table unchanged
     *         if the index is out of bounds.
     */
    public TableData removeColumnAt( int columnIndex ) {
        return removeColumnsAt(columnIndex, 1);
    }

    /**
     *  Removes a whole range of columns at once, along with their names and classes.
     * @param columnIndex The index of the first column to remove.
     * @param count       The number of columns to remove.
     * @return A new {@link TableData} without those columns, or this table unchanged
     *         if the range is out of bounds or there is nothing to remove.
     */
    public TableData removeColumnsAt( int columnIndex, int count ) {
        if ( columnIndex < 0 || count <= 0 || columnIndex + count > _columnCount )
            return this;
        Tuple<@Nullable String> names   = _removeFrom(_padNamesTo(_columnCount), columnIndex, count);
        Tuple<Class<?>>         classes = _removeFrom(_padClassesTo(_columnCount), columnIndex, count);
        Tuple<Tuple<@Nullable Object>> cells =
                _cellOrder.isRowMajor()
                    ? _removeMinors(columnIndex, count)
                    : _removeFrom(_cells, columnIndex, count);
        return new TableData(_cellOrder, _editability, names, classes, cells);
    }

    /**
     *  Renames a single column, which a table picks up as a change of its header.
     * @param columnIndex The index of the column to rename.
     * @param columnName  The new name of the column, where {@code null} means
     *                    "fall back to the default (spreadsheet style) name".
     * @return A new {@link TableData} with the renamed column, or this table unchanged
     *         if the index is out of bounds or the name did not change.
     */
    public TableData setColumnNameAt( int columnIndex, @Nullable String columnName ) {
        if ( columnIndex < 0 || columnIndex >= _columnCount )
            return this;
        if ( Objects.equals(getColumnName(columnIndex), columnName) )
            return this;
        return new TableData(
                    _cellOrder,
                    _editability,
                    _padNamesTo(_columnCount).setAt(columnIndex, columnName),
                    _columnClasses,
                    _cells
                );
    }

    /**
     *  Renames every column at once.
     * @param columnNames The new column names, one per column.
     * @return A new {@link TableData} with the new column names.
     */
    public TableData setColumnNames( String... columnNames ) {
        Objects.requireNonNull(columnNames);
        return setColumnNames(Tuple.ofNullable(String.class, columnNames));
    }

    /**
     *  Renames every column at once. Note that the number of names takes part in
     *  deciding how many columns this table has, so handing over more names than
     *  the cells fill widens the table.
     * @param columnNames The new column names, one per column.
     * @return A new {@link TableData} with the new column names.
     */
    public TableData setColumnNames( Tuple<@Nullable String> columnNames ) {
        Objects.requireNonNull(columnNames);
        return new TableData(_cellOrder, _editability, columnNames, _columnClasses, _cells);
    }

    /**
     *  Changes the class of a single column, which is how you tell a
     *  {@link javax.swing.JTable} to render and edit it differently (a
     *  {@link Boolean} column becomes a column of check boxes, say).
     * @param columnIndex The index of the column.
     * @param columnClass The new class of the column.
     * @return A new {@link TableData} with the new column class, or this table
     *         unchanged if the index is out of bounds or the class did not change.
     */
    public TableData setColumnClassAt( int columnIndex, Class<?> columnClass ) {
        Objects.requireNonNull(columnClass);
        if ( columnIndex < 0 || columnIndex >= _columnCount )
            return this;
        if ( getColumnClass(columnIndex).equals(columnClass) )
            return this;
        return new TableData(
                    _cellOrder,
                    _editability,
                    _columnNames,
                    _padClassesTo(_columnCount).setAt(columnIndex, columnClass),
                    _cells
                );
    }

    /**
     *  Changes the class of every column at once.
     * @param columnClasses The new column classes, one per column.
     * @return A new {@link TableData} with the new column classes.
     */
    public TableData setColumnClasses( Tuple<Class<?>> columnClasses ) {
        Objects.requireNonNull(columnClasses);
        return new TableData(_cellOrder, _editability, _columnNames, columnClasses, _cells);
    }

    // ---- Changing everything ----

    /**
     *  Replaces all of the cells of this table at once, keeping its cell order, editability and columns.
     * @param cells The new cell values, an immutable tuple of rows (or columns, see {@link #cellOrder()}).
     * @return A new {@link TableData} with the new cells.
     */
    public TableData setCells( Tuple<Tuple<@Nullable Object>> cells ) {
        Objects.requireNonNull(cells);
        return _withCells(cells);
    }

    /**
     *  Changes the cell order of this table, which is how you tell it to read the very
     *  same {@link #cells()} along the other axis:
     *  <pre>{@code
     *  data.withCellOrder(UI.CellOrder.COLUMN_MAJOR)
     *  }</pre>
     *  <b>Careful:</b> this reinterprets the cells rather than rearranging them, so a
     *  table whose cells stay put comes out transposed. Use it when you know the cells
     *  were the other way around all along, not to flip a table on screen.
     *
     * @param cellOrder The new {@link UI.CellOrder} of this table.
     * @return A new {@link TableData} with the given cell order, or this table unchanged
     *         if it already has it.
     */
    public TableData withCellOrder( UI.CellOrder cellOrder ) {
        Objects.requireNonNull(cellOrder);
        if ( _cellOrder == cellOrder )
            return this;
        return new TableData(cellOrder, _editability, _columnNames, _columnClasses, _cells);
    }

    /**
     *  Changes whether the user may edit this table, without touching a single cell
     *  and without any risk of disturbing how the cells are read:
     *  <pre>{@code
     *  data.withEditability(UI.Editability.EDITABLE)
     *  }</pre>
     *  Note that an editable table also has to live in a mutable {@link sprouts.Var}
     *  before an edit has anywhere to go.
     *
     * @param editability The new {@link UI.Editability} of this table.
     * @return A new {@link TableData} with the given editability, or this table unchanged
     *         if it already has it.
     */
    public TableData withEditability( UI.Editability editability ) {
        Objects.requireNonNull(editability);
        if ( _editability == editability )
            return this;
        return new TableData(_cellOrder, editability, _columnNames, _columnClasses, _cells);
    }

    /**
     *  Permits the user to edit the cells of this table, shorthand for
     *  {@code withEditability(UI.Editability.EDITABLE)}.
     * @return A new editable {@link TableData}, or this table unchanged if it already is.
     */
    public TableData asEditable() {
        return withEditability(UI.Editability.EDITABLE);
    }

    /**
     *  Forbids the user to edit the cells of this table, shorthand for
     *  {@code withEditability(UI.Editability.READ_ONLY)}.
     * @return A new read only {@link TableData}, or this table unchanged if it already is.
     */
    public TableData asReadOnly() {
        return withEditability(UI.Editability.READ_ONLY);
    }

    // ---- Internals ----

    private int _majorOf( int rowIndex, int columnIndex ) {
        return _cellOrder.isRowMajor() ? rowIndex : columnIndex;
    }

    private int _minorOf( int rowIndex, int columnIndex ) {
        return _cellOrder.isRowMajor() ? columnIndex : rowIndex;
    }

    private Tuple<@Nullable Object> _lineAt( int majorIndex ) {
        if ( majorIndex < 0 || majorIndex >= _cells.size() )
            return EMPTY_LINE;
        return _cells.get(majorIndex);
    }

    private @Nullable Object _cellAt( int majorIndex, int minorIndex ) {
        Tuple<@Nullable Object> line = _lineAt(majorIndex);
        if ( minorIndex < 0 || minorIndex >= line.size() )
            return null;
        return line.get(minorIndex);
    }

    /** Reads one value out of every major line, which is how the minor axis is read. */
    private Tuple<@Nullable Object> _gatherAcrossLines( int minorIndex, int length ) {
        List<@Nullable Object> values = new ArrayList<>(length);
        for ( int i = 0; i < length; i++ )
            values.add(_cellAt(i, minorIndex));
        return Tuple.ofNullable(Object.class, values);
    }

    /**
     *  Inserts one value into every major line, which is how a row is added to a
     *  column major table (and a column to a row major one). The supplied lines are
     *  the new minor lines, each holding one value per major line.
     */
    private Tuple<Tuple<@Nullable Object>> _insertMinors(
        int minorIndex, Tuple<Tuple<@Nullable Object>> minorLines, int minorCount
    ) {
        int majorCount = _cells.size();
        for ( Tuple<@Nullable Object> minorLine : minorLines )
            majorCount = Math.max(majorCount, minorLine.size());
        List<Tuple<@Nullable Object>> lines = new ArrayList<>(majorCount);
        for ( int major = 0; major < majorCount; major++ ) {
            List<@Nullable Object> inserted = new ArrayList<>(minorLines.size());
            for ( Tuple<@Nullable Object> minorLine : minorLines )
                inserted.add(major < minorLine.size() ? minorLine.get(major) : null);
            lines.add(
                _padTo(_lineAt(major), minorCount)
                    .addAllAt(minorIndex, Tuple.ofNullable(Object.class, inserted))
            );
        }
        return Tuple.of(LINE_TYPE, lines);
    }

    /** Replaces one value in every major line, the mirror image of {@link #_insertMinors}. */
    private Tuple<Tuple<@Nullable Object>> _replaceMinors( int minorIndex, Tuple<Tuple<@Nullable Object>> minorLines ) {
        int majorCount = _cells.size();
        for ( Tuple<@Nullable Object> minorLine : minorLines )
            majorCount = Math.max(majorCount, minorLine.size());
        List<Tuple<@Nullable Object>> lines = new ArrayList<>(majorCount);
        for ( int major = 0; major < majorCount; major++ ) {
            List<@Nullable Object> replacements = new ArrayList<>(minorLines.size());
            for ( Tuple<@Nullable Object> minorLine : minorLines )
                replacements.add(major < minorLine.size() ? minorLine.get(major) : null);
            lines.add(
                _padTo(_lineAt(major), minorIndex + replacements.size())
                    .setAllAt(minorIndex, Tuple.ofNullable(Object.class, replacements))
            );
        }
        return Tuple.of(LINE_TYPE, lines);
    }

    /** Removes a range of values from every major line, the mirror image of {@link #_insertMinors}. */
    private Tuple<Tuple<@Nullable Object>> _removeMinors( int minorIndex, int count ) {
        List<Tuple<@Nullable Object>> lines = new ArrayList<>(_cells.size());
        for ( Tuple<@Nullable Object> line : _cells )
            lines.add(_removeFrom(line, minorIndex, count));
        return Tuple.of(LINE_TYPE, lines);
    }

    private static Tuple<@Nullable Object> _padTo( Tuple<@Nullable Object> line, int length ) {
        if ( line.size() >= length )
            return line;
        List<@Nullable Object> padding = new ArrayList<>(length - line.size());
        for ( int i = line.size(); i < length; i++ )
            padding.add(null);
        return line.addAll(Tuple.ofNullable(Object.class, padding));
    }

    private Tuple<Tuple<@Nullable Object>> _padLines( int length ) {
        if ( _cells.size() >= length )
            return _cells;
        List<Tuple<@Nullable Object>> padding = new ArrayList<>(length - _cells.size());
        for ( int i = _cells.size(); i < length; i++ )
            padding.add(EMPTY_LINE);
        return _cells.addAll(Tuple.of(LINE_TYPE, padding));
    }

    private Tuple<@Nullable String> _padNamesTo( int length ) {
        if ( _columnNames.size() >= length )
            return _columnNames;
        List<@Nullable String> padding = new ArrayList<>(length - _columnNames.size());
        for ( int i = _columnNames.size(); i < length; i++ )
            padding.add(null);
        return _columnNames.addAll(Tuple.ofNullable(String.class, padding));
    }

    private Tuple<Class<?>> _padClassesTo( int length ) {
        if ( _columnClasses.size() >= length )
            return _columnClasses;
        List<Class<?>> padding = new ArrayList<>(length - _columnClasses.size());
        for ( int i = _columnClasses.size(); i < length; i++ )
            padding.add(Object.class);
        return _columnClasses.addAll(Tuple.of(CLASS_TYPE, padding));
    }

    private static <T extends @Nullable Object> Tuple<T> _removeFrom( Tuple<T> tuple, int index, int count ) {
        int from = Math.min(Math.max(0, index), tuple.size());
        int to   = Math.min(from + count, tuple.size());
        return from < to ? tuple.removeRange(from, to) : tuple;
    }

    private TableData _withCells( Tuple<Tuple<@Nullable Object>> cells ) {
        if ( cells.equals(_cells) )
            return this;
        return new TableData(_cellOrder, _editability, _columnNames, _columnClasses, cells);
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == this ) return true;
        if ( !(obj instanceof TableData) ) return false;
        TableData other = (TableData) obj;
        // Note that the two dimensions are derived from the state below, so they take care of themselves.
        return _cellOrder == other._cellOrder
            && _editability == other._editability
            && _columnNames.equals(other._columnNames)
            && _columnClasses.equals(other._columnClasses)
            && _cells.equals(other._cells);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_cellOrder, _editability, _columnNames, _columnClasses, _cells);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" +
                    "cellOrder="   + _cellOrder     + ", " +
                    "editability=" + _editability   + ", " +
                    "rowCount="    + _rowCount      + ", " +
                    "columnCount=" + _columnCount   + ", " +
                    "columnNames=" + _columnNames   + ", " +
                    "cells="       + _cells         +
                "]";
    }
}
