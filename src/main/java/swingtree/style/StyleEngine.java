package swingtree.style;

import org.slf4j.Logger;
import swingtree.UI;
import swingtree.animation.LifeTime;
import swingtree.api.Painter;

import javax.swing.JComponent;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.util.*;

/**
 *  Orchestrates the rendering of a component's style and animations.
 */
final class StyleEngine
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(StyleEngine.class);

    public static StyleEngine none() {
        CachedStylePainter[] layerCaches = new CachedStylePainter[UI.Layer.values().length];
        return new StyleEngine(StyleRenderState.none(), new Expirable[0], new CachedShapeCalculator(), layerCaches);
    }

    static boolean IS_ANTIALIASING_ENABLED(){
        return UI.scale() < 1.5;
    }


    private final StyleRenderState       _state;
    private final Expirable<Painter>[]   _animationPainters;
    private final CachedShapeCalculator  _shapes;
    private final CachedStylePainter[]   _layerPainters;


    private StyleEngine(
        StyleRenderState      state,
        Expirable<Painter>[]  animationPainters,
        CachedShapeCalculator shapes,
        CachedStylePainter[]  layerCache
    ) {
        _state             = Objects.requireNonNull(state);
        _animationPainters = Objects.requireNonNull(animationPainters);
        _shapes            = Objects.requireNonNull(shapes);
        _layerPainters     = Objects.requireNonNull(layerCache);
        for ( int i = 0; i < _layerPainters.length && _layerPainters[i] == null; i++ )
            _layerPainters[i] = new CachedStylePainter(UI.Layer.values()[i]);
    }

    StyleRenderState getState() { return _state; }

    StyleEngine withNewStyleAndComponent( Style style, JComponent component ) {
        StyleRenderState newState = _state.with(style, component);
        _shapes.validate(_state, newState);
        for ( CachedStylePainter layerCache : _layerPainters)
            layerCache.validate(_state, newState);
        return new StyleEngine( newState, _animationPainters, _shapes, _layerPainters);
    }

    StyleEngine withAnimationPainter(LifeTime lifeTime, Painter animationPainter ) {
        java.util.List<Expirable<Painter>> animationPainters = new ArrayList<>(Arrays.asList(_animationPainters));
        animationPainters.add(new Expirable<>(lifeTime, animationPainter));
        return new StyleEngine( _state, animationPainters.toArray(new Expirable[0]), _shapes, _layerPainters);
    }

    StyleEngine withoutAnimationPainters() {
        return new StyleEngine( _state, new Expirable[0], _shapes, _layerPainters);
    }

    StyleEngine withoutExpiredAnimationPainters() {
        List<Expirable<Painter>> animationPainters = new ArrayList<>(Arrays.asList(_animationPainters));
        animationPainters.removeIf(Expirable::isExpired);
        return new StyleEngine( _state, animationPainters.toArray(new Expirable[0]), _shapes, _layerPainters);
    }

    Style getStyle() { return _state.style(); }

    Optional<Shape> interiorArea() {
        Shape contentClip = null;
        if ( _shapes.interiorComponentArea().exists() || getStyle().margin().isPositive() )
            contentClip = getInteriorArea();

        return Optional.ofNullable(contentClip);
    }

    Area getInteriorArea()
    {
        return _shapes.interiorComponentArea().getFor(_state);
    }

    Area getExteriorArea()
    {
        return _shapes.exteriorComponentArea().getFor(_state);
    }

    Area getBorderArea()
    {
        return _shapes.borderArea().getFor(_state);
    }

    void paintWithContentAreaClip( Graphics g, Runnable painter ) {
        Shape oldClip = g.getClip();

        Shape newClip = getInteriorArea();
        if ( newClip != null && newClip != oldClip ) {
            newClip = StyleUtility.intersect(newClip, oldClip);
            g.setClip(newClip);
        }

        painter.run();

        g.setClip(oldClip);
    }

    void renderBackgroundStyle( Graphics2D g2d )
    {
        // We remember if antialiasing was enabled before we render:
        boolean antialiasingWasEnabled = g2d.getRenderingHint( RenderingHints.KEY_ANTIALIASING ) == RenderingHints.VALUE_ANTIALIAS_ON;

        // We enable antialiasing:
        if ( IS_ANTIALIASING_ENABLED() )
            g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

        _render(UI.Layer.BACKGROUND, g2d);

        // Reset antialiasing to its previous state:
        g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, antialiasingWasEnabled ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF );
    }

    boolean hasNoPainters() {
        return _animationPainters.length == 0;
    }

    void renderAnimations(Graphics2D g2d )
    {
        // We remember if antialiasing was enabled before we render:
        boolean antialiasingWasEnabled = g2d.getRenderingHint( RenderingHints.KEY_ANTIALIASING ) == RenderingHints.VALUE_ANTIALIAS_ON;

        // We enable antialiasing:
        if ( StyleEngine.IS_ANTIALIASING_ENABLED() )
            g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

        // Animations are last: they are rendered on top of everything else:
        for ( Expirable<Painter> expirablePainter : _animationPainters )
            if ( !expirablePainter.isExpired() ) {
                try {
                    expirablePainter.get().paint(g2d);
                } catch ( Exception e ) {
                    e.printStackTrace();
                    log.warn(
                        "Exception while painting animation '" + expirablePainter.get() + "' " +
                        "with lifetime " + expirablePainter.getLifeTime()+ ".",
                        e
                    );
                    // An exception inside a painter should not prevent everything else from being painted!
                    // Note that we log as warning because exceptions during rendering are not considered
                    // as harmful as elsewhere!
                }
            }

        // Reset antialiasing to its previous state:
        g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, antialiasingWasEnabled ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF );
    }

    private void _render( UI.Layer layer, Graphics2D g2d ) {
        _layerPainters[layer.ordinal()].paint(this, g2d);
    }

    void _withClip( Graphics2D g2d, Shape clip, Runnable paintTask ) {
        Shape formerClip = g2d.getClip();
        g2d.setClip(clip);
        try {
            paintTask.run();
        } finally {
            g2d.setClip(formerClip);
        }
    }

    void paintBorderStyle( Graphics2D g2d )
    {
        // We remember if antialiasing was enabled before we render:
        boolean antialiasingWasEnabled = g2d.getRenderingHint( RenderingHints.KEY_ANTIALIASING ) == RenderingHints.VALUE_ANTIALIAS_ON;

        // We enable antialiasing:
        if ( IS_ANTIALIASING_ENABLED() )
            g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

        _render(UI.Layer.CONTENT, g2d);

        _render(UI.Layer.BORDER, g2d);

        // Reset antialiasing to its previous state:
        g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, antialiasingWasEnabled ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF );
    }

    public void paintForegroundStyle( Graphics2D g2d )
    {
        // We remember if antialiasing was enabled before we render:
        boolean antialiasingWasEnabled = g2d.getRenderingHint( RenderingHints.KEY_ANTIALIASING ) == RenderingHints.VALUE_ANTIALIAS_ON;

        // We enable antialiasing:
        if ( IS_ANTIALIASING_ENABLED() )
            g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

        _render(UI.Layer.FOREGROUND, g2d);

        // Reset antialiasing to its previous state:
        g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, antialiasingWasEnabled ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF );
    }

}