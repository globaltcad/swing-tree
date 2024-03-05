package swingtree.style;

import swingtree.UI;
import swingtree.api.Painter;

import java.awt.*;
import java.awt.geom.Area;
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


    static Shape intersect( Shape clipA, Shape clipB )
    {
        if ( Objects.equals(clipA, clipB) )
            return clipA;

        Shape finalClip = null;

        if ( clipA == null && clipB != null )
            finalClip = clipB;

        if ( clipA != null && clipB == null )
            finalClip = clipA;

        if ( clipA != null && clipB != null ) {
            Area intersected = new Area(clipB);
            intersected.intersect(new Area(clipA));
            finalClip = intersected;
        }
        return finalClip;
    }

    static String toString( Color color ) {
        if ( color == UI.COLOR_UNDEFINED) return "DEFAULT";
        if ( color == null ) return "?";
        return "rgba(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + "," + color.getAlpha() + ")";
    }

    static String toString( Arc arc ) {
        if ( arc == null ) return "?";
        return "Arc(" + arc.width() + "," + arc.height() + ")";
    }

    static String toString( Painter painter ) {
        if ( painter == null ) return "?";
        if ( painter == Painter.none() ) return "none";
        return "Painter@" + Integer.toHexString(Objects.hashCode(painter));
    }

    static String toString( Enum<?> enumBasedId ) {
        return enumBasedId.getClass().getSimpleName() + "." + enumBasedId.name();
    }

}