package swingtree.style;

import swingtree.UI;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.Objects;

/**
 *  An immutable snapshot of essential component state needed for rendering
 *  the style of a particular component layer using the {@link StyleRenderer} and its
 *  {@link StyleRenderer#renderStyleOn(UI.Layer, LayerRenderConf, Graphics2D)} ethod. <br>
 *  This (and all of its parts) is immutable to use it as a basis for caching.
 *  When the config changes compared to the previous one, the image buffer based
 *  render cache is being invalidated and the component is rendered again
 *  (potentially with a new cached image buffer).
 */
final class LayerRenderConf
{
    private static final LayerRenderConf _NONE = new LayerRenderConf(
                                                    BoxModelConf.none(),
                                                    BaseColorConf.none(),
                                                    StyleConfLayer.empty()
                                                );

    public static LayerRenderConf none() { return _NONE; }

    private final BoxModelConf   _boxModelConf;
    private final BaseColorConf  _baseColor;
    private final StyleConfLayer _layer;

    private boolean _wasAlreadyHashed = false;
    private int     _hashCode         = 0; // cached hash code


    private LayerRenderConf(
        BoxModelConf   boxModelConf,
        BaseColorConf  base,
        StyleConfLayer layers
    ) {
        _boxModelConf = ComponentAreas.intern(Objects.requireNonNull(boxModelConf));
        _baseColor    = Objects.requireNonNull(base);
        _layer        = Objects.requireNonNull(layers);
    }

    static LayerRenderConf of(
        BoxModelConf   boxModelConf,
        BaseColorConf  base,
        StyleConfLayer layers
    ) {
        if (
            boxModelConf == BoxModelConf.none() &&
            base         == BaseColorConf.none() &&
            layers       == _NONE._layer
        )
            return _NONE;
        else
            return new LayerRenderConf(boxModelConf, base, layers);
    }

    static LayerRenderConf of( UI.Layer layer, ComponentConf fullConf ) {
        BoxModelConf boxModelConf = BoxModelConf.of(
                                        fullConf.style().border(),
                                        fullConf.baseOutline(),
                                        fullConf.currentBounds().size()
                                    );
        BaseColorConf colorConf = BaseColorConf.of(
                                    fullConf.style().base().foundationColor().filter( c -> layer == UI.Layer.BACKGROUND ).orElse(null),
                                    fullConf.style().base().backgroundColor().filter( c -> layer == UI.Layer.BACKGROUND ).orElse(null),
                                    fullConf.style()
                                            .border()
                                            .color()
                                            .filter(c -> layer == UI.Layer.BORDER )
                                            .orElse(null)
                                );
        return of(
                    boxModelConf,
                    colorConf,
                    fullConf.style().layer(layer)
                );
    }

    BoxModelConf boxModel() { return _boxModelConf; }

    BaseColorConf baseColors() { return _baseColor; }

    StyleConfLayer layer() { return _layer; }

    ComponentAreas areas() { return ComponentAreas.of(_boxModelConf); }


    @Override
    public int hashCode() {
        if ( _wasAlreadyHashed )
            return _hashCode;

        _hashCode = Objects.hash(_boxModelConf, _baseColor, _layer);
        _wasAlreadyHashed = true;
        return _hashCode;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o == this ) return true;
        if ( o == null ) return false;
        if ( o.getClass() != this.getClass() ) return false;
        LayerRenderConf other = (LayerRenderConf) o;
        return Objects.equals(_boxModelConf, other._boxModelConf) &&
               Objects.equals(_baseColor, other._baseColor) &&
               Objects.equals(_layer, other._layer);
    }


}
