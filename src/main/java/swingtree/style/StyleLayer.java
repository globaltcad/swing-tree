package swingtree.style;

import java.util.Objects;

final class StyleLayer implements Simplifiable<StyleLayer>
{
    static final NamedStyles<ShadowStyle>   _NO_SHADOWS   = NamedStyles.of(NamedStyle.of(StyleUtility.DEFAULT_KEY,ShadowStyle.none()));
    static final NamedStyles<PainterStyle>  _NO_PAINTERS  = NamedStyles.of(NamedStyle.of(StyleUtility.DEFAULT_KEY + "_" + PainterStyle.DEFAULT_LAYER.name(),PainterStyle.none()));
    static final NamedStyles<GradientStyle> _NO_GRADIENTS = NamedStyles.of(NamedStyle.of(StyleUtility.DEFAULT_KEY, GradientStyle.none()));
    static final NamedStyles<ImageStyle>    _NO_IMAGES    = NamedStyles.of(NamedStyle.of(StyleUtility.DEFAULT_KEY, ImageStyle.none()));

    private static final StyleLayer _EMPTY = new StyleLayer(
                                                    _NO_SHADOWS,
                                                    _NO_PAINTERS,
                                                    _NO_GRADIENTS,
                                                    _NO_IMAGES
                                                );

    static final StyleLayer empty() {
        return _EMPTY;
    }

    private final NamedStyles<ShadowStyle>   _shadows;
    private final NamedStyles<PainterStyle>  _painters;
    private final NamedStyles<GradientStyle> _gradients;
    private final NamedStyles<ImageStyle>    _images;


    static StyleLayer of(
        NamedStyles<ShadowStyle>   shadows,
        NamedStyles<PainterStyle>  painters,
        NamedStyles<GradientStyle> gradients,
        NamedStyles<ImageStyle>    images
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
        NamedStyles<ShadowStyle>   shadows,
        NamedStyles<PainterStyle>  painters,
        NamedStyles<GradientStyle> gradients,
        NamedStyles<ImageStyle>    images
    ) {
        _shadows   = shadows;
        _painters  = painters;
        _gradients = gradients;
        _images    = images;
    }


    NamedStyles<ShadowStyle>   shadows() {
        return _shadows;
    }
    NamedStyles<PainterStyle>  painters() {
        return _painters;
    }
    NamedStyles<GradientStyle> gradients() {
        return _gradients;
    }
    NamedStyles<ImageStyle>    images() {
        return _images;
    }

    StyleLayer withShadows( NamedStyles<ShadowStyle> shadows ) {
        return of(shadows, _painters, _gradients, _images);
    }
    StyleLayer withPainters( NamedStyles<PainterStyle> painters ) {
        return of(_shadows, painters, _gradients, _images);
    }
    StyleLayer withGradients( NamedStyles<GradientStyle> gradients ) {
        return of(_shadows, _painters, gradients, _images);
    }
    StyleLayer withImages( NamedStyles<ImageStyle> images ) {
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
        String painterString    = _painters.toString(StyleUtility.DEFAULT_KEY + "_" + PainterStyle.DEFAULT_LAYER.name(), "");
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
        NamedStyles<ShadowStyle>   simplifiedShadows   = _shadows.simplified();
        NamedStyles<PainterStyle>  simplifiedPainters  = _painters.simplified();
        NamedStyles<GradientStyle> simplifiedGradients = _gradients.simplified();
        NamedStyles<ImageStyle>    simplifiedImages    = _images.simplified();

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
