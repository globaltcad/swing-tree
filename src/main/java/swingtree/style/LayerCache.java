package swingtree.style;

import swingtree.UI;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 *  A {@link BufferedImage} based cache for the rendering of a particular layer of a component's style. <br>
 *  So if the {@link StyleRenderState} of a component changes, the cache is invalidated and the layer
 *  is rendered again. <br>
 *  This is made possible by the fact that the {@link StyleRenderState} is deeply immutable and can be used
 *  as a key data structure for caching.
 */
final class LayerCache
{
    private static final Map<StyleRenderState, BufferedImage> _BACKGROUND_CACHE = new WeakHashMap<>();
    private static final Map<StyleRenderState, BufferedImage> _BORDER_CACHE = new WeakHashMap<>();
    private static final Map<StyleRenderState, BufferedImage> _CONTENT_AND_FOREGROUND_CACHE = new WeakHashMap<>();


    private final UI.Layer   _layer;
    private BufferedImage _localCache;
    private StyleRenderState _strongRef; // The key must be referenced strongly so that the value is not garbage collected (the cached image)
    private boolean          _renderIntoCache = true;
    private boolean          _cachingMakesSense = true;
    private boolean          _cacheBufferIsShared = false;


    public LayerCache(UI.Layer layer ) {
        _layer = Objects.requireNonNull(layer);
    }

    private Map<StyleRenderState, BufferedImage> _getGlobalCacheForThisLayer() {
        switch (_layer) {
            case BACKGROUND: return _BACKGROUND_CACHE;
            case BORDER: return _BORDER_CACHE;
            case CONTENT:
            case FOREGROUND:
                return _CONTENT_AND_FOREGROUND_CACHE;
                // We allow them to share a buffer because they do not have unique styles
        }
        return _CONTENT_AND_FOREGROUND_CACHE;
    }

    private boolean allocateOrGetCachedBuffer( StyleRenderState styleConf )
    {
        boolean foundSomethingInGlobalCache = false;

        Map<StyleRenderState, BufferedImage> _CACHE = _getGlobalCacheForThisLayer();

        BufferedImage bufferedImage = _CACHE.get(styleConf);

        if ( bufferedImage == null ) {
            Bounds bounds = styleConf.currentBounds();
            bufferedImage = new BufferedImage(bounds.width(), bounds.height(), BufferedImage.TYPE_INT_ARGB);
        }
        else
            foundSomethingInGlobalCache = true;

        _localCache = bufferedImage;
        _strongRef  = styleConf; // We keep a strong reference to the state so that the cached image is not garbage collected
        _cacheBufferIsShared = foundSomethingInGlobalCache;
        _CACHE.put(styleConf, bufferedImage);
        /*
            Note that we refresh the key in the map using the above put() call.
            This is necessary because the most recent state is always strongly referenced
            whereas the old state may no longer be referenced by anything else.
        */

        return foundSomethingInGlobalCache;
    }

    private void _freeLocalCache() {
        _strongRef       = null;
        _localCache      = null;
        _renderIntoCache = false;
    }

    public final void validate( StyleRenderState oldState, StyleRenderState newState )
    {
        if ( newState.currentBounds().width() == 0 || newState.currentBounds().height() == 0 )
            return;

        oldState = oldState.retainingOnlyLayer(_layer);
        newState = newState.retainingOnlyLayer(_layer);

        _cachingMakesSense = _cachingMakesSenseFor(newState);
        if ( !_cachingMakesSense ) {
            _freeLocalCache();
            return;
        }

        boolean cacheIsInvalid = !oldState.equals(newState);
        boolean cacheIsFull    = _getGlobalCacheForThisLayer().size() > 128;

        boolean newBufferNeeded = false;

        if ( _localCache == null )
            newBufferNeeded = true;
        else
        {
            Bounds bounds = newState.currentBounds();
            boolean sizeChanged = !bounds.hasSize( _localCache.getWidth(), _localCache.getHeight() );
            if ( sizeChanged || cacheIsInvalid && _cacheBufferIsShared ) {
                _freeLocalCache();
                newBufferNeeded = true;
            }
        }

        boolean justAllocatedANewBuffer = false;
        boolean foundSomethingInGlobalCache = false;

        if ( newBufferNeeded ) {
            if ( cacheIsFull )
                return;

            foundSomethingInGlobalCache = allocateOrGetCachedBuffer(newState);

            justAllocatedANewBuffer = !foundSomethingInGlobalCache;
        }

        if ( justAllocatedANewBuffer )
            _renderIntoCache = true;

        if ( cacheIsInvalid && !justAllocatedANewBuffer )
            _renderIntoCache = true;
    }

    public final void paint( StyleEngine engine, Graphics2D g, Consumer<Graphics2D> renderer )
    {
        if ( engine.getState().currentBounds().width() == 0 || engine.getState().currentBounds().height() == 0 )
            return;

        if ( !_cachingMakesSense ) {
            renderer.accept(g);
            return;
        }

        if ( _localCache == null )
            return;

        if ( _renderIntoCache ) {
            Graphics2D g2 = _localCache.createGraphics();
            g2.setBackground(g.getBackground());
            g2.setClip(null); // We want to capture the full style and clip it later (see g.drawImage(_cache, 0, 0, null); below.
            g2.setComposite(g.getComposite());
            g2.setPaint(g.getPaint());
            g2.setRenderingHints(g.getRenderingHints());
            g2.setStroke(g.getStroke());
            renderer.accept(g2);
            _renderIntoCache = false;
        }

        g.drawImage(_localCache, 0, 0, null);
    }

    public boolean _cachingMakesSenseFor( StyleRenderState state )
    {
        Bounds bounds = state.currentBounds();
        if ( bounds.width() <= 0 || bounds.height() <= 0 )
            return false;

        if ( state.style().hasCustomPaintersOnLayer(_layer) )
            return false; // We don't know what the painters will do, so we don't cache their painting!

        int heavyStyleCount = 0;
        for ( ImageStyle imageStyle : state.style().images(_layer) )
            if ( !imageStyle.equals(ImageStyle.none()) && imageStyle.image().isPresent() )
                heavyStyleCount++;
        for ( GradientStyle gradient : state.style().gradients(_layer) )
            if ( !gradient.equals(GradientStyle.none()) && gradient.colors().length > 0 )
                heavyStyleCount++;
        for ( ShadowStyle shadow : state.style().shadows(_layer) )
            if ( !shadow.equals(ShadowStyle.none()) )
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
        int pixelCount = bounds.width() * bounds.height();

        return pixelCount <= threshold;
    }

}
