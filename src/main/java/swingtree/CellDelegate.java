package swingtree;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

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
    private final C                   owner;
    private final @Nullable V         value;
    private final boolean             isSelected;
    private final boolean             hasFocus;
    private final int                 row;
    private final int                 column;
    private final @Nullable Component componentRef;
    private final List<String>        toolTips;
    private final @Nullable V         defaultValueRef;


    public CellDelegate(
        C                   owner,
        @Nullable V         value,
        boolean             isSelected,
        boolean             hasFocus,
        int                 row,
        int                 column,
        @Nullable Component renderer,
        List<String>        toolTips,
        @Nullable V         defaultValue
    ) {
        this.owner           = Objects.requireNonNull(owner);
        this.value           = value;
        this.isSelected      = isSelected;
        this.hasFocus        = hasFocus;
        this.row             = row;
        this.column          = column;
        this.componentRef    = renderer;
        this.toolTips        = Objects.requireNonNull(toolTips);
        this.defaultValueRef = defaultValue;
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

    public Optional<Component> renderer() {
        return Optional.ofNullable(componentRef);
    }

    public CellDelegate<C, V> withRenderer(Component component) {
        return new CellDelegate<>(
            owner,
            value,
            isSelected,
            hasFocus,
            row,
            column,
            component,
            toolTips,
            defaultValueRef
        );
    }

    public CellDelegate<C, V> withRenderer( Consumer<Graphics2D> painter ) {
        return withRenderer(new Component() {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                painter.accept((Graphics2D) g);
            }
        });
    }

    public CellDelegate<C, V> withToolTip( String toolTip ) {
        ArrayList<String> newToolTips = new ArrayList<>(toolTips);
        newToolTips.add(toolTip);
        return new CellDelegate<>(
            owner,
            value,
            isSelected,
            hasFocus,
            row,
            column,
            componentRef,
            newToolTips,
            defaultValueRef
        );
    }

    public Optional<V> defaultValue() {
        return Optional.ofNullable(defaultValueRef);
    }

    public CellDelegate<C, V> withDefaultRenderValue( V newValue ) {
        return new CellDelegate<>(
            owner,
            value,
            isSelected,
            hasFocus,
            row,
            column,
            componentRef,
            toolTips,
            newValue
        );
    }
}
