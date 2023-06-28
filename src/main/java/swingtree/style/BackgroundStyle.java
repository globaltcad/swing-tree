package swingtree.style;

import java.awt.*;
import java.util.Objects;
import java.util.Optional;

public final class BackgroundStyle
{
    private static final BackgroundStyle _NONE = new BackgroundStyle(
                                                    null,
                                                    null
                                                );

    public static BackgroundStyle none() { return _NONE; }

    private final Color _color;
    private final Color _foundationColor;

    private BackgroundStyle(
        Color color,
        Color foundation
    ) {
        _color           = color;
        _foundationColor = foundation;
    }

    public Optional<Color> color() { return Optional.ofNullable(_color); }

    public Optional<Color> foundationColor() { return Optional.ofNullable(_foundationColor); }

    BackgroundStyle color( Color color ) { return new BackgroundStyle(color, _foundationColor); }

    BackgroundStyle foundationColor( Color foundation ) { return new BackgroundStyle(_color, foundation); }

    @Override
    public int hashCode() { return Objects.hash(_color, _foundationColor); }

    @Override
    public boolean equals(Object obj) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        BackgroundStyle rhs = (BackgroundStyle) obj;
        return Objects.equals(_color, rhs._color) &&
               Objects.equals(_foundationColor, rhs._foundationColor);
    }

    @Override
    public String toString() {
        return "BackgroundStyle[" +
                    "color="           + StyleUtility.toString(_color) + ", " +
                    "foundationColor=" + StyleUtility.toString(_foundationColor) +
                "]";
    }
}
