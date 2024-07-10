package swingtree;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import swingtree.api.Configurator;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.CellEditorListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreeCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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
 *          .asText( cell -> cell.valueAsString().orElse("")+" km/h" )
 *          .when( String.class )
 *          .as( cell -> {
 *              // do component based rendering:
 *              cell.setRenderer( new JLabel( cell.valueAsString().orElse("") ) );
 *              // or do 2D graphics rendering directly:
 *              cell.setRenderer( g -> {
 *              	// draw something
 *                  g.setColor( UI.color( cell.valueAsString().orElse("") ) );
 *                  g.fillRect( 0, 0, cell.getComponent().getWidth(), cell.getComponent().getHeight() );
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

    private final Class<C> _componentType;
    private final Class<E> _elementType;
    private final Map<Class<?>, List<Configurator<CellConf<C, ?>>>> _rendererLookup = new LinkedHashMap<>(16);


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
        _componentType = componentType;
        _elementType = elementType;
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
        List<Configurator<CellConf<C, ?>>> found = _rendererLookup.computeIfAbsent(valueType, k -> new ArrayList<>());
        found.add(cell -> {
            if (predicate.test(cell))
                return valueInterpreter.configure((CellConf<C, V>) cell);
            else
                return cell;
        });
    }

    class SimpleTableCellRenderer implements TableCellRenderer, TableCellEditor, TreeCellRenderer, TreeCellEditor
    {
        private final DefaultTableCellRenderer _defaultRenderer = new DefaultTableCellRenderer();
        private final DefaultTreeCellRenderer _defaultTreeRenderer = new DefaultTreeCellRenderer();
        private final InternalCellEditor _basicEditor = new InternalCellEditor();
        private @Nullable Component _lastCustomRenderer;

        SimpleTableCellRenderer() {}

        public <T extends JComponent> Component _updateAndGetComponent(
            Function<@Nullable Object, Component> defaultRenderer,
            BiConsumer<@Nullable Component, CellConf<?,?>> saveComponent,
            CellConf<T, Object> cell
        ) {
            @Nullable Object value = cell.entry().orElse(null);
            List<Configurator<CellConf<C, ?>>> interpreter = _find(value, _rendererLookup);
            if ( interpreter.isEmpty() )
                return defaultRenderer.apply(value);
            else {
                for ( Configurator<CellConf<C,?>> configurator : interpreter ) {
                    CellConf newCell = configurator.configure((CellConf)cell);
                    if ( newCell != null )
                        cell = newCell;
                }
                Component choice;
                if (cell.view().isPresent()) {
                    choice = cell.view().orElseThrow();
                    saveComponent.accept(choice, cell);
                } else if (cell.presentationEntry().isPresent()) {
                    choice = defaultRenderer.apply(cell.presentationEntry().get());
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

        public @Nullable Component getEditorComponent() {
            return _basicEditor.getComponent();
        }

        private void _setEditor(@Nullable Component choice, CellConf<?,?> currentCell) {
            if ( _basicEditor.getComponent() == choice )
                return;
            boolean success = false;
            if (choice instanceof JCheckBox) {
                _basicEditor.setEditor((JCheckBox) choice);
                success = true;
            } else if (choice instanceof JComboBox) {
                _basicEditor.setEditor((JComboBox<?>) choice);
                success = true;
            } else if (choice instanceof JTextField) {
                _basicEditor.setEditor((JTextField) choice);
                success = true;
            }
            if ( success ) {
                try {
                    if ( currentCell.presentationEntry().isPresent() )
                        _basicEditor.setValue(currentCell.presentationEntry().orElse(null));
                    else
                        _basicEditor.setValue(currentCell.entryAsString());
                } catch (Exception e) {
                    log.error("Failed to populate cell editor!", e);
                }
            }
        }

        private void _setRenderer(@Nullable Component choice, CellConf<?,?> currentCell) {
            _lastCustomRenderer = choice;
        }

        private Component _fit(JTable table, int row, int column, Component view) {
            if ( !(view instanceof InternalLabelForRendering) ) {
                try {
                    Dimension minSize = view.getMinimumSize();
                    TableColumn currentColumn = table.getColumnModel().getColumn(column);
                    if (currentColumn.getMinWidth() < minSize.width)
                        currentColumn.setMinWidth(minSize.width);
                    if (table.getRowHeight(row) < minSize.height)
                        table.setRowHeight(row, minSize.height);

                } catch (Exception e) {
                    log.error("Failed to fit cell size", e);
                }
            }
            return view;
        }

        @Override
        public Component getTableCellRendererComponent(
            final JTable           table,
            final @Nullable Object value,
            final boolean          isSelected,
            final boolean          hasFocus,
            final int              row,
            final int              column
        ) {
            return _fit(table, row, column,
                        _updateAndGetComponent(
                             localValue -> _defaultRenderer.getTableCellRendererComponent(table, localValue, isSelected, hasFocus, row, column),
                             this::_setRenderer,
                             CellConf.of(
                                 _lastCustomRenderer,
                                 table, value, isSelected, hasFocus, false, false, false, row, column,
                                 () -> _defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                             )
                        )
                    );
        }

        @Override
        public Component getTableCellEditorComponent(
            final JTable           table,
            final @Nullable Object value,
            final boolean          isSelected,
            final int              row,
            final int              column
        ) {
            return _fit(table, row, column,
                        _updateAndGetComponent(
                             localValue -> _basicEditor.getTableCellEditorComponent(table, localValue, isSelected, row, column),
                             this::_setEditor,
                             CellConf.of(
                                 _basicEditor.getCustomComponent(),
                                 table, value, isSelected, true, true, false, false, row, column,
                                 () -> _basicEditor.getTableCellEditorComponent(table, value, isSelected, row, column)
                             )
                        )
                    );
        }

        @Override
        public Component getTreeCellRendererComponent(
            final JTree            tree,
            final @Nullable Object value,
            final boolean          selected,
            final boolean          expanded,
            final boolean          leaf,
            final int              row,
            final boolean          hasFocus
        ) {
            return _updateAndGetComponent(
                         localValue -> _defaultTreeRenderer.getTreeCellRendererComponent(tree, localValue, selected, expanded, leaf, row, hasFocus),
                         this::_setRenderer,
                         CellConf.of(
                             _lastCustomRenderer,
                             tree, value, selected, hasFocus, false, expanded, leaf, row, 0,
                             () -> _defaultTreeRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
                         )
                    );
        }

        @Override
        public Component getTreeCellEditorComponent(
            final JTree            tree,
            final @Nullable Object value,
            final boolean          isSelected,
            final boolean          expanded,
            final boolean          leaf,
            final int              row
        ) {
            return _updateAndGetComponent(
                         localValue -> _basicEditor.getTreeCellEditorComponent(tree, localValue, isSelected, expanded, leaf, row),
                         this::_setEditor,
                         CellConf.of(
                             _basicEditor.getComponent(), tree, value, isSelected,
                             true, true, expanded, leaf, row, 0,
                             () -> _basicEditor.getTreeCellEditorComponent(tree, value, isSelected, expanded, leaf, row)
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

    private class SimpleListCellRenderer<O extends C> implements ListCellRenderer<Object>
    {
        private final O _component;
        private final DefaultListCellRenderer _defaultRenderer = new DefaultListCellRenderer();
        //private final ComboBoxEditor _basicEditor = new ComboBoxEditor();
        private @Nullable Component _lastCustomRenderer;

        private SimpleListCellRenderer(O component) {
            _component = Objects.requireNonNull(component);
        }

        @Override
        public Component getListCellRendererComponent(
            final JList   list,
            final Object  value,
            final int     row,
            final boolean isSelected,
            final boolean hasFocus
        ) {
            List<Configurator<CellConf<C, ?>>> interpreter = _find(value, _rendererLookup);
            if (interpreter.isEmpty())
                return _defaultRenderer.getListCellRendererComponent(list, value, row, isSelected, hasFocus);
            else {
                CellConf<O, Object> cell = CellConf.of(
                                                        _lastCustomRenderer,
                                                        _component, value, isSelected,
                                                        hasFocus, false, false, false, row, 0,
                                                        ()->_defaultRenderer.getListCellRendererComponent(list, value, row, isSelected, hasFocus)
                                                    );

                for ( Configurator<CellConf<C,?>> configurator : interpreter ) {
                    CellConf newCell = configurator.configure((CellConf)cell);
                    if ( newCell != null )
                        cell = newCell;
                }
                Component choice;
                if (cell.view().isPresent()) {
                    choice = cell.view().orElseThrow();
                    _lastCustomRenderer = choice;
                } else if (cell.presentationEntry().isPresent()) {
                    choice = _defaultRenderer.getListCellRendererComponent(list, cell.presentationEntry().get(), row, isSelected, hasFocus);
                    _lastCustomRenderer = null;
                } else {
                    choice = _defaultRenderer.getListCellRendererComponent(list, value, row, isSelected, hasFocus);
                    _lastCustomRenderer = null;
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
                null, comboBox, null, false, false, true, false, false, 0, 0, () -> null
            );
            List<Configurator<CellConf<C, ?>>> interpreter = _findAll(_rendererLookup);
            if (interpreter.isEmpty())
                return Optional.empty();
            else {
                for ( Configurator<CellConf<C,?>> configurator : interpreter ) {
                    CellConf<JComboBox<?>,Object> newCell = configurator.configure((CellConf)cell);
                    if ( newCell != null )
                        cell = newCell.view( v -> v.update( c -> c instanceof JTextField ? c : null ) );
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
        Map<Class<?>, List<Configurator<CellConf<C, ?>>>> rendererLookup
    ) {
        Class<?> type = (value == null ? Object.class : value.getClass());
        List<Configurator<CellConf<C, ?>>> cellRenderer = new ArrayList<>();
        for (Map.Entry<Class<?>, List<Configurator<CellConf<C, ?>>>> e : rendererLookup.entrySet()) {
            if (e.getKey().isAssignableFrom(type))
                cellRenderer.addAll(e.getValue());
        }
        // We reverse the cell renderers, so that the most un-specific one is first
        Collections.reverse(cellRenderer);
        return cellRenderer;
    }

    private static <C extends JComponent> List<Configurator<CellConf<C,?>>> _findAll(
            Map<Class<?>, List<Configurator<CellConf<C, ?>>>> rendererLookup
    ) {
        List<Configurator<CellConf<C, ?>>> cellRenderer = new ArrayList<>();
        for (List<Configurator<CellConf<C, ?>>> e : rendererLookup.values()) {
            cellRenderer.addAll(e);
        }
        // We reverse the cell renderers, so that the most un-specific one is first
        Collections.reverse(cellRenderer);
        return cellRenderer;
    }

    SimpleTableCellRenderer getForTable() {
        _addDefaultRendering();
        if (JTable.class.isAssignableFrom(_componentType)) {
            SimpleTableCellRenderer renderer = new SimpleTableCellRenderer();
            return renderer;
        } else
            throw new IllegalArgumentException("Renderer was set up to be used for a JTable!");
    }

    TreeCellRenderer getForTree() {
        _addDefaultRendering();
        if (JTree.class.isAssignableFrom(_componentType))
            return new SimpleTableCellRenderer();
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
     * @return The new {@link ListCellRenderer} instance specific to the given list.
     */
    void buildForList( C list ) {
        _addDefaultRendering();
        if (JList.class.isAssignableFrom(_componentType)) {
            SimpleListCellRenderer<C> renderer = new SimpleListCellRenderer<>(list);
            JList<E> jList = (JList<E>) list;
            jList.setCellRenderer(renderer);
        } else if (JComboBox.class.isAssignableFrom(_componentType))
            throw new IllegalArgumentException(
                    "Renderer was set up to be used for a JList! (not " + _componentType.getSimpleName() + ")"
            );
        else
            throw new IllegalArgumentException(
                    "Renderer was set up to be used for an unknown component type! (cannot handle '" + _componentType.getSimpleName() + "')"
            );
    }

    void buildForCombo(C comboBox, boolean establishEditorAlso) {
        _addDefaultRendering();
        if (JComboBox.class.isAssignableFrom(_componentType)) {
            SimpleListCellRenderer<C> renderer = new SimpleListCellRenderer<>(comboBox);
            JComboBox<E> combo = (JComboBox<E>) comboBox;
            combo.setRenderer(renderer);
            if (establishEditorAlso) {
                renderer.establishEditor().ifPresent(combo::setEditor);
            }
        } else
            throw new IllegalArgumentException(
                    "Renderer was set up to be used for a JComboBox! (not " + _componentType.getSimpleName() + ")"
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
        return cell -> {
            if ( cell.isEditing() )
                return cell;

            Component existing = cell.view().orElseNullable(null);
            InternalLabelForRendering l = (existing instanceof InternalLabelForRendering) ? (InternalLabelForRendering) existing : null;
            if ( existing != null && l == null )
                return cell; // The user has defined a custom renderer, so we don't touch it.

            if ( l == null )
                l = new InternalLabelForRendering(renderer.apply(cell));
            else
                l.setText(renderer.apply(cell));

            Color bg = null;
            Color fg = null;

            if ( cell.getHost() instanceof JList ) {
                JList<?> jList = (JList<?>) cell.getHost();
                bg = jList.getSelectionBackground();
                fg = jList.getSelectionForeground();
                if ( bg == null )
                    bg = UIManager.getColor("List.selectionBackground");
                if ( fg == null )
                    fg = UIManager.getColor("List.selectionForeground");
            }

            if ( cell.getHost() instanceof JTable ) {
                JTable jTable = (JTable) cell.getHost();
                bg = jTable.getSelectionBackground();
                fg = jTable.getSelectionForeground();
                if ( bg == null )
                    bg = UIManager.getColor("Table.selectionBackground");
                if ( fg == null )
                    fg = UIManager.getColor("Table.selectionForeground");
            }

            if ( bg == null && cell.getHost() != null )
                bg = cell.getHost().getBackground();
            if ( fg == null && cell.getHost() != null )
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
                if ( bg != null ) l.setBackground(bg);
                if ( fg != null ) l.setForeground(fg);
            }
            else {
                Color normalBg = Color.WHITE;
                if (  cell.getHost() != null )
                    normalBg = cell.getHost().getBackground();

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
                if ( bg != null ) l.setBackground( normalBg );
                if ( fg != null && cell.getHost() != null )
                    l.setForeground( cell.getHost().getForeground() );
            }

            // TODO:
            //l.setEnabled(cell.getComponent().isEnabled());
            //l.setFont(cell.getComponent().getFont());

            Border border = null;
            if ( cell.hasFocus() ) {
                if ( cell.isSelected() )
                    border = UIManager.getBorder( "List.focusSelectedCellHighlightBorder" );

                if ( border == null )
                    border = UIManager.getBorder( "List.focusCellHighlightBorder" );
            }
            else
                border = UIManager.getBorder( "List.cellNoFocusBorder" );

            if ( border != null ) l.setBorder(border);

            return cell.view(l);
        };
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
