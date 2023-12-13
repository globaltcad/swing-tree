package swingtree.style;

import swingtree.UI;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.WeakHashMap;

abstract class CachedPainter
{
    private static Map<StyleRenderState, BufferedImage[]> _globalCache = new WeakHashMap<>();

    protected final UI.Layer _layer;
    private BufferedImage _value;
    private boolean _renderIntoCache = true;
    private boolean _cachingMakesSense = true;


    public CachedPainter(UI.Layer layer) {
        _layer = layer;
    }

    private boolean allocateOrGetCachedBuffer( StyleRenderState state )
    {
        BufferedImage[] buffers = _globalCache.get(state);
        if ( buffers == null )
            buffers = new BufferedImage[UI.Layer.values().length];

        _globalCache.put(state, buffers);
        /*
            Note that we refresh the key in the map using the above put() call.
            This is necessary because the most recent state is always strongly referenced
            whereas the old state may no longer be referenced by anything else.
        */

        boolean foundSomethingInGlobalCache = false;

        BufferedImage buffer = buffers[_layer.ordinal()];
        if ( buffer == null ) {
            Bounds bounds = state.currentBounds();
            buffer = new BufferedImage(bounds.width(), bounds.height(), BufferedImage.TYPE_INT_ARGB);
            buffers[_layer.ordinal()] = buffer;
            foundSomethingInGlobalCache = false;
        }
        else
            foundSomethingInGlobalCache = true;

        _value = buffer;

        return foundSomethingInGlobalCache;
    }

    public final void validate( StyleRenderState oldState, StyleRenderState newState )
    {
        _cachingMakesSense = cachingMakesSenseFor(newState);
        if ( !_cachingMakesSense ) {
            _value = null;
            _renderIntoCache = false;
            return;
        }

        if ( newState.currentBounds().width() == 0 || newState.currentBounds().height() == 0 ) {
            _renderIntoCache = true;
            return;
        }

        boolean newBufferAllocated = false;
        Bounds bounds = newState.currentBounds();
        if ( _value != null ) {
            boolean sizeChanged = bounds.width() != _value.getWidth() || bounds.height() != _value.getHeight();
            if ( sizeChanged ) {
                boolean foundSomethingInGlobalCache = allocateOrGetCachedBuffer(newState);
                newBufferAllocated = !foundSomethingInGlobalCache;
            }
        }
        else
        {
            boolean foundSomethingInGlobalCache = allocateOrGetCachedBuffer(newState);
            newBufferAllocated = !foundSomethingInGlobalCache;
        }

        if ( newBufferAllocated || !leadsToSameValue(oldState, newState) ) {
            _renderIntoCache = true;
            if ( !newBufferAllocated ) {
                // We clear the image manually so that the alpha channel is cleared to 0.
                Graphics2D g = _value.createGraphics();
                g.setBackground(new Color(0, 0, 0, 0));
                g.clearRect(0, 0, _value.getWidth(), _value.getHeight());
                g.dispose();
            }
        }
    }

    public final void paint( StylePainter painter, Graphics2D g )
    {
        if ( !_cachingMakesSense ) {
            produce(painter, g);
            return;
        }

        if ( _value == null )
            return;

        if ( _renderIntoCache ) {
            Graphics2D g2 = _value.createGraphics();
            g2.setBackground(g.getBackground());
            g2.setClip(null);
            g2.setComposite(g.getComposite());
            g2.setPaint(g.getPaint());
            g2.setRenderingHints(g.getRenderingHints());
            g2.setStroke(g.getStroke());
            produce(painter, g2);
            _renderIntoCache = false;
        }

        g.drawImage(_value, 0, 0, null);
    }

    protected abstract void produce( StylePainter painter, Graphics2D g );

    public abstract boolean leadsToSameValue( StyleRenderState oldState, StyleRenderState newState );

    public abstract boolean cachingMakesSenseFor( StyleRenderState state );
}
