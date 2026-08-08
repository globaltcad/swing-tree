package swingtree.style;

import sprouts.Tuple;
import swingtree.UI;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 *  Caches and paints one {@link UI.Layer} of a component's style, by composing the
 *  {@link LayerRenderConfPartitions}s that layer is made of. <br>
 *  <br>
 *  Ordinarily a layer is a single {@link LayerRenderConfPartitions#WHOLE} part, and this is then nothing
 *  more than a wrapper around the one {@link LayerPartitionCache} holding it. The reason it exists is
 *  that a layer mixing style which caches in incompatible ways cannot be one rasterization, and
 *  then it is painted as several parts in order - see {@link LayerRenderConfPartitions} for what those parts
 *  are and why source-over compositing makes painting them one after another identical to
 *  painting the layer whole. <br>
 *  <br>
 *  <b>Status:</b> {@link #_parts} is currently always that single {@link LayerRenderConfPartitions#WHOLE}
 *  entry, so no layer is split yet and this composes a sequence of exactly one. Deciding when a
 *  layer should be built from the three noise parts instead is what this is preparation for, and
 *  {@link LayerRenderConfPartitions} documents the gate such a decision has to pass.
 */
final class StyleLayerCache
{
    private final UI.Layer         _layer;
    private final LayerPartitionCache[] _parts;
    private final BiConsumer<LayerRenderConf, Graphics2D> _renderer;

    private int _paintCacheHitCount  = 0;
    private int _paintCacheMissCount = 0;


    StyleLayerCache( UI.Layer layer ) {
        _layer    = Objects.requireNonNull(layer);
        _parts    = new LayerPartitionCache[] { new LayerPartitionCache(layer, LayerRenderConfPartitions.WHOLE) };
        _renderer = ( conf, graphics ) -> StyleRenderer.renderStyleOn(_layer, conf, graphics);
    }

    int paintCacheHitCount() { return _paintCacheHitCount; }

    int paintCacheMissCount() { return _paintCacheMissCount; }

    void validate( ComponentConf newConf ) {
        for ( LayerPartitionCache part : _parts )
            part.validate(newConf);
    }

    void paint( Graphics2D g2d ) {
        boolean anyPainted  = false;
        boolean anyRendered = false;
        for ( LayerPartitionCache part : _parts ) {
            LayerPartitionCache.PaintOutcome outcome = part.paint(g2d, _renderer);
            anyPainted  |= ( outcome != LayerPartitionCache.PaintOutcome.NOTHING_RENDERED);
            anyRendered |= ( outcome == LayerPartitionCache.PaintOutcome.RENDERED_FROM_STYLE);
        }
        if ( anyPainted ) {
            if ( anyRendered )
                _paintCacheMissCount++;
            else
                _paintCacheHitCount++;
        }
    }

    Tuple<BufferedImage> renderedImages() {
        List<BufferedImage> images = new ArrayList<>(_parts.length);
        for ( LayerPartitionCache part : _parts ) {
            BufferedImage rendered = part.renderedImage();
            if ( rendered != null )
                images.add(rendered);
        }
        return Tuple.of(BufferedImage.class, images);
    }

}
