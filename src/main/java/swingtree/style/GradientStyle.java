package swingtree.style;

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
    private static final GradientStyle _NONE = new GradientStyle(GradientAlignment.TOP_TO_BOTTOM, GradientType.LINEAR, new Color[0], Layer.BACKGROUND);

    /**
     *  Use the returned instance as a representation of the absence of a gradient.
     *
     *  @return A gradient without any colors, effectively
     *          representing the absence of a gradient.
     */
    public static GradientStyle none() { return _NONE; }

    private final GradientAlignment _gradientAlignment;
    private final GradientType _gradientType;
    private final Color[] _colors;
    private final Layer _layer;


    public GradientStyle( GradientAlignment gradientAlignment, GradientType type, Color[] colors, Layer layer )
    {
        _gradientAlignment = Objects.requireNonNull(gradientAlignment);
        _gradientType      = Objects.requireNonNull(type);
        _colors            = Objects.requireNonNull(colors);
        _layer             = Objects.requireNonNull(layer);
    }

    GradientAlignment align() { return _gradientAlignment; }

    GradientType type() { return _gradientType; }

    Color[] colors() { return _colors; }

    Layer layer() { return _layer; }

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
     *     <li>{@link GradientAlignment#TOP_LEFT_TO_BOTTOM_RIGHT}</li>
     *     <li>{@link GradientAlignment#BOTTOM_LEFT_TO_TOP_RIGHT}</li>
     *     <li>{@link GradientAlignment#TOP_RIGHT_TO_BOTTOM_LEFT}</li>
     *     <li>{@link GradientAlignment#BOTTOM_RIGHT_TO_TOP_LEFT}</li>
     *     <li>{@link GradientAlignment#TOP_TO_BOTTOM}</li>
     *     <li>{@link GradientAlignment#LEFT_TO_RIGHT}</li>
     *     <li>{@link GradientAlignment#BOTTOM_TO_TOP}</li>
     *     <li>{@link GradientAlignment#RIGHT_TO_LEFT}</li>
     *  </ul>
     *
     * @param gradientAlignment The alignment of the gradient.
     * @return A new gradient style with the specified alignment.
     * @throws NullPointerException if the alignment is {@code null}.
     */
    public GradientStyle align( GradientAlignment gradientAlignment ) {
        Objects.requireNonNull(gradientAlignment);
        return new GradientStyle(gradientAlignment, _gradientType, _colors, _layer);
    }

    /**
     *  Define the type of the gradient which is one of the following:
     *  <ul>
     *     <li>{@link GradientType#LINEAR}</li>
     *     <li>{@link GradientType#RADIAL}</li>
     *  </ul>
     *
     * @param type The type of the gradient.
     * @return A new gradient style with the specified type.
     * @throws NullPointerException if the type is {@code null}.
     */
    public GradientStyle type( GradientType type ) {
        Objects.requireNonNull(type);
        return new GradientStyle(_gradientAlignment, type, _colors, _layer);
    }

    /**
     *  Define the layer of the gradient which is one of the following:
     *  <ul>
     *     <li>{@link Layer#BACKGROUND}</li>
     *     <li>{@link Layer#BORDER}</li>
     *  </ul>
     *
     * @param layer The layer of the gradient.
     * @return A new gradient style with the specified layer.
     * @throws NullPointerException if the layer is {@code null}.
     */
    public GradientStyle layer( Layer layer ) {
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
    public boolean equals(Object o) {
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
