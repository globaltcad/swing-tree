package swingtree.layout;

import com.google.errorprone.annotations.Immutable;

import java.awt.Rectangle;
import java.util.Objects;

/**
 *  An immutable value object that represents the position and size of a component
 *  in the form of an x and y coordinate modeled by a {@link Position} object
 *  and a width and height modeled by a {@link Size} object.
 *  Note the rectangular bounds object is positioned in a coordinate system
 *  where the y-axis is growing positively downwards and the x-axis is growing
 *  positively to the right. <br>
 *  The bounds object may also be incomplete in the sense that the width and height
 *  may not be defined, in which case the {@link Size#unknown()} object is used.
 *  A bounds object with an unknown size located at the origin is considered
 *  the null object for this class and can be accessed using the {@link #none()} method.
 *  You may use this object instead of {@code null} to represent a missing bounds object.
 *  <p>
 *  Also note that the {@link #equals(Object)} and {@link #hashCode()} methods
 *  are implemented to compare the {@link Position} and {@link Size} objects
 *  for value based equality instead of reference based equality.
 */
@Immutable
public final class Bounds
{
    private final static Bounds EMPTY = new Bounds(Position.origin(), Size.unknown());

    /**
     *  Returns an empty bounds object, which is the null object for this class.
     *  <p>
     *  The returned bounds object has a location of {@link Position#origin()}
     *  and a size of {@link Size#unknown()}.
     *
     *  @return an empty bounds object that is the null object for this class.
     */
    public static Bounds none() {
        return EMPTY;
    }

    private final Position _position;
    private final Size     _size;

    /**
     *  Returns a bounds object with the specified location and size.
     *  <p>
     *  If the location is {@link Position#origin()} and the size is
     *  {@link Size#unknown()} then the {@link #none()} object is returned.
     *
     *  @param position the location of the bounds object.
     *  @param size the size of the bounds object.
     *  @return a bounds object with the specified location and size.
     */
    public static Bounds of(Position position, Size size ) {
        Objects.requireNonNull(position);
        Objects.requireNonNull(size);
        if ( position.equals(Position.origin()) && size.equals(Size.unknown()) )
            return EMPTY;

        return new Bounds(position, size);
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

        return new Bounds(Position.of(x, y), Size.of(width, height));
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
    public static Bounds of( float x, float y, float width, float height ) {
        if ( width < 0 || height < 0 )
            return EMPTY;

        return new Bounds(Position.of(x, y), Size.of(width, height));
    }

    /**
     *  Creates a bounds object from an AWT {@link Rectangle} object.
     *  <p>
     *  If the width or height is less than zero then the {@link #none()}
     *  object is returned.
     *
     *  @param rectangle an AWT rectangle object to create a bounds object from.
     *  @return a bounds object with the location and size of the AWT rectangle object.
     */
    public static Bounds of( java.awt.Rectangle rectangle ) {
        return of(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
    }

    private Bounds(Position position, Size size ) {
        _position = Objects.requireNonNull(position);
        _size     = Objects.requireNonNull(size);
    }

    /**
     *  If you think of the bounds object as a rectangle, then the
     *  {@link Position} defines the top left corner and the {@link Size}
     *  defines the width and height of the rectangle.
     *  Note that the y-axis is growing positively downwards and the x-axis
     *  is growing positively to the right.
     *
     * @return The {@link Position} of this bounds object,
     *         which contains the x and y coordinates.
     */
    public Position location() {
        return _position;
    }

    /**
     *  Allows you to check weather the bounds object has a width
     *  that is greater than zero.
     *
     * @return The truth value of whether this bounds object has a width,
     *       which is true if the width is greater than zero.
     */
    public boolean hasWidth() { return _size._width > 0; }

    /**
     *  Allows you to check weather the bounds object has a height
     *  that is greater than zero.
     *
     * @return The truth value of whether this bounds object has a height,
     *       which is true if the height is greater than zero.
     */
    public boolean hasHeight() {
        return _size._height > 0;
    }

    /**
     *  The {@link Size} of define the width and height of the bounds
     *  starting from the x and y coordinates of the {@link Position}.
     *  Note that the {@link Position} is always the top left corner
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
     *  Allows you to create an updated version of this bounds object with the
     *  specified x-coordinate and the same y-coordinate and size as this bounds instance.
     *  See also {@link #withY(int)}, {@link #withWidth(int)}, and {@link #withHeight(int)}.
     *
     * @param x A new x coordinate for the location of this bounds object.
     * @return A new bounds object with a new location that has the specified x coordinate.
     */
    public Bounds withX( int x ) {
        return new Bounds(_position.withX(x), _size);
    }

    /**
     *  Allows you to create an updated version of this bounds object with the
     *  specified y-coordinate and the same x-coordinate and size as this bounds instance.
     *  See also {@link #withX(int)}, {@link #withWidth(int)}, and {@link #withHeight(int)}.
     *
     * @param y A new y coordinate for the location of this bounds object.
     * @return A new bounds object with a new location that has the specified y coordinate.
     */
    public Bounds withY( int y ) {
        return new Bounds(_position.withY(y), _size);
    }

    /**
     *  Allows you to create an updated version of this bounds object with the
     *  specified width and the same x and y coordinates as well as height as this bounds instance.
     *  See also {@link #withX(int)}, {@link #withY(int)}, and {@link #withHeight(int)}, and {@link #withSize(int, int)}.
     *
     * @param width A new width for the size of this bounds object.
     * @return A new bounds object with a new size that has the specified width.
     */
    public Bounds withWidth( int width ) {
        return new Bounds(_position, _size.withWidth(width));
    }

    /**
     *  Allows you to create an updated version of this bounds object with the
     *  specified height and the same x and y coordinates as well as width as this bounds instance.
     *  See also {@link #withX(int)}, {@link #withY(int)}, and {@link #withWidth(int)}, and {@link #withSize(int, int)}.
     *
     * @param height A new height for the size of this bounds object.
     * @return A new bounds object with a new size that has the specified height.
     */
    public Bounds withHeight( int height ) {
        return new Bounds(_position, _size.withHeight(height));
    }

    /**
     *  Allows you to create an updated version of this bounds object with the
     *  specified width and height and the same x and y coordinates as this bounds instance.
     *  See also {@link #withX(int)}, {@link #withY(int)}, {@link #withWidth(int)}, and {@link #withHeight(int)}.
     *
     * @param width A new width for the size of this bounds object.
     * @param height A new height for the size of this bounds object.
     * @return A new bounds object with a new size that has the specified width and height.
     */
    public Bounds withSize( int width, int height ) {
        return new Bounds(_position, Size.of(width, height));
    }

    /**
     *  Creates a new bounds object which tightly fits around this bounds object
     *  and the specified bounds object, effectively merging the two bounds objects.
     *  This is done by finding the minimum x and y coordinates and
     *  the maximum width and height of the two bounds objects.
     *
     * @param other The bounds object to merge with this bounds object.
     * @return A new bounds object that tightly fits around this bounds object and the specified bounds object.
     */
    public Bounds merge( Bounds other ) {
        Objects.requireNonNull(other);
        if ( this.equals(other) )
            return this;

        final float thisLeft   = _position.x();
        final float thisTop    = _position.y();
        final float thisRight  = thisLeft + _size._width;
        final float thisBottom = thisTop  + _size._height;

        final float otherLeft   = other._position.x();
        final float otherTop    = other._position.y();
        final float otherRight  = otherLeft + other._size._width;
        final float otherBottom = otherTop  + other._size._height;

        final float left   = Math.min( thisLeft,   otherLeft   );
        final float top    = Math.min( thisTop,    otherTop    );
        final float right  = Math.max( thisRight,  otherRight  );
        final float bottom = Math.max( thisBottom, otherBottom );

        return Bounds.of(left, top, right - left, bottom - top);
    }

    /**
     *  Allows you to check weather the bounds object has the specified
     *  width and height, which is true if the width and height are equal
     *  to the width and height of the {@link Size} of this bounds object
     *  (see {@link #size()}).
     *
     * @param width A new width for the size of this bounds object.
     * @param height A new height for the size of this bounds object.
     * @return A new bounds object with a new size that has the specified width and height.
     */
    public boolean hasSize( int width, int height ) {
        return _size._width == width && _size._height == height;
    }

    /**
     *  Allows you to check weather the bounds object has the specified
     *  width, which is true if the width is equal to the width of the
     *  {@link Size} of this bounds object (see {@link #size()}).
     *
     * @param width An integer value to compare to the width of this bounds object.
     * @return The truth value of whether the specified width is equal to the width of this bounds object.
     */
    public boolean hasWidth( int width ) {
        return _size._width == width;
    }

    /**
     *  Allows you to check weather the bounds object has the specified
     *  height, which is true if the height is equal to the height of the
     *  {@link Size} of this bounds object (see {@link #size()}).
     *
     * @param height An integer value to compare to the height of this bounds object.
     * @return The truth value of whether the specified height is equal to the height of this bounds object.
     */
    public boolean hasHeight( int height ) {
        return _size._height == height;
    }

    /**
     *  The bounds object has a location and size which form a rectangular area
     *  exposed by this method as a float value defined by the width multiplied by the height.
     *
     * @return The area of this bounds object, which is the width multiplied by the height.
     */
    public float area() {
        return _size._width * _size._height;
    }

    /**
     *  The bounds object has a location and size which form a rectangular area
     *  which can easily be converted to a {@link Rectangle} object using this method.
     *
     * @return A {@link Rectangle} object with the same location and size as this bounds object.
     */
    public Rectangle toRectangle() {
        return new Rectangle((int) _position.x(), (int) _position.y(), (int) _size._width, (int) _size._height);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"[" +
                    "location=" + _position + ", "+
                    "size="     + _size     +
                "]";
    }

    /**
     *  A convent method to check if the specified x and y coordinates and width and height
     *  are equal to the location and size of this bounds object.
     *  This is equivalent to calling {@link #equals(Object)} with
     *  a new bounds object created with the specified x and y coordinates and width and height
     *  like so: {@code equals(Bounds.of(x, y, width, height))}.
     *
     * @param x An integer value to compare to the x coordinate of the location of this bounds object.
     * @param y An integer value to compare to the y coordinate of the location of this bounds object.
     * @param width An integer value to compare to the width of this bounds object.
     * @param height An integer value to compare to the height of this bounds object.
     * @return The truth value of whether the specified x and y coordinates and width and height
     *        are equal to the location and size of this bounds object.
     */
    public boolean equals( int x, int y, int width, int height ) {
        return _position.x() == x && _position.y() == y && _size._width == width && _size._height == height;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o == this ) return true;
        if ( o == null ) return false;
        if ( o.getClass() != this.getClass() ) return false;
        Bounds that = (Bounds)o;
        return Objects.equals(this._position, that._position) &&
               Objects.equals(this._size,     that._size);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_position, _size);
    }
}
