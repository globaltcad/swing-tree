package swingtree.style;

import java.awt.*;
import java.util.Arrays;
import java.util.Objects;

public class ShadeStyle
{
    private static final ShadeStyle _NONE = new ShadeStyle(ShadingStrategy.TOP_LEFT_TO_BOTTOM_RIGHT, new Color[0]);


    public static ShadeStyle none() { return _NONE; }

    private final ShadingStrategy _shadingStrategy;
    private final Color[] _colors;


    public ShadeStyle(ShadingStrategy shadingStrategy, Color... colors)
    {
        _shadingStrategy = Objects.requireNonNull(shadingStrategy);
        _colors = Objects.requireNonNull(colors);
    }

    public ShadingStrategy shade() { return _shadingStrategy; }

    public Color[] colors() { return _colors; }

    public ShadeStyle colors( Color... colors ) { return new ShadeStyle(_shadingStrategy, colors); }

    public ShadeStyle shade( ShadingStrategy shadingStrategy) { return new ShadeStyle(shadingStrategy, _colors); }


    @Override
    public String toString() {
        return "ShadeStyle[" +
            "strategy=" + _shadingStrategy +
            ", colors=" + Arrays.toString(_colors) +
            ']';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShadeStyle)) return false;
        ShadeStyle that = (ShadeStyle) o;
        return _shadingStrategy == that._shadingStrategy &&
            Arrays.equals(_colors, that._colors);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(_shadingStrategy);
        result = 31 * result + Arrays.hashCode(_colors);
        return result;
    }

}
