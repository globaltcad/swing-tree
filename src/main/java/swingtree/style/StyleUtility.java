package swingtree.style;

import java.awt.*;

class StyleUtility
{
    private StyleUtility() {}

    static String toString( Color color ) {
        if ( color == null ) return "null";
        return "rgba(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + "," + color.getAlpha() + ")";
    }
}
