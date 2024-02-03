package swingtree.style;

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
 *  So if the {@link RenderConf} of a component changes, the cache is invalidated and the layer
 *  is rendered again. <br>
 *  This is made possible by the fact that the {@link RenderConf} is deeply immutable and can be used
 *  as a key data structure for caching.
 */
final class LayerCache
{
    private static final Map<RenderConf, CachedImage> _CACHE = new WeakHashMap<>();


    private static final class CachedImage extends BufferedImage
    {
        private WeakReference<RenderConf> _key;
        private boolean _isRendered = false;

        CachedImage( int width, int height, RenderConf cacheKey ) {
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

        public RenderConf getKeyOrElse(RenderConf newFallbackKey ) {
            RenderConf key = _key.get();
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
    private RenderConf       _strongRef; // The key must be referenced strongly so that the value is not garbage collected (the cached image)
    private boolean          _cachingMakesSense = false;
    private boolean          _isInitialized     = false;


    public LayerCache( UI.Layer layer ) {
        _layer     = Objects.requireNonNull(layer);
        _strongRef = RenderConf.none();
    }

    RenderConf getCurrentKey() {
        return _strongRef;
    }

    public boolean hasBufferedImage() {
        return _localCache != null;
    }

    private void _allocateOrGetCachedBuffer( RenderConf renderConf)
    {
        Map<RenderConf, CachedImage> CACHE = _CACHE;

        CachedImage bufferedImage = CACHE.get(renderConf);

        if ( bufferedImage == null ) {
            Size size = renderConf.boxModel().size();
            bufferedImage = new CachedImage(
                                size.width().map(Number::intValue).orElse(1),
                                size.height().map(Number::intValue).orElse(1),
                                renderConf
                            );
            CACHE.put(renderConf, bufferedImage);
            _strongRef = renderConf;
        }
        else {
            // We keep a strong reference to the state so that the cached image is not garbage collected
            _strongRef = bufferedImage.getKeyOrElse(renderConf);
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
            _strongRef = RenderConf.none();
            return;
        }

        final RenderConf oldState = oldConf.toRenderConfFor(_layer);
        final RenderConf newState = newConf.toRenderConfFor(_layer);

        boolean validationNeeded = ( !_isInitialized || !oldState.equals(newState) );

        _isInitialized = true;

        if ( validationNeeded )
            _cachingMakesSense = _cachingMakesSenseFor(newState);

        if ( !_cachingMakesSense ) {
            _freeLocalCache();
            _strongRef = newState;
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
            _strongRef = newState;
            return;
        }

        if ( newBufferNeeded )
            _allocateOrGetCachedBuffer(newState);
        else
            _strongRef = newState;
    }

    public final void paint( Graphics2D g, BiConsumer<RenderConf, Graphics2D> renderer )
    {
        Size size = _strongRef.boxModel().size();

        if ( size.width().orElse(0f) == 0f || size.height().orElse(0f) == 0f )
            return;

        if ( !_cachingMakesSense ) {
            renderer.accept(_strongRef, g);
            return;
        }

        if ( _localCache == null )
            return;

        if ( !_localCache.isRendered() ) {
            Graphics2D g2 = _localCache.createGraphics();
            try {
                g2.setFont(g.getFont());
                g2.setColor(g.getColor());
                g2.setBackground(g.getBackground());
                g2.setComposite(g.getComposite());
                g2.setClip(null); // We want to capture the full style and clip it later (see g.drawImage(_cache, 0, 0, null); below.
                g2.setComposite(g.getComposite());
                g2.setPaint(g.getPaint());
                g2.setRenderingHints(g.getRenderingHints());
                g2.setStroke(g.getStroke());
            }
            catch (Exception ignored) {}
            finally {
                renderer.accept(_strongRef, g2);
                g2.dispose();
            }
        }

        g.drawImage(_localCache, 0, 0, null);
    }

    public boolean _cachingMakesSenseFor( RenderConf state )
    {
        Size size = state.boxModel().size();
        if ( !size.hasPositiveWidth() || !size.hasPositiveHeight() )
            return false;

        if ( state.layer().hasPainters() )
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
        for ( ShadowConf shadow : state.layer().shadows().sortedByNames() )
            if ( !shadow.equals(ShadowConf.none()) && shadow.color().isPresent() )
                heavyStyleCount++;

        BoxModelConf border = state.boxModel();
        BaseColorConf colorConf = state.baseColors();
        boolean rounded = border.hasAnyNonZeroArcs();

        if ( _layer == UI.Layer.BORDER ) {
            boolean hasWidth = !Outline.none().equals(border.widths());
            if ( hasWidth && colorConf.borderColor().isPresent() )
                heavyStyleCount++;
        }
        if ( _layer == UI.Layer.BACKGROUND ) {
            BaseColorConf base = state.baseColors();
            boolean roundedOrHasMargin = rounded || !state.boxModel().margin().equals(Outline.none());
            if ( base.backgroundColor().isPresent() && roundedOrHasMargin )
                heavyStyleCount++;
        }

        if ( heavyStyleCount < 1 )
            return false;

        int threshold = 256 * 256 * Math.min(heavyStyleCount, 5);
        int pixelCount = (int) (size.width().orElse(0f) * size.height().orElse(0f));

        return pixelCount <= threshold;
    }

}
