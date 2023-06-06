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
public final class Outline
{
    private static final Outline _NONE = new Outline(null, null, null, null);

    public static Outline none() { return _NONE; }

    public static Outline of( int top, int right, int bottom, int left ) {
        return new Outline(top, right, bottom, left);
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

    public Optional<Integer> top() { return Optional.ofNullable(top); }

    public Optional<Integer> right() { return Optional.ofNullable(right); }

    public Optional<Integer> bottom() { return Optional.ofNullable(bottom); }

    public Optional<Integer> left() { return Optional.ofNullable(left); }

    Outline top( int top) { return new Outline(top, right, bottom, left); }

    Outline left( int left) { return new Outline(top, right, bottom, left); }

    Outline right( int right) { return new Outline(top, right, bottom, left); }

    Outline bottom( int bottom) { return new Outline(top, right, bottom, left); }

    public boolean isPositive() {
        return top    != null && top    > 0 ||
               right  != null && right  > 0 ||
               bottom != null && bottom > 0 ||
               left   != null && left   > 0;
    }

    public double average() {
        int count = 0;
        int sum   = 0;
        if ( top    != null ) { count++; sum += top;    }
        if ( right  != null ) { count++; sum += right;  }
        if ( bottom != null ) { count++; sum += bottom; }
        if ( left   != null ) { count++; sum += left;   }
        return count == 0 ? 0 : (double) sum / count;
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
        return "Outline[" +
                    "top="    + ( top    == null ? "?" : top    ) + ", " +
                    "right="  + ( right  == null ? "?" : right  ) + ", " +
                    "bottom=" + ( bottom == null ? "?" : bottom ) + ", " +
                    "left="   + ( left   == null ? "?" : left   ) +
                "]";
    }

}