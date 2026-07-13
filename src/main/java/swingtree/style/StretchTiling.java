package swingtree.style;

import org.jspecify.annotations.Nullable;
import swingtree.layout.Size;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
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

    /**
     *  Determines if the supplied transform allows for reconstructing the
     *  component rendering through nine tile blits. Anything beyond a positive
     *  scale and a translation (rotation, shear, flips) would require the tiles
     *  to be resampled in ways this class does not support, in which case the
     *  caller must fall back to direct rendering.
     *
     * @param transform The destination graphics transform to check.
     * @return True if {@link #blit(Graphics2D, LayerRenderConf, BufferedImage, BufferedImage[], Size)} may be used.
     */
    static boolean isBlitCompatible( AffineTransform transform ) {
        return transform.getShearX() == 0 &&
               transform.getShearY() == 0 &&
               transform.getScaleX() >  0 &&
               transform.getScaleY() >  0;
    }

    /** Indices into the stretch tile array produced by {@link #extractStretchTiles}. */
    private static final int TOP = 0, LEFT = 1, CENTER = 2, RIGHT = 3, BOTTOM = 4;

    /**
     *  Copies the five stretchable regions of the canonical rendering (the four
     *  edge bands and the center) into dedicated, exactly-fitting images. <br>
     *  <br>
     *  <b>Why this is necessary:</b> the stretched tiles could in principle be
     *  drawn straight out of the canonical image with sub-rectangle
     *  {@code drawImage} calls — and on software surfaces that is pixel perfect.
     *  But on accelerated pipelines (notably XRender on Linux) a scaled blit
     *  whose source is an <i>interior sub-rectangle</i> of a larger texture is
     *  executed as a transformed composite whose fixed-point sampling breaks
     *  down at large stretch ratios: beyond a few hundred times the band either
     *  samples entirely outside the source band or produces nothing at all,
     *  which visually manifested as long component edges losing their shadows.
     *  A scaled blit whose source is a <i>whole image</i> takes the ordinary,
     *  well-trodden scale path and measures pixel perfect even at extreme
     *  ratios — so each stretched tile gets its own image, while the corner
     *  tiles (copied 1:1, never transformed) keep sourcing the canonical image
     *  directly.
     *
     * @param gc The graphics configuration used to allocate compatible (managed) images,
     *           or null (headless), in which case plain ARGB buffers are used.
     * @param canonicalConf The canonical configuration the image was rendered from.
     * @param canonicalImage The canonical rendering to extract the stretchable regions from.
     * @return The five stretch tiles: top band, left band, center, right band, bottom band.
     */
    static BufferedImage[] extractStretchTiles(
        final @Nullable GraphicsConfiguration gc,
        final LayerRenderConf                 canonicalConf,
        final BufferedImage                   canonicalImage
    ) {
        final Outline insets = sliceInsets(canonicalConf);
        final int insetTop    = (int) _positive(insets.top());
        final int insetRight  = (int) _positive(insets.right());
        final int insetBottom = (int) _positive(insets.bottom());
        final int insetLeft   = (int) _positive(insets.left());
        final int width  = canonicalImage.getWidth();
        final int height = canonicalImage.getHeight();

        final BufferedImage[] tiles = new BufferedImage[5];
        tiles[TOP]    = _copyRegion(gc, canonicalImage, insetLeft,          0,                    width - insetRight, insetTop           );
        tiles[LEFT]   = _copyRegion(gc, canonicalImage, 0,                  insetTop,             insetLeft,          height - insetBottom);
        tiles[CENTER] = _copyRegion(gc, canonicalImage, insetLeft,          insetTop,             width - insetRight, height - insetBottom);
        tiles[RIGHT]  = _copyRegion(gc, canonicalImage, width - insetRight, insetTop,             width,              height - insetBottom);
        tiles[BOTTOM] = _copyRegion(gc, canonicalImage, insetLeft,          height - insetBottom, width - insetRight, height             );
        return tiles;
    }

    private static BufferedImage _copyRegion(
        final @Nullable GraphicsConfiguration gc,
        final BufferedImage source,
        final int x1, final int y1, final int x2, final int y2
    ) {
        final int width  = Math.max(1, x2 - x1);
        final int height = Math.max(1, y2 - y1);
        final BufferedImage region = ( gc != null )
                    ? gc.createCompatibleImage(width, height, Transparency.TRANSLUCENT)
                    : new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        region.setAccelerationPriority(1.0f);
        final Graphics2D g = region.createGraphics();
        try {
            g.setComposite(AlphaComposite.Src); // exact pixel copy, including alpha
            g.drawImage(source, 0, 0, width, height, x1, y1, x2, y2, null);
        } finally {
            g.dispose();
        }
        return region;
    }

    /**
     *  Reconstructs the rendering of the supplied canonical image at the
     *  supplied actual component size by drawing nine tiles: the four corners
     *  1:1 (in user space) straight from the canonical image, the four edge
     *  bands stretched along their edge and the center stretched in both
     *  directions — the latter five from their dedicated tile images
     *  (see {@link #extractStretchTiles} for why they must be dedicated). <br>
     *  <br>
     *  The tiles are drawn in <b>integer device space</b>: the cut lines are
     *  transformed to device pixels once and shared between adjacent tiles, so
     *  that under fractional HiDPI scales the independent rounding of nine
     *  user space rectangles can never produce one pixel gaps or double
     *  blended overlaps. Nearest neighbor interpolation ensures that
     *  stretching a constant source band produces an exactly constant
     *  destination band and that sampling never bleeds across tile boundaries. <br>
     *  <br>
     *  The caller must ensure {@link #isBlitCompatible(AffineTransform)}
     *  holds for the graphics transform and that the actual size is strictly
     *  larger than the canonical image in both dimensions
     *  (which {@link #canonicalize(LayerRenderConf)} guarantees).
     *
     * @param g The destination graphics to draw the tiles into.
     * @param canonicalConf The canonical configuration the image was rendered from,
     *                      used to recompute the slice insets (a pure function,
     *                      so it is guaranteed to be consistent with the
     *                      canonicalization that produced the image).
     * @param canonicalImage The canonical rendering, sourcing the four corner tiles.
     * @param stretchTiles The dedicated stretchable tiles from {@link #extractStretchTiles}.
     * @param actualSize The actual component size to reconstruct.
     */
    static void blit(
        final Graphics2D      g,
        final LayerRenderConf canonicalConf,
        final BufferedImage   canonicalImage,
        final BufferedImage[] stretchTiles,
        final Size            actualSize
    ) {
        final Outline insets = sliceInsets(canonicalConf);
        final int insetTop    = (int) _positive(insets.top());
        final int insetRight  = (int) _positive(insets.right());
        final int insetBottom = (int) _positive(insets.bottom());
        final int insetLeft   = (int) _positive(insets.left());

        final int canonicalWidth  = canonicalImage.getWidth();
        final int canonicalHeight = canonicalImage.getHeight();
        final float actualWidth  = actualSize.widthOrElse(0f);
        final float actualHeight = actualSize.heightOrElse(0f);

        final AffineTransform transform = g.getTransform();
        final double scaleX     = transform.getScaleX();
        final double scaleY     = transform.getScaleY();
        final double translateX = transform.getTranslateX();
        final double translateY = transform.getTranslateY();

        // The horizontal and vertical cut lines in integer device space,
        // shared between adjacent tiles (no seams, no overlaps):
        final int[] dx = {
                        (int) Math.round(translateX),
                        (int) Math.round(translateX + insetLeft * scaleX),
                        (int) Math.round(translateX + (actualWidth - insetRight) * scaleX),
                        (int) Math.round(translateX + actualWidth * scaleX)
                    };
        final int[] dy = {
                        (int) Math.round(translateY),
                        (int) Math.round(translateY + insetTop * scaleY),
                        (int) Math.round(translateY + (actualHeight - insetBottom) * scaleY),
                        (int) Math.round(translateY + actualHeight * scaleY)
                    };
        // The corresponding cut lines in the canonical source image:
        final int[] sx = { 0, insetLeft, canonicalWidth  - insetRight,  canonicalWidth  };
        final int[] sy = { 0, insetTop,  canonicalHeight - insetBottom, canonicalHeight };

        final Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setTransform(new AffineTransform()); // We draw in device space.
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            // The four corners, 1:1 sub-rectangle copies from the canonical image:
            _drawRegion(g2, canonicalImage, dx[0], dy[0], dx[1], dy[1], sx[0], sy[0], sx[1], sy[1]); // top-left
            _drawRegion(g2, canonicalImage, dx[2], dy[0], dx[3], dy[1], sx[2], sy[0], sx[3], sy[1]); // top-right
            _drawRegion(g2, canonicalImage, dx[0], dy[2], dx[1], dy[3], sx[0], sy[2], sx[1], sy[3]); // bottom-left
            _drawRegion(g2, canonicalImage, dx[2], dy[2], dx[3], dy[3], sx[2], sy[2], sx[3], sy[3]); // bottom-right
            // The stretched bands and center, each a whole dedicated image:
            _drawStretched(g2, stretchTiles[TOP],    dx[1], dy[0], dx[2], dy[1]);
            _drawStretched(g2, stretchTiles[LEFT],   dx[0], dy[1], dx[1], dy[2]);
            _drawStretched(g2, stretchTiles[CENTER], dx[1], dy[1], dx[2], dy[2]);
            _drawStretched(g2, stretchTiles[RIGHT],  dx[2], dy[1], dx[3], dy[2]);
            _drawStretched(g2, stretchTiles[BOTTOM], dx[1], dy[2], dx[2], dy[3]);
        } finally {
            g2.dispose();
        }
    }

    private static void _drawRegion(
        final Graphics2D g2, final BufferedImage source,
        final int dx1, final int dy1, final int dx2, final int dy2,
        final int sx1, final int sy1, final int sx2, final int sy2
    ) {
        if ( dx2 <= dx1 || dy2 <= dy1 )
            return; // Degenerate tile, nothing to draw (a negative span would mirror the image!).
        g2.drawImage(source, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
    }

    private static void _drawStretched(
        final Graphics2D g2, final BufferedImage tile,
        final int dx1, final int dy1, final int dx2, final int dy2
    ) {
        if ( dx2 <= dx1 || dy2 <= dy1 )
            return; // Degenerate tile, nothing to draw.
        g2.drawImage(tile, dx1, dy1, dx2, dy2, 0, 0, tile.getWidth(), tile.getHeight(), null);
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
