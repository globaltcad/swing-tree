package swingtree.style;

import java.util.Objects;
import java.util.Optional;

/**
 *  A style that defines the layout of a component.
 *  The layout manager of a component will use this information
 *  to determine the actual layout of the component in the layout.
 **/
public final class LayoutStyle
{
    private static final LayoutStyle _NONE = new LayoutStyle(Layout.unspecific(), null, null, null);

    public static LayoutStyle none() { return _NONE; }


    private final Layout _layout;

    private final Object _constraint;
    private final Float  _alignmentX;
    private final Float  _alignmentY;


    private LayoutStyle(
        Layout layout,
        Object constraint,
        Float alignmentX,
        Float alignmentY
    ) {
        _layout     = Objects.requireNonNull(layout);
        _constraint = constraint;
        _alignmentX = alignmentX;
        _alignmentY = alignmentY;
    }

    Layout layout() { return _layout; }

    public Optional<Object> constraint() { return Optional.ofNullable(_constraint); }

    Optional<Float> alignmentX() { return Optional.ofNullable(_alignmentX); }

    Optional<Float> alignmentY() { return Optional.ofNullable(_alignmentY); }

    LayoutStyle layout( Layout installer ) { return new LayoutStyle(installer, _constraint, _alignmentX, _alignmentY); }

    LayoutStyle constraint( Object constraint ) { return new LayoutStyle(_layout, constraint, _alignmentX, _alignmentY); }

    LayoutStyle alignmentX( Float alignmentX ) { return new LayoutStyle(_layout, _constraint, alignmentX, _alignmentY); }

    LayoutStyle alignmentY( Float alignmentY ) { return new LayoutStyle(_layout, _constraint, _alignmentX, alignmentY); }

    @Override
    public int hashCode() { return Objects.hash(_layout, _alignmentX, _alignmentY); }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        LayoutStyle other = (LayoutStyle) obj;
        return Objects.equals(_layout,            other._layout)         &&
               Objects.equals(_constraint,        other._constraint)     &&
               Objects.equals(_alignmentX,        other._alignmentX)     &&
               Objects.equals(_alignmentY,        other._alignmentY);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" +
                    "layout="     + _layout                                   + ", " +
                    "constraint=" + (_constraint == null ? "?" : _constraint) + ", " +
                    "alignmentX=" + (_alignmentX == null ? "?" : _alignmentX) + ", " +
                    "alignmentY=" + (_alignmentY == null ? "?" : _alignmentY) +
               "]";
    }

}
