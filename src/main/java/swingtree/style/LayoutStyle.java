package swingtree.style;

import java.util.Objects;
import java.util.Optional;

final class LayoutStyle
{
    private static final LayoutStyle _NONE = new LayoutStyle(null, null);

    public static LayoutStyle none() { return _NONE; }

    private final Float  _alignmentX;
    private final Float  _alignmentY;

    LayoutStyle(
        Float alignmentX,
        Float alignmentY
    ) {
        _alignmentX      = alignmentX;
        _alignmentY      = alignmentY;
    }

    public Optional<Float> alignmentX() { return Optional.ofNullable(_alignmentX); }

    public Optional<Float> alignmentY() { return Optional.ofNullable(_alignmentY); }

    LayoutStyle alignmentX( Float alignmentX ) { return new LayoutStyle(alignmentX, _alignmentY); }

    LayoutStyle alignmentY( Float alignmentY ) { return new LayoutStyle(_alignmentX, alignmentY); }

    @Override
    public int hashCode() {
        return Objects.hash(_alignmentX, _alignmentY);
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        LayoutStyle rhs = (LayoutStyle) obj;
        return Objects.equals(_alignmentX, rhs._alignmentX) &&
               Objects.equals(_alignmentY, rhs._alignmentY);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" +
                    "alignmentX=" + (_alignmentX == null ? "?" : _alignmentX) + ", " +
                    "alignmentY=" + (_alignmentY == null ? "?" : _alignmentY) +
               "]";
    }

}
