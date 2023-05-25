package swingtree.style;

import java.awt.*;
import java.util.Objects;
import java.util.Optional;

public final class BackgroundStyle
{
    private static final BackgroundStyle _NONE = new BackgroundStyle(null, null, null);

    public static BackgroundStyle none() { return _NONE; }

    private final Color _color;
    private final Color _foundationColor;
    private final Painter _painter;

    private BackgroundStyle(
        Color color,
        Color foundation,
        Painter painter
    ) {
        _color           = color;
        _foundationColor = foundation;
        _painter = painter;
    }

    public Optional<Color> color() { return Optional.ofNullable(_color); }

    public Optional<Color> foundationColor() { return Optional.ofNullable(_foundationColor); }

    public Optional<Painter> painter() { return Optional.ofNullable(_painter); }

    public BackgroundStyle withColor( Color color ) { return new BackgroundStyle(color, _foundationColor, _painter); }

    public BackgroundStyle withFoundationColor( Color foundation ) { return new BackgroundStyle(_color, foundation, _painter); }

    public BackgroundStyle withPainter( Painter renderer ) { return new BackgroundStyle(_color, _foundationColor, renderer); }

    @Override
    public int hashCode() { return Objects.hash(_color, _foundationColor, _painter); }

    @Override
    public boolean equals(Object obj) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        BackgroundStyle rhs = (BackgroundStyle) obj;
        return Objects.equals(_color, rhs._color) &&
               Objects.equals(_foundationColor, rhs._foundationColor) &&
               Objects.equals(_painter, rhs._painter);
    }

    @Override
    public String toString() {
        return "BackgroundStyle[" +
                    "color="           + StyleUtility.toString(_color) + ", " +
                    "foundationColor=" + StyleUtility.toString(_foundationColor) + ", " +
                    "painter="         + _painter +
                "]";
    }
}
