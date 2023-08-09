package swingtree.style;

import java.awt.*;
import java.util.Objects;
import java.util.Optional;

public final class ForegroundStyle
{
    private static final ForegroundStyle _NONE = new ForegroundStyle(null);

    public static ForegroundStyle none() { return _NONE; }

    private final Color _color;


    private ForegroundStyle(
        Color color
    ) {
        _color   = color;
    }

    public Optional<Color> foregroundColo() { return Optional.ofNullable(_color); }

    ForegroundStyle foregroundColo(Color color ) { return new ForegroundStyle(color); }

    @Override
    public int hashCode() { return Objects.hash(_color); }

    @Override
    public boolean equals(Object obj) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        ForegroundStyle rhs = (ForegroundStyle) obj;
        return Objects.equals(_color, rhs._color);
    }

    @Override
    public String toString() {
        return "ForegroundStyle[" +
                    "color="   + StyleUtility.toString(_color) +
                "]";
    }
}
