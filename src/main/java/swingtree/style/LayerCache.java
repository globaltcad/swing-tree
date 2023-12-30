package swingtree.style;

import swingtree.UI;
import swingtree.layout.Bounds;
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
 *  So if the {@link ComponentConf} of a component changes, the cache is invalidated and the layer
 *  is rendered again. <br>
 *  This is made possible by the fact that the {@link ComponentConf} is deeply immutable and can be used
 *  as a key data structure for caching.
 */
final class LayerCache
{
    private static final Map<ComponentConf, CachedImage> _CACHE = new WeakHashMap<>();


    private static final class CachedImage extends BufferedImage
    {
        private WeakReference<ComponentConf> _key;
        private boolean _isRendered = false;

        CachedImage( int width, int height, ComponentConf cacheKey ) {
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

        public ComponentConf getKeyOrElse( ComponentConf newFallbackKey ) {
            ComponentConf key = _key.get();
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


    private final UI.Layer   _layer;
    private CachedImage      _localCache;
    private ComponentConf    _strongRef; // The key must be referenced strongly so that the value is not garbage collected (the cached image)
    private boolean          _cachingMakesSense = true;


    public LayerCache( UI.Layer layer ) {
        _layer = Objects.requireNonNull(layer);
    }

    public boolean hasBufferedImage() {
        return _localCache != null;
    }

    private void _allocateOrGetCachedBuffer( ComponentConf styleConf )
    {
        Map<ComponentConf, CachedImage> CACHE = _CACHE;

        CachedImage bufferedImage = CACHE.get(styleConf);

        if ( bufferedImage == null ) {
            Size size = styleConf.currentBounds().size();
            bufferedImage = new CachedImage(size.width().orElse(1), size.height().orElse(1), styleConf);
            CACHE.put(styleConf, bufferedImage);
            _strongRef = styleConf;
        }
        else {
            // We keep a strong reference to the state so that the cached image is not garbage collected
            _strongRef = bufferedImage.getKeyOrElse(styleConf);
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
        _strongRef         = null;
        _localCache        = null;
        _cachingMakesSense = false;
    }

    public final void validate( ComponentConf oldState, ComponentConf newState )
    {
        if ( newState.currentBounds().hasWidth(0) || newState.currentBounds().hasHeight(0) )
            return;

        oldState = oldState.onlyRetainingLayer(_layer);
        newState = newState.onlyRetainingLayer(_layer);

        _cachingMakesSense = _cachingMakesSenseFor(newState);

        if ( !_cachingMakesSense ) {
            _freeLocalCache();
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

        if ( cacheIsFull )
            return;

        if ( newBufferNeeded )
            _allocateOrGetCachedBuffer(newState);
    }

    public final void paint( ComponentConf conf, Graphics2D g, BiConsumer<ComponentConf, Graphics2D> renderer )
    {
        Bounds componentBounds = conf.currentBounds();

        if ( componentBounds.hasWidth(0) || componentBounds.hasHeight(0) )
            return;

        if ( !_cachingMakesSense ) {
            renderer.accept(_strongRef == null ? conf : _strongRef, g);
            return;
        }

        if ( _localCache == null )
            return;

        if ( !_localCache.isRendered() ) {
            Graphics2D g2 = _localCache.createGraphics();
            try {
                g2.setBackground(g.getBackground());
                g2.setClip(null); // We want to capture the full style and clip it later (see g.drawImage(_cache, 0, 0, null); below.
                g2.setComposite(g.getComposite());
                g2.setPaint(g.getPaint());
                g2.setRenderingHints(g.getRenderingHints());
                g2.setStroke(g.getStroke());
            }
            catch (Exception ignored) {}
            finally {
                renderer.accept(_strongRef == null ? conf : _strongRef, g2);
                g2.dispose();
            }
        }

        g.drawImage(_localCache, 0, 0, null);
    }

    public boolean _cachingMakesSenseFor( ComponentConf state )
    {
        Bounds bounds = state.currentBounds();
        if ( !bounds.hasWidth() || !bounds.hasHeight() )
            return false;

        if ( state.style().hasCustomPaintersOnLayer(_layer) )
            return false; // We don't know what the painters will do, so we don't cache their painting!

        int heavyStyleCount = 0;
        for ( ImageStyle imageStyle : state.style().images(_layer) )
            if ( !imageStyle.equals(ImageStyle.none()) && imageStyle.image().isPresent() ) {
                ImageIcon icon = imageStyle.image().get();
                boolean isSpecialIcon = ( icon.getClass() != ImageIcon.class );
                boolean hasSize = ( icon.getIconHeight() > 0 || icon.getIconWidth() > 0 );
                if ( isSpecialIcon || hasSize )
                    heavyStyleCount++;
            }
        for ( GradientStyle gradient : state.style().gradients(_layer) )
            if ( !gradient.equals(GradientStyle.none()) && gradient.colors().length > 0 )
                heavyStyleCount++;
        for ( ShadowStyle shadow : state.style().shadows(_layer) )
            if ( !shadow.equals(ShadowStyle.none()) && shadow.color().isPresent() )
                heavyStyleCount++;

        BorderStyle border = state.style().border();
        boolean rounded = border.hasAnyNonZeroArcs();

        if ( _layer == UI.Layer.BORDER ) {
            boolean hasWidth = !Outline.none().equals(border.widths());
            if ( hasWidth && border.color().isPresent() )
                heavyStyleCount++;
        }
        if ( _layer == UI.Layer.BACKGROUND ) {
            BaseStyle base = state.style().base();
            boolean roundedOrHasMargin = rounded || !state.style().margin().equals(Outline.none());
            if ( base.backgroundColor().isPresent() && roundedOrHasMargin )
                heavyStyleCount++;
        }

        if ( heavyStyleCount < 1 )
            return false;

        int threshold = 256 * 256 * Math.min(heavyStyleCount, 5);
        int pixelCount = bounds.area();

        return pixelCount <= threshold;
    }

}
