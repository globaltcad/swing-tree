package com.globaltcad.swingtree.api.model;

import com.globaltcad.swingtree.api.Buildable;

import javax.swing.event.TableModelListener;
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

         /**
          *  Use this to define the lambda which dynamically determines the row count of the table model.
          * @param rowCount The lambda which will be used to determine the row count of the table model.
          * @return This builder instance.
          */
        public Builder onRowCount(RowCount rowCount) {
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
            if ( this.columnClass != null ) throw new IllegalStateException(ColumnClass.class.getSimpleName()+" already set");
            this.columnClass = columnClass;
            return this;
        }
        /**
         *  Use this to define the lambda which allows the {@link javax.swing.JTable} to determine if the cell at a given row and column is editable.
         * @param cellEditable The lambda which will be used to determine if the cell at a given row and column is editable.
         * @return This builder instance.
         */
        public Builder onIsEditable(CellEditable cellEditable) {
            if ( this.cellEditable != null ) throw new IllegalStateException(CellEditable.class.getSimpleName()+" already set");
            this.cellEditable = cellEditable;
            return this;
        }
        /**
         *  Use this to build the {@link BasicTableModel} instance.
         * @return The {@link BasicTableModel} instance.
         */
        @Override public BasicTableModel build() {
            return new BasicTableModel() {
                @Override public int getRowCount() { return rowCount.get(); }
                @Override public int getColumnCount() { return colCount.get(); }
                @Override public Object getValueAt(int rowIndex, int colIndex) { return valueAt.get(rowIndex, colIndex); }
                @Override public void setValueAt(Object value, int rowIndex, int colIndex) { setValueAt.set(rowIndex, colIndex, value); }
                @Override public Class<?> getColumnClass(int colIndex) { return columnClass.get(colIndex); }
                @Override public boolean isCellEditable(int rowIndex, int colIndex) { return cellEditable.is(rowIndex, colIndex); }
            };
        }

     }
}
