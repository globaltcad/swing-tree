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

    /*
     *  A lazily computed, cached hash. These configurations form a deep tree which is
     *  hashed on the hot path - every paint interns a cache key whose hash walks all of
     *  it - while a resize only ever replaces the component's bounds and leaves the style
     *  tree itself untouched, so the very same nodes are re-hashed frame after frame.
     *  Caching it per node turns that walk into a single field read per child.
     *
     *  Deliberately one non-volatile int with 0 as the "not computed yet" sentinel, and
     *  not an int plus a boolean flag: with two fields a racing thread may observe the
     *  flag already set while the value is still stale, whereas with one field a race can
     *  only ever make a second thread recompute the identical value. This is exactly what
     *  String.hashCode() does, and it is sound only because these objects are immutable.
     */
    private int _hashCode = 0; // Lazily computed, 0 means "not computed yet".



    private NamedConf(String name, S style ) {
        _name = Objects.requireNonNull(name);
        _style = Objects.requireNonNull(style);
    }

    String name() { return _name; }

    S style() { return _style; }


    @Override
    public int hashCode() {
        int hash = _hashCode;
        if ( hash == 0 )
            _hashCode = hash = Objects.hash(_name, _style);
        return hash;
    }

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
        if ( this.isNone() ) {
            return this;
        }
        if ( _style instanceof Simplifiable ) {
            Simplifiable<S> asSimplifiable = ((Simplifiable<S>)_style);
            S simplifiedStyle = asSimplifiable.simplified();
            if ( ((Simplifiable<Object>)simplifiedStyle).isNone() )
                return new NamedConf<>(_simplifiedName(), simplifiedStyle);
            else {
                if (simplifiedStyle == _style)
                    return this;
                return new NamedConf<>(_name, simplifiedStyle);
            }
        }
        if ( _style instanceof Pooled ) {
            Pooled<Object> pooled = (Pooled<Object>) _style;
            if ( pooled.get() instanceof Simplifiable<?> ) {
                pooled = pooled.map( it -> ((Simplifiable<S>)it).simplified());
                Simplifiable<?> simplifiable = (Simplifiable<?>)pooled.get();
                if (simplifiable.isNone() )
                    return new NamedConf<>(_simplifiedName(), (S)pooled);
            }
            pooled = pooled.intern();
            if ( pooled != _style )
                return new NamedConf<>(_name, (S)pooled);
        }
        return this;
    }

    private String _simplifiedName() {
        return StyleUtil.DEFAULT_KEY.equals(_name) ? StyleUtil.DEFAULT_KEY : "";
        // Note: The default style can not be simplified away entirely! The default always exists!
    }

    @Override
    public boolean isNone() {
        boolean hasName = !_name.isEmpty();
        if ( hasName )
            return false;

        Simplifiable<?> asSimplifiable = null;
        if ( _style instanceof Simplifiable<?> ) {
            asSimplifiable = ((Simplifiable<?>)_style);
        }
        if ( _style instanceof Pooled<?> && ((Pooled<?>)_style).get() instanceof Simplifiable<?> ) {
            asSimplifiable = ((Simplifiable<?>)((Pooled<?>)_style).get());
        }
        if ( asSimplifiable != null )
            return asSimplifiable.isNone();
        return false;
    }
}
