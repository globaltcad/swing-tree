package swingtree.style;

import sprouts.Tuple;
import swingtree.UI;
import swingtree.layout.Size;

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
 *  painting the layer whole.
 */
final class StyleLayerCache
{
    private static final int PAINTS_UNTIL_REJOINED = 4;

    private final UI.Layer         _layer;
    private final BiConsumer<LayerRenderConf, Graphics2D> _renderer;
    private LayerPartitionCache[]  _parts;
    private boolean                _isSplitAroundNoises;
    private LayerRenderConf        _uncachedPartition;
    private Size                   _lastSize;
    private int                    _paintsAtThisSize;

    private int _paintCacheHitCount  = 0;
    private int _paintCacheMissCount = 0;


    StyleLayerCache( UI.Layer layer ) {
        _layer                 = Objects.requireNonNull(layer);
        _renderer              = ( conf, graphics ) -> StyleRenderer.renderStyleOn(_layer, conf, graphics);
        _parts                 = new LayerPartitionCache[] { new LayerPartitionCache(layer, LayerRenderConfPartitions.WHOLE) };
        _isSplitAroundNoises   = false;
        _uncachedPartition = LayerRenderConf.none();
        _lastSize              = Size.unknown();
        _paintsAtThisSize      = PAINTS_UNTIL_REJOINED;
    }

    int paintCacheHitCount() { return _paintCacheHitCount; }

    int paintCacheMissCount() { return _paintCacheMissCount; }

    void validate( ComponentConf newConf ) {
        final LayerRenderConf full = newConf.renderConfFor(_layer);
        // Note that _isResizing() records the size and so must run on every validation:
        final boolean split = _isResizing(newConf.currentBounds().size()) && _canBeSplitAroundNoises(full);
        if ( split != _isSplitAroundNoises ) {
            _isSplitAroundNoises = split;
            _parts = split
                    ? new LayerPartitionCache[] {
                            new LayerPartitionCache(_layer, LayerRenderConfPartitions.UNDER_NOISE),
                            new LayerPartitionCache(_layer, LayerRenderConfPartitions.OVER_NOISE)
                        }
                    : new LayerPartitionCache[] {
                            new LayerPartitionCache(_layer, LayerRenderConfPartitions.WHOLE)
                        };
        }
        for ( LayerPartitionCache part : _parts )
            part.validate(newConf);

        _uncachedPartition = split
                ? LayerRenderConfPartitions.NOISES.restrict(full)
                : LayerRenderConf.none();
    }

    private boolean _isResizing( Size size ) {
        if ( !size.equals(_lastSize) ) {
            final boolean grewFromNothing = !_lastSize.hasPositiveWidth() || !_lastSize.hasPositiveHeight();
            _lastSize         = size;
            _paintsAtThisSize = grewFromNothing ? PAINTS_UNTIL_REJOINED : 0;
        }
        return _paintsAtThisSize < PAINTS_UNTIL_REJOINED;
    }

    private static boolean _canBeSplitAroundNoises( LayerRenderConf conf ) {
        if ( !conf.layer().hasRenderableNoises() )
            return false; // Nothing to cut around.
        /*
            Size independent caching is switched off, so a cut could win nothing anyway - but this
            is stated here rather than left to the checks below, because that switch is the
            documented safety hatch for restoring the classic exact-size caching (see
            SwingTree.setCacheTilingEnabled), and a layer painted in pieces is not that.
        */
        if ( !CacheBudget.tilingEnabled() )
            return false;
        if ( !StyleRenderer.allNoisesAreCheapToReplay(conf) )
            return false; // Replaying this noise every paint would cost more than it saves.
        return _isWorthCuttingOut(LayerRenderConfPartitions.UNDER_NOISE.restrict(conf))
            && _isWorthCuttingOut(LayerRenderConfPartitions.OVER_NOISE.restrict(conf));
    }

    private static boolean _isWorthCuttingOut( LayerRenderConf part ) {
        return part.rendersNothing() || LayerPartitionCache.cachesSizeIndependently(part);
    }

    void paint( Graphics2D g2d ) {
        boolean anythingRendered          = false;
        boolean anythingRenderedFromStyle = false;
        boolean anythingRenderedFromCache = false;
        for ( int i = 0; i < _parts.length; i++ ) {
            // The lifted out noises go between the parts - which is the only reason a layer
            // ever has more than one part, so "not before the first" locates them exactly:
            if ( i > 0 ) {
                final Size size = _uncachedPartition.boxModel().size();
                if ( size.hasPositiveWidth() && size.hasPositiveHeight() ) {
                    StyleRenderer.renderStyleOn(_layer, _uncachedPartition, g2d);
                    anythingRendered = true;
                }
            }
            LayerPartitionCache.PaintOutcome outcome = _parts[i].paint(g2d, _renderer);
            anythingRendered          |= ( outcome != LayerPartitionCache.PaintOutcome.NOTHING_RENDERED   );
            anythingRenderedFromStyle |= ( outcome == LayerPartitionCache.PaintOutcome.RENDERED_FROM_STYLE);
            anythingRenderedFromCache |= ( outcome == LayerPartitionCache.PaintOutcome.RENDERED_FROM_CACHE);
        }

        if ( _paintsAtThisSize < PAINTS_UNTIL_REJOINED )
            _paintsAtThisSize++;

        if ( !anythingRendered )
            return; // Nothing reached the destination, so this was not a paint of this layer.

        if ( anythingRenderedFromStyle || !anythingRenderedFromCache )
            _paintCacheMissCount++;
        else
            _paintCacheHitCount++;
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
