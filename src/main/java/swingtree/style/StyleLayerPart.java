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
 *  <b>But only sometimes, and that is the hard part.</b> Cutting is not free: a replayed noise
 *  costs a fill on <i>every</i> paint, whereas a noise baked into a cached image costs nothing
 *  on a cache hit. So the cut only pays for itself when it actually buys size independence -
 *  that is, when everything on <i>both</i> sides of the noise is
 *  {@code LayerPartCache._isStretchTileable}, and when the component size really changes. At an
 *  unchanged size a cut is a loss by construction, because several blits replace the single one
 *  a whole-layer cache hit would have done. Whoever wires the cut up must gate it on that, and
 *  must measure a real, busy UI rather than an isolated large noise - the two disagree sharply.
 *  <br>
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
 *  A layer without any noise in it is not split at all - it is a single {@link #WHOLE} part,
 *  which restricts to the configuration unchanged and so behaves exactly as it did before parts
 *  existed. <br>
 *  <br>
 *  <b>Status:</b> only {@link #WHOLE} is currently ever constructed (see {@link StyleLayerCache}),
 *  so no layer is split yet and painting is bit for bit what it always was. The other three
 *  constants exist so that the machinery around them - restriction, per part caching, per layer
 *  composition - can be built and verified before anything depends on it, and their decomposition
 *  is pinned as pixel identical to {@link #WHOLE} by {@code Style_Layer_Part_Decomposition_Spec}.
 *  Two things still have to be solved before a layer may actually be cut: the gate described
 *  above, and the fact that {@link LayerPartCache} has no way to express "never cache this part"
 *  - a {@link #NOISES} part handed to it today would be cached like any other, at full size,
 *  which is the very thing the cut is meant to avoid.
 */
enum StyleLayerPart
{
    /** The entire layer, used when there is no noise to split around. */
    WHOLE,
    /** Everything the renderer draws before the noises: fill, border, images and gradients. */
    UNDER_NOISE,
    /** The noises themselves, which are meant to be replayed on every paint rather than cached
     *  (relying on the noise tile cache one level down); note that nothing enforces that yet,
     *  see the status note on this enum. */
    NOISES,
    /** Everything the renderer draws after the noises: shadows, texts and painters. */
    OVER_NOISE;

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
        }
        throw new IllegalStateException("Unknown style layer part: " + this);
    }
}
