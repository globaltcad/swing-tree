package swingtree.style;

import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.util.Objects;
import java.util.Optional;

final class BaseColorConf
{
    private static final BaseColorConf _NONE = new BaseColorConf(null, null, null);

    static BaseColorConf none() { return _NONE; }

    private final @Nullable Color _foundationColor;
    private final @Nullable Color _backgroundColor;
    private final @Nullable Color _borderColor;

    static BaseColorConf of(
        @Nullable Color foundationColor,
        @Nullable Color backgroundColor,
        @Nullable Color borderColor
    ) {
        if (
            foundationColor == null &&
            backgroundColor == null &&
            borderColor     == null
        )
            return _NONE;
        else
            return new BaseColorConf(foundationColor, backgroundColor, borderColor);
    }

    BaseColorConf(
        @Nullable Color foundationColor,
        @Nullable Color backgroundColor,
        @Nullable Color borderColor
    ) {
        _foundationColor = foundationColor;
        _backgroundColor = backgroundColor;
        _borderColor     = borderColor;
    }

    Optional<Color> foundationColor() { return Optional.ofNullable(_foundationColor); }

    Optional<Color> backgroundColor() { return Optional.ofNullable(_backgroundColor); }

    Optional<Color> borderColor() { return Optional.ofNullable(_borderColor); }


    @Override
    public int hashCode() { return Objects.hash(_foundationColor, _backgroundColor, _borderColor); }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        BaseColorConf rhs = (BaseColorConf) obj;
        return Objects.equals(_foundationColor, rhs._foundationColor) &&
               Objects.equals(_backgroundColor, rhs._backgroundColor) &&
               Objects.equals(_borderColor,     rhs._borderColor);
    }
}
