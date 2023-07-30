package swingtree.style;

import swingtree.UI;

import java.awt.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

/**
 *  Use this class to specify a gradient style for various sub styles,
 *  like for example {@link BackgroundStyle} or {@link BorderStyle} through the
 *  {@link StyleDelegate#gradient(String, Function)} or {@link StyleDelegate#borderGradient(String, Function)}
 *  methods.
 *  <p>
 *  Note that you can use the {@link #none()} method to specify that no gradient should be used,
 *  as the instance returned by that method is a gradient without any colors, effectively
 *  making it a representation of the absence of a gradient.
 */
public final class GradientStyle
{
    private static final GradientStyle _NONE = new GradientStyle(UI.Transition.TOP_TO_BOTTOM, UI.GradientType.LINEAR, new Color[0], UI.Layer.BACKGROUND);

    /**
     *  Use the returned instance as a representation of the absence of a gradient.
     *
     *  @return A gradient without any colors, effectively
     *          representing the absence of a gradient.
     */
    public static GradientStyle none() { return _NONE; }

    private final UI.Transition _gradientAlignment;
    private final UI.GradientType _gradientType;
    private final Color[] _colors;
    private final UI.Layer _layer;


    public GradientStyle(UI.Transition gradientAlignment, UI.GradientType type, Color[] colors, UI.Layer layer )
    {
        _gradientAlignment = Objects.requireNonNull(gradientAlignment);
        _gradientType      = Objects.requireNonNull(type);
        _colors            = Objects.requireNonNull(colors);
        _layer             = Objects.requireNonNull(layer);
    }

    UI.Transition align() { return _gradientAlignment; }

    UI.GradientType type() { return _gradientType; }

    Color[] colors() { return _colors; }

    UI.Layer layer() { return _layer; }

    /**
     *  Define a list of colors which will, as art of the gradient, transition from one
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
        return new GradientStyle(_gradientAlignment, _gradientType, colors, _layer);
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
     * @param gradientAlignment The alignment of the gradient.
     * @return A new gradient style with the specified alignment.
     * @throws NullPointerException if the alignment is {@code null}.
     */
    public GradientStyle align( UI.Transition gradientAlignment ) {
        Objects.requireNonNull(gradientAlignment);
        return new GradientStyle(gradientAlignment, _gradientType, _colors, _layer);
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
        return new GradientStyle(_gradientAlignment, type, _colors, _layer);
    }

    /**
     *  Define the layer of the gradient which is one of the following:
     *  <ul>
     *     <li>{@link UI.Layer#BACKGROUND}</li>
     *     <li>{@link UI.Layer#BORDER}</li>
     *  </ul>
     *
     * @param layer The layer of the gradient.
     * @return A new gradient style with the specified layer.
     * @throws NullPointerException if the layer is {@code null}.
     */
    public GradientStyle layer( UI.Layer layer ) {
        Objects.requireNonNull(layer);
        return new GradientStyle(_gradientAlignment, _gradientType, _colors, layer);
    }


    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" +
                    "alignment=" + _gradientAlignment       + ", " +
                    "type="      + _gradientType            + ", " +
                    "colors="    + Arrays.toString(_colors) + ", " +
                    "layer="     + _layer                   +
                ']';
    }

    @Override
    public boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( !(o instanceof GradientStyle) ) return false;
        GradientStyle that = (GradientStyle) o;
        return _gradientAlignment == that._gradientAlignment &&
               _gradientType      == that._gradientType      &&
               Arrays.equals(_colors, that._colors)          &&
               _layer == that._layer;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(_gradientAlignment);
        result = 31 * result + Objects.hash(_gradientType);
        result = 31 * result + Arrays.hashCode(_colors);
        result = 31 * result + Objects.hash(_layer);
        return result;
    }

}
