package swingtree;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;
import java.util.Objects;
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
    private final @Nullable V value;
    private final boolean isSelected;
    private final boolean hasFocus;
    private final int row;
    private final int column;
    private final Component[] componentRef;
    private final List<String> toolTips;
    private final V[] defaultValueRef;
    private final Function<CellDelegate<C, V>, Component> defaultRenderer;

    public CellDelegate(
        C owner,
        @Nullable V value,
        boolean isSelected,
        boolean hasFocus,
        int row,
        int column,
        Component[] componentRef,
        List<String> toolTips,
        V[] defaultValueRef,
        Function<CellDelegate<C, V>, Component> defaultRenderer
    ) {
        this.owner           = Objects.requireNonNull(owner);
        this.value           = value;
        this.isSelected      = isSelected;
        this.hasFocus        = hasFocus;
        this.row             = row;
        this.column          = column;
        this.componentRef    = Objects.requireNonNull(componentRef);
        this.toolTips        = Objects.requireNonNull(toolTips);
        this.defaultValueRef = Objects.requireNonNull(defaultValueRef);
        this.defaultRenderer = Objects.requireNonNull(defaultRenderer);
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

    public Optional<Component> getRenderer() {
        return Optional.ofNullable(componentRef[0]);
    }

    public CellDelegate<C, V> setRenderer(Component component) {
        componentRef[0] = component;
        return this;
    }

    public CellDelegate<C, V> setRenderer( Consumer<Graphics2D> painter ) {
        return setRenderer(new Component() {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                painter.accept((Graphics2D) g);
            }
        });
    }

    public CellDelegate<C, V> setToolTip(String toolTip) {
        toolTips.add(toolTip);
        return this;
    }

    public Optional<V> defaultValue() {
        return Optional.ofNullable(defaultValueRef[0]);
    }

    public CellDelegate<C, V> setDefaultRenderValue(V newValue) {
        defaultValueRef[0] = newValue;
        return this;
    }
}
