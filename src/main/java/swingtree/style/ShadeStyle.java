package swingtree.style;

import java.awt.*;
import java.util.Arrays;
import java.util.Objects;

public final class ShadeStyle
{
    private static final ShadeStyle _NONE = new ShadeStyle(ShadingStrategy.NONE, new Color[0], Layer.BACKGROUND);


    public static ShadeStyle none() { return _NONE; }

    private final ShadingStrategy _shadingStrategy;
    private final Color[] _colors;
    private final Layer _layer;


    public ShadeStyle(ShadingStrategy shadingStrategy, Color[] colors, Layer layer)
    {
        _shadingStrategy = Objects.requireNonNull(shadingStrategy);
        _colors          = Objects.requireNonNull(colors);
        _layer           = Objects.requireNonNull(layer);
    }

    public ShadingStrategy strategy() { return _shadingStrategy; }

    public Color[] colors() { return _colors; }

    public ShadeStyle colors( Color... colors ) { return new ShadeStyle(_shadingStrategy, colors, _layer); }

    public ShadeStyle strategy(ShadingStrategy shadingStrategy) { return new ShadeStyle(shadingStrategy, _colors, _layer); }

    public Layer layer() { return _layer; }

    public ShadeStyle layer(Layer layer) { return new ShadeStyle(_shadingStrategy, _colors, layer); }


    @Override
    public String toString() {
        return "ShadeStyle[" +
                    "strategy=" + _shadingStrategy         + ", " +
                    "colors="   + Arrays.toString(_colors) + ", " +
                    "layer="    + _layer +
                ']';
    }

    @Override
    public boolean equals(Object o) {
        if ( this == o ) return true;
        if ( !(o instanceof ShadeStyle) ) return false;
        ShadeStyle that = (ShadeStyle) o;
        return _shadingStrategy == that._shadingStrategy &&
               Arrays.equals(_colors, that._colors)      &&
               _layer == that._layer;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(_shadingStrategy);
        result = 31 * result + Arrays.hashCode(_colors);
        result = 31 * result + Objects.hash(_layer);
        return result;
    }

}
