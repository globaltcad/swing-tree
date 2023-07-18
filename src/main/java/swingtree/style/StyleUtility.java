package swingtree.style;

import java.awt.*;
import java.util.Map;
import java.util.Objects;

final class StyleUtility
{
    static final String DEFAULT_KEY = "default";
    static final Styler<?> STYLER_NONE = delegate -> delegate;
    static final Painter PAINTER_NONE = new Painter() {
                                            @Override
                                            public void paint(Graphics2D g2d) {
                                                // None
                                            }
                                            @Override public String toString() { return "none"; }
                                        };

    private StyleUtility() {}

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
        if ( painter == Painter.none() ) return "null";
        return "Painter@" + Integer.toHexString(Objects.hashCode(painter));
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

        // Let's try a few common color names
        if ( colorString.equalsIgnoreCase("black")       ) return Color.BLACK;
        if ( colorString.equalsIgnoreCase("blue")        ) return Color.BLUE;
        if ( colorString.equalsIgnoreCase("cyan")        ) return Color.CYAN;
        if ( colorString.equalsIgnoreCase("darkGray")    ) return Color.DARK_GRAY;
        if ( colorString.equalsIgnoreCase("gray")        ) return Color.GRAY;
        if ( colorString.equalsIgnoreCase("green")       ) return Color.GREEN;
        if ( colorString.equalsIgnoreCase("lightGray")   ) return Color.LIGHT_GRAY;
        if ( colorString.equalsIgnoreCase("magenta")     ) return Color.MAGENTA;
        if ( colorString.equalsIgnoreCase("orange")      ) return Color.ORANGE;
        if ( colorString.equalsIgnoreCase("pink")        ) return Color.PINK;
        if ( colorString.equalsIgnoreCase("red")         ) return Color.RED;
        if ( colorString.equalsIgnoreCase("white")       ) return Color.WHITE;
        if ( colorString.equalsIgnoreCase("yellow")      ) return Color.YELLOW;
        if ( colorString.equalsIgnoreCase("transparent") ) return new Color(0, 0, 0, 0);

        // Let's try to find it as a system property
        try {
            return Color.getColor(colorString);
        } catch ( IllegalArgumentException e ) {
            // Ignore
        }

        throw new IllegalArgumentException("Could not parse or find color value: " + colorString);
    }

    public static  <T> boolean mapEquals( Map<String, T> map1, Map<String, T> map2 ) {
        if ( map1.size() != map2.size() ) return false;
        for ( Map.Entry<String, T> entry : map1.entrySet() ) {
            if ( !map2.containsKey(entry.getKey()) ) return false;
            if ( !Objects.equals(entry.getValue(), map2.get(entry.getKey())) ) return false;
        }
        return true;
    }

    public static <T> int mapHash( Map<String, T> map ) {
        return map.entrySet().stream().mapToInt(e -> Objects.hash(e.getKey(), e.getValue())).sum();
    }
}
