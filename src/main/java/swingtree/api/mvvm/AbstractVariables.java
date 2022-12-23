package swingtree.api.mvvm;

import java.util.*;
import java.util.stream.Collectors;

public class AbstractVariables<T> implements Vars<T>
{
    private final List<Var<T>> _variables = new ArrayList<>();
    private final boolean _allowsNull;
    private final Class<T> _type;

    private final List<Action<ValsDelegate<T>>> _viewActions = new ArrayList<>();

    @SafeVarargs
    public AbstractVariables(Class<T> type, boolean allowsNull, Var<T>... vals) {
        _type = type;
        _allowsNull = allowsNull;
        _variables.addAll(Arrays.asList(vals));
        _checkNullSafety();
    }

    @Override public Var<T> at(int index) { return _variables.get(index); }

    @Override public Class<T> type() { return _type; }

    @Override public int size() { return _variables.size(); }

    @Override
    public Vars<T> addAt(int index, Var<T> value) {
        _checkNullSafetyOf(value);
        _triggerAction( Mutation.ADD, index, value );
        _variables.add(index, value);
        return this;
    }

    @Override
    public Vars<T> removeAt(int index) {
        if ( index < 0 || index >= _variables.size() )
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + _variables.size());
        _triggerAction( Mutation.REMOVE, index, _variables.get(index) );
        _variables.remove(index);
        return this;
    }

    @Override
    public Vars<T> setAt( int index, Var<T> value ) {
        if ( index < 0 || index >= _variables.size() )
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + _variables.size());

        _checkNullSafetyOf(value);

        Var<T> old = _variables.get(index);

        if ( !old.equals(value) ) {
            _triggerAction(Mutation.SET, index, value);
            _variables.set(index, value);
        }
        return this;
    }

    @Override
    public Vals<T> onShow(Action<ValsDelegate<T>> action) {
        _viewActions.add(action);
        return this;
    }

    @Override
    public Vals<T> show() {
        _triggerAction(Mutation.NONE, -1, null);
        return this;
    }

    private ValsDelegate<T> _createDelegate(int index, Mutation type, Var<T> value) {
        return new ValsDelegate<T>() {
            @Override public int index() { return index; }
            @Override public Mutation type() { return type; }
            @Override public Optional<Val<T>> value() { return Optional.ofNullable(value); }
        };
    }

    private void _triggerAction(Mutation type, int index, Var<T> value ) {
        List<Action<ValsDelegate<T>>> removableActions = new ArrayList<>();
        ValsDelegate<T> showAction = _createDelegate(index, type, value);
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

    @Override
    public java.util.Iterator<T> iterator() {
        return new java.util.Iterator<T>() {
            private int index = 0;

            @Override public boolean hasNext() { return index < size(); }

            @Override public T next() { return at(index++).get(); }
        };
    }

    @Override
    public String toString() {
        return "Vars[" + _variables.stream().map(Object::toString).collect(Collectors.joining(",")) + "]";
    }

    @Override
    public boolean equals(Object obj) {
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
