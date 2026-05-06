package swingtree;

import sprouts.Action;
import sprouts.Pair;
import sprouts.Tuple;
import swingtree.layout.Position;

import javax.swing.JComponent;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.List;


/**
 *  A {@link JComponent} and {@link MouseEvent} delegate providing useful context information to
 *  {@link sprouts.Action} listeners registered through {@link UIForAnySwing#onMouseDrag(Action)},
 *  like for example the {@link #mouseX()} and {@link #mouseY()} of the current event as well as
 *  more drag specific information like {@link #dragEvents()}, {@link #dragPositions()},
 *  {@link #initialComponentPosition()} and the {@link #deltaXSinceStart()} /
 *  {@link #deltaYSinceStart()} accumulated since the drag began.
 *  <p>
 *  A common use case is to move the dragged component along with the mouse pointer
 *  by combining the component's start position with the accumulated drag delta:
 *  <pre>{@code
 *  UI.panel("fill")
 *  .onMouseDrag( e -> {
 *      e.setLocation(
 *          e.initialComponentPosition().x() + e.deltaXSinceStart(),
 *          e.initialComponentPosition().y() + e.deltaYSinceStart()
 *      );
 *      e.getParent().repaint();
 *  })
 *  }</pre>
 *  <p>
 *  All position and delta values exposed by this delegate are reported in
 *  unscaled "developer pixels" (see {@link UI#scale()}), so they can be passed
 *  directly to other SwingTree APIs without further conversion. When interfacing
 *  with the underlying Swing API, scale them back up using {@link UI#scale(int)}
 *  or {@link UI#scale(float)}.
 *
 * @param <C> The type of {@link JComponent} that this {@link ComponentDragEventDelegate} is delegating to.
 */
public final class ComponentDragEventDelegate<C extends JComponent> extends ComponentMouseEventDelegate<C>
{
    /*
        Important, we also track the position history of the dragged component relative
        to the parent component. This is extremely important in order for the drag coordinate
        history to stay correct even when the component is moved during the drag.
    */
    private final Tuple<Pair<Position, MouseEvent>> _dragEventHistory;


    ComponentDragEventDelegate(
        C component,
        MouseEvent event,
        List<Pair<Position, MouseEvent>> dragEventHistory
    ) {
        super(component, event);
        _dragEventHistory = Tuple.of(Pair.classTyped(Position.class, MouseEvent.class), dragEventHistory);
    }

    /**
     *  Returns the {@link Position} (relative to the parent container) that the dragged
     *  component had at the moment the user started the current continuous drag, i.e.
     *  when the mouse button was first pressed. The returned position remains constant
     *  for the entire duration of the drag, even if the component is moved in response
     *  to drag events. <br>
     *  Combined with {@link #deltaXSinceStart()} and {@link #deltaYSinceStart()}, this
     *  is the typical building block for repositioning a component as it is being
     *  dragged:
     *  <pre>{@code
     *  .onMouseDrag( e -> {
     *      Position start = e.initialComponentPosition();
     *      e.setLocation(
     *          start.x() + e.deltaXSinceStart(),
     *          start.y() + e.deltaYSinceStart()
     *      );
     *  })
     *  }</pre>
     *  Note that the returned position is in "developer pixels" rather than the actual
     *  "UI scaled component space" of the underlying Swing component. Use
     *  {@link UI#scale(int)} to convert it back to the scaled component space if you
     *  need to interface with the raw Swing API. <br>
     *  If for any reason no drag has been recorded yet (e.g. the history is empty),
     *  {@link Position#origin()} is returned as a sensible default.
     *
     * @return The unscaled {@link Position} of the component (relative to its parent)
     *         at the start of the current continuous drag, or {@link Position#origin()}
     *         if no drag has been recorded yet.
     */
    public Position initialComponentPosition() {
         Position scaledPosition = !_dragEventHistory.isEmpty() ? _dragEventHistory.get(0).first() : Position.origin();
         return Position.of(UI.unscale(scaledPosition.x()), UI.unscale(scaledPosition.y()));
    }

    /**
     *  Provides a {@link Tuple} (immutable list) of all {@link MouseEvent}s of a
     *  continuous mouse drag performed on the component.
     *  When a drag ends, the tuple is empty.
     *
     * @return A tuple of all {@link MouseEvent}s of a continuous mouse drag performed on the component.
     */
    public Tuple<MouseEvent> dragEvents() {
        return _dragEventHistory.mapTo(MouseEvent.class, Pair::second);
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
        Position firstPosition = !_dragEventHistory.isEmpty() ? _dragEventHistory.get(0).first() : Position.origin();
        return _dragEventHistory.stream()
                                .map(it->{
                                    Position positionAtEvent = it.first();
                                    Position correction = positionAtEvent.minus(firstPosition);
                                    MouseEvent event = it.second();
                                    return Position.of(
                                            UI.unscale(event.getX()) + UI.unscale(correction.x()),
                                            UI.unscale(event.getY()) + UI.unscale(correction.y())
                                    );
                                })
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
        Tuple<Position> positions = dragPositions();
        return positions.get(positions.size() - 1).x() - positions.get(0).x();
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
        Tuple<Position> positions = dragPositions();
        return positions.get(positions.size() - 1).y() - positions.get(0).y();
    }

}
