package swingtree;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import sprouts.Association;
import sprouts.Pair;
import swingtree.api.Configurator;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.CellEditorListener;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.JTextComponent;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 *  A builder type for creating cell renderer for a list, combo box or table
 *  using a fluent API, typically through methods like {@link UIForList#withCells(Configurator)},
 *  {@link UIForCombo#withCells(Configurator)} or {@link UIForTable#withCells(Configurator)},
 *  where the builder is exposed to the configurator lambda. <p>
 *  A typical usage of this API may look something like this:
 *  <pre>{@code
 *      .withCells( it -> it
 *          .when( Number.class )
 *          .asText( cell -> cell.entryAsString()+" km/h" )
 *          .when( String.class )
 *          .as( cell -> {
 *              // do component based rendering:
 *              return cell.view( new JLabel( cell.entryAsString() ) );
 *              // or do 2D graphics rendering directly:
 *              return cell.renderer(Size.of(200,100), g -> {
 *              	// draw something
 *                  g.setColor( UI.color( cell.entryAsString() ) );
 *                  g.fillRect( 0, 0, 200, 100 );
 *              });
 *          })
 *      )
 *  }</pre>
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a collection of examples demonstrating how to use the API of this class.</b>
 *
 * @param <C> The type of the component which is used to render the cell.
 * @param <E> The type of the value of the cell.
 */
public final class CellBuilder<C extends JComponent, E> {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(CellBuilder.class);

    private BuiltCells<C,E> _state;

    static class CellView<C extends JComponent> {
        @Nullable Component _renderer = null;
        @Nullable Component _editor = null;
        final List<Configurator<CellConf<C, ?>>> _configurators = new ArrayList<>();
    }

    static <E> CellBuilder<JList<E>,E> forList(Class<E> elementType) {
        return (CellBuilder) new CellBuilder<>(JList.class, elementType);
    }
    static <C extends JComboBox<E>, E> CellBuilder<C,E> forCombo(Class<E> elementType) {
        return (CellBuilder) new CellBuilder<>(JComboBox.class, elementType);
    }
    static <E> CellBuilder<JTable,E> forTable(Class<E> elementType) {
        return (CellBuilder) new CellBuilder<>(JTable.class, elementType);
    }


    private CellBuilder(Class<C> componentType, Class<E> elementType) {
        _state = new BuiltCells<>(componentType, elementType);
    }

    private @Nullable Component findRenderer(@Nullable Object value) {
        Class type = (value == null ? Object.class : value.getClass());
        _state = _state.computeIfAbsent(type, CellView::new);
        return _state.rendererLookup().get(type).get()._renderer;
    }

    private void safeRenderer(@Nullable Object value, @Nullable Component renderer) {
        Class type = (value == null ? Object.class : value.getClass());
        _state = _state.computeIfAbsent(type, CellView::new);
        _state.rendererLookup().get(type).get()._renderer = renderer;
    }

    private @Nullable Component findEditor(@Nullable Object value) {
        Class type = (value == null ? Object.class : value.getClass());
        _state = _state.computeIfAbsent(type, CellView::new);
        return _state.rendererLookup().get(type).get()._editor;
    }

    private void safeEditor(@Nullable Object value, @Nullable Component editor) {
        Class type = (value == null ? Object.class : value.getClass());
        _state = _state.computeIfAbsent(type, CellView::new);
        _state.rendererLookup().get(type).get()._editor = editor;
    }

    /**
     * Use this to specify for which type of cell value you want custom rendering next.
     * The object returned by this method allows you to specify how to render the values.
     *
     * @param valueType The type of cell value, for which you want custom rendering.
     * @param <T>       The type parameter of the cell value, for which you want custom rendering.
     * @return The {@link RenderAs} builder API step which expects you to provide a lambda for customizing how a cell is rendered.
     */
    public <T extends E> RenderAs<C, E, T> when( Class<T> valueType ) {
        NullUtil.nullArgCheck(valueType, "valueType", Class.class);
        return when(valueType, cell -> true);
    }

    /**
     * Use this to specify a specific type for which you want custom rendering
     * as well as a predicate which tests if a cell value should be rendered.
     * The object returned by this method allows you to specify how to render the values
     * using methods like {@link RenderAs#as(Configurator)} or {@link RenderAs#asText(Function)}.
     *
     * @param valueType      The type of cell value, for which you want custom rendering.
     * @param valueValidator A predicate which should return true if the cell value should be rendered.
     * @param <T>            The type parameter of the cell value, for which you want custom rendering.
     * @return The {@link RenderAs} builder API step which expects you to provide a lambda for customizing how a cell is rendered.
     */
    public <T extends E> RenderAs<C, E, T> when(
        Class<T> valueType,
        Predicate<CellConf<C, T>> valueValidator
    ) {
        NullUtil.nullArgCheck(valueType, "valueType", Class.class);
        NullUtil.nullArgCheck(valueValidator, "valueValidator", Predicate.class);
        return new RenderAs<>(this, valueType, valueValidator);
    }

    <V> void _store(
        Class valueType,
        Predicate predicate,
        Configurator<CellConf<C, V>> valueInterpreter
    ) {
        NullUtil.nullArgCheck(valueType, "valueType", Class.class);
        NullUtil.nullArgCheck(predicate, "predicate", Predicate.class);
        NullUtil.nullArgCheck(valueInterpreter, "valueInterpreter", Configurator.class);
        _state = _state.computeIfAbsent(valueType, CellView::new);
        List<Configurator<CellConf<C, ?>>> found = _state.rendererLookup().get(valueType).get()._configurators;
        found.add(cell -> {
            if (predicate.test(cell))
                return valueInterpreter.configure((CellConf<C, V>) cell);
            else
                return cell;
        });
    }

    static <C extends JComponent, E, T extends JComponent> Component _updateAndGetComponent(
            BuiltCells<C,E> state,
            Function<@Nullable Object, Component> defaultRenderer,
            BiConsumer<@Nullable Component, CellConf<?,?>> saveComponent,
            CellConf<T, Object> cell
    ) {
        @Nullable Object value = cell.entry().orElse(null);
        List<Configurator<CellConf<C, ?>>> interpreter = _find(value, state.rendererLookup());
        if ( interpreter.isEmpty() )
            return defaultRenderer.apply(value);
        else {
           /*
               If a view is persisted from previous rendering, initialize with what is most
               like what the user would expect. This is however mainly to avoid
               rendering state left over from previous rendering.
            */
            cell = _initializeViewIfPresent(cell);

            for ( Configurator<CellConf<C,?>> configurator : interpreter ) {
                CellConf newCell = cell;
                try {
                    newCell = configurator.configure(newCell);
                } catch (Exception e) {
                    log.error(
                            "Failed to configure cell renderer for " +
                                    "component '"+cell.getHost().getClass().getSimpleName()+"'.",
                            e
                    );
                }
                if ( newCell != null )
                    cell = newCell;
            }
            Component choice;
            Optional<Object> presentationEntry = cell.presentationEntry();
            if (cell.view().isPresent()) {
                choice = cell.view().orElseThrow();
                saveComponent.accept(choice, cell);
            } else if (presentationEntry.isPresent()) {
                choice = defaultRenderer.apply(presentationEntry.get());
                saveComponent.accept(null, cell);
            } else {
                choice = defaultRenderer.apply(value);
                saveComponent.accept(null, cell);
            }

            if (!cell.toolTips().isEmpty() && choice instanceof JComponent)
                ((JComponent) choice).setToolTipText(String.join("; ", cell.toolTips()));

            return choice;
        }
    }

    private static CellConf _initializeViewIfPresent(CellConf<?, Object> cell) {
        if ( cell.view().isPresent() ) {
            Component view = cell.view().orElseThrow();
            @Nullable Object value = cell.entry().orElse(null);
            view.setEnabled(true);
            view.setVisible(true);
            if ( view instanceof AbstractButton ) {
                AbstractButton button = (AbstractButton) view;
                button.setSelected(false);
                if ( value instanceof Boolean )
                    button.setSelected((Boolean) value);
                else if ( value instanceof String )
                    button.setText((String) value);
                else if ( value instanceof Icon )
                    button.setIcon((Icon) value);
            } else if ( view instanceof JComboBox ) {
                JComboBox<?> comboBox = (JComboBox<?>) view;
                if ( value != null )
                    comboBox.setSelectedItem(value);
            } else if ( view instanceof JTextComponent) {
                JTextComponent textField = (JTextComponent) view;
                if ( value != null )
                    textField.setText(value.toString());
            } else if ( view instanceof JLabel ) {
                JLabel label = (JLabel) view;
                if ( value != null )
                    label.setText(value.toString());
            }
        }
        return cell;
    }

    class SimpleTableCellRenderer implements TableCellRenderer, TableCellEditor, TreeCellRenderer, TreeCellEditor
    {
        private final DefaultTableCellRenderer _defaultRenderer = new DefaultTableCellRenderer();
        private final DefaultTreeCellRenderer _defaultTreeRenderer = new DefaultTreeCellRenderer();
        private final InternalCellEditor _basicEditor;

        SimpleTableCellRenderer(Class<? extends JComponent> hostType) {
            _basicEditor = new InternalCellEditor(hostType);
        }

        public @Nullable Component getEditorComponent() {
            return _basicEditor.getComponent();
        }

        private @Nullable Component _loadEditor(@Nullable Object value) {
            @Nullable Component editor = findEditor(value);
            if ( editor != null )
                editor = _setEditorComponent(editor);
            return editor;
        }

        private @Nullable Component _setEditorComponent(@Nullable Component editor) {
            if ( _basicEditor.getComponent() != editor ) {
                if (editor instanceof JCheckBox) {
                    _basicEditor.setEditor((JCheckBox) editor);
                } else if (editor instanceof JComboBox) {
                    _basicEditor.setEditor((JComboBox<?>) editor);
                } else if (editor instanceof JTextField) {
                    _basicEditor.setEditor((JTextField) editor);
                }
            }
            return _basicEditor.getComponent();
        }

        private void _setEditor(
                @Nullable Component newEdior,
                @Nullable Object entryFromModel,
                CellConf<?,?> currentCell
        ) {
            newEdior = _setEditorComponent(newEdior);
            safeEditor(entryFromModel, newEdior);
            try {
                // Apply user values to editor:
                Optional<Object> presentationEntry = currentCell.presentationEntry();
                if ( presentationEntry.isPresent() )
                    _basicEditor.setEntry(presentationEntry.orElse(null), entryFromModel, entryFromModel == null ? Object.class : entryFromModel.getClass());
                else if ( currentCell.view().isEmpty() )
                    _basicEditor.setEntry(currentCell.entry().orElse(null), entryFromModel, entryFromModel == null ? Object.class : entryFromModel.getClass());
            } catch (Exception e) {
                log.error("Failed to populate cell editor!", e);
            }
        }

        private void _setRenderer(
            @Nullable Component newRenderer,
            @Nullable Object entryFromModel,
            CellConf<?,?> currentCell
        ) {
            safeRenderer(entryFromModel, newRenderer);
            try {
                Optional<Object> presentationEntry = currentCell.presentationEntry();

                if ( presentationEntry.isPresent() || currentCell.view().isEmpty() ) {
                    @Nullable Object toBePresented = presentationEntry.orElse(currentCell.entry().orElse(null));
                    if ( newRenderer instanceof AbstractButton ) {
                        AbstractButton button = (AbstractButton) newRenderer;
                        if ( toBePresented instanceof Boolean )
                            button.setSelected((Boolean) toBePresented);
                        else if ( toBePresented instanceof String )
                            button.setText((String) toBePresented);
                        else if ( toBePresented instanceof Icon )
                            button.setIcon((Icon) toBePresented);
                    } else if ( newRenderer instanceof JComboBox ) {
                        JComboBox<?> comboBox = (JComboBox<?>) newRenderer;
                        comboBox.setSelectedItem(toBePresented);
                    } else if ( newRenderer instanceof JTextComponent ) {
                        JTextComponent textField = (JTextComponent) newRenderer;
                        textField.setText(toBePresented == null ? "" : toBePresented.toString());
                    } else if ( newRenderer instanceof JLabel ) {
                        JLabel label = (JLabel) newRenderer;
                        label.setText(toBePresented == null ? "" : toBePresented.toString());
                    }
                }
            } catch (Exception e) {
                log.error("Failed to populate cell editor!", e);
            }
        }

        private Component _fit( JTable table, int row, int column, Component view ) {
            try {
                boolean isDefaultEditor = _basicEditor.getComponent() == view && _basicEditor.hasDefaultComponent();
                boolean isDefaultRenderer = view instanceof InternalLabelForRendering ||
                                            view.getClass() == DefaultListCellRenderer.class ||
                                            view instanceof DefaultTableCellRenderer ||
                                            view instanceof DefaultTreeCellRenderer;

                if ( !isDefaultRenderer && !isDefaultEditor ) {
                    /*
                        If you want the table to fit the cell size to the content,
                        then you have to use a custom view / editor!
                    */
                    Dimension minSize = view.getMinimumSize();
                    TableColumn currentColumn = table.getColumnModel().getColumn(column);
                    if (currentColumn.getMinWidth() < minSize.width)
                        currentColumn.setMinWidth(minSize.width);
                    if (table.getRowHeight(row) < minSize.height)
                        table.setRowHeight(row, minSize.height);
                }
            } catch (Exception e) {
                log.error("Failed to fit cell size", e);
            }
            return view;
        }

        @Override
        public Component getTableCellRendererComponent(
            final JTable           table,
            final @Nullable Object entryFromModel,
            final boolean          isSelected,
            final boolean          hasFocus,
            final int              row,
            final int              column
        ) {
            _state.checkTypeValidity(entryFromModel);
            return _fit(table, row, column,
                        _updateAndGetComponent(
                            _state,
                             localEntry -> _defaultRenderer.getTableCellRendererComponent(table, localEntry, isSelected, hasFocus, row, column),
                             (choice, newRenderer) -> _setRenderer(choice, entryFromModel, newRenderer),
                             CellConf.of(
                                 null, findRenderer(entryFromModel),
                                 table, entryFromModel, isSelected, hasFocus, false, false, false, row, column,
                                 () -> _defaultRenderer.getTableCellRendererComponent(table, entryFromModel, isSelected, hasFocus, row, column)
                             )
                        )
                    );
        }

        @Override
        public Component getTableCellEditorComponent(
            final JTable           table,
            final @Nullable Object entryFromModel,
            final boolean          isSelected,
            final int              row,
            final int              column
        ) {
            _state.checkTypeValidity(entryFromModel);
            _basicEditor.ini(table, row, column);
            _basicEditor.updateForTable(table, column);
            _basicEditor.setEntry(entryFromModel, entryFromModel, entryFromModel == null ? Object.class : entryFromModel.getClass());
            return _fit(table, row, column,
                        _updateAndGetComponent(
                            _state,
                             localEntry -> _basicEditor.getTableCellEditorComponent(table, localEntry, isSelected, row, column),
                             (choice, newEditor) -> _setEditor(choice, entryFromModel, newEditor),
                             CellConf.of(
                                 null, _loadEditor(entryFromModel),
                                 table, entryFromModel, isSelected, true, true, false, false, row, column,
                                 () -> _basicEditor.getTableCellEditorComponent(table, entryFromModel, isSelected, row, column)
                             )
                        )
                    );
        }

        @Override
        public Component getTreeCellRendererComponent(
            final JTree            tree,
            final @Nullable Object entryFromModel,
            final boolean          selected,
            final boolean          expanded,
            final boolean          leaf,
            final int              row,
            final boolean          hasFocus
        ) {
            _state.checkTypeValidity(entryFromModel);
            String entryAsString = tree.convertValueToText(entryFromModel, selected, expanded, leaf, row, false);
            _basicEditor.ini(tree, row, 0);
            _basicEditor.setEntry(entryAsString, entryFromModel, entryFromModel == null ? Object.class : entryFromModel.getClass());
            return _updateAndGetComponent(
                         _state,
                         localValue -> _defaultTreeRenderer.getTreeCellRendererComponent(tree, localValue, selected, expanded, leaf, row, hasFocus),
                         (choice, newRenderer) -> _setRenderer(choice, entryFromModel, newRenderer),
                         CellConf.of(
                             null, findRenderer(entryFromModel),
                             tree, entryFromModel, selected, hasFocus, false, expanded, leaf, row, 0,
                             () -> _defaultTreeRenderer.getTreeCellRendererComponent(tree, entryFromModel, selected, expanded, leaf, row, hasFocus)
                         )
                    );
        }

        @Override
        public Component getTreeCellEditorComponent(
            final JTree            tree,
            final @Nullable Object entryFromModel,
            final boolean          isSelected,
            final boolean          expanded,
            final boolean          leaf,
            final int              row
        ) {
            _state.checkTypeValidity(entryFromModel);
            _basicEditor.ini(tree, row, 0);
            return _updateAndGetComponent(
                         _state,
                         localEntry -> _basicEditor.getTreeCellEditorComponent(tree, localEntry, isSelected, expanded, leaf, row),
                        (choice, newEditor) -> _setEditor(choice, entryFromModel, newEditor),
                         CellConf.of(
                             null, _loadEditor(entryFromModel),
                             tree, entryFromModel, isSelected,
                             true, true, expanded, leaf, row, 0,
                             () -> _basicEditor.getTreeCellEditorComponent(tree, entryFromModel, isSelected, expanded, leaf, row)
                         )
                    );
        }

        @Override
        public @Nullable Object getCellEditorValue() {
            return _basicEditor.getCellEditorValue();
        }

        @Override
        public boolean isCellEditable(EventObject anEvent) {
            return _basicEditor.isCellEditable(anEvent);
        }

        @Override
        public boolean shouldSelectCell(EventObject anEvent) {
            return _basicEditor.shouldSelectCell(anEvent);
        }

        @Override
        public boolean stopCellEditing() {
            return _basicEditor.stopCellEditing();
        }

        @Override
        public void cancelCellEditing() {
            _basicEditor.cancelCellEditing();
        }

        @Override
        public void addCellEditorListener(CellEditorListener l) {
            _basicEditor.addCellEditorListener(l);
        }

        @Override
        public void removeCellEditorListener(CellEditorListener l) {
            _basicEditor.removeCellEditorListener(l);
        }
    }

    private static class SimpleListCellRenderer<O extends JComponent, E> implements ListCellRenderer<Object>
    {
        private final O _component;
        private final ListCellRenderer<Object> _defaultRenderer;
        private BuiltCells<O, E> _state;


        private SimpleListCellRenderer(O component, BuiltCells<O, E> state) {
            _state = state;
            _component = Objects.requireNonNull(component);
            if ( component instanceof JComboBox )
                _defaultRenderer = new BasicComboBoxRenderer.UIResource();
            else
                _defaultRenderer = new DefaultListCellRenderer.UIResource();
        }

        private @Nullable Component findRenderer(@Nullable Object value) {
            Class type = (value == null ? Object.class : value.getClass());
            _state = _state.computeIfAbsent(type, CellView::new);
            return _state.rendererLookup().get(type).get()._renderer;
        }

        private void safeRenderer(@Nullable Object value, @Nullable Component renderer) {
            Class type = (value == null ? Object.class : value.getClass());
            _state = _state.computeIfAbsent(type, CellView::new);
            _state.rendererLookup().get(type).get()._renderer = renderer;
        }

        @Override
        public Component getListCellRendererComponent(
            final JList   list,
            final Object  value,
            final int     row,
            final boolean isSelected,
            final boolean hasFocus
        ) {
            _state.checkTypeValidity(value);
            List<Configurator<CellConf<O, ?>>> interpreter = _find(value, _state.rendererLookup());
            if (interpreter.isEmpty())
                return _defaultRenderer.getListCellRendererComponent(list, value, row, isSelected, hasFocus);
            else {
                CellConf<O, Object> cell = CellConf.of(
                                                        list,
                                                        findRenderer(value),
                                                        _component, value, isSelected,
                                                        hasFocus, false, false, false, row, 0,
                                                        ()->_defaultRenderer.getListCellRendererComponent(list, value, row, isSelected, hasFocus)
                                                    );

                for ( Configurator<CellConf<O,?>> configurator : interpreter ) {
                    CellConf newCell = cell;
                    try {
                        newCell = configurator.configure(newCell);
                    } catch (Exception e) {
                        log.error(
                                "Failed to configure cell renderer for " +
                                "component '"+_component.getClass().getSimpleName()+"'.",
                                e
                            );
                    }
                    if ( newCell != null )
                        cell = newCell;
                }
                Component choice;
                Optional<Object> presentationEntry = cell.presentationEntry();
                if (cell.view().isPresent()) {
                    choice = cell.view().orElseThrow();
                    safeRenderer(value, choice);
                } else if (presentationEntry.isPresent()) {
                    choice = _defaultRenderer.getListCellRendererComponent(list, presentationEntry.get(), row, isSelected, hasFocus);
                    safeRenderer(value, null);
                } else {
                    choice = _defaultRenderer.getListCellRendererComponent(list, value, row, isSelected, hasFocus);
                    safeRenderer(value, null);
                }

                if (!cell.toolTips().isEmpty() && choice instanceof JComponent)
                    ((JComponent) choice).setToolTipText(String.join("; ", cell.toolTips()));

                return choice;
            }
        }

        Optional<ComboBoxEditor> establishEditor() {
            if ( !( _component instanceof JComboBox ) )
                return Optional.empty();
            JComboBox<?> comboBox = (JComboBox<?>) _component;

            CellConf<JComboBox<?>, Object> cell = CellConf.of(
                null, null, comboBox, null, false, false, true, false, false, 0, 0, () -> null
            );
            List<Configurator<CellConf<O, ?>>> interpreter = _findAll(_state.rendererLookup());
            if (interpreter.isEmpty())
                return Optional.empty();
            else {
                for ( Configurator<CellConf<O,?>> configurator : interpreter ) {
                    CellConf<JComboBox<?>,Object> newCell = cell;
                    try {
                        newCell = configurator.configure((CellConf)newCell);
                    } catch (Exception e) {
                        log.error(
                                "Failed to establish cell editor through cell configurator " +
                                "for component '"+_component.getClass().getSimpleName()+"'.",
                                e
                            );
                    }
                    try {
                        if ( newCell != null )
                            cell = newCell.updateView(v -> v.update(c -> c instanceof JTextField ? c : null ) );
                    } catch (Exception e) {
                        log.error(
                                "Failed to establish cell editor through cell configurator " +
                                "for component '"+_component.getClass().getSimpleName()+"'.",
                                e
                            );
                    }
                }

                if (!cell.view().isPresent())
                    return Optional.empty();

                Component choice = cell.view().orElseThrow();

                if ( !(choice instanceof JTextField) )
                    return Optional.empty();

                JTextField textField = (JTextField) choice;

                return Optional.of(new InternalComboBoxCellEditor(textField));
            }
        }
    }

    private static <C extends JComponent> List<Configurator<CellConf<C, ?>>> _find(
        @Nullable Object value,
        Association<Class<?>, CellView<C>> rendererLookup
    ) {
        Class<?> type = (value == null ? Object.class : value.getClass());
        List<Configurator<CellConf<C, ?>>> cellRenderer = new ArrayList<>();
        for (Pair<Class<?>, CellView<C>> e : rendererLookup.entrySet()) {
            if (e.first().isAssignableFrom(type))
                cellRenderer.addAll(e.second()._configurators);
        }
        // We reverse the cell renderers, so that the most un-specific one is first
        Collections.reverse(cellRenderer);
        return cellRenderer;
    }

    private static <C extends JComponent> List<Configurator<CellConf<C,?>>> _findAll(
        Association<Class<?>, CellView<C>> rendererLookup
    ) {
        List<Configurator<CellConf<C, ?>>> cellRenderer = new ArrayList<>();
        for (CellView<C> e : rendererLookup.values()) {
            cellRenderer.addAll(e._configurators);
        }
        // We reverse the cell renderers, so that the most un-specific one is first
        Collections.reverse(cellRenderer);
        return cellRenderer;
    }

    SimpleTableCellRenderer getForTable() {
        _addDefaultRendering();
        if (JTable.class.isAssignableFrom(_state.componentType())) {
            SimpleTableCellRenderer renderer = new SimpleTableCellRenderer(_state.componentType());
            return renderer;
        } else
            throw new IllegalArgumentException("Renderer was set up to be used for a JTable!");
    }

    TreeCellRenderer getForTree() {
        _addDefaultRendering();
        if (JTree.class.isAssignableFrom(_state.componentType()))
            return new SimpleTableCellRenderer(_state.componentType());
        else
            throw new IllegalArgumentException("Renderer was set up to be used for a JTree!");
    }

    /**
     * Like many things in the SwingTree library, this class is
     * essentially a convenient builder for a {@link ListCellRenderer}.
     * This internal method actually builds the {@link ListCellRenderer} instance,
     * see {@link UIForList#withCell(swingtree.api.Configurator)} for more details
     * about how to use this class as pat of the main API.
     *
     * @param list The list for which the renderer is to be built.
     */
    void buildForList( C list ) {
        _addDefaultRendering();
        if (JList.class.isAssignableFrom(_state.componentType())) {
            SimpleListCellRenderer<C,E> renderer = new SimpleListCellRenderer<>(list, _state);
            JList<E> jList = (JList<E>) list;
            jList.setCellRenderer(renderer);
        } else if (JComboBox.class.isAssignableFrom(_state.componentType()))
            throw new IllegalArgumentException(
                "Renderer was set up to be used for a JList! " +
                "(not " + _state.componentType().getSimpleName() + ")"
            );
        else
            throw new IllegalArgumentException(
                "Renderer was set up to be used for an unknown component type! " +
                "(cannot handle '" + _state.componentType().getSimpleName() + "')"
            );
    }

    void buildForCombo(C comboBox, boolean establishEditorAlso) {
        _addDefaultRendering();
        if (JComboBox.class.isAssignableFrom(_state.componentType())) {
            SimpleListCellRenderer<C, E> renderer = new SimpleListCellRenderer<>(comboBox, _state);
            JComboBox<E> combo = (JComboBox<E>) comboBox;
            combo.setRenderer(renderer);
            if (establishEditorAlso) {
                renderer.establishEditor().ifPresent(combo::setEditor);
            }
        } else
            throw new IllegalArgumentException(
                "Renderer was set up to be used for a JComboBox! " +
                "(not " + _state.componentType().getSimpleName() + ")"
            );
    }

    private void _addDefaultRendering() {
        // We use the default text renderer for objects
        _store(Object.class, cell -> true, _createDefaultTextRenderer(cell -> cell.entryAsString()));
    }

    static class InternalLabelForRendering extends DefaultListCellRenderer {
        InternalLabelForRendering(String text) {
            setText(text);
            setOpaque(true);
        }
    }

    static <C extends JComponent, V> Configurator<CellConf<C, V>> _createDefaultTextRenderer(
            Function<CellConf<C, V>, String> renderer
    ) {
        Function<CellConf<C, V>, String> exceptionSafeRenderer = cell -> {
            try {
                return renderer.apply(cell);
            } catch (Exception e) {
                log.error("Failed to convert cell to displayable String!", e);
                return "";
            }
        };
        return cell -> {
            if ( cell.isEditing() )
                return cell;

            Component existing = cell.view().orElseNullable(null);
            InternalLabelForRendering l = (existing instanceof InternalLabelForRendering) ? (InternalLabelForRendering) existing : null;
            if ( existing != null && l == null )
                return cell; // The user has defined a custom renderer, so we don't touch it.

            if ( l == null )
                l = new InternalLabelForRendering(exceptionSafeRenderer.apply(cell));
            else
                l.setText(exceptionSafeRenderer.apply(cell));

            Color bg = null;
            Color fg = null;

            if ( cell.getHost() instanceof JComboBox && cell.getListView().isPresent() ) {
                JList<?> list = cell.getListView().get();
                if (cell.isSelected()) {
                    bg = list.getSelectionBackground();
                    fg = list.getSelectionForeground();
                }
                else {
                    bg = list.getBackground();
                    fg = list.getForeground();
                }
            } else if ( cell.getHost() instanceof JList ) {
                JList<?> jList = (JList<?>) cell.getHost();
                bg = jList.getSelectionBackground();
                fg = jList.getSelectionForeground();
                if ( bg == null )
                    bg = UIManager.getColor("List.selectionBackground");
                if ( fg == null )
                    fg = UIManager.getColor("List.selectionForeground");
            } else if ( cell.getHost() instanceof JTable ) {
                JTable jTable = (JTable) cell.getHost();
                bg = jTable.getSelectionBackground();
                fg = jTable.getSelectionForeground();
                if ( bg == null )
                    bg = UIManager.getColor("Table.selectionBackground");
                if ( fg == null )
                    fg = UIManager.getColor("Table.selectionForeground");
            }

            if ( bg == null )
                bg = cell.getHost().getBackground();
            if ( fg == null )
                fg = cell.getHost().getForeground();

            if ( bg == null )
                bg = UIManager.getColor( "ComboBox.selectionBackground" );
            if ( fg == null )
                fg = UIManager.getColor( "ComboBox.selectionForeground" );

            if ( bg == null )
                bg = UIManager.getColor( "List.dropCellBackground" );
            if ( fg == null )
                fg = UIManager.getColor( "List.dropCellForeground" );

            if ( bg == null )
                bg = UIManager.getColor( "ComboBox.background" );
            if ( fg == null )
                fg = UIManager.getColor( "ComboBox.foreground" );

            // Lastly we make sure the color is a user color, not a LaF color:
            if ( bg != null ) // This is because of a weired JDK bug it seems!
                bg = new Color( bg.getRGB() );
            if ( fg != null )
                fg = new Color( fg.getRGB() );

            if (cell.isSelected()) {
                if ( bg != null ) _setBackgroundColor(l, bg);
                if ( fg != null ) _setForegroundColor(l, fg);
            }
            else {
                Color normalBg = cell.getHost().getBackground();

                // We need to make sure the color is a user color, not a LaF color:
                if ( normalBg != null )
                    normalBg = new Color( normalBg.getRGB() ); // This is because of a weired JDK bug it seems!

                if ( cell.row() % 2 == 1 ) {
                    // We determine if the base color is more bright or dark,
                    // and then we set the foreground color accordingly
                    double brightness = (0.299 * normalBg.getRed() + 0.587 * normalBg.getGreen() + 0.114 * normalBg.getBlue()) / 255;
                    if ( brightness < 0.5 )
                        normalBg = brighter(normalBg);
                    else
                        normalBg = darker(normalBg);
                }
                if ( bg != null )
                    _setBackgroundColor( l, normalBg );
                if ( fg != null )
                    _setForegroundColor( l, cell.getHost().getForeground() );
            }

            // TODO:
            //l.setFont(cell.getHost().getFont()); // Is this a good idea?
            if ( l.isEnabled() != cell.getHost().isEnabled() )
                l.setEnabled(cell.getHost().isEnabled());

            Border border = null;
            if ( cell.hasFocus() ) {
                if ( cell.isSelected() )
                    border = UIManager.getBorder( "List.focusSelectedCellHighlightBorder" );

                if ( border == null )
                    border = UIManager.getBorder( "List.focusCellHighlightBorder" );
            }
            else
                border = UIManager.getBorder( "List.cellNoFocusBorder" );

            if ( border != null && border != l.getBorder() )
                l.setBorder(border);

            return cell.view(l);
        };
    }

    private static void _setBackgroundColor( JComponent comp, @Nullable Color color ) {
        if ( color == null ) {
            if ( comp.isBackgroundSet() )
                comp.setBackground(null);
            else
                return; // Already null!
        }
        else
            if ( !Objects.equals(comp.getBackground(), color) )
                comp.setBackground( color );
    }

    private static void _setForegroundColor( JComponent comp, @Nullable Color color ) {
        if ( color == null ) {
            if ( comp.isForegroundSet() )
                comp.setForeground(null);
            else
                return; // Already null!
        }
        else
            if ( !Objects.equals(comp.getForeground(), color) )
                comp.setForeground( color );
    }


    private static Color darker( Color c ) {
        final double PERCENTAGE = (242*3.0)/(255*3.0);
        return new Color(
                (int)(c.getRed()*PERCENTAGE),
                (int)(c.getGreen()*PERCENTAGE),
                (int)(c.getBlue()*PERCENTAGE)
        );
    }

    private static Color brighter( Color c ) {
        final double FACTOR = (242*3.0)/(255*3.0);
        int r = c.getRed();
        int g = c.getGreen();
        int b = c.getBlue();
        int alpha = c.getAlpha();

        int i = (int)(1.0/(1.0-FACTOR));
        if ( r == 0 && g == 0 && b == 0) {
            return new Color(i, i, i, alpha);
        }
        if ( r > 0 && r < i ) r = i;
        if ( g > 0 && g < i ) g = i;
        if ( b > 0 && b < i ) b = i;

        return new Color(Math.min((int)(r/FACTOR), 255),
                Math.min((int)(g/FACTOR), 255),
                Math.min((int)(b/FACTOR), 255),
                alpha);
    }

}
