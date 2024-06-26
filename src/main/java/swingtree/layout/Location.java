package swingtree.layout;

import com.google.errorprone.annotations.Immutable;

import java.awt.Point;
import java.util.Objects;

/**
 *  An immutable value based class that represents a location in a two-dimensional
 *  coordinate system specified in float precision and specifically designed
 *  for Swing components.
 *  It can be used as an alternative to the AWT {@link Point} class,
 *  but in situations where immutability is desired (which should be most cases).
 *  <p>
 *  Use the {@link #of(float, float)} factory method to create a new instance
 *  or {@link #withX(int)} and {@link #withY(int)} to create a new instance
 *  with a modified value.
 */
@Immutable
public final class Location
{
    private final static Location ORIGIN = new Location( 0, 0 );

    /**
     *  A factory method that creates a new location with the specified x- and y-coordinates
     *  or returns the {@link #origin()} constant if both coordinates are zero.
     *
     * @param x The x-coordinate of the location to create.
     * @param y The y-coordinate of the location to create.
     * @return A new location with the specified x- and y-coordinates.
     *         If both coordinates are zero, the {@link #origin()} is returned.
     */
    public static Location of( float x, float y ) {
        if ( x == 0 && y == 0 )
            return ORIGIN;

        return new Location(x, y);
    }

    /**
     *  A factory method that creates a new location from the supplied AWT {@link Point}.
     *  If the point is null, a {@link NullPointerException} is thrown.
     *
     * @param p The point to create a location from.
     * @return A new location with the x- and y-coordinates of the specified point.
     *         If both coordinates are zero, the {@link #origin()} is returned.
     */
    public static Location of( Point p ) {
        Objects.requireNonNull(p);
        return of( p.x, p.y );
    }

    public static Location origin() {
        return ORIGIN;
    }

    final float _x;
    final float _y;


    private Location( float x, float y ) {
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
    public Location withY( int y ) {
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
    public Location withX( int x ) {
        return of( x, _y );
    }

    /**
     *  A {@link Location} consists of two x and y coordinates in 2D space, which is
     *  why this convenience method allows you to transform this
     *  {@link Location} object to an AWT {@link Point}.
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
        Location other = (Location) o;
        return _x == other._x && _y == other._y;
    }

    @Override
    public int hashCode() {
        return Float.hashCode(_x) ^ Float.hashCode(_y);
    }

}
