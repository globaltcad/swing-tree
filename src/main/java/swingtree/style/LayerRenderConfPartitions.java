package swingtree.style;

import swingtree.UI;

import java.awt.*;

/**
 *  This enum specifies multiple ways in which the {@link LayerRenderConf} of a {@link UI.Layer}
 *  can be "narrowed down" or "restricted" to a new {@link LayerRenderConf} instance.
 *  This narrowed down version of a layer render configuration can then be used to
 *  feed the different {@link LayerPartitionCache}s of a {@link StyleLayerCache}.
 *  The purpose of this is to created cached renderings from simplified style
 *  configurations in order to improve cache hit rate and robustness.<br>
 *  <br>
 *  <b>A style consists of, and is rendered in, the following order:</b>
 *  <ul>
 *      <li>1. Foundation</li>
 *      <li>2. Border</li>
 *      <li>3. Images</li>
 *      <li>4. Gradients</li>
 *      <li>5. Noises</li>
 *      <li>6. Shadows</li>
 *      <li>7. Text</li>
 *      <li>8. Painters</li>
 *  </ul>
 *  <i>
 *      Checkout {@link StyleRenderer#renderStyleOn(UI.Layer, LayerRenderConf, Graphics2D)}
 *      to see the above list unfold...
 *  </i>
 */
enum LayerRenderConfPartitions
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
