package swingtree.style;

import java.awt.*;
import java.util.Arrays;
import java.util.Objects;

public final class GradientStyle
{
    private static final GradientStyle _NONE = new GradientStyle(GradientAlignment.NONE, new Color[0], Layer.BACKGROUND);


    public static GradientStyle none() { return _NONE; }

    private final GradientAlignment _gradientAlignment;
    private final Color[] _colors;
    private final Layer _layer;


    public GradientStyle( GradientAlignment gradientAlignment, Color[] colors, Layer layer )
    {
        _gradientAlignment = Objects.requireNonNull(gradientAlignment);
        _colors          = Objects.requireNonNull(colors);
        _layer           = Objects.requireNonNull(layer);
    }

    public GradientAlignment align() { return _gradientAlignment; }

    public Color[] colors() { return _colors; }

    public GradientStyle colors( Color... colors ) { return new GradientStyle(_gradientAlignment, colors, _layer); }

    public GradientStyle align( GradientAlignment gradientAlignment ) { return new GradientStyle(gradientAlignment, _colors, _layer); }

    public Layer layer() { return _layer; }

    public GradientStyle layer( Layer layer ) { return new GradientStyle(_gradientAlignment, _colors, layer); }


    @Override
    public String toString() {
        return "ShadeStyle[" +
                    "strategy=" + _gradientAlignment + ", " +
                    "colors="   + Arrays.toString(_colors) + ", " +
                    "layer="    + _layer +
                ']';
    }

    @Override
    public boolean equals(Object o) {
        if ( this == o ) return true;
        if ( !(o instanceof GradientStyle) ) return false;
        GradientStyle that = (GradientStyle) o;
        return _gradientAlignment == that._gradientAlignment &&
               Arrays.equals(_colors, that._colors)      &&
               _layer == that._layer;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(_gradientAlignment);
        result = 31 * result + Arrays.hashCode(_colors);
        result = 31 * result + Objects.hash(_layer);
        return result;
    }

}
