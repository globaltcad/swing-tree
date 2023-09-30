package swingtree.api.model;

import sprouts.Observable;
import sprouts.Event;
import swingtree.UI;
import swingtree.api.Buildable;

import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

/**
 *  This interface defines a basic table model which can be used to create a table model using lambda expressions.
 *  Implementations of this are typically created declarative like so: <br>
 *  <pre>{@code
 *      UI.table().withModel(
 *          UI.tableModel()
 *          .colNames("A", "B")
 *          .colCount(()->2)
 *          .rowCount(()->3)
 *          .getter((int row, int col)->
 *              vm.getData(row, col)
 *          )
 *      )
 *  }</pre>
 */
public interface BasicTableModel extends TableModel
{
    /** {@inheritDoc} */
    int getRowCount();
    /** {@inheritDoc} */
    int getColumnCount();
    /** {@inheritDoc} */
    Object getValueAt(int rowIndex, int columnIndex);
    /** {@inheritDoc} */
    void setValueAt(Object aValue, int rowIndex, int columnIndex);
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
    @FunctionalInterface interface ValueAt { Object get(int rowIndex, int colIndex); }
    /**
     *  Implementations of this functional interface translate to the {@link TableModel#setValueAt(Object, int, int)} method.
     */
    @FunctionalInterface interface SetValueAt { void set(int rowIndex, int colIndex, Object aValue); }
    /**
     *  Implementations of this functional interface translate to the {@link TableModel#getColumnClass(int)} method.
     */
    @FunctionalInterface interface ColumnClass { Class<?> get(int colIndex); }
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
     class Builder implements Buildable<BasicTableModel>
     {
        private RowCount rowCount;
        private ColumnCount colCount;
        private ValueAt valueAt;
        private SetValueAt setValueAt;
        private ColumnClass columnClass;
        private CellEditable cellEditable;
        private ColumnName columnName;
        private Observable noticeableEvent;

         /**
          *  Use this to define the lambda which dynamically determines the row count of the table model.
          * @param rowCount The lambda which will be used to determine the row count of the table model.
          * @return This builder instance.
          */
        public Builder rowCount( RowCount rowCount ) {
            if ( rowCount == null ) throw new IllegalArgumentException("rowCount cannot be null");
            if ( this.rowCount != null ) throw new IllegalStateException(RowCount.class.getSimpleName()+" already set");
            this.rowCount = rowCount;
            return this;
        }
        /**
         *  Use this to define the lambda which dynamically determines the column count of the table model.
         * @param columnCount The lambda which will be used to determine the column count of the table model.
         * @return This builder instance.
         */
        public Builder colCount( ColumnCount columnCount ) {
            if ( columnCount == null ) throw new IllegalArgumentException("columnCount cannot be null");
            if ( this.colCount != null ) throw new IllegalStateException(ColumnCount.class.getSimpleName()+" already set");
            this.colCount = columnCount;
            return this;
        }
        /**
         *  Accepts a lambda allowing the {@link javax.swing.JTable} to dynamically determines the value at a given row and column.
         * @param valueAt The lambda which will be used to determine the value at a given row and column.
         * @return This builder instance.
         */
        public Builder getter( ValueAt valueAt ) {
            if ( valueAt == null ) throw new IllegalArgumentException("valueAt cannot be null");
            if ( this.valueAt != null ) throw new IllegalStateException(ValueAt.class.getSimpleName()+" already set");
            this.valueAt = valueAt;
            return this;
        }
        /**
         *  Accepts a lambda allowing lambda which allows the user of the {@link javax.swing.JTable} to set the value at a given row and column.
         * @param setValueAt The lambda which will be used to set the value at a given row and column.
         * @return This builder instance.
         */
        public Builder setter( SetValueAt setValueAt ) {
            if ( setValueAt == null ) throw new IllegalArgumentException("setValueAt cannot be null");
            if ( this.setValueAt != null ) throw new IllegalStateException(SetValueAt.class.getSimpleName()+" already set");
            this.setValueAt = setValueAt;
            return this;
        }
        /**
         *  Accepts a lambda which allows the {@link javax.swing.JTable} to determine the class of the column at a given index.
         * @param columnClass The lambda which will be used to determine the class of the column at a given index.
         * @return This builder instance.
         */
        public Builder colClass( ColumnClass columnClass ) {
            if ( columnClass == null ) throw new IllegalArgumentException("columnClass cannot be null");
            if ( this.columnClass != null ) throw new IllegalStateException(ColumnClass.class.getSimpleName()+" already set");
            this.columnClass = columnClass;
            return this;
        }
        /**
         *  Use this to define a fixed array of column classes.
         * @param classes An array of column classes.
         * @return This builder instance.
         */
        public Builder colClasses( Class<?>... classes ) {
            if ( classes == null ) throw new IllegalArgumentException("classes cannot be null");
            return colClass((colIndex) -> colIndex >= classes.length ? Object.class : classes[colIndex]);
        }
        /**
         *  Accepts a lambda allowing the {@link javax.swing.JTable} to determine if the cell at a given row and column is editable.
         * @param cellEditable The lambda which will be used to determine if the cell at a given row and column is editable.
         * @return This builder instance.
         */
        public Builder isEditableIf( CellEditable cellEditable ) {
            if ( cellEditable == null ) throw new IllegalArgumentException("cellEditable cannot be null");
            if ( this.cellEditable != null ) throw new IllegalStateException(CellEditable.class.getSimpleName()+" already set");
            this.cellEditable = cellEditable;
            return this;
        }
         /**
          *  Use this to define the lambda which allows the {@link javax.swing.JTable} to determine the name of the column at a given index.
          * @param columnName The lambda which will be used to determine the name of the column at a given index.
          * @return This builder instance.
          */
        public Builder colName( ColumnName columnName ) {
            if ( columnName == null ) throw new IllegalArgumentException("columnName cannot be null");
            if (this.columnName != null)
                throw new IllegalStateException(ColumnName.class.getSimpleName() + " already set");
            this.columnName = columnName;
            return this;
        }
        public Builder colNames( String... names ) {
            if ( names == null ) throw new IllegalArgumentException("names cannot be null");
            return colName((colIndex) -> names[colIndex]);
        }
        /**
         *  Use this to define the event which will be fired when the table model is updated.
         * @param updateEvent The event which will be fired when the table model is updated.
         * @return This builder instance.
         */
        public Builder updateOn( Observable updateEvent ) {
            if ( updateEvent == null ) throw new IllegalArgumentException("updateEvent cannot be null");
            if ( this.noticeableEvent != null ) throw new IllegalStateException(Event.class.getSimpleName()+" already set");
            this.noticeableEvent = updateEvent;
            return this;
        }
        /**
         *  Use this to build the {@link BasicTableModel} instance.
         * @return The {@link BasicTableModel} instance.
         */
        @Override public BasicTableModel build() {
            FunTableModel tm = new FunTableModel();
            if ( noticeableEvent != null )
                noticeableEvent.subscribe(()-> UI.run(()->{
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
             @Override public Object getValueAt(int rowIndex, int colIndex) { return valueAt == null ? null : valueAt.get(rowIndex, colIndex); }
             @Override public void setValueAt(Object value, int rowIndex, int colIndex) { if ( setValueAt != null ) setValueAt.set(rowIndex, colIndex, value); }
             @Override public Class<?> getColumnClass(int colIndex) { return columnClass == null ? super.getColumnClass(colIndex) : columnClass.get(colIndex); }
             @Override public boolean isCellEditable(int rowIndex, int colIndex) { return cellEditable != null && cellEditable.is(rowIndex, colIndex); }
             @Override public String getColumnName(int colIndex) { return columnName == null ? super.getColumnName(colIndex) : columnName.get(colIndex); }
         }

     }

}
