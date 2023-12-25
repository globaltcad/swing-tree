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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 *  Orchestrates the rendering of a component's style and animations. <br>
 *  Note that this class is immutable so that it is easier to reason about...
 */
final class StyleEngine
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(StyleEngine.class);

    public static StyleEngine create() {
        return new StyleEngine(ComponentConf.none(), new Expirable[0], null);
    }

    static boolean IS_ANTIALIASING_ENABLED(){
        return UI.scale() < 1.5;
    }


    private final ComponentConf        _componentConf;
    private final Expirable<Painter>[] _animationPainters;
    private final LayerCache[]         _layerCaches;


    private StyleEngine(
        ComponentConf         componentConf,
        Expirable<Painter>[]  animationPainters,
        LayerCache[]          layerCaches // Null when the style engine is freshly created
    ) {
        _componentConf = Objects.requireNonNull(componentConf);
        _animationPainters = Objects.requireNonNull(animationPainters);
        if ( layerCaches == null ) {
            layerCaches = new LayerCache[UI.Layer.values().length];
            for ( int i = 0; i < layerCaches.length; i++ )
                layerCaches[i] = new LayerCache(UI.Layer.values()[i]);
        }
        _layerCaches = Objects.requireNonNull(layerCaches);
    }

    ComponentConf getComponentConf() { return _componentConf; }

    StyleEngine withNewStyleAndComponent( Style newStyle, JComponent component ) {
        ComponentConf newConf = _componentConf.with(newStyle, component);
        for ( LayerCache layerCache : _layerCaches )
            layerCache.validate(_componentConf, newConf);
        return new StyleEngine( newConf, _animationPainters, _layerCaches);
    }

    StyleEngine withAnimationPainter( LifeTime lifeTime, Painter animationPainter ) {
        java.util.List<Expirable<Painter>> animationPainters = new ArrayList<>(Arrays.asList(_animationPainters));
        animationPainters.add(new Expirable<>(lifeTime, animationPainter));
        return new StyleEngine(_componentConf, animationPainters.toArray(new Expirable[0]), _layerCaches);
    }

    StyleEngine withoutAnimationPainters() {
        return new StyleEngine(_componentConf, new Expirable[0], _layerCaches);
    }

    StyleEngine withoutExpiredAnimationPainters() {
        List<Expirable<Painter>> animationPainters = new ArrayList<>(Arrays.asList(_animationPainters));
        animationPainters.removeIf(Expirable::isExpired);
        return new StyleEngine(_componentConf, animationPainters.toArray(new Expirable[0]), _layerCaches);
    }

    Style getStyle() { return _componentConf.style(); }

    void paintWithContentAreaClip( Graphics g, Runnable painter ) {
        Shape oldClip = g.getClip();

        Shape newClip = _componentConf.getMainComponentArea();
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

    void paintAnimations( Graphics2D g2d )
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

    void paintBorder( Graphics2D g2d, Runnable formerBorderPainter )
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

        try {
            formerBorderPainter.run();
        } catch ( Exception ex ) {
            /*
                Note that if any exceptions happen during the border style painting,
                then we don't want to mess up how the rest of the component is painted...
                Therefore, we catch any exceptions that happen in the above code.
            */
            log.error("Exception while painting former border!", ex);
        }
    }

    public void paintForeground( Graphics2D g2d )
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

    private void _render( UI.Layer layer, Graphics2D g2d ) {
        _layerCaches[layer.ordinal()].paint(this, g2d, graphics -> {
            StyleRenderer.renderStyleFor(this, layer, graphics);
        });
    }

}
