package swingtree;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * This class models the state of an individual table/list/drop down cell alongside
 * various properties that a cell should have, like for example
 * the value of the cell, its position within the table
 * as well as a renderer in the form of a AWT {@link Component}
 * which may or not be replaced or modified.
 * <br>
 * When configuring your cell, you may use methods like
 * {@link CellDelegate#withRenderer(Component)} or {@link CellDelegate#withRenderer(Consumer)}
 * to define how the cell should be rendered.
 *
 * @param <V> The value type of the entry of this {@link CellDelegate}.
 */
public final class CellDelegate<C extends JComponent, V>
{
    private static final Logger log = LoggerFactory.getLogger(CellDelegate.class);

    public static <C extends JComponent, V> CellDelegate<C, V> of(
        C                   owner,
        @Nullable V         value,
        boolean             isSelected,
        boolean             hasFocus,
        int                 row,
        int                 column,
        Supplier<Component> defaultRenderSource
    ) {
        List<String> toolTips = new ArrayList<>();
        return new CellDelegate<>(
            owner,
            value,
            isSelected,
            hasFocus,
            row,
            column,
            null,
            toolTips,
            null,
            defaultRenderSource
        );
    }

    private final C                   owner;
    private final @Nullable V         value;
    private final boolean             isSelected;
    private final boolean             hasFocus;
    private final int                 row;
    private final int                 column;
    private final @Nullable Component cellRenderer;
    private final List<String>        toolTips;
    private final @Nullable Object    presentationValue;
    private final Supplier<Component> defaultRenderSource;


    private CellDelegate(
        C                   owner,
        @Nullable V         value,
        boolean             isSelected,
        boolean             hasFocus,
        int                 row,
        int                 column,
        @Nullable Component renderer,
        List<String>        toolTips,
        @Nullable Object    presentationValue,
        Supplier<Component> defaultRenderSource
    ) {
        this.owner               = Objects.requireNonNull(owner);
        this.value               = value;
        this.isSelected          = isSelected;
        this.hasFocus            = hasFocus;
        this.row                 = row;
        this.column              = column;
        this.cellRenderer        = renderer;
        this.toolTips            = Objects.requireNonNull(toolTips);
        this.presentationValue   = presentationValue;
        this.defaultRenderSource = Objects.requireNonNull(defaultRenderSource);
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

    public List<String> toolTips() {
        return Collections.unmodifiableList(toolTips);
    }

    public int row() {
        return row;
    }

    public int column() {
        return column;
    }

    public Optional<Component> renderer() {
        return Optional.ofNullable(cellRenderer);
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
            presentationValue,
            defaultRenderSource
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
                cellRenderer,
            newToolTips,
            presentationValue,
            defaultRenderSource
        );
    }

    /**
     *  The presentation value is the first choice of the
     *  cell renderer to be used for rendering and presentation
     *  to the user. If it does not exist then the regular
     *  cell value is used for rendering.
     *
     * @return An optional of the presentation value.
     *         It may be an empty optional if no presentation value was specified.
     */
    public Optional<Object> presentationValue() {
        return Optional.ofNullable(presentationValue);
    }

    /**
     *  Represents the value how it should be displayed
     *  to the user by the cell renderer. By default, this
     *  value is null, in which case the regular cell value is
     *  presented to the user.
     *
     * @param toBeShown The object which should be used by the renderer
     *                  to present to the user, typically a String.
     * @return An updated cell delegate object with the new presentation value.
     */
    public CellDelegate<C, V> withPresentationValue( @Nullable Object toBeShown ) {
        return new CellDelegate<>(
            owner,
            value,
            isSelected,
            hasFocus,
            row,
            column,
                cellRenderer,
            toolTips,
            toBeShown,
            defaultRenderSource
        );
    }

    public CellDelegate<C, V> withDefaultRenderer() {
        try {
            return this.withRenderer(this.defaultRenderSource.get());
        } catch (Exception e) {
            log.error("Failed to create default renderer!", e);
        }
        return this;
    }
}
