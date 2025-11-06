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
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

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

    private static final int    MAX_CACHE_ENTRIES                    = 1024; // There can never be more entries!
    private static final int    MAX_CACHE_ENTRIES_PER_AGGRESSIVENESS = 32; // for every dynamic cache aggressiveness unit, we get more entries!
    private static final int    PIXELS_PER_UNIT_OF_AGGRESSIVENESS    = 256 * 256; // Determines how many pixels a single unit of cache aggressiveness can cache
    private static final double EAGER_ALLOCATION_FRIENDLINESS        = 0.1; // Has to be between 0 and 1!
    private static final int    MAX_CACHE_HIT_COUNT                  = 12;

    static int CACHE_AGGRESSIVENESS_OVERRIDE = -1;

    // Higher means more memory usage but better performance
    private static int DYNAMIC_CACHE_AGGRESSIVENESS() {
        if ( CACHE_AGGRESSIVENESS_OVERRIDE >= 0 )
            return CACHE_AGGRESSIVENESS_OVERRIDE;
        double availableGiB = ( ( Runtime.getRuntime().maxMemory() * 1000 ) >> 30 ) / 1e3;
        return (int) Math.round( 4 * Math.log(Math.max(1, availableGiB-1)) );
    }
    private static int DYNAMIC_CACHE_CAP() {
        return Math.min(MAX_CACHE_ENTRIES, MAX_CACHE_ENTRIES_PER_AGGRESSIVENESS * DYNAMIC_CACHE_AGGRESSIVENESS());
    }

    private static final Map<LayerRenderConf, CachedImage> _CACHE = new WeakHashMap<>();


    private final UI.Layer        _layer;
    private @Nullable CachedImage _localCache;
    private LayerRenderConf       _layerRenderData; // The key must be referenced strongly so that the value is not garbage collected (the cached image)
    private int                   _cacheHitsUntilAllocation;
    private boolean               _isInitialized;


    public LayerCache( UI.Layer layer ) {
        _layer                    = Objects.requireNonNull(layer);
        _layerRenderData          = LayerRenderConf.none();
        _cacheHitsUntilAllocation = -1;
        _isInitialized            = false;
    }

    LayerRenderConf getCurrentRenderInputData() {
        return _layerRenderData;
    }

    public boolean hasBufferedImage() {
        return _localCache != null;
    }

    private void _allocateOrGetCachedBuffer( LayerRenderConf layerRenderConf )
    {
        Map<LayerRenderConf, CachedImage> CACHE = _CACHE;

        CachedImage bufferedImage = CACHE.get(layerRenderConf);

        if ( bufferedImage == null ) {
            Size size = layerRenderConf.boxModel().size();
            bufferedImage = new CachedImage(size, layerRenderConf, _cacheHitsUntilAllocation);
            CACHE.put(layerRenderConf, bufferedImage);

            _layerRenderData = layerRenderConf;
        }
        else {
            // We keep a strong reference to the state so that the cached image is not garbage collected
            _layerRenderData = bufferedImage.getKeyOrElse(layerRenderConf);
            /*
                The reason why we take the key stored in the cached image as a strong reference is because this
                key object is also the key in the global (weak) hash map based cache
                whose reachability determines if the cached image is garbage collected or not!
                So in order to avoid the cache being freed too early, we need to keep a strong
                reference to the key object for all LayerCache instances that make use of the
                corresponding cached image (the value of a particular key in the global cache).
            */
        }

        _localCache = bufferedImage;
    }

    private void _freeLocalCache() {
        _localCache               = null;
        _cacheHitsUntilAllocation = -1;
        _isInitialized            = false;
    }

    public final void validate( ComponentConf oldConf, ComponentConf newConf )
    {
        if ( newConf.currentBounds().hasWidth(0) || newConf.currentBounds().hasHeight(0) ) {
            _layerRenderData = LayerRenderConf.none();
            return;
        }

        final LayerRenderConf oldState = oldConf.toRenderConfFor(_layer);
        final LayerRenderConf newState = newConf.toRenderConfFor(_layer);

        boolean validationNeeded = ( !_isInitialized || !oldState.equals(newState) );

        _isInitialized = true;

        if ( validationNeeded ) {
            _cacheHitsUntilAllocation = _cachingMakesSenseFor(newState);
            if ( _localCache != null )
                _localCache.updateNumberOfHitsUntilAllocation(_cacheHitsUntilAllocation);
        }

        if ( _cacheHitsUntilAllocation < 0 ) { // -1 means caching does not make sense
            _freeLocalCache();
            _layerRenderData = newState;
            return;
        }

        boolean cacheIsInvalid = true;
        boolean cacheIsFull    = _CACHE.size() > DYNAMIC_CACHE_CAP();

        boolean newBufferNeeded = false;

        if ( _localCache == null )
            newBufferNeeded = true;
        else
            cacheIsInvalid = !oldState.equals(newState);

        if ( cacheIsInvalid ) {
            _freeLocalCache();
            newBufferNeeded = true;
        }

        if ( cacheIsFull ) {
            _layerRenderData = newState;
            return;
        }

        if ( newBufferNeeded )
            _allocateOrGetCachedBuffer(newState);
    }

    public final void paint( Graphics2D g, BiConsumer<LayerRenderConf, Graphics2D> renderer )
    {
        Size size = _layerRenderData.boxModel().size();

        if ( size.width().orElse(0f) == 0f || size.height().orElse(0f) == 0f )
            return;

        if ( _cacheHitsUntilAllocation < 0 ) { // -1 means caching does not make sense
            renderer.accept(_layerRenderData, g);
            return;
        }

        if ( _localCache == null )
            return;

        if ( !_localCache.isRendered() ) {
            Graphics2D g2 = _localCache.createGraphics();
            if ( g2 == null ) {
                /*
                    The cache is not yet ready to render into!
                    It will need a few more hits to be ready...
                    So we just do normal rendering instead:
                */
                renderer.accept(_layerRenderData, g);
                return;
            }
            try {
                StyleUtil.transferConfigurations(g, g2);
            }
            catch ( Exception ignored ) {
                log.debug(SwingTree.get().logMarker(), "Error while transferring configurations to the cached image graphics context.");
            }
            finally {
                renderer.accept(_layerRenderData, g2);
                g2.dispose();
            }
        }

        g.drawImage(_localCache.getImage(), 0, 0, null);
    }

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
        for ( NoiseConf noise : state.layer().noises().sortedByNames() )
            if ( !noise.equals(NoiseConf.none()) && noise.colors().length > 0 )
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

        final int maxSizeLimit         = DYNAMIC_CACHE_AGGRESSIVENESS() * PIXELS_PER_UNIT_OF_AGGRESSIVENESS;
        final int eagerAllocationLimit = (int) (maxSizeLimit * EAGER_ALLOCATION_FRIENDLINESS);
        final int cacheHitCountLimit   = (int) (maxSizeLimit * (1 - EAGER_ALLOCATION_FRIENDLINESS));

        final int pixelCount = (int) (size.width().orElse(0f) * size.height().orElse(0f));
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
        private final Supplier<BufferedImage>  _imageAllocator;
        private WeakReference<LayerRenderConf> _key;
        private @Nullable BufferedImage        _image;
        private boolean                        _isRendered;
        private int                            _numberOfHitsUntilAllocation;


        CachedImage( Size size, LayerRenderConf cacheKey, int numberOfHitsUntilAllocation ) {
            _key                         = new WeakReference<>(cacheKey);
            _isRendered                  = false;
            _imageAllocator              = () -> new BufferedImage(size.width().map(Number::intValue).orElse(1), size.height().map(Number::intValue).orElse(1), BufferedImage.TYPE_INT_ARGB);
            _image                       = null;
            _numberOfHitsUntilAllocation = numberOfHitsUntilAllocation;
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
        public @Nullable Graphics2D createGraphics() {
            if ( _isRendered )
                throw new IllegalStateException("This image has already been rendered into!");
            if ( _numberOfHitsUntilAllocation > 0 ) {
                _numberOfHitsUntilAllocation--;
                return null;
            }
            if ( _image == null )
                _image = _imageAllocator.get();
            _isRendered = true;
            return _image.createGraphics();
        }

        public LayerRenderConf getKeyOrElse( LayerRenderConf newFallbackKey ) {
            LayerRenderConf key = _key.get();
            if ( key == null ) {
                _key = new WeakReference<>(newFallbackKey);
                key = newFallbackKey;
            }
            return key;
        }

        public boolean isRendered() {
            return _isRendered;
        }
    }

}
