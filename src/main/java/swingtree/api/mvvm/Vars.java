package swingtree.api.mvvm;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 *  A list of mutable properties.
 * @param <T> The type of the properties.
 */
public interface Vars<T> extends Vals<T>
{
    static <T> Vars<T> of( Class<T> type, Var<T>... vars ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(vars);
        return new AbstractVariables<T>(type, false, vars ){};
    }

    static <T> Vars<T> of( Var<T> first, Var<T>... rest ) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(rest);
        Var<T>[] vars = new Var[rest.length+1];
        vars[0] = first;
        System.arraycopy(rest, 0, vars, 1, rest.length);
        return of(first.type(), vars);
    }

    static <T> Vars<T> of( T first, T... rest ) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(rest);
        Var<T>[] vars = new Var[rest.length+1];
        vars[0] = Var.of(first);
        for ( int i = 0; i < rest.length; i++ )
            vars[ i + 1 ] = Var.of( rest[ i ] );
        return of(vars[0].type(), vars);
    }

    static <T> Vars<T> of( Class<T> type, Iterable<Var<T>> vars ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(vars);
        List<Var<T>> list = new ArrayList<>();
        vars.forEach( list::add );
        Var<T>[] array = new Var[list.size()];
        return new AbstractVariables<T>(type, false, list.toArray(array) ){};
    }

    static <T> Vars<T> ofNullable( Class<T> type, Var<T>... vars ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(vars);
        return new AbstractVariables<T>(type, true, vars ){};
    }

    static <T> Vars<T> ofNullable( Class<T> type, T... vars ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(vars);
        Var<T>[] array = new Var[vars.length];
        for ( int i = 0; i < vars.length; i++ )
            array[i] = Var.ofNullable(type, vars[i]);
        return new AbstractVariables<T>(type, true, array ){};
    }

    static <T> Vars<T> ofNullable( Var<T> first, Var<T>... vars ) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(vars);
        Var<T>[] array = new Var[vars.length+1];
        array[0] = first;
        System.arraycopy(vars, 0, array, 1, vars.length);
        return ofNullable(first.type(), array);
    }

    @Override Var<T> at( int index );

    @Override default Var<T> first() { return at(0); }

    @Override default Var<T> last() { return at(size() - 1); }

    default Vars<T> add( T value ) { return add( Var.of(value) ); }

    default Vars<T> remove( Var<T> value ) { return removeAt( indexOf(value) ); }

    default Vars<T> remove( T value ) { return removeAt( indexOf(Var.of(value)) ); }

    default Vars<T> removeFirst() { return removeAt(0); }

    default Vars<T> removeLast() { return removeAt(size() - 1); }

    default Vars<T> add( Var<T> value ) { return addAt( size(), value ); }

    default Vars<T> addAt( int index, T value ) { return addAt(index, Var.of(value)); }

    default Vars<T> setAt( int index, T value ) { return setAt(index, Var.of(value)); }

    default Vars<T> addAll( Vals<T> values )
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
        Var<U>[] vars = new Var[size()];
        for ( int i = 0; i < size(); i++ )
            vars[i] = at(i).mapTo( type, mapper );
        return Vars.of( type, vars );
    }
}
