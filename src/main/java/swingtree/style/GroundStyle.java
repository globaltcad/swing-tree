package swingtree.style;

import swingtree.UI;

import java.awt.*;
import java.util.Objects;
import java.util.Optional;

public final class GroundStyle
{
    private static final GroundStyle _NONE = new GroundStyle(
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

    static GroundStyle none() { return _NONE; }

    private final UI.Layer     _layer;
    private final Color        _color;

    private final Image        _image;
    private final UI.Placement _placement;
    private final boolean      _repeat;
    private final boolean      _autofit;
    private final Integer      _width;
    private final Integer      _height;
    private final float        _transparency;

    public GroundStyle(
        UI.Layer     layer,
        Color        color,
        Image        image,
        UI.Placement placement,
        boolean      repeat,
        boolean      autofit,
        Integer      width,
        Integer      height,
        float        transparency
    ) {
        _layer        = Objects.requireNonNull(layer);
        _color        = color;
        _image        = image;
        _placement    = Objects.requireNonNull(placement);
        _repeat       = repeat;
        _autofit      = autofit;
        _width        = width;
        _height       = height;
        _transparency = transparency;
        if ( _transparency < 0.0f || _transparency > 1.0f )
            throw new IllegalArgumentException("transparency must be between 0.0f and 1.0f");
    }

    public UI.Layer layer() { return _layer; }

    public Optional<Color> color() { return Optional.ofNullable(_color); }

    public Optional<Image> image() { return Optional.ofNullable(_image); }

    public UI.Placement placement() { return _placement; }

    public boolean repeat() { return _repeat; }

    public boolean autofit() { return _autofit; }

    public Optional<Integer> width() { return Optional.ofNullable(_width); }

    public Optional<Integer> height() { return Optional.ofNullable(_height); }

    public float transparency() { return _transparency; }


    public GroundStyle layer( UI.Layer layer ) { return new GroundStyle(layer, _color, _image, _placement, _repeat, _autofit, _width, _height, _transparency); }

    public GroundStyle color( Color color ) { return new GroundStyle(_layer, color, _image, _placement, _repeat, _autofit, _width, _height, _transparency); }

    public GroundStyle image( Image image ) { return new GroundStyle(_layer, _color, image, _placement, _repeat, _autofit, _width, _height, _transparency); }

    public GroundStyle placement( UI.Placement placement ) { return new GroundStyle(_layer, _color, _image, placement, _repeat, _autofit, _width, _height, _transparency); }

    public GroundStyle repeat( boolean repeat ) { return new GroundStyle(_layer, _color, _image, _placement, repeat, _autofit, _width, _height, _transparency); }

    public GroundStyle autofit( boolean autofit ) { return new GroundStyle(_layer, _color, _image, _placement, _repeat, autofit, _width, _height, _transparency); }

    public GroundStyle width( Integer width ) { return new GroundStyle(_layer, _color, _image, _placement, _repeat, _autofit, width, _height, _transparency); }

    public GroundStyle height( Integer height ) { return new GroundStyle(_layer, _color, _image, _placement, _repeat, _autofit, _width, height, _transparency); }

    public GroundStyle size( int width, int height ) { return new GroundStyle(_layer, _color, _image, _placement, _repeat, _autofit, width, height, _transparency); }

    public GroundStyle transparency( float transparency ) { return new GroundStyle(_layer, _color, _image, _placement, _repeat, _autofit, _width, _height, transparency); }

    @Override
    public int hashCode() { return Objects.hash(_layer, _color, _image, _placement, _repeat, _autofit, _width, _height, _transparency); }

    @Override
    public boolean equals(Object obj) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        GroundStyle rhs = (GroundStyle) obj;
        return
                Objects.equals(_layer,        rhs._layer) &&
                Objects.equals(_color,        rhs._color) &&
                Objects.equals(_image,        rhs._image) &&
                Objects.equals(_placement,    rhs._placement) &&
                Objects.equals(_repeat,       rhs._repeat) &&
                Objects.equals(_autofit,      rhs._autofit) &&
                Objects.equals(_width,        rhs._width) &&
                Objects.equals(_height,       rhs._height) &&
                Objects.equals(_transparency, rhs._transparency);
    }

    @Override
    public String toString() {
        return "GroundStyle[" +
                    "layer="        + _layer +
                    ", color="      + StyleUtility.toString(_color) +
                    ", image="      + (_image == null ? "?" : _image.toString()) +
                    ", placement="  + _placement +
                    ", repeat="     + _repeat +
                    ", autofit="    + _autofit +
                    ", width="      + ( _width == null ? "?" : _width.toString() ) +
                    ", height="     + ( _height == null ? "?" : _height.toString() ) +
                    ", transparency=" + _transparency +
                "]";
    }
}
