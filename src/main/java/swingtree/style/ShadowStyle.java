package swingtree.style;

import java.awt.*;
import java.util.Objects;
import java.util.Optional;

public final class ShadowStyle
{
    private static final ShadowStyle _NONE = new ShadowStyle(0,0,0,0, null, true);

    public static ShadowStyle none() { return _NONE; }

    private final int _horizontalOffset;
    private final int _verticalOffset;
    private final int _blurRadius;
    private final int _spreadRadius;
    private final Color _color;
    private final boolean _isOutset;

    private ShadowStyle(
        int horizontalShadowOffset,
        int verticalShadowOffset,
        int shadowBlurRadius,
        int shadowSpreadRadius,
        Color shadowColor,
        boolean isInset
    ) {
        _horizontalOffset = horizontalShadowOffset;
        _verticalOffset = verticalShadowOffset;
        _blurRadius = shadowBlurRadius;
        _spreadRadius = shadowSpreadRadius;
        _color = shadowColor;
        _isOutset = !isInset;
    }

    public int horizontalOffset() { return _horizontalOffset; }

    public int verticalOffset() { return _verticalOffset; }

    public int blurRadius() { return _blurRadius; }

    public int spreadRadius() { return _spreadRadius; }

    public Optional<Color> color() { return Optional.ofNullable(_color); }

    public boolean isOutset() { return _isOutset; }

    public boolean isInset() { return !_isOutset; }

    public ShadowStyle horizontalOffset(int horizontalShadowOffset) { return new ShadowStyle(horizontalShadowOffset, _verticalOffset, _blurRadius, _spreadRadius, _color, _isOutset); }

    public ShadowStyle verticalOffset(int verticalShadowOffset) { return new ShadowStyle(_horizontalOffset, verticalShadowOffset, _blurRadius, _spreadRadius, _color, _isOutset); }

    public ShadowStyle offset(int horizontalShadowOffset, int verticalShadowOffset) { return new ShadowStyle(horizontalShadowOffset, verticalShadowOffset, _blurRadius, _spreadRadius, _color, _isOutset); }

    public ShadowStyle offset(int shadowOffset) { return new ShadowStyle(shadowOffset, shadowOffset, _blurRadius, _spreadRadius, _color, _isOutset); }

    public ShadowStyle blurRadius(int shadowBlurRadius) { return new ShadowStyle(_horizontalOffset, _verticalOffset, shadowBlurRadius, _spreadRadius, _color, _isOutset); }

    public ShadowStyle spreadRadius(int shadowSpreadRadius) { return new ShadowStyle(_horizontalOffset, _verticalOffset, _blurRadius, shadowSpreadRadius, _color, _isOutset); }

    public ShadowStyle color(Color shadowColor) { return new ShadowStyle(_horizontalOffset, _verticalOffset, _blurRadius, _spreadRadius, shadowColor, _isOutset); }

    public ShadowStyle color(String shadowColor) { return new ShadowStyle(_horizontalOffset, _verticalOffset, _blurRadius, _spreadRadius, StyleUtility.toColor(shadowColor), _isOutset); }

    public ShadowStyle isInset(boolean shadowInset) { return new ShadowStyle(_horizontalOffset, _verticalOffset, _blurRadius, _spreadRadius, _color, shadowInset); }

    public ShadowStyle isOutset(boolean shadowOutset) { return new ShadowStyle(_horizontalOffset, _verticalOffset, _blurRadius, _spreadRadius, _color, !shadowOutset); }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + _horizontalOffset;
        hash = 31 * hash + _verticalOffset;
        hash = 31 * hash + _blurRadius;
        hash = 31 * hash + _spreadRadius;
        hash = 31 * hash + Objects.hashCode(_color);
        hash = 31 * hash + (_isOutset ? 1 : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        ShadowStyle rhs = (ShadowStyle) obj;
        return _horizontalOffset == rhs._horizontalOffset &&
               _verticalOffset == rhs._verticalOffset &&
               _blurRadius == rhs._blurRadius &&
               _spreadRadius == rhs._spreadRadius &&
               Objects.equals(_color, rhs._color)         &&
               _isOutset == rhs._isOutset;
    }

    @Override
    public String toString() {
        return "ShadowStyle[" +
                    "horizontalOffset=" + _horizontalOffset + ", " +
                    "verticalOffset="   + _verticalOffset   + ", " +
                    "blurRadius="       + _blurRadius       + ", " +
                    "spreadRadius="     + _spreadRadius     + ", " +
                    "color="            + StyleUtility.toString(_color) + ", " +
                    "isInset="          + !_isOutset +
                "]";
    }

}
