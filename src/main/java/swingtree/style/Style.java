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
 *  of a {@link Style} instance will always return a new {@link Style} instance with the
 *  updated property.
 *  <p>
 *  Consider the following example demonstrating how a {@link javax.swing.JPanel} is styled through the SwingTree
 *  style API, which consists of a functional {@link swingtree.api.Styler} lambda that processes a
 *  {@link ComponentStyleDelegate} instance that internally assembles a {@link Style} object:
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
public final class Style
{
    private static final Style _NONE = new Style(
                                            LayoutStyle.none(),
                                            BorderStyle.none(),
                                            BaseStyle.none(),
                                            FontStyle.none(),
                                            DimensionalityStyle.none(),
                                            StyleLayers.empty(),
                                            NamedStyles.empty()
                                        );

    /**
     * @return The default style instance, representing the absence of a style.
     */
    public static Style none() { return _NONE; }


    private final LayoutStyle                _layout;
    private final BorderStyle                _border;
    private final BaseStyle                  _base;
    private final FontStyle                  _font;
    private final DimensionalityStyle        _dimensionality;
    private final StyleLayers                _layers;
    private final NamedStyles<String>        _properties;


    private Style(
        LayoutStyle         layout,
        BorderStyle         border,
        BaseStyle           base,
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

    BorderStyle border() { return _border; }

    BaseStyle base() { return _base; }


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

    boolean anyVisibleShadows() {
        return Arrays.stream(UI.Layer.values()).anyMatch(this::anyVisibleShadows);
    }

    boolean anyVisibleShadows(UI.Layer layer) {
        return _layers.get(layer)
                .shadows()
                .stylesStream()
                .anyMatch(s -> s.color().isPresent() && s.color().get().getAlpha() > 0 );
    }

    public FontStyle font() { return _font; }

    /**
     * @return An unmodifiable list of painters sorted by their names in ascending alphabetical order.
     */
    List<Painter> painters( UI.Layer layer ) {
        return Collections.unmodifiableList(
                        _layers.get(layer)
                        .painters()
                        .sortedByNamesAndFilteredBy()
                        .stream()
                        .map(PainterStyle::painter)
                        .collect(Collectors.toList())
                    );
    }

    Style painter( UI.Layer layer, String painterName, Painter painter ) {
        Objects.requireNonNull(painterName);
        Objects.requireNonNull(painter);
        painterName = painterName + "_" + layer.name();
        // We clone the painter map:
        NamedStyles<PainterStyle> newPainters = _layers.get(layer)
                                                    .painters()
                                                    .withNamedStyle(
                                                        painterName, // Existing painters are overwritten if they have the same name.
                                                        PainterStyle.none().painter(painter)
                                                    ); 
        return painter(layer, newPainters);
    }

    /**
     *  Returns a new {@link Style} instance which only contains style information relevant
     *  for rendering the provided {@link UI.Layer}. Style information on other layers is discarded.
     * @param layer The layer to retain.
     * @return A new {@link Style} instance which only contains style information relevant to the provided {@link UI.Layer}.
     */
    public Style onlyRetainingRenderCacheRelevantConfForLayer( UI.Layer layer ) {
        return new Style(
                    LayoutStyle.none(),
                    _border, //( layer == UI.Layer.BORDER ? _border : BorderStyle.none() ),
                    ( layer == UI.Layer.BACKGROUND ? _base : BaseStyle.none() ),
                    FontStyle.none(),
                    DimensionalityStyle.none(),
                    _layers.onlyRetainingAsUnnamedLayer(layer),
                    NamedStyles.empty()
                );
    }

    boolean hasCustomBackgroundPainters() {
        return hasCustomPaintersOnLayer(UI.Layer.BACKGROUND);
    }

    boolean hasCustomForegroundPainters() {
        return hasCustomPaintersOnLayer(UI.Layer.FOREGROUND);
    }

    boolean hasCustomPaintersOnLayer( UI.Layer layer ) {
        return _layers.get(layer).painters().stylesStream().anyMatch(p -> !Painter.none().equals(p.painter()));
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

    boolean hasActiveBackgroundGradients() {
        List<GradientStyle> gradients = gradients(UI.Layer.BACKGROUND);
        if ( gradients.isEmpty() ) return false;
        return gradients.stream().anyMatch( s -> s.colors().length > 0 );
    }

    public Style foundationColor( Color color ) { return _withBase(base().foundationColor(color)); }

    public Style backgroundColor( Color color ) { return _withBase(base().backgroundColor(color)); }

    Style _withLayout( LayoutStyle layout ) {
        if ( layout == _layout )
            return this;

        return new Style(layout, _border, _base, _font, _dimensionality, _layers, _properties);
    }

    Style _withBorder( BorderStyle border ) {
        if ( border == _border )
            return this;

        return new Style(_layout, border, _base, _font, _dimensionality, _layers, _properties);
    }

    Style _withBase( BaseStyle background ) {
        if ( background == _base )
            return this;

        return new Style(_layout, _border, background, _font, _dimensionality, _layers, _properties);
    }

    Style _withFont( FontStyle font ) {
        if ( font == _font )
            return this;

        return new Style(_layout, _border, _base, font, _dimensionality, _layers, _properties);
    }

    Style _withDimensionality( DimensionalityStyle dimensionality ) {
        if ( dimensionality == _dimensionality )
            return this;

        return new Style(_layout, _border, _base, _font, dimensionality, _layers, _properties);
    }

    Style _withShadow( UI.Layer layer, NamedStyles<ShadowStyle> shadows ) {
        return new Style(_layout, _border, _base, _font, _dimensionality, _layers.with(layer, _layers.get(layer).withShadows(shadows)), _properties);
    }

    Style _withProperties( NamedStyles<String> properties ) {
        if ( properties == _properties )
            return this;

        return new Style(_layout, _border, _base, _font, _dimensionality, _layers, properties);
    }

    Style _withShadow( UI.Layer layer, Function<ShadowStyle, ShadowStyle> styler ) {
        // A new map is created where all the styler is applied to all the values:
        NamedStyles<ShadowStyle> styledShadows = _layers.get(layer).shadows().mapStyles(styler::apply);
        return _withShadow(layer, styledShadows);
    }

    Style _withShadow( Function<ShadowStyle, ShadowStyle> styler ) {
        return _withLayers(_layers.map( layer -> layer.withShadows(layer.shadows().mapStyles(styler::apply)) ));
    }

    Style _withImages( UI.Layer layer, NamedStyles<ImageStyle> images ) {
        return new Style(_layout, _border, _base, _font, _dimensionality, _layers.with(layer, _layers.get(layer).withImages(images)), _properties);
    }

    Style _withGradients( UI.Layer layer, NamedStyles<GradientStyle> shades ) {
        Objects.requireNonNull(shades);
        return new Style(_layout, _border, _base, _font, _dimensionality, _layers.with(layer, _layers.get(layer).withGradients(shades)), _properties);
    }

    Style _withLayers( StyleLayers layers ) {
        if ( layers == _layers )
            return this;

        return new Style(_layout, _border, _base, _font, _dimensionality, layers, _properties);
    }

    Style painter( UI.Layer layer, NamedStyles<PainterStyle> painters ) {
        Objects.requireNonNull(painters);
        return new Style(_layout, _border, _base, _font, _dimensionality, _layers.with(layer, _layers.get(layer).withPainters(painters)), _properties);
    }

    Style property( String key, String value ) {
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

    Style gradient( UI.Layer layer, String shadeName, Function<GradientStyle, GradientStyle> styler ) {
        Objects.requireNonNull(shadeName);
        Objects.requireNonNull(styler);
        GradientStyle shadow = Optional.ofNullable(_layers.get(layer).gradients().get(shadeName)).orElse(GradientStyle.none());
        // We clone the shadow map:
        NamedStyles<GradientStyle> newShadows = _layers.get(layer).gradients().withNamedStyle(shadeName, styler.apply(shadow));
        return _withGradients(layer, newShadows);
    }

    Style images( UI.Layer layer, String imageName, Function<ImageStyle, ImageStyle> styler ) {
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

    Style scale( double scale ) {
        return new Style(
                    _layout,
                    _border._scale(scale),
                    _base, // Just colors and the cursor
                    _font._scale(scale),
                    _dimensionality._scale(scale),
                    _layers.map( layer -> layer._scale(scale) ),
                    _properties
                );
    }

    Style simplified() {
        return _withBorder(_border.simplified())
               ._withDimensionality(_dimensionality.simplified())
               ._withLayers(_layers.simplified());
    }

    boolean hasEqualLayoutAs( Style otherStyle ) {
        return Objects.equals(_layout, otherStyle._layout);
    }

    boolean hasEqualMarginAndPaddingAs( Style otherStyle ) {
        return Objects.equals(_border.margin(), otherStyle._border.margin()) &&
               Objects.equals(_border.padding(), otherStyle._border.padding());
    }

    boolean hasEqualBorderAs( Style otherStyle ) {
        return Objects.equals(_border, otherStyle._border);
    }

    boolean hasEqualBaseAs( Style otherStyle ) {
        return Objects.equals(_base, otherStyle._base);
    }

    boolean hasEqualFontAs( Style otherStyle ) {
        return Objects.equals(_font, otherStyle._font);
    }

    boolean hasEqualDimensionalityAs( Style otherStyle ) {
        return Objects.equals(_dimensionality, otherStyle._dimensionality);
    }

    boolean hasEqualShadowsAs( Style otherStyle ) {
        boolean allLayersAreEqual = true;
        for ( UI.Layer layer : UI.Layer.values() ) {
            if ( !hasEqualShadowsAs(layer, otherStyle) ) {
                allLayersAreEqual = false;
                break;
            }
        }
        return allLayersAreEqual;
    }

    boolean hasEqualShadowsAs( UI.Layer layer, Style otherStyle ) {
        StyleLayer thisLayer = _layers.get(layer);
        StyleLayer otherLayer = otherStyle._layers.get(layer);
        if ( thisLayer == null && otherLayer == null )
            return true;
        if ( thisLayer == null || otherLayer == null )
            return false;
        return thisLayer.hasEqualShadowsAs(otherLayer);
    }

    boolean hasEqualPaintersAs( Style otherStyle ) {
        boolean allLayersAreEqual = true;
        for ( UI.Layer layer : UI.Layer.values() ) {
            if ( !hasEqualPaintersAs(layer, otherStyle) ) {
                allLayersAreEqual = false;
                break;
            }
        }
        return allLayersAreEqual;
    }

    boolean hasEqualPaintersAs( UI.Layer layer, Style otherStyle ) {
        StyleLayer thisLayer = _layers.get(layer);
        StyleLayer otherLayer = otherStyle._layers.get(layer);
        if ( thisLayer == null && otherLayer == null )
            return true;
        if ( thisLayer == null || otherLayer == null )
            return false;
        return thisLayer.hasEqualPaintersAs(otherLayer);
    }

    boolean hasEqualGradientsAs( Style otherStyle ) {
        boolean allLayersAreEqual = true;
        for ( UI.Layer layer : UI.Layer.values() ) {
            if ( !hasEqualGradientsAs(layer, otherStyle) ) {
                allLayersAreEqual = false;
                break;
            }
        }
        return allLayersAreEqual;
    }

    boolean hasEqualGradientsAs( UI.Layer layer, Style otherStyle ) {
        StyleLayer thisLayer = _layers.get(layer);
        StyleLayer otherLayer = otherStyle._layers.get(layer);
        if ( thisLayer == null && otherLayer == null )
            return true;
        if ( thisLayer == null || otherLayer == null )
            return false;
        return thisLayer.hasEqualGradientsAs(otherLayer);
    }

    boolean hasEqualImagesAs(Style otherStyle ) {
        boolean allLayersAreEqual = true;
        for ( UI.Layer layer : UI.Layer.values() ) {
            if ( !hasEqualImagesAs(layer, otherStyle) ) {
                allLayersAreEqual = false;
                break;
            }
        }
        return allLayersAreEqual;
    }

    boolean hasEqualImagesAs( UI.Layer layer, Style otherStyle ) {
        StyleLayer thisLayer = _layers.get(layer);
        StyleLayer otherLayer = otherStyle._layers.get(layer);
        if ( thisLayer == null && otherLayer == null )
            return true;
        if ( thisLayer == null || otherLayer == null )
            return false;
        return thisLayer.hasEqualImagesAs(otherLayer);
    }

    boolean hasEqualPropertiesAs( Style otherStyle ) {
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
        if ( !(obj instanceof Style) ) return false;
        Style other = (Style) obj;
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


        private Report( Style style ) {
            this.noLayoutStyle           = Style.none().hasEqualLayoutAs(style);
            this.noPaddingAndMarginStyle = Style.none().hasEqualMarginAndPaddingAs(style);
            this.noBorderStyle           = Style.none().hasEqualBorderAs(style);
            this.noBaseStyle             = Style.none().hasEqualBaseAs(style);
            this.noFontStyle             = Style.none().hasEqualFontAs(style);
            this.noDimensionalityStyle   = Style.none().hasEqualDimensionalityAs(style);
            this.noShadowStyle           = Style.none().hasEqualShadowsAs(style);
            this.noPainters              = Style.none().hasEqualPaintersAs(style);
            this.noGradients             = Style.none().hasEqualGradientsAs(style);
            this.noImages                = Style.none().hasEqualImagesAs(style);
            this.noProperties            = Style.none().hasEqualPropertiesAs(style);

            this.borderIsVisible = style.border().isVisible();

            this.allShadowsAreBorderShadows     = style._layers.everyNamedStyle( (layer, styleLayer) -> layer == UI.Layer.BORDER || styleLayer.shadows().everyNamedStyle(   ns -> !ns.style().color().isPresent() ) );
            this.allGradientsAreBorderGradients = style._layers.everyNamedStyle( (layer, styleLayer) -> layer == UI.Layer.BORDER || styleLayer.gradients().everyNamedStyle( ns -> ns.style().colors().length == 0 ) );
            this.allPaintersAreBorderPainters   = style._layers.everyNamedStyle( (layer, styleLayer) -> layer == UI.Layer.BORDER || styleLayer.painters().everyNamedStyle(  ns -> Painter.none().equals(ns.style().painter()) ) );
            this.allImagesAreBorderImages       = style._layers.everyNamedStyle( (layer, styleLayer) -> layer == UI.Layer.BORDER || styleLayer.images().everyNamedStyle(    ns -> !ns.style().image().isPresent() && !ns.style().primer().isPresent() ) );
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
