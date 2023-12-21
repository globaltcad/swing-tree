package swingtree.layout;

import java.awt.Rectangle;
import java.util.Objects;

/**
 *  An immutable value object that represents a position and size
 *  in the form of an x and y coordinate and a width and height.
 */
public final class Bounds
{
    private final static Bounds EMPTY = new Bounds(Location.origin(), Size.unknown());

    public static Bounds none() {
        return EMPTY;
    }

    private final Location _location;
    private final Size     _size;


    public static Bounds of( Location location, Size size ) {
        if ( location == null || size == null )
            return EMPTY;

        return new Bounds(location, size);
    }

    public static Bounds of( int x, int y, int width, int height ) {
        if ( width < 0 || height < 0 )
            return EMPTY;

        return new Bounds(Location.of(x, y), Size.of(width, height));
    }

    private Bounds( Location location, Size size ) {
        _location = Objects.requireNonNull(location);
        _size     = Objects.requireNonNull(size);
    }

    public Location location() {
        return _location;
    }

    public boolean hasWidth() { return _size._width > 0; }

    public boolean hasHeight() {
        return _size._height > 0;
    }

    public Size size() {
        return _size;
    }

    public Bounds withX( int x ) {
        return new Bounds(_location.withX(x), _size);
    }

    public Bounds withY( int y ) {
        return new Bounds(_location.withY(y), _size);
    }

    public Bounds withWidth( int width ) {
        return new Bounds(_location, _size.withWidth(width));
    }

    public Bounds withHeight( int height ) {
        return new Bounds(_location, _size.withHeight(height));
    }

    public boolean hasSize( int width, int height ) {
        return _size._width == width && _size._height == height;
    }

    public boolean hasWidth( int width ) {
        return _size._width == width;
    }

    public int area() {
        return _size._width * _size._height;
    }

    public boolean hasHeight( int height ) {
        return _size._height == height;
    }

    public Rectangle toRectangle() {
        return new Rectangle(_location.x(), _location.y(), _size._width, _size._height);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"[" +
                    "location=" + _location + ", "+
                    "size="     + _size     +
                "]";
    }

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
