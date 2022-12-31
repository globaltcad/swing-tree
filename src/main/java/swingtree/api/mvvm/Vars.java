package swingtree.api.mvvm;

import java.util.Objects;
import java.util.function.Function;

/**
 *  A list of mutable properties.
 * @param <T> The type of the properties.
 */
public interface Vars<T> extends Vals<T>
{
    @SuppressWarnings("unchecked")
    static <T> Vars<T> of( Class<T> type, Var<T>... vars ) {
        return AbstractVariables.of( false, type, vars );
    }

    @SuppressWarnings("unchecked")
    static <T> Vars<T> of( Var<T> first, Var<T>... rest ) {
        return AbstractVariables.of( false, first, rest );
    }

    @SuppressWarnings("unchecked")
    static <T> Vars<T> of( T first, T... rest ) {
        return AbstractVariables.of( false, first, rest );
    }

    static <T> Vars<T> of( Class<T> type, Iterable<Var<T>> vars ) {
        return AbstractVariables.of( false, type, vars );
    }

    @SuppressWarnings("unchecked")
    static <T> Vars<T> ofNullable( Class<T> type, Var<T>... vars ) {
        return AbstractVariables.ofNullable( false, type, vars );
    }

    @SuppressWarnings("unchecked")
    static <T> Vars<T> ofNullable( Class<T> type, T... vars ) {
        return AbstractVariables.ofNullable( false, type, vars );
    }

    @SuppressWarnings("unchecked")
    static <T> Vars<T> ofNullable( Var<T> first, Var<T>... vars ) {
        return AbstractVariables.ofNullable( false, first, vars );
    }

    @Override Var<T> at( int index );

    @Override default Var<T> first() { return at(0); }

    @Override default Var<T> last() { return at(size() - 1); }

    default Vars<T> add( T value ) { return add( Var.of(value) ); }

    default Vars<T> remove( Var<T> value ) { return removeAt( indexOf(value) ); }

    default Vars<T> remove( T value ) { return removeAt( indexOf(Var.of(value)) ); }

    default Vars<T> removeFirst() { return removeAt(0); }

    default Vars<T> removeLast() { return removeAt(size() - 1); }

    default Vars<T> removeLast( int count )
    {
        for ( int i = 0; i < count; i++ ) removeLast();
        return this;
    }

    default Vars<T> removeFirst( int count )
    {
        for ( int i = 0; i < count; i++ ) removeFirst();
        return this;
    }

    default Vars<T> add( Var<T> value ) { return addAt( size(), value ); }

    default Vars<T> addAt( int index, T value ) { return addAt(index, Var.of(value)); }

    default Vars<T> setAt( int index, T value ) { return setAt(index, Var.of(value)); }

    default Vars<T> addAll( Vals<T> values )
    {
        for ( T v : values ) add(v);
        return this;
    }

    @SuppressWarnings("unchecked")
    default Vars<T> addAll( T... values )
    {
        for ( T v : values ) add(v);
        return this;
    }

    Vars<T> removeAt( int index );

    Vars<T> addAt( int index, Var<T> value );

    Vars<T> setAt( int index, Var<T> value );

    Vars<T> clear();

    /**
     *  Use this for mapping a list of properties to another list of properties.
     */
    @Override default Vars<T> map( Function<T,T> mapper ) {
        Objects.requireNonNull(mapper);
        @SuppressWarnings("unchecked")
        Var<T>[] vars = new Var[size()];
        int i = 0;
        for( T v : this ) vars[i++] = Var.of( mapper.apply(v) );
        return Vars.of( type(), vars );
    }

    /**
     *  Use this for mapping a list of properties to another list of properties.
     */
    @Override default <U> Vars<U> mapTo( Class<U> type, Function<T,U> mapper ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(mapper);
        @SuppressWarnings("unchecked")
        Var<U>[] vars = new Var[size()];
        for ( int i = 0; i < size(); i++ )
            vars[i] = at(i).mapTo( type, mapper );
        return Vars.of( type, vars );
    }
}
