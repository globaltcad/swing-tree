package swingtree.style;

import java.awt.*;
import java.util.Objects;
import java.util.Optional;

public class BorderStyle
{
    private final int   _borderArcWidth;
    private final int   _borderArcHeight;
    private final int   _borderWidth;
    private final Color _borderColor;

    public BorderStyle(
        int borderArcWidth,
        int borderArcHeight,
        int borderWidth,
        Color borderColor
    ) {
        _borderArcWidth  = borderArcWidth;
        _borderArcHeight = borderArcHeight;
        _borderWidth     = borderWidth;
        _borderColor     = borderColor;
    }

    public int arcWidth() { return _borderArcWidth; }

    public int arcHeight() { return _borderArcHeight; }

    public int thickness() { return _borderWidth; }

    public Optional<Color> color() { return Optional.ofNullable(_borderColor); }

    public BorderStyle withArcWidth( int borderArcWidth ) { return new BorderStyle(borderArcWidth, _borderArcHeight, _borderWidth, _borderColor); }

    public BorderStyle withArcHeight( int borderArcHeight ) { return new BorderStyle(_borderArcWidth, borderArcHeight, _borderWidth, _borderColor); }

    public BorderStyle withWidth( int borderThickness ) { return new BorderStyle(_borderArcWidth, _borderArcHeight, borderThickness, _borderColor); }

    public BorderStyle withColor( Color borderColor ) { return new BorderStyle(_borderArcWidth, _borderArcHeight, _borderWidth, borderColor); }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + _borderArcWidth;
        hash = 31 * hash + _borderArcHeight;
        hash = 31 * hash + _borderWidth;
        hash = 31 * hash + Objects.hashCode(_borderColor);
        return hash;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        BorderStyle rhs = (BorderStyle) obj;
        return _borderArcWidth  == rhs._borderArcWidth  &&
               _borderArcHeight == rhs._borderArcHeight &&
               _borderWidth     == rhs._borderWidth     &&
               _borderColor.equals(rhs._borderColor);
    }

    @Override
    public String toString() {
        boolean arcWidthEqualsHeight = _borderArcWidth == _borderArcHeight;
        return "BorderStyle[" +
                    (
                        arcWidthEqualsHeight
                            ? "radius=" + _borderArcWidth
                            : "arcWidth=" + _borderArcWidth + ", arcHeight=" + _borderArcHeight
                    ) +
                    ", width=" + _borderWidth +
                    ", color=" + StyleUtility.toString(_borderColor) +
                "]";
    }
}
