package swingtree.style;

import org.slf4j.Logger;
import swingtree.UI;

import java.awt.Color;
import java.util.Objects;
import java.util.Optional;

/**
 *  An immutable config API
 *  designed for defining shadow styles
 *  as part of the full {@link StyleConf} configuration object.
 *  The state of this object can only be updated by using wither like update methods,
 *  like {@link #horizontalOffset(double)}, {@link #verticalOffset(double)}, {@link #blurRadius(double)}...
 *  which return a new instance of this class with the updated state.
 *  <p>
 *  The following properties with their respective purpose are available:
 *  <br>
 *  <ol>
 *      <li><h3>Horizontal Offset</h3>
 *          <p>
 *              The horizontal shadow offset, if positive the shadow will move to the right,
 *              if negative the shadow will move to the left.
 *          </p>
 *      </li>
 *      <li><h3>Vertical Offset</h3>
 *          <p>
 *              The vertical shadow offset, if positive the shadow will move down,
 *              if negative the shadow will move up.
 *          </p>
 *      </li>
 *      <li><h3>Blur Radius</h3>
 *          <p>
 *              The blur radius of the shadow, which defines the width of the blur effect.
 *              The higher the value, the bigger the blur, so the shadow transition will be
 *              stretched over a wider area.
 *          </p>
 *      </li>
 *      <li><h3>Spread Radius</h3>
 *          <p>
 *              The spread radius of the shadow defines how far inwards or
 *              outwards ({@link #isInset()}) the shadow begins.
 *              This offsets the start of the shadow similarly to the vertical and horizontal
 *              offsets, but instead of moving the shadow, it extends the shadow
 *              so that it either grows or shrinks in size.
 *              <br>
 *              You can imagine a shadow effect as a rectangular box, where the gradients of the shadow
 *              start at the edges of said box. The spread radius then defines the scale of the box,
 *              so that the shadow either grows or shrinks in size.
 *          </p>
 *      </li>
 *      <li><h3>Color</h3>
 *          <p>
 *              The color of the shadow.
 *          </p>
 *      </li>
 *      <li><h3>Inset</h3>
 *          <p>
 *              Whether the shadow is inset or outset.
 *              If true, the shadow is inset, otherwise it is outset.
 *              Inset shadows go inward, starting from the inner edge of the box (and its border),
 *              whereas outset shadows go outward, starting from the outer edge of the box's border.
 *          </p>
 *      </li>
 *  </ol>
 *  <p>
 *  Note that you can use the {@link #none()} method to specify that no shadow should be used,
 *  as the instance returned by that method is a shadow with no offset, no blur, no spread and no color,
 *  effectively making it a representation of the absence of a shadow.
 */
public final class ShadowConf implements Simplifiable<ShadowConf>
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ShadowConf.class);
    static final UI.Layer DEFAULT_LAYER = UI.Layer.CONTENT;

    private static final ShadowConf _NONE = new ShadowConf(
                                                    Offset.none(),0, 0,
                                                    null, true
                                                );

    public static ShadowConf none() { return _NONE; }
    
    static ShadowConf of(
        Offset  offset,
        float   shadowBlurRadius,
        float   shadowSpreadRadius,
        Color   shadowColor,
        boolean isOutset
    ) {
        if ( 
            offset == _NONE._offset &&
            shadowBlurRadius == _NONE._blurRadius &&
            shadowSpreadRadius == _NONE._spreadRadius &&
            shadowColor == _NONE._color &&
            isOutset == _NONE._isOutset
        )
            return _NONE;
        else
            return new ShadowConf(offset, shadowBlurRadius, shadowSpreadRadius, shadowColor, isOutset);
    }

    private final Offset  _offset;
    private final float   _blurRadius;
    private final float   _spreadRadius;
    private final Color   _color;
    private final boolean _isOutset;


    private ShadowConf(
        Offset  offset,
        float   shadowBlurRadius,
        float   shadowSpreadRadius,
        Color   shadowColor,
        boolean isOutset
    ) {
        _offset           = Objects.requireNonNull(offset);
        _blurRadius       = shadowBlurRadius;
        _spreadRadius     = shadowSpreadRadius;
        _color            = shadowColor;
        _isOutset         = isOutset;
    }

    float horizontalOffset() { return _offset.x(); }

    float verticalOffset() { return _offset.y(); }

    float blurRadius() { return _blurRadius; }

    public float spreadRadius() { return _spreadRadius; }

    Optional<Color> color() { return Optional.ofNullable(_color); }

    boolean isOutset() { return _isOutset; }

    boolean isInset() { return !_isOutset; }

    /**
     * @param horizontalShadowOffset The horizontal shadow offset, if positive the shadow will move to the right,
     *                               if negative the shadow will move to the left.
     * @return A new {@link ShadowConf} with the specified horizontal shadow offset.
     */
    public ShadowConf horizontalOffset( double horizontalShadowOffset ) {
        return ShadowConf.of(_offset.withX((float) horizontalShadowOffset), _blurRadius, _spreadRadius, _color, _isOutset);
    }

    /**
     * @param verticalShadowOffset The vertical shadow offset, if positive the shadow will move down,
     *                             if negative the shadow will move up.
     * @return A new {@link ShadowConf} with the specified vertical shadow offset.
     */
    public ShadowConf verticalOffset( double verticalShadowOffset ) {
        return ShadowConf.of(_offset.withY((float) verticalShadowOffset), _blurRadius, _spreadRadius, _color, _isOutset);
    }

    /**
     * @param horizontalShadowOffset The horizontal shadow offset, if positive the shadow will move to the right,
     *                               if negative the shadow will move to the left.
     * @param verticalShadowOffset The vertical shadow offset, if positive the shadow will move down,
     *                             if negative the shadow will move up.
     * @return A new {@link ShadowConf} with the specified horizontal and vertical shadow offsets.
     */
    public ShadowConf offset( double horizontalShadowOffset, double verticalShadowOffset ) {
        return ShadowConf.of(Offset.of(horizontalShadowOffset, verticalShadowOffset), _blurRadius, _spreadRadius, _color, _isOutset);
    }

    /**
     *  Use this to offset the shadow diagonally between the top left corner and the bottom right corner.
     *
     * @param shadowOffset The shadow offset, if positive the shadow will move to the right and down,
     *                     if negative the shadow will move to the left and up.
     * @return A new {@link ShadowConf} with the specified horizontal and vertical shadow offsets.
     */
    public ShadowConf offset( double shadowOffset ) {
        return ShadowConf.of(Offset.of(shadowOffset, shadowOffset), _blurRadius, _spreadRadius, _color, _isOutset);
    }

    /**
     * @param shadowBlurRadius The blur radius of the shadow, which defines the width of the blur effect.
     *                         The higher the value, the bigger the blur, so the shadow transition will be
     *                         stretched over a wider area.
     * @return A new {@link ShadowConf} with the specified blur radius.
     */
    public ShadowConf blurRadius( double shadowBlurRadius ) {
        return ShadowConf.of(_offset, (float) shadowBlurRadius, _spreadRadius, _color, _isOutset);
    }

    /**
     * @param shadowSpreadRadius The spread radius of the shadow, which defines how far the shadow spreads
     *                           outwards or inwards ({@link #isInset()}) from the element.
     *                           This offsets the start of the shadow similarly to the vertical and horizontal
     *                           offsets, but instead of moving the shadow, it extends the shadow
     *                           so that it either grows or shrinks in size.
     * @return A new {@link ShadowConf} with the specified spread radius.
     */
    public ShadowConf spreadRadius( double shadowSpreadRadius ) {
        return ShadowConf.of(_offset, _blurRadius, (float) shadowSpreadRadius, _color, _isOutset);
    }

    /**
     * @param shadowColor The color of the shadow.
     * @return A new {@link ShadowConf} with the specified color.
     */
    public ShadowConf color( Color shadowColor ) {
        Objects.requireNonNull(shadowColor, "Use UI.COLOR_UNDEFINED to specify no color instead of null");
        if ( shadowColor == UI.COLOR_UNDEFINED)
            shadowColor = null;
        if ( shadowColor == _color )
            return this;
        return ShadowConf.of(_offset, _blurRadius, _spreadRadius, shadowColor, _isOutset);
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
     * @return A new {@link ShadowConf} with the specified color.
     */
    public ShadowConf color( String shadowColor ) {
        Objects.requireNonNull(shadowColor);
        Color newColor;
        try {
            newColor = UI.color(shadowColor);
        } catch ( Exception e ) {
            log.error("Failed to parse color string: '{}'", shadowColor, e);
            return this; // We want to avoid side effects other than a wrong color
        }
        return ShadowConf.of(_offset, _blurRadius, _spreadRadius, newColor, _isOutset);
    }

    /**
     * @param red The red component of the shadow color.
     * @param green The green component of the shadow color.
     * @param blue The blue component of the shadow color.
     * @return A new {@link ShadowConf} with the specified color.
     */
    public ShadowConf color( double red, double green, double blue ) {
        return color(red, green, blue, 1.0);
    }

    /**
     * @param red The red component of the shadow color.
     * @param green The green component of the shadow color.
     * @param blue The blue component of the shadow color.
     * @param alpha The alpha component of the shadow color.
     * @return A new {@link ShadowConf} with the specified color.
     */
    public ShadowConf color( double red, double green, double blue, double alpha ) {
        return color(new Color((float) red, (float) green, (float) blue, (float) alpha));
    }

    /**
     * @param shadowInset Whether the shadow is inset or outset.
     *                    If true, the shadow is inset, otherwise it is outset.
     *                    Inset shadows go inward, starting from the inner edge of the box (and its border),
     *                    whereas outset shadows go outward, starting from the outer edge of the box's border.
     * @return A new {@link ShadowConf} with the specified inset/outset state.
     */
    public ShadowConf isInset(boolean shadowInset ) {
        return ShadowConf.of(_offset, _blurRadius, _spreadRadius, _color, !shadowInset);
    }

    /**
     * @param shadowOutset Whether the shadow is outset or inset.
     *                     If true, the shadow is outset, otherwise it is inset.
     *                     Outset shadows go outward, starting from the outer edge of the box's border,
     *                     whereas inset shadows go inward, starting from the inner edge of the box (and its border).
     * @return A new {@link ShadowConf} with the specified outset/inset state.
     */
    public ShadowConf isOutset( boolean shadowOutset ) {
        return ShadowConf.of(_offset, _blurRadius, _spreadRadius, _color, shadowOutset);
    }

    ShadowConf _scale( double scaleFactor ) {
        return ShadowConf.of(
                            _offset.scale(scaleFactor),
                            (float) (_blurRadius * scaleFactor),
                            (float) (_spreadRadius * scaleFactor),
                            _color,
                            _isOutset
                        );
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + _offset.hashCode();
        hash = 31 * hash + Float.hashCode(_blurRadius);
        hash = 31 * hash + Float.hashCode(_spreadRadius);
        hash = 31 * hash + Objects.hashCode(_color);
        hash = 31 * hash + (_isOutset ? 1 : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        ShadowConf rhs = (ShadowConf) obj;
        return Objects.equals(_offset, rhs._offset)       &&
               _blurRadius       == rhs._blurRadius       &&
               _spreadRadius     == rhs._spreadRadius     &&
               Objects.equals(_color, rhs._color)         &&
               _isOutset         == rhs._isOutset;
    }

    @Override
    public String toString() {
        if ( this == _NONE )
            return this.getClass().getSimpleName()+"[NONE]";
        return this.getClass().getSimpleName()+"[" +
                    "horizontalOffset=" + _toString(_offset.x()  ) + ", " +
                    "verticalOffset="   + _toString(_offset.y()  ) + ", " +
                    "blurRadius="       + _toString(_blurRadius  ) + ", " +
                    "spreadRadius="     + _toString(_spreadRadius) + ", " +
                    "color="            + StyleUtil.toString(_color) + ", " +
                    "isInset="          + !_isOutset +
                "]";
    }

    private static String _toString( float value ) {
        return value == 0 ? "0" : String.valueOf(value).replace(".0", "");
    }

    @Override
    public ShadowConf simplified() {
        if ( this == _NONE )
            return _NONE;

        if ( _color == null || _color.getAlpha() == 0 )
            return _NONE;

        if ( _color == UI.COLOR_UNDEFINED)
            return _NONE;

        return this;
    }
}
