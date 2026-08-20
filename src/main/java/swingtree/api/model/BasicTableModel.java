package swingtree.api.model;

import org.jspecify.annotations.Nullable;
import sprouts.Observable;
import sprouts.Event;
import sprouts.Tuple;
import swingtree.api.Buildable;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *  This interface defines a basic table model which can be used to create a table model using lambda expressions.
 *  Implementations of this are typically created declarative like so: <br>
 *  <pre>{@code
 *      UI.table( conf -> conf
 *          .colNames("A", "B")
 *          .colCount(()->2)
 *          .rowCount(()->3)
 *          .getsEntryAt((int row, int col)->
 *              vm.getDataAt(row, col)
 *          )
 *      )
 *  }</pre>
 *  <b>Note that {@link TableData} is the recommended way of modelling a table in
 *  SwingTree.</b> It is a single immutable value describing the whole table (cells,
 *  column names, column classes, cell order and editability), which you hold in a
 *  {@link sprouts.Var} property and bind through {@link swingtree.UI#table(sprouts.Var)}:
 *  <pre>{@code
 *      Var<TableData> data = Var.of(
 *              TableData.of(UI.CellOrder.ROW_MAJOR, "A", "B")
 *                  .addRow(1, 2)
 *          );
 *
 *      UI.table(data);
 *  }</pre>
 *  A table bound like that updates itself whenever the property changes (so there is
 *  no {@code updateOn(..)} to remember), it is thread safe by construction, and it
 *  syncs row changes to the {@link javax.swing.JTable} incrementally instead of
 *  rebuilding it. The lambda based model below, by contrast, is read <i>live</i>,
 *  which is why SwingTree has to copy the whole table on every refresh under the
 *  {@link swingtree.threading.EventProcessor#DECOUPLED} protocol.
 *  <p>
 *  Note that an implementation of this interface is merely a <i>description of where
 *  the table data lives</i>, it is not the model which the {@link javax.swing.JTable} ends up
 *  talking to. SwingTree always wraps it in a thread safe model of its own, which
 *  reads through the methods declared here and, under the
 *  {@link swingtree.threading.EventProcessor#DECOUPLED} protocol, keeps a UI thread
 *  owned snapshot of them so that the AWT Event Dispatch Thread never reads your
 *  application thread owned state while it paints. This is also why you should not
 *  expect {@link javax.swing.JTable#getModel()} to return the very object you passed
 *  to {@code withModel(..)}.
 */
public interface BasicTableModel extends TableModel
{
    /** {@inheritDoc} */
    @Override int getRowCount();
    /** {@inheritDoc} */
    @Override int getColumnCount();
    /** {@inheritDoc} */
    @Override Object getValueAt(int rowIndex, int columnIndex);
    /** {@inheritDoc} */
    @Override void setValueAt(Object aValue, int rowIndex, int columnIndex);
    /** {@inheritDoc} */
    @Override default Class<?> getColumnClass(int columnIndex) { return Object.class; }
    /** {@inheritDoc} */
    @Override default String getColumnName(int columnIndex) { return null; }
    /** {@inheritDoc} */
    @Override default boolean isCellEditable(int rowIndex, int columnIndex) { return false; }
    /**
     *  Registers a listener which wants to be told when the data of this model changed.
     *  <p>
     *  A {@link BasicTableModel} is a plain description of where the table data comes
     *  from, which SwingTree wraps in a thread safe model of its own before handing it
     *  to a {@link javax.swing.JTable}. That wrapper is the only listener you will ever
     *  see here, and it uses this to learn that it has to re-read the data.
     *  The default implementation ignores the listener, which is what you want for a
     *  model that never signals changes on its own (use {@code updateTableOn(..)} on
     *  the table builder, or {@link Builder#updateOn(Observable)}, in that case).
     *
     * @param l The listener which wants to be told about changes to the data of this model.
     */
    @Override default void addTableModelListener(TableModelListener l) {}
    /**
     *  Unregisters a listener previously registered through
     *  {@link #addTableModelListener(TableModelListener)}.
     *  The default implementation does nothing, mirroring the default of
     *  {@link #addTableModelListener(TableModelListener)}.
     *
     * @param l The listener which no longer wants to be told about changes to the data of this model.
     */
    @Override default void removeTableModelListener(TableModelListener l) {}

    /**
     *  Implementations of this functional interface translate to the {@link TableModel#getRowCount()} method.
     */
    @FunctionalInterface interface RowCount { int get(); }
    /**
     *  Implementations of this functional interface translate to the {@link TableModel#getColumnCount()} method.
     */
    @FunctionalInterface interface ColumnCount { int get(); }
    /**
     *  Implementations of this functional interface translate to the {@link TableModel#getValueAt(int, int)} method.
     */
    @FunctionalInterface interface EntryGetter<E> { E get(int rowIndex, int colIndex); }
    /**
     *  Implementations of this functional interface translate to the {@link TableModel#setValueAt(Object, int, int)} method.
     */
    @FunctionalInterface interface EntrySetter<E> { void set(int rowIndex, int colIndex, E aValue); }
    /**
     *  Implementations of this functional interface translate to the {@link TableModel#getColumnClass(int)} method.
     */
    @FunctionalInterface interface ColumnClass<E> { Class<? extends E> get(int colIndex); }
    /**
     *  Implementations of this functional interface translate to the {@link TableModel#isCellEditable(int, int)} method.
     */
    @FunctionalInterface interface CellEditable { boolean is(int rowIndex, int colIndex); }
    /**
     *  Implementations of this functional interface translate to the {@link TableModel#getColumnName(int)} method.
     */
    @FunctionalInterface interface ColumnName { String get(int colIndex); }

    /**
     *  The class below is a functional builder for creating a lambda based implementation of the {@link BasicTableModel}.
     *  This allows fo a boilerplate free functional API.
     *
     * @param <E> Common type for all entry items in the table.
     */
     class Builder<E> implements Buildable<BasicTableModel>
     {
         private static class FixedColumnNames implements ColumnName {
            final Tuple<String> names;
             private FixedColumnNames(String... names) {
                 this.names = Tuple.of(String.class, names);
             }
             @Override
             public String get(int colIndex) {
                 return colIndex < 0 || colIndex >= names.size() ? "" : names.get(colIndex);
             }
         }

         private static class FixedColumnClasses implements ColumnClass {
             private final Class<?> commonEntryType;
             final Tuple<Class> classes;
             private FixedColumnClasses(Class<?> commonEntryType, Class<?>... names) {
                 this.commonEntryType = commonEntryType;
                 this.classes = Tuple.of(Class.class, names);
             }
             @Override
             public Class get(int colIndex) {
                 return colIndex < 0 || colIndex >= classes.size() ? commonEntryType : classes.get(colIndex);
             }
         }

        private final Class<E>       commonEntryType;
        private final @Nullable RowCount       rowCount;
        private final @Nullable ColumnCount    colCount;
        private final @Nullable EntryGetter<E> entryGetter;
        private final @Nullable EntrySetter<E> entrySetter;
        private final @Nullable ColumnClass<E> columnClass;
        private final @Nullable CellEditable   cellEditable;
        private final @Nullable ColumnName     columnName;
        private final @Nullable Observable     observableEvent;

         public Builder( Class<E> commonEntryType ) {
             this(
                 Objects.requireNonNull(commonEntryType),
                 null,
                 null,
                 null,
                 null,
                 null,
                 null,
                 null,
                 null
             );
         }

         private Builder(
             Class<E>                 commonEntryType,
             @Nullable RowCount       rowCount,
             @Nullable ColumnCount    colCount,
             @Nullable EntryGetter<E> entryGetter,
             @Nullable EntrySetter<E> entrySetter,
             @Nullable ColumnClass<E> columnClass,
             @Nullable CellEditable   cellEditable,
             @Nullable ColumnName     columnName,
             @Nullable Observable     observableEvent
         ) {
             this.commonEntryType = Objects.requireNonNull(commonEntryType);
             this.rowCount        = rowCount;
             this.colCount        = colCount;
             this.entryGetter     = entryGetter;
             this.entrySetter     = entrySetter;
             this.columnClass     = columnClass;
             this.cellEditable    = cellEditable;
             this.columnName      = columnName;
             this.observableEvent = observableEvent;
         }

         /**
          *  Use this to define the lambda which dynamically determines the row count of the table model.
          * @param rowCount The lambda which will be used to determine the row count of the table model.
          * @return This builder instance.
          */
        public Builder<E> rowCount( RowCount rowCount ) {
            if ( rowCount == null ) throw new IllegalArgumentException("rowCount cannot be null");
            if ( this.rowCount != null ) throw new IllegalStateException(RowCount.class.getSimpleName()+" already set");
            return new Builder<>(
                    commonEntryType, rowCount, colCount, entryGetter, entrySetter,
                    columnClass, cellEditable, columnName, observableEvent
                );
        }
        /**
         *  Use this to define the lambda which dynamically determines the column count of the table model.
         * @param columnCount The lambda which will be used to determine the column count of the table model.
         * @return This builder instance.
         */
        public Builder<E> colCount( ColumnCount columnCount ) {
            if ( columnCount == null ) throw new IllegalArgumentException("columnCount cannot be null");
            if ( this.colCount != null ) throw new IllegalStateException(ColumnCount.class.getSimpleName()+" already set");
            return new Builder<>(
                    commonEntryType, rowCount, columnCount, entryGetter, entrySetter,
                    columnClass, cellEditable, columnName, observableEvent
                );
        }
        /**
         *  Accepts a lambda allowing the {@link javax.swing.JTable} to dynamically determines the value at a given row and column.
         * @param entryGetter The lambda which will be used to determine the value at a given row and column.
         * @return This builder instance.
         */
        public Builder<E> getsEntryAt(EntryGetter<E> entryGetter) {
            if ( entryGetter == null ) throw new IllegalArgumentException("valueAt cannot be null");
            if ( this.entryGetter != null ) throw new IllegalStateException(EntryGetter.class.getSimpleName()+" already set");
            return new Builder<>(
                    commonEntryType, rowCount, colCount, entryGetter, entrySetter,
                    columnClass, cellEditable, columnName, observableEvent
                );
        }
        /**
         *  Accepts a lambda allowing lambda which allows the user of the {@link javax.swing.JTable} to set the value at a given row and column.
         * @param entrySetter The lambda which will be used to set the value at a given row and column.
         * @return This builder instance.
         */
        public Builder<E> setsEntryAt(EntrySetter<E> entrySetter) {
            if ( entrySetter == null ) throw new IllegalArgumentException("setValueAt cannot be null");
            if ( this.entrySetter != null ) throw new IllegalStateException(EntrySetter.class.getSimpleName()+" already set");
            return new Builder<>(
                    commonEntryType, rowCount, colCount, entryGetter, entrySetter,
                    columnClass, cellEditable, columnName, observableEvent
                );
        }
        /**
         *  Accepts a lambda which allows the {@link javax.swing.JTable} to determine the class of the column at a given index.
         * @param columnClass The lambda which will be used to determine the class of the column at a given index.
         * @return This builder instance.
         */
        public Builder<E> colClass( ColumnClass<E> columnClass ) {
            if ( columnClass == null ) throw new IllegalArgumentException("columnClass cannot be null");
            if ( this.columnClass != null ) throw new IllegalStateException(ColumnClass.class.getSimpleName()+" already set");
            return new Builder<>(
                    commonEntryType, rowCount, colCount, entryGetter, entrySetter,
                    columnClass, cellEditable, columnName, observableEvent
                );
        }
        /**
         *  Use this to define a fixed array of column classes.
         * @param classes An array of column classes.
         * @return This builder instance.
         */
        public Builder<E> colClasses( Class<? extends E>... classes ) {
            if ( classes == null ) throw new IllegalArgumentException("classes cannot be null");
            return colClass(new FixedColumnClasses(commonEntryType, classes));
        }
        /**
         *  Accepts a lambda allowing the {@link javax.swing.JTable} to determine if the cell at a given row and column is editable.
         * @param cellEditable The lambda which will be used to determine if the cell at a given row and column is editable.
         * @return This builder instance.
         */
        public Builder<E> isEditableIf( CellEditable cellEditable ) {
            if ( cellEditable == null ) throw new IllegalArgumentException("cellEditable cannot be null");
            if ( this.cellEditable != null ) throw new IllegalStateException(CellEditable.class.getSimpleName()+" already set");
            return new Builder<>(
                    commonEntryType, rowCount, colCount, entryGetter, entrySetter,
                    columnClass, cellEditable, columnName, observableEvent
                );
        }
         /**
          *  Use this to define the lambda which allows the {@link javax.swing.JTable} to determine the name of the column at a given index.
          * @param columnName The lambda which will be used to determine the name of the column at a given index.
          * @return This builder instance.
          */
        public Builder<E> colName( ColumnName columnName ) {
            if ( columnName == null ) throw new IllegalArgumentException("columnName cannot be null");
            if (this.columnName != null)
                throw new IllegalStateException(ColumnName.class.getSimpleName() + " already set");
            return new Builder<>(
                    commonEntryType, rowCount, colCount, entryGetter, entrySetter,
                    columnClass, cellEditable, columnName, observableEvent
                );
        }
        /**
         *  Use this to define a fixed array of column names.
         * @param names An array of column names.
         * @return This builder instance.
         */
        public Builder<E> colNames( String... names ) {
            if ( names == null ) throw new IllegalArgumentException("names cannot be null");
            return colName(new FixedColumnNames(names));
        }
        /**
         *  Use this to define the event which will be fired when the table model is updated.
         * @param updateEvent The event which will be fired when the table model is updated.
         * @return This builder instance.
         */
        public Builder<E> updateOn( Observable updateEvent ) {
            if ( updateEvent == null ) throw new IllegalArgumentException("updateEvent cannot be null");
            if ( this.observableEvent != null ) throw new IllegalStateException(Event.class.getSimpleName()+" already set");
            return new Builder<>(
                    commonEntryType, rowCount, colCount, entryGetter, entrySetter,
                    columnClass, cellEditable, columnName, updateEvent
                );
        }
        /**
         *  Use this to build the {@link BasicTableModel} instance.
         * @return The {@link BasicTableModel} instance.
         */
        @Override public BasicTableModel build() {
            FunTableModel tm = new FunTableModel();
            if ( observableEvent != null )
                observableEvent.subscribe(()->
                    // We merely tell whoever reads this model that the data changed.
                    // The thread safe model which SwingTree wraps around this listens
                    // in, and it is that model which decides how to thread and fire the
                    // refresh, based on the event processor it was installed with.
                    tm.fireDataChanged()
                );
            return tm;
        }

         private class FunTableModel implements BasicTableModel {
             /*
                A table built through this builder is a plain data source, so the only
                change signal it has is the one configured through 'updateOn(..)'.
                The list is copy on write because the signal above may be raised on the
                application thread while the UI thread installs the table.
              */
             private final List<TableModelListener> listeners = new CopyOnWriteArrayList<>();

             @Override public int getRowCount() { return rowCount == null ? 0 : rowCount.get(); }
             @Override public int getColumnCount() {
                 if (colCount == null) {
                     if ( columnClass instanceof FixedColumnClasses ) {
                         return ((FixedColumnClasses)columnClass).classes.size();
                     }
                     if ( columnName instanceof FixedColumnNames ) {
                         return ((FixedColumnNames)columnName).names.size();
                     }
                 }
                 return colCount == null ? 0 : colCount.get();
             }
             @Override public @Nullable Object getValueAt(int rowIndex, int colIndex) { return entryGetter == null ? null : entryGetter.get(rowIndex, colIndex); }
             @Override public void setValueAt(@Nullable Object value, int rowIndex, int colIndex) { if ( entrySetter != null ) entrySetter.set(rowIndex, colIndex, (E) value); }
             @Override public Class<?> getColumnClass(int colIndex) { return columnClass == null ? Object.class : columnClass.get(colIndex); }
             @Override public boolean isCellEditable(int rowIndex, int colIndex) { return cellEditable != null && cellEditable.is(rowIndex, colIndex); }
             @Override public @Nullable String getColumnName(int colIndex) {
                 if (columnName == null) {
                     if ( columnClass instanceof FixedColumnClasses ) {
                         return ((FixedColumnClasses)columnClass).get(colIndex).getSimpleName();
                     }
                     return null; // A null name lets the table fall back to the default (spreadsheet style) column name.
                 }
                 return columnName.get(colIndex);
             }
             @Override public void addTableModelListener(TableModelListener l) { listeners.add(l); }
             @Override public void removeTableModelListener(TableModelListener l) { listeners.remove(l); }

             private void fireDataChanged() {
                 TableModelEvent event = new TableModelEvent(this);
                 for ( TableModelListener listener : listeners )
                     listener.tableChanged(event);
             }
         }

     }

}
