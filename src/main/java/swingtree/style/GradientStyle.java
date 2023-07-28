package swingtree.style;

import java.awt.*;
import java.util.Arrays;
import java.util.Objects;

public final class GradientStyle
{
    private static final GradientStyle _NONE = new GradientStyle(GradientAlignment.TOP_TO_BOTTOM, GradientType.LINEAR, new Color[0], Layer.BACKGROUND);


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

    public GradientAlignment align() { return _gradientAlignment; }

    public GradientType type() { return _gradientType; }

    public Color[] colors() { return _colors; }

    Layer layer() { return _layer; }

    public GradientStyle colors( Color... colors ) { return new GradientStyle(_gradientAlignment, _gradientType, colors, _layer); }

    public GradientStyle align( GradientAlignment gradientAlignment ) { return new GradientStyle(gradientAlignment, _gradientType, _colors, _layer); }

    public GradientStyle type( GradientType type ) { return new GradientStyle(_gradientAlignment, type, _colors, _layer); }

    public GradientStyle layer( Layer layer ) { return new GradientStyle(_gradientAlignment, _gradientType, _colors, layer); }


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
               Arrays.equals(_colors, that._colors)      &&
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
