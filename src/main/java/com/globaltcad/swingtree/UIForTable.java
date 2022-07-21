package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.Buildable;
import com.globaltcad.swingtree.api.model.BasicTableModel;
import com.globaltcad.swingtree.api.model.TableListDataSource;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.List;

public class UIForTable<T extends JTable> extends UIForAbstractSwing<UIForTable<T>, T>
{
    /**
     * Extensions of the {@link  UIForAbstractSwing} always wrap
     * a single component for which they are responsible.
     *
     * @param component The JComponent type which will be wrapped by this builder node.
     */
    public UIForTable(T component) {
        super(component);
    }

    public final UIForTable<T> withModel(Buildable<BasicTableModel> dataModelBuilder) {
        return this.withModel(dataModelBuilder.build());
    }

    public final UIForTable<T> withModel(BasicTableModel dataSource) {
        LogUtil.nullArgCheck(dataSource, "dataSource", BasicTableModel.class);
        _component.setModel(new AbstractTableModel() {
            @Override public int getRowCount() { return dataSource.getRowCount(); }
            @Override public int getColumnCount() { return dataSource.getColumnCount(); }
            @Override public Object getValueAt(int rowIndex, int columnIndex) { return dataSource.getValueAt(rowIndex, columnIndex); }
            @Override public void setValueAt(Object aValue, int rowIndex, int columnIndex) { dataSource.setValueAt(aValue, rowIndex, columnIndex); }
            @Override public boolean isCellEditable(int rowIndex, int columnIndex) { return dataSource.isCellEditable(rowIndex, columnIndex); }
        });
        return this;
    }

    /**
     *  Use this instead of {@link JTable#setModel(TableModel)} if your table data can be represented through
     *  either a row major {@link List} of {@link List}s of entry {@link Object}s (a list of rows)      <br>
     *  or a columns major {@link List} of {@link List}s of entry {@link Object}s (a list of columns).  <br>
     *  This method will automatically create a {@link AbstractTableModel} instance for you.
     *
     * @param model An enum which configures the layout as well as modifiability of the table in a readable fashion.
     * @param dataSource The {@link TableListDataSource} returning a list matrix which will be used to populate the table.
     * @return This builder node.
     * @param <E> The type of the table entry {@link Object}s.
     */
    public final <E> UIForTable<T> with(UI.TableData model, TableListDataSource<E> dataSource) {
        boolean isRowMajor = model.isRowMajor();
        boolean isEditable = model.isEditable();
        if ( isRowMajor ) {
            _component.setModel(new ListBasedTableModel<E>(isEditable, dataSource)
            {
                @Override public int getRowCount() { return getData().size(); }
                @Override public int getColumnCount() {
                    List<List<E>> data = getData();
                    return (data.isEmpty() ? 0 : data.get(0).size());
                }
                @Override public Object getValueAt(int rowIndex, int columnIndex) {
                    List<List<E>> data = getData();
                    if (isNotWithinBounds(rowIndex, columnIndex)) return null;
                    return data.get(rowIndex).get(columnIndex);
                }
                @Override public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                    List<List<E>> data = getData();
                    if ( !isEditable || isNotWithinBounds(rowIndex, columnIndex) ) return;
                    data.get(rowIndex).set(columnIndex, (E)aValue);
                }
            });
        } else { // isColumnMajor
            _component.setModel(new ListBasedTableModel<E>(isEditable, dataSource)
            {
                @Override public int getRowCount() {
                    List<List<E>> data = getData();
                    return (data.isEmpty() ? 0 : data.get(0).size());
                }
                @Override public int getColumnCount() { return getData().size(); }
                @Override public Object getValueAt(int rowIndex, int columnIndex) {
                    List<List<E>> data = getData();
                    if (isNotWithinBounds(rowIndex, columnIndex)) return null;
                    return data.get(columnIndex).get(rowIndex);
                }
                @Override public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                    List<List<E>> data = getData();
                    if ( !isEditable || isNotWithinBounds(rowIndex, columnIndex) ) return;
                    data.get(columnIndex).set(rowIndex, (E)aValue);
                }
            });
        }
        return this;
    }

    private static abstract class ListBasedTableModel<E> extends AbstractTableModel {

        private final TableListDataSource<E> dataSource;
        private final boolean isEditable;


        ListBasedTableModel(boolean isEditable, TableListDataSource<E> dataSource) {
            this.isEditable = isEditable;
            this.dataSource = dataSource;
        }

        @Override public boolean isCellEditable(int rowIndex, int columnIndex) { return this.isEditable; }

        protected List<List<E>> getData() {
            List<List<E>> data = dataSource.get();
            if ( data == null ) return new ArrayList<>(); // We really don't want null pointer in UIs.
            return data;
        }
        protected boolean isNotWithinBounds(int rowIndex, int colIndex) {
            if ( rowIndex < 0 || rowIndex >= getRowCount()     ) return true;
            if ( colIndex < 0 || colIndex >= getColumnCount()  ) return true;
            return false;
        }
    }

}
