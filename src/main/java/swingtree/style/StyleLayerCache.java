package swingtree.style;

import sprouts.Tuple;
import swingtree.UI;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *  Caches and paints one {@link UI.Layer} of a component's style, by composing the
 *  {@link StyleLayerPart}s that layer is made of. <br>
 *  <br>
 *  Ordinarily a layer is a single {@link StyleLayerPart#WHOLE} part, and this is then nothing
 *  more than a wrapper around the one {@link LayerPartCache} holding it. The reason it exists is
 *  that a layer mixing style which caches in incompatible ways cannot be one rasterization, and
 *  then it is painted as several parts in order - see {@link StyleLayerPart} for what those parts
 *  are and why source-over compositing makes painting them one after another identical to
 *  painting the layer whole.
 */
final class StyleLayerCache
{
    private final UI.Layer         _layer;
    /** The parts this layer is cached and painted in, in paint order. */
    private final LayerPartCache[] _parts;


    StyleLayerCache( UI.Layer layer ) {
        _layer = Objects.requireNonNull(layer);
        _parts = new LayerPartCache[] { new LayerPartCache(layer, StyleLayerPart.WHOLE) };
    }

    void validate( ComponentConf newConf ) {
        for ( LayerPartCache part : _parts )
            part.validate(newConf);
    }

    void paint( Graphics2D g2d ) {
        for ( LayerPartCache part : _parts )
            part.paint(g2d, ( conf, graphics ) -> StyleRenderer.renderStyleOn(_layer, conf, graphics));
    }

    /** The rendered images of the parts which currently have one, in paint order. */
    Tuple<BufferedImage> renderedImages() {
        List<BufferedImage> images = new ArrayList<>(_parts.length);
        for ( LayerPartCache part : _parts ) {
            BufferedImage rendered = part.renderedImage();
            if ( rendered != null )
                images.add(rendered);
        }
        return Tuple.of(BufferedImage.class, images);
    }

    /** Paints served entirely from a cached image. A paint only counts when every part of the
     *  layer was served from its cache, because a layer painted partly from a cache and partly
     *  by the renderer is, from the outside, a paint the renderer was invoked for. */
    int paintCacheHitCount() {
        int hits = Integer.MAX_VALUE;
        for ( LayerPartCache part : _parts )
            hits = Math.min(hits, part.paintCacheHitCount());
        return ( hits == Integer.MAX_VALUE ? 0 : hits );
    }

    /** Paints which had to invoke the style renderer for at least one part of the layer. */
    int paintCacheMissCount() {
        int misses = 0;
        for ( LayerPartCache part : _parts )
            misses = Math.max(misses, part.paintCacheMissCount());
        return misses;
    }
}
