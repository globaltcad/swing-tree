package swingtree.layout;

import java.awt.Point;

/**
 *  An immutable value class that represents a location in a two-dimensional
 *  coordinate system specified in integer precision and specifically designed
 *  for Swing components.
 *  It can be used as an alternative to the AWT {@link Point} class,
 *  but in situations where immutability is desired.
 *  <p>
 *  Use the {@link #of(int, int)} factory method to create a new instance
 *  or {@link #withX(int)} and {@link #withY(int)} to create a new instance
 *  with a modified value.
 */
public final class Location
{
    private final static Location ORIGIN = new Location( 0, 0 );

    /**
     * @return A new location with the specified x- and y-coordinates.
     *         If both coordinates are zero, the {@link #origin()} is returned.
     */
    public static Location of( int x, int y ) {
        if ( x == 0 && y == 0 )
            return ORIGIN;

        return new Location(x, y);
    }

    /**
     * @return A new location with the x- and y-coordinates of the specified point.
     *         If both coordinates are zero, the {@link #origin()} is returned.
     */
    public static Location of( Point p ) {
        return of( p.x, p.y );
    }

    public static Location origin() {
        return ORIGIN;
    }

    final int _x;
    final int _y;


    private Location( int x, int y ) {
        _x = x;
        _y = y;
    }

    /**
     * @return The x-coordinate of this location.
     */
    public int x() {
        return _x;
    }

    /**
     * @return The y-coordinate of this location.
     */
    public int y() {
        return _y;
    }

    /**
     * @return A new location with the same x-coordinate as this location
     *         and the specified y-coordinate.
     */
    public Location withY( int y ) {
        return of( _x, y );
    }

    /**
     * @return A new location with the same y-coordinate as this location
     *         and the specified x-coordinate.
     */
    public Location withX( int x ) {
        return of( x, _y );
    }

    /**
     * @return A new AWT {@link Point} with the same x- and y-coordinates as this location.
     */
    public Point toPoint() {
        return new Point( _x, _y );
    }

}
