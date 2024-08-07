package swingtree.api;


import swingtree.UI;
import swingtree.animation.Animation;
import swingtree.animation.AnimationStatus;

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
 *  {@link swingtree.ComponentDelegate#paint(AnimationStatus, Painter)} method
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
     *  Exposes a constant painter that paints nothing.
     *  This is useful as a no-op null object pattern.
     * @return A painter that paints nothing.
     */
    static Painter none() { return Constants.PAINTER_NONE; }

    /**
     *  Allows you to create a cacheable painter that uses the given data object as a cache key.
     *  The provided data object should be immutable and have exhaustive {@link Object#hashCode()}
     *  and {@link Object#equals(Object)} implementations. <br>
     *  The data object is expected to be the sole source of information for the painter's painting operations.
     *  Otherwise, the usage of this method is discouraged as cache based
     *  rendering may not reflect the actual state of the component. <br>
     *
     * @param data The data object to use as a cache key, must be immutable and have
     *             exhaustive {@link Object#hashCode()} and {@link Object#equals(Object)} implementations.
     * @param painter The painter to use for painting, must be stateless and immutable as well.
     *                It is expected to use the given data object as the sole source of information
     *                for its painting operations.
     * @return A cache friendly painter that uses the given data object as a cache key.
     * @param <D> The type of the data object to use as a cache key.
     */
    static <D> Painter of( D data, Painter painter ) {
        return new CachablePainter(data, painter);
    }

    /**
     * Paints a custom style on a component using the given graphics context.
     * @param g2d the graphics context to use for painting.
     */
    void paint( Graphics2D g2d );

    /**
     *  If a painter implementation reports that it can be cached, SwingTree will
     *  use the painter as a cache key and the result of its painting operations
     *  will be cached and reused for equal cache keys. <br>
     *  So a painter that can be cached should be stateless and immutable as well as
     *  have exhaustive {@link Object#hashCode()} and
     *  {@link Object#equals(Object)} implementations. <br>
     *
     * @return true If the painting operation is cachable, false otherwise.
     */
    default boolean canBeCached() { return false; }

    /**
     * Returns a new painter that paints this painter's style and then the given painter's style.
     * @param after the painter to paint after this painter.
     * @return a new painter that paints this painter's style and then the given painter's style.
     */
    default Painter andThen( Painter after ) {
        return new AndThenPainter(this, after);
    }

}
