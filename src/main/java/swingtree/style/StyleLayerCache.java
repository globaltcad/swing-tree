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
    /**
     *  How many validations at an unchanged size end a cut (see {@link #_isResizing}). Roughly
     *  a validation per paint, so this is a short tail after the last drag frame - long enough
     *  to bridge the paints a drag emits at a repeated size, short enough to be imperceptible.
     */
    private static final int VALIDATIONS_UNTIL_REJOINED = 8;

    private final UI.Layer         _layer;
    /**
     *  The renderer handed to every part, allocated once instead of per part and per paint,
     *  because this sits directly in the paint path. A part draws itself by handing this a
     *  <i>restricted</i> configuration - the renderer draws whatever the configuration
     *  contains, so it needs no notion of parts at all.
     */
    private final BiConsumer<LayerRenderConf, Graphics2D> _renderer;
    /** The cached parts of this layer, in paint order. */
    private LayerPartCache[]       _parts;
    /** Whether the layer is currently cut around its noises, see {@link #validate}. */
    private boolean                _isSplitAroundNoises;
    /** The noises, ready to be replayed straight onto the destination between the cached
     *  parts. Empty whenever the layer is not split, in which case nothing is replayed. */
    private LayerRenderConf        _noises;
    /** The size seen by the previous validation, and how many validations ago it last
     *  changed - the resize detection {@link #_isResizing} implements. */
    private Size                   _lastSize;
    private int                    _validationsAtThisSize;
    /*
     *  Per-layer utilisation counters, observable through the ComponentExtension public API,
     *  which is also how the test suite reasons about cache effectiveness without coupling to
     *  any of the package-private machinery. They are counted here rather than per part
     *  because whether a *layer* was served from cache is a property of the whole paint - and
     *  because the parts themselves are replaced whenever the layer starts or stops being cut
     *  around its noises, which must not reset a counter the public API promises is cumulative.
     */
    private int _paintCacheHitCount  = 0;
    private int _paintCacheMissCount = 0;


    StyleLayerCache( UI.Layer layer ) {
        _layer                 = Objects.requireNonNull(layer);
        _renderer              = ( conf, graphics ) -> StyleRenderer.renderStyleOn(_layer, conf, graphics);
        _parts                 = new LayerPartCache[] { new LayerPartCache(layer, StyleLayerPart.WHOLE) };
        _isSplitAroundNoises   = false;
        _noises                = LayerRenderConf.none();
        _lastSize              = Size.unknown();
        _validationsAtThisSize = VALIDATIONS_UNTIL_REJOINED;
    }

    /**
     *  Decides how many parts this layer is cached in, and validates them. <br>
     *  <br>
     *  A noise can neither be cached size independently (its pixels vary per pixel position)
     *  nor needs caching at all (the noise tiles behind it already live in a size independent
     *  space). Cutting a layer around it therefore lets everything else on that layer keep the
     *  small exemplar that survives a resize, which a noise in the same image would deny it -
     *  but only while the component actually resizes, see {@link #_isResizing} and
     *  {@link #_canBeSplitAroundNoises}. Flipping between the two shapes replaces the
     *  {@link LayerPartCache}s, costing one re-render.
     */
    void validate( ComponentConf newConf ) {
        final LayerRenderConf full = newConf.renderConfFor(_layer);
        // Note that _isResizing() records the size and so must run on every validation:
        final boolean split = _isResizing(newConf.currentBounds().size()) && _canBeSplitAroundNoises(full);
        if ( split != _isSplitAroundNoises ) {
            _isSplitAroundNoises = split;
            _parts = split
                    ? new LayerPartCache[] {
                            new LayerPartCache(_layer, StyleLayerPart.UNDER_NOISE),
                            new LayerPartCache(_layer, StyleLayerPart.OVER_NOISE)
                        }
                    : new LayerPartCache[] {
                            new LayerPartCache(_layer, StyleLayerPart.WHOLE)
                        };
        }
        for ( LayerPartCache part : _parts )
            part.validate(newConf);

        _noises = split
                ? StyleLayerPart.NOISES.restrict(full)
                : LayerRenderConf.none();
    }

    /**
     *  Whether the component size is currently in flux, recording the supplied size for the
     *  next call. <br>
     *  <br>
     *  Cutting a layer hinges on this, because a cut only earns anything on a paint which
     *  would otherwise have missed the cache - for a noise-bearing layer, a paint at an unseen
     *  size. At an unchanged size a cut is a loss by construction: several stretch and tile
     *  blits replacing the single exact-size blit of a whole-layer hit. Measured on a large
     *  noise gradient, cutting regardless is ~5x faster while resizing and ~60% slower at an
     *  unchanged size - and a UI spends nearly all its paints at an unchanged size. <br>
     *  <br>
     *  Hence the tail rather than a bare "differs from last time": a validation runs per paint,
     *  and a drag emits plenty of paints at a repeated size. And hence growing out of an
     *  unrenderable size does not count - a component is validated at 0x0 before it is ever
     *  laid out, and reaching its first real size is a birth rather than a resize.
     */
    private boolean _isResizing( Size size ) {
        if ( !size.equals(_lastSize) ) {
            final boolean grewFromNothing = !_lastSize.hasPositiveWidth() || !_lastSize.hasPositiveHeight();
            _lastSize              = size;
            _validationsAtThisSize = grewFromNothing ? VALIDATIONS_UNTIL_REJOINED : 0;
        }
        else if ( _validationsAtThisSize < VALIDATIONS_UNTIL_REJOINED )
            _validationsAtThisSize++;

        return _validationsAtThisSize < VALIDATIONS_UNTIL_REJOINED;
    }

    /**
     *  Whether cutting this layer around its noises would achieve anything, which it does
     *  exactly when everything left on both sides of the noise can then be cached size
     *  independently. When it cannot - a gradient under the noise, say - that part is
     *  re-rendered at every size either way and lifting the noise out only adds a per paint
     *  cost on top (measured on {@code ChatView}: cutting regardless took a repaint from
     *  10.6 to 16.4 ms). <br>
     *  <br>
     *  The noise must also be big enough for {@link StyleRenderer.NoisePaintCache#renderNoise}
     *  to blit pre-rendered tiles for it. Below that it falls back to the per-pixel
     *  {@link java.awt.Paint} pipeline, which is fine into a software image but ruinous
     *  straight onto an accelerated surface - measured 3x worse at 240x240.
     */
    private static boolean _canBeSplitAroundNoises( LayerRenderConf conf ) {
        if ( !conf.layer().hasRenderableNoises() )
            return false; // Nothing to cut around.
        if ( !CacheBudget.tilingEnabled() )
            return false; // Size independent caching is switched off, so there is nothing to win.
        if ( !StyleRenderer.allNoisesUseLargeTiles(conf) )
            return false; // Replaying this noise every paint would cost more than it saves.
        return LayerPartCache.isStretchTileable(StyleLayerPart.UNDER_NOISE.restrict(conf))
            && LayerPartCache.isStretchTileable(StyleLayerPart.OVER_NOISE.restrict(conf));
    }

    void paint( Graphics2D g2d ) {
        boolean anyPainted  = false;
        boolean anyRendered = false;
        for ( int i = 0; i < _parts.length; i++ ) {
            /*
                The noises go in between the cached parts, straight onto the destination at the
                component's real size. Painting the pieces one after another is pixel identical
                to painting the layer whole, because source-over compositing is associative -
                see StyleLayerPart. The replay is not a cache outcome of its own: it is
                unconditional work, which is why the cut is gated on the component resizing.
            */
            if ( i > 0 )
                StyleRenderer.renderStyleOn(_layer, _noises, g2d);
            LayerPartCache.PaintOutcome outcome = _parts[i].paint(g2d, _renderer);
            anyPainted  |= ( outcome != LayerPartCache.PaintOutcome.NOT_PAINTED );
            anyRendered |= ( outcome == LayerPartCache.PaintOutcome.RENDERED   );
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
        for ( LayerPartCache part : _parts ) {
            BufferedImage rendered = part.renderedImage();
            if ( rendered != null )
                images.add(rendered);
        }
        return Tuple.of(BufferedImage.class, images);
    }

    /** Paints served entirely from a cached image. A paint only counts when every part of the
     *  layer which draws anything was served from its cache, because a layer painted partly
     *  from a cache and partly by the renderer is, from the outside, a paint the renderer was
     *  invoked for. A part holding none of the layer's style neither paints nor counts. */
    int paintCacheHitCount() { return _paintCacheHitCount; }

    /** Paints which had to invoke the style renderer for at least one part of the layer. */
    int paintCacheMissCount() { return _paintCacheMissCount; }
}
