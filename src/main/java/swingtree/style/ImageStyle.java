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


    public ImageStyle layer( UI.Layer layer ) { return new ImageStyle(layer, _primer, _image, _placement, _repeat, _autoFit, _width, _height, _opacity); }

    public ImageStyle primer( Color color ) { return new ImageStyle(_layer, color, _image, _placement, _repeat, _autoFit, _width, _height, _opacity); }

    public ImageStyle image( Image image ) { return new ImageStyle(_layer, _primer, image, _placement, _repeat, _autoFit, _width, _height, _opacity); }

    public ImageStyle placement( UI.Placement placement ) { return new ImageStyle(_layer, _primer, _image, placement, _repeat, _autoFit, _width, _height, _opacity); }

    public ImageStyle repeat( boolean repeat ) { return new ImageStyle(_layer, _primer, _image, _placement, repeat, _autoFit, _width, _height, _opacity); }

    public ImageStyle autoFit( boolean autoFit ) { return new ImageStyle(_layer, _primer, _image, _placement, _repeat, autoFit, _width, _height, _opacity); }

    public ImageStyle width( Integer width ) { return new ImageStyle(_layer, _primer, _image, _placement, _repeat, _autoFit, width, _height, _opacity); }

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
