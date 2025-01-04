package swingtree;

import swingtree.layout.Position;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.event.MouseEvent;

/**
 *  A {@link JComponent} as well as {@link MouseEvent} delegate
 *  providing useful context information to various {@link sprouts.Action} listeners which are
 *  typically registered through
 *  {@link UIForAnySwing#onMouseClick(sprouts.Action)}},
 *  {@link UIForAnySwing#onMousePress(sprouts.Action)} and
 *  {@link UIForAnySwing#onMouseRelease(sprouts.Action)}, among others.
 *  <br>
 *  This delegate is designed to provide clutter-free access to both the
 *  concrete {@link JComponent} subtype {@code C} as well a the {@link MouseEvent}
 *  which triggered the action.
 *  <p>
 *  Here is an example of how this delegate is typically exposed in your {@link sprouts.Action}:
 *  <pre>{@code
 *  UI.panel()
 *  .onMouseMove( it -> {
 *    System.out.println("Moved on " + it.get());
 *    System.out.println("Moved at " + it.mouseX() + ", " + it.mouseY());
 *  })
 *  .onMouseClick( it -> {
 *    System.out.println("Clicked on " + it.get());
 *    System.out.println("Click count: " + it.clickCount());
 *  })
 *  //...
 *  }</pre>
 *  <p>
 *  For some more examples <b>please take a look at the
 *  <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a></b>.
 *
 * @param <C> The type of {@link JComponent} that this {@link ComponentMouseEventDelegate} is delegating to.
 */
public class ComponentMouseEventDelegate<C extends JComponent> extends ComponentDelegate<C, MouseEvent>
{
    ComponentMouseEventDelegate(
        C component,
        MouseEvent event
    ) {
        super(component, event);
    }

    /**
     * Returns true if the mouse event of this delegate is the left mouse button.
     *
     * @return true if the left mouse button was active
     */
    public final boolean isLeftMouseButton() {
        return SwingUtilities.isLeftMouseButton(getEvent());
    }

    /**
     * Returns true if the mouse event of this delegate is the right mouse button.
     *
     * @return true if the right mouse button was active
     */
    public final boolean isRightMouseButton() {
        return SwingUtilities.isRightMouseButton(getEvent());
    }

    /**
     * Returns true if the mouse event of this delegate is the middle mouse button.
     *
     * @return true if the middle mouse button was active
     */
    public final boolean isMiddleMouseButton() {
        return SwingUtilities.isMiddleMouseButton(getEvent());
    }

    /**
     * Returns whether the Alt modifier is down on the event of this delegate.
     *
     * @return true if the alt modifier is down
     */
    public final boolean isAltDown() {
        return getEvent().isAltDown();
    }

    /**
     * Returns whether the Control modifier is down on the event of this delegate.
     *
     * @return true if the control modifier is down
     */
    public final boolean isCtrlDown() {
        return getEvent().isControlDown();
    }

    /**
     * Returns whether the Shift modifier is down on the event of this delegate.
     *
     * @return true if the shift modifier is down
     */
    public final boolean isShiftDown() {
        return getEvent().isShiftDown();
    }

    /**
     * Returns whether the Meta modifier is down on the event of this delegate.
     *
     * @return true if the meta modifier is down
     */
    public final boolean isMetaDown() {
        return getEvent().isMetaDown();
    }

    /**
     * Returns the number of mouse clicks associated with the event of this delegate.
     * You can use the returned number to distinguish between single-click and double-click events.
     *
     * @return An integer indicating the number of mouse clicks associated with the mouse event.
     */
    public final int clickCount() {
        return getEvent().getClickCount();
    }

    /**
     * Returns the x coordinate of the mouse event of this delegate.
     *
     * @return integer value for the x coordinate
     */
    public final int mouseX() {
        return getEvent().getX();
    }

    /**
     * Returns the y coordinate of the mouse event of this delegate.
     *
     * @return integer value for the y coordinate
     */
    public final int mouseY() {
        return getEvent().getY();
    }

    /**
     * Returns the position of the mouse event of this delegate
     * in the form of an immutable {@link Position} value object.
     * So you may safely share this object without
     * having to worry about side effects.
     *
     * @return the position of the mouse event
     */
    public final Position mousePosition() {
        return Position.of(getEvent().getPoint());
    }

    /**
     * Returns the absolute horizontal x position of the event of this delegate.
     * In a virtual device multi-screen environment in which the
     * desktop area could span multiple physical screen devices,
     * this coordinate is relative to the virtual coordinate system.
     * Otherwise, this coordinate is relative to the coordinate system
     * associated with the Component's GraphicsConfiguration.
     *
     * @return x  an integer indicating absolute horizontal position.
     */
    public final int mouseXOnScreen() {
        return getEvent().getXOnScreen();
    }

    /**
     * Returns the absolute vertical y position of the event of this delegate.
     * In a virtual device multiscreen environment in which the
     * desktop area could span multiple physical screen devices,
     * this coordinate is relative to the virtual coordinate system.
     * Otherwise, this coordinate is relative to the coordinate system
     * associated with the Component's GraphicsConfiguration.
     *
     * @return y  an integer indicating absolute vertical position.
     */
    public final int mouseYOnScreen() {
        return getEvent().getYOnScreen();
    }

    /**
     * Returns the absolute x and y position of the event of this delegate
     * in the form of an immutable {@link Position} value object.
     * So you may safely share this object without
     * having to worry about side effects.
     *
     * @return the absolute position of the mouse event
     */
    public final Position mousePositionOnScreen() {
        return Position.of(getEvent().getLocationOnScreen());
    }

}
