package swingtree;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Supplier;

/**
 *  A {@link JComponent} delegate providing useful context information to various {@link sprouts.Action} listeners
 *  used by {@link UIForAnySwing#onMouseClick(sprouts.Action)}}, {@link UIForAnySwing#onMousePress(sprouts.Action)} and
 *  {@link UIForAnySwing#onMouseRelease(sprouts.Action)}, for example.
 *  <br>
 *  Not only does this delegate provide access to the {@link JComponent} component itself, but also to the
 *  {@link MouseEvent} that triggered the event.
 */
public class ComponentMouseEventDelegate<C extends JComponent> extends ComponentDelegate<C, MouseEvent>
{
    public ComponentMouseEventDelegate(
        C component,
        MouseEvent event,
        Supplier<List<JComponent>> siblingSource
    ) {
        super(component, event, siblingSource);
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
     */
    public boolean isAltDown() { return getEvent().isAltDown(); }

    /**
     * Returns whether the Control modifier is down on the event of this delegate.
     */
    public boolean isCtrlDown() { return getEvent().isControlDown(); }

    /**
     * Returns whether the Shift modifier is down on the event of this delegate.
     */
    public boolean isShiftDown() { return getEvent().isShiftDown(); }

    /**
     * Returns whether the Meta modifier is down on the event of this delegate.
     */
    public boolean isMetaDown() { return getEvent().isMetaDown(); }

    /**
     * Returns the number of mouse clicks associated with the event of this delegate.
     *
     * @return integer value for the number of clicks
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
     * In a virtual device multi-screen environment in which the
     * desktop area could span multiple physical screen devices,
     * this coordinate is relative to the virtual coordinate system.
     * Otherwise, this coordinate is relative to the coordinate system
     * associated with the Component's GraphicsConfiguration.
     *
     * @return y  an integer indicating absolute vertical position.
     */
    public int mouseYOnScreen() { return getEvent().getYOnScreen(); }

}
