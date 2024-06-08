package swingtree;

import javax.swing.JComponent;
import java.awt.Component;
import java.util.function.Consumer;

/**
 * An interface for interpreting the value of a {@link CellDelegate} and
 * setting a {@link Component} or custom renderer which is then used to render the cell.
 * Inside your interpreter, use {@link CellDelegate#setRenderer(Component)} or {@link CellDelegate#setRenderer(Consumer)}
 * to define how the cell should be rendered.
 *
 * @param <C> The type of the component which is used to render the cell.
 * @param <V> The type of the value of the cell.
 */
@FunctionalInterface
public
interface CellInterpreter<C extends JComponent, V> {

    /**
     * Interprets the value of a {@link CellDelegate} and produces a {@link Component}
     * which is then used to render the cell.
     *
     * @param cell The cell which is to be rendered.
     */
    void interpret(CellDelegate<C, V> cell);
}
