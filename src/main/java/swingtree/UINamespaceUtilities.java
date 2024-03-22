package swingtree;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import swingtree.style.Colour;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Objects;

/**
 *  A namespace for useful factory methods like
 *  {@link #color(String)} and {@link #font(String)},
 *  and layout constants (see {@link UILayoutConstants}).
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

    /**
     *  This constant is a {@link java.awt.Color} object with all of its rgba values set to 0.
     *  Its identity is used to represent the absence of a color being specified,
     *  and it is used as a safe replacement for null,
     *  meaning that when the style engine of a component encounters it, it will pass it onto
     *  the {@link java.awt.Component#setBackground(Color)} and
     *  {@link java.awt.Component#setForeground(Color)} methods as null.
     *  Passing null to these methods means that the look and feel determines the coloring.
     */
    public static final Colour COLOR_UNDEFINED = Colour.UNDEFINED;

    /**
     *  This constant is a {@link java.awt.Font} object with a font name of "" (empty string),
     *  a font style of -1 (undefined) and a font size of 0.
     *  Its identity is used to represent the absence of a font being specified,
     *  and it is used as a safe replacement for null,
     *  meaning that when the style engine of a component encounters it, it will pass it onto
     *  the {@link java.awt.Component#setFont(Font)} method as null.
     *  Passing null to this method means that the look and feel determines the font.
     */
    public static final Font FONT_UNDEFINED = new Font("", -1, 0);

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
    public static Color color( double r, double g, double b ) {
        return new Color((float) r, (float) g, (float) b);
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
    public static Color color( double r, double g, double b, double a ) {
        return new Color((float) r, (float) g, (float) b, (float) a);
    }

    /**
     *  Tries to parse the supplied string as a color value
     *  based on various formats.
     *
     * @param colorAsString The string to parse.
     * @return The parsed color.
     * @throws IllegalArgumentException If the string could not be parsed.
     * @throws NullPointerException If the string is null.
     */
    public static Color color( final String colorAsString )
    {
        Objects.requireNonNull(colorAsString);
        return Colour.of(colorAsString);
    }

    /**
     * Returns the {@code Font} that the {@code fontString}
     * argument describes.
     * To ensure that this method returns the desired Font,
     * format the {@code fontString} parameter in
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
     * If {@code fontString} is not formed with 3 components, e.g. such that
     * {@code style} or {@code pointsize} fields are not present in
     * {@code fontString}, and {@code fontname} also contains a
     * character determined to be the separator character
     * then these characters where they appear as intended to be part of
     * {@code fontname} may instead be interpreted as separators
     * so the font name may not be properly recognised.
     *
     * <p>
     * The default size is 12 and the default style is PLAIN.
     * If {@code str} does not specify a valid size, the returned
     * {@code Font} has a size of 12.  If {@code fontString} does not
     * specify a valid style, the returned Font has a style of PLAIN.
     * If you do not specify a valid font name in
     * the {@code fontString} argument, this method will return
     * a font with the family name "Dialog".
     * To determine what font family names are available on
     * your system, use the
     * {@link GraphicsEnvironment#getAvailableFontFamilyNames()} method.
     * If {@code fontString} is {@code null}, a new {@code Font}
     * is returned with the family name "Dialog", a size of 12 and a
     * PLAIN style.
     * @param fontString the name of the font, or {@code null}
     * @return the {@code Font} object that {@code fontString} describes.
     * @throws NullPointerException if {@code fontString} is {@code null}
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
