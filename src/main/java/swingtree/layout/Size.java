package swingtree.layout;

import swingtree.api.Styler;
import swingtree.style.ComponentStyleDelegate;
import swingtree.style.ImageStyle;

import java.awt.Dimension;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 *  An immutable value object that represents a size
 *  in the form of a width and height or lack thereof.
 *  This is used to represent the size of icons
 *  as part of {@link swingtree.api.IconDeclaration}s
 *  and the SwingTree style API, see {@link swingtree.UIForAnySwing#withStyle(Styler)},
 *  {@link ComponentStyleDelegate#image(Function)} and
 *  {@link ImageStyle#size(int, int)}.
 */
public final class Size
{
    private static final Size UNKNOWN = new Size(-1, -1);

    /**
     * @return A {@link Size} instance that represents an unknown size.
     */
    public static Size unknown() {
        return UNKNOWN;
    }

    /**
     * @return A {@link Size} instance that represents the given width and height.
     */
    public static Size of( int width, int height ) {
        if ( width < 0 && height < 0 )
            return UNKNOWN;
        return new Size( width, height );
    }

    final int _width;
    final int _height;


    private Size( int width, int height ) {
        _width  = Math.max(-1, width);
        _height = Math.max(-1, height);
    }

    /**
     * @return The width of this {@link Size} instance or {@link Optional#empty()} if unknown.
     */
    public Optional<Integer> width() {
        return ( _width < 0 ? Optional.empty() : Optional.of(_width) );
    }

    /**
     * @return The height of this {@link Size} instance or {@link Optional#empty()} if unknown.
     */
    public Optional<Integer> height() {
        return ( _height < 0 ? Optional.empty() : Optional.of(_height) );
    }

    /**
     * @return A new {@link Size} instance with the given width.
     */
    public Size withWidth( int width ) {
        return new Size(width, _height);
    }

    /**
     * @return A new {@link Size} instance with the given height.
     */
    public Size withHeight(int height ) {
        return new Size(_width, height);
    }

    public Dimension toDimension() {
        return new Dimension(_width, _height);
    }

    public Size scale( double scaleFactor ) {
        int width  = _width  < 0 ? -1 : (int) Math.round(_width  * scaleFactor);
        int height = _height < 0 ? -1 : (int) Math.round(_height * scaleFactor);
        return of(width, height);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"[" +
                    "width="  + ( _width  < 0 ? "?" : _width  ) + ", "+
                    "height=" + ( _height < 0 ? "?" : _height ) +
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
