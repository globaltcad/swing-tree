package swingtree.style;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class BackgroundStyle
{
    private static final BackgroundStyle _NONE = new BackgroundStyle(null, null, null, Collections.singletonMap(ShadowStyle.DEFAULT_KEY, ShadeStyle.none()));

    public static BackgroundStyle none() { return _NONE; }

    private final Color _color;
    private final Color _foundationColor;
    private final Painter _painter;
    private final Map<String, ShadeStyle> _shades = new TreeMap<>();


    private BackgroundStyle(
        Color color,
        Color foundation,
        Painter painter,
        Map<String, ShadeStyle> shades
    ) {
        _color           = color;
        _foundationColor = foundation;
        _painter         = painter;
        _shades.putAll(shades);
    }

    public Optional<Color> color() { return Optional.ofNullable(_color); }

    public Optional<Color> foundationColor() { return Optional.ofNullable(_foundationColor); }

    public Optional<Painter> painter() { return Optional.ofNullable(_painter); }

    public List<ShadeStyle> shades() { return new ArrayList<>(_shades.values()); }

    BackgroundStyle color( Color color ) { return new BackgroundStyle(color, _foundationColor, _painter, _shades); }

    BackgroundStyle foundationColor( Color foundation ) { return new BackgroundStyle(_color, foundation, _painter, _shades); }

    BackgroundStyle painter( Painter renderer ) { return new BackgroundStyle(_color, _foundationColor, renderer, _shades); }

    BackgroundStyle shade( Map<String, ShadeStyle> shades ) {
        Objects.requireNonNull(shades);
        return new BackgroundStyle(_color, _foundationColor, _painter, shades);
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


    @Override
    public int hashCode() { return Objects.hash(_color, _foundationColor, _painter, _mapHash(_shades)); }

    private int _mapHash( Map<String, ShadeStyle> map ) {
        return map.entrySet().stream().mapToInt(e -> Objects.hash(e.getKey(), e.getValue())).sum();
    }

    @Override
    public boolean equals(Object obj) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        BackgroundStyle rhs = (BackgroundStyle) obj;
        return Objects.equals(_color, rhs._color) &&
               Objects.equals(_foundationColor, rhs._foundationColor) &&
               Objects.equals(_painter, rhs._painter) &&
                _shadeEquals(_shades, rhs._shades);
    }

    private boolean _shadeEquals(Map<String, ShadeStyle> map1, Map<String, ShadeStyle> map2 ) {
        if ( map1.size() != map2.size() ) return false;
        for ( Map.Entry<String, ShadeStyle> entry : map1.entrySet() ) {
            if ( !map2.containsKey(entry.getKey()) ) return false;
            if ( !Objects.equals(entry.getValue(), map2.get(entry.getKey())) ) return false;
        }
        return true;
    }

    @Override
    public String toString() {

        String shadesString;
        if ( _shades.size() == 1 )
            shadesString = _shades.get(ShadowStyle.DEFAULT_KEY).toString();
        else
            shadesString = _shades.entrySet()
                                    .stream()
                                    .map(e -> e.getKey() + ": " + e.getValue())
                                    .collect(Collectors.joining(", ", "shades=[", "]"));

        return "BackgroundStyle[" +
                    "color="           + StyleUtility.toString(_color) + ", " +
                    "foundationColor=" + StyleUtility.toString(_foundationColor) + ", " +
                    "painter="         + _painter + ", " +
                    shadesString +
                "]";
    }
}
