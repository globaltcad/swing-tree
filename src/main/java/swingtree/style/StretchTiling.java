package swingtree.style;

import swingtree.layout.Size;

import java.util.Optional;

/**
 *  A purely static utility which decides if and how the rendering of a
 *  particular {@link LayerRenderConf} can be cached <b>independently of the
 *  component size</b> using a "nine slice" (also known as "stretch tiling") scheme. <br>
 *  <br>
 *  <b>The problem this solves:</b> The {@link LayerCache} is keyed by the full
 *  {@link LayerRenderConf}, whose {@link BoxModelConf} includes the exact component
 *  {@link Size}. So while a component is being resized (think of a user dragging
 *  the window edge), every single paint produces a brand new cache key, every
 *  frame is a cache miss and the style is rasterized from scratch, which is
 *  extremely expensive for antialiased rounded corners and shadows. <br>
 *  <br>
 *  <b>The key insight:</b> {@link Size} is the <i>only</i> size dependent
 *  property in the entire render configuration. Corner arcs, border widths,
 *  margins, paddings and shadow parameters are all fixed pixel values.
 *  For style content whose pixels are <i>constant along the edges</i> of the
 *  component (flat background/foundation fills, borders, shadows), the rendering
 *  at any size can be reconstructed exactly from a single small "canonical"
 *  rendering by cutting it into nine tiles:
 *  <ul>
 *      <li>the four corner tiles are copied 1:1 (their pixels never change),</li>
 *      <li>the four edge tiles are stretched along their edge
 *          (their pixels are constant in the stretch direction),</li>
 *      <li>the center tile is stretched in both directions
 *          (its pixels are one constant color).</li>
 *  </ul>
 *  So the {@link #canonicalize(LayerRenderConf)} method maps every eligible
 *  render configuration of every size onto <b>one shared, size independent
 *  cache key</b>: the same configuration with its size replaced by the minimal
 *  {@link #canonicalSize(Outline)}. The canonical configuration is a perfectly
 *  valid {@link LayerRenderConf}, which means the regular {@link StyleRenderer}
 *  renders the canonical image without knowing about any of this. <br>
 *  <br>
 *  <b>Where the cut lines are:</b> {@link #sliceInsets(LayerRenderConf)} computes,
 *  for each side, how far the size dependent (non-constant) pixels reach into the
 *  component: margin + base outline + border width + the adjacent corner arc
 *  extents + the shadow reach (blur + spread + gradient fade distance + offset),
 *  plus a small safety margin for antialiasing bleed and rendering artifact
 *  adjustments. Everything between opposite cut lines is constant along the
 *  respective axis and may be stretched freely. <br>
 *  <br>
 *  <b>Why the canonical size is symmetric per axis:</b> Some rendering internals
 *  split their work at the component <i>center</i> (corner shadow clip boxes as
 *  well as the border edge polygons both meet in the middle). By making the
 *  canonical size {@code 2 * max(insetA, insetB) + STRETCH_BAND} per axis, the
 *  canonical center line is guaranteed to fall into the constant band, so these
 *  center-splitting artifacts are pixel-identical to a real rendering of any
 *  larger size. <br>
 *  <br>
 *  <b>What is not eligible (v1):</b> gradients, noises, images, texts and
 *  painters all produce pixels which depend on the full component size or on the
 *  absolute pixel position, so configurations containing any of these fall back
 *  to the classic exact-size caching (canonicalization returns the configuration
 *  unchanged, and everything behaves exactly as it did before this class existed).
 *  Non-homogeneous (per-edge) border colors are only eligible without corner
 *  arcs, because the diagonal color seam between two differently colored edges
 *  runs towards the component center, so inside a rounded corner its slope
 *  (and therefore the corner pixels) depends on the component aspect ratio.
 */
final class StretchTiling
{
    /**
     *  The width/height of the freely stretchable band between the slice insets
     *  of the canonical rendering. One pixel would suffice mathematically, two
     *  give slack against off-by-one region math.
     */
    static final int STRETCH_BAND = 2;

    /**
     *  Extra pixels added to every slice inset to absorb antialiasing bleed
     *  and small rendering artifact adjustments (like the outset shadow's
     *  one pixel artifact adjustment in {@link StyleRenderer}).
     */
    static final int SAFETY_MARGIN = 2;

    private StretchTiling() {}

    /**
     *  Maps the supplied render configuration onto its size independent
     *  canonical form, or returns it <b>unchanged</b> when stretch tiling does
     *  not apply (ineligible content, or the component is not strictly larger
     *  than the canonical size in both dimensions). <br>
     *  This method is idempotent: a configuration which already has the
     *  canonical size is returned unchanged (its size is not strictly larger
     *  than the canonical size), so canonical configurations map onto themselves.
     *
     * @param conf The layer render configuration to canonicalize.
     * @return The canonical configuration acting as a size independent cache key,
     *         or {@code conf} itself if stretch tiling does not apply.
     */
    static LayerRenderConf canonicalize( LayerRenderConf conf ) {
        if ( !isEligible(conf) )
            return conf;

        final Outline sliceInsets = sliceInsets(conf);
        final Size    canonical   = canonicalSize(sliceInsets);
        final Size    actual      = conf.boxModel().size();

        final boolean strictlyLarger =
                        actual.widthOrElse(0f)  > canonical.widthOrElse(0f) &&
                        actual.heightOrElse(0f) > canonical.heightOrElse(0f);
        if ( !strictlyLarger )
            return conf;

        return conf.withBoxModel(conf.boxModel().withSize(canonical));
    }

    /**
     *  Determines if the <i>content</i> of the supplied render configuration
     *  only consists of style elements whose pixels are constant along the
     *  component edges, which is the precondition for reconstructing any
     *  component size from the canonical rendering through nine tile blits.
     *  Note that this does not check the component size, see
     *  {@link #canonicalize(LayerRenderConf)} for the size gate.
     *
     * @param conf The layer render configuration to check.
     * @return True if the configuration content is stretch tiling safe.
     */
    static boolean isEligible( LayerRenderConf conf ) {
        final StyleConfLayer layer = conf.layer();

        for ( GradientConf gradient : layer.gradients().sortedByNames() )
            if ( !gradient.equals(GradientConf.none()) )
                return false; // Gradient geometry spans the component bounds.

        for ( Pooled<NoiseConf> noise : layer.noises().sortedByNames() )
            if ( !noise.get().equals(NoiseConf.none()) )
                return false; // Noise varies per pixel position.

        for ( ImageConf image : layer.images().sortedByNames() )
            if ( !image.equals(ImageConf.none()) )
                return false; // Image placement/fit depends on the component bounds.

        for ( TextConf text : layer.texts().sortedByNames() )
            if ( !text.equals(TextConf.none()) )
                return false; // Text layout depends on the component bounds.

        for ( PainterConf painter : layer.painters().sortedByNames() )
            if ( !painter.equals(PainterConf.none()) )
                return false; // We cannot know what a custom painter does.

        // Shadows and flat base/border colors are eligible, except for the
        // per-edge border color seam inside rounded corners (see class javadoc):
        final BorderColorsConf borderColors = conf.baseColors().borderColor();
        if ( !borderColors.equals(BorderColorsConf.none()) && !borderColors.isHomogeneous() )
            if ( conf.boxModel().hasAnyNonZeroArcs() )
                return false;

        return true;
    }

    /**
     *  Computes for each side of the component how far the size dependent
     *  pixels of the supplied render configuration reach into the component.
     *  Between opposite insets the rendered pixels are constant along the
     *  respective axis. All values are whole numbers (ceiled). <br>
     *  This is a pure function of the size independent parts of the
     *  configuration, which is why it may be recomputed at blit time and
     *  is guaranteed to be consistent with the canonicalization.
     *
     * @param conf The layer render configuration to analyze.
     * @return The slice insets in the form of an {@link Outline}.
     */
    static Outline sliceInsets( LayerRenderConf conf ) {
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
            The reach of a shadow is how far its 2D-varying pixels (corner
            gradients and gradient fades) extend from the shadow box border:
            the gradients fade over blur + spread + gradient start offset,
            and the whole shadow box is displaced by the shadow offset.
            We conservatively use the same reach for all four sides
            (overestimation only costs a few canonical pixels).
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

    /**
     *  The size of the canonical rendering for the supplied slice insets.
     *  Symmetric per axis (see class javadoc for why), leaving a freely
     *  stretchable band of at least {@link #STRETCH_BAND} pixels between
     *  the cut lines of each axis.
     *
     * @param sliceInsets The slice insets as computed by {@link #sliceInsets(LayerRenderConf)}.
     * @return The canonical component size.
     */
    static Size canonicalSize( Outline sliceInsets ) {
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
