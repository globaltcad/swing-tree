package swingtree;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Supplier;

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
 *  <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 */
public class ComponentMouseEventDelegate<C extends JComponent> extends ComponentDelegate<C, MouseEvent>
{
    public ComponentMouseEventDelegate(
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
    public boolean isLeftMouseButton() { return SwingUtilities.isLeftMouseButton(getEvent()); }

    /**
     * Returns true if the mouse event of this delegate is the right mouse button.
     *
     * @return true if the right mouse button was active
     */
    public boolean isRightMouseButton() { return SwingUtilities.isRightMouseButton(getEvent()); }

    /**
     * Returns true if the mouse event of this delegate is the middle mouse button.
     *
     * @return true if the middle mouse button was active
     */
    public boolean isMiddleMouseButton() { return SwingUtilities.isMiddleMouseButton(getEvent()); }

    /**
     * Returns whether the Alt modifier is down on the event of this delegate.
     *
     * @return true if the alt modifier is down
     */
    public boolean isAltDown() { return getEvent().isAltDown(); }

    /**
     * Returns whether the Control modifier is down on the event of this delegate.
     *
     * @return true if the control modifier is down
     */
    public boolean isCtrlDown() { return getEvent().isControlDown(); }

    /**
     * Returns whether the Shift modifier is down on the event of this delegate.
     *
     * @return true if the shift modifier is down
     */
    public boolean isShiftDown() { return getEvent().isShiftDown(); }

    /**
     * Returns whether the Meta modifier is down on the event of this delegate.
     *
     * @return true if the meta modifier is down
     */
    public boolean isMetaDown() { return getEvent().isMetaDown(); }

    /**
     * Returns the number of mouse clicks associated with the event of this delegate.
     * You can use the returned number to distinguish between single-click and double-click events.
     *
     * @return An integer indicating the number of mouse clicks associated with the mouse event.
     */
    public int clickCount() { return getEvent().getClickCount(); }

    /**
     * Returns the x coordinate of the mouse event of this delegate.
     *
     * @return integer value for the x coordinate
     */
    public int mouseX() { return getEvent().getX(); }

    /**
     * Returns the y coordinate of the mouse event of this delegate.
     *
     * @return integer value for the y coordinate
     */
    public int mouseY() { return getEvent().getY(); }

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
    public int mouseXOnScreen() { return getEvent().getXOnScreen(); }

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
    public int mouseYOnScreen() { return getEvent().getYOnScreen(); }

}
