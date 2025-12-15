package swingtree.style;

import java.util.Objects;

/**
 *  An immutable value container that pairs a name with a
 *  style where the name is a string that is supposed to uniquely identify
 *  the style in a collection of named styles.
 *
 * @param <S> The type of the style.
 */
final class NamedConf<S> implements Simplifiable<NamedConf<S>>
{
    static <S> NamedConf<S> of(String name, S style ) {
        return new NamedConf<>( name, style );
    }

    private final String _name;
    private final S      _style;


    private NamedConf(String name, S style ) {
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
        NamedConf<?> rhs = (NamedConf<?>) obj;
        return Objects.equals(_name, rhs._name) &&
               Objects.equals(_style, rhs._style);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"[" +
                    "name="  + _name  +", "+
                    "style=" + (_style instanceof Pooled ? ((Pooled<?>)_style).get() : _style) +
                "]";
    }

    @Override
    public NamedConf<S> simplified() {
        if ( _style instanceof Simplifiable ) {
            S simplifiedStyle = ((Simplifiable<S>)_style).simplified();
            if (simplifiedStyle == _style)
                return this;
            return new NamedConf<>(_name, simplifiedStyle);
        }
        if ( _style instanceof Pooled ) {
            Pooled<Object> pooled = (Pooled<Object>) _style;
            pooled = pooled.map( it -> {
                if ( it instanceof Simplifiable ) {
                    return ((Simplifiable<S>)it).simplified();
                }
                return it;
            });
            pooled = pooled.intern();
            if ( pooled != _style )
                return new NamedConf<>(_name, (S)pooled);
        }
        return this;
    }
}
