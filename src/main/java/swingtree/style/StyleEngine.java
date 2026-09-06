package swingtree.style;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import sprouts.Pair;
import swingtree.SwingTree;
import swingtree.UI;
import swingtree.layout.Bounds;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 *  Orchestrates the rendering of a component's style and animations. <br>
 *  Note that this class is immutable so that it is easier to reason about...
 */
final class StyleEngine
{
    private static final Logger    log        = org.slf4j.LoggerFactory.getLogger(StyleEngine.class);
    private static final UI.Layer[] ALL_LAYERS = UI.Layer.values();

    static StyleEngine create() {
        return new StyleEngine(new Pooled<>(BoxModelConf.none()), new Pooled<>(ComponentConf.none()), null);
    }

    static boolean IS_ANTIALIASING_ENABLED(){
        return UI.scale() < 2.25;
    }

    private final Pooled<BoxModelConf>  _boxModelConf;
    private final Pooled<ComponentConf> _componentConf;
    private final StyleLayerCache[]     _layerCaches;


    private StyleEngine(
        Pooled<BoxModelConf>  boxModelConf,
        Pooled<ComponentConf> componentConf,
        @Nullable StyleLayerCache[] layerCaches // Null when the style engine is freshly created
    ) {
        _boxModelConf  = Objects.requireNonNull(boxModelConf).intern();
        _componentConf = Objects.requireNonNull(componentConf).intern();
        if ( layerCaches == null ) {
            layerCaches = new StyleLayerCache[ALL_LAYERS.length];
            for ( int i = 0; i < layerCaches.length; i++ )
                layerCaches[i] = new StyleLayerCache(ALL_LAYERS[i]);
        }
        _layerCaches = Objects.requireNonNull(layerCaches);
    }

    ComponentConf getComponentConf() { return _componentConf.get(); }

    StyleLayerCache[] getLayerCaches() { return _layerCaches; }

    BoxModelConf getBoxModelConf() { return _boxModelConf.get(); }

    Optional<Shape> componentArea( UI.ComponentArea area ) {
        ComponentAreas areas = ComponentAreas.of(_boxModelConf);
        if ( areas.areaExists(area) )
            return Optional.ofNullable(areas.get(area));
        return Optional.empty();
    }

    @SuppressWarnings("ReferenceEquality") // Identity means the configuration is literally the one already installed.
    StyleEngine update(
        final Bounds      newBounds,
        final StyleConf   newStyle,
        final Outline     marginCorrection
    ) {
        final ComponentConf currentConf = getComponentConf();
        final Pair<BoxModelConf, ComponentConf> boxModelAndComponentConfs = _calculateBoxModelAndComponentConfs(newBounds, newStyle, marginCorrection, currentConf);
        final ComponentConf newConf = boxModelAndComponentConfs.second();

        for ( StyleLayerCache layerCache : _layerCaches )
            layerCache.validate(newConf);

        if ( newConf == currentConf )
            return this;

        return new StyleEngine(new Pooled<>(boxModelAndComponentConfs.first()), new Pooled<>(newConf), _layerCaches);
    }

    static sprouts.Pair<BoxModelConf, ComponentConf> _calculateBoxModelAndComponentConfs(
            final Bounds        newBounds,
            final StyleConf     newStyle,
            final Outline       marginCorrection,
            final ComponentConf previousConf
    ) {
        final boolean sameStyle      = previousConf.style().equals(newStyle);
        final boolean sameBounds     = previousConf.currentBounds().equals(newBounds);
        final boolean sameCorrection = previousConf.areaMarginCorrection().equals(marginCorrection);

        ComponentConf newConf;
        if ( sameStyle && sameBounds && sameCorrection )
            newConf = previousConf;
        else
            newConf = new ComponentConf(sameStyle ? previousConf.style() : newStyle, newBounds, marginCorrection);

        BoxModelConf newBoxModelConf = BoxModelConf.of(newConf.style().border(), newConf.areaMarginCorrection(), newConf.currentBounds().size());
        return Pair.of(newBoxModelConf, newConf);
    }

    void renderBackgroundStyle( Graphics2D g2d, @Nullable BufferedImage parentRendering, int x, int y ) {
        // A component may have a filter on the parent:
        if ( parentRendering != null ) {
            FilterConf filter = _componentConf.get().style().layers().filter();
            if ( !filter.equals(FilterConf.none()) ) {
                // Location relative to the parent:
                try {
                    StyleRenderer.renderParentFilter(filter, parentRendering, g2d, x, y, _boxModelConf);
                } catch ( Exception ex ) {
                    log.error(SwingTree.get().logMarker(), "Exception while trying to apply and render parent filter!", ex);
                }
            }
        }
        _render(UI.Layer.BACKGROUND, g2d);
    }

    void paintBorder( Graphics2D g2d, Consumer<Graphics> formerBorderPainter ) {
        _render(UI.Layer.CONTENT, g2d);
        _render(UI.Layer.BORDER, g2d);
        try {
            formerBorderPainter.accept(g2d);
        } catch ( Exception ex ) {
            /*
                Note that if any exceptions happen during the border style painting,
                then we don't want to mess up how the rest of the component is painted...
                Therefore, we catch any exceptions that happen in the above code.
            */
            log.error(SwingTree.get().logMarker(), "Exception while painting former border!", ex);
        }
    }

    void paintForeground( Graphics2D g2d ) {
        _render(UI.Layer.FOREGROUND, g2d);
    }

    @SuppressWarnings("EnumOrdinal") // Layer ordinals are used intentionally to index the per-layer cache array.
    private void _render( UI.Layer layer, Graphics2D g2d ) {

        final boolean antialiasingEnabled = IS_ANTIALIASING_ENABLED();

        // We remember if antialiasing was enabled before we render (only when we will touch the hint):
        final boolean antialiasingWasEnabled;
        if ( antialiasingEnabled ) {
            antialiasingWasEnabled = g2d.getRenderingHint( RenderingHints.KEY_ANTIALIASING ) == RenderingHints.VALUE_ANTIALIAS_ON;
            g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        } else {
            antialiasingWasEnabled = false;
        }

        /*
            The caches are created from ALL_LAYERS, so the layer's own ordinal is what indexes
            them - deliberately not a hardcoded layer-to-index switch. Each cache now renders
            through the layer it was constructed with, so a mapping that disagreed with
            ALL_LAYERS would not merely cache in the wrong slot, it would paint the wrong
            layer's style. ComponentExtension indexes the very same array the same way.
        */
        final int layerIndex = layer.ordinal();
        if ( layerIndex >= 0 && layerIndex < _layerCaches.length )
            _layerCaches[layerIndex].paint(g2d);
        else
            log.error(SwingTree.get().logMarker(),
                    "No layer cache for layer: {}",
                    layer, new Throwable("Stack trace for debugging purposes.")
                );

        // Reset antialiasing to its previous state (only when we changed it):
        if ( antialiasingEnabled )
            g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                    antialiasingWasEnabled
                            ? RenderingHints.VALUE_ANTIALIAS_ON
                            : RenderingHints.VALUE_ANTIALIAS_OFF
            );
    }

}
