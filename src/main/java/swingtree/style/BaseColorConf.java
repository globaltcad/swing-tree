package swingtree.style;

import com.google.errorprone.annotations.Immutable;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.util.Objects;
import java.util.Optional;

@Immutable
final class BaseColorConf
{
    private static final BaseColorConf _NONE = new BaseColorConf(null, null, BorderColorsConf.none());

    static BaseColorConf none() { return _NONE; }

    private final @Nullable Color  _foundationColor;
    private final @Nullable Color  _backgroundColor;
    private final BorderColorsConf _borderColors;

    static BaseColorConf of(
        @Nullable Color  foundationColor,
        @Nullable Color  backgroundColor,
        BorderColorsConf borderColors
    ) {
        if (
            foundationColor == null &&
            backgroundColor == null &&
            borderColors.equals(BorderColorsConf.none())
        )
            return _NONE;
        else
            return new BaseColorConf(foundationColor, backgroundColor, borderColors);
    }

    BaseColorConf(
        @Nullable Color  foundationColor,
        @Nullable Color  backgroundColor,
        BorderColorsConf borderColors
    ) {
        _foundationColor = foundationColor;
        _backgroundColor = backgroundColor;
        _borderColors    = Objects.requireNonNull(borderColors);
    }

    Optional<Color> foundationColor() {
        return Optional.ofNullable(_foundationColor);
    }

    Optional<Color> backgroundColor() {
        return Optional.ofNullable(_backgroundColor);
    }

    BorderColorsConf borderColor() {
        return _borderColors;
    }


    @Override
    public int hashCode() { return Objects.hash(_foundationColor, _backgroundColor, _borderColors); }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        BaseColorConf rhs = (BaseColorConf) obj;
        return Objects.equals(_foundationColor, rhs._foundationColor) &&
               Objects.equals(_backgroundColor, rhs._backgroundColor) &&
               Objects.equals(_borderColors,    rhs._borderColors);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" +
                "foundationColor=" + _foundationColor + ", " +
                "backgroundColor=" + _backgroundColor + ", " +
                "borderColor="     + _borderColors    +
            "]";
    }
}
