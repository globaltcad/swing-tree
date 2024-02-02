package swingtree.style;

import java.util.Objects;

final class StyleLayer implements Simplifiable<StyleLayer>
{
    static final NamedConfigs<ShadowConf> _NO_SHADOWS   = NamedConfigs.of(NamedConf.of(StyleUtility.DEFAULT_KEY, ShadowConf.none()));
    static final NamedConfigs<PainterConf> _NO_PAINTERS  = NamedConfigs.of(NamedConf.of(StyleUtility.DEFAULT_KEY, PainterConf.none()));
    static final NamedConfigs<GradientConf> _NO_GRADIENTS = NamedConfigs.of(NamedConf.of(StyleUtility.DEFAULT_KEY, GradientConf.none()));
    static final NamedConfigs<ImageConf> _NO_IMAGES    = NamedConfigs.of(NamedConf.of(StyleUtility.DEFAULT_KEY, ImageConf.none()));

    private static final StyleLayer _EMPTY = new StyleLayer(
                                                    _NO_SHADOWS,
                                                    _NO_PAINTERS,
                                                    _NO_GRADIENTS,
                                                    _NO_IMAGES
                                                );

    static final StyleLayer empty() {
        return _EMPTY;
    }

    private final NamedConfigs<ShadowConf> _shadows;
    private final NamedConfigs<PainterConf> _painters;
    private final NamedConfigs<GradientConf> _gradients;
    private final NamedConfigs<ImageConf> _images;


    static StyleLayer of(
        NamedConfigs<ShadowConf> shadows,
        NamedConfigs<PainterConf> painters,
        NamedConfigs<GradientConf> gradients,
        NamedConfigs<ImageConf> images
    ) {
        StyleLayer empty = StyleLayer.empty();
        if (
            shadows  .equals(_NO_SHADOWS  ) &&
            painters .equals(_NO_PAINTERS ) &&
            gradients.equals(_NO_GRADIENTS) &&
            images   .equals(_NO_IMAGES   )
        )
            return empty;

        return new StyleLayer(shadows, painters, gradients, images);
    }

    StyleLayer(
        NamedConfigs<ShadowConf> shadows,
        NamedConfigs<PainterConf> painters,
        NamedConfigs<GradientConf> gradients,
        NamedConfigs<ImageConf> images
    ) {
        _shadows   = shadows;
        _painters  = painters;
        _gradients = gradients;
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
    NamedConfigs<ImageConf> images() {
        return _images;
    }

    StyleLayer withShadows( NamedConfigs<ShadowConf> shadows ) {
        return of(shadows, _painters, _gradients, _images);
    }
    StyleLayer withPainters( NamedConfigs<PainterConf> painters ) {
        return of(_shadows, painters, _gradients, _images);
    }
    StyleLayer withGradients( NamedConfigs<GradientConf> gradients ) {
        return of(_shadows, _painters, gradients, _images);
    }
    StyleLayer withImages( NamedConfigs<ImageConf> images ) {
        return of(_shadows, _painters, _gradients, images);
    }

    StyleLayer _scale( double scale ) {
        return of(
                    _shadows.mapStyles( s -> s._scale(scale) ),
                    _painters, // This is the users problem...
                    _gradients, // Scaling does not make sense
                    _images.mapStyles( s -> s._scale(scale) )
                );
    }

    boolean hasEqualShadowsAs( StyleLayer otherStyle ) {
        return Objects.equals(_shadows, otherStyle._shadows);
    }

    boolean hasEqualPaintersAs( StyleLayer otherStyle ) {
        return Objects.equals(_painters, otherStyle._painters);
    }

    boolean hasEqualGradientsAs( StyleLayer otherStyle ) {
        return Objects.equals(_gradients, otherStyle._gradients);
    }

    boolean hasEqualImagesAs( StyleLayer otherStyle ) {
        return Objects.equals(_images, otherStyle._images);
    }

    public boolean hasPainters() {
        return !_painters.equals(_NO_PAINTERS);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_shadows, _painters, _gradients, _images);
    }

    @Override
    public boolean equals( Object other ) {
        if ( other == null )
            return false;
        if ( other.getClass() != this.getClass() )
            return false;

        StyleLayer otherLayer = (StyleLayer) other;
        return
            Objects.equals(this._shadows,   otherLayer._shadows)   &&
            Objects.equals(this._painters,  otherLayer._painters)  &&
            Objects.equals(this._gradients, otherLayer._gradients) &&
            Objects.equals(this._images,    otherLayer._images);
    }

    @Override
    public String toString() {
        if ( this == _EMPTY )
            return "StyleLayer[EMPTY]";
        String shadowString     = _shadows.toString(StyleUtility.DEFAULT_KEY, "");
        String painterString    = _painters.toString(StyleUtility.DEFAULT_KEY, "");
        String gradientString   = _gradients.toString(StyleUtility.DEFAULT_KEY, "");
        String imagesString     = _images.toString(StyleUtility.DEFAULT_KEY, "");
        return this.getClass().getSimpleName() + "[" +
                    "shadows="   + shadowString   + ", " +
                    "painters="  + painterString  + ", " +
                    "gradients=" + gradientString + ", " +
                    "images="    + imagesString   +
                "]";
    }

    @Override
    public StyleLayer simplified() {
        NamedConfigs<ShadowConf> simplifiedShadows   = _shadows.simplified();
        NamedConfigs<PainterConf> simplifiedPainters  = _painters.simplified();
        NamedConfigs<GradientConf> simplifiedGradients = _gradients.simplified();
        NamedConfigs<ImageConf> simplifiedImages    = _images.simplified();

        if (
            simplifiedShadows   == _shadows   &&
            simplifiedPainters  == _painters  &&
            simplifiedGradients == _gradients &&
            simplifiedImages    == _images
        )
            return this;

        simplifiedShadows   = ( simplifiedShadows.equals(_NO_SHADOWS)     ? _NO_SHADOWS   : simplifiedShadows );
        simplifiedPainters  = ( simplifiedPainters.equals(_NO_PAINTERS)   ? _NO_PAINTERS  : simplifiedPainters );
        simplifiedGradients = ( simplifiedGradients.equals(_NO_GRADIENTS) ? _NO_GRADIENTS : simplifiedGradients );
        simplifiedImages    = ( simplifiedImages.equals(_NO_IMAGES)       ? _NO_IMAGES    : simplifiedImages );

        if (
            simplifiedShadows   == _NO_SHADOWS   &&
            simplifiedPainters  == _NO_PAINTERS  &&
            simplifiedGradients == _NO_GRADIENTS &&
            simplifiedImages    == _NO_IMAGES
        )
            return _EMPTY;

        return of(
                    simplifiedShadows,
                    simplifiedPainters,
                    simplifiedGradients,
                    simplifiedImages
                );
    }
}
