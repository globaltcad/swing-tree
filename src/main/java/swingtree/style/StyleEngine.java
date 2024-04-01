package swingtree.style;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import swingtree.UI;
import swingtree.animation.LifeSpan;
import swingtree.api.Painter;

import javax.swing.JComponent;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.util.*;

/**
 *  Orchestrates the rendering of a component's style and animations. <br>
 *  Note that this class is immutable so that it is easier to reason about...
 */
final class StyleEngine
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(StyleEngine.class);

    static StyleEngine create() {
        return new StyleEngine(BoxModelConf.none(), ComponentConf.none(), null);
    }

    static boolean IS_ANTIALIASING_ENABLED(){
        return UI.scale() < 1.5;
    }

    private final BoxModelConf         _boxModelConf;
    private final ComponentConf        _componentConf;
    private final LayerCache[]         _layerCaches;


    private StyleEngine(
        BoxModelConf           boxModelConf,
        ComponentConf          componentConf,
        @Nullable LayerCache[] layerCaches // Null when the style engine is freshly created
    ) {
        _boxModelConf      = Objects.requireNonNull(boxModelConf);
        _componentConf     = Objects.requireNonNull(componentConf);
        if ( layerCaches == null ) {
            layerCaches = new LayerCache[UI.Layer.values().length];
            for ( int i = 0; i < layerCaches.length; i++ )
                layerCaches[i] = new LayerCache(UI.Layer.values()[i]);
        }
        _layerCaches = Objects.requireNonNull(layerCaches);
    }

    ComponentConf getComponentConf() { return _componentConf; }

    BoxModelConf getBoxModelConf() { return _boxModelConf; }

    void paintClippedTo( UI.ComponentArea area, Graphics g, Runnable painter ) {
        Shape oldClip = g.getClip();

        Shape newClip = ComponentAreas.of(_boxModelConf).get(area);
        if ( newClip != null && newClip != oldClip ) {
            newClip = StyleUtil.intersect(newClip, oldClip);
            g.setClip(newClip);
        }

        painter.run();

        g.setClip(oldClip);
    }

    Optional<Shape> componentArea() {
        Shape contentClip = null;
        ComponentAreas _areas = ComponentAreas.of(_boxModelConf);
        if ( _areas.bodyArea().exists() || _componentConf.style().margin().isPositive() )
            contentClip = _areas.get(UI.ComponentArea.BODY);

        return Optional.ofNullable(contentClip);
    }

    StyleEngine withNewStyleAndComponent(StyleConf newStyle, JComponent component ) {
        ComponentConf newConf = _componentConf.with(newStyle, component);
        BoxModelConf newBoxModelConf = BoxModelConf.of(newConf.style().border(), newConf.baseOutline(), newConf.currentBounds().size());
        for ( LayerCache layerCache : _layerCaches )
            layerCache.validate(_componentConf, newConf);
        return new StyleEngine(newBoxModelConf, newConf, _layerCaches);
    }

    StyleEngine withoutAnimationPainters() {
        return new StyleEngine(_boxModelConf, _componentConf, _layerCaches);
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

    void paintForeground( Graphics2D g2d )
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
        _layerCaches[layer.ordinal()].paint(g2d, (conf, graphics) -> {
            StyleRenderer.renderStyleOn(layer, conf, graphics);
        });
    }

}
