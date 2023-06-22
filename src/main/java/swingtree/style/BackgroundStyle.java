package swingtree.style;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class BackgroundStyle
{
    private static final BackgroundStyle _NONE = new BackgroundStyle(
                                                    null,
                                                    null,
                                                    Collections.singletonMap(StyleUtility.DEFAULT_KEY, Painter.none()),
                                                    Collections.singletonMap(StyleUtility.DEFAULT_KEY, ShadeStyle.none())
                                                );

    public static BackgroundStyle none() { return _NONE; }

    private final Color _color;
    private final Color _foundationColor;
    private final Map<String, Painter> _painters = new TreeMap<>();
    private final Map<String, ShadeStyle> _shades = new TreeMap<>();


    private BackgroundStyle(
        Color color,
        Color foundation,
        Map<String, Painter> painters,
        Map<String, ShadeStyle> shades
    ) {
        _color           = color;
        _foundationColor = foundation;
        _painters.putAll(painters);
        _shades.putAll(shades);
    }

    public Optional<Color> color() { return Optional.ofNullable(_color); }

    public Optional<Color> foundationColor() { return Optional.ofNullable(_foundationColor); }

    /**
     * @return An unmodifiable list of painters sorted by their names in ascending alphabetical order.
     */
    public List<Painter> painters() {
        return Collections.unmodifiableList(
                _painters
                        .entrySet()
                        .stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(Map.Entry::getValue)
                        .collect(Collectors.toList())
        );
    }

    public boolean hasCustomPainters() {
        return !( _painters.size() == 1 && Painter.none().equals(_painters.get(StyleUtility.DEFAULT_KEY)) );
    }

    public List<ShadeStyle> shades() {
        return Collections.unmodifiableList(
                _shades.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(Map.Entry::getValue)
                        .collect(Collectors.toList())
            );
    }

    BackgroundStyle color( Color color ) { return new BackgroundStyle(color, _foundationColor, _painters, _shades); }

    BackgroundStyle foundationColor( Color foundation ) { return new BackgroundStyle(_color, foundation, _painters, _shades); }

    BackgroundStyle painter( Map<String, Painter> painters ) { return new BackgroundStyle(_color, _foundationColor, painters, _shades); }

    BackgroundStyle shade( Map<String, ShadeStyle> shades ) {
        Objects.requireNonNull(shades);
        return new BackgroundStyle(_color, _foundationColor, _painters, shades);
    }

    public BackgroundStyle shade( String shadeName, Function<ShadeStyle, ShadeStyle> styler ) {
        Objects.requireNonNull(shadeName);
        Objects.requireNonNull(styler);
        ShadeStyle shadow = Optional.ofNullable(_shades.get(shadeName)).orElse(ShadeStyle.none());
        // We clone the shadow map:
        Map<String, ShadeStyle> newShadows = new HashMap<>(_shades);
        newShadows.put(shadeName, styler.apply(shadow));
        return shade(newShadows);
    }

    public BackgroundStyle painter( String painterName, Painter painter ) {
        Objects.requireNonNull(painterName);
        Objects.requireNonNull(painter);
        // We clone the painter map:
        Map<String, Painter> newPainters = new HashMap<>(_painters);
        newPainters.put(painterName, painter); // Existing painters are overwritten if they have the same name.
        return painter(newPainters);
    }

    @Override
    public int hashCode() { return Objects.hash(_color, _foundationColor, StyleUtility.mapHash(_painters), StyleUtility.mapHash(_shades)); }

    @Override
    public boolean equals(Object obj) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        BackgroundStyle rhs = (BackgroundStyle) obj;
        return Objects.equals(_color, rhs._color) &&
               Objects.equals(_foundationColor, rhs._foundationColor) &&
                StyleUtility.mapEquals(_painters, rhs._painters) &&
                StyleUtility.mapEquals(_shades, rhs._shades);
    }

    @Override
    public String toString() {

        String shadesString;
        if ( _shades.size() == 1 )
            shadesString = _shades.get(StyleUtility.DEFAULT_KEY).toString();
        else
            shadesString = _shades.entrySet()
                                    .stream()
                                    .map(e -> e.getKey() + ": " + e.getValue())
                                    .collect(Collectors.joining(", ", "shades=[", "]"));

        String painterString;
        if ( _painters.size() == 1 )
            painterString = "painter=" + StyleUtility.toString(_painters.values().iterator().next());
        else
            painterString = _painters.values()
                                     .stream()
                                     .map(StyleUtility::toString)
                                     .collect(Collectors.joining(", ", "painters=[", "]"));

        return "BackgroundStyle[" +
                    "color="           + StyleUtility.toString(_color) + ", " +
                    "foundationColor=" + StyleUtility.toString(_foundationColor) + ", " +
                    painterString + ", " +
                    shadesString +
                "]";
    }
}
