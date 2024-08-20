package swingtree.api.model;

import org.jspecify.annotations.Nullable;
import sprouts.Observable;
import sprouts.Event;
import swingtree.UI;
import swingtree.api.Buildable;

import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.util.Objects;

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
    /** {@inheritDoc} */
    @Override default void addTableModelListener(TableModelListener l) {throw new IllegalStateException("Not implemented");}
    /** {@inheritDoc} */
    @Override default void removeTableModelListener(TableModelListener l) {throw new IllegalStateException("Not implemented");}

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
     */
     class Builder<E> implements Buildable<BasicTableModel>
     {
        private final Class<E>       commonEntryType;
        private final RowCount       rowCount;
        private final ColumnCount    colCount;
        private final EntryGetter<E> entryGetter;
        private final EntrySetter<E> entrySetter;
        private final ColumnClass<E> columnClass;
        private final CellEditable   cellEditable;
        private final ColumnName     columnName;
        private final Observable     observableEvent;

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

         public Builder(
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
            return colClass((colIndex) -> colIndex >= classes.length ? commonEntryType : classes[colIndex]);
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
            return colName((colIndex) -> names[colIndex]);
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
                observableEvent.subscribe(()-> UI.run(()->{
                    // We want the table model update to be as thorough as possible, so we
                    // will fire a table structure changed event, followed by a table data
                    // changed event.
                    tm.fireTableStructureChanged();
                    tm.fireTableDataChanged();
                }));
            return tm;
        }

         private class FunTableModel extends AbstractTableModel implements BasicTableModel {
             @Override public int getRowCount() { return rowCount == null ? 0 : rowCount.get(); }
             @Override public int getColumnCount() { return colCount == null ? 0 : colCount.get(); }
             @Override public Object getValueAt(int rowIndex, int colIndex) { return entryGetter == null ? null : entryGetter.get(rowIndex, colIndex); }
             @Override public void setValueAt(Object value, int rowIndex, int colIndex) { if ( entrySetter != null ) entrySetter.set(rowIndex, colIndex, (E) value); }
             @Override public Class<?> getColumnClass(int colIndex) { return columnClass == null ? super.getColumnClass(colIndex) : columnClass.get(colIndex); }
             @Override public boolean isCellEditable(int rowIndex, int colIndex) { return cellEditable != null && cellEditable.is(rowIndex, colIndex); }
             @Override public String getColumnName(int colIndex) { return columnName == null ? super.getColumnName(colIndex) : columnName.get(colIndex); }
         }

     }

}
