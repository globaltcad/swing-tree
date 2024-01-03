package swingtree;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Objects;

/**
 *  A namespace for useful factory methods and
 *  layout constants (see {@link UILayoutConstants}).
 *  <br>
 *  <b>
 *      This class is intended to be used
 *      by the {@link UI} namespace class ONLY!
 *      <br>
 *      Please do not inherit or import this class
 *      in your own code, as it is not intended to be
 *      used outside of the {@link UI} namespace.
 *  </b>
 */
public abstract class UINamespaceUtilities extends UILayoutConstants
{
    private static final Logger log = LoggerFactory.getLogger(UINamespaceUtilities.class);

    public static final Color NO_COLOR = new Color(0, 0, 0, 0);

    /**
     *  Creates a new {@link Color} object from the specified
     *  red, green and blue values.
     *
     * @param r The red value (0-255).
     * @param g The green value (0-255).
     * @param b The blue value (0-255).
     * @return The new color.
     */
    public static Color color( int r, int g, int b ) {
        return new Color(r, g, b);
    }

    /**
     *  Creates a new {@link Color} object from the specified
     *  red, green, blue and alpha values.
     *
     * @param r The red value (0-255).
     * @param g The green value (0-255).
     * @param b The blue value (0-255).
     * @param a The alpha value (0-255).
     * @return The new color.
     */
    public static Color color( int r, int g, int b, int a ) {
        return new Color(r, g, b, a);
    }

    /**
     *  Creates a new {@link Color} object from the specified
     *  red, green and blue values.
     *
     * @param r The red value (0.0-1.0).
     * @param g The green value (0.0-1.0).
     * @param b The blue value (0.0-1.0).
     * @return The new color.
     */
    public static Color color( float r, float g, float b ) {
        return new Color(r, g, b);
    }

    /**
     *  Creates a new {@link Color} object from the specified
     *  red, green, blue and alpha values.
     *
     * @param r The red value (0.0-1.0).
     * @param g The green value (0.0-1.0).
     * @param b The blue value (0.0-1.0).
     * @param a The alpha value (0.0-1.0).
     * @return The new color.
     */
    public static Color color( float r, float g, float b, float a ) {
        return new Color(r, g, b, a);
    }

    /**
     *  Tries to parse the supplied string as a color value
     *  based on various formats.
     *
     * @param colorString The string to parse.
     * @return The parsed color.
     * @throws IllegalArgumentException If the string could not be parsed.
     * @throws NullPointerException If the string is null.
     */
    public static Color color( String colorString )
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

    /**
     * Returns the {@code Font} that the {@code str}
     * argument describes.
     * To ensure that this method returns the desired Font,
     * format the {@code str} parameter in
     * one of these ways
     *
     * <ul>
     * <li><em>fontname-style-pointsize</em>
     * <li><em>fontname-pointsize</em>
     * <li><em>fontname-style</em>
     * <li><em>fontname</em>
     * <li><em>fontname style pointsize</em>
     * <li><em>fontname pointsize</em>
     * <li><em>fontname style</em>
     * <li><em>fontname</em>
     * </ul>
     * in which <i>style</i> is one of the four
     * case-insensitive strings:
     * {@code "PLAIN"}, {@code "BOLD"}, {@code "BOLDITALIC"}, or
     * {@code "ITALIC"}, and pointsize is a positive decimal integer
     * representation of the point size.
     * For example, if you want a font that is Arial, bold, with
     * a point size of 18, you would call this method with:
     * "Arial-BOLD-18".
     * This is equivalent to calling the Font constructor :
     * {@code new Font("Arial", Font.BOLD, 18);}
     * and the values are interpreted as specified by that constructor.
     * <p>
     * A valid trailing decimal field is always interpreted as the pointsize.
     * Therefore a fontname containing a trailing decimal value should not
     * be used in the fontname only form.
     * <p>
     * If a style name field is not one of the valid style strings, it is
     * interpreted as part of the font name, and the default style is used.
     * <p>
     * Only one of ' ' or '-' may be used to separate fields in the input.
     * The identified separator is the one closest to the end of the string
     * which separates a valid pointsize, or a valid style name from
     * the rest of the string.
     * Null (empty) pointsize and style fields are treated
     * as valid fields with the default value for that field.
     *<p>
     * Some font names may include the separator characters ' ' or '-'.
     * If {@code str} is not formed with 3 components, e.g. such that
     * {@code style} or {@code pointsize} fields are not present in
     * {@code str}, and {@code fontname} also contains a
     * character determined to be the separator character
     * then these characters where they appear as intended to be part of
     * {@code fontname} may instead be interpreted as separators
     * so the font name may not be properly recognised.
     *
     * <p>
     * The default size is 12 and the default style is PLAIN.
     * If {@code str} does not specify a valid size, the returned
     * {@code Font} has a size of 12.  If {@code str} does not
     * specify a valid style, the returned Font has a style of PLAIN.
     * If you do not specify a valid font name in
     * the {@code str} argument, this method will return
     * a font with the family name "Dialog".
     * To determine what font family names are available on
     * your system, use the
     * {@link GraphicsEnvironment#getAvailableFontFamilyNames()} method.
     * If {@code str} is {@code null}, a new {@code Font}
     * is returned with the family name "Dialog", a size of 12 and a
     * PLAIN style.
     * @param fontString the name of the font, or {@code null}
     * @return the {@code Font} object that {@code str} describes.
     * @throws NullPointerException if {@code str} is {@code null}
     */
    public static Font font( String fontString ) {
        Objects.requireNonNull(fontString);
        Exception potentialProblem1 = null;
        Exception potentialProblem2 = null;
        String mayBeProperty = System.getProperty(fontString);
        Font font = null;
        try {
            if ( mayBeProperty == null )
                font = Font.decode(fontString);
        } catch( Exception e ) {
            potentialProblem1 = e;
        }
        try {
            if ( mayBeProperty != null )
                font = Font.decode(mayBeProperty);
        } catch( Exception e ) {
            potentialProblem2 = e;
        }
        if ( font == null ) {
            if ( potentialProblem1 != null )
                log.error("Could not parse font string '" + fontString + "' using 'Font.decode(String)'.", potentialProblem1);
            if ( potentialProblem2 != null )
                log.error("Could not parse font string '" + fontString + "' from 'System.getProperty(String)'.", potentialProblem2);

            log.error("Could not parse font string '" + fontString + "' using 'Font.decode(String)' or 'System.getProperty(String)'.", new Throwable());

            try {
                return new Font(fontString, Font.PLAIN, UI.scale(12));
            } catch (Exception e) {
                log.error("Could not create font with name '" + fontString + "' and size 12.", e);
                return new Font(Font.DIALOG, Font.PLAIN, UI.scale(12));
            }
        }
        return font;
    }

}
