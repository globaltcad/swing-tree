package swingtree.style;

import org.jspecify.annotations.Nullable;
import swingtree.UI;

import java.util.Comparator;

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
 *      Checkout {@link StyleRenderer#renderStyleOn(UI.Layer, LayerRenderConf, java.awt.Graphics2D)}
 *      to see the above list unfold...
 *  </i>
 */
enum LayerRenderConfPartitions
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
     *  {@link #NOISES} this is not cheap - it is arbitrary user code  */
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

    private static NamedConfigs<PainterConf> _paintersBefore( LayerRenderConf conf, boolean wantPrefix ) {
        final NamedConfigs<PainterConf> painters = conf.layer().painters();
        final @Nullable String cut = _firstUncacheablePainterName(painters);
        if ( cut == null )
            return ( wantPrefix ? painters : StyleConfLayer._NO_PAINTERS ); // Nothing to replay.
        return painters.namedStylesStream()
                       .filter( named -> ( named.name().compareTo(cut) < 0 ) == wantPrefix )
                       .filter( named -> !named.style().equals(PainterConf.none()) )
                       .reduce( StyleConfLayer._NO_PAINTERS,
                                ( keptSoFar, named ) -> keptSoFar.withNamedStyle(named.name(), named.style()),
                                ( a, b ) -> a ); // Never called: the stream is sequential.
    }

    private static @Nullable String _firstUncacheablePainterName( NamedConfigs<PainterConf> painters ) {
        return painters.namedStylesStream()
                       .filter( named -> !named.style().painter().canBeCached() )
                       .map( NamedConf::name )
                       .min( Comparator.naturalOrder() )
                       .orElse(null);
    }
}
