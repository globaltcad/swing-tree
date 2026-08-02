package swingtree.style;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import swingtree.SwingTree;
import swingtree.UI;
import swingtree.layout.Size;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;

/**
 *  A {@link BufferedImage} based cache for the rendering of one {@link LayerRenderConfPartitions} of a
 *  particular layer of a component's style - which is ordinarily {@link LayerRenderConfPartitions#WHOLE},
 *  the entire layer. <br>
 *  Caching is keyed by the deeply immutable {@link LayerRenderConf} of that part: as long as it
 *  stays equal across paint calls, the cached image is blitted instead of re-rendered, and when
 *  it changes, the entry is invalidated. An instance of this exists per component, layer and
 *  part (inside the style engine), while the rendered images live in a global, weakly keyed pool
 *  shared by all components with an equal configuration. <br>
 *  <br>
 *  <b>Size independent caching through stretch tiling ("nine slice"):</b><br>
 *  The render configuration includes the exact component {@link Size}, so naively every frame of
 *  a live resize would be a cache miss and heavily styled components would be rasterized from
 *  scratch dozens of times per second. For eligible styles this cache therefore keys its entry
 *  on a <b>minimal exemplar</b> of the style instead: the same configuration with its size
 *  replaced by the smallest size at which every size dependent pixel still exists. Because
 *  {@link Size} is the <i>only</i> size dependent property in the whole configuration, the
 *  exemplar is both a size independent cache key (all sizes of a style collapse onto one value)
 *  and an honest render recipe (the {@link StyleRenderer} receives exactly what a real component
 *  of that small size would send - it never learns that tiling exists). The exemplar rendering
 *  then acts like a small texture atlas from which any actual size is reconstructed with nine
 *  tile blits: the four corners copied 1:1, the four edge bands and the center stretched - the
 *  technique behind Android 9-patch drawables and CSS {@code border-image}. <br>
 *  <br>
 *  The reconstruction is exact, not an approximation, but it rests on an invariant: <b>the body
 *  must be homogeneous, and each edge must be homogeneous along its own axis</b>. Flat
 *  background/foundation fills, borders and shadows satisfy it. Gradients, noises, images, texts
 *  and custom painters do not (their pixels depend on the full component bounds), and neither do
 *  per-edge border colors combined with corner arcs (the diagonal color seam between two
 *  differently colored edges runs towards the component center, so inside a rounded corner its
 *  slope - and thereby the corner pixels - depends on the component aspect ratio). The
 *  eligibility check must stay conservative, because an over-eager rule produces subtly wrong
 *  pixels, not a crash. Ineligible or too small configurations keep the classic exact-size key
 *  and behave exactly as they did before stretch tiling existed. <br>
 *  <br>
 *  Two related configurations are therefore managed here, and telling them apart is essential
 *  for understanding this class:
 *  <ul>
 *      <li><b>the render input</b> ({@code _layerRenderData}) - always the actual configuration
 *          at the real component size. All direct-render fallbacks receive it, and it determines
 *          the destination geometry of the final cache blit.</li>
 *      <li><b>the cache key</b> ({@code _cacheKey}) - the canonical (possibly exemplar sized)
 *          form of the render input. It keys the entry in the global cache, it is what the
 *          renderer receives when filling the shared image, and its strong reference is what
 *          keeps the weakly keyed entry alive.</li>
 *  </ul>
 */
final class LayerPartitionCache
{
    private static final Logger log = LoggerFactory.getLogger(LayerPartitionCache.class);

    private static final int    MAX_CACHE_ENTRIES                 = 1024; // There can never be more entries!
    private static final int    PIXELS_PER_UNIT_OF_AGGRESSIVENESS = 256 * 256; // Determines how many pixels a single unit of cache aggressiveness can cache
    private static final double EAGER_ALLOCATION_FRIENDLINESS     = 0.1; // Has to be between 0 and 1!
    private static final int    MAX_CACHE_HIT_COUNT               = 12;
    private static final int    STRETCH_BAND                      = 2; // Freely stretchable band between the slice insets; one pixel suffices mathematically, two give slack.
    private static final int    SAFETY_MARGIN                     = 2; // Added to every slice inset to absorb antialiasing bleed and artifact adjustments in the renderer.
    private static final int    BYTES_PER_PIXEL                   = 4; // Every cached rendering is 32 bit ARGB, see CachedImage._allocate.

    /** The largest device-pixel area a single style-layer image may occupy to still be
     *  cached. Expressed in {@link CacheBudget#units()} so that, at the default mode,
     *  <em>which</em> components qualify for caching is exactly as it always was. */
    private static int _maxCacheableImageArea() {
        return (int) (CacheBudget.units() * PIXELS_PER_UNIT_OF_AGGRESSIVENESS);
    }

    /** The backstop on how many style-layer images the global cache retains, derived from
     *  this cache's slice of the shared {@link CacheBudget} byte budget (so the total
     *  footprint is tangible) and clamped to an absolute ceiling. */
    private static int _maxCacheEntries() {
        return Math.min(MAX_CACHE_ENTRIES, CacheBudget.maxEntriesFor(CacheBudget.Kind.STYLE_LAYER));
    }

    private static final Map<Pooled<LayerRenderConf>, CachedImage> _CACHE = new WeakHashMap<>();

    /** What the live entries have reserved right now - which is what they will occupy, but
     *  counted from the moment an entry exists rather than from the moment its buffer is
     *  actually allocated, see {@link CachedImage#reservedBytes()}. */
    private static long _bytesReservedByCache() {
        long total = 0;
        for ( CachedImage image : _CACHE.values() )
            total += image.reservedBytes();
        return total;
    }

    /** Live number of cached style-layer renderings (for monitoring/tests). */
    static int globalEntryCount() {
        return _CACHE.size();
    }

    /** Live bytes reserved by the cached style-layer renderings (for monitoring/tests).
     *  Must be called on the painting thread: it walks the same map that painting writes to. */
    static long globalBytesReserved() {
        return _bytesReservedByCache();
    }

    /** Drops every globally cached layer image. Called when the library cache configuration
     *  changes (see {@link ComponentExtension#updateAllCachesFromLibraryConfig()}) so memory
     *  shrinks immediately; the cache repopulates lazily under the new budget. <br>
     *  Note that living {@link LayerPartitionCache} instances keep holding their {@code _localCache}
     *  image until their next {@link #validate(ComponentConf)}, so a component
     *  which revalidates afterwards may briefly mint a second image for a key another component
     *  is still painting from. This costs a little duplicated memory until the stragglers
     *  revalidate; it is never a correctness problem (the images are equal by construction),
     *  and it only ever happens on the rare library configuration change. */
    static void clearGlobalCache() {
        _CACHE.clear();
    }


    private final UI.Layer          _layer;
    /** Which piece of {@link #_layer} this instance caches, see {@link LayerRenderConfPartitions}. */
    private final LayerRenderConfPartitions _part;
    private @Nullable CachedImage   _localCache;
    /** The render input: always the actual configuration at the real component size (see class javadoc). */
    private Pooled<LayerRenderConf> _layerRenderData;
    /** The cache key: the interned canonical form of {@code _layerRenderData} (see class javadoc). */
    private Pooled<LayerRenderConf> _cacheKey;
    private int                     _cacheHitsUntilAllocation;
    private boolean                 _isInitialized;


    public LayerPartitionCache(UI.Layer layer, LayerRenderConfPartitions part ) {
        _layer                    = Objects.requireNonNull(layer);
        _part                     = Objects.requireNonNull(part);
        _layerRenderData          = new Pooled<>(LayerRenderConf.none());
        _cacheKey                 = _layerRenderData;
        _cacheHitsUntilAllocation = -1;
        _isInitialized            = false;
    }

    /** The fully rendered cached image which subsequent paint calls will be served from,
     *  or null while there is none (caching not worthwhile, or the lazy allocation
     *  count-down has not finished yet). */
    public @Nullable BufferedImage renderedImage() {
        return _localCache != null && _localCache.isRendered() ? _localCache.getImage() : null;
    }

    public final void validate( ComponentConf newConf )
    {
        if ( newConf.currentBounds().hasWidth(0) || newConf.currentBounds().hasHeight(0) ) {
            /*
                The component is (currently) non-renderable - collapsed or hidden.
                We drop our reference to any previously cached image so it can be
                reclaimed promptly instead of being pinned for as long as this
                component lives, and we reset the initialization state so that a
                later non-zero size revalidates and re-allocates from scratch.
            */
            _localCache               = null;
            _cacheHitsUntilAllocation = -1;
            _isInitialized            = false;
            _layerRenderData          = new Pooled<>(LayerRenderConf.none());
            _cacheKey                 = _layerRenderData;
            return;
        }

        final LayerRenderConf newState = _part.restrict(newConf.renderConfFor(_layer));
        /*
            Canonicalization maps eligible configurations onto the size independent
            exemplar key, so that a resize does not invalidate the cache. For everything
            else it is the identity and this whole method behaves exactly as it did
            when key and render input were one and the same.
        */
        final LayerRenderConf newCacheState = CacheBudget.tilingEnabled()
                                                ? _canonicalize(newState)
                                                : newState;

        final boolean cacheStateChanged = !_cacheKey.get().equals(newCacheState);
        final boolean validationNeeded  = !_isInitialized || cacheStateChanged;

        _isInitialized = true;

        if ( !_layerRenderData.get().equals(newState) )
            _layerRenderData = new Pooled<>(newState);

        if ( validationNeeded ) {
            _cacheHitsUntilAllocation = _cachingMakesSenseFor(newCacheState);
            if ( _localCache != null )
                _localCache.updateNumberOfHitsUntilAllocation(_cacheHitsUntilAllocation);
        }

        if ( _cacheHitsUntilAllocation < 0 ) { // -1 means caching does not make sense
            _cacheHitsUntilAllocation = -1;
            _localCache               = null;
            _isInitialized            = false;
            _cacheKey                 = _layerRenderData;
            return;
        }

        if ( _localCache == null || cacheStateChanged ) {
            // Now we bind the configuration to a new entry:
            _localCache = null;
            Pooled<LayerRenderConf> layerRenderConf = new Pooled<>(newCacheState).intern();
            /*
                We store a pooled ref as the key because this key object is also the key in the global
                (weak) hash map based cache whose reachability determines if the cached image is
                garbage collected or not! So in order to avoid the cache being freed too early, we need to keep a strong
                reference to the key object for all LayerCache instances that make use of the
                corresponding cached image (the value of a particular key in the global cache).
                And so a pooled object has a higher likely hood of being strongly referenced somewhere.
            */
            CachedImage bufferedImage = _CACHE.get(layerRenderConf);

            if ( bufferedImage == null ) {
                Size size = layerRenderConf.get().boxModel().size();
                bufferedImage = new CachedImage(size, _cacheHitsUntilAllocation);
                _CACHE.put(layerRenderConf, bufferedImage);
            }
            _cacheKey = layerRenderConf;

            _localCache = bufferedImage;
        }
    }

    /**
     *  What a single {@link #paint(Graphics2D, BiConsumer)} call did, so that the owning
     *  {@link StyleLayerCache} can tell a paint served purely by blitting from one that had
     *  to run the style renderer. A layer is only as cached as its least cached part, and
     *  that verdict cannot be recovered from per part totals after the fact - hence it is
     *  reported per call rather than counted here.
     */
    enum PaintOutcome {
        /** Nothing was painted at all (component currently has no area, or no meaningful style conf) */
        NOTHING_RENDERED,
        /** The pixels came entirely from the cached image. */
        RENDERED_FROM_CACHE,
        /** The style renderer had to be invoked (caching disabled, or the cache was not yet rendered). */
        RENDERED_FROM_STYLE
    }

    public PaintOutcome paint( Graphics2D g, BiConsumer<LayerRenderConf, Graphics2D> renderer )
    {
        Size size = _layerRenderData.get().boxModel().size();

        if ( size.widthOrElse(0f) == 0f || size.heightOrElse(0f) == 0f )
            return PaintOutcome.NOTHING_RENDERED;

        if ( _cacheHitsUntilAllocation < 0 ) { // -1 means caching does not make sense
            renderer.accept(_layerRenderData.get(), g);
            return PaintOutcome.RENDERED_FROM_STYLE;
        }

        /*
            A cache key size differing from the actual size means the entry is the small
            exemplar rendering, and painting means reconstructing the actual size from it
            through nine tile blits. That is only possible for plain scaling transforms;
            under anything more exotic (rotation, shear, flips) we render directly
            instead of using the cache for this paint.
        */
        final boolean isTiled = !_cacheKey.get().boxModel().size().equals(size);
        if ( isTiled && !_isBlitCompatible(g.getTransform()) ) {
            renderer.accept(_layerRenderData.get(), g);
            return PaintOutcome.RENDERED_FROM_STYLE;
        }

        if ( _localCache == null ) {
            renderer.accept(_layerRenderData.get(), g);
            log.error(
                "Caching enabled for layer '{}', but the local buffer is null; rendered without cache. " +
                "Hit countdown until allocation is '{}'.",
                _layer, _cacheHitsUntilAllocation
            );
            return PaintOutcome.RENDERED_FROM_STYLE;
        }

        final PaintOutcome outcome;
        if ( !_localCache.isRendered() ) {
            Graphics2D g2 = _localCache.createGraphics(g.getDeviceConfiguration());
            if ( g2 == null ) {
                /*
                    The cache is not yet ready to render into!
                    It will need a few more hits to be ready...
                    So we just do normal rendering instead:
                */
                renderer.accept(_layerRenderData.get(), g);
                return PaintOutcome.RENDERED_FROM_STYLE;
            }
            try {
                StyleUtil.transferConfigurations(g, g2);
            }
            catch ( Exception ignored ) {
                log.debug(SwingTree.get().logMarker(), "Error while transferring configurations to the cached image graphics context.");
            }
            finally {
                /*
                    Note the deliberate asymmetry: the shared image is filled by rendering
                    the *cache key* configuration (possibly the small exemplar), whereas
                    the direct-render fallbacks above render the full sized render input.
                */
                renderer.accept(_cacheKey.get(), g2);
                g2.dispose();
            }
            outcome = PaintOutcome.RENDERED_FROM_STYLE;
        } else {
            outcome = PaintOutcome.RENDERED_FROM_CACHE;
        }

        final BufferedImage cachedImage = _localCache.getImage();
        if ( cachedImage == null )
            return outcome; // Cannot happen (the count-down path returned above), but let's be defensive.

        if ( isTiled )
            _localCache.paintStretched(g, _cacheKey.get(), size);
        else
            g.drawImage(cachedImage, 0, 0, null);

        return outcome;
    }

    /**
     *  Scores whether caching pays off for the supplied configuration: -1 means never
     *  cache, otherwise the number of cache hits to wait before allocating and rendering
     *  (0 = eagerly). The supplied state is the <i>cache key</i>, which for stretch
     *  tileable styles is the small exemplar configuration - so the size based gates
     *  (maximum cacheable image area, allocation warm-up) measure the memory that will
     *  actually be allocated, not the component size. This is what makes arbitrarily
     *  large components cacheable (and typically eagerly so) when their style tiles. <br>
     *  <br>
     *  Note the second order effect on the warm-up gate, which exists to stop short-lived
     *  configurations (a style animation mints a fresh one every frame) from paying for an
     *  image: a tileable style always scores small enough to be allocated eagerly, so an
     *  animated tileable style now does allocate an exemplar per frame. This is a deliberate
     *  trade: the exemplar is a few kilobytes and is rendered far more cheaply than the full
     *  sized component the direct fallback would otherwise rasterize, and because the keys
     *  are held weakly the debris is reclaimed as soon as the animation moves on (pinned by
     *  {@code Stretch_Tiling_Eligibility_Spec}). What must not regress is that weak
     *  reclamation - were a frame's key ever strongly retained, a long animation would fill
     *  the cache to {@link #_maxCacheEntries()} and lock every other component out of it.
     */
    private int _cachingMakesSenseFor( LayerRenderConf state )
    {
        final int maxEntries = _maxCacheEntries();
        if ( maxEntries <= 0 || _CACHE.size() >= maxEntries )
            return -1; // Caching disabled or cache already too full, don't admit more entries.

        final Size size = state.boxModel().size();

        if ( !size.hasPositiveWidth() || !size.hasPositiveHeight() )
            return -1; // The component does not have a size that can be displayed.

        if ( state.layer().hasPaintersWhichCannotBeCached() )
            return -1; // We don't know what the painters will do, so we don't cache their painting!

        int heavyStyleCount = 0;

        for ( ImageConf imageConf : state.layer().images().sortedByNames() )
            if ( !imageConf.equals(ImageConf.none()) && imageConf.image().isPresent() ) {
                ImageIcon icon = imageConf.image().get();
                boolean isSpecialIcon = ( icon.getClass() != ImageIcon.class && icon.getClass() != ScalableImageIcon.class );
                boolean hasSize = ( icon.getIconHeight() > 0 || icon.getIconWidth() > 0 );
                if ( isSpecialIcon || hasSize )
                    heavyStyleCount++;
            }
        for ( GradientConf gradient : state.layer().gradients().sortedByNames() )
            if ( !gradient.equals(GradientConf.none()) && gradient.colors().length > 0 )
                heavyStyleCount++;
        for ( Pooled<NoiseConf> noise : state.layer().noises().sortedByNames() )
            if ( !noise.get().equals(NoiseConf.none()) && noise.get().colors().length > 0 )
                heavyStyleCount += 2;
        for ( TextConf text : state.layer().texts().sortedByNames() )
            if ( !text.equals(TextConf.none()) && !text.content().isEmpty() )
                heavyStyleCount++;
        for ( ShadowConf shadow : state.layer().shadows().sortedByNames() )
            if ( !shadow.equals(ShadowConf.none()) && shadow.color().isPresent() )
                heavyStyleCount++;

        final BaseColorConf baseCoors = state.baseColors();
        final BoxModelConf  boxModel  = state.boxModel();
        final boolean       isRounded = boxModel.hasAnyNonZeroArcs();

        if ( _layer == UI.Layer.BORDER ) {
            boolean hasWidth = !Outline.none().equals(boxModel.widths());
            boolean hasColoring = !baseCoors.borderColor().equals(BorderColorsConf.none());
            if ( hasWidth && hasColoring )
                heavyStyleCount++;
        }
        if ( _layer == UI.Layer.BACKGROUND ) {
            boolean roundedOrHasMargin = isRounded || !boxModel.margin().equals(Outline.none());
            if ( roundedOrHasMargin ) {
                if ( baseCoors.backgroundColor().filter( c -> c.getAlpha() > 0 ).isPresent() )
                    heavyStyleCount++;
                if ( baseCoors.foundationColor().filter( c -> c.getAlpha() > 0 ).isPresent() )
                    heavyStyleCount++;
            }
        }

        if ( heavyStyleCount < 1 )
            return -1;

        final int maxSizeLimit         = _maxCacheableImageArea();
        final int eagerAllocationLimit = (int) (maxSizeLimit * EAGER_ALLOCATION_FRIENDLINESS);
        final int cacheHitCountLimit   = (int) (maxSizeLimit * (1 - EAGER_ALLOCATION_FRIENDLINESS));

        final int pixelCount = (int) (size.widthOrElse(0f) * size.heightOrElse(0f));
        final int score      = pixelCount / Math.min(heavyStyleCount, 5); // Heavier styles get cached more easily!

        if ( score > maxSizeLimit )
            return -1; // We are not going to cache such a large image!
        else if ( score <= eagerAllocationLimit )
            return 0; // Nice and small, definitely worth allocating and caching right away!
        else
            return 1 + (score - eagerAllocationLimit) / Math.max(1, cacheHitCountLimit / MAX_CACHE_HIT_COUNT);
            // Here we return the number of cache hits until allocation and rendering should happen.
    }

    /*  ------------------------------------------------------------------------------------
        Stretch tiling geometry - pure functions deriving the size independent cache key
        and the slice cut lines from a render configuration (see class javadoc).
        ------------------------------------------------------------------------------------ */

    /**
     *  Maps eligible configurations of any size onto the size independent exemplar key,
     *  and returns ineligible ones (as well as those not strictly larger than the exemplar
     *  in both dimensions) unchanged - which also makes this idempotent, so a configuration
     *  which already has the exemplar size maps onto itself.
     */
    private static LayerRenderConf _canonicalize( LayerRenderConf conf ) {
        if ( !_isStretchTileable(conf) )
            return conf;

        final Outline sliceInsets = _sliceInsets(conf);
        final Size    canonical   = _canonicalSize(sliceInsets);
        final Size    actual      = conf.boxModel().size();

        final boolean strictlyLarger =
                        actual.widthOrElse(0f)  > canonical.widthOrElse(0f) &&
                        actual.heightOrElse(0f) > canonical.heightOrElse(0f);
        if ( !strictlyLarger )
            return conf;

        return conf.withBoxModel(conf.boxModel().withSize(canonical));
    }

    /** Whether the layer content satisfies the tiling invariant (see class javadoc). */
    private static boolean _isStretchTileable( LayerRenderConf conf ) {
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
     *  How far the size dependent pixels reach into the component from each side:
     *  margin + base outline + border width + the adjacent corner arc extents + the
     *  shadow reach, plus a safety margin, ceiled to whole numbers. Everything between
     *  opposite insets repeats along the respective axis and may be stretched freely. <br>
     *  A pure function of the size independent parts of the configuration, so it can be
     *  recomputed at blit time and always agrees with the canonicalization.
     */
    private static Outline _sliceInsets( LayerRenderConf conf ) {
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

    /**
     *  The exemplar size for the supplied slice insets: {@code 2 * max(insetA, insetB) + band}
     *  per axis. Symmetric, because some rendering internals (corner shadow clip boxes and
     *  border edge polygons) split their work at the component <i>center</i> - the symmetric
     *  size guarantees the exemplar's center line falls into the repeating band, keeping those
     *  artifacts pixel-identical to a real rendering of any larger size.
     */
    private static Size _canonicalSize( Outline sliceInsets ) {
        final float maxHorizontal = Math.max(sliceInsets.left().orElse(0f), sliceInsets.right().orElse(0f));
        final float maxVertical   = Math.max(sliceInsets.top().orElse(0f),  sliceInsets.bottom().orElse(0f));
        return Size.of(
                    2 * maxHorizontal + STRETCH_BAND,
                    2 * maxVertical   + STRETCH_BAND
                );
    }

    /** Tile blits support positive scaling and translation; rotation, shear and flips do not. */
    private static boolean _isBlitCompatible( AffineTransform transform ) {
        return transform.getShearX() == 0 &&
               transform.getShearY() == 0 &&
               transform.getScaleX() >  0 &&
               transform.getScaleY() >  0;
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

    /**
     *  A wrapper for a cached image that is either rendered or not yet allocated and
     *  associated with a particular {@link LayerRenderConf} key, which is used
     *  by the {@link LayerPartitionCache} instance of a particular component to get a strong
     *  reference to the key (causing it to stay in cache and not get garbage collected). <br>
     *  <br>
     *  So instances of this are stored as values in the global {@link #_CACHE},
     *  and can be accessed and shared by multiple {@link LayerPartitionCache} instances.
     *  (So be careful with modifying this class!)<br>
     *  The image can be allocated lazily only after a certain number of cache
     *  hits have been reached. This is to avoid allocating and rendering cache
     *  data for short-lived paint jobs (like animations for example). <br>
     *  <br>
     *  When the image is an exemplar rendering, this class also owns its reconstruction:
     *  {@link #paintStretched} reassembles any actual component size from the image
     *  through nine tile blits.
     */
    private static final class CachedImage
    {
        /** Indices into the {@link #_stretchTiles} array. */
        private static final int TOP = 0, LEFT = 1, CENTER = 2, RIGHT = 3, BOTTOM = 4;

        private final int                      _width;
        private final int                      _height;
        private @Nullable BufferedImage        _image;
        /** The five dedicated stretchable tiles (four edge bands + center), lazily
         *  extracted on the first stretch tiled paint (see {@link #_extractStretchTiles}
         *  for why they cannot be blitted straight out of {@link #_image}). */
        private BufferedImage @Nullable []     _stretchTiles;
        private boolean                        _isRendered;
        private int                            _numberOfHitsUntilAllocation;


        CachedImage( Size size, int numberOfHitsUntilAllocation ) {
            _isRendered                  = false;
            _width                       = Math.max(1, size.width().map(Number::intValue).orElse(1));
            _height                      = Math.max(1, size.height().map(Number::intValue).orElse(1));
            _image                       = null;
            _numberOfHitsUntilAllocation = numberOfHitsUntilAllocation;
        }

        /** The memory this entry has claimed: its image - counted from the moment the entry
         *  exists, not from the moment the buffer is actually allocated.  */
        long reservedBytes() {
            long total = (long) _width * _height * BYTES_PER_PIXEL;
            if ( _stretchTiles != null )
                for ( BufferedImage tile : _stretchTiles )
                    total += _bytesOf(tile);
            return total;
        }

        private static long _bytesOf( @Nullable BufferedImage image ) {
            return ( image == null ? 0 : (long) image.getWidth() * image.getHeight() * BYTES_PER_PIXEL );
        }

        private static BufferedImage _allocate( @Nullable GraphicsConfiguration gc, int width, int height ) {
            BufferedImage img = ( gc != null )
                    ? gc.createCompatibleImage(width, height, Transparency.TRANSLUCENT) // potentially accelerated
                    : new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB); // probably headless
            img.setAccelerationPriority(1.0f);
            return img;
        }

        public void updateNumberOfHitsUntilAllocation( int latestNumberOfHitsUntilAllocation ) {
            if ( _numberOfHitsUntilAllocation < 0 )
                _numberOfHitsUntilAllocation = latestNumberOfHitsUntilAllocation;
        }

        public @Nullable BufferedImage getImage() {
            return _image;
        }

        /**
         *  Creates a {@link Graphics2D} for rendering into the cached image, or returns
         *  null while the hits-until-allocation count-down (which this call decrements)
         *  has not reached zero yet. The image is allocated on the first call after that.
         */
        public @Nullable Graphics2D createGraphics( @Nullable GraphicsConfiguration gc ) {
            if ( _isRendered )
                throw new IllegalStateException("This image has already been rendered into!");
            if ( _numberOfHitsUntilAllocation > 0 ) {
                _numberOfHitsUntilAllocation--;
                return null;
            }
            if ( _image == null )
                _image = _allocate(gc, _width, _height);
            _isRendered = true;
            return _image.createGraphics();
        }

        public boolean isRendered() {
            return _isRendered;
        }

        /**
         *  Reconstructs the rendering of this (exemplar) image at the supplied actual
         *  component size by drawing nine tiles: the four corners 1:1 straight from the
         *  image, the four edge bands stretched along their edge and the center stretched
         *  in both directions - the latter five from their dedicated tile images. <br>
         *  <br>
         *  The tiles are drawn in <b>integer device space</b>: the cut lines are transformed
         *  to device pixels once and shared between adjacent tiles, so that under fractional
         *  HiDPI scales the independent rounding of nine user space rectangles can never
         *  produce one pixel gaps or double blended overlaps. Nearest neighbor interpolation
         *  ensures that stretching a constant source band produces an exactly constant
         *  destination band and that sampling never bleeds across tile boundaries. <br>
         *  <br>
         *  The caller must ensure the graphics transform is blit compatible and that the
         *  actual size is strictly larger than the image in both dimensions (both of which
         *  {@link LayerPartitionCache#paint} guarantees).
         *
         * @param g The destination graphics to draw the tiles into.
         * @param canonicalConf The exemplar configuration this image was rendered from,
         *                      used to recompute the slice insets.
         * @param actualSize The actual component size to reconstruct.
         */
        public void paintStretched( Graphics2D g, LayerRenderConf canonicalConf, Size actualSize )
        {
            final BufferedImage image = _image;
            if ( image == null )
                return; // Cannot happen (callers check `isRendered()` first), but let's be defensive.

            final Outline insets = _sliceInsets(canonicalConf);
            final int insetTop    = (int) _positive(insets.top());
            final int insetRight  = (int) _positive(insets.right());
            final int insetBottom = (int) _positive(insets.bottom());
            final int insetLeft   = (int) _positive(insets.left());

            BufferedImage[] tiles = _stretchTiles;
            if ( tiles == null ) {
                tiles = _extractStretchTiles(g.getDeviceConfiguration(), image, insetTop, insetRight, insetBottom, insetLeft);
                _stretchTiles = tiles;
            }

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
            // The corresponding cut lines in the exemplar source image:
            final int[] sx = { 0, insetLeft, _width  - insetRight,  _width  };
            final int[] sy = { 0, insetTop,  _height - insetBottom, _height };

            final Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setTransform(new AffineTransform()); // We draw in device space.
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                // The four corners, 1:1 sub-rectangle copies from the exemplar image:
                _drawRegion(g2, image, dx[0], dy[0], dx[1], dy[1], sx[0], sy[0], sx[1], sy[1]); // top-left
                _drawRegion(g2, image, dx[2], dy[0], dx[3], dy[1], sx[2], sy[0], sx[3], sy[1]); // top-right
                _drawRegion(g2, image, dx[0], dy[2], dx[1], dy[3], sx[0], sy[2], sx[1], sy[3]); // bottom-left
                _drawRegion(g2, image, dx[2], dy[2], dx[3], dy[3], sx[2], sy[2], sx[3], sy[3]); // bottom-right
                // The stretched bands and center, each a whole dedicated image:
                _drawStretched(g2, tiles[TOP],    dx[1], dy[0], dx[2], dy[1]);
                _drawStretched(g2, tiles[LEFT],   dx[0], dy[1], dx[1], dy[2]);
                _drawStretched(g2, tiles[CENTER], dx[1], dy[1], dx[2], dy[2]);
                _drawStretched(g2, tiles[RIGHT],  dx[2], dy[1], dx[3], dy[2]);
                _drawStretched(g2, tiles[BOTTOM], dx[1], dy[2], dx[2], dy[3]);
            } finally {
                g2.dispose();
            }
        }

        /**
         *  Copies the five stretchable regions (four edge bands + center) into dedicated,
         *  exactly-fitting images. <br>
         *  <br>
         *  <b>Why:</b> the stretched tiles could in principle be drawn straight out of the
         *  exemplar image with sub-rectangle {@code drawImage} calls - and on software
         *  surfaces that is pixel perfect. But on accelerated pipelines (notably XRender on
         *  Linux) a scaled blit whose source is an <i>interior sub-rectangle</i> of a larger
         *  texture breaks down at large stretch ratios: beyond a few hundred times, the blit
         *  samples outside the source band or produces nothing at all, which visually
         *  manifested as long component edges losing their shadows. A scaled blit whose
         *  source is a <i>whole image</i> measures pixel perfect even at extreme ratios -
         *  so each stretched tile gets its own image, while the corner tiles (copied 1:1,
         *  never stretched) keep sourcing the exemplar image directly.
         */
        private static BufferedImage[] _extractStretchTiles(
            final @Nullable GraphicsConfiguration gc,
            final BufferedImage image,
            final int insetTop, final int insetRight, final int insetBottom, final int insetLeft
        ) {
            final int width  = image.getWidth();
            final int height = image.getHeight();
            final BufferedImage[] tiles = new BufferedImage[5];
            tiles[TOP]    = _copyRegion(gc, image, insetLeft,          0,                    width - insetRight, insetTop           );
            tiles[LEFT]   = _copyRegion(gc, image, 0,                  insetTop,             insetLeft,          height - insetBottom);
            tiles[CENTER] = _copyRegion(gc, image, insetLeft,          insetTop,             width - insetRight, height - insetBottom);
            tiles[RIGHT]  = _copyRegion(gc, image, width - insetRight, insetTop,             width,              height - insetBottom);
            tiles[BOTTOM] = _copyRegion(gc, image, insetLeft,          height - insetBottom, width - insetRight, height             );
            return tiles;
        }

        private static BufferedImage _copyRegion(
            final @Nullable GraphicsConfiguration gc,
            final BufferedImage source,
            final int x1, final int y1, final int x2, final int y2
        ) {
            final int width  = Math.max(1, x2 - x1);
            final int height = Math.max(1, y2 - y1);
            final BufferedImage region = _allocate(gc, width, height);
            final Graphics2D g = region.createGraphics();
            try {
                g.setComposite(AlphaComposite.Src); // exact pixel copy, including alpha
                g.drawImage(source, 0, 0, width, height, x1, y1, x2, y2, null);
            } finally {
                g.dispose();
            }
            return region;
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
    }

}
