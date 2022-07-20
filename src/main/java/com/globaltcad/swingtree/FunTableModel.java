package com.globaltcad.swingtree;

import javax.swing.table.AbstractTableModel;

public class FunTableModel extends AbstractTableModel
{
    @Override
    public int getRowCount() { return 0; }

    @Override
    public int getColumnCount() {
        return 0;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return null;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    }
}
