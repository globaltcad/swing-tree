package swingtree.style;

import java.util.Objects;
import java.util.Optional;

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

    public static Outline none() { return _NONE; }

    public static Outline of( int top, int right, int bottom, int left ) {
        return new Outline(top, right, bottom, left);
    }

    public static Outline of( int allSides ) {
        return new Outline(allSides, allSides, allSides, allSides);
    }


    private final Integer top;
    private final Integer right;
    private final Integer bottom;
    private final Integer left;

    private Outline( Integer top, Integer right, Integer bottom, Integer left ) {
        this.top    = top;
        this.right  = right;
        this.bottom = bottom;
        this.left   = left;
    }

    /**
     * @return An {@link Optional} containing the top outline value if it was specified,
     *        {@link Optional#empty()} otherwise.
     */
    Optional<Integer> top() { return Optional.ofNullable(top); }

    /**
     * @return An {@link Optional} containing the right outline value if it was specified,
     *        {@link Optional#empty()} otherwise.
     */
    Optional<Integer> right() { return Optional.ofNullable(right); }

    /**
     * @return An {@link Optional} containing the bottom outline value if it was specified,
     *        {@link Optional#empty()} otherwise.
     */
    Optional<Integer> bottom() { return Optional.ofNullable(bottom); }

    /**
     * @return An {@link Optional} containing the left outline value if it was specified,
     *        {@link Optional#empty()} otherwise.
     */
    Optional<Integer> left() { return Optional.ofNullable(left); }

    /**
     * @param top The top outline value.
     * @return A new {@link Outline} with the specified top outline value.
     */
    Outline withTop( int top ) { return new Outline(top, right, bottom, left); }

    /**
     * @param right The right outline value.
     * @return A new {@link Outline} with the specified right outline value.
     */
    Outline withRight( int right ) { return new Outline(top, right, bottom, left); }

    /**
     * @param bottom The bottom outline value.
     * @return A new {@link Outline} with the specified bottom outline value.
     */
    Outline withBottom( int bottom ) { return new Outline(top, right, bottom, left); }

    /**
     * @param left The left outline value.
     * @return A new {@link Outline} with the specified left outline value.
     */
    Outline withLeft( int left ) { return new Outline(top, right, bottom, left); }

    /**
     * @param scale The scale factor.
     * @return A new {@link Outline} with the outline values scaled by the specified factor.
     */
    Outline scale( double scale ) {
        return new Outline(
                    top    == null ? null : (int) Math.round( top    * scale ),
                    right  == null ? null : (int) Math.round( right  * scale ),
                    bottom == null ? null : (int) Math.round( bottom * scale ),
                    left   == null ? null : (int) Math.round( left   * scale )
                );
    }

    Outline simplified() {
        if ( this == _NONE )
            return _NONE;

        Integer top    = Objects.equals(this.top   , 0) ? null : this.top;
        Integer right  = Objects.equals(this.right , 0) ? null : this.right;
        Integer bottom = Objects.equals(this.bottom, 0) ? null : this.bottom;
        Integer left   = Objects.equals(this.left  , 0) ? null : this.left;

        if ( top == null && right == null && bottom == null && left == null )
            return _NONE;

        return new Outline(top, right, bottom, left);
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

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + ( top    == null ? 1 : top    );
        hash = 31 * hash + ( right  == null ? 2 : right  );
        hash = 31 * hash + ( bottom == null ? 3 : bottom );
        hash = 31 * hash + ( left   == null ? 4 : left   );
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
                    "top="    + ( top    == null ? "?" : top    ) + ", " +
                    "right="  + ( right  == null ? "?" : right  ) + ", " +
                    "bottom=" + ( bottom == null ? "?" : bottom ) + ", " +
                    "left="   + ( left   == null ? "?" : left   ) +
                "]";
    }

}
