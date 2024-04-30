package swingtree.style;

import com.google.errorprone.annotations.Immutable;

import java.util.Objects;

/**
 *  An immutable value object that represents a 2D scale
 *  in the form of x and y scale values or lack thereof (1, 1).
 */
@Immutable
final class Scale
{
    private static final Scale _NONE = new Scale(1, 1);

    public static Scale none() { return _NONE; }

    public static Scale of( float x, float y ) {
        if ( x == 1 && y == 1 )
            return _NONE;

        x = Math.max(0, x);
        y = Math.max(0, y);

        return new Scale(x, y);
    }

    public static Scale of(double x, double y ) {
        return Scale.of((float) x, (float) y);
    }


    private final float _x;
    private final float _y;


    private Scale(float x, float y ) {
        _x = x;
        _y = y;
    }

    float x() { return _x; }

    float y() { return _y; }

    Scale withX(float x ) { return Scale.of(x, _y); }

    Scale withY(float y ) { return Scale.of(_x, y); }

    Scale scale(double scaleFactor ) {
        return Scale.of((int) Math.round(_x * scaleFactor), (int) Math.round(_y * scaleFactor));
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"[" +
                    "x=" + _toString(_x) + ", "+
                    "y=" + _toString(_y) +
                "]";
    }

    private static String _toString( float value ) {
        return String.valueOf(value).replace(".0", "");
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
        Scale rhs = (Scale) obj;
        return _x == rhs._x && _y == rhs._y;
    }
}
