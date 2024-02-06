package swingtree.style;

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


    private final Float top;
    private final Float right;
    private final Float bottom;
    private final Float left;


    static Outline ofNullable( Float top, Float right, Float bottom, Float left ) {
        if ( top == null && right == null && bottom == null && left == null )
            return _NONE;

        return new Outline(top, right, bottom, left);
    }
    
    private Outline( Float top, Float right, Float bottom, Float left ) {
        this.top    = top;
        this.right  = right;
        this.bottom = bottom;
        this.left   = left;
    }

    /**
     * @return An {@link Optional} containing the top outline value if it was specified,
     *        {@link Optional#empty()} otherwise.
     */
    Optional<Float> top() { return Optional.ofNullable(top); }

    /**
     * @return An {@link Optional} containing the right outline value if it was specified,
     *        {@link Optional#empty()} otherwise.
     */
    Optional<Float> right() { return Optional.ofNullable(right); }

    /**
     * @return An {@link Optional} containing the bottom outline value if it was specified,
     *        {@link Optional#empty()} otherwise.
     */
    Optional<Float> bottom() { return Optional.ofNullable(bottom); }

    /**
     * @return An {@link Optional} containing the left outline value if it was specified,
     *        {@link Optional#empty()} otherwise.
     */
    Optional<Float> left() { return Optional.ofNullable(left); }

    /**
     * @param top The top outline value.
     * @return A new {@link Outline} with the specified top outline value.
     */
    Outline withTop( float top ) { return Outline.ofNullable(top, right, bottom, left); }

    /**
     * @param right The right outline value.
     * @return A new {@link Outline} with the specified right outline value.
     */
    Outline withRight( float right ) { return Outline.ofNullable(top, right, bottom, left); }

    /**
     * @param bottom The bottom outline value.
     * @return A new {@link Outline} with the specified bottom outline value.
     */
    Outline withBottom( float bottom ) { return Outline.ofNullable(top, right, bottom, left); }

    /**
     * @param left The left outline value.
     * @return A new {@link Outline} with the specified left outline value.
     */
    Outline withLeft( float left ) { return Outline.ofNullable(top, right, bottom, left); }

    /**
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
        return top    != null && top    > 0 ||
               right  != null && right  > 0 ||
               bottom != null && bottom > 0 ||
               left   != null && left   > 0;
    }

    private static Float _plus( Float a, Float b ) {
        if ( a == null && b == null )
            return null;
        return a == null ? b : b == null ? a : a + b;
    }

    public Outline plus( Outline other ) {
        if ( this == _NONE )
            return other;
        if ( other == _NONE )
            return this;

        return Outline.ofNullable(
                    _plus(top,    other.top   ),
                    _plus(right,  other.right ),
                    _plus(bottom, other.bottom),
                    _plus(left,   other.left  )
                );
    }

    public Outline map( Function<Float, Float> mapper ) {
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

    private static String _toString( Float value ) {
        return value == null ? "?" : value.toString().replace(".0", "");
    }

}
