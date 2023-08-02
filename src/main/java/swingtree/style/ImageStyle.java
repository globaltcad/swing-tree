package swingtree.style;

import swingtree.UI;

import java.awt.*;
import java.util.Objects;
import java.util.Optional;

public final class ImageStyle
{
    private static final ImageStyle _NONE = new ImageStyle(
                                                UI.Layer.BACKGROUND,
                                                null,
                                                null,
                                                UI.Placement.CENTER,
                                                false,
                                                false,
                                                null,
                                                null,
                                                1.0f
                                            );

    static ImageStyle none() { return _NONE; }

    private final UI.Layer     _layer;
    private final Color        _primer;

    private final Image        _image;
    private final UI.Placement _placement;
    private final boolean      _repeat;
    private final boolean      _autoFit;
    private final Integer      _width;
    private final Integer      _height;
    private final float        _opacity;

    public ImageStyle(
        UI.Layer     layer,
        Color        primer,
        Image        image,
        UI.Placement placement,
        boolean      repeat,
        boolean      autoFit,
        Integer      width,
        Integer      height,
        float        opacity
    ) {
        _layer     = Objects.requireNonNull(layer);
        _primer    = primer;
        _image     = image;
        _placement = Objects.requireNonNull(placement);
        _repeat    = repeat;
        _autoFit   = autoFit;
        _width     = width;
        _height    = height;
        _opacity   = opacity;
        if ( _opacity < 0.0f || _opacity > 1.0f )
            throw new IllegalArgumentException("transparency must be between 0.0f and 1.0f");
    }

    public UI.Layer layer() { return _layer; }

    public Optional<Color> primer() { return Optional.ofNullable(_primer); }

    public Optional<Image> image() { return Optional.ofNullable(_image); }

    public UI.Placement placement() { return _placement; }

    public boolean repeat() { return _repeat; }

    public boolean autoFit() { return _autoFit; }

    public Optional<Integer> width() { return Optional.ofNullable(_width); }

    public Optional<Integer> height() { return Optional.ofNullable(_height); }

    public float opacity() { return _opacity; }

    /**
     *  This method allows you to specify the layer onto which the image will be drawn.
     *  The default layer is the background layer. <br>
     *  Here a list of available layers:
     *  <ul>
     *      <li>{@link swingtree.UI.Layer#BACKGROUND}</li>
     *      <li>{@link swingtree.UI.Layer#CONTENT}</li>
     *      <li>{@link swingtree.UI.Layer#BORDER}</li>
     *      <li>{@link swingtree.UI.Layer#FOREGROUND}</li>
     *  </ul>
     *
     * @param layer The layer onto which the image will be drawn.
     * @return A new {@link ImageStyle} instance with the specified layer.
     */
    public ImageStyle layer( UI.Layer layer ) { return new ImageStyle(layer, _primer, _image, _placement, _repeat, _autoFit, _width, _height, _opacity); }

    /**
     *  Here you can specify the <b>primer color of the image style</b> which will be used
     *  as a filler color for the image background. <br>
     *  Note that the primer color will not be visible if the image is opaque and it fills the entire component.
     *
     * @param color The primer color of the image style.
     * @return A new {@link ImageStyle} instance with the specified primer color.
     */
    public ImageStyle primer( Color color ) { return new ImageStyle(_layer, color, _image, _placement, _repeat, _autoFit, _width, _height, _opacity); }

    /**
     *  Here you can specify the <b>image</b> which will be drawn onto the component.
     *  The supplied object must be an instance of {@link Image} implementation.
     *
     * @param image The image which will be drawn onto the component.
     * @return A new {@link ImageStyle} instance with the specified image.
     */
    public ImageStyle image( Image image ) { return new ImageStyle(_layer, _primer, image, _placement, _repeat, _autoFit, _width, _height, _opacity); }

    /**
     *  Here you can specify the <b>placement</b> of the image onto the component.
     *  The default placement is {@link swingtree.UI.Placement#CENTER}. <br
     *  Here a list of available options and their effect:
     *  <ul>
     *      <li>{@link swingtree.UI.Placement#CENTER} -
     *          The image will be drawn at the center of the component.
     *          So the center of the image will be at the center of the inner component area.
     *      </li>
     *      <li>{@link swingtree.UI.Placement#TOP_LEFT} -
     *          The image will be drawn beginning at the top left corner of the inner component area.
     *          So the top left corner of the image will be in the top left corner of the inner component area.
     *      </li>
     *      <li>{@link swingtree.UI.Placement#TOP_RIGHT} -
     *          The image will be placed in the top right corner of the inner component area.
     *          So the top right corner of the image will be in the top right corner of the inner component area.
     *      </li>
     *      <li>{@link swingtree.UI.Placement#BOTTOM_LEFT} -
     *          The image will be drawn in the bottom left corner of the inner component area.
     *          So the bottom left corner of the image will be in the bottom left corner of the inner component area.
     *      </li>
     *      <li>{@link swingtree.UI.Placement#BOTTOM_RIGHT} -
     *          The image will be drawn in the bottom right corner of the inner component area.
     *          So the bottom right corner of the image will be in the bottom right corner of the inner component area.
     *      </li>
     *      <li>{@link swingtree.UI.Placement#TOP} -
     *          The image will be drawn in the top center of the inner component area.
     *          So the top center of the image will be in the top center of the inner component area.
     *      </li>
     *      <li>{@link swingtree.UI.Placement#BOTTOM} -
     *          The image will be drawn in the bottom center of the inner component area.
     *          So the bottom center of the image will be in the bottom center of the inner component area.
     *      </li>
     *      <li>{@link swingtree.UI.Placement#LEFT} -
     *          The image will be drawn in the left center of the inner component area.
     *          So the left center of the image will be in the left center of the inner component area.
     *      </li>
     *  </ul>
     *
     * @param placement The placement of the image onto the component.
     * @return A new {@link ImageStyle} instance with the specified placement.
     */
    public ImageStyle placement( UI.Placement placement ) { return new ImageStyle(_layer, _primer, _image, placement, _repeat, _autoFit, _width, _height, _opacity); }

    /**
     *  If this flag is set to {@code true}, then the image may be painted
     *  multiple times so that it fills up the entire inner component area.
     *  There will not be a noticeable effect of this flag if the
     *  image already fills out the inner component area (see {@link #autoFit(boolean)}, {@link #size(int, int)}).
     *
     * @param repeat Weather the image should be painted repeatedly across the inner component area.
     * @return A new {@link ImageStyle} instance with the specified {@code repeat} flag value.
     */
    public ImageStyle repeat( boolean repeat ) { return new ImageStyle(_layer, _primer, _image, _placement, repeat, _autoFit, _width, _height, _opacity); }

    /**
     *  If this flag is set to {@link true}, then the image will be stretched or shrunk
     *  to fill the inner component area dependent on the specified width and height,
     *  meaning that if the width was not specified explicitly through {@link #width(Integer)}
     *  then the image will be scaled to fit the inner component width,
     *  and if a height was not specified through {@link #height(Integer)} then
     *  the image will be scaled to fit the inner component height. <br>
     *  <b>Note that the inner component area is the area enclosed by the border, which
     *  is itself not part of said area!</b>
     *
     * @param autoFit If true the image will be scaled to fit the inner component area for every
     *                dimension which was not specified,
     *                otherwise the image will not be scaled to fit the inner component area.
     * @return A new {@link ImageStyle} instance with the specified {@code autoFit} flag value.
     */
    public ImageStyle autoFit( boolean autoFit ) { return new ImageStyle(_layer, _primer, _image, _placement, _repeat, autoFit, _width, _height, _opacity); }

    /**
     *  Ensures that the image has the specified width.
     *
     * @param width The width of the image.
     * @return A new {@link ImageStyle} instance with the specified {@code width}.
     */
    public ImageStyle width( Integer width ) { return new ImageStyle(_layer, _primer, _image, _placement, _repeat, _autoFit, width, _height, _opacity); }

    /**
     *  Ensures that the image has the specified height.
     *
     * @param height The height of the image.
     * @return A new {@link ImageStyle} instance with the specified {@code heiht}.
     */
    public ImageStyle height( Integer height ) { return new ImageStyle(_layer, _primer, _image, _placement, _repeat, _autoFit, _width, height, _opacity); }

    public ImageStyle size( int width, int height ) { return new ImageStyle(_layer, _primer, _image, _placement, _repeat, _autoFit, width, height, _opacity); }

    public ImageStyle opacity( float opacity ) { return new ImageStyle(_layer, _primer, _image, _placement, _repeat, _autoFit, _width, _height, opacity); }

    @Override
    public int hashCode() { return Objects.hash(_layer, _primer, _image, _placement, _repeat, _autoFit, _width, _height, _opacity); }

    @Override
    public boolean equals(Object obj) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        ImageStyle rhs = (ImageStyle) obj;
        return
                Objects.equals(_layer,        rhs._layer) &&
                Objects.equals(_primer,       rhs._primer) &&
                Objects.equals(_image,        rhs._image) &&
                Objects.equals(_placement,    rhs._placement) &&
                Objects.equals(_repeat,       rhs._repeat) &&
                Objects.equals(_autoFit,      rhs._autoFit) &&
                Objects.equals(_width,        rhs._width) &&
                Objects.equals(_height,       rhs._height) &&
                Objects.equals(_opacity, rhs._opacity);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" +
                    "layer="        + _layer +
                    ", primer="     + StyleUtility.toString(_primer) +
                    ", image="      + (_image == null ? "?" : _image.toString()) +
                    ", placement="  + _placement +
                    ", repeat="     + _repeat +
                    ", autoFit="    + _autoFit +
                    ", width="      + ( _width == null ? "?" : _width.toString() ) +
                    ", height="     + ( _height == null ? "?" : _height.toString() ) +
                    ", transparency=" + _opacity +
                "]";
    }
}
