package swingtree;

import sprouts.Action;
import sprouts.Tuple;
import swingtree.layout.Position;

import javax.swing.JComponent;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.List;


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
    private final Tuple<MouseEvent> _dragEventHistory;


    ComponentDragEventDelegate(
        C component,
        MouseEvent event,
        List<MouseEvent> dragEventHistory
    ) {
        super(component, event);
        _dragEventHistory = Tuple.of(MouseEvent.class, dragEventHistory);
    }

    /**
     *  Provides a {@link Tuple} (immutable list) of all {@link MouseEvent}s of a
     *  continuous mouse drag performed on the component.
     *  When a drag ends, the tuple is empty.
     *
     * @return A tuple of all {@link MouseEvent}s of a continuous mouse drag performed on the component.
     */
    public Tuple<MouseEvent> dragEvents() {
        return _dragEventHistory;
    }

    /**
     *  SwingTree keeps track of the most recent mouse drag events of a continuous drag.
     *  This method returns a {@link Tuple} (immutable list) of all mouse {@link Position}s
     *  of a continuous mouse drag performed on the component. <br>
     *  Note that these points are scaled to "developer pixel" instead of the actual "UI scaled component space"
     *  of the underlying Swing component.<br>
     *  Use {@link UI#scale(int)} to convert these points back to the actual "UI scaled component space".
     *  Also note that this method returns an unmodifiable list consisting
     *  of immutable {@link Position} objects instead of mutable {@link Point} objects,
     *  to protect the client from side effects.
     *
     * @return A tuple (immutable list) of all mouse {@link Position}s of a continuous mouse drag performed on the component.
     *         The points of this list represent the mouse movement track since the start of a continuous drag.
     */
    public Tuple<Position> dragPositions() {
        return _dragEventHistory.stream()
                                .map(it->Position.of(UI.unscale(it.getX()), UI.unscale(it.getY())))
                                .collect(Tuple.collectorOf(Position.class));
    }

    /**
     * Provides the x-axis movement delta of the mouse since the start of a continuous drag <b>without DPI scaling</b>.
     * So the value is in "developer pixel" not in <b>UI scaled component space</b>.
     * This means that when you need to interface with the underlying Swing API then you may
     * want to consider upscaling it again using {@link UI#scale(float)}.
     *
     * @return The x-axis movement delta of the mouse since the start of a continuous drag.
     *         This value is in "developer pixel" not in <b>UI scaled component space</b>.
     */
    public float deltaXSinceStart() {
        if (_dragEventHistory.size() < 2) return 0;
        float xInComponentPixel = _dragEventHistory.get(_dragEventHistory.size() - 1).getX() - _dragEventHistory.get(0).getX();
        return UI.unscale(xInComponentPixel);
    }

    /**
     * Provides the y-axis movement delta of the mouse since the start of a continuous drag <b>without DPI scaling</b>.
     * So the value is in "developer pixel" not in <b>UI scaled component space</b>.
     * This means that when you need to interface with the underlying Swing API then you may
     * want to consider upscaling it again using {@link UI#scale(float)}.
     * @return The y-axis movement delta of the mouse since the start of a continuous drag.
     *         This value is in "developer pixel" not in <b>UI scaled component space</b>.
     */
    public float deltaYSinceStart() {
        if (_dragEventHistory.size() < 2) return 0;
        float yInComponentPixel = _dragEventHistory.get(_dragEventHistory.size() - 1).getY() - _dragEventHistory.get(0).getY();
        return UI.unscale(yInComponentPixel);
    }

}
