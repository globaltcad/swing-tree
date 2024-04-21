package swingtree.style;

import org.jspecify.annotations.Nullable;

import java.awt.Insets;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 *  Outline is an immutable value object that represents the outline of a UI component
 *  where every side of the outline can have varying thicknesses and even be completely
 *  optional (null).
 *  <p>
 *  The values of this object are optional in order to determine if the outline
 *  was specified through the styling API or not so that the default properties of a component
 *  can be preserved (the insets of a layout manager, for example).
 */
final class Outline
{
    private static final Outline _NONE = new Outline(null, null, null, null);

    static Outline none() { return _NONE; }

    static Outline of( float top, float right, float bottom, float left ) {
        return new Outline(top, right, bottom, left);
    }

    static Outline of( float topAndBottom, float rightAndLeft ) {
        return new Outline(topAndBottom, rightAndLeft, topAndBottom, rightAndLeft);
    }

    static Outline of( double top, double right, double bottom, double left ) {
        return new Outline((float) top, (float) right, (float) bottom, (float) left);
    }

    static Outline of( float allSides ) {
        return new Outline(allSides, allSides, allSides, allSides);
    }

    static Outline of( Insets insets ) {
        return of(insets.top, insets.right, insets.bottom, insets.left);
    }


    private final @Nullable Float top;
    private final @Nullable Float right;
    private final @Nullable Float bottom;
    private final @Nullable Float left;


    static Outline ofNullable( @Nullable Float top, @Nullable Float right, @Nullable Float bottom, @Nullable Float left ) {
        if ( top == null && right == null && bottom == null && left == null )
            return _NONE;

        return new Outline(top, right, bottom, left);
    }
    
    private Outline( @Nullable Float top, @Nullable Float right, @Nullable Float bottom, @Nullable Float left ) {
        this.top    = top;
        this.right  = right;
        this.bottom = bottom;
        this.left   = left;
    }

    /**
     *  The top outline value in the form of an {@link Optional}, where {@link Optional#empty()}
     *  means that the top outline was not specified.
     *
     * @return An {@link Optional} containing the top outline value if it was specified,
     *        {@link Optional#empty()} otherwise.
     */
    Optional<Float> top() { return Optional.ofNullable(top); }

    /**
     *  An optional value for the right outline.
     *
     * @return An {@link Optional} containing the right outline value if it was specified,
     *        {@link Optional#empty()} otherwise.
     */
    Optional<Float> right() { return Optional.ofNullable(right); }

    /**
     *  The bottom outline value in the form of an {@link Optional}, where {@link Optional#empty()}
     *  means that the bottom outline was not specified.
     *
     * @return An {@link Optional} containing the bottom outline value if it was specified,
     *        {@link Optional#empty()} otherwise.
     */
    Optional<Float> bottom() { return Optional.ofNullable(bottom); }

    /**
     *  Returns an optional value for the left outline where {@link Optional#empty()}
     *  means that the left outline was not specified.
     *
     * @return An {@link Optional} containing the left outline value if it was specified,
     *        {@link Optional#empty()} otherwise.
     */
    Optional<Float> left() { return Optional.ofNullable(left); }

    /**
     *  Creates an updated {@link Outline} with the specified {@code top} outline value.
     *
     * @param top The top outline value.
     * @return A new {@link Outline} with the specified top outline value.
     */
    Outline withTop( float top ) { return Outline.ofNullable(top, right, bottom, left); }

    /**
     *  Creates an updated {@link Outline} with the specified {@code right} outline value.
     *
     * @param right The right outline value.
     * @return A new {@link Outline} with the specified right outline value.
     */
    Outline withRight( float right ) { return Outline.ofNullable(top, right, bottom, left); }

    /**
     *  Creates an updated {@link Outline} with the specified {@code bottom} outline value.
     *
     * @param bottom The bottom outline value.
     * @return A new {@link Outline} with the specified bottom outline value.
     */
    Outline withBottom( float bottom ) { return Outline.ofNullable(top, right, bottom, left); }

    /**
     *  Creates an updated {@link Outline} with the specified {@code left} outline value.
     * @param left The left outline value.
     * @return A new {@link Outline} with the specified left outline value.
     */
    Outline withLeft( float left ) { return Outline.ofNullable(top, right, bottom, left); }

    Outline minus( Outline other ) {
        return Outline.ofNullable(
                    top    == null ? null : top    - (other.top    == null ? 0 : other.top),
                    right  == null ? null : right  - (other.right  == null ? 0 : other.right),
                    bottom == null ? null : bottom - (other.bottom == null ? 0 : other.bottom),
                    left   == null ? null : left   - (other.left   == null ? 0 : other.left)
                );
    }

    /**
     *  An {@link Outline} may be scaled by a factor to increase or decrease the thickness of the outline.
     *  If any of the sides was not specified, it will remain unspecified.
     *
     * @param scale The scale factor.
     * @return A new {@link Outline} with the outline values scaled by the specified factor.
     */
    Outline scale( double scale ) {
        return Outline.ofNullable(
                    top    == null ? null : (float) ( top    * scale ),
                    right  == null ? null : (float) ( right  * scale ),
                    bottom == null ? null : (float) ( bottom * scale ),
                    left   == null ? null : (float) ( left   * scale )
                );
    }

    Outline simplified() {
        if ( this.equals(_NONE) )
            return _NONE;

        Float top    = Objects.equals(this.top   , 0f) ? null : this.top;
        Float right  = Objects.equals(this.right , 0f) ? null : this.right;
        Float bottom = Objects.equals(this.bottom, 0f) ? null : this.bottom;
        Float left   = Objects.equals(this.left  , 0f) ? null : this.left;

        if ( top == null && right == null && bottom == null && left == null )
            return _NONE;

        return Outline.ofNullable(top, right, bottom, left);
    }

    /**
     * @return {@code true} if any of the outline values are not null and positive,
     *         {@code false} otherwise.
     */
    public boolean isPositive() {
        return ( top    != null && top    > 0 ) ||
               ( right  != null && right  > 0 ) ||
               ( bottom != null && bottom > 0 ) ||
               ( left   != null && left   > 0 );
    }

    private static @Nullable Float _plus( @Nullable Float a, @Nullable Float b ) {
        if ( a == null && b == null )
            return null;
        return a == null ? b : b == null ? a : a + b;
    }

    /**
     *  Adds the outline values of this {@link Outline} with the specified {@code other} {@link Outline} values.
     *
     * @param other The other {@link Outline} to merge with.
     * @return A new {@link Outline} with the merged outline values.
     */
    public Outline plus( Outline other ) {
        if ( this.equals(_NONE) )
            return other;
        if ( other.equals(_NONE) )
            return this;

        return Outline.ofNullable(
                    _plus(top,    other.top   ),
                    _plus(right,  other.right ),
                    _plus(bottom, other.bottom),
                    _plus(left,   other.left  )
                );
    }

    public Outline or( Outline other ) {
        if ( this.equals(_NONE) )
            return other;
        if ( other.equals(_NONE) )
            return this;

        return Outline.ofNullable(
                    top    == null ? other.top    : top,
                    right  == null ? other.right  : right,
                    bottom == null ? other.bottom : bottom,
                    left   == null ? other.left   : left
                );
    }

    /**
     *  Maps the outline values of this {@link Outline} using the specified {@code mapper} function.
     *
     * @param mapper The mapper function.
     * @return A new {@link Outline} with the mapped outline values.
     */
    public Outline map( Function<Float, @Nullable Float> mapper ) {
        return Outline.ofNullable(
                    top    == null ? null : mapper.apply(top),
                    right  == null ? null : mapper.apply(right),
                    bottom == null ? null : mapper.apply(bottom),
                    left   == null ? null : mapper.apply(left)
                );
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.top);
        hash = 97 * hash + Objects.hashCode(this.right);
        hash = 97 * hash + Objects.hashCode(this.bottom);
        hash = 97 * hash + Objects.hashCode(this.left);
        return hash;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        Outline rhs = (Outline) obj;
        return Objects.equals(top,    rhs.top   ) &&
               Objects.equals(right,  rhs.right ) &&
               Objects.equals(bottom, rhs.bottom) &&
               Objects.equals(left,   rhs.left  );
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

    private static String _toString( @Nullable Float value ) {
        return value == null ? "?" : value.toString().replace(".0", "");
    }

}
