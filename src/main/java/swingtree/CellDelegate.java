package swingtree;

import javax.swing.JComponent;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * This interface models an individual table/list/drop down cell alongside
 * various properties that a cell should have, like for example
 * the value of the cell, its position within the table
 * as well as a renderer in the form of a AWT {@link Component}
 * which may or not be replaced or modified.
 *
 * @param <V> The value type of the entry of this {@link CellDelegate}.
 */
public interface CellDelegate<C extends JComponent, V>
{
    C getComponent();

    Optional<V> value();

    default Optional<String> valueAsString() {
        return value().map(Object::toString);
    }

    boolean isSelected();

    boolean hasFocus();

    int getRow();

    int getColumn();

    Component getRenderer();

    void setRenderer(Component component);

    void setToolTip(String toolTip);

    void setDefaultRenderValue(Object newValue);

    default void setRenderer(Consumer<Graphics2D> painter) {
        setRenderer(new Component() {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                painter.accept((Graphics2D) g);
            }
        });
    }

}
