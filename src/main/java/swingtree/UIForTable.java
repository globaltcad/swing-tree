package swingtree;

import org.jspecify.annotations.Nullable;
import sprouts.Event;
import swingtree.api.Buildable;
import swingtree.api.model.BasicTableModel;
import swingtree.api.model.TableListDataSource;
import swingtree.api.model.TableMapDataSource;

import javax.swing.*;
import javax.swing.table.*;
import java.security.PublicKey;
import java.util.*;
import java.util.function.Function;

/**
 *  A SwingTree builder node designed for configuring {@link JTable} instances allowing
 *  for a fluent API to build tables in a declarative way.
 */
public final class UIForTable<T extends JTable> extends UIForAnySwing<UIForTable<T>, T>
{
    private final BuilderState<T> _state;


    /**
     * Extensions of the {@link  UIForAnySwing} always wrap
     * a single component for which they are responsible.
     *
     * @param state The {@link BuilderState} modelling how the component is built.
     */
    UIForTable( BuilderState<T> state ) {
        Objects.requireNonNull(state);
        _state = state;
    }

    @Override
    protected BuilderState<T> _state() {
        return _state;
    }
    
    @Override
    protected UIForTable<T> _newBuilderWithState(BuilderState<T> newState ) {
        return new UIForTable<>(newState);
    }

    /**
     *  Use this to set the table header.
     *
     * @param header The table header to be set.
     * @return This builder node.
     */
    public final UIForTable<T> withHeader( UIForTableHeader<?> header ) {
        NullUtil.nullArgCheck(header, "header", UIForTableHeader.class);
        return withHeader(header.getComponent());
    }

    /**
     *  Use this to set the table header.
     *
     * @param header The table header to be set.
     * @return This builder node.
     */
    public final UIForTable<T> withHeader( JTableHeader header ) {
        NullUtil.nullArgCheck(header, "header", JTableHeader.class);
        return _with( thisComponent -> {
                    thisComponent.setTableHeader(header);
                })
                ._this();
    }

    /**
     *  Use this to build a table cell renderer for a particular column.
     *  The second argument accepts the builder API for a cell renderer.
     *
     * @param columnName The name of the column for which the cell renderer will be built.
     * @param renderBuilder The builder API for a cell renderer.
     * @return This builder node.
     * @deprecated Use {@link #withRendererForColumn(String, Function)} instead.
     */
    @Deprecated
    public final UIForTable<T> withRendererForColumn( String columnName, Render.Builder<JTable, ?> renderBuilder ) {
        NullUtil.nullArgCheck(renderBuilder, "renderBuilder", Render.Builder.class);
        return withRenderComponentForColumn(columnName, renderBuilder.getForTable());
    }

    private static Render.Builder<JTable, Object> _renderTable() {
        return Render.forTable(Object.class)
                .when(Object.class)
                .asText( cell -> cell.valueAsString().orElse("") );
    }

    /**
     *  Use this to build a table cell renderer for a particular column.
     *  The second argument accepts a lambda function which exposes the builder API for a cell renderer.
     *  Here is an example of how to use this method:
     * <pre>{@code
     *     UI.table(myModel)
     *     .withRendererForColumn("column1",
     *         UI.renderTable()
     *         .when(String.class)
     *         .asText( cell -> "[" + cell.valueAsString().orElse("") + "]" ) )
     *     )
     *     .withRendererForColumn("column2",
     *         UI.renderTable()
     *         .when(Float.class)
     *         .asText( cell -> "(" + cell.valueAsString().orElse("") + "f)" ) )
     *         .when(Double.class)
     *         .asText( cell -> "(" + cell.valueAsString().orElse("") + "d)" ) )
     *     );
     * }</pre>
     *
     * @param columnName The name of the column for which the cell renderer will be built.
     * @param renderBuilder A lambda function which exposes a fluent builder API for a cell renderer
     *                      and returns the builder API for a cell renderer.
     *                      Call the appropriate methods on the builder API to configure the cell renderer.
     * @return This builder node.
     */
    public final UIForTable<T> withRendererForColumn(
        String columnName,
        Function<Render.Builder<JTable, ?>, Render.Builder<JTable, ?>> renderBuilder
    ) {
        NullUtil.nullArgCheck(renderBuilder, "renderBuilder", Render.Builder.class);
        Render.Builder<JTable, ?> builder = renderBuilder.apply(_renderTable());
        return withRenderComponentForColumn(columnName, builder.getForTable());
    }

    /**
     *  Use this to build a table cell renderer for a particular column.
     *  The second argument accepts the builder API for a cell renderer.
     *
     * @param columnIndex The index of the column for which the cell renderer will be built.
     * @param renderBuilder The builder API for a cell renderer.
     * @return This builder node.
     * @deprecated Use {@link #withRendererForColumn(int, Function)} instead.
     */
    @Deprecated
    public final UIForTable<T> withRendererForColumn( int columnIndex, Render.Builder<JTable, ?> renderBuilder ) {
        NullUtil.nullArgCheck(renderBuilder, "renderBuilder", Render.Builder.class);
        return withRenderComponentForColumn(columnIndex, renderBuilder.getForTable());
    }

    /**
     *  Use this to build a table cell renderer for a particular column.
     *  The second argument accepts a lambda function which exposes the builder API for a cell renderer.
     *  Here an example of how this method may be used:
     * <pre>{@code
     *     UI.table(myModel)
     *     .withRendererForColumn(0,
     *         UI.renderTable()
     *         .when(String.class)
     *         .asText( cell -> "[" + cell.valueAsString().orElse("") + "]" ) )
     *     )
     *     .withRendererForColumn(1,
     *         UI.renderTable()
     *         .when(Float.class)
     *         .asText( cell -> "(" + cell.valueAsString().orElse("") + "f)" ) )
     *         .when(Double.class)
     *         .asText( cell -> "(" + cell.valueAsString().orElse("") + "d)" ) )
     *     );
     * }</pre>
     *
     * @param columnIndex The index of the column for which the cell renderer will be built.
     * @param renderBuilder A lambda function which exposes a fluent builder API for a cell renderer
     *                      and returns the builder API for a cell renderer.
     *                      Call the appropriate methods on the builder API to configure the cell renderer.
     * @return This builder node.
     */
    public final UIForTable<T> withRendererForColumn(
        int columnIndex,
        Function<Render.Builder<JTable, ?>, Render.Builder<JTable, ?>> renderBuilder
    ) {
        NullUtil.nullArgCheck(renderBuilder, "renderBuilder", Render.Builder.class);
        Render.Builder<JTable, ?> builder = renderBuilder.apply(_renderTable());
        return withRenderComponentForColumn(columnIndex, builder.getForTable());
    }

    /**
     * Use this to register a table cell renderer for a particular column.
     * A {@link TableCellRenderer} is a supplier of {@link java.awt.Component} instances which are used to render
     * the cells of a table.
     * <b>Note that in SwingTree, the preferred way of defining a cell renderer for a particular column is through the
     * {@link #withRendererForColumn(String, Function)} method, which allows for a more fluent and declarative
     * way of defining cell renderers.</b>
     *
     * @param columnName The name of the column for which the cell renderer will be registered.
     * @param renderer The cell renderer to be registered.
     * @return This builder node, to allow for builder-style method chaining.
     */
    public final UIForTable<T> withRenderComponentForColumn( String columnName, TableCellRenderer renderer ) {
        NullUtil.nullArgCheck(columnName, "columnName", String.class);
        NullUtil.nullArgCheck(renderer, "renderer", TableCellRenderer.class);
        return _with( thisComponent -> {
                    thisComponent.getColumn(columnName).setCellRenderer(renderer);
                })
                ._this();
    }

    /**
     * Use this to register a table cell renderer for a particular column. <br>
     * A {@link TableCellRenderer} is a supplier of {@link java.awt.Component} instances which are used to render
     * the cells of a table.
     * <b>Note that in SwingTree, the preferred way of defining a cell renderer for a particular column is through the
     * {@link #withRendererForColumn(int, Function)} method, which allows for a more fluent and declarative
     * way of defining cell renderers.</b>
     *
     * @param columnIndex The index of the column for which the cell renderer will be registered.
     * @param renderer The cell renderer to be registered.
     * @return This builder node.
     */
    public final UIForTable<T> withRenderComponentForColumn( int columnIndex, TableCellRenderer renderer ) {
        NullUtil.nullArgCheck(renderer, "renderer", TableCellRenderer.class);
        return _with( thisComponent -> {
                    thisComponent.getColumnModel().getColumn(columnIndex).setCellRenderer(renderer);
                })
                ._this();
    }

    /**
     *  Use this to register a table cell renderer for all columns of this table.<br>
     *  A {@link TableCellRenderer} is a supplier of {@link java.awt.Component} instances which are used to render
     *  the cells of a table.<br><br>
     *  <b>Note that in SwingTree, the preferred way of defining a cell renderer is through the
     *  {@link #withRenderer(Function)} method, which allows for a more fluent and declarative
     *  way of defining cell renderers.</b>
     *
     * @param renderer A provider of {@link java.awt.Component} instances which are used to render the cells of a table.
     * @return This builder instance, to allow for method chaining.
     */
    public final UIForTable<T> withRenderComponent( TableCellRenderer renderer ) {
        NullUtil.nullArgCheck(renderer, "renderer", TableCellRenderer.class);
        return _with( thisComponent -> {
                    thisComponent.setDefaultRenderer(Object.class, renderer);
                })
                ._this();
    }

    /**
     * Use this to register a table cell renderer for all columns of this table.
     * @param renderBuilder The builder API for a cell renderer.
     * @return  This builder node.
     * @deprecated Use {@link #withRenderer(Function)} instead.
     */
    @Deprecated
    public final UIForTable<T> withRenderer( Render.Builder<JTable, ?> renderBuilder ) {
        NullUtil.nullArgCheck(renderBuilder, "renderBuilder", Render.Builder.class);
        return withRenderComponent(renderBuilder.getForTable());
    }

    /**
     *  Use this to define a table cell renderer for all columns of this table
     *  using the fluent builder API exposed to the provided lambda function.<br>
     *  Here is an example of how this method is used:
     *  <pre>{@code
     *    UI.table()
     *    .withRenderer(it -> it.asText(cell -> cell.value().get().toString() ))
     *    // ...
     *  }</pre>
     *
     * @param renderBuilder A lambda function which exposes the builder API for a cell renderer
     *                      and returns the builder API for a cell renderer.
     *                      Call the appropriate methods on the builder API to configure the cell renderer.
     * @return This builder node.
     */
    public final UIForTable<T> withRenderer(
        Function<Render.Builder<JTable, ?>, Render.Builder<JTable, ?>> renderBuilder
    ) {
        NullUtil.nullArgCheck(renderBuilder, "renderBuilder", Render.Builder.class);
        Render.Builder<JTable, ?> builder = renderBuilder.apply(_renderTable());
        Objects.requireNonNull(builder);
        return withRenderComponent(builder.getForTable());
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
        return _with( thisComponent -> {
                    thisComponent.getColumn(columnName).setCellEditor(editor);
                })
                ._this();
    }

    /**
     * Use this to register a table cell editor for a particular column.
     * @param columnIndex The index of the column for which the cell editor will be registered.
     * @param editor The cell editor to be registered.
     * @return This builder node, to allow for builder-style method chaining.
     */
    public final UIForTable<T> withCellEditorForColumn( int columnIndex, TableCellEditor editor ) {
        NullUtil.nullArgCheck(editor, "editor", TableCellEditor.class);
        return _with( thisComponent -> {
                    thisComponent.getColumnModel().getColumn(columnIndex).setCellEditor(editor);
                })
                ._this();
    }

    /**
     *  Use this to set a table model.
     *  The provided argument is a builder object whose build method will be called
     *  for you instead of having to call the build method on the builder object yourself.
     * @param dataModelBuilder The builder object which will be used to build and then set the table model.
     * @return This builder object.
     */
    public final UIForTable<T> withModel( Buildable<BasicTableModel> dataModelBuilder ) {
        Objects.requireNonNull(dataModelBuilder);
        return this.withModel(dataModelBuilder.build());
    }

    /**
     *  Exposes a fluent builder API for a table model. <br>
     *  Here an example demonstrating how this API
     *  is typically used as part of a UI declaration:
     *  <pre>{@code
     *  UI.table().withModel( m -> m
     *      .colName( col -> new String[]{"X", "Y", "Z"}[col] )
     *      .colCount( () -> 3 )
     *      .rowCount( () -> data.size() )
     *      .getsEntryAt( (r, c) -> data[r][c] )
     *      .updateOn(update)
     *  )
     *  }</pre>
     *  The builder API is exposed to the lambda function passed to this method.
     *  The actually {@link TableModel} is built internally and then set on the table.
     *
     * @param dataModelBuilder A lambda function which receives a builder API for a table model
     * @return This builder instance, to allow for further method chaining.
     */
    public final UIForTable<T> withModel(
        Function<BasicTableModel.Builder<Object>, BasicTableModel.Builder<Object>> dataModelBuilder
    ) {
        Objects.requireNonNull(dataModelBuilder);
        BasicTableModel.Builder<Object> builder = UI.tableModel();
        builder = dataModelBuilder.apply(builder);
        return this.withModel(builder.build());
    }

    /**
     *  Exposes a fluent builder API for a table model holding a specific type of entry {@link Object}s. <br>
     *  Here an example demonstrating how this API
     *  is typically used as part of a UI declaration:
     *  <pre>{@code
     *  UI.table().withModel(Double.class, m -> m
     *      .colName( col -> new String[]{"X", "Y", "Z"}[col] )
     *      .colCount( () -> 3 )
     *      .rowCount( () -> data.size() )
     *      .getsEntryAt( (r, c) -> data[r][c] )
     *      .updateOn(update)
     *  )
     *  }</pre>
     *  In this example, the table model is built for a table holding {@link Double} entries.
     *  So the data array is expected to be a two-dimensional array of {@link Double}s. <br>
     *  <br>
     *  Note that the builder API is exposed to the lambda function passed to this method.
     *  The actually {@link TableModel} is built internally and then set on the table.
     *
     * @param itemType The type of the table entry {@link Object}s.
     * @param dataModelBuilder A lambda function which receives a builder API for a table model
     * @return This builder instance, to allow for further method chaining.
     */
    public final <E> UIForTable<T> withModel(
        Class<E> itemType,
        Function<BasicTableModel.Builder<E>, BasicTableModel.Builder<E>> dataModelBuilder
    ) {
        Objects.requireNonNull(dataModelBuilder);
        BasicTableModel.Builder<E> builder = UI.tableModel(itemType);
        builder = dataModelBuilder.apply(builder);
        return this.withModel(builder.build());
    }
    /**
     * Use this to set a basic table model for this table.
     * @param model The model for the table model.
     * @return This builder object.
     */
    public final UIForTable<T> withModel( BasicTableModel model ) {
        NullUtil.nullArgCheck(model, "model", BasicTableModel.class);
        return _with( thisComponent -> {
                    thisComponent.setModel(model);
                })
                ._this();
    }

    /**
     *  Use this instead of {@link JTable#setModel(TableModel)} if your table data can be represented by
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
        if ( isRowMajor )
            return _with( thisComponent ->
                    thisComponent.setModel(new ListBasedTableModel<E>(isEditable, dataSource)
                    {
                        @Override public int getRowCount() { return getData().size(); }
                        @Override public int getColumnCount() {
                            List<List<E>> data = getData();
                            return ( data.isEmpty() ? 0 : data.get(0).size() );
                        }
                        @Override public @Nullable Object getValueAt(int rowIndex, int columnIndex) {
                            List<List<E>> data = getData();
                            if (isNotWithinBounds(rowIndex, columnIndex)) return null;
                            return data.get(rowIndex).get(columnIndex);
                        }
                        @Override public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                            List<List<E>> data = getData();
                            if ( !isEditable || isNotWithinBounds(rowIndex, columnIndex) ) return;
                            data.get(rowIndex).set(columnIndex, (E)aValue);
                        }
                    })
                )
                ._this();
        else // isColumnMajor
            return _with( thisComponent ->
                    thisComponent.setModel(new ListBasedTableModel<E>(isEditable, dataSource)
                    {
                        @Override public int getRowCount() {
                            List<List<E>> data = getData();
                            return (data.isEmpty() ? 0 : data.get(0).size());
                        }
                        @Override public int getColumnCount() { return getData().size(); }
                        @Override public @Nullable Object getValueAt( int rowIndex, int columnIndex ) {
                            List<List<E>> data = getData();
                            if ( isNotWithinBounds(rowIndex, columnIndex) ) return null;
                            return data.get(columnIndex).get(rowIndex);
                        }
                        @Override public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                            List<List<E>> data = getData();
                            if ( !isEditable || isNotWithinBounds(rowIndex, columnIndex) ) return;
                            data.get(columnIndex).set(rowIndex, (E)aValue);
                        }
                    })
                )
                ._this();
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
        return _with( thisComponent -> {
                    thisComponent.setModel(new MapBasedColumnMajorTableModel<>(mode.isEditable(), dataSource));
                })
                ._this();
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
        return _with( thisComponent -> {
                    event.subscribe(()->
                        _runInUI(()->{
                            TableModel model = thisComponent.getModel();
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
                })
                ._this();
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
        public @Nullable String getColumnName(int column) {
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
        public @Nullable Object getValueAt( int rowIndex, int columnIndex ) {
            if ( isNotWithinBounds(rowIndex, columnIndex) ) return null;
            List<E> column = getData().values().stream().skip(columnIndex).findFirst().orElse(null);
            if ( column == null ) return null;
            if ( rowIndex < 0 || rowIndex >= column.size() ) return null;
            return column.get(rowIndex);
        }
    }

}
