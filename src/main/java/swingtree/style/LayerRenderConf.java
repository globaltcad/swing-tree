package swingtree.style;

import com.google.errorprone.annotations.Immutable;
import swingtree.UI;

import java.awt.Graphics2D;
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
@Immutable
@SuppressWarnings("Immutable")
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
            boxModelConf .equals( BoxModelConf.none()  ) &&
            base         .equals( BaseColorConf.none() ) &&
            layers       .equals( _NONE._layer )
        )
            return _NONE;
        else
            return new LayerRenderConf(boxModelConf, base, layers);
    }

    static LayerRenderConf of( UI.Layer layer, ComponentConf fullConf ) {
        BoxModelConf boxModelConf = BoxModelConf.of(
                                        fullConf.style().border(),
                                        fullConf.areaMarginCorrection(),
                                        fullConf.currentBounds().size()
                                    );
        BaseColorConf colorConf = BaseColorConf.of(
                                    fullConf.style().base().foundationColor().filter( c -> layer == UI.Layer.BACKGROUND ).orElse(null),
                                    fullConf.style().base().backgroundColor().filter( c -> layer == UI.Layer.BACKGROUND ).orElse(null),
                                    layer == UI.Layer.BORDER ? fullConf.style().border().colors() : BorderColorsConf.none()
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

    ComponentAreas areas() { return _boxModelConf.areas(); }


    @Override
    public int hashCode() {
        return Objects.hash(_boxModelConf, _baseColor, _layer);
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

    @Override
    public String toString() {
        return getClass().getSimpleName()+"[" +
                    "boxModel=" + _boxModelConf + ", " +
                    "baseColor=" + _baseColor + ", " +
                    "layer=" + _layer +
                ']';
    }


}
