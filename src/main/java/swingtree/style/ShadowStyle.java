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

    /**
     * @param horizontalShadowOffset The horizontal shadow offset, if positive the shadow will move to the right,
     *                               if negative the shadow will move to the left.
     * @return A new {@link ShadowStyle} with the specified horizontal shadow offset.
     */
    public ShadowStyle horizontalOffset( int horizontalShadowOffset ) {
        return new ShadowStyle(horizontalShadowOffset, _verticalOffset, _blurRadius, _spreadRadius, _color, _isOutset);
    }

    /**
     * @param verticalShadowOffset The vertical shadow offset, if positive the shadow will move down,
     *                             if negative the shadow will move up.
     * @return A new {@link ShadowStyle} with the specified vertical shadow offset.
     */
    public ShadowStyle verticalOffset( int verticalShadowOffset ) {
        return new ShadowStyle(_horizontalOffset, verticalShadowOffset, _blurRadius, _spreadRadius, _color, _isOutset);
    }

    /**
     * @param horizontalShadowOffset The horizontal shadow offset, if positive the shadow will move to the right,
     *                               if negative the shadow will move to the left.
     * @param verticalShadowOffset The vertical shadow offset, if positive the shadow will move down,
     *                             if negative the shadow will move up.
     * @return A new {@link ShadowStyle} with the specified horizontal and vertical shadow offsets.
     */
    public ShadowStyle offset( int horizontalShadowOffset, int verticalShadowOffset ) {
        return new ShadowStyle(horizontalShadowOffset, verticalShadowOffset, _blurRadius, _spreadRadius, _color, _isOutset);
    }

    /**
     *  Use this to offset the shadow diagonally between the top left corner and the bottom right corner.
     *
     * @param shadowOffset The shadow offset, if positive the shadow will move to the right and down,
     *                     if negative the shadow will move to the left and up.
     * @return A new {@link ShadowStyle} with the specified horizontal and vertical shadow offsets.
     */
    public ShadowStyle offset( int shadowOffset ) {
        return new ShadowStyle(shadowOffset, shadowOffset, _blurRadius, _spreadRadius, _color, _isOutset);
    }

    /**
     * @param shadowBlurRadius The blur radius of the shadow, which defines the width of the blur effect.
     *                         The higher the value, the bigger the blur, so the shadow transition will be
     *                         stretched over a wider area.
     * @return A new {@link ShadowStyle} with the specified blur radius.
     */
    public ShadowStyle blurRadius( int shadowBlurRadius ) {
        return new ShadowStyle(_horizontalOffset, _verticalOffset, shadowBlurRadius, _spreadRadius, _color, _isOutset);
    }

    /**
     * @param shadowSpreadRadius The spread radius of the shadow, which defines how far the shadow spreads
     *                           outwards or inwards ({@link #isInset()}) from the element.
     *                           This offsets the start of the shadow similarly to the vertical and horizontal
     *                           offsets, but instead of moving the shadow, it extends the shadow
     *                           so that it either grows or shrinks in size.
     * @return A new {@link ShadowStyle} with the specified spread radius.
     */
    public ShadowStyle spreadRadius( int shadowSpreadRadius ) {
        return new ShadowStyle(_horizontalOffset, _verticalOffset, _blurRadius, shadowSpreadRadius, _color, _isOutset);
    }

    /**
     * @param shadowColor The color of the shadow.
     * @return A new {@link ShadowStyle} with the specified color.
     */
    public ShadowStyle color( Color shadowColor ) {
        return new ShadowStyle(_horizontalOffset, _verticalOffset, _blurRadius, _spreadRadius, shadowColor, _isOutset);
    }

    /**
     * @param shadowColor The color of the shadow in the form of a String.
     *                    The color can be specified in the following formats:
     *                    <ul>
     *                      <li>HTML color name - like "red"</li>
     *                      <li>Hexadecimal RGB value - like "#ff0000"</li>
     *                      <li>Hexadecimal RGBA value - like "#ff0000ff"</li>
     *                      <li>RGB value - like "rgb(255, 0, 0)"</li>
     *                      <li>RGBA value - like "rgba(255, 0, 0, 1.0)"</li>
     *                      <li>HSB value - like "hsb(0, 100%, 100%)"</li>
     *                      <li>HSBA value - like "hsba(0, 100%, 100%, 1.0)"</li>
     *                    </ul>
     * @return A new {@link ShadowStyle} with the specified color.
     */
    public ShadowStyle color( String shadowColor ) {
        return new ShadowStyle(_horizontalOffset, _verticalOffset, _blurRadius, _spreadRadius, StyleUtility.toColor(shadowColor), _isOutset);
    }

    /**
     * @param shadowInset Whether the shadow is inset or outset.
     *                    If true, the shadow is inset, otherwise it is outset.
     *                    Inset shadows go inward, starting from the inner edge of the box (and its border),
     *                    whereas outset shadows go outward, starting from the outer edge of the box's border.
     * @return A new {@link ShadowStyle} with the specified inset/outset state.
     */
    public ShadowStyle isInset( boolean shadowInset ) { return new ShadowStyle(_horizontalOffset, _verticalOffset, _blurRadius, _spreadRadius, _color, shadowInset); }

    /**
     * @param shadowOutset Whether the shadow is outset or inset.
     *                     If true, the shadow is outset, otherwise it is inset.
     *                     Outset shadows go outward, starting from the outer edge of the box's border,
     *                     whereas inset shadows go inward, starting from the inner edge of the box (and its border).
     * @return A new {@link ShadowStyle} with the specified outset/inset state.
     */
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
