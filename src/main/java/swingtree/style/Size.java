package swingtree.style;

import java.util.Objects;
import java.util.Optional;

/**
 *  An immutable value object that represents a size
 *  in the form of a width and height or lack thereof.
 */
final class Size
{
    private static final Size EMPTY = new Size(null, null);

    public static Size none() {
        return EMPTY;
    }

    public static Size of( int width, int height ) {
        return new Size(width, height);
    }

    private final Integer _width;
    private final Integer _height;


    public Size( Integer width, Integer height ) {
        _width  = width;
        _height = height;
    }

    Optional<Integer> width() {
        return Optional.ofNullable(_width);
    }

    Optional<Integer> height() {
        return Optional.ofNullable(_height);
    }

    Size width( Integer width ) {
        Objects.requireNonNull(width);
        return new Size(width, _height);
    }

    Size height( Integer height ) {
        Objects.requireNonNull(height);
        return new Size(_width, height);
    }

    Size scale( double scaleFactor ) {
        if ( _width == null && _height == null ) return this;
        Integer width  = _width  == null ? null : (int) Math.round(_width  * scaleFactor);
        Integer height = _height == null ? null : (int) Math.round(_height * scaleFactor);
        return new Size(width, height);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"[" +
                    "width="  + ( _width  == null ? "?" : _width  ) +", "+
                    "height=" + ( _height == null ? "?" : _height ) +
                "]";
    }

    @Override
    public boolean equals( Object o ) {
        if ( o == this ) return true;
        if ( o == null ) return false;
        if ( o.getClass() != this.getClass() ) return false;
        Size that = (Size)o;
        return Objects.equals(this._width,  that._width) &&
               Objects.equals(this._height, that._height);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_width, _height);
    }

}
