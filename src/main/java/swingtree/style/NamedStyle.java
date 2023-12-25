package swingtree.style;

import java.util.Objects;

/**
 *  An immutable value container that pairs a name with a
 *  style where the name is a string that is supposed to uniquely identify
 *  the style in a collection of named styles.
 *
 * @param <S> The type of the style.
 */
final class NamedStyle<S> implements Simplifiable<NamedStyle<S>>
{
    static <S> NamedStyle<S> of( String name, S style ) {
        return new NamedStyle<>( name, style );
    }

    private final String _name;
    private final S      _style;


    private NamedStyle( String name, S style ) {
        _name = Objects.requireNonNull(name);
        _style = Objects.requireNonNull(style);
    }

    String name() { return _name; }

    S style() { return _style; }


    @Override
    public int hashCode() { return Objects.hash(_name, _style); }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        NamedStyle<?> rhs = (NamedStyle<?>) obj;
        return Objects.equals(_name, rhs._name) &&
               Objects.equals(_style, rhs._style);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"[" +
                    "name="  + _name  +", "+
                    "style=" + _style +
                "]";
    }

    @Override
    public NamedStyle<S> simplified() {
        if ( _style instanceof Simplifiable ) {
            S simplifiedStyle = ((Simplifiable<S>)_style).simplified();
            if (simplifiedStyle == _style)
                return this;
            return new NamedStyle<>(_name, simplifiedStyle);
        }
        return this;
    }
}
