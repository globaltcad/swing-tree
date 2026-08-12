package swingtree.style;

import com.google.errorprone.annotations.Immutable;
import swingtree.UI;

import java.awt.Graphics2D;
import java.util.Objects;

/**
 *  An immutable snapshot of essential component state needed for rendering
 *  the style of a particular component layer using the {@link StyleRenderer} and its
 *  {@link StyleRenderer#renderStyleOn(UI.Layer, LayerRenderConf, Graphics2D)} method. <br>
 *  This (and all of its parts) is immutable to use it as a basis for caching.
 *  When the config changes compared to the previous one, the image buffer based
 *  render cache is being invalidated and the component is rendered again
 *  (potentially with a new cached image buffer).
 *  <p>
 *  <b>Warning to maintainers:</b> the component {@link swingtree.layout.Size} carried by the
 *  {@link #boxModel()} must remain the <i>only</i> size dependent property reachable from here.
 *  {@link LayerPartitionCache} relies on it: for stretch tileable styles it derives a size independent
 *  cache key by swapping in a smaller size through {@link #withBoxModel(BoxModelConf)} and
 *  reconstructs any actual size from the resulting rendering. A newly added field whose value
 *  (or whose rendering) depends on the component size would silently break that reconstruction,
 *  producing subtly wrong pixels rather than a failure. Should such a field become necessary,
 *  it must also be rejected by {@code LayerPartitionCache._isStretchTileable}.
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

    private final Pooled<BoxModelConf> _boxModelConf;
    private final BaseColorConf _baseColor;
    private final StyleConfLayer _layer;

    private LayerRenderConf(
        BoxModelConf   boxModelConf,
        BaseColorConf  base,
        StyleConfLayer layers
    ) {
        _boxModelConf = new Pooled<>(Objects.requireNonNull(boxModelConf)).intern();
        _baseColor    = Objects.requireNonNull(base);
        _layer        = Objects.requireNonNull(layers);
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

    private static LayerRenderConf of(
        final BoxModelConf   boxModelConf,
        final BaseColorConf  base,
        final StyleConfLayer layers
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

    BoxModelConf boxModel() { return _boxModelConf.get(); }

    /**
     *  Returns a new {@link LayerRenderConf} with the supplied box model
     *  and everything else unchanged. Used by {@link LayerPartitionCache} to derive
     *  a size independent canonical render configuration from this one.
     */
    LayerRenderConf withBoxModel( BoxModelConf boxModelConf ) {
        if ( boxModelConf.equals(_boxModelConf.get()) )
            return this;
        return of(boxModelConf, _baseColor, _layer);
    }

    BaseColorConf baseColors() { return _baseColor; }

    /**
     *  Returns a new {@link LayerRenderConf} with the supplied base colors and everything
     *  else unchanged. Used by {@link LayerRenderConfPartitions} to narrow this configuration down to
     *  a single part of the layer.
     */
    LayerRenderConf withBaseColors( BaseColorConf baseColors ) {
        if ( baseColors.equals(_baseColor) )
            return this;
        return of(_boxModelConf.get(), baseColors, _layer);
    }

    StyleConfLayer layer() { return _layer; }

    /**
     *  Returns a new {@link LayerRenderConf} with the supplied style layer and everything
     *  else unchanged. Used by {@link LayerRenderConfPartitions} to narrow this configuration down to
     *  a single part of the layer.
     */
    LayerRenderConf withLayer( StyleConfLayer layer ) {
        if ( layer.equals(_layer) )
            return this;
        return of(_boxModelConf.get(), _baseColor, layer);
    }

    /** Whether handing this to the style renderer would put no pixels anywhere - a common
     *  outcome of narrowing a configuration down to a {@link StyleLayerPart}. */
    boolean rendersNothing() {
        return _baseColor.equals(BaseColorConf.none()) && _layer.isNone();
    }

    ComponentAreas areas() { return ComponentAreas.of(_boxModelConf); }


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
                    "boxModel=" + _boxModelConf.get() + ", " +
                    "baseColor=" + _baseColor + ", " +
                    "layer=" + _layer +
                ']';
    }


}
