package swingtree.style;

import swingtree.UI;
import swingtree.api.Painter;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *  An immutable, wither like method based settings container for {@link javax.swing.JComponent} styling.
 *  The styling in SwingTree is functional, meaning that changing a property
 *  of a {@link Style} instance will return a new {@link Style} instance with the
 *  updated property.
 *  <p>
 *  Here an example showing how a {@link javax.swing.JPanel} is styled through the SwingTree
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
 */
public final class Style
{
    private static final Style _NONE = new Style(
                                            LayoutStyle.none(),
                                            BorderStyle.none(),
                                            BaseStyle.none(),
                                            FontStyle.none(),
                                            DimensionalityStyle.none(),
                                            Collections.singletonMap(StyleUtility.DEFAULT_KEY,ShadowStyle.none()),
                                            Collections.singletonMap(StyleUtility.DEFAULT_KEY + "_" + PainterStyle.none().layer().name(),PainterStyle.none()),
                                            Collections.singletonMap(StyleUtility.DEFAULT_KEY, GradientStyle.none()),
                                            Collections.singletonMap(StyleUtility.DEFAULT_KEY, ImageStyle.none())
                                        );

    public static Style none() { return _NONE; }

    private final LayoutStyle                _layout;
    private final BorderStyle                _border;
    private final BaseStyle                  _base;
    private final FontStyle                  _font;
    private final DimensionalityStyle        _dimensionality;
    private final Map<String, ShadowStyle>   _shadows   = new TreeMap<>();
    private final Map<String, PainterStyle>  _painters  = new TreeMap<>();
    private final Map<String, GradientStyle> _gradients = new TreeMap<>();
    private final Map<String, ImageStyle>    _images    = new TreeMap<>();


    private Style(
        LayoutStyle                layout,
        BorderStyle                border,
        BaseStyle                  base,
        FontStyle                  font,
        DimensionalityStyle        dimensionality,
        Map<String, ShadowStyle>   shadows,
        Map<String, PainterStyle>  painters,
        Map<String, GradientStyle> gradients,
        Map<String, ImageStyle>    images
    ) {
        _layout         = layout;
        _border         = border;
        _base           = base;
        _font           = font;
        _dimensionality = dimensionality;
        _shadows.putAll(shadows);
        _painters.putAll(painters);
        _gradients.putAll(gradients);
        _images.putAll(images);
    }

    Style _withLayout( LayoutStyle layout ) {
        return new Style(layout, _border, _base, _font, _dimensionality, _shadows, _painters, _gradients, _images);
    }
    Style _withBorder( BorderStyle border ) {
        return new Style(_layout, border, _base, _font, _dimensionality, _shadows, _painters, _gradients, _images);
    }
    Style _withBackground( BaseStyle background ) {
        return new Style(_layout, _border, background, _font, _dimensionality, _shadows, _painters, _gradients, _images);
    }

    public Style foundationColor( Color color ) { return _withBackground(base().foundationColor(color)); }

    public Style backgroundColor( Color color ) { return _withBackground(base().backgroundColor(color)); }

    Style _withFont( FontStyle font ) {
        return new Style(_layout, _border, _base, font, _dimensionality, _shadows, _painters, _gradients, _images);
    }

    Style _withDimensionality( DimensionalityStyle dimensionality ) {
        return new Style(_layout, _border, _base, _font, dimensionality, _shadows, _painters, _gradients, _images);
    }

    Style _withShadow( Map<String, ShadowStyle> shadows ) {
        return new Style(_layout, _border, _base, _font, _dimensionality, shadows, _painters, _gradients, _images);
    }

    Style _withShadow( Function<ShadowStyle, ShadowStyle> styler ) {
        // A new map is created where all the styler is applied to all the values:
        Map<String, ShadowStyle> styledShadows = new TreeMap<>();
        _shadows.forEach( (key, value) -> styledShadows.put(key, styler.apply(value)) );
        return _withShadow(styledShadows);
    }

    Style _withImages( Map<String, ImageStyle> grounds ) {
        return new Style(_layout, _border, _base, _font, _dimensionality, _shadows, _painters, _gradients, grounds);
    }

    Style _withImages( Function<ImageStyle, ImageStyle> styler ) {
        // A new map is created where all the styler is applied to all the values:
        Map<String, ImageStyle> styledGrounds = new TreeMap<>();
        _images.forEach( (key, value) -> styledGrounds.put(key, styler.apply(value)) );
        return _withImages(styledGrounds);
    }

    public LayoutStyle layout() { return _layout; }

    public Outline padding() { return _layout.padding(); }

    public Outline margin() { return _layout.margin(); }

    public BorderStyle border() { return _border; }

    public BaseStyle base() { return _base; }


    public DimensionalityStyle dimensionality() { return _dimensionality; }


    /**
     * @return The default shadow style.
     */
    public ShadowStyle shadow() { return _shadows.get(StyleUtility.DEFAULT_KEY); }

    /**
     * @param shadowName The name of the shadow style to retrieve.
     * @return The shadow style with the provided name.
     */
    public ShadowStyle shadow( String shadowName ) {
        Objects.requireNonNull(shadowName);
        return _shadows.get(shadowName);
    }

    /**
     * @return An unmodifiable list of all shadow styles sorted by their names in ascending alphabetical order.
     */
    List<ShadowStyle> shadows( UI.Layer layer ) {
        return Collections.unmodifiableList(
                _shadows.entrySet()
                        .stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(Map.Entry::getValue)
                        .filter( s -> s.layer() == layer )
                        .collect(Collectors.toList())
            );
    }

    Map<String, ShadowStyle> shadowsMap() { return _shadows; }

    boolean anyVisibleShadows() {
        return _shadows.values().stream().anyMatch(s -> s.color().isPresent() && s.color().get().getAlpha() > 0 );
    }

    public FontStyle font() { return _font; }

    /**
     * @return An unmodifiable list of painters sorted by their names in ascending alphabetical order.
     */
    List<Painter> painters( UI.Layer layer ) {
        return Collections.unmodifiableList(
                _painters
                        .entrySet()
                        .stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(Map.Entry::getValue)
                        .filter( p -> p.layer() == layer )
                        .map(PainterStyle::painter)
                        .collect(Collectors.toList())
        );
    }

    public List<Painter> painters() {
        return Collections.unmodifiableList(
                                _painters
                                    .entrySet()
                                    .stream()
                                    .sorted(Map.Entry.comparingByKey())
                                    .map(Map.Entry::getValue)
                                    .map(PainterStyle::painter)
                                    .collect(Collectors.toList())
                            );
    }

    Style painter( String painterName, UI.Layer layer, Painter painter ) {
        Objects.requireNonNull(painterName);
        Objects.requireNonNull(painter);
        painterName = painterName + "_" + layer.name();
        // We clone the painter map:
        Map<String, PainterStyle> newPainters = new HashMap<>(_painters);
        newPainters.put(painterName, PainterStyle.none().painter(painter).layer(layer)); // Existing painters are overwritten if they have the same name.
        return painter(newPainters);
    }


    boolean hasCustomBackgroundPainters() {
        return _painters.values().stream().anyMatch(p -> p.layer() == UI.Layer.BACKGROUND && !Painter.none().equals(p.painter()));
    }

    boolean hasCustomForegroundPainters() {
        return _painters.values().stream().anyMatch(p -> p.layer() == UI.Layer.FOREGROUND && !Painter.none().equals(p.painter()));
    }

    List<GradientStyle> gradients(UI.Layer layer) {
        return Collections.unmodifiableList(
                _gradients.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(Map.Entry::getValue)
                        .filter( s -> s.layer() == layer )
                        .collect(Collectors.toList())
        );
    }

    boolean hasCustomGradients() {
        return !( _gradients.size() == 1 && GradientStyle.none().equals(_gradients.get(StyleUtility.DEFAULT_KEY)) );
    }

    boolean hasActiveBackgroundGradients() {
        List<GradientStyle> gradients = gradients(UI.Layer.BACKGROUND);
        if ( gradients.isEmpty() ) return false;
        return gradients.stream().anyMatch( s -> s.colors().length > 0 );
    }

    Style painter( Map<String, PainterStyle> painters ) {
        Objects.requireNonNull(painters);
        return new Style(_layout, _border, _base, _font, _dimensionality, _shadows, painters, _gradients, _images);
    }

    Style gradient( Map<String, GradientStyle> shades ) {
        Objects.requireNonNull(shades);
        return new Style(_layout, _border, _base, _font, _dimensionality, _shadows, _painters, shades, _images);
    }

    Style gradient( String shadeName, Function<GradientStyle, GradientStyle> styler ) {
        Objects.requireNonNull(shadeName);
        Objects.requireNonNull(styler);
        GradientStyle shadow = Optional.ofNullable(_gradients.get(shadeName)).orElse(GradientStyle.none());
        // We clone the shadow map:
        Map<String, GradientStyle> newShadows = new HashMap<>(_gradients);
        newShadows.put(shadeName, styler.apply(shadow));
        return gradient(newShadows);
    }

    Style images( Map<String, ImageStyle> images ) {
        Objects.requireNonNull(images);
        return new Style(_layout, _border, _base, _font, _dimensionality, _shadows, _painters, _gradients, images);
    }

    Style images( String imageName, Function<ImageStyle, ImageStyle> styler ) {
        Objects.requireNonNull(imageName);
        Objects.requireNonNull(styler);
        ImageStyle ground = Optional.ofNullable(_images.get(imageName)).orElse(ImageStyle.none());
        // We clone the ground map:
        Map<String, ImageStyle> newImages = new HashMap<>(_images);
        newImages.put(imageName, styler.apply(ground));
        return images(newImages);
    }

    List<ImageStyle> images( UI.Layer layer ) {
        return Collections.unmodifiableList(
                _images.entrySet()
                        .stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(Map.Entry::getValue)
                        .filter( s -> s.layer() == layer )
                        .collect(Collectors.toList())
        );
    }

    Style scale( double scale ) {
        return new Style(
                    _layout._scale(scale),
                    _border._scale(scale),
                    _base, // Just colors and the cursor
                    _font._scale(scale),
                    _dimensionality._scale(scale),
                    _shadows.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()._scale(scale))),
                    _painters, // This is the users problem...
                    _gradients, // Scaling does not make sense
                    _images.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()._scale(scale)))
                );
    }

    boolean hasEqualLayoutAs( Style otherStyle ) {
        return Objects.equals(_layout, otherStyle._layout);
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
        return StyleUtility.mapEquals(_shadows, otherStyle._shadows);
    }

    boolean hasEqualPaintersAs( Style otherStyle ) {
        return StyleUtility.mapEquals(_painters, otherStyle._painters);
    }

    boolean hasEqualGradientsAs( Style otherStyle ) {
        return StyleUtility.mapEquals(_gradients, otherStyle._gradients);
    }

    boolean hasEqualGroundsAs( Style otherStyle ) {
        return StyleUtility.mapEquals(_images, otherStyle._images);
    }

    Report getReport() {
        return new Report(this);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                    _layout, _border, _base, _font, _dimensionality,
                    StyleUtility.mapHash(_shadows), StyleUtility.mapHash(_painters),
                    StyleUtility.mapHash(_gradients),  StyleUtility.mapHash(_images)
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
               hasEqualGroundsAs(other);
    }

    @Override
    public String toString() {
        String shadowString;
        if ( _shadows.size() == 1 )
            shadowString = _shadows.get(StyleUtility.DEFAULT_KEY).toString();
        else
            shadowString = _shadows.entrySet()
                                    .stream()
                                    .map(e -> e.getKey() + ": " + e.getValue())
                                    .collect(Collectors.joining(", ", "shadows=[", "]"));

        String painterString;
        if ( _painters.size() == 1 )
            painterString = _painters.get(StyleUtility.DEFAULT_KEY + "_" + PainterStyle.none().layer().name()).toString();
        else
            painterString = _painters.entrySet()
                                    .stream()
                                    .map(e -> e.getKey() + ": " + e.getValue())
                                    .collect(Collectors.joining(", ", "painters=[", "]"));

        String gradientString;
        if ( _gradients.size() == 1 )
            gradientString = _gradients.get(StyleUtility.DEFAULT_KEY).toString();
        else
            gradientString = _gradients.entrySet()
                    .stream()
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .collect(Collectors.joining(", ", "gradients=[", "]"));

        String imagesString;
        if ( _images.size() == 1 )
            imagesString = _images.get(StyleUtility.DEFAULT_KEY).toString();
        else
            imagesString = _images.entrySet()
                    .stream()
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .collect(Collectors.joining(", ", "images=[", "]"));

        return "Style[" +
                    _layout         + ", " +
                    _border         + ", " +
                    _base           + ", " +
                    _font           + ", " +
                    _dimensionality + ", " +
                    shadowString    + ", " +
                    painterString   + ", " +
                    gradientString  + ", " +
                    imagesString   +
                "]";
    }

    static class Report
    {
        public final boolean noLayoutStyle;
        public final boolean noBorderStyle;
        public final boolean noBaseStyle;
        public final boolean noFontStyle;
        public final boolean noDimensionalityStyle;
        public final boolean noShadowStyle;
        public final boolean noPainters;
        public final boolean noGradients;
        public final boolean noImages;

        public final boolean allShadowsAreBorderShadows;
        public final boolean allGradientsAreBorderGradients;
        public final boolean allPaintersAreBorderPainters;
        public final boolean allImagesAreBorderImages;


        private Report( Style style ) {
            this.noLayoutStyle         = Style.none().hasEqualLayoutAs(style);
            this.noBorderStyle         = Style.none().hasEqualBorderAs(style);
            this.noBaseStyle           = Style.none().hasEqualBaseAs(style);
            this.noFontStyle           = Style.none().hasEqualFontAs(style);
            this.noDimensionalityStyle = Style.none().hasEqualDimensionalityAs(style);
            this.noShadowStyle         = Style.none().hasEqualShadowsAs(style);
            this.noPainters            = Style.none().hasEqualPaintersAs(style);
            this.noGradients           = Style.none().hasEqualGradientsAs(style);
            this.noImages              = Style.none().hasEqualGroundsAs(style);

            this.allShadowsAreBorderShadows     = style._shadows.values().stream().allMatch( s -> s.layer() == UI.Layer.BORDER );
            this.allGradientsAreBorderGradients = style._gradients.values().stream().allMatch(s -> s.layer() == UI.Layer.BORDER );
            this.allPaintersAreBorderPainters   = style._painters.values().stream().allMatch( s -> s.layer() == UI.Layer.BORDER );
            this.allImagesAreBorderImages       = style._images.values().stream().allMatch(s -> s.layer() == UI.Layer.BORDER );
        }

        public boolean isNotStyled() {
            return noLayoutStyle          &&
                   noBorderStyle          &&
                   noBaseStyle            &&
                   noFontStyle            &&
                   noDimensionalityStyle  &&
                   noShadowStyle          &&
                   noPainters             &&
                   noGradients            &&
                   noImages;
        }

        public boolean onlyDimensionalityAndOrLayoutIsStyled() {
            return this.onlyDimensionalityIsStyled() ||
                   this.onlyLayoutIsStyled()         ||
                   this.onlyLayoutAndDimensionalityIsStyled();
        }

        public boolean onlyLayoutIsStyled() {
            return !noLayoutStyle          &&
                    noBorderStyle          &&
                    noBaseStyle            &&
                    noFontStyle            &&
                    noDimensionalityStyle  &&
                    noShadowStyle          &&
                    noPainters             &&
                    noGradients            &&
                    noImages;
        }


        public boolean onlyDimensionalityIsStyled() {
            return noLayoutStyle          &&
                   noBorderStyle          &&
                    noBaseStyle &&
                   noFontStyle            &&
                   !noDimensionalityStyle &&
                   noShadowStyle          &&
                   noPainters             &&
                   noGradients            &&
                   noImages;
        }

        public boolean onlyLayoutAndDimensionalityIsStyled() {
            return !noLayoutStyle         &&
                   noBorderStyle          &&
                   noBaseStyle            &&
                   noFontStyle            &&
                   !noDimensionalityStyle &&
                   noShadowStyle          &&
                   noPainters             &&
                   noGradients            &&
                   noImages;
        }
    }

}
