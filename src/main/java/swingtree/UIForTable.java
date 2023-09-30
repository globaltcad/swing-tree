package swingtree;

import sprouts.Event;
import swingtree.api.Buildable;
import swingtree.api.model.BasicTableModel;
import swingtree.api.model.TableListDataSource;
import swingtree.api.model.TableMapDataSource;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.util.*;

/**
 *  A swing tree builder node for {@link JTable} instances allowing
 *  for a fluent API to build tables in a declarative way.
 */
public class UIForTable<T extends JTable> extends UIForAnySwing<UIForTable<T>, T>
{
    /**
     * Extensions of the {@link  UIForAnySwing} always wrap
     * a single component for which they are responsible.
     *
     * @param component The JComponent type which will be wrapped by this builder node.
     */
    public UIForTable( T component ) { super(component); }

    /**
     *  Use this to build a table cell renderer for a particular column.
     *  The second argument accepts the builder API for a cell renderer.
     *
     * @param columnName The name of the column for which the cell renderer will be built.
     * @param renderBuilder The builder API for a cell renderer.
     * @return This builder node.
     */
    public final UIForTable<T> withRendererForColumn( String columnName, Render.Builder<JTable, ?> renderBuilder ) {
        NullUtil.nullArgCheck(renderBuilder, "renderBuilder", Render.Builder.class);
        return withRendererForColumn(columnName, renderBuilder.getForTable());
    }

    /**
     *  Use this to build a table cell renderer for a particular column.
     *  The second argument accepts the builder API for a cell renderer.
     *
     * @param columnIndex The index of the column for which the cell renderer will be built.
     * @param renderBuilder The builder API for a cell renderer.
     * @return This builder node.
     */
    public final UIForTable<T> withRendererForColumn( int columnIndex, Render.Builder<JTable, ?> renderBuilder ) {
        NullUtil.nullArgCheck(renderBuilder, "renderBuilder", Render.Builder.class);
        return withRendererForColumn(columnIndex, renderBuilder.getForTable());
    }

    /**
     * Use this to register a table cell renderer for a particular column.
     * @param columnName The name of the column for which the cell renderer will be registered.
     * @param renderer The cell renderer to be registered.
     * @return This builder node, to allow for builder-style method chaining.
     */
    public final UIForTable<T> withRendererForColumn( String columnName, TableCellRenderer renderer ) {
        NullUtil.nullArgCheck(columnName, "columnName", String.class);
        NullUtil.nullArgCheck(renderer, "renderer", TableCellRenderer.class);
        getComponent().getColumn(columnName).setCellRenderer(renderer);
        return this;
    }

    /**
     * Use this to register a table cell renderer for a particular column.
     * @param columnIndex The index of the column for which the cell renderer will be registered.
     * @param renderer The cell renderer to be registered.
     * @return This builder node.
     */
    public final UIForTable<T> withRendererForColumn( int columnIndex, TableCellRenderer renderer ) {
        NullUtil.nullArgCheck(renderer, "renderer", TableCellRenderer.class);
        getComponent().getColumnModel().getColumn(columnIndex).setCellRenderer(renderer);
        return this;
    }

    /**
     * Use this to register a table cell renderer for all columns of this table.
     * @param renderer The cell renderer to be registered.
     * @return This builder node.
     */
    public final UIForTable<T> withRenderer( TableCellRenderer renderer ) {
        NullUtil.nullArgCheck(renderer, "renderer", TableCellRenderer.class);
        getComponent().setDefaultRenderer(Object.class, renderer);
        return this;
    }

    /**
     * Use this to register a table cell renderer for all columns of this table.
     * @param renderBuilder The builder API for a cell renderer.
     * @return  This builder node.
     */
    public final UIForTable<T> withRenderer( Render.Builder<JTable, ?> renderBuilder ) {
        NullUtil.nullArgCheck(renderBuilder, "renderBuilder", Render.Builder.class);
        return withRenderer(renderBuilder.getForTable());
    }

    /**
     * Use this to register a table cell editor for a particular column.
     * @param columnName The name of the column for which the cell editor will be registered.
     * @param editor The cell editor to be registered.
     * @return This builder instance, to allow for method chaining.
     */
    public final UIForTable<T> withCellEditorForColumn( String columnName, TableCellEditor editor ) {
        NullUtil.nullArgCheck(columnName, "columnName", String.class);
        NullUtil.nullArgCheck(editor, "editor", TableCellEditor.class);
        getComponent().getColumn(columnName).setCellEditor(editor);
        return this;
    }

    /**
     * Use this to register a table cell editor for a particular column.
     * @param columnIndex The index of the column for which the cell editor will be registered.
     * @param editor The cell editor to be registered.
     * @return This builder node, to allow for builder-style method chaining.
     */
    public final UIForTable<T> withCellEditorForColumn( int columnIndex, TableCellEditor editor ) {
        NullUtil.nullArgCheck(editor, "editor", TableCellEditor.class);
        getComponent().getColumnModel().getColumn(columnIndex).setCellEditor(editor);
        return this;
    }

    /**
     *  Use this to set a table model.
     *  The provided argument is a builder object whose build method will be called
     *  for you instead of having to call the build method on the builder object yourself.
     * @param dataModelBuilder The builder object which will be used to build and then set the table model.
     * @return This builder object.
     */
    public final UIForTable<T> withModel( Buildable<BasicTableModel> dataModelBuilder ) {
        return this.withModel(dataModelBuilder.build());
    }

    /**
     * Use this to set a table model.
     * @param model The model for the table model.
     * @return This builder object.
     */
    public final UIForTable<T> withModel( BasicTableModel model ) {
        NullUtil.nullArgCheck(model, "model", BasicTableModel.class);
        getComponent().setModel(model);
        return this;
    }

    /**
     *  Use this instead of {@link JTable#setModel(TableModel)} if your table data can be represented through
     *  either a row major {@link List} of {@link List}s of entry {@link Object}s (a list of rows)      <br>
     *  or a columns major {@link List} of {@link List}s of entry {@link Object}s (a list of columns).  <br>
     *  This method will automatically create a {@link AbstractTableModel} instance for you.
     *  <p>
     *      <b>Please note that when the data of the provided data source changes (i.e. when the data source
     *      is a {@link List} and the list is modified), the table model will not be updated automatically!
     *      Use {@link #updateTableOn(sprouts.Event)} to bind an update {@link sprouts.Event} to the table model.</b>
     *
     * @param mode An enum which configures the layout as well as modifiability of the table in a readable fashion.
     * @param dataSource The {@link TableListDataSource} returning a list matrix which will be used to populate the table.
     * @return This builder node.
     * @param <E> The type of the table entry {@link Object}s.
     */
    public final <E> UIForTable<T> withModel( UI.ListData mode, TableListDataSource<E> dataSource ) {
        boolean isRowMajor = mode.isRowMajor();
        boolean isEditable = mode.isEditable();
        if ( isRowMajor ) {
            getComponent().setModel(new ListBasedTableModel<E>(isEditable, dataSource)
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
            getComponent().setModel(new ListBasedTableModel<E>(isEditable, dataSource)
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

    /**
     *  Use this instead of {@link JTable#setModel(TableModel)} if your table data can be represented based
     *  on a map of column names to lists of table entries (basically a column major matrix).  <br>
     *  This method will automatically create a {@link AbstractTableModel} instance for you.
     *  <p>
     *      <b>Please note that when the data of the provided data source changes (i.e. when the data source
     *      is a {@link Map} which gets modified), the table model will not be updated automatically!
     *      Use {@link #updateTableOn(sprouts.Event)} to bind an update {@link sprouts.Event} to the table model.</b>
     *
     * @param mode An enum which configures the modifiability of the table in a readable fashion.
     * @param dataSource The {@link TableMapDataSource} returning a column major map based matrix which will be used to populate the table.
     * @return This builder node.
     * @param <E> The type of the table entry {@link Object}s.
     */
    public final <E> UIForTable<T> withModel( UI.MapData mode, TableMapDataSource<E> dataSource ) {
        getComponent().setModel(new MapBasedColumnMajorTableModel<>(mode.isEditable(), dataSource));
        return this;
    }

    /**
     *  Use this to bind an {@link sprouts.Event} to the {@link TableModel} of this table
     *  which will trigger the {@link AbstractTableModel#fireTableDataChanged()} method.
     *  This is useful if you want to update the table when the data source changes.
     *
     * @param event The event to be bound.
     * @return This builder node, for chaining.
     */
    public final UIForTable<T> updateTableOn( Event event ) {
        NullUtil.nullArgCheck(event, "event", Event.class);
        event.subscribe(()->
            _doUI(()->{
                TableModel model = getComponent().getModel();
                if ( model instanceof AbstractTableModel ) {
                    // We want the table model update to be as thorough as possible, so we
                    // will fire a table structure changed event, followed by a table data
                    // changed event.
                    ((AbstractTableModel)model).fireTableStructureChanged();
                    ((AbstractTableModel)model).fireTableDataChanged();
                }
                else
                    throw new IllegalStateException("The table model is not an AbstractTableModel instance.");
            })
        );
        return this;
    }


    private static abstract class ListBasedTableModel<E> extends AbstractTableModel
    {
        private final TableListDataSource<E> dataSource;
        private final boolean isEditable;

        ListBasedTableModel(boolean isEditable, TableListDataSource<E> dataSource) {
            this.isEditable = isEditable;
            this.dataSource = dataSource;
        }

        @Override public boolean isCellEditable( int rowIndex, int columnIndex ) { return this.isEditable; }

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


    private abstract static class MapBasedTableModel<E> extends AbstractTableModel
    {
        private final TableMapDataSource<E> dataSource;
        private final boolean isEditable;

        MapBasedTableModel(boolean isEditable, TableMapDataSource<E> dataSource) {
            this.isEditable = isEditable;
            this.dataSource = dataSource;
        }

        protected Map<String, List<E>> getData() {
            Map<String, List<E>> data = dataSource.get();
            if ( data == null ) return Collections.emptyMap(); // We really don't want null pointer in UIs.
            return data;
        }

        @Override
        public String getColumnName(int column) {
            List<String> columnNames = new ArrayList<>(getData().keySet());
            if ( column < 0 || column >= columnNames.size() ) return null;
            return columnNames.get(column);
        }

        @Override public boolean isCellEditable( int rowIndex, int columnIndex ) { return this.isEditable; }


        protected boolean isNotWithinBounds(int rowIndex, int colIndex) {
            if ( rowIndex < 0 || rowIndex >= getRowCount()     ) return true;
            if ( colIndex < 0 || colIndex >= getColumnCount()  ) return true;
            return false;
        }

    }

    private static class MapBasedColumnMajorTableModel<E> extends MapBasedTableModel<E>
    {
        MapBasedColumnMajorTableModel(boolean isEditable, TableMapDataSource<E> dataSource) {
            super(isEditable, dataSource);
        }

        @Override
        public int getRowCount() {
            Map<String, List<E>> data = getData();
            return data.values()
                        .stream()
                        .filter(Objects::nonNull) // Again, we don't want null pointer exceptions in UIs.
                        .mapToInt(List::size)
                        .max()
                        .orElse(0);
        }

        @Override
        public int getColumnCount() { return getData().size(); }

        @Override
        public Object getValueAt( int rowIndex, int columnIndex ) {
            if ( isNotWithinBounds(rowIndex, columnIndex) ) return null;
            List<E> column = getData().values().stream().skip(columnIndex).findFirst().orElse(null);
            if ( column == null ) return null;
            if ( rowIndex < 0 || rowIndex >= column.size() ) return null;
            return column.get(rowIndex);
        }
    }

}
