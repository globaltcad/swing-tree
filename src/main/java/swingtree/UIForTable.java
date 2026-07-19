package swingtree;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import sprouts.Event;
import sprouts.Lens;
import sprouts.Observable;
import sprouts.Tuple;
import sprouts.Val;
import sprouts.Vals;
import sprouts.Var;
import swingtree.api.Buildable;
import swingtree.api.Configurator;
import swingtree.api.model.BasicTableModel;
import swingtree.api.model.TableListDataSource;
import swingtree.api.model.TableMapDataSource;
import swingtree.api.model.TableData;
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
     * <p>
     * Note that the supplied model merely describes where the table data lives.
     * SwingTree wraps it in a thread safe model of its own before handing it to the
     * {@link JTable}, so that under the {@link swingtree.threading.EventProcessor#DECOUPLED}
     * protocol the AWT Event Dispatch Thread never reads your application thread owned
     * state while it paints. Consequently {@link JTable#getModel()} does not return the
     * object supplied here.
     *
     * @param model The model for the table model.
     * @return This builder object.
     */
    public final UIForTable<T> withModel( BasicTableModel model ) {
        NullUtil.nullArgCheck(model, "model", BasicTableModel.class);
        return _with( thisComponent -> {
                    _installModel(thisComponent, new BasicTableModelAdapter(model));
                })
                ._this();
    }

    /**
     *  Binds the table to a read only property holding an immutable
     *  {@link TableData} value, which is the most complete way of describing
     *  the contents of a table: it carries the cells, the column names, the column
     *  classes and the {@link UI.ListData} layout of the data, all in a single value.
     *  <p>
     *  Because a {@link TableData} is deeply immutable, the table works with the
     *  property value itself as its UI thread owned snapshot (so the AWT Event Dispatch
     *  Thread never reads application thread owned mutable state), and no copying is
     *  needed to hand it between the threads. The table updates itself automatically
     *  whenever the property changes, so no {@code updateTableOn(..)} binding is needed.
     *  For a row major snapshot, the change diff carried by the cells is furthermore
     *  used to sync row insertions, removals and updates to the {@link JTable}
     *  incrementally, instead of rebuilding the whole table.
     *  <p>
     *  The cells of a table bound like this are always read only. Use
     *  {@link #withModel(Var)} together with one of the {@code *_EDITABLE} layouts
     *  if you want the user to be able to edit them.
     *  <pre>{@code
     *  Val<TableData> model = vm.tableModel();
     *  UI.table().withModel(model);
     *  }</pre>
     *
     * @param model A read only property holding the {@link TableData} describing this table.
     * @return This builder node, to allow for method chaining.
     */
    public final UIForTable<T> withModel( Val<TableData> model ) {
        NullUtil.nullArgCheck(model, "model", Val.class);
        // A read only 'Val' overload always yields a read only table, even if the
        // reference happens to point at a 'Var' at runtime (a view model exposing its
        // 'Var' as a 'Val', say). The overload the user picked is what expresses intent.
        return _withTableData(model, false);
    }

    /**
     *  Binds the table to a mutable property holding an immutable {@link TableData}
     *  value, which, in addition to what {@link #withModel(Val)} does, allows the user
     *  to edit the cells of the table: an edit is applied to the UI thread owned snapshot
     *  right away (so that the table does not flicker) and then written back into this
     *  property on the application thread.
     *  <p>
     *  Note that the cells only actually become editable if the {@link TableData#layout()}
     *  of the bound value is one of the {@code *_EDITABLE} constants of {@link UI.ListData}.
     *  This mirrors {@link #withModel(UI.ListData, Val)}, where the very same two conditions
     *  (an editable layout and a mutable property) decide the matter.
     *  <pre>{@code
     *  Var<TableData> model = vm.tableModel();
     *  UI.table().withModel(model);
     *  }</pre>
     *
     * @param model A mutable property holding the {@link TableData} describing this table.
     * @return This builder node, to allow for method chaining.
     */
    public final UIForTable<T> withModel( Var<TableData> model ) {
        NullUtil.nullArgCheck(model, "model", Var.class);
        // A mutable 'Var' overload yields an editable table (if the layout allows it),
        // so unlike the 'Val' overload it must not funnel through as read only.
        return _withTableData(model, true);
    }

    /**
     *  The single install path behind every {@link TableData} property based binding.
     *  The {@code editable} flag carries the user's intent (which {@code withModel}
     *  overload they picked) down to the {@link PropertyTableModel}, instead of letting
     *  the model guess it from the runtime type of the property, which would wrongly
     *  make a table editable when a {@link Var} is bound through a read only {@link Val}.
     */
    private UIForTable<T> _withTableData( Val<TableData> model, boolean editable ) {
        return _with( thisComponent -> {
                    _installModel(thisComponent, new PropertyTableModel(model, editable));
                })
                ._this();
    }

    /**
     *  Binds the table to a {@link Tuple} based, fully thread safe and reactive
     *  data source, whose layout is described by the supplied {@link UI.ListData}
     *  constant: for a {@code ROW_MAJOR*} layout the outer {@link Tuple} holds the
     *  rows and each inner {@link Tuple} the cells of a row, whereas for a
     *  {@code COLUMN_MAJOR*} layout the outer {@link Tuple} holds the columns and
     *  each inner {@link Tuple} the cells of a column.
     *  <p>
     *  This is the recommended way to model dynamic, application thread owned
     *  table data: because a {@link Tuple} is deeply immutable, the table works
     *  with a UI thread owned snapshot of it (so the AWT Event Dispatch Thread
     *  never reads application thread owned mutable state). The table updates
     *  itself automatically whenever the property changes, so no
     *  {@code updateTableOn(..)} binding is needed. For a row major layout, the
     *  change diff carried by the tuple is furthermore used to sync row
     *  insertions, removals and updates to the {@link JTable} incrementally,
     *  instead of rebuilding the whole table.
     *  <p>
     *  If you pass one of the {@code *_EDITABLE} constants <i>and</i> a mutable
     *  {@link Var}, then the user may edit the cells of the table, in which case
     *  the edits are written back into the property on the application thread.
     *  A read only {@link Val} always yields a read only table.
     *  <pre>{@code
     *  var rows = Var.of(Tuple.of(
     *      Tuple.of("Alice", "30"),
     *      Tuple.of("Bob",   "42")
     *  ));
     *  UI.table().withModel(UI.ListData.ROW_MAJOR_EDITABLE, rows);
     *  }</pre>
     *
     * @param dataFormat The layout of the supplied cells, see {@link UI.ListData}.
     * @param cells A property holding a {@link Tuple} of {@link Tuple}s of cell values.
     * @return This builder node, to allow for method chaining.
     * @param <E> The common type of the cell values.
     */
    public final <E> UIForTable<T> withModel( UI.ListData dataFormat, Val<Tuple<Tuple<E>>> cells ) {
        NullUtil.nullArgCheck(dataFormat, "dataFormat", UI.ListData.class);
        NullUtil.nullArgCheck(cells, "cells", Val.class);
        // A tuple based table has only this one overload, so its editability is
        // driven by the runtime mutability of the cells property (which decides
        // whether '_asSnapshotProperty' zooms into a mutable snapshot or merely
        // views a read only one), matching what that method does internally.
        boolean editable = cells instanceof Var && cells.isMutable();
        return _withTableData(_asSnapshotProperty(dataFormat, cells), editable);
    }

    /**
     *  Translates a {@link Tuple} based cells property into the {@link TableData}
     *  property which the table models actually speak, so that a tuple based table is
     *  simply a snapshot based table with a differently shaped source.
     *  <p>
     *  Note that this translation costs nothing: a {@link TableData} keeps the very
     *  tuple it is handed, so the change diff of that tuple (which the model uses to
     *  fire targeted row events) survives the trip.
     *  Mutable cells are zoomed into through a {@link Lens} so that a user edit finds
     *  its way back into the original property, whereas read only cells only need a view.
     */
    @SuppressWarnings("unchecked")
    private static <E> Val<TableData> _asSnapshotProperty( UI.ListData dataFormat, Val<Tuple<Tuple<E>>> cells ) {
        Function<Tuple<Tuple<E>>, TableData> toSnapshot = tuple ->
                tuple == null
                    // A nullable property may hold no tuple at all (data not loaded
                    // yet, say), which simply reads as an empty table.
                    ? TableData.empty().withLayout(dataFormat)
                    : TableData.of(dataFormat, (Tuple<Tuple<@Nullable Object>>)(Tuple<?>) tuple);
        /*
            Careful: a read only property may well be a 'Var' instance, which is why
            its mutability has to be checked explicitly (a lens onto an immutable
            property would throw as soon as the user edits a cell).
         */
        if ( cells instanceof Var && cells.isMutable() )
            // The null object flavour of 'zoomTo' is what makes a lens over a
            // nullable property well defined: a null tuple reads as the empty table.
            return ((Var<Tuple<Tuple<E>>>) cells).zoomTo(
                        TableData.empty().withLayout(dataFormat),
                        Lens.of(
                            toSnapshot,
                            (oldCells, snapshot) -> (Tuple<Tuple<E>>)(Tuple<?>) snapshot.cells()
                        )
                    );
        return cells.viewAs(TableData.class, toSnapshot);
    }

    /**
     *  Installs a table model on the given table, first handing our thread safe
     *  {@link AbstractSnapshotTableModel}s the current {@link swingtree.threading.EventProcessor}
     *  (which they need to decide whether to snapshot across threads or read live)
     *  before the {@link JTable} starts querying the model.
     */
    private void _installModel( T thisComponent, TableModel model ) {
        if ( model instanceof AbstractSnapshotTableModel )
            ((AbstractSnapshotTableModel) model)._setEventProcessor(_state().eventProcessor());
        thisComponent.setModel(model);
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
     *  <p>
     *  <b>Consider {@link #withModel(Var)} with a {@link TableData} value instead:</b> it
     *  describes the whole table (cells, column names, column classes and layout) as a
     *  single immutable value which a property hands to the table, so the table updates
     *  itself, it is thread safe by construction, and it syncs row changes incrementally
     *  instead of rebuilding. The data source here can only ever refresh everything.
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
                    _installModel(thisComponent, new ListBasedTableModel<E>(isEditable, dataSource)
                    {
                        @Override protected int _liveRowCount() { return getData().size(); }
                        @Override protected int _liveColumnCount() {
                            List<List<E>> data = getData();
                            return ( data.isEmpty() ? 0 : data.get(0).size() );
                        }
                        @Override protected @Nullable Object _liveValueAt(int rowIndex, int columnIndex) {
                            List<List<E>> data = getData();
                            // Bound-check against the live data, never the (possibly stale)
                            // snapshot: this method feeds 'takeLiveSnapshot', which runs while
                            // the previous snapshot is still installed for the UI thread.
                            if ( rowIndex < 0 || rowIndex >= data.size() ) return null;
                            List<E> row = data.get(rowIndex);
                            if ( columnIndex < 0 || columnIndex >= row.size() ) return null;
                            return row.get(columnIndex);
                        }
                        @Override protected void _liveSetValueAt(@Nullable Object aValue, int rowIndex, int columnIndex) {
                            if ( !isEditable ) return;
                            List<List<E>> data = getData();
                            if ( rowIndex < 0 || rowIndex >= data.size() ) return;
                            List<E> row = data.get(rowIndex);
                            if ( columnIndex < 0 || columnIndex >= row.size() ) return;
                            row.set(columnIndex, (E)aValue);
                        }
                    })
                )
                ._this();
        else // isColumnMajor
            return _with( thisComponent ->
                    _installModel(thisComponent, new ListBasedTableModel<E>(isEditable, dataSource)
                    {
                        @Override protected int _liveRowCount() {
                            List<List<E>> data = getData();
                            return (data.isEmpty() ? 0 : data.get(0).size());
                        }
                        @Override protected int _liveColumnCount() { return getData().size(); }
                        @Override protected @Nullable Object _liveValueAt( int rowIndex, int columnIndex ) {
                            List<List<E>> data = getData();
                            // Bound-check against the live data, never the (possibly stale)
                            // snapshot: this method feeds 'takeLiveSnapshot', which runs while
                            // the previous snapshot is still installed for the UI thread.
                            if ( columnIndex < 0 || columnIndex >= data.size() ) return null;
                            List<E> column = data.get(columnIndex);
                            if ( rowIndex < 0 || rowIndex >= column.size() ) return null;
                            return column.get(rowIndex);
                        }
                        @Override protected void _liveSetValueAt(@Nullable Object aValue, int rowIndex, int columnIndex) {
                            if ( !isEditable ) return;
                            List<List<E>> data = getData();
                            if ( columnIndex < 0 || columnIndex >= data.size() ) return;
                            List<E> column = data.get(columnIndex);
                            if ( rowIndex < 0 || rowIndex >= column.size() ) return;
                            column.set(rowIndex, (E)aValue);
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
     *  <p>
     *  <b>Consider {@link #withModel(Var)} with a {@link TableData} value instead:</b> it
     *  describes the whole table (cells, column names, column classes and layout) as a
     *  single immutable value which a property hands to the table, so the table updates
     *  itself, it is thread safe by construction, and it syncs row changes incrementally
     *  instead of rebuilding. The data source here can only ever refresh everything.
     *
     * @param mode An enum which configures the modifiability of the table in a readable fashion.
     * @param dataSource The {@link TableMapDataSource} returning a column major map based matrix which will be used to populate the table.
     * @return This builder node.
     * @param <E> The type of the table entry {@link Object}s.
     */
    public final <E> UIForTable<T> withModel( UI.MapData mode, TableMapDataSource<E> dataSource ) {
        return _with( thisComponent -> {
                    _installModel(thisComponent, new MapBasedColumnMajorTableModel<>(mode.isEditable(), dataSource));
                })
                ._this();
    }

    /**
     *  Use this to bind an {@link sprouts.Event} to the {@link TableModel} of this table
     *  which will trigger the {@link AbstractTableModel#fireTableDataChanged()} method when
     *  the {@link Event} triggers a change event through {@link Event#fire()}.
     *  This is useful when you want to update the table after the data source changes.
     *
     * @param event The event to be bound.
     * @return This builder node, for chaining.
     * @see #updateTableOn(Observable) To access a more general API that can update the table from any
     *                                 kind of reative source like a {@link Var#view()} or {@link Vals#view()}...
     */
    public final UIForTable<T> updateTableOn( Event event ) {
        Objects.requireNonNull(event);
        return this.updateTableOn(event.observable());
    }

    /**
     *  Use this to bind an {@link sprouts.Observable} to the {@link TableModel} of this table
     *  which will trigger the {@link AbstractTableModel#fireTableDataChanged()} method when the source
     *  of the observable changes in some way.
     *  This is useful if you want to update the table when the data source changes.
     *  You may derive the observable from a {@link sprouts.Var} property or {@link Event}.
     *
     * @param observable The observable to be bound.
     * @return This builder node, for chaining.
     * @see #updateTableOn(Event) For a convenience method specifically for the {@link Event} type.
     */
    public final UIForTable<T> updateTableOn( Observable observable ) {
        Objects.requireNonNull(observable);
        return _with( thisComponent -> {
                    WeakReference<T> thisComponentRef = new WeakReference<>(thisComponent);
                    ComponentExtension.from(thisComponent).storeBoundObservable(
                        observable.subscribe(()-> {
                            T innerComponent = thisComponentRef.get();
                            if (innerComponent == null)
                                return;
                            TableModel model = innerComponent.getModel();
                            if ( model instanceof AbstractSnapshotTableModel ) {
                                // Our own thread safe models own the threading of the refresh:
                                // under the decoupled protocol they snapshot on this (application)
                                // thread and then publish the swap to the UI thread, whereas under
                                // the coupled protocol they simply fire the change events on the UI thread.
                                ((AbstractSnapshotTableModel)model).refresh();
                            }
                            else
                                _runInUI(()->{
                                    T inner = thisComponentRef.get();
                                    if (inner == null)
                                        return;
                                    TableModel innerModel = inner.getModel();
                                    if ( innerModel instanceof AbstractTableModel ) {
                                        // We want the table model update to be as thorough as possible, so we
                                        // will fire a table structure changed event, followed by a table data
                                        // changed event.
                                        ((AbstractTableModel)innerModel).fireTableStructureChanged();
                                        ((AbstractTableModel)innerModel).fireTableDataChanged();
                                    }
                                    else
                                        throw new IllegalStateException("The table model is not an AbstractTableModel instance.");
                                });
                        })
                    );
                })
                ._this();
    }


    private static abstract class ListBasedTableModel<E> extends AbstractSnapshotTableModel
    {
        private final TableListDataSource<E> dataSource;
        private final boolean isEditable;

        ListBasedTableModel(boolean isEditable, TableListDataSource<E> dataSource) {
            this.isEditable = isEditable;
            this.dataSource = dataSource;
        }

        @Override protected boolean _liveCellEditable( int rowIndex, int columnIndex ) { return this.isEditable; }
        @Override protected @Nullable String _liveColumnName( int columnIndex ) { return null; }
        @Override protected Class<?> _liveColumnClass( int columnIndex ) { return Object.class; }

        protected List<List<E>> getData() {
            List<List<E>> data = dataSource.get();
            if ( data == null ) return new ArrayList<>(); // We really don't want null pointer in UIs.
            return data;
        }
    }


    private abstract static class MapBasedTableModel<E> extends AbstractSnapshotTableModel
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
        protected @Nullable String _liveColumnName(int column) {
            List<String> columnNames = new ArrayList<>(getData().keySet());
            if ( column < 0 || column >= columnNames.size() ) return null;
            return columnNames.get(column);
        }

        @Override protected Class<?> _liveColumnClass( int columnIndex ) { return Object.class; }
        @Override protected boolean _liveCellEditable( int rowIndex, int columnIndex ) { return this.isEditable; }
    }

    private static class MapBasedColumnMajorTableModel<E> extends MapBasedTableModel<E>
    {
        MapBasedColumnMajorTableModel(boolean isEditable, TableMapDataSource<E> dataSource) {
            super(isEditable, dataSource);
        }

        @Override
        protected int _liveRowCount() {
            Map<String, List<E>> data = getData();
            return data.values()
                        .stream()
                        .filter(Objects::nonNull) // Again, we don't want null pointer exceptions in UIs.
                        .mapToInt(List::size)
                        .max()
                        .orElse(0);
        }

        @Override
        protected int _liveColumnCount() { return getData().size(); }

        @Override
        protected @Nullable Object _liveValueAt( int rowIndex, int columnIndex ) {
            // Bound-check against the live map, never the (possibly stale) snapshot:
            // this method feeds 'takeLiveSnapshot', which runs while the previous
            // snapshot is still installed for the UI thread. A negative column index
            // also has to be caught before 'skip(..)', which would otherwise throw.
            if ( columnIndex < 0 )
                return null;
            List<E> column = getData().values().stream().skip(columnIndex).findFirst().orElse(null);
            if ( column == null )
                return null;
            if ( rowIndex < 0 || rowIndex >= column.size() )
                return null;
            return column.get(rowIndex);
        }

        @Override
        protected void _liveSetValueAt( @Nullable Object aValue, int rowIndex, int columnIndex ) {
            if ( columnIndex < 0 )
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
