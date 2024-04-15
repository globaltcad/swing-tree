package swingtree.style;

import org.jspecify.annotations.Nullable;
import swingtree.api.Layout;

import java.util.Objects;
import java.util.Optional;

/**
 *  A style that defines the layout of a component.
 *  The layout manager of a component will use this information
 *  to determine the actual layout of the component in the layout.
 **/
final class LayoutConf
{
    private static final LayoutConf _NONE = new LayoutConf(Layout.unspecific(), null, null, null);

    static LayoutConf none() { return _NONE; }


    private final Layout          _layout;

    private final @Nullable Object _constraint;
    private final @Nullable Float  _alignmentX;
    private final @Nullable Float  _alignmentY;


    private LayoutConf(
        Layout           layout,
        @Nullable Object constraint,
        @Nullable Float  alignmentX,
        @Nullable Float  alignmentY
    ) {
        _layout     = Objects.requireNonNull(layout);
        _constraint = constraint;
        _alignmentX = alignmentX;
        _alignmentY = alignmentY;
    }

    Layout layout() { return _layout; }

    Optional<Object> constraint() { return Optional.ofNullable(_constraint); }

    Optional<Float> alignmentX() { return Optional.ofNullable(_alignmentX); }

    Optional<Float> alignmentY() { return Optional.ofNullable(_alignmentY); }

    LayoutConf layout(Layout installer ) { return new LayoutConf(installer, _constraint, _alignmentX, _alignmentY); }

    LayoutConf constraint(Object constraint ) { return new LayoutConf(_layout, constraint, _alignmentX, _alignmentY); }

    LayoutConf alignmentX(Float alignmentX ) { return new LayoutConf(_layout, _constraint, alignmentX, _alignmentY); }

    LayoutConf alignmentY(Float alignmentY ) { return new LayoutConf(_layout, _constraint, _alignmentX, alignmentY); }

    @Override
    public int hashCode() { return Objects.hash(_layout, _constraint, _alignmentX, _alignmentY); }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        LayoutConf other = (LayoutConf) obj;
        return Objects.equals(_layout,            other._layout)         &&
               Objects.equals(_constraint,        other._constraint)     &&
               Objects.equals(_alignmentX,        other._alignmentX)     &&
               Objects.equals(_alignmentY,        other._alignmentY);
    }

    @Override
    public String toString() {
        if ( this.equals(_NONE) )
            return this.getClass().getSimpleName() + "[NONE]";
        return this.getClass().getSimpleName() + "[" +
                    "layout="     + _layout                                   + ", " +
                    "constraint=" + (_constraint == null ? "?" : _constraint) + ", " +
                    "alignmentX=" + (_alignmentX == null ? "?" : _alignmentX) + ", " +
                    "alignmentY=" + (_alignmentY == null ? "?" : _alignmentY) +
               "]";
    }

}
