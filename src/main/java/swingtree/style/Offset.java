package swingtree.style;

import java.util.Objects;

/**
 *  An immutable value object that represents an offset from a position
 *  in the form of an x and y offset or lack thereof (0, 0).
 */
final class Offset
{
    private static final Offset _NONE = new Offset(0, 0);

    public static Offset none() { return _NONE; }

    public static Offset of( float x, float y ) { return new Offset(x, y); }


    private final float _x;
    private final float _y;


    private Offset( float x, float y ) {
        _x = x;
        _y = y;
    }

    float x() { return _x; }

    float y() { return _y; }

    Offset withX( float x ) { return new Offset(x, _y); }

    Offset withY( float y ) { return new Offset(_x, y); }

    Offset scale( double scaleFactor ) {
        return new Offset((int) Math.round(_x * scaleFactor), (int) Math.round(_y * scaleFactor));
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"[" +
                    "x=" + _x +", "+
                    "y=" + _y +
                "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(_x, _y);
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        Offset rhs = (Offset) obj;
        return _x == rhs._x && _y == rhs._y;
    }
}
