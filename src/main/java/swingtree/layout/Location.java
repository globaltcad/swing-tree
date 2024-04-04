package swingtree.layout;

import java.awt.Point;

/**
 *  An immutable value class that represents a location in a two-dimensional
 *  coordinate system specified in float precision and specifically designed
 *  for Swing components.
 *  It can be used as an alternative to the AWT {@link Point} class,
 *  but in situations where immutability is desired.
 *  <p>
 *  Use the {@link #of(float, float)} factory method to create a new instance
 *  or {@link #withX(int)} and {@link #withY(int)} to create a new instance
 *  with a modified value.
 */
public final class Location
{
    private final static Location ORIGIN = new Location( 0, 0 );

    /**
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
     * @param p The point to create a location from.
     * @return A new location with the x- and y-coordinates of the specified point.
     *         If both coordinates are zero, the {@link #origin()} is returned.
     */
    public static Location of( Point p ) {
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
     * @return The x-coordinate of this location.
     */
    public float x() {
        return _x;
    }

    /**
     * @return The y-coordinate of this location.
     */
    public float y() {
        return _y;
    }

    /**
     * @param y The y-coordinate of the location to create.
     * @return A new location with the same x-coordinate as this location
     *         and the specified y-coordinate.
     */
    public Location withY( int y ) {
        return of( _x, y );
    }

    /**
     * @param x The x-coordinate of the location to create.
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
