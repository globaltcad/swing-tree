package swingtree.style;

import java.awt.*;

class StyleUtility
{
    private StyleUtility() {}

    static String toString( Color color ) {
        if ( color == null ) return "null";
        return "rgba(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + "," + color.getAlpha() + ")";
    }

    static String toString( Arc arc ) {
        if ( arc == null ) return "null";
        return "Arc(" + arc.width() + "," + arc.height() + ")";
    }
}
