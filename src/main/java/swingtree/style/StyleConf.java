package swingtree.style;

import swingtree.UI;
import swingtree.api.Painter;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *  An immutable config container with cloning based update methods designed
 *  for functional {@link javax.swing.JComponent} styling.
 *  The styling in SwingTree is completely functional, meaning that changing a property
 *  of a {@link StyleConf} instance will always return a new {@link StyleConf} instance with the
 *  updated property.
 *  <p>
 *  Consider the following example demonstrating how a {@link javax.swing.JPanel} is styled through the SwingTree
 *  style API, which consists of a functional {@link swingtree.api.Styler} lambda that processes a
 *  {@link ComponentStyleDelegate} instance that internally assembles a {@link StyleConf} object:
 *  <pre>{@code
 *  panel(FILL)
 *  .withStyle( it -> it
 *      .foundationColor(new Color(26,191,230))
 *      .backgroundColor(new Color(255,255,255))
 *      .paddingTop(30)
 *      .paddingLeft(35)
 *      .paddingRight(35)
 *      .paddingBottom(30)
 *      .borderRadius(25, 25)
 *      .borderWidth(3)
 *      .borderColor(new Color(0,102,255))
 *      .shadowColor(new Color(64,64,64))
 *      .shadowBlurRadius(6)
 *      .shadowSpreadRadius(5)
 *      .shadowInset(false)
 *  )
 *  }</pre>
 *
 *  This design is inspired by the CSS styling language and the use of immutable objects
 *  is a key feature of the SwingTree API which makes it possible to safely compose
 *  {@link swingtree.api.Styler} lambdas into a complex style pipeline
 *  without having to worry about side effects.
 *  See {@link swingtree.style.StyleSheet} for more information about
 *  how this composition of styles is achieved in practice.
 */
public final class StyleConf
{
    private static final StyleConf _NONE = new StyleConf(
                                            LayoutStyle.none(),
                                            BorderConf.none(),
                                            BaseConf.none(),
                                            FontStyle.none(),
                                            DimensionalityStyle.none(),
                                            StyleLayers.empty(),
                                            NamedStyles.empty()
                                        );

    /**
     * @return The default style instance, representing the absence of a style.
     */
    public static StyleConf none() { return _NONE; }

    static StyleConf of(
        LayoutStyle         layout,
        BorderConf border,
        BaseConf base,
        FontStyle           font,
        DimensionalityStyle dimensionality,
        StyleLayers         layers,
        NamedStyles<String> properties
    ) {
        if (
            layout         == _NONE._layout &&
            border         == _NONE._border &&
            base           == _NONE._base &&
            font           == _NONE._font &&
            dimensionality == _NONE._dimensionality &&
            layers         == _NONE._layers &&
            properties     == _NONE._properties
        )
            return _NONE;
        else
            return new StyleConf(layout, border, base, font, dimensionality, layers, properties);
    }


    private final LayoutStyle                _layout;
    private final BorderConf _border;
    private final BaseConf _base;
    private final FontStyle                  _font;
    private final DimensionalityStyle        _dimensionality;
    private final StyleLayers                _layers;
    private final NamedStyles<String>        _properties;

    private StyleConf(
        LayoutStyle         layout,
        BorderConf border,
        BaseConf base,
        FontStyle           font,
        DimensionalityStyle dimensionality,
        StyleLayers         layers,
        NamedStyles<String> properties
    ) {
        _layout         = Objects.requireNonNull(layout);
        _border         = Objects.requireNonNull(border);
        _base           = Objects.requireNonNull(base);
        _font           = Objects.requireNonNull(font);
        _dimensionality = Objects.requireNonNull(dimensionality);
        _layers         = Objects.requireNonNull(layers);
        _properties     = Objects.requireNonNull(properties);
    }

    public Optional<Object> layoutConstraint() { return _layout.constraint(); }

    LayoutStyle layout() { return _layout; }

    Outline padding() { return _border.padding(); }

    Outline margin() { return _border.margin(); }

    BorderConf border() { return _border; }

    BaseConf base() { return _base; }


    DimensionalityStyle dimensionality() { return _dimensionality; }


    /**
     * @return The default shadow style.
     */
    public ShadowStyle shadow() {
        return _layers.get(ShadowStyle.DEFAULT_LAYER)
                        .shadows()
                        .get(StyleUtility.DEFAULT_KEY);
    }

    /**
     * @param layer The layer to retrieve the shadow style from.
     * @param shadowName The name of the shadow style to retrieve.
     * @return The shadow style with the provided name.
     */
    public ShadowStyle shadow( UI.Layer layer, String shadowName ) {
        Objects.requireNonNull(shadowName);
        return _layers.get(layer).shadows().get(shadowName);
    }

    /**
     * @return An unmodifiable list of all shadow styles sorted by their names in ascending alphabetical order.
     */
    List<ShadowStyle> shadows( UI.Layer layer ) {
        return Collections.unmodifiableList(
                        _layers.get(layer)
                        .shadows()
                        .namedStyles()
                        .stream()
                        .sorted(Comparator.comparing(NamedStyle::name))
                        .map(NamedStyle::style)
                        .collect(Collectors.toList())
                    );
    }

    NamedStyles<ShadowStyle> shadowsMap(UI.Layer layer) {
        return _layers.get(layer).shadows();
    }

    boolean hasVisibleShadows() {
        return Arrays.stream(UI.Layer.values()).anyMatch(this::hasVisibleShadows);
    }

    boolean hasVisibleShadows(UI.Layer layer) {
        return _layers.get(layer)
                .shadows()
                .stylesStream()
                .anyMatch(s -> s.color().isPresent() && s.color().get().getAlpha() > 0 );
    }

    public FontStyle font() { return _font; }

    /**
     * @return An unmodifiable list of painters sorted by their names in ascending alphabetical order.
     */
    List<PainterStyle> painters( UI.Layer layer ) {
        return Collections.unmodifiableList(
                            new ArrayList<>(_layers.get(layer)
                                .painters()
                                .sortedByNamesAndFilteredBy()
                            )
                        );
    }

    StyleConf painter(UI.Layer layer, UI.ComponentArea area, String painterName, Painter painter ) {
        Objects.requireNonNull(painterName);
        Objects.requireNonNull(painter);
        // We clone the painter map:
        NamedStyles<PainterStyle> newPainters = _layers.get(layer)
                                                    .painters()
                                                    .withNamedStyle(
                                                        painterName, // Existing painters are overwritten if they have the same name.
                                                        PainterStyle.of(painter, area)
                                                    ); 
        return _withPainters(layer, newPainters);
    }

    /**
     *  Returns a new {@link StyleConf} instance which only contains style information relevant
     *  for rendering the provided {@link UI.Layer}. Style information on other layers is discarded.
     * @param layer The layer to retain.
     * @return A new {@link StyleConf} instance which only contains style information relevant to the provided {@link UI.Layer}.
     */
    public StyleConf onlyRetainingRenderCacheRelevantConfForLayer(UI.Layer layer ) {
        return StyleConf.of(
                    LayoutStyle.none(),
                    ( layer == UI.Layer.BORDER     ? _border : _border.withColor(null) ),
                    ( layer == UI.Layer.BACKGROUND ? _base   : BaseConf.none() ),
                    FontStyle.none(),
                    DimensionalityStyle.none(),
                    _layers.onlyRetainingAsUnnamedLayer(layer),
                    NamedStyles.empty()
                );
    }

    boolean hasPaintersOnLayer(UI.Layer layer ) {
        return _layers.get(layer).painters().stylesStream().anyMatch(p -> !Painter.none().equals(p.painter()));
    }

    boolean hasImagesOnLayer(UI.Layer layer ) {
        return _layers.get(layer).images().stylesStream().anyMatch(i -> i.image().isPresent() || i.primer().isPresent());
    }

    List<GradientStyle> gradients( UI.Layer layer ) {
        return _layers.get(layer).gradients().sortedByNamesAndFilteredBy();
    }

    boolean hasCustomGradients() {
        boolean hasCustomGradients = false;
        for ( UI.Layer layer : UI.Layer.values() ) {
            if ( hasCustomGradients(layer) ) {
                hasCustomGradients = true;
                break;
            }
        }
        return hasCustomGradients;
    }

    boolean hasCustomGradients( UI.Layer layer ) {
        NamedStyles<GradientStyle> gradients = _layers.get(layer).gradients();
        return !( gradients.size() == 1 && GradientStyle.none().equals(gradients.get(StyleUtility.DEFAULT_KEY)) );
    }

    boolean hasVisibleGradientsOnLayer( UI.Layer layer ) {
        List<GradientStyle> gradients = gradients(layer);
        if ( gradients.isEmpty() ) return false;
        return gradients.stream().anyMatch( s -> s.colors().length > 0 );
    }

    List<UI.ComponentArea> gradientCoveredAreas() {
        return gradientCoveredAreas(UI.Layer.values());
    }

    List<UI.ComponentArea> gradientCoveredAreas(UI.Layer... layers) {
        return Arrays.stream(layers)
                .map(_layers::get)
                .map(StyleLayer::gradients)
                .flatMap( g -> g
                    .stylesStream()
                    .map( grad -> grad.isOpaque() ? grad.area() : null )
                    .filter(Objects::nonNull)
                )
                .distinct()
                .collect(Collectors.toList());
    }

    public StyleConf foundationColor(Color color ) { return _withBase(base().foundationColor(color)); }

    public StyleConf backgroundColor(Color color ) { return _withBase(base().backgroundColor(color)); }

    StyleConf _withLayout(LayoutStyle layout ) {
        if ( layout == _layout )
            return this;

        return StyleConf.of(layout, _border, _base, _font, _dimensionality, _layers, _properties);
    }

    StyleConf _withBorder(BorderConf border ) {
        if ( border == _border )
            return this;

        return StyleConf.of(_layout, border, _base, _font, _dimensionality, _layers, _properties);
    }

    StyleConf _withBase(BaseConf background ) {
        if ( background == _base )
            return this;

        return StyleConf.of(_layout, _border, background, _font, _dimensionality, _layers, _properties);
    }

    StyleConf _withFont(FontStyle font ) {
        if ( font == _font )
            return this;

        return StyleConf.of(_layout, _border, _base, font, _dimensionality, _layers, _properties);
    }

    StyleConf _withDimensionality(DimensionalityStyle dimensionality ) {
        if ( dimensionality == _dimensionality )
            return this;

        return StyleConf.of(_layout, _border, _base, _font, dimensionality, _layers, _properties);
    }

    StyleConf _withShadow(UI.Layer layer, NamedStyles<ShadowStyle> shadows ) {
        return StyleConf.of(_layout, _border, _base, _font, _dimensionality, _layers.with(layer, _layers.get(layer).withShadows(shadows)), _properties);
    }

    StyleConf _withProperties(NamedStyles<String> properties ) {
        if ( properties == _properties )
            return this;

        return StyleConf.of(_layout, _border, _base, _font, _dimensionality, _layers, properties);
    }

    StyleConf _withShadow(UI.Layer layer, Function<ShadowStyle, ShadowStyle> styler ) {
        // A new map is created where all the styler is applied to all the values:
        NamedStyles<ShadowStyle> styledShadows = _layers.get(layer).shadows().mapStyles(styler::apply);
        return _withShadow(layer, styledShadows);
    }

    StyleConf _withShadow(Function<ShadowStyle, ShadowStyle> styler ) {
        return _withLayers(_layers.map( layer -> layer.withShadows(layer.shadows().mapStyles(styler::apply)) ));
    }

    StyleConf _withImages(UI.Layer layer, NamedStyles<ImageStyle> images ) {
        return StyleConf.of(_layout, _border, _base, _font, _dimensionality, _layers.with(layer, _layers.get(layer).withImages(images)), _properties);
    }

    StyleConf _withGradients(UI.Layer layer, NamedStyles<GradientStyle> shades ) {
        Objects.requireNonNull(shades);
        return StyleConf.of(_layout, _border, _base, _font, _dimensionality, _layers.with(layer, _layers.get(layer).withGradients(shades)), _properties);
    }

    StyleConf _withLayers(StyleLayers layers ) {
        if ( layers == _layers )
            return this;

        return StyleConf.of(_layout, _border, _base, _font, _dimensionality, layers, _properties);
    }

    StyleConf _withPainters(UI.Layer layer, NamedStyles<PainterStyle> painters ) {
        Objects.requireNonNull(painters);
        return StyleConf.of(_layout, _border, _base, _font, _dimensionality, _layers.with(layer, _layers.get(layer).withPainters(painters)), _properties);
    }

    StyleConf property(String key, String value ) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        return _withProperties(_properties.withNamedStyle(key, value));
    }

    List<NamedStyle<String>> properties() {
        return _properties.namedStyles()
                            .stream()
                            .sorted(Comparator.comparing(NamedStyle::name))
                            .collect(Collectors.toList());
    }

    StyleConf gradient(UI.Layer layer, String shadeName, Function<GradientStyle, GradientStyle> styler ) {
        Objects.requireNonNull(shadeName);
        Objects.requireNonNull(styler);
        GradientStyle shadow = Optional.ofNullable(_layers.get(layer).gradients().get(shadeName)).orElse(GradientStyle.none());
        // We clone the shadow map:
        NamedStyles<GradientStyle> newShadows = _layers.get(layer).gradients().withNamedStyle(shadeName, styler.apply(shadow));
        return _withGradients(layer, newShadows);
    }

    GradientStyle gradient( UI.Layer layer, String shadeName ) {
        Objects.requireNonNull(shadeName);
        return _layers.get(layer).gradients().get(shadeName);
    }

    StyleConf images(UI.Layer layer, String imageName, Function<ImageStyle, ImageStyle> styler ) {
        Objects.requireNonNull(imageName);
        Objects.requireNonNull(styler);
        ImageStyle ground = _layers.get(layer).images().style(imageName).orElse(ImageStyle.none());
        // We clone the ground map:
        NamedStyles<ImageStyle> newImages = _layers.get(layer).images().withNamedStyle(imageName, styler.apply(ground));
        return _withImages( layer, newImages );
    }

    List<ImageStyle> images( UI.Layer layer ) {
        return _layers.get(layer).images().sortedByNamesAndFilteredBy();
    }

    StyleConf scale(double scale ) {
        return StyleConf.of(
                    _layout,
                    _border._scale(scale),
                    _base, // Just colors and the cursor
                    _font._scale(scale),
                    _dimensionality._scale(scale),
                    _layers.map( layer -> layer._scale(scale) ),
                    _properties
                );
    }

    StyleConf simplified() {
        return _withBase(_base.simplified())
               ._withBorder(_border.simplified())
               ._withDimensionality(_dimensionality.simplified())
               ._withLayers(_layers.simplified());
    }

    StyleConf correctedForRounding() {
        return _withBorder(_border.correctedForRounding());
    }

    boolean hasEqualLayoutAs( StyleConf otherStyle ) {
        return Objects.equals(_layout, otherStyle._layout);
    }

    boolean hasEqualMarginAndPaddingAs( StyleConf otherStyle ) {
        return Objects.equals(_border.margin(), otherStyle._border.margin()) &&
               Objects.equals(_border.padding(), otherStyle._border.padding());
    }

    boolean hasEqualBorderAs( StyleConf otherStyle ) {
        return Objects.equals(_border, otherStyle._border);
    }

    boolean hasEqualBaseAs( StyleConf otherStyle ) {
        return Objects.equals(_base, otherStyle._base);
    }

    boolean hasEqualFontAs( StyleConf otherStyle ) {
        return Objects.equals(_font, otherStyle._font);
    }

    boolean hasEqualDimensionalityAs( StyleConf otherStyle ) {
        return Objects.equals(_dimensionality, otherStyle._dimensionality);
    }

    boolean hasEqualShadowsAs( StyleConf otherStyle ) {
        boolean allLayersAreEqual = true;
        for ( UI.Layer layer : UI.Layer.values() ) {
            if ( !hasEqualShadowsAs(layer, otherStyle) ) {
                allLayersAreEqual = false;
                break;
            }
        }
        return allLayersAreEqual;
    }

    boolean hasEqualShadowsAs( UI.Layer layer, StyleConf otherStyle ) {
        StyleLayer thisLayer = _layers.get(layer);
        StyleLayer otherLayer = otherStyle._layers.get(layer);
        if ( thisLayer == null && otherLayer == null )
            return true;
        if ( thisLayer == null || otherLayer == null )
            return false;
        return thisLayer.hasEqualShadowsAs(otherLayer);
    }

    boolean hasEqualPaintersAs( StyleConf otherStyle ) {
        boolean allLayersAreEqual = true;
        for ( UI.Layer layer : UI.Layer.values() ) {
            if ( !hasEqualPaintersAs(layer, otherStyle) ) {
                allLayersAreEqual = false;
                break;
            }
        }
        return allLayersAreEqual;
    }

    boolean hasEqualPaintersAs( UI.Layer layer, StyleConf otherStyle ) {
        StyleLayer thisLayer = _layers.get(layer);
        StyleLayer otherLayer = otherStyle._layers.get(layer);
        if ( thisLayer == null && otherLayer == null )
            return true;
        if ( thisLayer == null || otherLayer == null )
            return false;
        return thisLayer.hasEqualPaintersAs(otherLayer);
    }

    boolean hasEqualGradientsAs( StyleConf otherStyle ) {
        boolean allLayersAreEqual = true;
        for ( UI.Layer layer : UI.Layer.values() ) {
            if ( !hasEqualGradientsAs(layer, otherStyle) ) {
                allLayersAreEqual = false;
                break;
            }
        }
        return allLayersAreEqual;
    }

    boolean hasEqualGradientsAs( UI.Layer layer, StyleConf otherStyle ) {
        StyleLayer thisLayer = _layers.get(layer);
        StyleLayer otherLayer = otherStyle._layers.get(layer);
        if ( thisLayer == null && otherLayer == null )
            return true;
        if ( thisLayer == null || otherLayer == null )
            return false;
        return thisLayer.hasEqualGradientsAs(otherLayer);
    }

    boolean hasEqualImagesAs(StyleConf otherStyle ) {
        boolean allLayersAreEqual = true;
        for ( UI.Layer layer : UI.Layer.values() ) {
            if ( !hasEqualImagesAs(layer, otherStyle) ) {
                allLayersAreEqual = false;
                break;
            }
        }
        return allLayersAreEqual;
    }

    boolean hasEqualImagesAs( UI.Layer layer, StyleConf otherStyle ) {
        StyleLayer thisLayer = _layers.get(layer);
        StyleLayer otherLayer = otherStyle._layers.get(layer);
        if ( thisLayer == null && otherLayer == null )
            return true;
        if ( thisLayer == null || otherLayer == null )
            return false;
        return thisLayer.hasEqualImagesAs(otherLayer);
    }

    boolean hasEqualPropertiesAs( StyleConf otherStyle ) {
        return Objects.equals(_properties, otherStyle._properties);
    }

    Report getReport() {
        return new Report(this);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                    _layout, _border, _base, _font, _dimensionality, _layers, _properties
                );
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == this ) return true;
        if ( obj == null ) return false;
        if ( !(obj instanceof StyleConf) ) return false;
        StyleConf other = (StyleConf) obj;
        return hasEqualLayoutAs(other)         &&
               hasEqualBorderAs(other)         &&
               hasEqualBaseAs(other)           &&
               hasEqualFontAs(other)           &&
               hasEqualDimensionalityAs(other) &&
               hasEqualShadowsAs(other)        &&
               hasEqualPaintersAs(other)       &&
               hasEqualGradientsAs(other)      &&
               hasEqualImagesAs(other)         &&
               hasEqualPropertiesAs(other);
    }

    @Override
    public String toString() {
        String propertiesString = _properties.toString(StyleUtility.DEFAULT_KEY, "properties");

        return this.getClass().getSimpleName() + "[" +
                    _layout          + ", " +
                    _border          + ", " +
                    _base            + ", " +
                    _font            + ", " +
                    _dimensionality  + ", " +
                    _layers          + ", " +
                    propertiesString +
                "]";
    }

    static class Report
    {
        public final boolean noLayoutStyle;
        public final boolean noPaddingAndMarginStyle;
        public final boolean noBorderStyle;
        public final boolean borderIsVisible;
        public final boolean noBaseStyle;
        public final boolean noFontStyle;
        public final boolean noDimensionalityStyle;
        public final boolean noShadowStyle;
        public final boolean noPainters;
        public final boolean noGradients;
        public final boolean noImages;
        public final boolean noProperties;

        public final boolean allShadowsAreBorderShadows;
        public final boolean allGradientsAreBorderGradients;
        public final boolean allPaintersAreBorderPainters;
        public final boolean allImagesAreBorderImages;


        private Report( StyleConf styleConf) {
            this.noLayoutStyle           = StyleConf.none().hasEqualLayoutAs(styleConf);
            this.noPaddingAndMarginStyle = StyleConf.none().hasEqualMarginAndPaddingAs(styleConf);
            this.noBorderStyle           = StyleConf.none().hasEqualBorderAs(styleConf);
            this.noBaseStyle             = StyleConf.none().hasEqualBaseAs(styleConf);
            this.noFontStyle             = StyleConf.none().hasEqualFontAs(styleConf);
            this.noDimensionalityStyle   = StyleConf.none().hasEqualDimensionalityAs(styleConf);
            this.noShadowStyle           = StyleConf.none().hasEqualShadowsAs(styleConf);
            this.noPainters              = StyleConf.none().hasEqualPaintersAs(styleConf);
            this.noGradients             = StyleConf.none().hasEqualGradientsAs(styleConf);
            this.noImages                = StyleConf.none().hasEqualImagesAs(styleConf);
            this.noProperties            = StyleConf.none().hasEqualPropertiesAs(styleConf);

            this.borderIsVisible = styleConf.border().isVisible();

            this.allShadowsAreBorderShadows     = styleConf._layers.everyNamedStyle( (layer, styleLayer) -> layer == UI.Layer.BORDER || styleLayer.shadows().everyNamedStyle(ns -> !ns.style().color().isPresent() ) );
            this.allGradientsAreBorderGradients = styleConf._layers.everyNamedStyle( (layer, styleLayer) -> layer == UI.Layer.BORDER || styleLayer.gradients().everyNamedStyle(ns -> ns.style().colors().length == 0 ) );
            this.allPaintersAreBorderPainters   = styleConf._layers.everyNamedStyle( (layer, styleLayer) -> layer == UI.Layer.BORDER || styleLayer.painters().everyNamedStyle(ns -> Painter.none().equals(ns.style().painter()) ) );
            this.allImagesAreBorderImages       = styleConf._layers.everyNamedStyle( (layer, styleLayer) -> layer == UI.Layer.BORDER || styleLayer.images().everyNamedStyle(ns -> !ns.style().image().isPresent() && !ns.style().primer().isPresent() ) );
        }

        public boolean isNotStyled() {
            return
               noLayoutStyle           &&
               noPaddingAndMarginStyle &&
               noBorderStyle           &&
               noBaseStyle             &&
               noFontStyle             &&
               noDimensionalityStyle   &&
               noShadowStyle           &&
               noPainters              &&
               noGradients             &&
               noImages                &&
               noProperties;
        }

        public boolean onlyDimensionalityIsStyled() {
            return
               noLayoutStyle           &&
               noPaddingAndMarginStyle &&
               noBorderStyle           &&
               noBaseStyle             &&
               noFontStyle             &&
               !noDimensionalityStyle  &&
               noShadowStyle           &&
               noPainters              &&
               noGradients             &&
               noImages                &&
               noProperties;
        }

    }

}
