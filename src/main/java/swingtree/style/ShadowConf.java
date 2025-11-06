package swingtree.style;

import com.google.errorprone.annotations.Immutable;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import swingtree.SwingTree;
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
 *      <li><b>Horizontal Offset</b>
 *          <p>
 *              The horizontal shadow offset, if positive the shadow will move to the right,
 *              if negative the shadow will move to the left.
 *          </p>
 *      </li>
 *      <li><b>Vertical Offset</b>
 *          <p>
 *              The vertical shadow offset, if positive the shadow will move down,
 *              if negative the shadow will move up.
 *          </p>
 *      </li>
 *      <li><b>Blur Radius</b>
 *          <p>
 *              The blur radius of the shadow, which defines the width of the blur effect.
 *              The higher the value, the bigger the blur, so the shadow transition will be
 *              stretched over a wider area.
 *          </p>
 *      </li>
 *      <li><b>Spread Radius</b>
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
 *      <li><b>Color</b>
 *          <p>
 *              The color of the shadow.
 *          </p>
 *      </li>
 *      <li><b>Inset</b>
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
@Immutable
@SuppressWarnings("ReferenceEquality")
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
        Offset          offset,
        float           shadowBlurRadius,
        float           shadowSpreadRadius,
        @Nullable Color shadowColor,
        boolean         isOutset
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

    private final Offset          _offset;
    private final float           _blurRadius;
    private final float           _spreadRadius;
    private final @Nullable Color _color;
    private final boolean         _isOutset;


    private ShadowConf(
        Offset          offset,
        float           shadowBlurRadius,
        float           shadowSpreadRadius,
        @Nullable Color shadowColor,
        boolean         isOutset
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
     *  Use this to offset the shadow position along the X axis.
     *  If the {@code horizontalShadowOffset} is positive, the shadow will move to the right,
     *  if negative the shadow will move to the left.
     *
     * @param horizontalShadowOffset The horizontal shadow offset, if positive the shadow will move to the right,
     *                               if negative the shadow will move to the left.
     * @return A new {@link ShadowConf} with the specified horizontal shadow offset.
     */
    public ShadowConf horizontalOffset( double horizontalShadowOffset ) {
        return ShadowConf.of(_offset.withX((float) horizontalShadowOffset), _blurRadius, _spreadRadius, _color, _isOutset);
    }

    /**
     *  Defines the shadow position along the Y axis in terms of the "vertical shadow offset".
     *  It will move the shadow up if negative and down if positive.
     *
     * @param verticalShadowOffset The vertical shadow offset, if positive the shadow will move down,
     *                             if negative the shadow will move up.
     * @return A new {@link ShadowConf} with the specified vertical shadow offset.
     */
    public ShadowConf verticalOffset( double verticalShadowOffset ) {
        return ShadowConf.of(_offset.withY((float) verticalShadowOffset), _blurRadius, _spreadRadius, _color, _isOutset);
    }

    /**
     *  Use this to offset the shadow position along the X or Y axis
     *  using the two supplied {@code horizontalShadowOffset} and {@code verticalShadowOffset} doubles.
     *  The {@code horizontalShadowOffset} will shift the shadow along the X axis,
     *  while the {@code verticalShadowOffset} will shift the shadow along the Y axis.
     *
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
     *  This is effectively a diagonal shadow offset as it is applied to both the X and Y axis.
     *  (see {@link #offset(double, double)} for more information)
     *
     * @param shadowOffset The shadow offset, if positive the shadow will move to the right and down,
     *                     if negative the shadow will move to the left and up.
     * @return A new {@link ShadowConf} with the specified horizontal and vertical shadow offsets.
     */
    public ShadowConf offset( double shadowOffset ) {
        return ShadowConf.of(Offset.of(shadowOffset, shadowOffset), _blurRadius, _spreadRadius, _color, _isOutset);
    }

    /**
     *  The blur radius of a shadow defines the gap size between the start and end of the shadow gradient.
     *  The higher the value, the bigger the blur, so the shadow transition will extend further
     *  inwards or outwards ({@link #isInset()}) from the shadow center.
     *
     * @param shadowBlurRadius The blur radius of the shadow, which defines the width of the blur effect.
     *                         The higher the value, the bigger the blur, so the shadow transition will be
     *                         stretched over a wider area.
     * @return A new {@link ShadowConf} with the specified blur radius.
     */
    public ShadowConf blurRadius( double shadowBlurRadius ) {
        return ShadowConf.of(_offset, (float) shadowBlurRadius, _spreadRadius, _color, _isOutset);
    }

    /**
     *  The spread radius of a shadow is a sort of scale for the shadow box.
     *  So when the spread radius is large the shadow will both begin and  end further away from the shadow center.
     *  When the spread radius is small the shadow will be more concentrated around the shadow center.
     *
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
     *  Use this to define the color of the visible shadow gradient.
     * @param shadowColor The color of the shadow.
     * @return A new {@link ShadowConf} with the specified color.
     */
    public ShadowConf color( Color shadowColor ) {
        Objects.requireNonNull(shadowColor, "Use UI.Color.UNDEFINED to specify no color instead of null");
        if ( shadowColor == UI.Color.UNDEFINED)
            shadowColor = null;
        if ( Objects.equals(shadowColor, _color) )
            return this;
        return ShadowConf.of(_offset, _blurRadius, _spreadRadius, shadowColor, _isOutset);
    }

    /**
     *  Updates the color of the shadow using a color string
     *  which can be specified in various formats.
     *  (see {@link UI#color(String)} for more information)
     *
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
            log.error(SwingTree.get().loggingMarker(), "Failed to parse color string: '{}'", shadowColor, e);
            return this; // We want to avoid side effects other than a wrong color
        }
        return ShadowConf.of(_offset, _blurRadius, _spreadRadius, newColor, _isOutset);
    }

    /**
     *  Use this to define the color of the visible shadow gradient
     *  in terms of the red, green and blue components
     *  consisting of three double values ranging from 0.0 to 1.0,
     *  where 0.0 represents the absence of the color component
     *  and 1.0 represents the color component at full strength.
     *
     * @param red The red component of the shadow color.
     * @param green The green component of the shadow color.
     * @param blue The blue component of the shadow color.
     * @return A new {@link ShadowConf} with the specified color.
     */
    public ShadowConf color( double red, double green, double blue ) {
        return color(red, green, blue, 1.0);
    }

    /**
     *  Use this to define the color of the visible shadow gradient
     *  in terms of the red, green, blue and alpha components
     *  consisting of four double values ranging from 0.0 to 1.0,
     *  where 0.0 represents the absence of the color component
     *  and 1.0 represents the color component at full strength.
     *
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
     *  The {@code isInset} parameter determines whether the shadow is inset or outset,
     *  in terms of the direction of the shadow gradient either going inward or outward.
     *  If the {@code isInset} parameter is true, the shadow color will fade away
     *  towards the center of the shadow box, whereas if the {@code isInset} parameter is false,
     *  the shadow color will fade away towards the outer edge of the shadow box. <br>
     *  This is essentially the inverse of the {@link #isOutset(boolean)} method.
     *
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
     *  Use this to define whether the shadow is outset or inset,
     *  which will determine the direction of the shadow gradient.
     *  If the {@code isOutset} parameter is true, the shadow gradient
     *  color will fade away towards the outer edge of the shadow box.
     *  If the {@code isOutset} parameter is false on the other hand,
     *  the shadow color fades away towards the center of the shadow box. <br>
     *  This is essentially the inverse of the {@link #isInset(boolean)} method.
     *
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
        if ( this.equals(_NONE) )
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
        if ( this.equals(_NONE) )
            return _NONE;

        if ( _color == null || _color.getAlpha() == 0 )
            return _NONE;

        if ( _color == UI.Color.UNDEFINED)
            return _NONE;

        return this;
    }
}
