package swingtree.api.mvvm;

import java.util.*;
import java.util.stream.Collectors;

public class AbstractVariables<T> implements Vars<T>
{

    static <T> Vars<T> of( boolean immutable, Class<T> type, Var<T>... vars ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(vars);
        return new AbstractVariables<T>( immutable, type, false, vars ){};
    }

    static <T> Vars<T> of( boolean immutable, Var<T> first, Var<T>... rest ) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(rest);
        Var<T>[] vars = new Var[rest.length+1];
        vars[0] = first;
        System.arraycopy(rest, 0, vars, 1, rest.length);
        return of(immutable, first.type(), vars);
    }

    static <T> Vars<T> of( boolean immutable, T first, T... rest ) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(rest);
        Var<T>[] vars = new Var[rest.length+1];
        vars[0] = Var.of(first);
        for ( int i = 0; i < rest.length; i++ )
            vars[ i + 1 ] = Var.of( rest[ i ] );
        return of(immutable, vars[0].type(), vars);
    }

    static <T> Vars<T> of( boolean immutable, Class<T> type, Iterable<Var<T>> vars ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(vars);
        List<Var<T>> list = new ArrayList<>();
        vars.forEach( list::add );
        Var<T>[] array = new Var[list.size()];
        return new AbstractVariables<T>( immutable, type, false, list.toArray(array) ){};
    }

    static <T> Vars<T> ofNullable( boolean immutable, Class<T> type ){
        Objects.requireNonNull(type);
        return new AbstractVariables<T>( immutable, type, true, new Var[0] ){};
    }

    static <T> Vars<T> ofNullable( boolean immutable, Class<T> type, Var<T>... vars ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(vars);
        return new AbstractVariables<T>( immutable, type, true, vars ){};
    }

    static <T> Vars<T> ofNullable( boolean immutable, Class<T> type, T... vars ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(vars);
        Var<T>[] array = new Var[vars.length];
        for ( int i = 0; i < vars.length; i++ )
            array[i] = Var.ofNullable(type, vars[i]);
        return new AbstractVariables<T>( immutable, type, true, array ){};
    }

    static <T> Vars<T> ofNullable( boolean immutable, Var<T> first, Var<T>... vars ) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(vars);
        Var<T>[] array = new Var[vars.length+1];
        array[0] = first;
        System.arraycopy(vars, 0, array, 1, vars.length);
        return ofNullable(immutable, first.type(), array);
    }


    private final List<Var<T>> _variables = new ArrayList<>();
    private final boolean _isImmutable;
    private final boolean _allowsNull;
    private final Class<T> _type;

    private final List<Action<ValsDelegate<T>>> _viewActions = new ArrayList<>();

    @SafeVarargs
    protected AbstractVariables( boolean isImmutable, Class<T> type, boolean allowsNull, Var<T>... vals ) {
        _isImmutable = isImmutable;
        _type = type;
        _allowsNull = allowsNull;
        _variables.addAll(Arrays.asList(vals));
        _checkNullSafety();
    }

    /** {@inheritDoc} */
    @Override public final Var<T> at( int index ) { return _variables.get(index); }

    /** {@inheritDoc} */
    @Override public final Class<T> type() { return _type; }

    /** {@inheritDoc} */
    @Override public final int size() { return _variables.size(); }

    /** {@inheritDoc} */
    @Override
    public Vars<T> addAt( int index, Var<T> value ) {
        if ( _isImmutable ) throw new UnsupportedOperationException("This is an immutable list.");
        _checkNullSafetyOf(value);
        _triggerAction( Change.ADD, index, value, null );
        _variables.add(index, value);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Vars<T> removeAt( int index ) {
        if ( _isImmutable ) throw new UnsupportedOperationException("This is an immutable list.");
        if ( index < 0 || index >= _variables.size() )
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + _variables.size());
        _triggerAction( Change.REMOVE, index, null, _variables.get(index) );
        _variables.remove(index);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Vars<T> setAt( int index, Var<T> value ) {
        if ( _isImmutable ) throw new UnsupportedOperationException("This is an immutable list.");
        if ( index < 0 || index >= _variables.size() )
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + _variables.size());

        _checkNullSafetyOf(value);

        Var<T> old = _variables.get(index);

        if ( !old.equals(value) ) {
            _triggerAction(Change.SET, index, value, at(index));
            _variables.set(index, value);
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Vars<T> clear() {
        if ( _isImmutable ) throw new UnsupportedOperationException("This is an immutable list.");
        _triggerAction( Change.CLEAR, -1, null, null );
        _variables.clear();
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public void sort( Comparator<T> comparator ) {
        if ( _isImmutable ) throw new UnsupportedOperationException("This is an immutable list.");
        _variables.sort( ( a, b ) -> comparator.compare( a.get(), b.get() ) );
        _triggerAction( Change.SORT, -1, null, null );
    }

    /** {@inheritDoc} */
    @Override
    public final void makeDistinct() {
        if ( _isImmutable ) throw new UnsupportedOperationException("This is an immutable list.");
        List<Var<T>> list = new ArrayList<>();
        for ( Var<T> v : _variables )
            if ( !list.contains(v) )
                list.add(v);

        _variables.clear();
        _variables.addAll(list);
        _triggerAction( Change.DISTINCT, -1, null, null );
    }

    /** {@inheritDoc} */
    @Override
    public Vals<T> onChange( Action<ValsDelegate<T>> action ) {
        _viewActions.add(action);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Vals<T> fireChange() {
        _triggerAction( Change.NONE, -1, null, null );
        return this;
    }

    private ValsDelegate<T> _createDelegate(
            int index, Change type, Var<T> newVal, Var<T> oldVal
    ) {
        return new ValsDelegate<T>() {
            @Override public int index() { return index; }
            @Override public Change changeType() { return type; }
            @Override public Val<T> newValue() { return newVal != null ? newVal : Val.ofNullable(_type, null); }
            @Override public Val<T> oldValue() { return oldVal != null ? oldVal : Val.ofNullable(_type, null); }
        };
    }

    private void _triggerAction(
            Change type, int index, Var<T> newVal, Var<T> oldVal
    ) {
        List<Action<ValsDelegate<T>>> removableActions = new ArrayList<>();
        ValsDelegate<T> showAction = _createDelegate(index, type, newVal, oldVal);
        for ( Action<ValsDelegate<T>> action : _viewActions ) {
            try {
                if ( action.canBeRemoved() )
                    removableActions.add(action);
                else
                    action.accept(showAction);
            } catch ( Exception e ) {
                e.printStackTrace();
            }
        }
        _viewActions.removeAll(removableActions);
    }

    /** {@inheritDoc} */
    @Override
    public java.util.Iterator<T> iterator() {
        return new java.util.Iterator<T>() {
            private int index = 0;
            @Override public boolean hasNext() { return index < size(); }
            @Override public T next() { return at(index++).get(); }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final String toString() {
        return "Vars[" + _variables.stream().map(Object::toString).collect(Collectors.joining(",")) + "]";
    }

    /** {@inheritDoc} */
    @Override
    public final boolean equals( Object obj ) {
        if( obj == null ) return false;
        if( obj == this ) return true;
        if( obj instanceof Vals ) {
            @SuppressWarnings("unchecked")
            Vals<T> other = (Vals<T>) obj;
            if( size() != other.size() ) return false;
            for( int i = 0; i < size(); i++ ) {
                if( !this.at(i).equals(other.at(i)) ) return false;
            }
            return true;
        }
        return false;
    }

    private void _checkNullSafety() {
        if ( !_allowsNull )
            for ( Var<T> val : _variables )
                _checkNullSafetyOf(val);
    }

    private void _checkNullSafetyOf(Val<T> value) {
        Objects.requireNonNull(value);
        if ( !_allowsNull && value.allowsNull() )
            throw new IllegalArgumentException("Null values are not allowed in this property list.");
    }

}
