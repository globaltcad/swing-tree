package swingtree.style;

import swingtree.UI;

import java.awt.Graphics;
import java.awt.Shape;
import java.util.Objects;

/**
 *  An immutable snapshot of essential component state needed for rendering
 *  the style of a component.
 *  This is immutable to use it as a basis for caching.
 *  When the snapshot changes compared to the previous one, the image buffer based
 *  render cache is being invalidated and the component is rendered again
 *  (potentially with a new cached image buffer).
 */
final class RenderConf
{
    private static final RenderConf _NONE = new RenderConf(
                                                    BoxModelConf.none(),
                                                    BaseColorConf.none(),
                                                    StyleLayer.empty()
                                                );

    public static RenderConf none() { return _NONE; }

    private final BoxModelConf   _boxModelConf;
    private final BaseColorConf  _baseColor;
    private final StyleLayer     _layer;

    private boolean _wasAlreadyHashed = false;
    private int     _hashCode         = 0; // cached hash code


    private RenderConf(
        BoxModelConf   boxModelConf,
        BaseColorConf  base,
        StyleLayer     layers
    ) {
        _boxModelConf = Objects.requireNonNull(boxModelConf);
        _baseColor    = Objects.requireNonNull(base);
        _layer        = Objects.requireNonNull(layers);
    }

    static RenderConf of(
        BoxModelConf   boxModelConf,
        BaseColorConf  base,
        StyleLayer     layers
    ) {
        if (
            boxModelConf == BoxModelConf.none() &&
            base         == BaseColorConf.none() &&
            layers       == _NONE._layer
        )
            return _NONE;
        else
            return new RenderConf(boxModelConf, base, layers);
    }

    static RenderConf of(UI.Layer layer, ComponentConf fullConf ) {
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

    StyleLayer layer() { return _layer; }

    ComponentAreas areas() { return ComponentAreas.of(_boxModelConf); }


    void paintClippedTo(UI.ComponentArea area, Graphics g, Runnable painter ) {
        Shape oldClip = g.getClip();

        Shape newClip = areas().get(area);
        if ( newClip != null && newClip != oldClip ) {
            newClip = StyleUtility.intersect(newClip, oldClip);
            g.setClip(newClip);
        }

        painter.run();

        g.setClip(oldClip);
    }

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
        RenderConf other = (RenderConf) o;
        return Objects.equals(_boxModelConf, other._boxModelConf) &&
               Objects.equals(_baseColor, other._baseColor) &&
               Objects.equals(_layer, other._layer);
    }


}
