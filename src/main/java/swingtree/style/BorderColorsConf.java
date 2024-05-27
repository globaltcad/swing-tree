package swingtree.style;

import com.google.errorprone.annotations.Immutable;
import swingtree.UI;

import java.awt.Color;
import java.util.Optional;

/**
 *  Contains the 4 possible colors of the 4 different parts
 *  of a border, namely the top, right, bottom and left border.
 *  The colors are stored in the order of top, right, bottom, left.
 *  Instead of null, the {@link swingtree.UI.Color#UNDEFINED}
 *  constant is used to indicate that a border color is not set.
 *  <p>
 *  The {@link #none()} method returns a singleton instance
 *  of this class with all border colors set to {@link swingtree.UI.Color#UNDEFINED}.
 */
@Immutable
final class BorderColorsConf
{
    private final Color _top;
    private final Color _right;
    private final Color _bottom;
    private final Color _left;

    private static final BorderColorsConf _NONE = new BorderColorsConf(
        UI.Color.UNDEFINED,
        UI.Color.UNDEFINED,
        UI.Color.UNDEFINED,
        UI.Color.UNDEFINED
    );

    static BorderColorsConf none() { return _NONE; }

    private BorderColorsConf(
        Color top,
        Color right,
        Color bottom,
        Color left
    ) {
        _top    = top;
        _right  = right;
        _bottom = bottom;
        _left   = left;
    }

    @SuppressWarnings("ReferenceEquality")
    static BorderColorsConf of(
        Color top,
        Color right,
        Color bottom,
        Color left
    ) {
        if (
            top    == UI.Color.UNDEFINED &&
            right  == UI.Color.UNDEFINED &&
            bottom == UI.Color.UNDEFINED &&
            left   == UI.Color.UNDEFINED
        )
            return _NONE;
        else
            return new BorderColorsConf(top, right, bottom, left);
    }

    public static BorderColorsConf of(
        Color homogeneous
    ) {
        return new BorderColorsConf(homogeneous, homogeneous, homogeneous, homogeneous);
    }

    @SuppressWarnings("ReferenceEquality")
    private boolean _isUndefined(Color c) {
        return c == UI.Color.UNDEFINED;
    }

    boolean isHomogeneous() {
        return _top.equals(_right) && _top.equals(_bottom) && _top.equals(_left);
    }

    Optional<Color> top() {
        return Optional.ofNullable(_isUndefined(_top) ? null : _top);
    }

    Optional<Color> right() {
        return Optional.ofNullable(_isUndefined(_right) ? null : _right);
    }

    Optional<Color> bottom() {
        return Optional.ofNullable(_isUndefined(_bottom) ? null : _bottom);
    }

    Optional<Color> left() {
        return Optional.ofNullable(_isUndefined(_left) ? null : _left);
    }

    @Override
    public int hashCode() {
        return _top.hashCode() + _right.hashCode() + _bottom.hashCode() + _left.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        BorderColorsConf rhs = (BorderColorsConf) obj;
        return _top.equals(rhs._top) &&
               _right.equals(rhs._right) &&
               _bottom.equals(rhs._bottom) &&
               _left.equals(rhs._left);
    }

    @Override
    public String toString() {
        if ( this.equals(_NONE) )
            return getClass().getSimpleName() + "[NONE]";
        else if ( isHomogeneous() )
            return getClass().getSimpleName() + "[" +
                    "all=" + _top +
                "]";
        else
            return getClass().getSimpleName() + "[" +
                    "top="    + _top + ", " +
                    "right="  + _right + ", " +
                    "bottom=" + _bottom + ", " +
                    "left="   + _left +
                "]";
    }
}
