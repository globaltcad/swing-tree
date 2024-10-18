package swingtree.layout;

import com.google.errorprone.annotations.Immutable;
import org.jspecify.annotations.Nullable;
import swingtree.api.Styler;
import swingtree.style.ComponentStyleDelegate;
import swingtree.style.ImageConf;

import java.awt.Dimension;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Function;

/**
 *  An immutable value object that represents a size in the form of a width and height or lack thereof.
 *  This is used to represent the size and layout dimensions of components in the SwingTree style API,
 *  as well as the size of icons as part of {@link swingtree.api.IconDeclaration}s.
 *  See {@link swingtree.UIForAnySwing#withStyle(Styler)},
 *  {@link ComponentStyleDelegate#image(swingtree.api.Configurator)} and
 *  {@link ImageConf#size(int, int)} for examples of where and how this class is used.
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
     *  If the width or height is negative, the returned size will be the {@link #UNKNOWN} size
     *  constant with a width or height of -1.
     *
     * @param width The width of the size in the form of a float.
     * @param height The height of the size in the form of a float.
     * @return A {@link Size} instance that represents the given width and height.
     */
    public static Size of( float width, float height ) {
        if ( width < 0 && height < 0 )
            return UNKNOWN;
        return new Size( width, height );
    }

    /**
     *  A factory method that creates a {@link Size} instance from a width and height.
     *  If the width or height is negative, the returned size will be the {@link #UNKNOWN} size
     *  constant with a width or height of -1.
     *
     * @param width The width of the size in the form of a double.
     * @param height The height of the size in the form of a double.
     * @return A {@link Size} instance that represents the given width and height.
     */
    public static Size of( double width, double height ) {
        if ( width < 0 && height < 0 )
            return UNKNOWN;
        return new Size((float) width, (float) height);
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

    /**
     *  Allows you to check if the width value of this {@link Size} instance
     *  is both specified (non-negative) and also positive.
     *
     * @return True if the width is both specified and positive, false otherwise.
     */
    public boolean hasPositiveWidth() {
        return _width > 0;
    }

    /**
     *  Allows you to check if the height value of this {@link Size} instance
     *  is both specified (non-negative) and also positive.
     *
     * @return True if the height is both specified and positive, false otherwise.
     */
    public boolean hasPositiveHeight() {
        return _height > 0;
    }

    /**
     *  Creates an updated {@link Size} instance with the given width.
     *  If the width is negative, the width of the returned size will be -1.
     * @param width The width of the size to create.
     * @return A new {@link Size} instance with the given width.
     */
    public Size withWidth( double width ) {
        return new Size((float) width, _height);
    }

    /**
     *  Creates an updated {@link Size} instance with the given height.
     *  If the height is negative, the height of the returned size will be -1.
     * @param height The height of the size to create.
     * @return A new {@link Size} instance with the given height.
     */
    public Size withHeight( double height ) {
        return new Size(_width, (float) height);
    }

    public Dimension toDimension() {
        return new Dimension((int) _width, (int) _height);
    }

    /**
     *  Scales the width and height of this {@link Size} instance by the given factor.
     *  If the width or height not specified (i.e. negative), they will remain negative.
     *  A negative scale factor will result in a negative width and height.
     *
     * @param scaleFactor The factor to scale the width and height by.
     * @return A new {@link Size} instance with the scaled width and height.
     */
    public Size scale( double scaleFactor ) {
        if ( this.equals(UNKNOWN) )
            return this;

        if ( scaleFactor < 0 )
            return UNKNOWN;

        float width  = _width  < 0 ? -1 : Math.round(_width  * scaleFactor);
        float height = _height < 0 ? -1 : Math.round(_height * scaleFactor);
        return of(width, height);
    }

    /**
     *  Rounds the float based width and height of this {@link Size} to
     *  the nearest integer value and returns these as a new {@link Size}.
     *
     * @return A {@link Size} objects whose width and height values does not have fractions.
     */
    public Size round() {
        if ( this.equals(UNKNOWN) )
            return this;
        else
            return Size.of(Math.round(_width), Math.round(_height));
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
