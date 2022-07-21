package com.globaltcad.swingtree.api.model;

import com.globaltcad.swingtree.api.Buildable;

import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

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

    @FunctionalInterface interface RowCount { int get(); }
    @FunctionalInterface interface ColumnCount { int get(); }
    @FunctionalInterface interface ValueAt { Object get(int rowIndex, int colIndex); }
    @FunctionalInterface interface SetValueAt { void set(int rowIndex, int colIndexx, Object aValue); }
    @FunctionalInterface interface ColumnClass { Class<?> get(int colIndex); }
    @FunctionalInterface interface CellEditable { boolean is(int rowIndex, int colIndex); }
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

         /**
          *  Use this to define the lambda which dynamically determines the row count of the table model.
          * @param rowCount The lambda which will be used to determine the row count of the table model.
          * @return This builder instance.
          */
        public Builder onRowCount(RowCount rowCount) {
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
        public Builder onColCount(ColumnCount columnCount) {
            if ( columnCount == null ) throw new IllegalArgumentException("columnCount cannot be null");
            if ( this.colCount != null ) throw new IllegalStateException(ColumnCount.class.getSimpleName()+" already set");
            this.colCount = columnCount;
            return this;
        }
        /**
         *  Use this to define the lambda which dynamically determines the value at a given row and column.
         * @param valueAt The lambda which will be used to determine the value at a given row and column.
         * @return This builder instance.
         */
        public Builder onGet(ValueAt valueAt) {
            if ( valueAt == null ) throw new IllegalArgumentException("valueAt cannot be null");
            if ( this.valueAt != null ) throw new IllegalStateException(ValueAt.class.getSimpleName()+" already set");
            this.valueAt = valueAt;
            return this;
        }
        /**
         *  Use this to define the lambda which allows the user of the {@link javax.swing.JTable} to set the value at a given row and column.
         * @param setValueAt The lambda which will be used to set the value at a given row and column.
         * @return This builder instance.
         */
        public Builder onSet(SetValueAt setValueAt) {
            if ( setValueAt == null ) throw new IllegalArgumentException("setValueAt cannot be null");
            if ( this.setValueAt != null ) throw new IllegalStateException(SetValueAt.class.getSimpleName()+" already set");
            this.setValueAt = setValueAt;
            return this;
        }
        /**
         *  Use this to define the lambda which allows the {@link javax.swing.JTable} to determine the class of the column at a given index.
         * @param columnClass The lambda which will be used to determine the class of the column at a given index.
         * @return This builder instance.
         */
        public Builder onColClass(ColumnClass columnClass) {
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
        public Builder colClasses(Class<?>... classes) {
            if ( classes == null ) throw new IllegalArgumentException("classes cannot be null");
            return onColClass((colIndex) -> classes[colIndex]);
        }
        /**
         *  Use this to define the lambda which allows the {@link javax.swing.JTable} to determine if the cell at a given row and column is editable.
         * @param cellEditable The lambda which will be used to determine if the cell at a given row and column is editable.
         * @return This builder instance.
         */
        public Builder onIsEditable(CellEditable cellEditable) {
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
        public Builder onColName(ColumnName columnName) {
            if ( columnName == null ) throw new IllegalArgumentException("columnName cannot be null");
            if (this.columnName != null)
                throw new IllegalStateException(ColumnName.class.getSimpleName() + " already set");
            this.columnName = columnName;
            return this;
        }
        public Builder colNames(String... names) {
            if ( names == null ) throw new IllegalArgumentException("names cannot be null");
            return onColName((colIndex) -> names[colIndex]);
        }
        /**
         *  Use this to build the {@link BasicTableModel} instance.
         * @return The {@link BasicTableModel} instance.
         */
        @Override public BasicTableModel build() { return new FunTableModel(); }

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
