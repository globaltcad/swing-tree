package swingtree.style;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import swingtree.UI;
import swingtree.layout.Size;

import javax.swing.ImageIcon;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
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
 */
final class LayerCache
{
    private static final Map<LayerRenderConf, CachedImage> _CACHE = new WeakHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(LayerCache.class);


    private static final class CachedImage extends BufferedImage
    {
        private WeakReference<LayerRenderConf> _key;
        private boolean _isRendered = false;

        CachedImage( int width, int height, LayerRenderConf cacheKey ) {
            super(width, height, BufferedImage.TYPE_INT_ARGB);
            _key = new WeakReference<>(cacheKey);
        }

        @Override
        public Graphics2D createGraphics() {
            if ( _isRendered )
                throw new IllegalStateException("This image has already been rendered into!");
            _isRendered = true;
            return super.createGraphics();
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


    private final UI.Layer        _layer;
    private @Nullable CachedImage _localCache;
    private LayerRenderConf       _layerRenderData; // The key must be referenced strongly so that the value is not garbage collected (the cached image)
    private boolean               _cachingMakesSense = false;
    private boolean               _isInitialized     = false;


    public LayerCache( UI.Layer layer ) {
        _layer           = Objects.requireNonNull(layer);
        _layerRenderData = LayerRenderConf.none();
    }

    LayerRenderConf getCurrentRenderInputData() {
        return _layerRenderData;
    }

    public boolean hasBufferedImage() {
        return _localCache != null;
    }

    private void _allocateOrGetCachedBuffer( LayerRenderConf layerRenderConf)
    {
        Map<LayerRenderConf, CachedImage> CACHE = _CACHE;

        CachedImage bufferedImage = CACHE.get(layerRenderConf);

        if ( bufferedImage == null ) {
            Size size = layerRenderConf.boxModel().size();
            bufferedImage = new CachedImage(
                                size.width().map(Number::intValue).orElse(1),
                                size.height().map(Number::intValue).orElse(1),
                                layerRenderConf
                            );
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
        _localCache        = null;
        _cachingMakesSense = false;
        _isInitialized     = false;
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

        if ( validationNeeded )
            _cachingMakesSense = _cachingMakesSenseFor(newState);

        if ( !_cachingMakesSense ) {
            _freeLocalCache();
            _layerRenderData = newState;
            return;
        }

        boolean cacheIsInvalid = true;
        boolean cacheIsFull    = _CACHE.size() > 128;

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

        if ( !_cachingMakesSense ) {
            renderer.accept(_layerRenderData, g);
            return;
        }

        if ( _localCache == null )
            return;

        if ( !_localCache.isRendered() ) {
            Graphics2D g2 = _localCache.createGraphics();
            try {
                StyleUtil.transferConfigurations(g, g2);
            }
            catch ( Exception ignored ) {
                log.debug("Error while transferring configurations to the cached image graphics context.");
            }
            finally {
                renderer.accept(_layerRenderData, g2);
                g2.dispose();
            }
        }

        g.drawImage(_localCache, 0, 0, null);
    }

    public boolean _cachingMakesSenseFor( LayerRenderConf state )
    {
        final Size size = state.boxModel().size();

        if ( !size.hasPositiveWidth() || !size.hasPositiveHeight() )
            return false;

        if ( state.layer().hasPaintersWhichCannotBeCached() )
            return false; // We don't know what the painters will do, so we don't cache their painting!

        int heavyStyleCount = 0;

        for ( ImageConf imageConf : state.layer().images().sortedByNames() )
            if ( !imageConf.equals(ImageConf.none()) && imageConf.image().isPresent() ) {
                ImageIcon icon = imageConf.image().get();
                boolean isSpecialIcon = ( icon.getClass() != ImageIcon.class );
                boolean hasSize = ( icon.getIconHeight() > 0 || icon.getIconWidth() > 0 );
                if ( isSpecialIcon || hasSize )
                    heavyStyleCount++;
            }
        for ( GradientConf gradient : state.layer().gradients().sortedByNames() )
            if ( !gradient.equals(GradientConf.none()) && gradient.colors().length > 0 )
                heavyStyleCount++;
        for ( NoiseConf noise : state.layer().noises().sortedByNames() )
            if ( !noise.equals(NoiseConf.none()) && noise.colors().length > 0 )
                heavyStyleCount++;
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
            if ( hasWidth && baseCoors.borderColor().isPresent() )
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
            return false;

        final int threshold = 256 * 256 * Math.min(heavyStyleCount, 5);
        final int pixelCount = (int) (size.width().orElse(0f) * size.height().orElse(0f));

        return pixelCount <= threshold;
    }

}
