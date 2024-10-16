package swingtree.style;

import com.google.errorprone.annotations.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import swingtree.UI;
import swingtree.api.Configurator;
import swingtree.api.Painter;

import java.awt.*;
import java.util.List;
import java.util.*;
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
@Immutable
@SuppressWarnings("ReferenceEquality")
public final class StyleConf
{
    private static final StyleConf _NONE = new StyleConf(
                                            LayoutConf.none(),
                                            BorderConf.none(),
                                            BaseConf.none(),
                                            FontConf.none(),
                                            DimensionalityConf.none(),
                                            StyleConfLayers.empty(),
                                            NamedConfigs.empty()
                                        );
    private static final Logger log = LoggerFactory.getLogger(StyleConf.class);

    /**
     *  Exposes the "null object" pattern for {@link StyleConf} instances.
     *  So the constant returned by this method is the default instance
     *  that represents the absence of a style.
     *
     * @return The default style instance, representing the absence of a style.
     */
    public static StyleConf none() { return _NONE; }

    static StyleConf of(
        LayoutConf layout,
        BorderConf border,
        BaseConf base,
        FontConf font,
        DimensionalityConf dimensionality,
        StyleConfLayers layers,
        NamedConfigs<String> properties
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


    private final LayoutConf           _layout;
    private final BorderConf           _border;
    private final BaseConf             _base;
    private final FontConf             _font;
    private final DimensionalityConf   _dimensionality;
    private final StyleConfLayers      _layers;
    private final NamedConfigs<String> _properties;


    private StyleConf(
        LayoutConf           layout,
        BorderConf           border,
        BaseConf             base,
        FontConf             font,
        DimensionalityConf   dimensionality,
        StyleConfLayers      layers,
        NamedConfigs<String> properties
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

    public LayoutConf layout() { return _layout; }

    Outline padding() { return _border.padding(); }

    Outline margin() { return _border.margin(); }

    BorderConf border() { return _border; }

    BaseConf base() { return _base; }

    DimensionalityConf dimensionality() { return _dimensionality; }

    StyleConfLayers layers() { return _layers; }

    StyleConfLayer layer(UI.Layer layer ) { return _layers.get(layer); }

    /**
     *  Exposes the default shadow style configuration object.
     * @return The default shadow style.
     */
    public ShadowConf shadow() {
        ShadowConf found =
                        _layers.get(ShadowConf.DEFAULT_LAYER)
                        .shadows()
                        .get(StyleUtil.DEFAULT_KEY);

        return found != null ? found : ShadowConf.none();
    }

    /**
     *  Internally, a style configuration consists of a set of layers defined by the {@link UI.Layer} enum.
     *  Using this method you can retrieve all shadow styles for a particular layer
     *  and with the provided name.
     *
     * @param layer The layer to retrieve the shadow style from.
     * @param shadowName The name of the shadow style to retrieve.
     * @return The shadow style with the provided name.
     */
    public ShadowConf shadow( UI.Layer layer, String shadowName ) {
        Objects.requireNonNull(shadowName);
        StyleConfLayer layerConf = _layers.get(layer);
        NamedConfigs<ShadowConf> shadows = layerConf.shadows();
        ShadowConf found = shadows.get(shadowName);
        return found != null ? found : ShadowConf.none();
    }

    /**
     *  Internally, a style configuration consists of a set of layers defined by the {@link UI.Layer} enum.
     *  You can retrieve all shadow styles for a specific layer by calling this method.
     *
     * @return An unmodifiable list of all shadow styles sorted by their names in ascending alphabetical order.
     */
    List<ShadowConf> shadows( UI.Layer layer ) {
        return Collections.unmodifiableList(
                        _layers.get(layer)
                        .shadows()
                        .namedStyles()
                        .stream()
                        .sorted(Comparator.comparing(NamedConf::name))
                        .map(NamedConf::style)
                        .collect(Collectors.toList())
                    );
    }

    NamedConfigs<ShadowConf> shadowsMap(UI.Layer layer) {
        return _layers.get(layer).shadows();
    }

    boolean hasVisibleShadows(UI.Layer layer) {
        return _layers.get(layer)
                .shadows()
                .stylesStream()
                .anyMatch(s -> s.color().isPresent() && s.color().get().getAlpha() > 0 );
    }

    public FontConf font() { return _font; }

    /**
     *  Returns a new {@link StyleConf} instance with the given layout constraint.
     * @return An unmodifiable list of painters sorted by their names in ascending alphabetical order.
     */
    List<PainterConf> painters( UI.Layer layer ) {
        return Collections.unmodifiableList(
                            new ArrayList<>(_layers.get(layer)
                                .painters()
                                .sortedByNames()
                            )
                        );
    }

    StyleConf painter(UI.Layer layer, UI.ComponentArea area, String painterName, Painter painter ) {
        Objects.requireNonNull(painterName);
        Objects.requireNonNull(painter);
        // We clone the painter map:
        NamedConfigs<PainterConf> newPainters = _layers.get(layer)
                                                    .painters()
                                                    .withNamedStyle(
                                                        painterName, // Existing painters are overwritten if they have the same name.
                                                        PainterConf.of(painter, area)
                                                    ); 
        return _withPainters(layer, newPainters);
    }

    boolean hasPaintersOnLayer(UI.Layer layer ) {
        return _layers.get(layer).painters().stylesStream().anyMatch(p -> !Painter.none().equals(p.painter()));
    }

    boolean hasImagesOnLayer(UI.Layer layer ) {
        return _layers.get(layer).images().stylesStream().anyMatch(i -> i.image().isPresent() || i.primer().isPresent());
    }

    List<GradientConf> gradients( UI.Layer layer ) {
        return _layers.get(layer).gradients().sortedByNames();
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
        NamedConfigs<GradientConf> gradients = _layers.get(layer).gradients();
        return !( gradients.size() == 1 && GradientConf.none().equals(gradients.get(StyleUtil.DEFAULT_KEY)) );
    }

    boolean hasVisibleGradientsOnLayer( UI.Layer layer ) {
        List<GradientConf> gradients = gradients(layer);
        if ( gradients.isEmpty() ) return false;
        return gradients.stream().anyMatch( s -> s.colors().length > 0 );
    }

    List<NoiseConf> noises( UI.Layer layer ) {
        return _layers.get(layer).noises().sortedByNames();
    }

    boolean hasVisibleNoisesOnLayer( UI.Layer layer ) {
        List<NoiseConf> noises = noises(layer);
        if ( noises.isEmpty() ) return false;
        return noises.stream().anyMatch( s -> !s.equals(NoiseConf.none()) );
    }

    List<UI.ComponentArea> gradientCoveredAreas() {
        return gradientCoveredAreas(UI.Layer.values());
    }

    List<UI.ComponentArea> gradientCoveredAreas( UI.Layer... layers ) {
        return Arrays.stream(layers)
                .map(_layers::get)
                .map(StyleConfLayer::gradients)
                .flatMap( g -> g
                    .stylesStream()
                    .map( grad -> grad.isOpaque() ? grad.area() : null )
                    .filter(Objects::nonNull)
                )
                .distinct()
                .collect(Collectors.toList());
    }

    List<UI.ComponentArea> noiseCoveredAreas() {
        return noiseCoveredAreas(UI.Layer.values());
    }

    List<UI.ComponentArea> noiseCoveredAreas( UI.Layer... layers ) {
        return Arrays.stream(layers)
                .map(_layers::get)
                .map(StyleConfLayer::noises)
                .flatMap( n -> n
                    .stylesStream()
                    .map( noise -> noise.isOpaque() ? noise.area() : null )
                    .filter(Objects::nonNull)
                )
                .distinct()
                .collect(Collectors.toList());
    }

    List<UI.ComponentArea> noiseAndGradientCoveredAreas() {
        List<UI.ComponentArea> areas = new ArrayList<>(gradientCoveredAreas());
        areas.addAll(noiseCoveredAreas());
        return areas;
    }

    public StyleConf foundationColor( Color color ) { return _withBase(base().foundationColor(color)); }

    public StyleConf backgroundColor( Color color ) { return _withBase(base().backgroundColor(color)); }

    StyleConf _withLayout( LayoutConf layout ) {
        if ( layout == _layout )
            return this;

        return StyleConf.of(layout, _border, _base, _font, _dimensionality, _layers, _properties);
    }

    StyleConf _withBorder( BorderConf border ) {
        if ( border == _border )
            return this;

        return StyleConf.of(_layout, border, _base, _font, _dimensionality, _layers, _properties);
    }

    StyleConf _withBase( BaseConf background ) {
        if ( background == _base )
            return this;

        return StyleConf.of(_layout, _border, background, _font, _dimensionality, _layers, _properties);
    }

    StyleConf _withFont( FontConf font ) {
        if ( font == _font )
            return this;

        return StyleConf.of(_layout, _border, _base, font, _dimensionality, _layers, _properties);
    }

    StyleConf _withDimensionality( DimensionalityConf dimensionality ) {
        if ( dimensionality == _dimensionality )
            return this;

        return StyleConf.of(_layout, _border, _base, _font, dimensionality, _layers, _properties);
    }

    StyleConf _withShadow( UI.Layer layer, NamedConfigs<ShadowConf> shadows ) {
        return StyleConf.of(_layout, _border, _base, _font, _dimensionality, _layers.with(layer, _layers.get(layer).withShadows(shadows)), _properties);
    }

    StyleConf _withProperties( NamedConfigs<String> properties ) {
        if ( properties == _properties )
            return this;

        return StyleConf.of(_layout, _border, _base, _font, _dimensionality, _layers, properties);
    }

    StyleConf _withShadow( UI.Layer layer, Configurator<ShadowConf> styler ) {
        // A new map is created where all the styler is applied to all the values:
        NamedConfigs<ShadowConf> styledShadows = _layers.get(layer).shadows().mapStyles(styler);
        return _withShadow(layer, styledShadows);
    }

    StyleConf _withShadow( Configurator<ShadowConf> styler ) {
        return _withLayers(_layers.map( layer -> layer.withShadows(layer.shadows().mapStyles(styler)) ));
    }

    StyleConf _withGradients( UI.Layer layer, NamedConfigs<GradientConf> shades ) {
        Objects.requireNonNull(shades);
        return StyleConf.of(_layout, _border, _base, _font, _dimensionality, _layers.with(layer, _layers.get(layer).withGradients(shades)), _properties);
    }

    StyleConf _withNoises( UI.Layer layer, NamedConfigs<NoiseConf> noises ) {
        Objects.requireNonNull(noises);
        return StyleConf.of(_layout, _border, _base, _font, _dimensionality, _layers.with(layer, _layers.get(layer).withNoises(noises)), _properties);
    }

    StyleConf _withImages( UI.Layer layer, NamedConfigs<ImageConf> images ) {
        return StyleConf.of(_layout, _border, _base, _font, _dimensionality, _layers.with(layer, _layers.get(layer).withImages(images)), _properties);
    }

    StyleConf _withTexts( UI.Layer layer, NamedConfigs<TextConf> texts ) {
        return StyleConf.of(_layout, _border, _base, _font, _dimensionality, _layers.with(layer, _layers.get(layer).withTexts(texts)), _properties);
    }

    StyleConf _withLayers( StyleConfLayers layers ) {
        if ( layers == _layers )
            return this;

        return StyleConf.of(_layout, _border, _base, _font, _dimensionality, layers, _properties);
    }

    StyleConf _withPainters( UI.Layer layer, NamedConfigs<PainterConf> painters ) {
        Objects.requireNonNull(painters);
        return StyleConf.of(_layout, _border, _base, _font, _dimensionality, _layers.with(layer, _layers.get(layer).withPainters(painters)), _properties);
    }

    StyleConf property( String key, String value ) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        return _withProperties(_properties.withNamedStyle(key, value));
    }

    List<NamedConf<String>> properties() {
        return _properties.namedStyles()
                            .stream()
                            .sorted(Comparator.comparing(NamedConf::name))
                            .collect(Collectors.toList());
    }

    StyleConf gradient( UI.Layer layer, String shadeName, Configurator<GradientConf> styler ) {
        Objects.requireNonNull(shadeName);
        Objects.requireNonNull(styler);
        GradientConf gradConf = _layers.get(layer).gradients().find(shadeName).orElse(GradientConf.none());
        // We clone the shadow map:
        try {
            gradConf = styler.configure(gradConf);
        } catch (Exception e) {
            log.error("Failed to configure gradient '{}' for layer '{}'", shadeName, layer, e);
        }
        NamedConfigs<GradientConf> newShadows = _layers.get(layer).gradients().withNamedStyle(shadeName, gradConf);
        return _withGradients(layer, newShadows);
    }

    GradientConf gradient( UI.Layer layer, String gradName ) {
        Objects.requireNonNull(gradName);
        StyleConfLayer layerConf = _layers.get(layer);
        NamedConfigs<GradientConf> gradients = layerConf.gradients();
        GradientConf found = gradients.get(gradName);
        return found != null ? found : GradientConf.none();
    }

    StyleConf noise( UI.Layer layer, String noiseName, Configurator<NoiseConf> styler ) {
        Objects.requireNonNull(noiseName);
        Objects.requireNonNull(styler);
        NoiseConf noise = _layers.get(layer).noises().find(noiseName).orElse(NoiseConf.none());
        try {
            noise = styler.configure(noise);
        } catch (Exception e) {
            log.error("Failed to configure noise '{}' for layer '{}'", noiseName, layer, e);
        }
        // We clone the noise map:
        NamedConfigs<NoiseConf> newNoises = _layers.get(layer).noises().withNamedStyle(noiseName, noise);
        return _withNoises(layer, newNoises);
    }

    StyleConf images(UI.Layer layer, String imageName, Configurator<ImageConf> styler ) {
        Objects.requireNonNull(imageName);
        Objects.requireNonNull(styler);
        ImageConf ground = _layers.get(layer).images().find(imageName).orElse(ImageConf.none());
        try {
            ground = styler.configure(ground);
        } catch (Exception e) {
            log.error("Failed to configure image '{}' for layer '{}'", imageName, layer, e);
        }
        // We clone the ground map:
        NamedConfigs<ImageConf> newImages = _layers.get(layer).images().withNamedStyle(imageName, ground);
        return _withImages( layer, newImages );
    }

    List<ImageConf> images( UI.Layer layer ) {
        return _layers.get(layer).images().sortedByNames();
    }

    StyleConf text(UI.Layer layer, String textName, Configurator<TextConf> styler ) {
        Objects.requireNonNull(textName);
        Objects.requireNonNull(styler);
        TextConf text = _layers.get(layer).texts().find(textName).orElse(TextConf.none());
        try {
            text = styler.configure(text);
        } catch (Exception e) {
            log.error("Failed to configure text '{}' for layer '{}'", textName, layer, e);
        }
        // We clone the text map:
        NamedConfigs<TextConf> newTexts = _layers.get(layer).texts().withNamedStyle(textName, text);
        return _withTexts( layer, newTexts );
    }

    StyleConf text( Configurator<TextConf> styler ) {
        return _withLayers(_layers.map( layer -> layer.withTexts(layer.texts().mapStyles(styler)) ));
    }

    List<TextConf> texts( UI.Layer layer ) {
        return _layers.get(layer).texts().sortedByNames();
    }

    StyleConf scale( double scale ) {
        return StyleConf.of(
                    _layout,
                    _border._scale(scale),
                    _base, // Just colors and the cursor
                    _font._scale(scale),
                    _dimensionality._scale(scale),
                    _layers._scale(scale),
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

    boolean hasEqualFilterAs( StyleConf otherStyle ) {
        return _layers.filter().equals(otherStyle._layers.filter());
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
        StyleConfLayer thisLayer = _layers.get(layer);
        StyleConfLayer otherLayer = otherStyle._layers.get(layer);
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
        StyleConfLayer thisLayer = _layers.get(layer);
        StyleConfLayer otherLayer = otherStyle._layers.get(layer);
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
        StyleConfLayer thisLayer = _layers.get(layer);
        StyleConfLayer otherLayer = otherStyle._layers.get(layer);
        if ( thisLayer == null && otherLayer == null )
            return true;
        if ( thisLayer == null || otherLayer == null )
            return false;
        return thisLayer.hasEqualGradientsAs(otherLayer);
    }

    boolean hasEqualNoisesAs( StyleConf otherStyle ) {
        boolean allLayersAreEqual = true;
        for ( UI.Layer layer : UI.Layer.values() ) {
            if ( !hasEqualNoisesAs(layer, otherStyle) ) {
                allLayersAreEqual = false;
                break;
            }
        }
        return allLayersAreEqual;
    }

    boolean hasEqualNoisesAs( UI.Layer layer, StyleConf otherStyle ) {
        StyleConfLayer thisLayer = _layers.get(layer);
        StyleConfLayer otherLayer = otherStyle._layers.get(layer);
        if ( thisLayer == null && otherLayer == null )
            return true;
        if ( thisLayer == null || otherLayer == null )
            return false;
        return thisLayer.hasEqualNoisesAs(otherLayer);
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
        StyleConfLayer thisLayer = _layers.get(layer);
        StyleConfLayer otherLayer = otherStyle._layers.get(layer);
        if ( thisLayer == null && otherLayer == null )
            return true;
        if ( thisLayer == null || otherLayer == null )
            return false;
        return thisLayer.hasEqualImagesAs(otherLayer);
    }

    boolean hasEqualTextsAs( StyleConf otherStyle ) {
        boolean allLayersAreEqual = true;
        for ( UI.Layer layer : UI.Layer.values() ) {
            if ( !hasEqualTextsAs(layer, otherStyle) ) {
                allLayersAreEqual = false;
                break;
            }
        }
        return allLayersAreEqual;
    }

    boolean hasEqualTextsAs( UI.Layer layer, StyleConf otherStyle ) {
        StyleConfLayer thisLayer = _layers.get(layer);
        StyleConfLayer otherLayer = otherStyle._layers.get(layer);
        if ( thisLayer == null && otherLayer == null )
            return true;
        if ( thisLayer == null || otherLayer == null )
            return false;
        return thisLayer.hasEqualTextsAs(otherLayer);
    }

    boolean hasEqualPropertiesAs( StyleConf otherStyle ) {
        return Objects.equals(_properties, otherStyle._properties);
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
               hasEqualFilterAs(other)         &&
               hasEqualShadowsAs(other)        &&
               hasEqualPaintersAs(other)       &&
               hasEqualGradientsAs(other)      &&
               hasEqualNoisesAs(other)         &&
               hasEqualImagesAs(other)         &&
               hasEqualTextsAs(other)          &&
               hasEqualPropertiesAs(other);
    }

    @Override
    public String toString() {
        String propertiesString = _properties.toString(StyleUtil.DEFAULT_KEY, "properties");

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

}
