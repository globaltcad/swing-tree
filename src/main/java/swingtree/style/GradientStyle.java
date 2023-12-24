package swingtree.style;

import org.slf4j.Logger;
import swingtree.UI;

import java.awt.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

/**
 *  An immutable, wither-like copy method based config API
 *  for specifying a gradient style as a sub-style of various other styles,
 *  like for example {@link BaseStyle} or {@link BorderStyle} accessed through the
 *  {@link ComponentStyleDelegate#gradient(String, Function)} or
 *  {@link ComponentStyleDelegate#borderGradient(String, Function)}
 *  methods.
 *  <p>
 *  The following properties with their respective purpose are available:
 *  <br>
 *  <ul>
 *      <li><h3>Transition</h3>
 *          The transition defines the direction of the gradient.
 *          <br>
 *          The following transitions are available:
 *          <ul>
 *              <li>{@link UI.Transition#TOP_LEFT_TO_BOTTOM_RIGHT}</li>
 *              <li>{@link UI.Transition#BOTTOM_LEFT_TO_TOP_RIGHT}</li>
 *              <li>{@link UI.Transition#TOP_RIGHT_TO_BOTTOM_LEFT}</li>
 *              <li>{@link UI.Transition#BOTTOM_RIGHT_TO_TOP_LEFT}</li>
 *              <li>{@link UI.Transition#TOP_TO_BOTTOM}</li>
 *              <li>{@link UI.Transition#LEFT_TO_RIGHT}</li>
 *              <li>{@link UI.Transition#BOTTOM_TO_TOP}</li>
 *              <li>{@link UI.Transition#RIGHT_TO_LEFT}</li>
 *          </ul>
 *      </li>
 *      <li><h3>Type</h3>
 *          The type defines the shape of the gradient
 *          which can be either linear or radial. <br>
 *          So the following types are available:
 *          <ul>
 *              <li>{@link UI.GradientType#LINEAR}</li>
 *              <li>{@link UI.GradientType#RADIAL}</li>
 *          </ul>
 *      </li>
 *      <li><h3>Colors</h3>
 *          An array of colors that will be used
 *          as a basis for the gradient transition.
 *      </li>
 *  </ul>
 *  <p>
 *  You can also use the {@link #none()} method to specify that no gradient should be used,
 *  as the instance returned by that method is a gradient without any colors, effectively
 *  making it a representation of the absence of a gradient style.
 */
public final class GradientStyle
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(GradientStyle.class);
    static final UI.Layer DEFAULT_LAYER = UI.Layer.BACKGROUND;

    private static final GradientStyle _NONE = new GradientStyle(
                                                        UI.Transition.TOP_TO_BOTTOM,
                                                        UI.GradientType.LINEAR,
                                                        new Color[0]
                                                    );

    /**
     *  Use the returned instance as a representation of the absence of a gradient.
     *
     *  @return A gradient without any colors, effectively
     *          representing the absence of a gradient.
     */
    public static GradientStyle none() { return _NONE; }


    private final UI.Transition   _transition;
    private final UI.GradientType _type;
    private final Color[]         _colors;


    private GradientStyle( UI.Transition transition, UI.GradientType type, Color[] colors )
    {
        _transition = Objects.requireNonNull(transition);
        _type       = Objects.requireNonNull(type);
        _colors     = Objects.requireNonNull(colors);
    }

    UI.Transition transition() { return _transition; }

    UI.GradientType type() { return _type; }

    Color[] colors() { return _colors; }

    /**
     *  Define a list of colors which will, as part of the gradient, transition from one
     *  to the next in the order they are specified.
     *  <p>
     *  Note that you need to specify at least two colors for a gradient to be visible.
     *
     * @param colors The colors in the gradient.
     * @return A new gradient style with the specified colors.
     * @throws NullPointerException if any of the colors is {@code null}.
     */
    public GradientStyle colors( Color... colors ) {
        Objects.requireNonNull(colors);
        for ( Color color : colors )
            Objects.requireNonNull(color);
        return new GradientStyle(_transition, _type, colors);
    }

    /**
     *  Define a list of {@link String} based colors which will, as part of the gradient, transition from one
     *  to the next in the order they are specified.
     *  <p>
     *  Note that you need to specify at least two colors for a gradient to be visible.
     *
     * @param colors The colors in the gradient in {@link String} format.
     * @return A new gradient style with the specified colors.
     * @throws NullPointerException if any of the colors is {@code null}.
     */
    public GradientStyle colors( String... colors ) {
        Objects.requireNonNull(colors);
        try {
            Color[] actualColors = new Color[colors.length];
            for ( int i = 0; i < colors.length; i++ )
                actualColors[i] = StyleUtility.toColor(colors[i]);

            return new GradientStyle(_transition, _type, actualColors);
        } catch ( Exception e ) {
            log.error("Failed to parse color strings: " + Arrays.toString(colors), e);
            return this; // We want to avoid side effects other than a wrong color
        }
    }

    /**
     *  Define the alignment of the gradient which is one of the following:
     *  <ul>
     *     <li>{@link UI.Transition#TOP_LEFT_TO_BOTTOM_RIGHT}</li>
     *     <li>{@link UI.Transition#BOTTOM_LEFT_TO_TOP_RIGHT}</li>
     *     <li>{@link UI.Transition#TOP_RIGHT_TO_BOTTOM_LEFT}</li>
     *     <li>{@link UI.Transition#BOTTOM_RIGHT_TO_TOP_LEFT}</li>
     *     <li>{@link UI.Transition#TOP_TO_BOTTOM}</li>
     *     <li>{@link UI.Transition#LEFT_TO_RIGHT}</li>
     *     <li>{@link UI.Transition#BOTTOM_TO_TOP}</li>
     *     <li>{@link UI.Transition#RIGHT_TO_LEFT}</li>
     *  </ul>
     *
     * @param transition The alignment of the gradient.
     * @return A new gradient style with the specified alignment.
     * @throws NullPointerException if the alignment is {@code null}.
     */
    public GradientStyle transition( UI.Transition transition ) {
        Objects.requireNonNull(transition);
        return new GradientStyle(transition, _type, _colors);
    }

    /**
     *  Define the type of the gradient which is one of the following:
     *  <ul>
     *     <li>{@link UI.GradientType#LINEAR}</li>
     *     <li>{@link UI.GradientType#RADIAL}</li>
     *  </ul>
     *
     * @param type The type of the gradient.
     * @return A new gradient style with the specified type.
     * @throws NullPointerException if the type is {@code null}.
     */
    public GradientStyle type( UI.GradientType type ) {
        Objects.requireNonNull(type);
        return new GradientStyle(_transition, type, _colors);
    }

    @Override
    public String toString() {
        if ( this == _NONE )
            return getClass().getSimpleName() + "[NONE]";
        return getClass().getSimpleName() + "[" +
                    "transition=" + _transition + ", " +
                    "type="       + _type + ", " +
                    "colors="     + Arrays.toString(_colors) +
                ']';
    }

    @Override
    public boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( !(o instanceof GradientStyle) ) return false;
        GradientStyle that = (GradientStyle) o;
        return _transition == that._transition       &&
               _type       == that._type             &&
               Arrays.equals(_colors, that._colors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_transition, _type, Arrays.hashCode(_colors));
    }

}
