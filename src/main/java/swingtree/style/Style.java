package swingtree.style;

import java.awt.*;
import java.util.*;
import java.util.List;
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
                                            Collections.singletonMap(StyleUtility.DEFAULT_KEY,ShadowStyle.none())
                                        );

    public static Style none() { return _NONE; }

    private final LayoutStyle     _layout;
    private final BorderStyle     _border;
    private final BackgroundStyle _background;
    private final ForegroundStyle _foreground;
    private final FontStyle       _font;
    private final DimensionalityStyle _dimensionality;
    private final Map<String, ShadowStyle> _shadows = new TreeMap<>();


    private Style(
        LayoutStyle layout,
        BorderStyle border,
        BackgroundStyle background,
        ForegroundStyle foreground,
        FontStyle font,
        DimensionalityStyle dimensionality,
        Map<String, ShadowStyle> shadows
    ) {
        _layout         = layout;
        _border         = border;
        _background     = background;
        _foreground     = foreground;
        _font           = font;
        _dimensionality = dimensionality;
        _shadows.putAll(shadows);
    }

    Style _withLayout( LayoutStyle layout ) {
        return new Style(layout, _border, _background, _foreground, _font, _dimensionality, _shadows);
    }
    Style _withBorder( BorderStyle border ) {
        return new Style(_layout, border, _background, _foreground, _font, _dimensionality, _shadows);
    }
    Style _withBackground( BackgroundStyle background ) {
        return new Style(_layout, _border, background, _foreground, _font, _dimensionality, _shadows);
    }
    Style _withForeground( ForegroundStyle foreground ) {
        return new Style(_layout, _border, _background, foreground, _font, _dimensionality, _shadows);
    }
    Style _withFont( FontStyle font ) {
        return new Style(_layout, _border, _background, _foreground, font, _dimensionality, _shadows);
    }
    Style _withDimensionality( DimensionalityStyle dimensionality ) {
        return new Style(_layout, _border, _background, _foreground, _font, dimensionality, _shadows);
    }
    Style _withShadow( Map<String, ShadowStyle> shadows ) {
        return new Style(_layout, _border, _background, _foreground, _font, _dimensionality, shadows);
    }
    Style _withShadow( Function<ShadowStyle, ShadowStyle> styler ) {
        // A new map is created where all the styler is applied to all the values:
        Map<String, ShadowStyle> styledShadows = new TreeMap<>();
        _shadows.forEach( (key, value) -> styledShadows.put(key, styler.apply(value)) );
        return _withShadow(styledShadows);
    }

    LayoutStyle layout() { return _layout; }

    public Outline padding() { return _layout.padding(); }

    public Outline margin() { return _layout.margin(); }

    public BorderStyle border() { return _border; }

    public BackgroundStyle background() { return _background; }

    public ForegroundStyle foreground() { return _foreground; }

    public DimensionalityStyle dimensionality() { return _dimensionality; }

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
    public List<ShadowStyle> shadows() {
        return Collections.unmodifiableList(
                _shadows.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(Map.Entry::getValue)
                        .collect(Collectors.toList())
            );
    }

    Map<String, ShadowStyle> shadowsMap() { return _shadows; }

    public boolean anyVisibleShadows() {
        return _shadows.values().stream().anyMatch(s -> s.color().isPresent() && s.color().get().getAlpha() > 0 );
    }


    public Style foundationColor( Color color ) { return _withBackground(background().foundationColor(color)); }

    public Style backgroundColor( Color color ) { return _withBackground(background().color(color)); }

    public FontStyle font() { return _font; }

    @Override
    public int hashCode() {
        return Objects.hash(_layout, _border, _background, _foreground, _font, StyleUtility.mapHash(_shadows));
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == this ) return true;
        if ( obj == null ) return false;
        if ( !(obj instanceof Style) ) return false;
        Style other = (Style) obj;
        return Objects.equals(_layout,     other._layout    ) &&
               Objects.equals(_border,     other._border    ) &&
               Objects.equals(_background, other._background) &&
               Objects.equals(_foreground, other._foreground) &&
               Objects.equals(_font,       other._font      ) &&
                StyleUtility.mapEquals(_shadows,    other._shadows   );
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

        return "Style[" +
                    _layout     + ", " +
                    _border     + ", " +
                    _background + ", " +
                    _foreground + ", " +
                    shadowString + ", " +
                    _font       +
                "]";
    }

}
