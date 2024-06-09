package swingtree;

import swingtree.api.CellInterpreter;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A builder for building simple customized {@link javax.swing.table.TableCellRenderer}!
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a collection of examples demonstrating how to use the API of this class.</b>
 *
 * @param <C> The type of the component which is used to render the cell.
 * @param <E> The type of the value of the cell.
 */
public class RenderBuilder<C extends JComponent, E> {

    private final Class<C> _componentType;
    private final Map<Class<?>, List<Consumer<CellDelegate<C, ?>>>> _rendererLookup = new LinkedHashMap<>(16);


    static <E> RenderBuilder<JList<E>,E> forList(Class<E> elementType) {
        return (RenderBuilder) new RenderBuilder<>(JList.class);
    }
    static <C extends JComboBox<E>, E> RenderBuilder<C,E> forCombo(Class<E> elementType) {
        return (RenderBuilder) new RenderBuilder<>(JComboBox.class);
    }
    static <E> RenderBuilder<JTable,E> forTable(Class<E> elementType) {
        return (RenderBuilder) new RenderBuilder<>(JTable.class);
    }


    private RenderBuilder(Class<C> componentType) {
        _componentType = componentType;
    }

    /**
     * Use this to specify which type of values should have custom rendering.
     * The object returned by this method allows you to specify how to render the values.
     *
     * @param valueType The type of cell value, for which you want custom rendering.
     * @param <T>       The type parameter of the cell value, for which you want custom rendering.
     * @return The {@link Render.As} builder API step which expects you to provide a lambda for customizing how a cell is rendered.
     */
    public <T extends E> Render.As<C, E, T> when(Class<T> valueType) {
        NullUtil.nullArgCheck(valueType, "valueType", Class.class);
        return when(valueType, cell -> true);
    }

    public <T extends E> Render.As<C, E, T> when(
            Class<T> valueType,
            Predicate<CellDelegate<C, T>> valueValidator
    ) {
        NullUtil.nullArgCheck(valueType, "valueType", Class.class);
        NullUtil.nullArgCheck(valueValidator, "valueValidator", Predicate.class);
        return new Render.As<C, E, T>() {
            @Override
            public RenderBuilder<C, E> as(CellInterpreter<C, T> valueInterpreter) {
                NullUtil.nullArgCheck(valueInterpreter, "valueInterpreter", CellInterpreter.class);
                _store(valueType, valueValidator, valueInterpreter);
                return RenderBuilder.this;
            }
        };
    }

    private void _store(
            Class valueType,
            Predicate predicate,
            CellInterpreter valueInterpreter
    ) {
        NullUtil.nullArgCheck(valueType, "valueType", Class.class);
        NullUtil.nullArgCheck(predicate, "predicate", Predicate.class);
        NullUtil.nullArgCheck(valueInterpreter, "valueInterpreter", CellInterpreter.class);
        List<Consumer<CellDelegate<C, ?>>> found = _rendererLookup.computeIfAbsent(valueType, k -> new ArrayList<>());
        found.add(cell -> {
            if (predicate.test(cell))
                valueInterpreter.interpret(cell);
        });
    }

    private class SimpleTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                final int row,
                int column
        ) {
            List<Consumer<CellDelegate<C, ?>>> interpreter = _find(value, _rendererLookup);
            if (interpreter.isEmpty())
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            else {
                Component[] componentRef = new Component[1];
                Object[] defaultValueRef = new Object[1];
                List<String> toolTips = new ArrayList<>();
                CellDelegate<JTable, Object> cell = new CellDelegate<>(
                        table, value, isSelected, hasFocus, row, column,
                        componentRef, toolTips, defaultValueRef,
                        c -> SimpleTableCellRenderer.super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                );

                interpreter.forEach(consumer -> consumer.accept((CellDelegate<C, ?>) cell));
                Component choice;
                if (componentRef[0] != null)
                    choice = componentRef[0];
                else if (defaultValueRef[0] != null)
                    choice = super.getTableCellRendererComponent(table, defaultValueRef[0], isSelected, hasFocus, row, column);
                else
                    choice = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!toolTips.isEmpty() && choice instanceof JComponent)
                    ((JComponent) choice).setToolTipText(String.join("; ", toolTips));

                return choice;
            }
        }

    }

    private class SimpleListCellRenderer<O extends C> extends DefaultListCellRenderer {
        private final O _component;

        private SimpleListCellRenderer(O component) {
            _component = Objects.requireNonNull(component);
        }

        @Override
        public Component getListCellRendererComponent(
                JList list,
                Object value,
                final int row,
                boolean isSelected,
                boolean hasFocus
        ) {
            List<Consumer<CellDelegate<C, ?>>> interpreter = _find(value, _rendererLookup);
            if (interpreter.isEmpty())
                return super.getListCellRendererComponent(list, value, row, isSelected, hasFocus);
            else {
                Component[] componentRef = new Component[1];
                Object[] defaultValueRef = new Object[1];
                List<String> toolTips = new ArrayList<>();
                CellDelegate<O, Object> cell = new CellDelegate<>(
                        _component, value, isSelected, hasFocus, row,
                        0, componentRef, toolTips, defaultValueRef,
                        c -> super.getListCellRendererComponent(list, value, row, isSelected, hasFocus)
                );

                interpreter.forEach(consumer -> consumer.accept((CellDelegate<C, ?>) cell));
                Component choice;
                if (componentRef[0] != null)
                    choice = componentRef[0];
                else if (defaultValueRef[0] != null)
                    choice = super.getListCellRendererComponent(list, defaultValueRef[0], row, isSelected, hasFocus);
                else
                    choice = super.getListCellRendererComponent(list, value, row, isSelected, hasFocus);

                if (!toolTips.isEmpty() && choice instanceof JComponent)
                    ((JComponent) choice).setToolTipText(String.join("; ", toolTips));

                return choice;
            }
        }
    }

    private static <C extends JComponent> List<Consumer<CellDelegate<C, ?>>> _find(
            Object value,
            Map<Class<?>, List<Consumer<CellDelegate<C, ?>>>> rendererLookup
    ) {
        Class<?> type = (value == null ? Object.class : value.getClass());
        List<Consumer<CellDelegate<C, ?>>> cellRenderer = new ArrayList<>();
        for (Map.Entry<Class<?>, List<Consumer<CellDelegate<C, ?>>>> e : rendererLookup.entrySet()) {
            if (e.getKey().isAssignableFrom(type))
                cellRenderer.addAll(e.getValue());
        }
        // We reverse the cell renderers, so that the most specific one is first
        Collections.reverse(cellRenderer);
        return cellRenderer;
    }

    DefaultTableCellRenderer getForTable() {
        _addDefaultRendering();
        if (JTable.class.isAssignableFrom(_componentType))
            return new SimpleTableCellRenderer();
        else
            throw new IllegalArgumentException("Renderer was set up to be used for a JTable!");
    }

    /**
     * Like many things in the SwingTree library, this class is
     * essentially a convenient builder for a {@link ListCellRenderer}.
     * This internal method actually builds the {@link ListCellRenderer} instance,
     * see {@link UIForList#withRenderer(swingtree.api.Configurator)} for more details
     * about how to use this class as pat of the main API.
     *
     * @param list The list for which the renderer is to be built.
     * @return The new {@link ListCellRenderer} instance specific to the given list.
     */
    ListCellRenderer<E> buildForList(C list) {
        _addDefaultRendering();
        if (JList.class.isAssignableFrom(_componentType))
            return (ListCellRenderer<E>) new SimpleListCellRenderer<>(list);
        else if (JComboBox.class.isAssignableFrom(_componentType))
            throw new IllegalArgumentException(
                    "Renderer was set up to be used for a JList! (not " + _componentType.getSimpleName() + ")"
            );
        else
            throw new IllegalArgumentException(
                    "Renderer was set up to be used for an unknown component type! (cannot handle '" + _componentType.getSimpleName() + "')"
            );
    }

    /**
     * Like many things in the SwingTree library, this class is
     * essentially a convenient builder for a {@link ListCellRenderer}.
     * This internal method actually builds the {@link ListCellRenderer} instance,
     * see {@link UIForList#withRenderer(swingtree.api.Configurator)} for more details
     * about how to use this class as pat of the main API.
     *
     * @param comboBox The combo box for which the renderer is to be built.
     * @return The new {@link ListCellRenderer} instance specific to the given combo box.
     */
    ListCellRenderer<E> buildForCombo(C comboBox) {
        _addDefaultRendering();
        if (JComboBox.class.isAssignableFrom(_componentType))
            return (ListCellRenderer<E>) new SimpleListCellRenderer<>(comboBox);
        else
            throw new IllegalArgumentException(
                    "Renderer was set up to be used for a JComboBox! (not " + _componentType.getSimpleName() + ")"
            );
    }

    private void _addDefaultRendering() {
        // We use the default text renderer for objects
        _store(Object.class, cell -> true, _createDefaultTextRenderer(cell -> cell.valueAsString().orElse("")));
    }


    static <C extends JComponent, V> CellInterpreter<C,V> _createDefaultTextRenderer(
            Function<CellDelegate<C, V>, String> renderer
    ) {
        return cell -> {
            JLabel l = new JLabel(renderer.apply(cell));
            l.setOpaque(true);

            Color bg = null;
            Color fg = null;

            if ( cell.getComponent() instanceof JList ) {
                JList<?> jList = (JList<?>) cell.getComponent();
                bg = jList.getSelectionBackground();
                fg = jList.getSelectionForeground();
                if ( bg == null )
                    bg = UIManager.getColor("List.selectionBackground");
                if ( fg == null )
                    fg = UIManager.getColor("List.selectionForeground");
            }

            if ( cell.getComponent() instanceof JTable ) {
                JTable jTable = (JTable) cell.getComponent();
                bg = jTable.getSelectionBackground();
                fg = jTable.getSelectionForeground();
                if ( bg == null )
                    bg = UIManager.getColor("Table.selectionBackground");
                if ( fg == null )
                    fg = UIManager.getColor("Table.selectionForeground");
            }

            if ( bg == null && cell.getComponent() != null )
                bg = cell.getComponent().getBackground();
            if ( fg == null && cell.getComponent() != null )
                fg = cell.getComponent().getForeground();

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
                if (  cell.getComponent() != null )
                    normalBg = cell.getComponent().getBackground();

                // We need to make sure the color is a user color, not a LaF color:
                if ( normalBg != null )
                    normalBg = new Color( normalBg.getRGB() ); // This is because of a weired JDK bug it seems!

                if ( cell.getRow() % 2 == 1 ) {
                    // We determine if the base color is more bright or dark,
                    // and then we set the foreground color accordingly
                    double brightness = (0.299 * normalBg.getRed() + 0.587 * normalBg.getGreen() + 0.114 * normalBg.getBlue()) / 255;
                    if ( brightness < 0.5 )
                        normalBg = brighter(normalBg);
                    else
                        normalBg = darker(normalBg);
                }
                if ( bg != null ) l.setBackground( normalBg );
                if ( fg != null && cell.getComponent() != null )
                    l.setForeground( cell.getComponent().getForeground() );
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

            cell.setRenderer(l);
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
