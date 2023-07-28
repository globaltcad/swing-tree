package swingtree.style;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *  An immutable, wither based settings container for {@link javax.swing.JComponent} styling.
 *  The styling in SwingTree is functional, meaning that changing a property
 *  of a {@link Style} instance will return a new {@link Style} instance with the
 *  updated property.
 *  <p>
 *  Here an example of how a {@link Style} instance is applied to a {@link javax.swing.JPanel}:
 *  <pre>{@code
 *  panel(FILL)
 *  .withStyle( it ->
 *      it.foundationColor(new Color(26,191,230))
 *        .backgroundColor(new Color(255,255,255))
 *        .padTop(30)
 *        .padLeft(35)
 *        .padRight(35)
 *        .padBottom(30)
 *        .borderRadius(25, 25)
 *        .borderWidth(3)
 *        .borderColor(new Color(0,102,255))
 *        .shadowColor(new Color(64,64,64))
 *        .shadowBlurRadius(6)
 *        .shadowSpreadRadius(5)
 *        .shadowInset(false)
 *  )
 *  }</pre>
 */
public final class Style
{
    private static final Style _NONE = new Style(
                                            LayoutStyle.none(),
                                            BorderStyle.none(),
                                            BackgroundStyle.none(),
                                            ForegroundStyle.none(),
                                            FontStyle.none(),
                                            DimensionalityStyle.none(),
                                            null,
                                            Collections.singletonMap(StyleUtility.DEFAULT_KEY,ShadowStyle.none()),
                                            Collections.singletonMap(StyleUtility.DEFAULT_KEY + "_" + PainterStyle.none().layer().name(),PainterStyle.none()),
                                            Collections.singletonMap(StyleUtility.DEFAULT_KEY, GradientStyle.none())
    );

    public static Style none() { return _NONE; }

    private final LayoutStyle               _layout;
    private final BorderStyle               _border;
    private final BackgroundStyle           _background;
    private final ForegroundStyle           _foreground;
    private final FontStyle                 _font;
    private final DimensionalityStyle       _dimensionality;
    private final Cursor                    _cursor;
    private final Map<String, ShadowStyle>  _shadows  = new TreeMap<>();
    private final Map<String, PainterStyle> _painters = new TreeMap<>();
    private final Map<String, GradientStyle>   _shades   = new TreeMap<>();




    private Style(
            LayoutStyle               layout,
            BorderStyle               border,
            BackgroundStyle           background,
            ForegroundStyle           foreground,
            FontStyle                 font,
            DimensionalityStyle       dimensionality,
            Cursor                    cursor,
            Map<String, ShadowStyle>  shadows,
            Map<String, PainterStyle> painters,
            Map<String, GradientStyle>   shades
    ) {
        _layout         = layout;
        _border         = border;
        _background     = background;
        _foreground     = foreground;
        _font           = font;
        _dimensionality = dimensionality;
        _cursor         = cursor;
        _shadows.putAll(shadows);
        _painters.putAll(painters);
        _shades.putAll(shades);
    }

    Style _withLayout( LayoutStyle layout ) {
        return new Style(layout, _border, _background, _foreground, _font, _dimensionality, _cursor, _shadows, _painters, _shades);
    }
    Style _withBorder( BorderStyle border ) {
        return new Style(_layout, border, _background, _foreground, _font, _dimensionality, _cursor, _shadows, _painters, _shades);
    }
    Style _withBackground( BackgroundStyle background ) {
        return new Style(_layout, _border, background, _foreground, _font, _dimensionality, _cursor, _shadows, _painters, _shades);
    }
    Style _withForeground( ForegroundStyle foreground ) {
        return new Style(_layout, _border, _background, foreground, _font, _dimensionality, _cursor, _shadows, _painters, _shades);
    }
    Style _withFont( FontStyle font ) {
        return new Style(_layout, _border, _background, _foreground, font, _dimensionality, _cursor, _shadows, _painters, _shades);
    }
    Style _withDimensionality( DimensionalityStyle dimensionality ) {
        return new Style(_layout, _border, _background, _foreground, _font, dimensionality, _cursor, _shadows, _painters, _shades);
    }
    Style _withCursor( Cursor cursor ) {
        return new Style(_layout, _border, _background, _foreground, _font, _dimensionality, cursor, _shadows, _painters, _shades);
    }

    Style _withShadow( Map<String, ShadowStyle> shadows ) {
        return new Style(_layout, _border, _background, _foreground, _font, _dimensionality, _cursor, shadows, _painters, _shades);
    }
    Style _withShadow( Function<ShadowStyle, ShadowStyle> styler ) {
        // A new map is created where all the styler is applied to all the values:
        Map<String, ShadowStyle> styledShadows = new TreeMap<>();
        _shadows.forEach( (key, value) -> styledShadows.put(key, styler.apply(value)) );
        return _withShadow(styledShadows);
    }

    public LayoutStyle layout() { return _layout; }

    public Outline padding() { return _layout.padding(); }

    public Outline margin() { return _layout.margin(); }

    public BorderStyle border() { return _border; }

    public BackgroundStyle background() { return _background; }

    public ForegroundStyle foreground() { return _foreground; }

    public DimensionalityStyle dimensionality() { return _dimensionality; }

    public Optional<Cursor> cursor() { return Optional.ofNullable(_cursor); }

    /**
     * @return The default shadow style.
     */
    public ShadowStyle shadow() { return _shadows.get(StyleUtility.DEFAULT_KEY); }

    /**
     * @param shadowName The name of the shadow style to retrieve.
     * @return The shadow style with the provided name.
     */
    public ShadowStyle shadow(String shadowName) {
        Objects.requireNonNull(shadowName);
        return _shadows.get(shadowName);
    }

    /**
     * @return An unmodifiable list of all shadow styles sorted by their names in ascending alphabetical order.
     */
    List<ShadowStyle> shadows(Layer layer) {
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


    public Style foundationColor( Color color ) { return _withBackground(background().foundationColor(color)); }

    public Style backgroundColor( Color color ) { return _withBackground(background().color(color)); }

    public FontStyle font() { return _font; }

    /**
     * @return An unmodifiable list of painters sorted by their names in ascending alphabetical order.
     */
    List<Painter> painters(Layer layer) {
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


    boolean hasCustomBackgroundPainters() {
        return _painters.values().stream().anyMatch(p -> p.layer() == Layer.BACKGROUND && !Painter.none().equals(p.painter()));
    }

    boolean hasCustomForegroundPainters() {
        return _painters.values().stream().anyMatch(p -> p.layer() == Layer.FOREGROUND && !Painter.none().equals(p.painter()));
    }

    List<GradientStyle> gradients(Layer layer) {
        return Collections.unmodifiableList(
                _shades.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(Map.Entry::getValue)
                        .filter( s -> s.layer() == layer )
                        .collect(Collectors.toList())
        );
    }

    boolean hasCustomBackgroundShades() {
        return !( _shades.size() == 1 && GradientStyle.none().equals(_shades.get(StyleUtility.DEFAULT_KEY)) );
    }

    Style painter( Map<String, PainterStyle> painters ) {
        Objects.requireNonNull(painters);
        return new Style(_layout, _border, _background, _foreground, _font, _dimensionality, _cursor, _shadows, painters, _shades);
    }

    Style gradient( Map<String, GradientStyle> shades ) {
        Objects.requireNonNull(shades);
        return new Style(_layout, _border, _background, _foreground, _font, _dimensionality, _cursor, _shadows, _painters, shades);
    }

    Style gradient( String shadeName, Function<GradientStyle, GradientStyle> styler ) {
        Objects.requireNonNull(shadeName);
        Objects.requireNonNull(styler);
        GradientStyle shadow = Optional.ofNullable(_shades.get(shadeName)).orElse(GradientStyle.none());
        // We clone the shadow map:
        Map<String, GradientStyle> newShadows = new HashMap<>(_shades);
        newShadows.put(shadeName, styler.apply(shadow));
        return gradient(newShadows);
    }

    Style painter( String painterName, Layer layer, Painter painter ) {
        Objects.requireNonNull(painterName);
        Objects.requireNonNull(painter);
        painterName = painterName + "_" + layer.name();
        // We clone the painter map:
        Map<String, PainterStyle> newPainters = new HashMap<>(_painters);
        newPainters.put(painterName, PainterStyle.none().painter(painter).layer(layer)); // Existing painters are overwritten if they have the same name.
        return painter(newPainters);
    }

    Style scale( double scale ) {
        return new Style(
                    _layout._scale(scale),
                    _border._scale(scale),
                    _background,
                    _foreground,
                    _font._scale(scale),
                    _dimensionality._scale(scale),
                    _cursor,
                    _shadows.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()._scale(scale))),
                    _painters,
                    _shades
                );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                _layout, _border, _background, _foreground, _font, _dimensionality, _cursor,
                StyleUtility.mapHash(_shadows), StyleUtility.mapHash(_painters), StyleUtility.mapHash(_shades)
            );
    }

    boolean hasEqualLayoutAs( Style otherStyle ) {
        return Objects.equals(_layout, otherStyle._layout);
    }

    boolean hasEqualBorderAs( Style otherStyle ) {
        return Objects.equals(_border, otherStyle._border);
    }

    boolean hasEqualBackgroundAs( Style otherStyle ) {
        return Objects.equals(_background, otherStyle._background);
    }

    boolean hasEqualForegroundAs( Style otherStyle ) {
        return Objects.equals(_foreground, otherStyle._foreground);
    }

    boolean hasEqualFontAs( Style otherStyle ) {
        return Objects.equals(_font, otherStyle._font);
    }

    boolean hasEqualDimensionalityAs( Style otherStyle ) {
        return Objects.equals(_dimensionality, otherStyle._dimensionality);
    }

    boolean hasEqualCursorAs( Style otherStyle ) {
        return Objects.equals(_cursor, otherStyle._cursor);
    }

    boolean hasEqualShadowsAs( Style otherStyle ) {
        return StyleUtility.mapEquals(_shadows, otherStyle._shadows);
    }

    boolean hasEqualPaintersAs( Style otherStyle ) {
        return StyleUtility.mapEquals(_painters, otherStyle._painters);
    }

    boolean hasEqualShadesAs( Style otherStyle ) {
        return StyleUtility.mapEquals(_shades, otherStyle._shades);
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == this ) return true;
        if ( obj == null ) return false;
        if ( !(obj instanceof Style) ) return false;
        Style other = (Style) obj;
        return hasEqualLayoutAs(other)         &&
               hasEqualBorderAs(other)         &&
               hasEqualBackgroundAs(other)     &&
               hasEqualForegroundAs(other)     &&
               hasEqualFontAs(other)           &&
               hasEqualDimensionalityAs(other) &&
               hasEqualCursorAs(other)         &&
               hasEqualShadowsAs(other)        &&
               hasEqualPaintersAs(other)       &&
               hasEqualShadesAs(other);
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

        String shadeString;
        if ( _shades.size() == 1 )
            shadeString = _shades.get(StyleUtility.DEFAULT_KEY).toString();
        else
            shadeString = _shades.entrySet()
                    .stream()
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .collect(Collectors.joining(", ", "shades=[", "]"));

        return "Style[" +
                    _layout         + ", " +
                    _border         + ", " +
                    _background     + ", " +
                    _foreground     + ", " +
                    _font           + ", " +
                    _dimensionality + ", " +
                    ( _cursor == null ? "" : _cursor ) +
                    shadowString    + ", " +
                    painterString   + ", " +
                    shadeString +
                "]";
    }

}
