package swingtree.style;

import com.google.errorprone.annotations.Immutable;
import swingtree.UI;
import swingtree.layout.Size;

import java.awt.Graphics2D;
import java.util.Objects;
import java.util.Optional;

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
 *  {@link #_compactionFor(LayerRenderConf)} must return {@link Compaction#NONE} whenever it is
 *  set.
 */
@Immutable
@SuppressWarnings("Immutable")
final class LayerRenderConf
{
    private static final int STRETCH_BAND  = 2; // Freely stretchable band between the slice insets; one pixel suffices mathematically, two give slack.
    private static final int SAFETY_MARGIN = 2; // Added to every slice inset to absorb antialiasing bleed and artifact adjustments in the renderer.
    private static final LayerRenderConf _NONE = new LayerRenderConf(
                                                    BoxModelConf.none(),
                                                    BaseColorConf.none(),
                                                    StyleConfLayer.empty()
                                                );

    public static LayerRenderConf none() { return _NONE; }

    private final Pooled<BoxModelConf> _boxModelConf;
    private final BaseColorConf _baseColor;
    private final StyleConfLayer _layer;
    private final LazyRef<LayerRenderConf> _canonicalRepresentation;
    private final LazyRef<Outline> _nineTileSliceInsets;

    private LayerRenderConf(
        BoxModelConf   boxModelConf,
        BaseColorConf  base,
        StyleConfLayer layers
    ) {
        _boxModelConf = new Pooled<>(Objects.requireNonNull(boxModelConf)).intern();
        _baseColor    = Objects.requireNonNull(base);
        _layer        = Objects.requireNonNull(layers);
        _canonicalRepresentation = new LazyRef<>(this, LayerRenderConf::_canonicalize);
        _nineTileSliceInsets = new LazyRef<>(this, LayerRenderConf::_compute9PatchSliceInsets);
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
     *  else unchanged. Used by {@link LayerRenderConfPartition} to narrow this configuration down to
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
     *  else unchanged. Used by {@link LayerRenderConfPartition} to narrow this configuration down to
     *  a single part of the layer.
     */
    LayerRenderConf withLayer( StyleConfLayer layer ) {
        if ( layer.equals(_layer) )
            return this;
        return of(_boxModelConf.get(), _baseColor, layer);
    }

    /** Whether handing this to the style renderer would put no pixels anywhere - a common
     *  outcome of narrowing a configuration down to a {@link LayerRenderConfPartition}. */
    boolean rendersNothing() {
        return _baseColor.equals(BaseColorConf.none()) && _layer.isNone();
    }

    ComponentAreas areas() { return ComponentAreas.of(_boxModelConf); }

    LayerRenderConf canonicalRepresentation() {
        return _canonicalRepresentation.get();
    }

    Outline nineTileSliceInsets() {
        return _nineTileSliceInsets.get();
    }

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


    // Canonical (size independent) representation for 9 patch caching:

    /**
     *  Maps a configuration of any size onto its exemplar key, compacting only the dimensions
     *  this layer may be stretched along (see the {@link LayerPartitionCache} class
     *  documentation). A configuration we cannot compact, or one with no room to stretch, comes
     *  back unchanged. That also makes this idempotent: a configuration already at the exemplar
     *  size maps onto itself.
     */
    private static LayerRenderConf _canonicalize( LayerRenderConf conf ) {
        final Compaction compaction = _compactionFor(conf);
        if ( compaction == Compaction.NONE )
            return conf;

        final Outline sliceInsets = conf.nineTileSliceInsets();
        final Size    exemplar    = _exemplarSize(sliceInsets);
        final Size    actual      = conf.boxModel().size();

        if ( !_borderEdgeSeamsAreSizeIndependent(conf, sliceInsets, exemplar) )
            return conf;

        if ( !_hasRoomToStretch(actual, exemplar) )
            return conf;

        final Size canonical = Size.of(
                                    compaction.includesWidth()  ? exemplar.widthOrElse(0f)  : actual.widthOrElse(0f),
                                    compaction.includesHeight() ? exemplar.heightOrElse(0f) : actual.heightOrElse(0f)
                                );

        return conf.withBoxModel(conf.boxModel().withSize(canonical));
    }

    /**
     *  Whichever dimension we stretch, the blit still cuts nine tiles and so still cuts along all
     *  four slice insets. That means both dimensions must exceed the exemplar's, even when only
     *  one of them is compacted.
     */
    private static boolean _hasRoomToStretch( Size actual, Size exemplar ) {
        return actual.widthOrElse(0f)  > exemplar.widthOrElse(0f)
            && actual.heightOrElse(0f) > exemplar.heightOrElse(0f);
    }

    /**
     *  Which of an exemplar's two dimensions we compacted to their minimum instead of taking
     *  them from the component, and stretch back on paint. A resize in a compacted dimension
     *  keeps the key; a resize in an uncompacted dimension gives a new one.
     */
    enum Compaction
    {
        NONE(false, false), WIDTH(true, false), HEIGHT(false, true), BOTH(true, true);

        static Compaction between( Size key, Size actual ) {
            return _of(
                    key.widthOrElse(0f)  != actual.widthOrElse(0f),
                    key.heightOrElse(0f) != actual.heightOrElse(0f)
                );
        }

        private final boolean _width;
        private final boolean _height;

        Compaction( boolean width, boolean height ) {
            _width  = width;
            _height = height;
        }

        boolean includesWidth()  { return _width;  }
        boolean includesHeight() { return _height; }

        /** The dimensions both of them allow, for a layer that has to satisfy two of these at once. */
        Compaction and( Compaction other ) {
            return _of(_width && other._width, _height && other._height);
        }

        private static Compaction _of( boolean width, boolean height ) {
            if ( width )
                return height ? BOTH : WIDTH;
            else
                return height ? HEIGHT : NONE;
        }
    }

    /**
     *  How far the size dependent pixels reach into the component from each side; everything
     *  between opposite insets repeats and may be stretched freely. A pure function of the size
     *  independent parts of the configuration, so the blit can recompute it and is guaranteed
     *  to agree with the canonicalization.
     */
    private static Outline _compute9PatchSliceInsets(LayerRenderConf conf ) {
        final BoxModelConf box = conf.boxModel();

        final float marginTop    = _positive(box.margin().top());
        final float marginRight  = _positive(box.margin().right());
        final float marginBottom = _positive(box.margin().bottom());
        final float marginLeft   = _positive(box.margin().left());

        final float baseTop    = _positive(box.baseOutline().top());
        final float baseRight  = _positive(box.baseOutline().right());
        final float baseBottom = _positive(box.baseOutline().bottom());
        final float baseLeft   = _positive(box.baseOutline().left());

        final float widthTop    = _positive(box.widths().top());
        final float widthRight  = _positive(box.widths().right());
        final float widthBottom = _positive(box.widths().bottom());
        final float widthLeft   = _positive(box.widths().left());

        // Per side, the larger of the two adjacent corner arc extents:
        final float arcTop    = Math.max(_arcHeight(box.topLeftArc()),    _arcHeight(box.topRightArc())   );
        final float arcRight  = Math.max(_arcWidth(box.topRightArc()),    _arcWidth(box.bottomRightArc()) );
        final float arcBottom = Math.max(_arcHeight(box.bottomLeftArc()), _arcHeight(box.bottomRightArc()));
        final float arcLeft   = Math.max(_arcWidth(box.topLeftArc()),     _arcWidth(box.bottomLeftArc())  );

        /*
            A shadow's 2D-varying pixels extend beyond its geometric box: its gradients
            fade over blur + spread + gradient start offset, and the whole shadow box is
            displaced by the shadow offset. We conservatively use the same reach for all
            four sides (overestimation only costs a few exemplar pixels).
        */
        float shadowReachH = 0;
        float shadowReachV = 0;
        for ( ShadowConf shadow : conf.layer().shadows().sortedByNames() ) {
            if ( shadow.equals(ShadowConf.none()) || !shadow.color().isPresent() )
                continue;
            final float blur   = Math.max(0, shadow.blurRadius());
            final float spread = Math.abs(shadow.spreadRadius());
            final float fade   = StyleRenderer.shadowGradientStartOffset(box, shadow);
            shadowReachH = Math.max(shadowReachH, blur + spread + fade + Math.abs(shadow.horizontalOffset()));
            shadowReachV = Math.max(shadowReachV, blur + spread + fade + Math.abs(shadow.verticalOffset()));
        }

        final float top    = marginTop    + baseTop    + widthTop    + arcTop    + shadowReachV + SAFETY_MARGIN;
        final float right  = marginRight  + baseRight  + widthRight  + arcRight  + shadowReachH + SAFETY_MARGIN;
        final float bottom = marginBottom + baseBottom + widthBottom + arcBottom + shadowReachV + SAFETY_MARGIN;
        final float left   = marginLeft   + baseLeft   + widthLeft   + arcLeft   + shadowReachH + SAFETY_MARGIN;

        return Outline.of(
                    (float) Math.ceil(top),
                    (float) Math.ceil(right),
                    (float) Math.ceil(bottom),
                    (float) Math.ceil(left)
                );
    }

    /** Which dimensions we may compact: we can shrink the width when every pixel strip along
     *  the y axis is identical, and the height when every pixel strip along the x axis is. */
    private static Compaction _compactionFor( LayerRenderConf conf ) {
        final StyleConfLayer layer = conf.layer();

        for ( Pooled<NoiseConf> noise : layer.noises().sortedByNames() )
            if ( !noise.get().equals(NoiseConf.none()) )
                return Compaction.NONE; // Noise varies per pixel position.

        for ( ImageConf image : layer.images().sortedByNames() )
            if ( !image.equals(ImageConf.none()) )
                return Compaction.NONE; // Image placement/fit depends on the component bounds.

        for ( TextConf text : layer.texts().sortedByNames() )
            if ( !text.equals(TextConf.none()) )
                return Compaction.NONE; // Text layout depends on the component bounds.

        for ( PainterConf painter : layer.painters().sortedByNames() )
            if ( !painter.equals(PainterConf.none()) )
                return Compaction.NONE; // We cannot know what a custom painter does.

        Compaction compaction = Compaction.BOTH;
        for ( GradientConf gradient : layer.gradients().sortedByNames() ) {
            if ( gradient.equals(GradientConf.none()) )
                continue;
            compaction = compaction.and(_compactionAllowedBy(gradient));
            if ( compaction == Compaction.NONE )
                return compaction;
        }
        return compaction;
    }

    /**
     *  A linear gradient straight down a component is built by
     *  {@link StyleRenderer#createGradientPaint(BoxModelConf, GradientConf)} from two points
     *  sharing an x coordinate, so the component width never enters its rendering and we can
     *  compact the width; a gradient straight across is the same with the axes swapped. We turn
     *  down every other kind, because they vary with x and y at once -
     *  {@link UI.ComponentBoundary#CENTER_TO_CONTENT} because it derives its insets from the
     *  component size itself.
     */
    private static Compaction _compactionAllowedBy( GradientConf gradient ) {
        if ( gradient.type() != UI.GradientType.LINEAR )
            return Compaction.NONE;
        if ( gradient.rotation() % 360f != 0f )
            return Compaction.NONE;
        if ( gradient.boundary() == UI.ComponentBoundary.CENTER_TO_CONTENT )
            return Compaction.NONE;
        switch ( gradient.span() ) {
            case TOP_TO_BOTTOM:
            case BOTTOM_TO_TOP:
                return Compaction.WIDTH;
            case LEFT_TO_RIGHT:
            case RIGHT_TO_LEFT:
                return Compaction.HEIGHT;
            default:
                return Compaction.NONE;
        }
    }

    /**
     *  Whether the seams of a border with a different color per edge fall in the same place in
     *  the exemplar as they do at any larger size, which is what lets such a border be
     *  reconstructed from an exemplar rather than re-rendered per size. <br>
     *  <br>
     *  Note that the border edges divide the <i>margin box</i> rather than the component: the
     *  seams are placed within, and the slice insets measured from, two different origins, so the
     *  margins have to be taken out of both before they can be compared.
     * @see ComponentAreas#getEdgeAreas() For more context related code...
     */
    private static boolean _borderEdgeSeamsAreSizeIndependent(
        LayerRenderConf conf,
        Outline         sliceInsets,
        Size            exemplar
    ) {
        final BorderColorsConf borderColors = conf.baseColors().borderColor();
        if ( borderColors.equals(BorderColorsConf.none()) || borderColors.isHomogeneous() )
            return true;
        if ( !conf.boxModel().hasAnyNonZeroArcs() )
            return true;

        final Outline widths = conf.boxModel().widths();
        final float   top    = _positive(widths.top());
        final float   right  = _positive(widths.right());
        final float   bottom = _positive(widths.bottom());
        final float   left   = _positive(widths.left());
        if ( top <= 0 || right <= 0 || bottom <= 0 || left <= 0 )
            return false;

        final Outline margin       = conf.boxModel().margin();
        final float   marginTop    = _positive(margin.top());
        final float   marginRight  = _positive(margin.right());
        final float   marginBottom = _positive(margin.bottom());
        final float   marginLeft   = _positive(margin.left());

        final float boxWidth  = exemplar.widthOrElse(0f)  - marginLeft - marginRight;
        final float boxHeight = exemplar.heightOrElse(0f) - marginTop  - marginBottom;
        if ( boxWidth <= 0 || boxHeight <= 0 )
            return false;

        return boxHeight * top    > ( _positive(sliceInsets.top())    - marginTop    ) * ( top  + bottom )
            && boxHeight * bottom > ( _positive(sliceInsets.bottom()) - marginBottom ) * ( top  + bottom )
            && boxWidth  * left   > ( _positive(sliceInsets.left())   - marginLeft   ) * ( left + right  )
            && boxWidth  * right  > ( _positive(sliceInsets.right())  - marginRight  ) * ( left + right  );
    }

    /**
     *  The exemplar size for the supplied slice insets: {@code 2 * max(insetA, insetB) + band}
     *  per axis. Symmetric, because some rendering internals split their work at the component
     *  <i>center</i> - corner shadow clip boxes meet there, and so do the seams between two
     *  opposite border edges. The symmetric size guarantees the exemplar's center line falls
     *  into the repeating band, keeping those artifacts pixel-identical to a real rendering of
     *  any larger size.
     */
    private static Size _exemplarSize( Outline sliceInsets ) {
        final float maxHorizontal = Math.max(sliceInsets.left().orElse(0f), sliceInsets.right().orElse(0f));
        final float maxVertical   = Math.max(sliceInsets.top().orElse(0f),  sliceInsets.bottom().orElse(0f));
        return Size.of(
                    2 * maxHorizontal + STRETCH_BAND,
                    2 * maxVertical   + STRETCH_BAND
                );
    }

    private static float _positive( Optional<Float> value ) {
        return Math.max(0f, value.orElse(0f));
    }

    private static float _arcWidth( Optional<Arc> arc ) {
        return arc.map( a -> Math.max(0f, a.width()) ).orElse(0f);
    }

    private static float _arcHeight( Optional<Arc> arc ) {
        return arc.map( a -> Math.max(0f, a.height()) ).orElse(0f);
    }
}
