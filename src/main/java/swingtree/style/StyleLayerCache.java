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
    /** The parts this layer is cached and painted in, in paint order. */
    private final LayerPartitionCache[] _parts;
    /**
     *  The renderer handed to every part, allocated once instead of per part and per paint,
     *  because this sits directly in the paint path. A part draws itself by handing this a
     *  <i>restricted</i> configuration - the renderer draws whatever the configuration
     *  contains, so it needs no notion of parts at all.
     */
    private final BiConsumer<LayerRenderConf, Graphics2D> _renderer;
    /*
     *  Per-layer utilisation counters, observable through the ComponentExtension public API,
     *  which is also how the test suite reasons about cache effectiveness without coupling to
     *  any of the package-private machinery. They are counted here rather than per part
     *  because whether a *layer* was served from cache is a property of the whole paint.
     */
    private int _paintCacheHitCount  = 0;
    private int _paintCacheMissCount = 0;


    StyleLayerCache( UI.Layer layer ) {
        _layer    = Objects.requireNonNull(layer);
        _parts    = new LayerPartitionCache[] { new LayerPartitionCache(layer, LayerRenderConfPartitions.WHOLE) };
        _renderer = ( conf, graphics ) -> StyleRenderer.renderStyleOn(_layer, conf, graphics);
    }

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

    /** The rendered images of the parts which currently have one, in paint order. Parts without
     *  one are skipped rather than represented by a hole, so a position in the result does not
     *  identify a part and the size of the result tells you how many parts are currently
     *  rendered, not how many parts the layer has. */
    Tuple<BufferedImage> renderedImages() {
        List<BufferedImage> images = new ArrayList<>(_parts.length);
        for ( LayerPartitionCache part : _parts ) {
            BufferedImage rendered = part.renderedImage();
            if ( rendered != null )
                images.add(rendered);
        }
        return Tuple.of(BufferedImage.class, images);
    }

    /** Paints served entirely from a cached image. A paint only counts when every part of the
     *  layer was served from its cache, because a layer painted partly from a cache and partly
     *  by the renderer is, from the outside, a paint the renderer was invoked for. */
    int paintCacheHitCount() { return _paintCacheHitCount; }

    /** Paints which had to invoke the style renderer for at least one part of the layer. */
    int paintCacheMissCount() { return _paintCacheMissCount; }
}
