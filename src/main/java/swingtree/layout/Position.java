package swingtree.layout;

import com.google.errorprone.annotations.Immutable;

import java.awt.Point;
import java.util.Objects;

/**
 *  An immutable value based class that represents a location in float based two-dimensional
 *  coordinate system specified in float precision and specifically designed
 *  for Swing components.
 *  It can be used as an alternative to the AWT {@link Point} class,
 *  but in situations where immutability is desired (which should be most cases).
 *  <p>
 *  Use the {@link #of(double, double)} factory method to create a new instance
 *  or {@link #withX(double)} and {@link #withY(double)} to create a new instance
 *  with a modified value.
 */
@Immutable
public final class Position
{
    private final static Position ORIGIN = new Position( 0, 0 );

    /**
     *  A factory method that creates a new location with the specified x- and y-coordinates
     *  or returns the {@link #origin()} constant if both coordinates are zero.
     *
     * @param x The x-coordinate of the location to create.
     * @param y The y-coordinate of the location to create.
     * @return A new location with the specified x- and y-coordinates.
     *         If both coordinates are zero, the {@link #origin()} is returned.
     */
    public static Position of( double x, double y ) {
        if ( x == 0 && y == 0 )
            return ORIGIN;

        return new Position((float) x, (float) y);
    }

    /**
     *  A factory method that creates a new location from the supplied AWT {@link Point}.
     *  If the point is null, a {@link NullPointerException} is thrown.
     *
     * @param p The point to create a location from.
     * @return A new location with the x- and y-coordinates of the specified point.
     *         If both coordinates are zero, the {@link #origin()} is returned.
     */
    public static Position of( Point p ) {
        Objects.requireNonNull(p);
        return of( p.x, p.y );
    }

    public static Position origin() {
        return ORIGIN;
    }

    final float _x;
    final float _y;


    private Position( float x, float y ) {
        _x = x;
        _y = y;
    }

    /**
     *  Exposes the x coordinate of this location in the form of a float,
     *  which describes the horizontal position in a two-dimensional coordinate system.
     *  So the larger the x value of the location, the further to the right it is.
     *
     * @return The x-coordinate of this location.
     */
    public float x() {
        return _x;
    }

    /**
     *  Exposes the y coordinate of this location in the form of a float,
     *  which describes the vertical position in a two-dimensional coordinate system.
     *  So the larger the y value of the location, the further down it is.
     *
     * @return The y-coordinate of this location.
     */
    public float y() {
        return _y;
    }

    /**
     *  Allows you to create an updated version of this location with the
     *  specified y-coordinate and the same x-coordinate
     *  as this location instance.
     *
     * @param y The y-coordinate of the location to create.
     * @return A new location with the same x-coordinate as this location
     *         and the specified y-coordinate.
     */
    public Position withY( double y ) {
        return of( _x, y );
    }

    /**
     *  Allows you to create an updated version of this location with the
     *  specified x-coordinate and the same y-coordinate
     *  as this location instance.
     *
     * @param x The x-coordinate of the location to create.
     * @return A new location with the same y-coordinate as this location
     *         and the specified x-coordinate.
     */
    public Position withX( double x ) {
        return of( x, _y );
    }

    /**
     *  Creates a new location where the specified {@code dx} and
     *  {@code dy} values are added to the x- and y-coordinates of this location.
     *
     * @param dx The amount to increase the x-coordinate by.
     * @param dy The amount to increase the y-coordinate by.
     * @return A new location with the x- and y-coordinates of this location
     *         increased by the specified values.
     */
    public Position plus( double dx, double dy ) {
        return of( _x + dx, _y + dy );
    }

    /**
     *  Creates a new location where the x- and y-coordinates of the specified
     *  {@link Position} are added to the x- and y-coordinates of this location.
     *
     * @param other The location to add to this location.
     * @return A new location with the x- and y-coordinates of this location
     *         increased by the x- and y-coordinates of the specified location.
     */
    public Position plus( Position other ) {
        return of( _x + other._x, _y + other._y );
    }

    /**
     *  Creates a new location where the specified {@code dx} and
     *  {@code dy} values are subtracted from the x- and y-coordinates of this location.
     *
     * @param dx The amount to decrease the x-coordinate by.
     * @param dy The amount to decrease the y-coordinate by.
     * @return A new location with the x- and y-coordinates of this location
     *         decreased by the specified values.
     */
    public Position minus( double dx, double dy ) {
        return of( _x - dx, _y - dy );
    }

    /**
     *  Creates a new location where the x- and y-coordinates of the specified
     *  {@link Position} are subtracted from the x- and y-coordinates of this location.
     *
     * @param other The location to subtract from this location.
     * @return A new location with the x- and y-coordinates of this location
     *         decreased by the x- and y-coordinates of the specified location.
     */
    public Position minus(Position other ) {
        return of( _x - other._x, _y - other._y );
    }

    /**
     *  A {@link Position} consists of two x and y coordinates in 2D space, which is
     *  why this convenience method allows you to transform this
     *  {@link Position} object to an AWT {@link Point}.
     *
     * @return A new AWT {@link Point} with the same x- and y-coordinates as this location.
     */
    public Point toPoint() {
        return new Point( (int) _x, (int) _y);
    }


    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"[" +
                    "x=" + _x + ", "+
                    "y=" + _y +
                "]";
    }

    @Override
    public boolean equals( Object o ) {
        if ( o == this ) return true;
        if ( o == null ) return false;
        if ( o.getClass() != this.getClass() ) return false;
        Position other = (Position) o;
        return _x == other._x && _y == other._y;
    }

    @Override
    public int hashCode() {
        return Float.hashCode(_x) ^ Float.hashCode(_y);
    }

}
