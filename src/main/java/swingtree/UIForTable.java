package swingtree;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import sprouts.Event;
import swingtree.api.Buildable;
import swingtree.api.Configurator;
import swingtree.api.model.BasicTableModel;
import swingtree.api.model.TableListDataSource;
import swingtree.api.model.TableMapDataSource;
import swingtree.style.ComponentExtension;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.List;
import java.util.function.Function;

/**
 *  A SwingTree declarative builder designed for configuring {@link JTable} instances allowing
 *  for a fluent API to build tables in a declarative way.
 *
 * @param <T> The type of {@link JTable} being built by this builder.
 */
public final class UIForTable<T extends JTable> extends UIForAnySwing<UIForTable<T>, T>
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(UIForTable.class);

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

    private static <T extends JTable> CellBuilder<T, Object> _renderTable() {
        return (CellBuilder) CellBuilder.forTable(Object.class);
    }

    /**
     *  Use this to build a table cell renderer for a particular column.
     *  The second argument accepts a lambda function which exposes the builder API for a cell renderer.
     *  Here is an example of how to use this method:
     * <pre>{@code
     *     UI.table(myModel)
     *     .withCellsForColumn("column1", it -> it
     *         .when(String.class)
     *         .asText( cell -> "[" + cell.valueAsString().orElse("") + "]" ) )
     *     )
     *     .withCellsForColumn("column2", it -> it
     *         .when(Float.class)
     *         .asText( cell -> "(" + cell.valueAsString().orElse("") + "f)" ) )
     *         .when(Double.class)
     *         .asText( cell -> "(" + cell.valueAsString().orElse("") + "d)" ) )
     *     );
     * }</pre>
     * The above example would render the first column of the table as a string surrounded by square brackets,
     * and the second column as a float or double value surrounded by parentheses.
     * Note that the API allows you to specify how specific types of table entry values
     * should be rendered. This is done by calling the {@link CellBuilder#when(Class)} method
     * before calling the {@link RenderAs#asText(Function)} method.
     * <br>
     * <b>
     *     Due to this method being inherently based on the expectation of type ambiguity it is
     *     a rather verbose way of defining how your cells should look and behave. The simpler and
     *     preferred way of defining cell views is through the {@link #withCell(Configurator)},
     *     {@link #withCellForColumn(String, Configurator)} and {@link #withCellForColumn(int, Configurator)}
     *     methods.
     * </b>
     *
     * @param columnName The name of the column for which the cell renderer will be built.
     * @param renderBuilder A lambda function which exposes a fluent builder API for a cell renderer
     *                      and returns the builder API for a cell renderer.
     *                      Call the appropriate methods on the builder API to configure the cell renderer.
     * @return This builder node.
     */
    public final UIForTable<T> withCellsForColumn(
        String columnName,
        Configurator<CellBuilder<T, Object>> renderBuilder
    ) {
        NullUtil.nullArgCheck(renderBuilder, "renderBuilder", CellBuilder.class);
        CellBuilder<T, Object> builder = _renderTable();
        try {
            builder = renderBuilder.configure(builder);
        } catch (Exception e) {
            log.error(SwingTree.get().logMarker(), "Error while building table renderer.", e);
            return this;
        }
        CellBuilder<T, Object> finalBuilder = builder;
        return _with(thisComponent -> {
                        CellBuilder.SimpleTableCellRenderer renderer = finalBuilder.getForTable(thisComponent.getColumn(columnName).getCellRenderer());
                        thisComponent.getColumn(columnName).setCellRenderer(renderer);
                        thisComponent.getColumn(columnName).setCellEditor(renderer);
                    })
                    ._this();
    }

    /**
     *  Use this to build a basic table cell renderer for a particular column.
     *  The second argument passed to this method is a lambda function
     *  which accepts a {@link CellConf} representing the cell to be rendered.
     *  You may then return an updated cell with a desired view component
     *  through methods like {@link CellConf#view(Component)} or {@link CellConf#updateView(Configurator)}.
     *  Here an example of how this method may be used:
     * <pre>{@code
     *     UI.table(UI.ListData.ROW_MAJOR_EDITABLE, ()->List.of(List.of(1, 2, 3), List.of(7, 8, 9)) )
     *     .withCellForColumn(0, cell -> cell
     *          .updateView( comp -> comp
     *              .orGet(JLabel::new) // initialize a new JLabel if not already present
     *              .updateIf(JLabel.class, l -> {
     *                  l.setText(cell.valueAsString().orElse(""));
     *                  l.setBackground(cell.isSelected() ? Color.YELLOW : Color.WHITE);
     *                  return l;
     *              })
     *              //...
     *          )
     *     )
     *     .withCellForColumn(1, cell -> cell
     *          .updateView( comp -> comp
     *              //...
     *          )
     *     );
     * }</pre>
     * Also see {@link #withCellForColumn(int, Configurator)} method to build a cell renderer for a column by index,
     * and {@link #withCell(Configurator)} method to build a cell renderer for all columns of the table.
     * <br>
     * This API also supports the configuration of cell editors as the supplied lambda will also be
     * called by an underlying {@link TableCellEditor} implementation when the cell is in editing mode.
     * The cell will indicate that it needs an editor component by having the {@link CellConf#isEditing()}
     * set to true. You can then decide to return a different view component for the cell editor
     * by checking this property. The next time the lambda is invoked with the {@link CellConf#isEditing()}
     * flag is set to true, then the cell will still contain the same editor component as previously specified.
     * In case of the flag being false, the cell will contain the view component
     * that was provided the last time the cell was not in editing mode.
     *
     *
     * @param columnName The name of the column for which the cell renderer will be built.
     * @param cellConfigurator A lambda function which configures the cell view.
     * @return This builder node.
     */
    public final UIForTable<T> withCellForColumn(
        String columnName,
        Configurator<CellConf<T, Object>> cellConfigurator
    ) {
        Objects.requireNonNull(cellConfigurator);
        Objects.requireNonNull(columnName);
        return withCellsForColumn(columnName, it -> it.when((Class)Object.class).as(cellConfigurator));
    }

    /**
     *  Use this to build a table cell renderer for a particular column.
     *  The second argument accepts a lambda function which exposes the builder API for a cell renderer.
     *  Here an example of how this method may be used:
     * <pre>{@code
     *     UI.table(myModel)
     *     .withCellForColumn(0, it -> it
     *         .when(String.class)
     *         .asText( cell -> "[" + cell.valueAsString().orElse("") + "]" ) )
     *     )
     *     .withCellForColumn(1, it -> it
     *         .when(Float.class)
     *         .asText( cell -> "(" + cell.valueAsString().orElse("") + "f)" ) )
     *         .when(Double.class)
     *         .asText( cell -> "(" + cell.valueAsString().orElse("") + "d)" ) )
     *     );
     * }</pre>
     * The above example would render the first column of the table as a string surrounded by square brackets,
     * and the second column as a float or double value surrounded by parentheses.
     * Note that the API allows you to specify how specific types of table entry values
     * should be rendered. This is done by calling the {@link CellBuilder#when(Class)} method
     * before calling the {@link RenderAs#asText(Function)} method. <br>
     * <br>
     * <b>
     *      Due to this method being inherently based on the expectation of type ambiguity it is
     *      a rather verbose way of defining how your cells should look and behave. The simpler and
     *      preferred way of defining cell views is through the {@link #withCell(Configurator)},
     *      {@link #withCellForColumn(String, Configurator)} and {@link #withCellForColumn(int, Configurator)}
     *      methods.
     * </b>
     *
     *
     * @param columnIndex The index of the column for which the cell renderer will be built.
     * @param renderBuilder A lambda function which exposes a fluent builder API for a cell renderer
     *                      and returns the builder API for a cell renderer.
     *                      Call the appropriate methods on the builder API to configure the cell renderer.
     * @return This builder node.
     */
    public final UIForTable<T> withCellsForColumn(
        int columnIndex,
        Configurator<CellBuilder<T, Object>> renderBuilder
    ) {
        NullUtil.nullArgCheck(renderBuilder, "renderBuilder", CellBuilder.class);
        CellBuilder<T, Object> builder = _renderTable();
        try {
            builder = renderBuilder.configure(builder);
        } catch (Exception e) {
            log.error(SwingTree.get().logMarker(), "Error while building table renderer.", e);
            return this;
        }
        CellBuilder<T, Object> finalBuilder = builder;
        return _with( thisComponent -> {
            CellBuilder.SimpleTableCellRenderer renderer = finalBuilder.getForTable(thisComponent.getColumnModel().getColumn(columnIndex).getCellRenderer());
                    thisComponent.getColumnModel().getColumn(columnIndex).setCellRenderer(renderer);
                    thisComponent.getColumnModel().getColumn(columnIndex).setCellEditor(renderer);
                })
                ._this();
    }

    /**
     *  Use this to build a basic table cell view for a particular column.
     *  The second argument passed to this method is a lambda function
     *  which accepts a {@link CellConf} representing the cell to be rendered and possibly even edited.
     *  You may then return an updated cell with a desired view component
     *  through methods like {@link CellConf#view(Component)} or {@link CellConf#updateView(Configurator)}.
     *  Here an example of how this method may be used:
     * <pre>{@code
     *     UI.table(UI.ListData.ROW_MAJOR_EDITABLE, ()->List.of(List.of(1, 2, 3), List.of(7, 8, 9)) )
     *     .withCellForColumn(0, cell -> cell
     *          .updateView( comp -> comp
     *              .orGet(JLabel::new) // initialize a new JLabel if not already present
     *              .updateIf(JLabel.class, l -> {
     *                  l.setText(cell.valueAsString().orElse(""));
     *                  l.setBackground(cell.isSelected() ? Color.YELLOW : Color.WHITE);
     *                  return l;
     *              })
     *              //...
     *          )
     *     )
     *     .withCellForColumn(1, cell -> cell
     *          .updateView( comp -> comp
     *              //...
     *          )
     *     );
     * }</pre>
     * Also see {@link #withCellForColumn(String, Configurator)} method to build a cell renderer for a column by name,
     * and {@link #withCell(Configurator)} method to build a cell renderer for all columns of the table.
     * <br>
     * This API also supports the configuration of cell editors as the supplied lambda will also be
     * called by an underlying {@link TableCellEditor} implementation when the cell is in editing mode.
     * The cell will indicate that it needs an editor component by having the {@link CellConf#isEditing()}
     * set to true. You can then decide to return a different view component for the cell editor
     * by checking this property. The next time the lambda is invoked with the {@link CellConf#isEditing()}
     * flag is set to true, then the cell will still contain the same editor component as previously specified.
     * In case of the flag being false, the cell will contain the view component
     * that was provided the last time the cell was not in editing mode.
     *
     * @param columnIndex The index of the column for which the cell renderer will be built.
     * @param cellConfigurator A lambda function which configures the cell view.
     *                         The lambda is invoked in two main situations: when the cell is in editing mode
     *                         and when the cell is not in editing mode (only rendering).
     *                         You may decide what to store in the cell based on its state.
     * @return This instance of the builder, to allow for declarative method chaining.
     */
    public final UIForTable<T> withCellForColumn(
        int columnIndex,
        Configurator<CellConf<T, Object>> cellConfigurator
    ) {
        Objects.requireNonNull(cellConfigurator);
        return withCellsForColumn(columnIndex, it -> it.when((Class)Object.class).as(cellConfigurator));
    }

    /**
     * Use this to register a table cell renderer for a particular column.
     * A {@link TableCellRenderer} is a supplier of {@link java.awt.Component} instances which are used to render
     * the cells of a table.
     * <b>Note that in SwingTree, the preferred way of defining a cell renderer for a particular column is through the
     * {@link #withCellForColumn(String, Configurator)} method, which allows for a more fluent and declarative
     * way of defining cell renderers as well as editors.</b>
     *
     * @param columnName The name of the column for which the cell renderer will be registered.
     * @param renderer The cell renderer to be registered.
     * @return This builder node, to allow for builder-style method chaining.
     */
    public final UIForTable<T> withCellRendererForColumn( String columnName, TableCellRenderer renderer ) {
        NullUtil.nullArgCheck(columnName, "columnName", String.class);
        NullUtil.nullArgCheck(renderer, "renderer", TableCellRenderer.class);
        return _with( thisComponent -> {
                    thisComponent.getColumn(columnName).setCellRenderer(renderer);
                    if ( renderer instanceof TableCellEditor )
                        thisComponent.getColumn(columnName).setCellEditor((TableCellEditor)renderer);
                })
                ._this();
    }

    /**
     * Use this to register a table cell renderer for a particular column. <br>
     * A {@link TableCellRenderer} is a supplier of {@link java.awt.Component} instances which are used to render
     * the cells of a table.
     * <b>Note that in SwingTree, the preferred way of defining a cell renderer for a particular column is through the
     * {@link #withCellForColumn(int, Configurator)} method, which allows for a more fluent and declarative
     * way of defining cell renderers. It also supports both cell rendering and editing.</b>
     *
     * @param columnIndex The index of the column for which the cell renderer will be registered.
     * @param renderer The cell renderer to be registered.
     * @return This builder instance, to allow for method chaining.
     */
    public final UIForTable<T> withCellRendererForColumn( int columnIndex, TableCellRenderer renderer ) {
        NullUtil.nullArgCheck(renderer, "renderer", TableCellRenderer.class);
        return _with( thisComponent -> {
                    thisComponent.getColumnModel().getColumn(columnIndex).setCellRenderer(renderer);
                })
                ._this();
    }

    /**
     *  Use this to register a {@link TableCellRenderer} for all columns of this table.<br>
     *  A {@link TableCellRenderer} is a supplier of {@link java.awt.Component} instances which are used to render
     *  the cells of a table.<br><br>
     *  <b>Note that in SwingTree, the preferred way of defining a cell renderer is through the
     *  {@link #withCell(Configurator)} method, which allows for a more fluent and declarative
     *  way of defining cell renderers and also supports both cell rendering and editing.</b>
     *
     * @param renderer A provider of {@link java.awt.Component} instances which are used to render the cells of a table.
     * @return This builder instance, to allow for method chaining.
     */
    public final UIForTable<T> withCellRenderer( TableCellRenderer renderer ) {
        NullUtil.nullArgCheck(renderer, "renderer", TableCellRenderer.class);
        return _with( thisComponent -> {
                    thisComponent.setDefaultRenderer(Object.class, renderer);
                })
                ._this();
    }

    /**
     *  Use this to define a table cell renderer for all columns of this table
     *  using the fluent builder API exposed to the provided lambda function.<br>
     *  Here is an example of how this method is used:
     *  <pre>{@code
     *    UI.table()
     *    .withCells( it -> it
     *        .when(SomeDataType.class)
     *        .asText( cell -> cell.value().get().toString() )
     *    )
     *    // ...
     *  }</pre>
     *  You may want to know that a similar API is also available for the {@link javax.swing.JList}
     *  and {@link javax.swing.JComboBox} components, see {@link UIForList#withCells(Configurator)},
     *  {@link UIForCombo#withCells(Configurator)} for more information.
     *  <p>
     *  <b>
     *      Also see {@link #withCell(Configurator)} method, which constitutes the preferred way
     *      to build a list cell renderer as it is simpler, more concise and less error-prone.
     *  </b>
     *
     * @param renderBuilder A lambda function which exposes the builder API for a cell renderer
     *                      and returns the builder API for a cell renderer.
     *                      Call the appropriate methods on the builder API to configure the cell renderer.
     * @return This builder node.
     */
    public final UIForTable<T> withCells(
        Configurator<CellBuilder<T, Object>> renderBuilder
    ) {
        NullUtil.nullArgCheck(renderBuilder, "renderBuilder", CellBuilder.class);
        CellBuilder<T, Object> builder = _renderTable();
        try {
            builder = renderBuilder.configure(builder);
        } catch (Exception e) {
            log.error(SwingTree.get().logMarker(), "Error while building table renderer.", e);
            return this;
        }
        Objects.requireNonNull(builder);
        CellBuilder<T, Object> finalBuilder = builder;
        return _with(thisComponent -> {
                    CellBuilder.SimpleTableCellRenderer renderer = finalBuilder.getForTable(thisComponent.getDefaultRenderer(Object.class));
                    thisComponent.setDefaultRenderer(Object.class, renderer);
                    thisComponent.setDefaultEditor(Object.class, renderer);
                })
                ._this();
    }

    /**
     *  Allows for the configuration of a cell view for the items of the {@link JTable} instance.
     *  The {@link Configurator} lambda function passed to this method receives a {@link CellConf}
     *  exposing a wide range of properties describing the state of the cell, like
     *  its current item, its index, its selection state, etc.
     *  You may update return an updated cell with a desired view component
     *  through methods like {@link CellConf#view(Component)} or {@link CellConf#updateView(Configurator)}.
     *  <p>
     *  Here code snippet demonstrating how this method may be used
     *  as part of a UI declaration:
     *  <pre>{@code
     *      UI.table(UI.MapData.EDITABLE,()->{
     *          Map<String, List<String>> data = new LinkedHashMap<>();
     *          data.put("A", List.of("A1", "A2", "A3"));
     *          data.put("B", List.of("B1", "B2", "B3"));
     *          data.put("C", List.of("C1", "C2", "C3"));
     *          return data;
     *      })
     *      .withCell( cell -> cell
     *          .updateView( comp -> comp
     *              .orGet(JLabel::new) // initialize a new JLabel if not already present
     *              .updateIf(JLabel.class, tf -> {
     *                  tf.setText(cell.valueAsString().orElse(""));
     *                  tf.setBackground(cell.isSelected() ? Color.YELLOW : Color.WHITE);
     *                  return tf;
     *              })
     *          )
     *      )
     *  }</pre>
     *  In this example, a new {@link JTable} is created from a map of column names to lists of strings.
     *  The {@link Configurator} lambda function passed to this method configures the cell view
     *  by setting the text of a {@link JLabel} to the value of the cell, and setting the background
     *  color of the label to yellow if the cell is selected, and white otherwise.
     *  <br>
     *  This API also supports the configuration of cell editors as the supplied lambda will also be
     *  called by an underlying {@link TableCellEditor} implementation when the cell is in editing mode.
     *  The cell will indicate that it needs an editor component by having the {@link CellConf#isEditing()}
     *  set to true. You can then decide to return a different view component for the cell editor
     *  by checking this property. The next time the lambda is invoked with the {@link CellConf#isEditing()}
     *  flag is set to true, then the cell will still contain the same editor component as previously specified.
     *  In case of the flag being false, the cell will contain the view component
     *  that was provided the last time the cell was not in editing mode.
     *
     *
     * @param cellConfigurator The {@link Configurator} lambda function that configures the cell view.
     * @return This instance of the builder node to allow for fluent method chaining.
     * @param <V> The type of the value that is being rendered in this combo box.
     */
    public final <V> UIForTable<T> withCell(
            Configurator<CellConf<T, V>> cellConfigurator
    ) {
        return withCells( it -> it.when((Class)Object.class).as(cellConfigurator) );
    }

    /**
     * Use this to register a table cell editor for a particular column.
     * <b>Note that in SwingTree, the preferred way of defining a cell editor for a particular column is through the
     * {@link #withCellForColumn(String, Configurator)} method, which allows for a more fluent and declarative
     * way of defining cell editors.</b>
     *
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
     * <b>Note that in SwingTree, the preferred way of defining a cell editor for a particular column is through the
     * {@link #withCellForColumn(int, Configurator)} method, which allows for a more fluent and declarative
     * way of defining cell editors.</b>
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
     *  <b>
     *      The preferred way of setting a table model is through the {@link #withModel(Configurator)}
     *      which exposes a fluent builder API for binding the table model to a data source
     *      without any boilerplate code.
     *  </b>
     * @param dataModelBuilder The builder object which will be used to build and then set the table model.
     * @return This builder object.
     */
    public final UIForTable<T> withModel( Buildable<BasicTableModel> dataModelBuilder ) {
        Objects.requireNonNull(dataModelBuilder);
        try {
            return this.withModel(dataModelBuilder.build());
        } catch (Exception e) {
            log.error(SwingTree.get().logMarker(), "Error while building a table model.", e);
            return this;
        }
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
        Configurator<BasicTableModel.Builder<Object>> dataModelBuilder
    ) {
        Objects.requireNonNull(dataModelBuilder);
        BasicTableModel.Builder<Object> builder = new BasicTableModel.Builder<>(Object.class);
        try {
            builder = dataModelBuilder.configure(builder);
        } catch (Exception e) {
            log.error(SwingTree.get().logMarker(), "Error while building table model.", e);
        }
        return this.withModel(builder.build());
    }

    /**
     *  Exposes a fluent builder API for a table model holding a specific type of entry. <br>
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
     *  In this example, the table model is built for a {@link Double} based data source.
     *  So here the data array is a two-dimensional array of {@link Double}s. <br>
     *  <br>
     *  Note that the builder API is exposed to the lambda function passed to this method.
     *  The actual {@link TableModel} is built internally and then installed on the table component.
     *  <p>
     *  You can also use the {@link UI#table(Configurator)} factory method to directly create a table
     *  with a custom table model. <br>
     *
     * @param <E> The type of the table entry {@link Object}s.
     * @param itemType The type of the table entry {@link Object}s.
     * @param dataModelBuilder A lambda function which receives a builder API for a table model
     * @return This builder instance, to allow for further method chaining.
     */
    public final <E> UIForTable<T> withModel(
        Class<E> itemType,
        Configurator<BasicTableModel.Builder<E>> dataModelBuilder
    ) {
        Objects.requireNonNull(itemType);
        Objects.requireNonNull(dataModelBuilder);
        BasicTableModel.Builder<E> builder = new BasicTableModel.Builder<>(itemType);
        try {
            builder = dataModelBuilder.configure(builder);
        } catch (Exception e) {
            log.error(SwingTree.get().logMarker(), "Error while building table model.", e);
        }
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
                    WeakReference<T> thisComponentRef = new WeakReference<>(thisComponent);
                    ComponentExtension.from(thisComponent).storeBoundObservable(
                        event.observable().subscribe(()->
                            _runInUI(()->{
                                T innerComponent = thisComponentRef.get();
                                if (innerComponent == null)
                                    return;
                                TableModel model = innerComponent.getModel();
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
                        )
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
            if ( isNotWithinBounds(rowIndex, columnIndex) )
                return null;
            List<E> column = getData().values().stream().skip(columnIndex).findFirst().orElse(null);
            if ( column == null )
                return null;
            if ( rowIndex < 0 || rowIndex >= column.size() )
                return null;
            return column.get(rowIndex);
        }

        @Override
        public void setValueAt( Object aValue, int rowIndex, int columnIndex ) {
            if ( isNotWithinBounds(rowIndex, columnIndex) )
                return;
            List<E> column = getData().values().stream().skip(columnIndex).findFirst().orElse(null);
            if ( column == null )
                return;
            if ( rowIndex < 0 || rowIndex >= column.size() )
                return;
            try {
                column.set(rowIndex, (E) aValue);
            } catch (Exception e) {
                log.warn(SwingTree.get().logMarker(), "Failed to set value in hash table based table model.", e);
            }
        }

    }

}
