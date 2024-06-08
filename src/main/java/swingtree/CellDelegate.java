package swingtree;

import javax.swing.JComponent;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This class models the state of an individual table/list/drop down cell alongside
 * various properties that a cell should have, like for example
 * the value of the cell, its position within the table
 * as well as a renderer in the form of a AWT {@link Component}
 * which may or not be replaced or modified.
 *
 * @param <V> The value type of the entry of this {@link CellDelegate}.
 */
public class CellDelegate<C extends JComponent, V>
{
    private final C owner;
    private final V value;
    private final boolean isSelected;
    private final boolean hasFocus;
    private final int row;
    private final int column;
    private final Component[] componentRef;
    private final List<String> toolTips;
    private final Object[] defaultValueRef;
    private final Function<CellDelegate<C, V>, Component> defaultRenderer;

    public CellDelegate(
        C owner,
        V value,
        boolean isSelected,
        boolean hasFocus,
        int row,
        int column,
        Component[] componentRef,
        List<String> toolTips,
        Object[] defaultValueRef,
        Function<CellDelegate<C, V>, Component> defaultRenderer
    ) {
        this.owner = owner;
        this.value = value;
        this.isSelected = isSelected;
        this.hasFocus = hasFocus;
        this.row = row;
        this.column = column;
        this.componentRef = componentRef;
        this.toolTips = toolTips;
        this.defaultValueRef = defaultValueRef;
        this.defaultRenderer = defaultRenderer;
    }

    public C getComponent() {
        return owner;
    }

    public Optional<V> value() {
        return Optional.ofNullable(value);
    }

    public Optional<String> valueAsString() {
        return value().map(Object::toString);
    }

    public boolean isSelected() {
        return isSelected;
    }

    public boolean hasFocus() {
        return hasFocus;
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }

    public Component getRenderer() {
        return defaultRenderer.apply(this);
    }

    public void setRenderer(Component component) {
        componentRef[0] = component;
    }

    public void setRenderer(Consumer<Graphics2D> painter) {
        setRenderer(new Component() {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                painter.accept((Graphics2D) g);
            }
        });
    }

    public void setToolTip(String toolTip) {
        toolTips.add(toolTip);
    }

    public void setDefaultRenderValue(Object newValue) {
        defaultValueRef[0] = newValue;
    }
}
