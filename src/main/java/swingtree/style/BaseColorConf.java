package swingtree.style;

import java.awt.Color;
import java.util.Objects;
import java.util.Optional;

final class BaseColorConf
{
    private static final BaseColorConf _NONE = new BaseColorConf(null, null, null);

    static BaseColorConf none() { return _NONE; }

    private final Color _foundationColor;
    private final Color _backgroundColor;
    private final Color _borderColor;

    static BaseColorConf of(
        Color foundationColor,
        Color backgroundColor,
        Color borderColor
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
        Color foundationColor,
        Color backgroundColor,
        Color borderColor
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
