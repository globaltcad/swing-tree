package swingtree.api.mvvm;

import java.util.*;
import java.util.function.Function;

/**
 *  A list of mutable properties.
 *
 * @param <T> The type of the properties.
 */
public interface Vars<T> extends Vals<T>
{
    /**
     *  Creates a list of non-nullable properties from the supplied type and vararg values.
     *  This factory method requires that the type be specified because the
     *  compiler cannot infer the type from a potentially empty vararg array.
     *  @param type The type of the properties.
     *  @param vars The properties to add to the new Vars instance.
     *  @param <T> The type of the properties.
     *  @return A new Vars instance.
     */
    @SuppressWarnings("unchecked")
    static <T> Vars<T> of( Class<T> type, Var<T>... vars ) {
        return AbstractVariables.of( false, type, vars );
    }

    /**
     *  Creates an empty list of non-nullable properties from the supplied type.
     *  This factory method requires that the type be specified because the
     *  compiler cannot infer the type from a potentially empty vararg array.
     *  @param type The type of the properties.
     *              This is used to check if the item is of the correct type.
     *  @param <T> The type of the properties.
     *  @return A new Vars instance.
     */
    static <T> Vars<T> of( Class<T> type ) {
        return AbstractVariables.of( false, type );
    }

    /**
     *  Creates a list of non-nullable properties from one or more non-nullable properties.
     *  @param first The first property to add to the new Vars instance.
     *  @param rest The remaining properties to add to the new Vars instance.
     *  @param <T> The type of the properties.
     *  @return A new Vars instance.
     */
    @SuppressWarnings("unchecked")
    static <T> Vars<T> of( Var<T> first, Var<T>... rest ) {
        return AbstractVariables.of( false, first, rest );
    }

    /**
     *  Creates a list of non-nullable properties from one or more non-null values.
     *  @param first The first value to add to the new Vars instance.
     *  @param rest The remaining values to add to the new Vars instance.
     *  @param <T> The type of the values.
     *  @return A new Vars instance.
     */
    @SuppressWarnings("unchecked")
    static <T> Vars<T> of( T first, T... rest ) {
        return AbstractVariables.of( false, first, rest );
    }

    /**
     *  Creates a list of non-nullable properties from the supplied type and iterable of values.
     *  This factory method requires that the type be specified because the
     *  compiler cannot infer the type from a potentially empty iterable.
     *  @param type The type of the properties.
     *  @param vars The iterable of values.
     *  @param <T> The type of the properties.
     *  @return A new Vars instance.
     */
    static <T> Vars<T> of( Class<T> type, Iterable<Var<T>> vars ) {
        return AbstractVariables.of( false, type, vars );
    }

    /**
     *  Creates a list of nullable properties from the supplied type and varargs properties.
     *  This factory method requires that the type be specified because the
     *  compiler cannot infer the type from the null values.
     *  @param type The type of the properties.
     *  @param vars The properties to add to the new Vars instance.
     *              The properties may be nullable properties, but they may not be null themselves.
     *  @param <T> The type of the properties.
     *  @return A new Vars instance.
     */
    @SuppressWarnings("unchecked")
    static <T> Vars<T> ofNullable( Class<T> type, Var<T>... vars ) {
        return AbstractVariables.ofNullable( false, type, vars );
    }

    /**
     *  Creates an empty list of nullable properties from the supplied type.
     *  This factory method requires that the type be specified because the
     *  compiler cannot infer the type from a potentially empty vararg array.
     *  @param type The type of the properties.
     *              This is used to check if the item is of the correct type.
     *  @param <T> The type of the properties.
     *  @return A new Vars instance.
     */
    static <T> Vars<T> ofNullable( Class<T> type ) {
        return AbstractVariables.ofNullable( false, type );
    }

    /**
     *  Creates a list of nullable properties from the supplied type and values.
     *  This factory method requires that the type be specified because the
     *  compiler cannot infer the type from the null values.
     *  @param values The values to be wrapped by properties and then added to the new Vars instance.
     *                The values may be null.
     * @param <T> The type of the values.
     * @return A new Vars instance.
     */
    @SuppressWarnings("unchecked")
    static <T> Vars<T> ofNullable( Class<T> type, T... values ) {
        return AbstractVariables.ofNullable( false, type, values );
    }

    /**
     *  Creates a list of nullable properties from the supplied properties.
     * @param first The first property to add to the new Vars instance.
     * @param rest The remaining properties to add to the new Vars instance.
     * @param <T> The type of the properties.
     * @return A new Vars instance.
     */
    @SuppressWarnings("unchecked")
    static <T> Vars<T> ofNullable( Var<T> first, Var<T>... rest ) {
        return AbstractVariables.ofNullable( false, first, rest );
    }

    /** {@inheritDoc} */
    @Override Var<T> at( int index );

    /** {@inheritDoc} */
    @Override default Var<T> first() { return at(0); }

    /** {@inheritDoc} */
    @Override default Var<T> last() { return at(size() - 1); }

    /**
     *  Wraps the provided value in a {@link Var} property and adds it to the list.
     *
     * @param value The value to add.
     * @return This list of properties.
     */
    default Vars<T> add( T value ) { return add( Var.of(value) ); }

    /**
     *  Adds the provided property to the list.
     *
     * @param var The property to add.
     * @return This list of properties.
     */
    default Vars<T> add( Var<T> var ) { return addAt( size(), var ); }

    /**
     *  Removes the property at the specified index.
     *
     * @param index The index of the property to remove.
     * @return This list of properties.
     */
    Vars<T> removeAt( int index );

    /**
     *  Removes the property containing the provided value from the list.
     *  If the value is not found, the list is unchanged.
     *  @param value The value to remove.
     *  @return This list of properties.
     */
    default Vars<T> remove( T value ) { return removeAt( indexOf(Var.of(value)) ); }

    /**
     *  Removes the provided property from the list.
     *  If the property is not found, the list is unchanged.
     *  @param var The property to remove.
     *  @return This list of properties.
     */
    default Vars<T> remove( Var<T> var ) { return removeAt( indexOf(var) ); }

    /**
     *  Removes the first property from the list.
     *
     * @return This list of properties.
     */
    default Vars<T> removeFirst() { return removeAt(0); }

    /**
     *  Removes the last property from the list.
     *
     * @return This list of properties.
     */
    default Vars<T> removeLast() { return removeAt(size() - 1); }

    /**
     *  Removes {@code count} number of properties from the end
     *  of the list.
     *  @param count The number of properties to remove.
     *  @return This list of properties.
     */
    default Vars<T> removeLast( int count )
    {
        for ( int i = 0; i < count; i++ ) removeLast();
        return this;
    }

    /**
     *  Removes the first {@code count} number of properties from the list.
     *  @param count The number of properties to remove.
     *  @return This list of properties.
     */
    default Vars<T> removeFirst( int count )
    {
        for ( int i = 0; i < count; i++ ) removeFirst();
        return this;
    }

    /**
     *  Wraps the provided value in a {@link Var} property and adds it to the list
     *  at the specified index.
     *  @param index The index at which to add the property.
     *  @param value The value to add.
     *  @return This list of properties.
     */
    default Vars<T> addAt( int index, T value ) { return addAt(index, Var.of(value)); }

    /**
     *  Adds the provided property to the list at the specified index.
     *  @param index The index at which to add the property.
     *  @param var The property to add.
     *  @return This list of properties.
     */
    Vars<T> addAt( int index, Var<T> var );

    /**
     *  Wraps the provided value in a property and sets it at the specified index
     *  effectively replacing the property at that index.
     *  @param index The index at which to set the property.
     *  @param value The value to set.
     *  @return This list of properties.
     */
    default Vars<T> setAt( int index, T value ) { return setAt(index, Var.of(value)); }

    /**
     *  Places the provided property at the specified index, effectively replacing the property
     *  at that index.
     *  @param index The index at which to set the property.
     *  @param var The property to set.
     *  @return This list of properties.
     */
    Vars<T> setAt( int index, Var<T> var );

    /**
     *  Wraps each provided value in a property and appends it to this
     *  list of properties.
     *  @param values The values to add.
     *  @return This list of properties.
     */
    @SuppressWarnings("unchecked")
    default Vars<T> addAll( T... values )
    {
        for ( T v : values ) add(v);
        return this;
    }


    /**
     *  Iterates over the supplied values, and appends
     *  them to this list as properties.
     *  @param values The values to add.
     *  @return This list of properties.
     */
    default Vars<T> addAll( Iterable<T> values )
    {
        for ( T v : values ) add(v);
        return this;
    }

    /**
     *  Iterates over the supplied property list, and appends
     *  them to this list.
     *  @param vals The properties to add.
     *  @return This list of properties.
     */
    default Vars<T> addAll( Vals<T> vals )
    {
        for ( T v : vals ) add(v);
        return this;
    }

    /**
     *  Removes all properties from this list.
     *  This is conceptually equivalent to calling {@link List#clear()}
     *  on a regular list.
     *
     * @return This list.
     */
    Vars<T> clear();

    /**
     *  Use this for mapping a list of properties to another list of properties.
     */
    @Override default Vars<T> map( Function<T,T> mapper ) {
        Objects.requireNonNull(mapper);
        @SuppressWarnings("unchecked")
        Var<T>[] vars = new Var[size()];
        int i = 0;
        for ( T v : this ) vars[i++] = Var.of( mapper.apply(v) );
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
            vars[i] = this.at( i ).mapTo( type, mapper );
        return Vars.of( type, vars );
    }

    default Vals<T> toVals() { return Vals.of( type(), this ); }

    /**
     *  Use this for sorting the list of properties.
     *
     * @param comparator The comparator to use for sorting.
     */
    void sort( Comparator<T> comparator );

    /**
     * Sorts the list of properties using the natural ordering of the
     * properties.
     * Note that this method expected the wrapped values to be
     * {@link Comparable}.
     */
    default void sort() {
        // First we have to check if the type is comparable:
        if ( Comparable.class.isAssignableFrom(type()) ) {
            @SuppressWarnings("unchecked")
            Comparator<T> comparator = (Comparator<T>) Comparator.naturalOrder();
            sort( comparator );
        }
        else
            throw new UnsupportedOperationException("Cannot sort a list of non-comparable types.");
    }

    /**
     *  Removes all duplicate properties from this list of properties.
     */
    void makeDistinct();

}
