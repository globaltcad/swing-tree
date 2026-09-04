package swingtree.style;

import org.jspecify.annotations.Nullable;
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
 *  {@link LayerRenderConfPartition}s that layer is made of. <br>
 *  <br>
 *  Ordinarily a layer is a single {@link LayerRenderConfPartition#WHOLE} part, and this is then nothing
 *  more than a wrapper around the one {@link LayerPartitionCache} holding it. The reason it exists is
 *  that a layer mixing style which caches in incompatible ways cannot be one rasterization, and
 *  then it is painted as several parts in order - see {@link LayerRenderConfPartition} for what those parts
 *  are and why source-over compositing makes painting them one after another identical to
 *  painting the layer whole.
 */
final class StyleLayerCache
{
    private static final int PAINTS_UNTIL_REJOINED = 4;

    // Sum type based states of what this cache was last brought in line with
    private interface Validation {
        final class None implements Validation { static final None INSTANCE = new None(); }
        final class Done implements Validation {
            final ComponentConf _conf;
            final boolean       _whileResizing;
            final boolean       _withTiling;
            Done( ComponentConf conf, boolean whileResizing, boolean withTiling ) {
                _conf          = conf;
                _whileResizing = whileResizing;
                _withTiling    = withTiling;
            }
        }
    }

    private final UI.Layer         _layer;
    private final BiConsumer<LayerRenderConf, Graphics2D> _renderer;
    private LayerPartitionCache[]  _parts;
    private PartitioningPolicy     _partitioningPolicy;
    private LayerRenderConf        _uncachedPartition;
    private Size                   _lastSize;
    private int                    _paintsAtThisSize;
    private Validation             _validation;

    private int _paintCacheHitCount  = 0;
    private int _paintCacheMissCount = 0;


    StyleLayerCache( UI.Layer layer ) {
        _layer               = Objects.requireNonNull(layer);
        _renderer            = ( conf, graphics ) -> StyleRenderer.renderStyleOn(_layer, conf, graphics);
        _partitioningPolicy  = PartitioningPolicy.NONE;
        _parts               = PartitioningPolicy.NONE.newPartsFor(layer);
        _uncachedPartition   = LayerRenderConf.none();
        _lastSize            = Size.unknown();
        _paintsAtThisSize    = PAINTS_UNTIL_REJOINED;
        _validation          = Validation.None.INSTANCE;
    }

    int paintCacheHitCount() { return _paintCacheHitCount; }

    int paintCacheMissCount() { return _paintCacheMissCount; }

    void validate( ComponentConf newConf ) {
        _recordSize(newConf.currentBounds().size());
        final boolean isResizing   = _isResizing();
        final boolean tilingIsOn   = CacheBudget.tilingEnabled();
        if ( _isSettledAndSameAsLastValidation(newConf, isResizing, tilingIsOn) )
            return; // We avoid validation work if nothing changed!

        final LayerRenderConf full = newConf.renderConfFor(_layer);
        final PartitioningPolicy policy = _partitioningPolicyFor(full, isResizing);
        if ( policy != _partitioningPolicy ) {
            _partitioningPolicy = policy;
            _parts              = policy.newPartsFor(_layer);
        }
        for ( LayerPartitionCache part : _parts )
            part.validate(newConf, isResizing);

        _uncachedPartition = policy.uncachedPartitionOf(full);
        _validation        = new Validation.Done(newConf, isResizing, tilingIsOn);
    }

    @SuppressWarnings("ReferenceEquality") // Identity means the configuration is literally the one validated against.
    private boolean _isSettledAndSameAsLastValidation(ComponentConf conf, boolean isResizing, boolean tilingIsOn ) {
        if ( !(_validation instanceof Validation.Done) )
            return false;
        final Validation.Done done = (Validation.Done) _validation;
        if ( done._conf != conf || done._whileResizing != isResizing || done._withTiling != tilingIsOn )
            return false;
        for ( LayerPartitionCache part : _parts )
            if ( part.admissionDecisionLeftOpen() )
                return false; // The cache is NOT settled because at least one part has a rejected cache attempt!
        return true;
    }

    private PartitioningPolicy _partitioningPolicyFor( LayerRenderConf full, boolean isResizing ) {
        if ( _canBeSplitAroundPainters(full) )
            return PartitioningPolicy.AROUND_PAINTERS;
        if ( isResizing && _canBeSplitAroundNoises(full) )
            return PartitioningPolicy.AROUND_NOISES;
        return PartitioningPolicy.NONE;
    }

    private boolean _canBeSplitAroundPainters( LayerRenderConf conf ) {
        if ( !conf.layer().hasPaintersWhichCannotBeCached() )
            return false; // Nothing to cut around, or nothing that stops the layer being cached whole.
        final LayerRenderConf underPainters = LayerRenderConfPartition.UNDER_PAINTERS.restrict(conf);
        if ( underPainters.rendersNothing() )
            return false; // The painters are all there is, so a cut would only add bookkeeping.
        return LayerPartitionCache.wouldBeAdmitted(_layer, underPainters);
    }

    private void _recordSize( Size size ) {
        if ( size.equals(_lastSize) )
            return;
        final boolean grewFromNothing = !_lastSize.hasPositiveWidth() || !_lastSize.hasPositiveHeight();
        _lastSize         = size;
        _paintsAtThisSize = grewFromNothing ? PAINTS_UNTIL_REJOINED : 0;
    }

    private boolean _isResizing() {
        return _paintsAtThisSize < PAINTS_UNTIL_REJOINED;
    }

    private static boolean _canBeSplitAroundNoises( LayerRenderConf conf ) {
        if ( !conf.layer().hasRenderableNoises() )
            return false; // Nothing to cut around.

        if ( !CacheBudget.tilingEnabled() )
            return false;
        if ( !StyleRenderer.allNoisesAreCheapToRepaint(conf) )
            return false; // Repainting this noise every paint would cost more than it saves.
        return _isWorthCuttingOut(LayerRenderConfPartition.UNDER_NOISE.restrict(conf))
            && _isWorthCuttingOut(LayerRenderConfPartition.OVER_NOISE.restrict(conf));
    }

    private static boolean _isWorthCuttingOut( LayerRenderConf part ) {
        return part.rendersNothing() || LayerPartitionCache.wouldCompactADimension(part);
    }

    void paint( Graphics2D g2d ) {
        boolean anythingRendered          = false;
        boolean anythingRenderedFromStyle = false;
        boolean anythingRenderedFromCache = false;
        for ( int i = 0; i < _parts.length; i++ ) {
            LayerPartitionCache.PaintOutcome outcome = _parts[i].paint(g2d, _renderer);
            anythingRendered          |= ( outcome != LayerPartitionCache.PaintOutcome.NOTHING_RENDERED   );
            anythingRenderedFromStyle |= ( outcome == LayerPartitionCache.PaintOutcome.RENDERED_FROM_STYLE);
            anythingRenderedFromCache |= ( outcome == LayerPartitionCache.PaintOutcome.RENDERED_FROM_CACHE);
            // The lifted out piece always sits directly after the first cached part: between
            // the two parts of a noise cut, and on top of the single part of a painter cut.
            if ( i == 0 ) {
                final Size size = _uncachedPartition.boxModel().size();
                if ( size.hasPositiveWidth() && size.hasPositiveHeight() ) {
                    StyleRenderer.renderStyleOn(_layer, _uncachedPartition, g2d);
                    anythingRendered = true;
                }
            }
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


    /**
     *  The shapes a layer is cached in. Each one names the cached parts it consists of and the
     *  uncached piece (if any), which is instead rendered straight onto the destination after
     *  the first of them - see {@link LayerRenderConfPartition} for why a layer is ever cut.
     */
    private enum PartitioningPolicy
    {
        /** One cached rendering of everything. */
        NONE( Tuple.of(LayerRenderConfPartition.WHOLE), null ),
        /** Cut around the noises, so that everything else keeps a size independent cache
         *  entry across a resize. Temporary: only while the component is resizing. */
        AROUND_NOISES(
            Tuple.of(LayerRenderConfPartition.UNDER_NOISE, LayerRenderConfPartition.OVER_NOISE),
            LayerRenderConfPartition.NOISES
        ),
        /** Cut around the user painters, so that a layer which carries one is cacheable at
         *  all rather than being re-rendered whole on every paint. */
        AROUND_PAINTERS(
            Tuple.of(LayerRenderConfPartition.UNDER_PAINTERS),
            LayerRenderConfPartition.PAINTERS
        );

        @SuppressWarnings("ImmutableEnumChecker") // A Tuple is immutable, it is simply not annotated as such.
        private final Tuple<LayerRenderConfPartition>    _cachedParts;
        private final @Nullable LayerRenderConfPartition _uncachedPart;

        PartitioningPolicy( Tuple<LayerRenderConfPartition> cachedParts, @Nullable LayerRenderConfPartition uncachedPart ) {
            _cachedParts  = cachedParts;
            _uncachedPart = uncachedPart;
        }

        LayerPartitionCache[] newPartsFor( UI.Layer layer ) {
            LayerPartitionCache[] parts = new LayerPartitionCache[_cachedParts.size()];
            for ( int i = 0; i < parts.length; i++ )
                parts[i] = new LayerPartitionCache(layer, _cachedParts.get(i));
            return parts;
        }

        LayerRenderConf uncachedPartitionOf( LayerRenderConf full ) {
            return ( _uncachedPart == null ? LayerRenderConf.none() : _uncachedPart.restrict(full) );
        }
    }

}
