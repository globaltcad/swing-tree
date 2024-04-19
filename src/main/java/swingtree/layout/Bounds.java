package swingtree.layout;

import java.awt.Rectangle;
import java.util.Objects;

/**
 *  An immutable value object that represents the position and size of a component
 *  in the form of an x and y coordinate modeled by a {@link Location} object
 *  and a width and height modeled by a {@link Size} object.
 *  Note the rectangular bounds object is positioned in a coordinate system
 *  where the y-axis is growing positively downwards and the x-axis is growing
 *  positively to the right.
 *  <p>
 *  Also note that the {@link #equals(Object)} and {@link #hashCode()} methods
 *  are implemented to compare the {@link Location} and {@link Size} objects
 *  for value based equality instead of reference based equality.
 */
public final class Bounds
{
    private final static Bounds EMPTY = new Bounds(Location.origin(), Size.unknown());

    /**
     *  Returns an empty bounds object, which is the null object for this class.
     *  <p>
     *  The returned bounds object has a location of {@link Location#origin()}
     *  and a size of {@link Size#unknown()}.
     *
     *  @return an empty bounds object that is the null object for this class.
     */
    public static Bounds none() {
        return EMPTY;
    }

    private final Location _location;
    private final Size     _size;

    /**
     *  Returns a bounds object with the specified location and size.
     *  <p>
     *  If the location is {@link Location#origin()} and the size is
     *  {@link Size#unknown()} then the {@link #none()} object is returned.
     *
     *  @param location the location of the bounds object.
     *  @param size the size of the bounds object.
     *  @return a bounds object with the specified location and size.
     */
    public static Bounds of( Location location, Size size ) {
        Objects.requireNonNull(location);
        Objects.requireNonNull(size);
        if ( location.equals(Location.origin()) && size.equals(Size.unknown()) )
            return EMPTY;

        return new Bounds(location, size);
    }

    /**
     *  Returns a bounds object with the specified location and size
     *  in the form of x and y coordinates, width and height.
     *  <p>
     *  If the width or height is less than zero then the {@link #none()}
     *  object is returned.
     *
     *  @param x the x coordinate of the location of the bounds object.
     *  @param y the y coordinate of the location of the bounds object.
     *  @param width the width of the bounds object.
     *  @param height the height of the bounds object.
     *  @return a bounds object with the specified location and size
     *  in the form of x and y coordinates, width and height.
     */
    public static Bounds of( int x, int y, int width, int height ) {
        if ( width < 0 || height < 0 )
            return EMPTY;

        return new Bounds(Location.of(x, y), Size.of(width, height));
    }

    public static Bounds of( float x, float y, float width, float height ) {
        if ( width < 0 || height < 0 )
            return EMPTY;

        return new Bounds(Location.of(x, y), Size.of(width, height));
    }

    private Bounds( Location location, Size size ) {
        _location = Objects.requireNonNull(location);
        _size     = Objects.requireNonNull(size);
    }

    /**
     *  If you think of the bounds object as a rectangle, then the
     *  {@link Location} defines the top left corner and the {@link Size}
     *  defines the width and height of the rectangle.
     *  Note that the y-axis is growing positively downwards and the x-axis
     *  is growing positively to the right.
     *
     * @return The {@link Location} of this bounds object,
     *         which contains the x and y coordinates.
     */
    public Location location() {
        return _location;
    }

    /**
     * @return The truth value of whether this bounds object has a width,
     *       which is true if the width is greater than zero.
     */
    public boolean hasWidth() { return _size._width > 0; }

    /**
     * @return The truth value of whether this bounds object has a height,
     *       which is true if the height is greater than zero.
     */
    public boolean hasHeight() {
        return _size._height > 0;
    }

    /**
     *  The {@link Size} of define the width and height of the bounds
     *  starting from the x and y coordinates of the {@link Location}.
     *  Note that the {@link Location} is always the top left corner
     *  of the bounds object where the y-axis is growing positively downwards
     *  and the x-axis is growing positively to the right.
     *
     * @return The {@link Size} of this bounds object,
     *        which contains the width and height.
     */
    public Size size() {
        return _size;
    }

    /**
     * @param x A new x coordinate for the location of this bounds object.
     * @return A new bounds object with a new location that has the specified x coordinate.
     */
    public Bounds withX( int x ) {
        return new Bounds(_location.withX(x), _size);
    }

    /**
     * @param y A new y coordinate for the location of this bounds object.
     * @return A new bounds object with a new location that has the specified y coordinate.
     */
    public Bounds withY( int y ) {
        return new Bounds(_location.withY(y), _size);
    }

    /**
     * @param width A new width for the size of this bounds object.
     * @return A new bounds object with a new size that has the specified width.
     */
    public Bounds withWidth( int width ) {
        return new Bounds(_location, _size.withWidth(width));
    }

    /**
     * @param height A new height for the size of this bounds object.
     * @return A new bounds object with a new size that has the specified height.
     */
    public Bounds withHeight( int height ) {
        return new Bounds(_location, _size.withHeight(height));
    }

    /**
     * @param width A new width for the size of this bounds object.
     * @param height A new height for the size of this bounds object.
     * @return A new bounds object with a new size that has the specified width and height.
     */
    public boolean hasSize( int width, int height ) {
        return _size._width == width && _size._height == height;
    }

    /**
     * @param width An integer value to compare to the width of this bounds object.
     * @return The truth value of whether the specified width is equal to the width of this bounds object.
     */
    public boolean hasWidth( int width ) {
        return _size._width == width;
    }

    /**
     * @param height An integer value to compare to the height of this bounds object.
     * @return The truth value of whether the specified height is equal to the height of this bounds object.
     */
    public boolean hasHeight( int height ) {
        return _size._height == height;
    }

    /**
     * @return The area of this bounds object, which is the width multiplied by the height.
     */
    public float area() {
        return _size._width * _size._height;
    }

    /**
     * @return A {@link Rectangle} object with the same location and size as this bounds object.
     */
    public Rectangle toRectangle() {
        return new Rectangle((int) _location.x(), (int) _location.y(), (int) _size._width, (int) _size._height);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"[" +
                    "location=" + _location + ", "+
                    "size="     + _size     +
                "]";
    }

    /**
     * @param x An integer value to compare to the x coordinate of the location of this bounds object.
     * @param y An integer value to compare to the y coordinate of the location of this bounds object.
     * @param width An integer value to compare to the width of this bounds object.
     * @param height An integer value to compare to the height of this bounds object.
     * @return The truth value of whether the specified x and y coordinates and width and height
     *        are equal to the location and size of this bounds object.
     */
    public boolean equals( int x, int y, int width, int height ) {
        return _location.x() == x && _location.y() == y && _size._width == width && _size._height == height;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o == this ) return true;
        if ( o == null ) return false;
        if ( o.getClass() != this.getClass() ) return false;
        Bounds that = (Bounds)o;
        return Objects.equals(this._location, that._location) &&
               Objects.equals(this._size,     that._size);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_location, _size);
    }
}
