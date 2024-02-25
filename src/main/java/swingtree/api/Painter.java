package swingtree.api;


import swingtree.UI;
import swingtree.animation.Animation;
import swingtree.animation.AnimationState;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;
import java.util.concurrent.TimeUnit;

/**
 *  A functional interface for doing custom painting on a component
 *  using the {@link Graphics2D} API.
 *  This is typically used to paint <b>custom styles on components</b> as part of the style API
 *  exposed by {@link swingtree.UIForAnySwing#withStyle(Styler)}, like so:
 *  <pre>{@code
 *  UI.label("I am a label")
 *  .withStyle( it -> it
 *    .size(120, 50)
 *    .padding(6)
 *    .painter(UI.Layer.BACKGROUND, g -> {
 *      g.setColor(Color.ORANGE);
 *      var e = new Ellipse2D.Double(5,5,25,25);
 *      g.fill(UI.scale(e);
 *    })
 *    .fontSize(12)
 *  )
 *  }</pre>
 *  Which is based on the {@link swingtree.style.ComponentStyleDelegate#painter(UI.Layer, Painter)}.
 *  You may also want to take a look at <br>
 *  {@link swingtree.style.ComponentStyleDelegate#painter(UI.Layer, UI.ComponentArea, Painter)}, <br>
 *  {@link swingtree.style.ComponentStyleDelegate#painter(UI.Layer, String, Painter)} and <br>
 *  {@link swingtree.style.ComponentStyleDelegate#painter(UI.Layer, UI.ComponentArea, String, Painter)}. <br>
 *  <br>
 *  You can also use painter implementations
 *  for <b>defining custom component event based animations</b> by registering through the
 *  {@link swingtree.ComponentDelegate#paint(AnimationState, Painter)} method
 *  inside of an {@link Animation} registered through
 *  {@link swingtree.ComponentDelegate#animateFor(double, TimeUnit, Animation)}.
 *  <br><br>
 *  Note that inside the painter the {@link Graphics2D} context may not
 *  be scaled according to the current UI scale factor (for high DPI displays). <br>
 *  Check out the following methods for scaling your paint operations: <br>
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
 *      <li>{@link UI#scale(java.awt.geom.Ellipse2D)} - scales the given ellipse according to the current UI scale factor.</li>
 *  </ul><br>
 *  <br>
 *  Note that your custom painters will yield the best performance if they are stateless and immutable
 *  as well has if they have a good {@link Object#hashCode()} and {@link Object#equals(Object)} implementation.
 *  This is because it allows SwingTree to cache the rendering of the painters and avoid unnecessary repaints. <br>
 *  <b>If you do not want to create a custom class just for painting but instead
 *  just want to pass an immutable cache key to a painter, then consider using the
 *  {@link #of(Object, Painter)} factory method to create a painter that has the
 *  with {@link Object#hashCode()} and {@link Object#equals(Object)} implemented as a delegate to the data object.</b>
 *  <p>
 *  <b>Also consider taking a look at the <br> <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 *  where you can browse a large collection of examples demonstrating how to use the API of Swing-Tree in general.</b>
 *
 */
@FunctionalInterface
public interface Painter
{
    /**
     * @return A painter that paints nothing.
     */
    static Painter none() { return Constants.PAINTER_NONE; }

    static <D> Painter of( D data, Painter painter ) {
        return new CachablePainter(data, painter);
    }


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
    default Painter andThen( Painter after ) {
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
