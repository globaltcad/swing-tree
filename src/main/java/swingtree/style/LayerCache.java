package swingtree.style;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import swingtree.SwingTree;
import swingtree.UI;
import swingtree.layout.Size;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;

/**
 *  A {@link BufferedImage} based cache for the rendering of a particular layer of a component's style. <br>
 *  So if the {@link LayerRenderConf} of a component changes, the cache is invalidated and the layer
 *  is rendered again. <br>
 *  This is made possible by the fact that the {@link LayerRenderConf} is deeply immutable and can be used
 *  as a key data structure for caching.
 *  <br>
 *  Instances of this exist for every component (inside their style engine) and are used to
 *  safely do cache based rendering of the component's style.
 */
final class LayerCache
{
    private static final Logger log = LoggerFactory.getLogger(LayerCache.class);

    private static final int    MAX_CACHE_ENTRIES                 = 1024; // There can never be more entries!
    private static final int    PIXELS_PER_UNIT_OF_AGGRESSIVENESS = 256 * 256; // Determines how many pixels a single unit of cache aggressiveness can cache
    private static final double EAGER_ALLOCATION_FRIENDLINESS     = 0.1; // Has to be between 0 and 1!
    private static final int    MAX_CACHE_HIT_COUNT               = 12;

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

    /** Live number of cached style-layer renderings (for monitoring/tests). */
    static int globalEntryCount() {
        return _CACHE.size();
    }

    /** Drops every globally cached layer image. Called when the library cache configuration
     *  changes (see {@link ComponentExtension#updateAllCachesFromLibraryConfig()}) so memory
     *  shrinks immediately; the cache repopulates lazily under the new budget. */
    static void clearGlobalCache() {
        _CACHE.clear();
    }


    private final UI.Layer          _layer;
    private @Nullable CachedImage   _localCache;
    /**
     *  The render input of this component's layer: always the <b>actual</b>
     *  {@link LayerRenderConf} (with the real component size), used for the
     *  direct-render fallbacks in {@link #paint(Graphics2D, BiConsumer)} and
     *  for the destination geometry of the final cache blit.
     */
    private Pooled<LayerRenderConf> _layerRenderData;
    /**
     *  The key of this cache's entry in the global {@link #_CACHE}: the interned
     *  <b>canonical</b> form of {@code _layerRenderData}. This strong reference is
     *  what keeps the weakly keyed cache entry (and thereby the shared image) alive.
     *  The renderer is invoked with this configuration when filling the cached image.
     */
    private Pooled<LayerRenderConf> _cacheKey;
    private int                     _cacheHitsUntilAllocation;
    private boolean                 _isInitialized;
    /*
     *  Per-instance utilisation counters. Each {@link LayerCache} instance lives
     *  inside the style engine of exactly one {@link JComponent}, so these
     *  counters describe the cache behaviour of *that* component's *that* layer.
     *  They are observable via the {@link ComponentExtension} public API and used
     *  by the test suite to reason about cache effectiveness without coupling to
     *  any of the package-private machinery.
     */
    private int                     _paintCacheHitCount  = 0; // paint() served entirely from a rendered cache image
    private int                     _paintCacheMissCount = 0; // paint() had to invoke the renderer (caching disabled, or the cache was not yet rendered)


    public LayerCache( UI.Layer layer ) {
        _layer                    = Objects.requireNonNull(layer);
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

    private void _allocateOrGetCachedBuffer( Pooled<LayerRenderConf> layerRenderConf )
    {
        Map<Pooled<LayerRenderConf>, CachedImage> CACHE = _CACHE;
        /*
            We store a pooled ref as the key because this
            key object is also the key in the global (weak) hash map based cache
            whose reachability determines if the cached image is garbage collected or not!
            So in order to avoid the cache being freed too early, we need to keep a strong
            reference to the key object for all LayerCache instances that make use of the
            corresponding cached image (the value of a particular key in the global cache).
            And so a pooled object has a higher likely hood of being strongly referenced somewhere.
        */
        layerRenderConf = layerRenderConf.intern();
        CachedImage bufferedImage = CACHE.get(layerRenderConf);

        if ( bufferedImage == null ) {
            Size size = layerRenderConf.get().boxModel().size();
            bufferedImage = new CachedImage(size, _cacheHitsUntilAllocation);
            CACHE.put(layerRenderConf, bufferedImage);
        }
        _cacheKey = layerRenderConf;

        _localCache = bufferedImage;
    }

    public final void validate( ComponentConf oldConf, ComponentConf newConf )
    {
        if ( newConf.currentBounds().hasWidth(0) || newConf.currentBounds().hasHeight(0) ) {
            _layerRenderData = new Pooled<>(LayerRenderConf.none());
            _cacheKey        = _layerRenderData;
            return;
        }

        final LayerRenderConf newState = newConf.renderConfFor(_layer);
        /*
            The render input (what the renderer sees, at the real component size)
            and the cache key (what the global cache is looked up with) are two
            different things: eligible configurations are canonicalized onto a
            size independent key so that a resize does not invalidate the cache.
            For everything else canonicalization is the identity and this whole
            method behaves exactly as it did when key and input were one.
        */
        final LayerRenderConf newCacheState = StretchTiling.canonicalize(newState);

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
            // We only drop the local cached image here – we deliberately do not
            // reset `_cacheHitsUntilAllocation` and `_isInitialized` (as
            // `_freeLocalCache()` does), because we just computed the correct
            // `_cacheHitsUntilAllocation` for the new state above and we want
            // it to be used by the upcoming `_allocateOrGetCachedBuffer` call.
            _localCache = null;
            _allocateOrGetCachedBuffer(new Pooled<>(newCacheState));
        }
    }

    public void paint( Graphics2D g, BiConsumer<LayerRenderConf, Graphics2D> renderer )
    {
        Size size = _layerRenderData.get().boxModel().size();

        if ( size.widthOrElse(0f) == 0f || size.heightOrElse(0f) == 0f )
            return;

        if ( _cacheHitsUntilAllocation < 0 ) { // -1 means caching does not make sense
            renderer.accept(_layerRenderData.get(), g);
            _paintCacheMissCount++;
            return;
        }

        /*
            When the cache key is the size independent canonical form (its size differs
            from the actual render input size), painting means reconstructing the actual
            size from the canonical image through nine tile blits. That is only possible
            for plain scaling transforms; under anything more exotic (rotation, shear,
            flips) we render directly instead of using the cache for this paint.
        */
        final boolean isTiled = !_cacheKey.get().boxModel().size().equals(size);
        if ( isTiled && !StretchTiling.isBlitCompatible(g.getTransform()) ) {
            renderer.accept(_layerRenderData.get(), g);
            _paintCacheMissCount++;
            return;
        }

        if ( _localCache == null ) {
            renderer.accept(_layerRenderData.get(), g);
            _paintCacheMissCount++;
            log.error(
                "Caching enabled for layer '{}', but the local buffer is null; rendered without cache. " +
                "Hit countdown until allocation is '{}'.",
                _layer, _cacheHitsUntilAllocation
            );
            return;
        }

        if ( !_localCache.isRendered() ) {
            Graphics2D g2 = _localCache.createGraphics(g.getDeviceConfiguration());
            if ( g2 == null ) {
                /*
                    The cache is not yet ready to render into!
                    It will need a few more hits to be ready...
                    So we just do normal rendering instead:
                */
                renderer.accept(_layerRenderData.get(), g);
                _paintCacheMissCount++;
                return;
            }
            try {
                StyleUtil.transferConfigurations(g, g2);
            }
            catch ( Exception ignored ) {
                log.debug(SwingTree.get().logMarker(), "Error while transferring configurations to the cached image graphics context.");
            }
            finally {
                /*
                    Note the deliberate asymmetry: the image shared through the global
                    cache is filled by rendering the *cache key* configuration (which
                    may be the size independent canonical form), whereas all the
                    direct-render fallbacks above render `_layerRenderData` (the
                    actual configuration at the real component size).
                */
                renderer.accept(_cacheKey.get(), g2);
                g2.dispose();
            }
            _paintCacheMissCount++;
        } else {
            _paintCacheHitCount++;
        }

        final BufferedImage cachedImage = _localCache.getImage();
        if ( cachedImage == null )
            return; // Cannot happen (the count-down path returned above), but let's be defensive.

        if ( isTiled )
            StretchTiling.blit(g, _cacheKey.get(), cachedImage, size);
        else
            g.drawImage(cachedImage, 0, 0, null);
    }

    /**
     *  Returns the number of {@link #paint(Graphics2D, BiConsumer)} calls served entirely
     *  from the cached image (i.e. the renderer was <i>not</i> invoked). Counts since this
     *  {@link LayerCache} instance was created.
     */
    int paintCacheHitCount()  { return _paintCacheHitCount;  }

    /**
     *  Returns the number of {@link #paint(Graphics2D, BiConsumer)} calls that had to
     *  invoke the renderer because the cache was either disabled or not yet rendered.
     *  Counts since this {@link LayerCache} instance was created.
     */
    int paintCacheMissCount() { return _paintCacheMissCount; }

    /**
     *  Determines if caching makes sense for the given rendering configuration of the layer
     *  represented as a number indicating the number of cache hits until allocation and rendering
     *  should happen, or -1 if caching does not make sense for the given rendering configuration.
     *
     * @param state The rendering configuration of the layer.
     * @return A number indicating the number of cache hits until allocation and rendering should happen,
     *         or -1 if caching does not make sense for the given rendering configuration.
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

    /**
     *  A wrapper for a cached image that is either rendered or not yet allocated and
     *  associated with a particular {@link LayerRenderConf} key, which is used
     *  by the {@link LayerCache} instance of a particular component to get a strong
     *  reference to the key (causing it to stay in cache and not get garbage collected). <br>
     *  <br>
     *  So instances of this are stored as values in the global {@link #_CACHE},
     *  and can be accessed and shared by multiple {@link LayerCache} instances.
     *  (So be careful with modifying this class!)<br>
     *  The image can be allocated lazily only after a certain number of cache
     *  hits have been reached. This is to avoid allocating and rendering cache
     *  data for short-lived paint jobs (like animations for example).
     */
    private static final class CachedImage
    {
        private final int                      _width;
        private final int                      _height;
        private @Nullable BufferedImage        _image;
        private boolean                        _isRendered;
        private int                            _numberOfHitsUntilAllocation;


        CachedImage( Size size, int numberOfHitsUntilAllocation ) {
            _isRendered                  = false;
            _width                       = Math.max(1, size.width().map(Number::intValue).orElse(1));
            _height                      = Math.max(1, size.height().map(Number::intValue).orElse(1));
            _image                       = null;
            _numberOfHitsUntilAllocation = numberOfHitsUntilAllocation;
        }

        /**
         *  Allocates the backing buffer. We derive it from the supplied
         *  {@link GraphicsConfiguration} when one is available, so it uses the device
         *  color model (typically premultiplied {@code INT_ARGB_PRE}, faster to
         *  composite) and so Java2D can keep an accelerated copy of it in video memory:
         *  this image is rendered once and then blitted on every repaint and never read
         *  back, which is exactly the managed-image pattern that gets texture-cached.
         *  The maximum acceleration priority keeps that copy resident under memory
         *  pressure. Falls back to a plain {@code INT_ARGB} buffer when there is no
         *  device configuration (e.g. headless rendering).
         */
        private BufferedImage _allocate( @Nullable GraphicsConfiguration gc ) {
            BufferedImage img = ( gc != null )
                    ? gc.createCompatibleImage(_width, _height, Transparency.TRANSLUCENT)
                    : new BufferedImage(_width, _height, BufferedImage.TYPE_INT_ARGB);
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
         *  Creates a {@link Graphics2D} object for rendering into the cached image if
         *  the number of cache hits until allocation count-down has reached zero.
         *  Calling this method will decrement the count-down once if it is greater than zero,
         *  or allocate the image if it is not greater than zero and the image is not yet allocated.
         *  A graphics object is only returned if the count-down has reached zero,
         *  otherwise null is returned.
         *
         * @return a {@link Graphics2D} object for rendering into the cached image or null if
         *         the image is not yet allocated. Continuous calls to this method will eventually
         *         allocate the image.
         */
        public @Nullable Graphics2D createGraphics( @Nullable GraphicsConfiguration gc ) {
            if ( _isRendered )
                throw new IllegalStateException("This image has already been rendered into!");
            if ( _numberOfHitsUntilAllocation > 0 ) {
                _numberOfHitsUntilAllocation--;
                return null;
            }
            if ( _image == null )
                _image = _allocate(gc);
            _isRendered = true;
            return _image.createGraphics();
        }

        public boolean isRendered() {
            return _isRendered;
        }
    }

}
