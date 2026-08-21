package swingtree.style;

import org.jspecify.annotations.Nullable;
import swingtree.UI;
import swingtree.api.Painter;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.Objects;

/**
 *  Well, here it is, almost every project seems to have at least one of these, right? <br>
 *  Although we tried really hard to avoid it, we still ended up with a random utility class.
 *  <b>But don't worry, it is package-private, so it may not pollute the public API.
 *  <br><br>
 *  Dear future maintainer(s): Please keep it private!</b>
 */
final class StyleUtil
{
    static final String DEFAULT_KEY = "default";


    private StyleUtil() {} // No instantiation, just a utility class

    static void transferConfigurations(
            Graphics2D from,
            Graphics2D to
    ) {
        to.setFont(from.getFont());
        to.setColor(from.getColor());
        to.setBackground(from.getBackground());
        to.setComposite(from.getComposite());
        to.setClip(null); // We want to capture the full style and clip it later (see g.drawImage(_cache, 0, 0, null); below.
        to.setComposite(from.getComposite());
        to.setPaint(from.getPaint());
        to.setRenderingHints(from.getRenderingHints());
        to.setStroke(from.getStroke());
    }

    /**
     *  A symmetric, implementation-tolerant equality check for two {@link Shape} instances.
     *  <p>
     *  This is needed because {@link Rectangle#equals(Object)} is <i>asymmetric</i> with respect
     *  to the other rectangular shape implementations of the AWT geometry API: a {@link Rectangle}
     *  only considers itself equal to another {@link Rectangle}, whereas a
     *  {@link Rectangle2D.Float} (or {@code Double}) considers itself equal to any
     *  {@link Rectangle2D} of the same geometry. So for two shapes of identical geometry
     *  {@code aRectangle.equals(aRectangle2D)} may return {@code false} while the reversed
     *  {@code aRectangle2D.equals(aRectangle)} returns {@code true}. Relying on the plain
     *  {@code equals} would therefore silently miss rendering optimizations that hinge on two
     *  areas being the same.
     *  <p>
     *  This helper normalizes the comparison so that the order of the arguments does not matter
     *  and two rectangular shapes with the same geometry are always reported as equal,
     *  regardless of their concrete {@link Rectangle2D} sub-type.
     *
     * @param a The first shape, may be {@code null}.
     * @param b The second shape, may be {@code null}.
     * @return {@code true} if both shapes are geometrically equal (or both {@code null}).
     */
    static boolean shapesAreEqual( @Nullable Shape a, @Nullable Shape b ) {
        if ( a == b )
            return true;
        if ( a == null || b == null )
            return false;
        if ( a instanceof Rectangle2D && b instanceof Rectangle2D ) {
            // Rectangle.equals() is not symmetric across the various Rectangle2D
            // implementations, so we compare the geometry through the Rectangle2D contract:
            Rectangle2D ra = (Rectangle2D) a;
            Rectangle2D rb = (Rectangle2D) b;
            return ra.getX()      == rb.getX()
                && ra.getY()      == rb.getY()
                && ra.getWidth()  == rb.getWidth()
                && ra.getHeight() == rb.getHeight();
        }
        return a.equals(b);
    }

    static @Nullable Shape intersect( @Nullable Shape clipA, @Nullable Shape clipB )
    {
        if ( Objects.equals(clipA, clipB) )
            return clipA;

        if ( clipA == null )
            return clipB;
        if ( clipB == null )
            return clipA;

        // An Area intersection is full boolean path geometry, and ~80% of calls do not need
        // it: usually one clip already contains the other, or both are rectangles. These
        // shortcuts describe the same region, and like the null cases above they can hand
        // back an argument, so callers must not mutate what they get.
        if ( clipB instanceof Rectangle2D && clipB.contains(clipA.getBounds2D()) )
            return clipA;
        if ( clipA instanceof Rectangle2D && clipA.contains(clipB.getBounds2D()) )
            return clipB;
        if ( clipA instanceof Rectangle2D && clipB instanceof Rectangle2D ) {
            final Rectangle2D a = (Rectangle2D) clipA;
            final Rectangle2D b = (Rectangle2D) clipB;
            if ( a.intersects(b) )
                return a.createIntersection(b);
        }

        Area intersected = new Area(clipB);
        intersected.intersect(new Area(clipA));
        return intersected;
    }

    @SuppressWarnings("ReferenceEquality")
    static String toString( @Nullable Color color ) {
        if ( color == UI.Color.UNDEFINED ) return "DEFAULT";
        if ( color == null ) return "?";
        return "rgba(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + "," + color.getAlpha() + ")";
    }

    static String toString( @Nullable Paint paint ) {
        if ( paint == null ) return "?";
        if ( paint instanceof Color ) return toString((Color) paint);
        return Objects.toString(paint);
    }

    static String toString( @Nullable Arc arc ) {
        if ( arc == null ) return "?";
        return "Arc(" + arc.width() + "," + arc.height() + ")";
    }

    static String toString( Painter painter ) {
        if ( painter == null ) return "?";
        if ( painter == Painter.none() ) return "none";
        return "Painter@" + Integer.toHexString(Objects.hashCode(painter));
    }

    @SuppressWarnings("GetClassOnEnum")
    static String toString( Enum<?> enumBasedId ) {
        return enumBasedId.getClass().getSimpleName() + "." + enumBasedId.name();
    }

    @SuppressWarnings("ReferenceEquality")
    static boolean isUndefinedColor( @Nullable Color color ) {
        return color == UI.Color.UNDEFINED;
    }

    @SuppressWarnings("ReferenceEquality")
    static boolean isUndefinedFont( @Nullable Font font ) {
        return font == UI.Font.UNDEFINED;
    }
}
