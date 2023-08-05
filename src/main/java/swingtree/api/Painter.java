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
 *  </ul>
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
                };
    }
}
