package swingtree;

import sprouts.Action;
import swingtree.layout.Position;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;


/**
 *  A {@link JComponent} and {@link MouseEvent} delegate providing useful context information to various {@link sprouts.Action} listeners
 *  used by {@link UIForAnySwing#onMouseDrag(Action)} like for example the {@link #mouseX()} and
 *  {@link #mouseY()} of the event as well as more drag specific information like
 *  {@link #dragEvents()} and {@link #dragPositions()}.
 *
 * @param <C> The type of {@link JComponent} that this {@link ComponentDragEventDelegate} is delegating to.
 */
public final class ComponentDragEventDelegate<C extends JComponent> extends ComponentMouseEventDelegate<C>
{
    private final List<MouseEvent> _dragEventHistory = new java.util.ArrayList<>();


    ComponentDragEventDelegate(
            C component,
            MouseEvent event,
            List<MouseEvent> dragEventHistory
    ) {
        super(component, event);
        _dragEventHistory.addAll(dragEventHistory);
    }

    /**
     *  Provides a list of all {@link MouseEvent}s of a continuous mouse drag performed on the component.
     *  When a drag ends, the list is cleared.
     *
     * @return A list of all {@link MouseEvent}s of a continuous mouse drag performed on the component.
     */
    public List<MouseEvent> dragEvents() {
        return Collections.unmodifiableList(_dragEventHistory);
    }

    /**
     *  SwingTree keeps track of the most recent mouse drag events of a continuous drag.
     *  This method returns a list of all mouse {@link Position}s of a continuous mouse drag
     *  performed on the component. <br>
     *  Note that this mehod returns an unmodifiable list consisting
     *  of immutable {@link Position} objects instead of mutable {@link Point} objects,
     *  to protect the client from side effects.
     *
     * @return A list of all mouse {@link Position}s of a continuous mouse drag performed on the component.
     *         The points of this list represent the mouse movement track since the start of a continuous drag.
     */
    public List<Position> dragPositions() {
        return Collections.unmodifiableList(
                _dragEventHistory.stream()
                                .map(MouseEvent::getPoint)
                                .map(Position::of)
                                .collect(Collectors.toList())
                            );
    }

    /**
     * Provides the x-axis movement delta of the mouse since the start of a continuous drag.
     * @return The x-axis movement delta of the mouse since the start of a continuous drag.
     */
    public int deltaX() {
        if (_dragEventHistory.size() < 2) return 0;
        return _dragEventHistory.get(_dragEventHistory.size() - 1).getX() - _dragEventHistory.get(0).getX();
    }

    /**
     * Provides the y-axis movement delta of the mouse since the start of a continuous drag.
     * @return The y-axis movement delta of the mouse since the start of a continuous drag.
     */
    public int deltaY() {
        if (_dragEventHistory.size() < 2) return 0;
        return _dragEventHistory.get(_dragEventHistory.size() - 1).getY() - _dragEventHistory.get(0).getY();
    }

}
