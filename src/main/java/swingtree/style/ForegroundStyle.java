package swingtree.style;

import swingtree.api.style.Painter;

import java.awt.*;
import java.util.Objects;
import java.util.Optional;

public final class ForegroundStyle
{
    private static final ForegroundStyle _NONE = new ForegroundStyle(null, null);

    public static ForegroundStyle none() { return _NONE; }

    private final Color _color;
    private final Painter _painter;

    private ForegroundStyle(
        Color color,
        Painter painter
    ) {
        _color   = color;
        _painter = painter;
    }

    public Optional<Color> color() { return Optional.ofNullable(_color); }

    public Optional<Painter> painter() { return Optional.ofNullable(_painter); }

    public ForegroundStyle withColor( Color color ) { return new ForegroundStyle(color, _painter); }

    public ForegroundStyle withPainter( Painter painter ) { return new ForegroundStyle(_color, painter); }

    @Override
    public int hashCode() { return Objects.hash(_color, _painter); }

    @Override
    public boolean equals(Object obj) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        ForegroundStyle rhs = (ForegroundStyle) obj;
        return Objects.equals(_color, rhs._color) &&
               Objects.equals(_painter, rhs._painter);
    }

    @Override
    public String toString() {
        return "ForegroundStyle[" +
                    "color="   + StyleUtility.toString(_color) + ", " +
                    "painter=" + _painter +
                "]";
    }
}
