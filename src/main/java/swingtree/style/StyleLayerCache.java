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
     *  How many <i>paints</i> at an unchanged size end a cut (see {@link #_isResizing}) - a short
     *  tail after the last drag frame, long enough to bridge the paints a drag emits at a
     *  repeated size, short enough to be imperceptible. <br>
     *  <br>
     *  Deliberately counted in paints rather than in {@link #validate(ComponentConf)} calls, even
     *  though the decision is taken there: a validation is not a paint. It also runs when a
     *  component is merely asked for its border insets or has its style recomputed, and a single
     *  paint drives more than one of them (two, for the components measured), so a tail counted
     *  in validations would be a different length per component type and would drain without
     *  anything having been painted at all. <br>
     *  <br>
     *  Every paint reaching the layer drains it, including one this layer had nothing to draw
     *  for - see {@link #paint(Graphics2D)} for why that distinction must not be made here.
     */
    private static final int PAINTS_UNTIL_REJOINED = 4;

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
    /** The size seen by the previous validation, and how many paints ago it last changed -
     *  the resize detection {@link #_isResizing} implements. */
    private Size                   _lastSize;
    private int                    _paintsAtThisSize;
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
        _paintsAtThisSize      = PAINTS_UNTIL_REJOINED;
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
        final boolean isResizing = _isResizing(newConf.currentBounds().size());
        final boolean split = isResizing && _canBeSplitAroundNoises(full);
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
            part.validate(newConf, isResizing);

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
     *  Hence the tail rather than a bare "differs from last time": a drag emits plenty of paints
     *  at a repeated size, and dropping the cut between two of them would re-render the layer
     *  twice for nothing. And hence growing out of an unrenderable size does not count - a
     *  component is validated at 0x0 before it is ever laid out, and reaching its first real
     *  size is a birth rather than a resize.
     */
    private boolean _isResizing( Size size ) {
        if ( !size.equals(_lastSize) ) {
            final boolean grewFromNothing = !_lastSize.hasPositiveWidth() || !_lastSize.hasPositiveHeight();
            _lastSize         = size;
            _paintsAtThisSize = grewFromNothing ? PAINTS_UNTIL_REJOINED : 0;
        }
        return _paintsAtThisSize < PAINTS_UNTIL_REJOINED;
    }

    /**
     *  Whether cutting this layer around its noises would achieve anything, which it does
     *  exactly when everything left on both sides of the noise can then be cached size
     *  independently. When it cannot - a gradient under the noise, say - that part is
     *  re-rendered at every size either way and lifting the noise out only adds a per paint
     *  cost on top (measured on {@code ChatView}: cutting regardless took a repaint from
     *  10.6 to 16.4 ms). <br>
     *  <br>
     *  The noise must also be cheap enough to draw again on every paint, which for anything but
     *  a single coloured noise means being big enough for
     *  {@link StyleRenderer.NoisePaintCache#renderNoise} to blit pre-rendered tiles for it.
     *  Below that it falls back to the per-pixel {@link java.awt.Paint} pipeline, which is fine
     *  into a software image but ruinous straight onto an accelerated surface - measured 3x
     *  worse at 240x240.
     */
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
        return _isWorthCuttingOut(StyleLayerPart.UNDER_NOISE.restrict(conf))
            && _isWorthCuttingOut(StyleLayerPart.OVER_NOISE.restrict(conf));
    }

    /**
     *  Whether one side of the cut is worth having: either it draws nothing, in which case it
     *  costs nothing either, or it is genuinely cached size independently. <br>
     *  <br>
     *  The second condition is deliberately {@link LayerPartCache#cachesSizeIndependently} and
     *  not merely "is stretch tileable". A part can satisfy the tiling invariant and still be
     *  keyed on the exact component size, because a component is only ever mapped onto the
     *  exemplar when it is strictly larger than that exemplar in both dimensions - which a
     *  chunky corner radius or a wide shadow on a short component is not. Cutting such a layer
     *  hands us two exact-size images in place of one, both of them re-rendered at every new
     *  size, plus the noise replayed on top of that: strictly worse than not cutting at all. <br>
     *  <br>
     *  Measured, because it is the one gate that keeps the whole feature away from a busy UI:
     *  making this method return {@code true} unconditionally, so that only the temporal gate
     *  and the replay cost gate remain, is a wash on {@code ChatView}'s drag frames (7.9 vs
     *  7.9 ms, 3 interleaved runs) and consistently <i>worse</i> on a partial repaint
     *  (2.0 → 2.4 ms), which is what a caret blink or a hover costs. The extra exact-size
     *  entries such cuts mint are the likely reason: {@code LayerPartCache} stops admitting
     *  new entries once the global cache is full, so filling it up with debris locks other
     *  components out of caching altogether.
     */
    private static boolean _isWorthCuttingOut( LayerRenderConf part ) {
        return part.rendersNothing() || LayerPartCache.cachesSizeIndependently(part);
    }

    void paint( Graphics2D g2d ) {
        boolean anythingRendered          = false;
        boolean anythingRenderedFromStyle = false;
        boolean anythingRenderedFromCache = false;
        for ( int i = 0; i < _parts.length; i++ ) {
            // The lifted out noises go between the parts - which is the only reason a layer
            // ever has more than one part, so "not before the first" locates them exactly:
            if ( i > 0 )
                anythingRendered |= _paintNoises(g2d);
            LayerPartCache.PaintOutcome outcome = _parts[i].paint(g2d, _renderer);
            anythingRendered          |= ( outcome != LayerPartCache.PaintOutcome.NOTHING_RENDERED   );
            anythingRenderedFromStyle |= ( outcome == LayerPartCache.PaintOutcome.RENDERED_FROM_STYLE);
            anythingRenderedFromCache |= ( outcome == LayerPartCache.PaintOutcome.RENDERED_FROM_CACHE);
        }
        /*
            This, and not validate(), is what drains the rejoin tail, see PAINTS_UNTIL_REJOINED -
            and it deliberately happens before the "nothing was painted" return below. Whether the
            component size has settled is a question about the component, not about this layer:
            a layer holding none of the style draws nothing on every single paint, so counting
            only paints that drew something would leave such a layer reporting "in flux" forever
            after its first resize, and every gate hanging off _isResizing permanently armed.
            Measured on ChatView: without this, a narrow clip repaint at a long settled size still
            saw ~1200 layer validations believing the component was mid-drag.
        */
        if ( _paintsAtThisSize < PAINTS_UNTIL_REJOINED )
            _paintsAtThisSize++;

        if ( !anythingRendered )
            return; // Nothing reached the destination, so this was not a paint of this layer.

        /*
            A hit requires a cache to have actually served something. Without that clause a layer
            whose only style is the replayed noise - so that both cut out parts hold nothing and
            neither renders nor caches - would report a hit for a paint no cache took part in.
        */
        if ( anythingRenderedFromStyle || !anythingRenderedFromCache )
            _paintCacheMissCount++;
        else
            _paintCacheHitCount++;
    }

    /**
     *  Draws the lifted out noises straight onto the destination at the component's real size,
     *  in between the cached parts, and reports whether it drew anything. <br>
     *  <br>
     *  Painting the pieces one after another is pixel identical to painting the layer whole,
     *  because source-over compositing is associative - see {@link StyleLayerPart}. The replay
     *  is not a cache outcome of its own: it is unconditional work, which is why the cut is
     *  gated on the component resizing.
     */
    private boolean _paintNoises( Graphics2D g2d ) {
        final Size size = _noises.boxModel().size();
        if ( !size.hasPositiveWidth() || !size.hasPositiveHeight() )
            return false; // Nothing lifted out, or nowhere to put it.
        StyleRenderer.renderStyleOn(_layer, _noises, g2d);
        return true;
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

    /** Paints served entirely from cached images. A paint only counts when at least one part of
     *  the layer came out of a cache and no part had to be rendered, because a layer painted
     *  partly from a cache and partly by the renderer is, from the outside, a paint the renderer
     *  was invoked for. A part holding none of the layer's style neither paints nor counts. <br>
     *  A noise lifted out of the layer is the one exception: it is replayed on every paint, yet
     *  a paint replaying it still counts as a hit, because it is cheap by construction and
     *  counting it as a render would hide the very saving the cut exists to make. */
    int paintCacheHitCount() { return _paintCacheHitCount; }

    /** Paints which had to invoke the style renderer for at least one part of the layer, or
     *  which no cache took part in at all. */
    int paintCacheMissCount() { return _paintCacheMissCount; }
}
