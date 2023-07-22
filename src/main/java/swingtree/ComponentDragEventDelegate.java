package swingtree;

import sprouts.Action;

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
 */
public final class ComponentDragEventDelegate<C extends JComponent> extends ComponentMouseEventDelegate<C>
{
    private final List<MouseEvent> _dragEventHistory = new java.util.ArrayList<>();

    ComponentDragEventDelegate(
            C component,
            MouseEvent event,
            Supplier<List<JComponent>> siblingSource,
            List<MouseEvent> dragEventHistory
    ) {
        super(component, event, siblingSource);
        _dragEventHistory.addAll(dragEventHistory);
    }

    /**
     * @return A list of all {@link MouseEvent}s of a continuous mouse drag performed on the component.
     */
    public List<MouseEvent> dragEvents() {
        return Collections.unmodifiableList(_dragEventHistory);
    }

    /**
     * @return A list of all mouse position {@link Point}s of a continuous mouse drag performed on the component.
     *         The points of this list represent the mouse movement track since the start of a continuous drag.
     */
    public List<Point> dragPositions() {
        return Collections.unmodifiableList(_dragEventHistory.stream().map(MouseEvent::getPoint).collect(Collectors.toList()));
    }

}
