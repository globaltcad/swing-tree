package swingtree.style;

import swingtree.api.Painter;

import java.awt.*;
import java.awt.geom.Area;
import java.util.Objects;

/**
 *  Well, here it is, almost every project seems to have at least one of these, right? <br>
 *  Although we tries really hard to avoid it, we still ended up with a random utility class.
 *  <b>But don't worry, it is package-private, so it may not pollute the public API.
 *  <br><br>
 *  Dear future maintainer(s): Please keep it private!</b>
 */
final class StyleUtility
{
    static final String DEFAULT_KEY = "default";
    static final Layout UNSPECIFIC_LAYOUT_CONSTANT = new Layout.Unspecific();
    static final Layout NONE_LAYOUT_CONSTANT = new Layout.None();

    private StyleUtility() {} // No instantiation, just a utility class


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

    /**
     *  Tries to parse the supplied string as a color value
     *  based on various formats.
     *
     * @param colorString The string to parse.
     * @return The parsed color.
     * @throws IllegalArgumentException If the string could not be parsed.
     */
    static Color toColor( String colorString )
    {
        Objects.requireNonNull(colorString);
        // First some cleanup
        colorString = colorString.trim();

        if ( colorString.startsWith("#") )
            return Color.decode(colorString);

        if ( colorString.startsWith("0x") )
            return Color.decode(colorString);

        if ( colorString.startsWith("rgb") ) {
            // We have an rgb() or rgba() color
            int start = colorString.indexOf('(');
            int end = colorString.indexOf(')');
            if ( start < 0 || end < 0 || end < start )
                throw new IllegalArgumentException("Invalid rgb() or rgba() color: " + colorString);

            String[] parts = colorString.substring(start + 1, end).split(",");
            if ( parts.length < 3 || parts.length > 4 )
                throw new IllegalArgumentException("Invalid rgb() or rgba() color: " + colorString);

            for ( int i = 0; i < parts.length; i++ )
                parts[i] = parts[i].trim();

            int[] values = new int[parts.length];

            for ( int i = 0; i < parts.length; i++ ) {
                String part = parts[i];
                if ( part.endsWith("%") ) {
                    part = part.substring(0, part.length() - 1);
                    values[i] = Integer.parseInt(part);
                    if ( values[i] < 0 || values[i] > 100 )
                        throw new IllegalArgumentException("Invalid rgb() or rgba() color: " + colorString);
                    values[i] = (int) Math.ceil(values[i] * 2.55);
                }
                else if ( part.matches("[0-9]+((\\.[0-9]+[fF]?)|[fF])") )
                    values[i] = (int) (Float.parseFloat(part) * 255);
                else
                    values[i] = Integer.parseInt(part);
            }
            int r = values[0];
            int g = values[1];
            int b = values[2];
            int a = values.length == 4 ? values[3] : 255;
            return new Color(r, g, b, a);
        }

        if ( colorString.startsWith("hsb") ) {
            // We have an hsb() or hsba() color
            int start = colorString.indexOf('(');
            int end = colorString.indexOf(')');
            if ( start < 0 || end < 0 || end < start )
                throw new IllegalArgumentException("Invalid hsb() or hsba() color: " + colorString);

            String[] parts = colorString.substring(start + 1, end).split(",");
            if ( parts.length < 3 || parts.length > 4 )
                throw new IllegalArgumentException("Invalid hsb() or hsba() color: " + colorString);

            for ( int i = 0; i < parts.length; i++ )
                parts[i] = parts[i].trim();

            float[] values = new float[parts.length];

            for ( int i = 0; i < parts.length; i++ ) {
                String part = parts[i];
                if ( part.endsWith("%") ) {
                    part = part.substring(0, part.length() - 1);
                    values[i] = Float.parseFloat(part);
                    if ( values[i] < 0 || values[i] > 100 )
                        throw new IllegalArgumentException(
                                "Invalid hsb() or hsba() string '" + colorString + "', value '" + part + "' out of range."
                        );
                    values[i] = values[i] / 100.0f;
                } else if ( part.endsWith("°") ) {
                    if ( i > 0 )
                        throw new IllegalArgumentException(
                                "Invalid hsb() or hsba() string '" + colorString + "', unexpected degree symbol in '" + part + "' (only allowed for hue)"
                        );

                    part = part.substring(0, part.length() - 1);
                    values[i] = Float.parseFloat(part);
                    if ( values[i] < 0 || values[i] > 360 )
                        throw new IllegalArgumentException(
                                "Invalid hsb() or hsba() string '" + colorString + "', hue value '" + part + "' out of range."
                        );
                    values[i] = values[i] / 360.0f;
                } else if ( part.matches("[0-9]+((\\.[0-9]+[fF]?)|[fF])") )
                    values[i] = Float.parseFloat(part);
                else
                    values[i] = Integer.parseInt(part);
            }

            float h = values[0];
            float s = values[1];
            float b = values[2];
            float a = values.length == 4 ? values[3] : 1.0f;
            Color c = Color.getHSBColor(h, s, b);
            return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int)(a * 255));
        }

        {
            boolean transparent = false;

            if ( colorString.trim().startsWith("transparent ") ) {
                transparent = true;
                colorString = colorString.substring(12);
            }

            Color color = null;

            // Let's try a few common color names
            if ( colorString.equalsIgnoreCase("black")       ) color = Color.BLACK;
            if ( colorString.equalsIgnoreCase("blue")        ) color = Color.BLUE;
            if ( colorString.equalsIgnoreCase("cyan")        ) color = Color.CYAN;
            if ( colorString.equalsIgnoreCase("darkGray")    ) color = Color.DARK_GRAY;
            if ( colorString.equalsIgnoreCase("gray")        ) color = Color.GRAY;
            if ( colorString.equalsIgnoreCase("green")       ) color = Color.GREEN;
            if ( colorString.equalsIgnoreCase("lightGray")   ) color = Color.LIGHT_GRAY;
            if ( colorString.equalsIgnoreCase("magenta")     ) color = Color.MAGENTA;
            if ( colorString.equalsIgnoreCase("orange")      ) color = Color.ORANGE;
            if ( colorString.equalsIgnoreCase("pink")        ) color = Color.PINK;
            if ( colorString.equalsIgnoreCase("red")         ) color = Color.RED;
            if ( colorString.equalsIgnoreCase("white")       ) color = Color.WHITE;
            if ( colorString.equalsIgnoreCase("yellow")      ) color = Color.YELLOW;
            if ( colorString.equalsIgnoreCase("transparent") ) color = new Color(0, 0, 0, 0);

            if ( color != null ) {
                if ( transparent )
                    return new Color(color.getRed(), color.getGreen(), color.getBlue(), 0);
                else
                    return color;
            }
        }

        // Let's try to find it as a system property
        try {
            return Color.getColor(colorString);
        } catch ( IllegalArgumentException e ) {
            // Ignore
        }

        throw new IllegalArgumentException("Could not parse or find color value: " + colorString);
    }

}
