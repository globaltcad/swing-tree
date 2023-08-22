package swingtree.api;


import swingtree.UI;
import swingtree.animation.Animation;
import swingtree.animation.AnimationState;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.concurrent.TimeUnit;

/**
 *  Implement this functional interface to either paint <b>custom styles on components</b>
 *  by registering painters on the style API using methods like <br>
 *  {@link swingtree.style.ComponentStyleDelegate#painter(UI.Layer, Painter)} and <br>
 *  {@link swingtree.style.ComponentStyleDelegate#painter(UI.Layer, String, Painter)} <br>
 *  or <br>
 *  for <b>defining custom animations</b> by registering painters in the animation API using the
 *  {@link swingtree.ComponentDelegate#paint(AnimationState, Painter)} method
 *  inside of an {@link Animation} registered through
 *  {@link swingtree.ComponentDelegate#animateOnce(double, TimeUnit, Animation)}.
 *  <br><br>
 *  Note that inside the painter the {@link Graphics2D} context may not
 *  be scaled according to the current UI scale factor (for high DPI displays). <br>
 *  Check out the following methods for scaling you paint operations: <br>
 *  <ul>
 *      <li>{@link UI#scale()} - returns the current UI scale factor.</li>
 *      <li>{@link UI#scale(Graphics2D)} - scales the given graphics context according to the current UI scale factor.</li>
 *      <li>{@link UI#scale(double)} - scales the given value according to the current UI scale factor.</li>
 *      <li>{@link UI#scale(float)} - scales the given value according to the current UI scale factor.</li>
 *      <li>{@link UI#scale(int)} - scales the given value according to the current UI scale factor.</li>
 *      <li>{@link UI#scale(Insets)} - scales the given insets according to the current UI scale factor.</li>
 *      <li>{@link UI#scale(Dimension)} - scales the given dimension according to the current UI scale factor.</li>
 *      <li>{@link UI#scale(Rectangle)} - scales the given rectangle according to the current UI scale factor.</li>
 *      <li>{@link UI#scale(RoundRectangle2D)} - scales the given round rectangle according to the current UI scale factor.</li>
 *  </ul><br>
 *
 *  <b>Also consider taking a look at the <br> <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 *  where you can browse a large collection of examples demonstrating how to use the API of Swing-Tree in general.</b>
 *
 */
@FunctionalInterface
public interface Painter
{
    static Painter none() { return Constants.PAINTER_NONE; }

    /**
     * Paints a custom style on a component using the given graphics context.
     * @param g2d the graphics context to use for painting.
     */
    void paint( Graphics2D g2d );

    /**
     * Returns a new painter that paints this painter's style and then the given painter's style.
     * @param after the painter to paint after this painter.
     * @return a new painter that paints this painter's style and then the given painter's style.
     */
    default Painter andThen(Painter after) {
        return g2d -> {
                    try {
                        paint(g2d);
                    } catch ( Exception e ) {
                        e.printStackTrace();
                        // Exceptions inside a painter should not be fatal.
                    }
                    try {
                        after.paint(g2d);
                    } catch ( Exception e ) {
                        e.printStackTrace();
                        // Exceptions inside a painter should not cripple the rest of the painting.
                    }
                    /*
                         Note that if any exceptions happen in the above Painter implementations,
                         then we don't want to mess up the execution of the rest of the component painting...
                         Therefore, we catch any exceptions that happen in the above code.

                         Ideally this would be logged in the logging framework of a user of the SwingTree
                         library, but we don't know which logging framework that is, so we just print
                         the stack trace to the console so that developers can see what went wrong.

                         Hi there! If you are reading this, you are probably a developer using the SwingTree
                         library, thank you for using it! Good luck finding out what went wrong! :)
                    */
                };
    }
}
