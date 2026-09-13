package swingtree.style;

import com.google.errorprone.annotations.Immutable;
import org.jspecify.annotations.Nullable;

import java.awt.Insets;
import java.util.Optional;
import java.util.function.Function;

/**
 *  OptionalInsets is an immutable value object holding four inset thicknesses, one for each
 *  side of a UI component, where any side may also be left unspecified. Unlike
 *  {@link java.awt.Insets}, a side is a {@code float} and does not have to have a value.
 *  <p>
 *  A side is optional in order to tell "the styling API asked for this thickness" apart from
 *  "the styling API said nothing about this side", so that a component's own defaults - the
 *  insets a layout manager wants, for example - survive being styled.
 *  <p>
 *  <b>A side is stored as a primitive float, and {@link Float#NaN} is what "unspecified"
 *  means.</b> The obvious spelling of an optional side is a boxed {@code Float}, and it is the
 *  wrong one here: a box model holds four of these objects - margin, padding, border widths
 *  and the insets a former border leaves behind - and they are built for every layer of every
 *  style gathered on every paint of every component, and {@code Float.valueOf} has no cache,
 *  so each side used to be a heap allocation in a former version of this class. Sides are
 *  compared by their bit patterns rather than with {@code ==}, which keeps every distinction
 *  {@code Float.equals} used to make - {@code 0.0} apart from {@code -0.0}, and one unspecified
 *  side equal to another. The one consequence is that a side which really is {@code NaN} now
 *  reads as unspecified instead of propagating a {@code NaN} into a layout.
 */
@Immutable
final class OptionalInsets
{
    /** What a side holds when the styling API said nothing about it. */
    private static final float UNSPECIFIED = Float.NaN;

    private static final OptionalInsets _NONE = new OptionalInsets(UNSPECIFIED, UNSPECIFIED, UNSPECIFIED, UNSPECIFIED);

    static OptionalInsets none() { return _NONE; }

    static OptionalInsets of( float top, float right, float bottom, float left ) {
        return new OptionalInsets(top, right, bottom, left);
    }

    static OptionalInsets of( float topAndBottom, float rightAndLeft ) {
        return new OptionalInsets(topAndBottom, rightAndLeft, topAndBottom, rightAndLeft);
    }

    static OptionalInsets of( double top, double right, double bottom, double left ) {
        return new OptionalInsets((float) top, (float) right, (float) bottom, (float) left);
    }

    static OptionalInsets of( float allSides ) {
        return new OptionalInsets(allSides, allSides, allSides, allSides);
    }

    static OptionalInsets of( Insets insets ) {
        return of(insets.top, insets.right, insets.bottom, insets.left);
    }


    private final float top;
    private final float right;
    private final float bottom;
    private final float left;


    private static OptionalInsets _of( float top, float right, float bottom, float left ) {
        if ( _isUnset(top) && _isUnset(right) && _isUnset(bottom) && _isUnset(left) )
            return _NONE;

        return new OptionalInsets(top, right, bottom, left);
    }

    private static OptionalInsets _ofNullable( @Nullable Float top, @Nullable Float right, @Nullable Float bottom, @Nullable Float left ) {
        return _of(_unbox(top), _unbox(right), _unbox(bottom), _unbox(left));
    }

    private static float _unbox( @Nullable Float value ) {
        return value == null ? UNSPECIFIED : value;
    }

    private static boolean _isUnset( float value ) {
        return Float.isNaN(value);
    }

    /**
     *  Whether two sides are the same side, which for an unspecified one means both are
     *  unspecified. Comparing the bit patterns rather than the numbers is what keeps
     *  {@code 0.0} and {@code -0.0} distinct, exactly as boxed {@code Float} equality did.
     *
     * @param a one side
     * @param b the other side
     * @return true when the two sides carry the same value, or are both unspecified
     */
    private static boolean _same( float a, float b ) {
        return Float.floatToIntBits(a) == Float.floatToIntBits(b);
    }

    private OptionalInsets( float top, float right, float bottom, float left ) {
        this.top    = top;
        this.right  = right;
        this.bottom = bottom;
        this.left   = left;
    }

    /**
     *  The top inset in the form of an {@link Optional}, where {@link Optional#empty()}
     *  means that the top inset was not specified.
     *
     * @return An {@link Optional} containing the top inset if it was specified,
     *        {@link Optional#empty()} otherwise.
     */
    Optional<Float> top() { return _isUnset(top) ? Optional.empty() : Optional.of(top); }

    /**
     *  An optional value for the right inset.
     *
     * @return An {@link Optional} containing the right inset if it was specified,
     *        {@link Optional#empty()} otherwise.
     */
    Optional<Float> right() { return _isUnset(right) ? Optional.empty() : Optional.of(right); }

    /**
     *  The bottom inset in the form of an {@link Optional}, where {@link Optional#empty()}
     *  means that the bottom inset was not specified.
     *
     * @return An {@link Optional} containing the bottom inset if it was specified,
     *        {@link Optional#empty()} otherwise.
     */
    Optional<Float> bottom() { return _isUnset(bottom) ? Optional.empty() : Optional.of(bottom); }

    /**
     *  Returns an optional value for the left inset where {@link Optional#empty()}
     *  means that the left inset was not specified.
     *
     * @return An {@link Optional} containing the left inset if it was specified,
     *        {@link Optional#empty()} otherwise.
     */
    Optional<Float> left() { return _isUnset(left) ? Optional.empty() : Optional.of(left); }

    /**
     *  Creates an updated {@link OptionalInsets} with the specified {@code top} inset.
     *
     * @param top The top inset.
     * @return A new {@link OptionalInsets} with the specified top inset.
     */
    OptionalInsets withTop( float top ) { return _of(top, right, bottom, left); }

    /**
     *  Creates an updated {@link OptionalInsets} with the specified {@code right} inset.
     *
     * @param right The right inset.
     * @return A new {@link OptionalInsets} with the specified right inset.
     */
    OptionalInsets withRight( float right ) { return _of(top, right, bottom, left); }

    /**
     *  Creates an updated {@link OptionalInsets} with the specified {@code bottom} inset.
     *
     * @param bottom The bottom inset.
     * @return A new {@link OptionalInsets} with the specified bottom inset.
     */
    OptionalInsets withBottom( float bottom ) { return _of(top, right, bottom, left); }

    /**
     *  Creates an updated {@link OptionalInsets} with the specified {@code left} inset.
     * @param left The left inset.
     * @return A new {@link OptionalInsets} with the specified left inset.
     */
    OptionalInsets withLeft( float left ) { return _of(top, right, bottom, left); }

    OptionalInsets minus( OptionalInsets other ) {
        return _of(
                    _minus(top,    other.top   ),
                    _minus(right,  other.right ),
                    _minus(bottom, other.bottom),
                    _minus(left,   other.left  )
                );
    }

    private static float _minus( float a, float b ) {
        if ( _isUnset(a) )
            return UNSPECIFIED;
        return _isUnset(b) ? a : a - b;
    }

    /**
     *  An {@link OptionalInsets} may be scaled by a factor to increase or decrease the thickness of every side.
     *  If any of the sides was not specified, it will remain unspecified.
     *
     * @param scale The scale factor.
     * @return A new {@link OptionalInsets} with the insets scaled by the specified factor.
     */
    OptionalInsets scale( double scale ) {
        return _of(
                    _isUnset(top)    ? UNSPECIFIED : (float) ( top    * scale ),
                    _isUnset(right)  ? UNSPECIFIED : (float) ( right  * scale ),
                    _isUnset(bottom) ? UNSPECIFIED : (float) ( bottom * scale ),
                    _isUnset(left)   ? UNSPECIFIED : (float) ( left   * scale )
                );
    }

    OptionalInsets simplified() {
        if ( this.equals(_NONE) )
            return _NONE;

        return _of(
                    _same(this.top   , 0f) ? UNSPECIFIED : this.top,
                    _same(this.right , 0f) ? UNSPECIFIED : this.right,
                    _same(this.bottom, 0f) ? UNSPECIFIED : this.bottom,
                    _same(this.left  , 0f) ? UNSPECIFIED : this.left
                );
    }

    /**
     *  Determines if any of the sides is specified and positive,
     *  which means that these insets take up room in the component's
     *  appearance or layout.
     *
     * @return {@code true} if any of the sides is specified and positive,
     *         {@code false} otherwise.
     */
    public boolean isPositive() {
        // An unspecified side is NaN, and no comparison against NaN is ever true:
        return top > 0 || right > 0 || bottom > 0 || left > 0;
    }

    private static float _plus( float a, float b ) {
        if ( _isUnset(a) )
            return b;
        return _isUnset(b) ? a : a + b;
    }

    /**
     *  Adds the sides of this {@link OptionalInsets} to the sides of the specified {@code other} {@link OptionalInsets}.
     *
     * @param other The other {@link OptionalInsets} to merge with.
     * @return A new {@link OptionalInsets} with the summed insets.
     */
    public OptionalInsets plus( OptionalInsets other ) {
        if ( this.equals(_NONE) )
            return other;
        if ( other.equals(_NONE) )
            return this;

        return _of(
                    _plus(top,    other.top   ),
                    _plus(right,  other.right ),
                    _plus(bottom, other.bottom),
                    _plus(left,   other.left  )
                );
    }

    static OptionalInsets roundingCorrectionOf( OptionalInsets first, OptionalInsets second, OptionalInsets third ) {
        return _of(
                    _roundingCorrectionOf(_plus(_plus(first.top,    second.top   ), third.top   )),
                    _roundingCorrectionOf(_plus(_plus(first.right,  second.right ), third.right )),
                    _roundingCorrectionOf(_plus(_plus(first.bottom, second.bottom), third.bottom)),
                    _roundingCorrectionOf(_plus(_plus(first.left,   second.left  ), third.left  ))
                );
    }

    private static float _roundingCorrectionOf( float sum ) {
        if ( !(sum > 0f) )
            return UNSPECIFIED;
        float fraction = sum - (float) Math.floor(sum);
        return fraction > 0f ? 1f - fraction : UNSPECIFIED;
    }

    public OptionalInsets or( OptionalInsets other ) {
        if ( this.equals(_NONE) )
            return other;
        if ( other.equals(_NONE) )
            return this;

        return _of(
                    _isUnset(top)    ? other.top    : top,
                    _isUnset(right)  ? other.right  : right,
                    _isUnset(bottom) ? other.bottom : bottom,
                    _isUnset(left)   ? other.left   : left
                );
    }

    /**
     *  Maps the specified sides of this {@link OptionalInsets} using the specified {@code mapper} function.
     *
     * @param mapper The mapper function.
     * @return A new {@link OptionalInsets} with the mapped insets.
     */
    public OptionalInsets map( Function<Float, @Nullable Float> mapper ) {
        return _ofNullable(
                    _isUnset(top)    ? null : mapper.apply(top),
                    _isUnset(right)  ? null : mapper.apply(right),
                    _isUnset(bottom) ? null : mapper.apply(bottom),
                    _isUnset(left)   ? null : mapper.apply(left)
                );
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Float.floatToIntBits(this.top);
        hash = 97 * hash + Float.floatToIntBits(this.right);
        hash = 97 * hash + Float.floatToIntBits(this.bottom);
        hash = 97 * hash + Float.floatToIntBits(this.left);
        return hash;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        OptionalInsets rhs = (OptionalInsets) obj;
        return _same(top,    rhs.top   ) &&
               _same(right,  rhs.right ) &&
               _same(bottom, rhs.bottom) &&
               _same(left,   rhs.left  );
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" +
                    "top="    + _toString( top    ) + ", " +
                    "right="  + _toString( right  ) + ", " +
                    "bottom=" + _toString( bottom ) + ", " +
                    "left="   + _toString( left   ) +
                "]";
    }

    private static String _toString( float value ) {
        return _isUnset(value) ? "?" : String.valueOf(value).replace(".0", "");
    }

}
