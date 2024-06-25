package swingtree.layout;

import com.google.errorprone.annotations.Immutable;
import org.jspecify.annotations.Nullable;
import swingtree.api.Styler;
import swingtree.style.ComponentStyleDelegate;
import swingtree.style.ImageConf;

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
 *  {@link ComponentStyleDelegate#image(swingtree.api.Configurator)} and
 *  {@link ImageConf#size(int, int)}.
 */
@Immutable
public final class Size
{
    private static final Size UNKNOWN = new Size(-1, -1);

    /**
     *  Exposes the {@link #UNKNOWN} size instance, which is a null object
     *  that represents an unknown size. It uses -1 for both width and height
     *  and will return {@link Optional#empty()} for both width and height.
     *
     * @return A {@link Size} instance that represents an unknown size.
     */
    public static Size unknown() {
        return UNKNOWN;
    }

    /**
     *  A factory method that creates a {@link Size} instance from a width and height.
     *  If the width or height is negative, the returned size will have be the {@link #UNKNOWN} size with
     *  a width or height of -1.
     *
     * @param width The width of the size.
     * @param height The height of the size.
     * @return A {@link Size} instance that represents the given width and height.
     */
    public static Size of( float width, float height ) {
        if ( width < 0 && height < 0 )
            return UNKNOWN;
        return new Size( width, height );
    }

    /**
     *  A factory method that creates a {@link Size} instance from a {@link Dimension}.
     * @param dimension The dimension to convert to a {@link Size} instance.
     * @return A {@link Size} instance that represents the given dimension.
     */
    public static Size of( @Nullable Dimension dimension ) {
        if ( dimension == null )
            return UNKNOWN;
        return of(dimension.width, dimension.height);
    }

    final float _width;
    final float _height;


    private Size( float width, float height ) {
        _width  = width  < 0 ? -1 : width;
        _height = height < 0 ? -1 : height;
    }

    /**
     *  The width of this {@link Size} instance may not be specified,
     *  in which case this method returns {@link Optional#empty()}
     *  and the thing that this configuration is applied to should
     *  resort to its default width.
     *
     * @return The width of this {@link Size} instance or {@link Optional#empty()} if unknown.
     */
    public Optional<Float> width() {
        return ( _width < 0 ? Optional.empty() : Optional.of(_width) );
    }

    /**
     *  The height of this {@link Size} instance may not be specified,
     *  in which case this method returns {@link Optional#empty()}
     *  and the thing that this configuration is applied to should
     *  resort to its default height.
     *
     * @return The height of this {@link Size} instance or {@link Optional#empty()} if unknown.
     */
    public Optional<Float> height() {
        return ( _height < 0 ? Optional.empty() : Optional.of(_height) );
    }

    public boolean hasPositiveWidth() {
        return _width > 0;
    }

    public boolean hasPositiveHeight() {
        return _height > 0;
    }

    /**
     *  Creates an updated {@link Size} instance with the given width.
     *  If the width is negative, the width of the returned size will be -1.
     * @param width The width of the size to create.
     * @return A new {@link Size} instance with the given width.
     */
    public Size withWidth( int width ) {
        return new Size(width, _height);
    }

    /**
     *  Creates an updated {@link Size} instance with the given height.
     *  If the height is negative, the height of the returned size will be -1.
     * @param height The height of the size to create.
     * @return A new {@link Size} instance with the given height.
     */
    public Size withHeight( int height ) {
        return new Size(_width, height);
    }

    public Dimension toDimension() {
        return new Dimension((int) _width, (int) _height);
    }

    public Size scale( double scaleFactor ) {
        float width  = _width  < 0 ? -1 : Math.round(_width  * scaleFactor);
        float height = _height < 0 ? -1 : Math.round(_height * scaleFactor);
        return of(width, height);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"[" +
                    "width="  + _toString( _width  ) + ", "+
                    "height=" + _toString( _height ) +
                "]";
    }

    private static String _toString( float value ) {
        return value < 0 ? "?" : String.valueOf(value).replace(".0", "");
    }

    @Override
    public boolean equals( Object o ) {
        if ( o == this ) return true;
        if ( o == null ) return false;
        if ( o.getClass() != this.getClass() ) return false;
        Size that = (Size)o;
        return _width  == that._width &&
               _height == that._height;
    }

    @Override
    public int hashCode() {
        return Objects.hash(_width, _height);
    }

}
