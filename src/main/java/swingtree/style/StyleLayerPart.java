package swingtree.style;

import swingtree.UI;

/**
 *  The pieces a single {@link UI.Layer} is rendered, and cached, in. <br>
 *  <br>
 *  A layer is ordinarily one rasterization: {@link StyleRenderer#renderStyleOn(UI.Layer,
 *  LayerRenderConf, java.awt.Graphics2D)} draws the whole of it into one image which
 *  {@link LayerPartCache} then blits. That breaks down when a layer mixes style which caches in
 *  incompatible ways. The case this exists for is <b>noise</b>: its pixels vary per pixel
 *  position, so unlike a flat fill, a border or a shadow it can never be stored as the small
 *  size independent exemplar that lets a component resize without re-rendering - yet noise is
 *  also the one kind of style that is nearly free to simply re-render, because
 *  {@code StyleRenderer.NoisePaintCache} already keeps it as tiles laid out in a size
 *  independent noise space. So a layer with a noise in it <i>can</i> be better off caching
 *  everything <i>except</i> the noise and replaying the noise on top, than not being cacheable
 *  at all. <br>
 *  <br>
 *  <b>But only while the component is resizing</b>, because a replayed noise costs a fill on
 *  <i>every</i> paint whereas a noise baked into a cached image costs nothing on a cache hit.
 *  {@link StyleLayerCache} owns that decision; anything changing it must measure a real, busy
 *  UI rather than an isolated large noise, because the two disagree sharply. <br>
 *  <br>
 *  The second case is a <b>user painter</b>, and its economics are the opposite in every
 *  respect. A painter is arbitrary user code, so its output cannot be cached at all and a layer
 *  carrying one is refused by {@link LayerPartCache} outright - shadows, gradients and all,
 *  re-rendered at full size on every single paint. Cutting the painters out lets everything
 *  else on that layer be cached normally, and the painters are then replayed exactly as often
 *  as they ran before. There is therefore nothing to trade off and no resizing condition: the
 *  cut is a strict improvement whenever the layer holds anything else worth caching. <br>
 *  <br>
 *  Splitting is possible at all because the renderer draws by kind in a fixed order - fill,
 *  border, images, gradients, <b>noises</b>, shadows, texts, painters - so the noises are
 *  always one contiguous block and the layer always cuts into exactly the same three pieces.
 *  A part is expressed by <i>restricting the configuration</i> rather than by running a
 *  different renderer: the renderer draws whatever the configuration contains, so handing it
 *  a configuration with the other kinds emptied out draws exactly that part and nothing else.
 *  The three parts therefore recompose to the original by construction, and because source-over
 *  compositing is associative, drawing them one after another is pixel identical to drawing the
 *  whole layer in one go. <br>
 *  <br>
 *  An uncut layer is a single {@link #WHOLE} part, which restricts to the configuration
 *  unchanged and so behaves exactly as it did before parts existed. A cut one is
 *  {@link #UNDER_NOISE} and {@link #OVER_NOISE} in a {@link LayerPartCache} each, with
 *  {@link #NOISES} replayed straight onto the destination in between - never cached, because a
 *  cached noise would be an image at the component's real size, re-rendered at every new size,
 *  which is exactly what the cut exists to avoid. <br>
 *  <br>
 *  That the cut paints what the whole does is pinned by {@code Stretch_Tiling_Equivalence_Spec}.
 */
enum StyleLayerPart
{
    /** The entire layer, used when there is nothing to split around. */
    WHOLE,
    /** Everything the renderer draws before the noises: fill, border, images and gradients. */
    UNDER_NOISE,
    /** The noises themselves, replayed on every paint rather than cached - which is cheap
     *  because the noise tile cache one level down keeps them in a size independent noise
     *  space. {@link StyleLayerCache} never hands this part to a {@link LayerPartCache}. */
    NOISES,
    /** Everything the renderer draws after the noises: shadows, texts and painters. */
    OVER_NOISE,
    /** Everything except the painters, which is the whole layer minus the one kind of style
     *  that can never be cached - see {@link #PAINTERS}. */
    UNDER_PAINTERS,
    /** The user painters, replayed straight onto the destination on every paint. Unlike
     *  {@link #NOISES} this is not cheap - it is arbitrary user code - but it is drawn exactly
     *  as often as it would have been without the cut, because a layer carrying it was never
     *  cacheable in the first place. {@link StyleLayerCache} never hands this part to a
     *  {@link LayerPartCache}. */
    PAINTERS;

    /**
     *  Narrows the supplied render configuration down to just this part of the layer, by
     *  emptying out the style kinds which belong to the other parts. Handing the result to
     *  the style renderer draws this part alone.
     */
    LayerRenderConf restrict( LayerRenderConf conf ) {
        switch ( this ) {
            case WHOLE:
                return conf;
            case UNDER_NOISE:
                return conf.withLayer(
                            conf.layer()
                                .withNoises(StyleConfLayer._NO_NOISES)
                                .withShadows(StyleConfLayer._NO_SHADOWS)
                                .withTexts(StyleConfLayer._NO_TEXTS)
                                .withPainters(StyleConfLayer._NO_PAINTERS)
                        );
            case NOISES:
                return conf.withBaseColors(BaseColorConf.none())
                           .withLayer(
                                conf.layer()
                                    .withImages(StyleConfLayer._NO_IMAGES)
                                    .withGradients(StyleConfLayer._NO_GRADIENTS)
                                    .withShadows(StyleConfLayer._NO_SHADOWS)
                                    .withTexts(StyleConfLayer._NO_TEXTS)
                                    .withPainters(StyleConfLayer._NO_PAINTERS)
                           );
            case OVER_NOISE:
                return conf.withBaseColors(BaseColorConf.none())
                           .withLayer(
                                conf.layer()
                                    .withImages(StyleConfLayer._NO_IMAGES)
                                    .withGradients(StyleConfLayer._NO_GRADIENTS)
                                    .withNoises(StyleConfLayer._NO_NOISES)
                           );
            case UNDER_PAINTERS:
                return conf.withLayer( conf.layer().withPainters(_paintersBefore(conf, true)) );
            case PAINTERS:
                return conf.withBaseColors(BaseColorConf.none())
                           .withLayer(
                                conf.layer()
                                    .withImages(StyleConfLayer._NO_IMAGES)
                                    .withGradients(StyleConfLayer._NO_GRADIENTS)
                                    .withNoises(StyleConfLayer._NO_NOISES)
                                    .withShadows(StyleConfLayer._NO_SHADOWS)
                                    .withTexts(StyleConfLayer._NO_TEXTS)
                                    .withPainters(_paintersBefore(conf, false))
                           );
        }
        throw new IllegalStateException("Unknown style layer part: " + this);
    }

    /**
     *  The painters of the supplied configuration, with everything on the other side of the
     *  painter cut replaced by {@link PainterConf#none()} - which draws nothing and, unlike an
     *  arbitrary painter, declares itself cacheable, so a part left with only these is a part
     *  the cache will take. <br>
     *  <br>
     *  <b>Where the cut falls, and why there rather than "all the cacheable ones".</b>
     *  A painter which promises to be cacheable (see {@link swingtree.api.Painter#of(Object,
     *  swingtree.api.Painter)}) can perfectly well be baked into the cached image, and it
     *  should be - the promise exists to be taken up. But the renderer runs painters in the
     *  order of their names, and painters on one layer may overlap, so <i>which</i> of them are
     *  moved into the image decides what the component looks like. This therefore takes the
     *  longest <b>prefix</b> in that order which is entirely cacheable, rather than every
     *  cacheable painter wherever it sits: a prefix keeps every painter in its original
     *  position relative to every other, so the cut cannot change a single pixel. Gathering all
     *  cacheable painters instead would hoist those that sit behind an uncacheable one in front
     *  of it, which is a visual change - a silent one, in code the library never sees.
     *
     * @param conf The configuration whose painters are to be partitioned.
     * @param wantPrefix True for the cacheable prefix (what is baked into the image),
     *                   false for the remainder (what is replayed on every paint).
     */
    private static NamedConfigs<PainterConf> _paintersBefore( LayerRenderConf conf, boolean wantPrefix ) {
        final NamedConfigs<PainterConf> painters = conf.layer().painters();
        final @org.jspecify.annotations.Nullable String cut = _firstUncacheablePainterName(painters);
        if ( cut == null )
            return ( wantPrefix ? painters : StyleConfLayer._NO_PAINTERS ); // Nothing to replay.
        return painters.mapNamedStyles( named -> {
                    boolean isInPrefix = named.name().compareTo(cut) < 0;
                    return ( isInPrefix == wantPrefix
                                ? named
                                : NamedConf.of(named.name(), PainterConf.none()) );
                });
    }

    /** The name of the first painter, in the order the renderer runs them, which cannot be
     *  cached - or null when every one of them can. */
    private static @org.jspecify.annotations.Nullable String _firstUncacheablePainterName( NamedConfigs<PainterConf> painters ) {
        String first = null;
        for ( NamedConf<PainterConf> named : painters.namedStylesStream().toArray(NamedConf[]::new) ) {
            if ( named.style().painter().canBeCached() )
                continue;
            if ( first == null || named.name().compareTo(first) < 0 )
                first = named.name();
        }
        return first;
    }
}
