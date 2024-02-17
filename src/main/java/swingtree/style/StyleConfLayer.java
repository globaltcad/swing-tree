package swingtree.style;

import java.util.Objects;

final class StyleConfLayer implements Simplifiable<StyleConfLayer>
{
    static final NamedConfigs<ShadowConf> _NO_SHADOWS   = NamedConfigs.of(NamedConf.of(StyleUtil.DEFAULT_KEY, ShadowConf.none()));
    static final NamedConfigs<PainterConf> _NO_PAINTERS  = NamedConfigs.of(NamedConf.of(StyleUtil.DEFAULT_KEY, PainterConf.none()));
    static final NamedConfigs<GradientConf> _NO_GRADIENTS = NamedConfigs.of(NamedConf.of(StyleUtil.DEFAULT_KEY, GradientConf.none()));
    static final NamedConfigs<NoiseConf> _NO_NOISES   = NamedConfigs.of(NamedConf.of(StyleUtil.DEFAULT_KEY, NoiseConf.none()));
    static final NamedConfigs<ImageConf> _NO_IMAGES    = NamedConfigs.of(NamedConf.of(StyleUtil.DEFAULT_KEY, ImageConf.none()));

    private static final StyleConfLayer _EMPTY = new StyleConfLayer(
                                                    _NO_SHADOWS,
                                                    _NO_PAINTERS,
                                                    _NO_GRADIENTS,
                                                    _NO_NOISES,
                                                    _NO_IMAGES
                                                );

    static final StyleConfLayer empty() {
        return _EMPTY;
    }

    private final NamedConfigs<ShadowConf> _shadows;
    private final NamedConfigs<PainterConf> _painters;
    private final NamedConfigs<GradientConf> _gradients;
    private final NamedConfigs<NoiseConf> _noises;
    private final NamedConfigs<ImageConf> _images;


    static StyleConfLayer of(
        NamedConfigs<ShadowConf> shadows,
        NamedConfigs<PainterConf> painters,
        NamedConfigs<GradientConf> gradients,
        NamedConfigs<NoiseConf> noises,
        NamedConfigs<ImageConf> images
    ) {
        StyleConfLayer empty = StyleConfLayer.empty();
        if (
            shadows  .equals(_NO_SHADOWS  ) &&
            painters .equals(_NO_PAINTERS ) &&
            gradients.equals(_NO_GRADIENTS) &&
            noises   .equals(_NO_NOISES   ) &&
            images   .equals(_NO_IMAGES   )
        )
            return empty;

        return new StyleConfLayer(shadows, painters, gradients, noises, images);
    }

    StyleConfLayer(
        NamedConfigs<ShadowConf> shadows,
        NamedConfigs<PainterConf> painters,
        NamedConfigs<GradientConf> gradients,
        NamedConfigs<NoiseConf> noises,
        NamedConfigs<ImageConf> images
    ) {
        _shadows   = shadows;
        _painters  = painters;
        _gradients = gradients;
        _noises    = noises;
        _images    = images;
    }


    NamedConfigs<ShadowConf> shadows() {
        return _shadows;
    }
    NamedConfigs<PainterConf> painters() {
        return _painters;
    }
    NamedConfigs<GradientConf> gradients() {
        return _gradients;
    }
    NamedConfigs<NoiseConf> noises() {
        return _noises;
    }
    NamedConfigs<ImageConf> images() {
        return _images;
    }

    StyleConfLayer withShadows(NamedConfigs<ShadowConf> shadows ) {
        return of(shadows, _painters, _gradients, _noises, _images);
    }
    StyleConfLayer withPainters(NamedConfigs<PainterConf> painters ) {
        return of(_shadows, painters, _gradients, _noises, _images);
    }
    StyleConfLayer withGradients(NamedConfigs<GradientConf> gradients ) {
        return of(_shadows, _painters, gradients, _noises, _images);
    }
    StyleConfLayer withNoises(NamedConfigs<NoiseConf> noises ) {
        return of(_shadows, _painters, _gradients, noises, _images);
    }
    StyleConfLayer withImages(NamedConfigs<ImageConf> images ) {
        return of(_shadows, _painters, _gradients, _noises, images);
    }

    StyleConfLayer _scale(double scale ) {
        return of(
                    _shadows.mapStyles( s -> s._scale(scale) ),
                    _painters, // This is the users problem...
                    _gradients.mapStyles( s -> s._scale(scale) ),
                    _noises.mapStyles( s -> s._scale(scale) ),
                    _images.mapStyles( s -> s._scale(scale) )
                );
    }

    boolean hasEqualShadowsAs( StyleConfLayer otherStyle ) {
        return Objects.equals(_shadows, otherStyle._shadows);
    }

    boolean hasEqualPaintersAs( StyleConfLayer otherStyle ) {
        return Objects.equals(_painters, otherStyle._painters);
    }

    boolean hasEqualGradientsAs( StyleConfLayer otherStyle ) {
        return Objects.equals(_gradients, otherStyle._gradients);
    }

    boolean hasEqualNoisesAs( StyleConfLayer otherStyle ) {
        return Objects.equals(_noises, otherStyle._noises);
    }

    boolean hasEqualImagesAs( StyleConfLayer otherStyle ) {
        return Objects.equals(_images, otherStyle._images);
    }

    public boolean hasPainters() {
        return !_painters.equals(_NO_PAINTERS);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_shadows, _painters, _gradients, _noises, _images);
    }

    @Override
    public boolean equals( Object other ) {
        if ( other == null )
            return false;
        if ( other.getClass() != this.getClass() )
            return false;

        StyleConfLayer otherLayer = (StyleConfLayer) other;
        return
            Objects.equals(this._shadows,   otherLayer._shadows)   &&
            Objects.equals(this._painters,  otherLayer._painters)  &&
            Objects.equals(this._gradients, otherLayer._gradients) &&
            Objects.equals(this._noises,    otherLayer._noises)    &&
            Objects.equals(this._images,    otherLayer._images);
    }

    @Override
    public String toString() {
        if ( this == _EMPTY )
            return "StyleLayer[EMPTY]";
        String shadowString     = _shadows.toString(StyleUtil.DEFAULT_KEY, "");
        String painterString    = _painters.toString(StyleUtil.DEFAULT_KEY, "");
        String gradientString   = _gradients.toString(StyleUtil.DEFAULT_KEY, "");
        String noiseString      = _noises.toString(StyleUtil.DEFAULT_KEY, "");
        String imagesString     = _images.toString(StyleUtil.DEFAULT_KEY, "");
        return this.getClass().getSimpleName() + "[" +
                    "shadows="   + shadowString   + ", " +
                    "painters="  + painterString  + ", " +
                    "gradients=" + gradientString + ", " +
                    "noises="    + noiseString    + ", " +
                    "images="    + imagesString   +
                "]";
    }

    @Override
    public StyleConfLayer simplified() {
        NamedConfigs<ShadowConf> simplifiedShadows   = _shadows.simplified();
        NamedConfigs<PainterConf> simplifiedPainters  = _painters.simplified();
        NamedConfigs<GradientConf> simplifiedGradients = _gradients.simplified();
        NamedConfigs<NoiseConf> simplifiedNoises    = _noises.simplified();
        NamedConfigs<ImageConf> simplifiedImages    = _images.simplified();

        if (
            simplifiedShadows   == _shadows   &&
            simplifiedPainters  == _painters  &&
            simplifiedGradients == _gradients &&
            simplifiedNoises    == _noises    &&
            simplifiedImages    == _images
        )
            return this;

        simplifiedShadows   = ( simplifiedShadows.equals(_NO_SHADOWS)     ? _NO_SHADOWS   : simplifiedShadows );
        simplifiedPainters  = ( simplifiedPainters.equals(_NO_PAINTERS)   ? _NO_PAINTERS  : simplifiedPainters );
        simplifiedGradients = ( simplifiedGradients.equals(_NO_GRADIENTS) ? _NO_GRADIENTS : simplifiedGradients );
        simplifiedNoises    = ( simplifiedNoises.equals(_NO_NOISES)       ? _NO_NOISES    : simplifiedNoises );
        simplifiedImages    = ( simplifiedImages.equals(_NO_IMAGES)       ? _NO_IMAGES    : simplifiedImages );

        if (
            simplifiedShadows   == _NO_SHADOWS   &&
            simplifiedPainters  == _NO_PAINTERS  &&
            simplifiedGradients == _NO_GRADIENTS &&
            simplifiedNoises    == _NO_NOISES    &&
            simplifiedImages    == _NO_IMAGES
        )
            return _EMPTY;

        return of(
                    simplifiedShadows,
                    simplifiedPainters,
                    simplifiedGradients,
                    simplifiedNoises,
                    simplifiedImages
                );
    }
}
