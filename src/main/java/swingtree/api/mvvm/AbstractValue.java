package swingtree.api.mvvm;

import swingtree.UI;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

abstract class AbstractValue<T> implements Val<T>
{
    protected final List<Action<ValDelegate<T>>> _viewActions = new ArrayList<>();

    protected T _value;
    protected final Class<T> _type;
    protected final String _id;

    protected final boolean _allowsNull;


    protected AbstractValue( Class<T> type, String id, boolean allowsNull, T iniValue ) {
        Objects.requireNonNull(id);
        _type = ( iniValue == null || type != null ? type : (Class<T>) iniValue.getClass());
        _id = id;
        _allowsNull = allowsNull;
        _value = iniValue;
        if ( _value != null ) {
			// We check if the type is correct
			if ( !_type.isAssignableFrom(_value.getClass()) )
				throw new IllegalArgumentException(
						"The provided type of the initial value is not compatible with the actual type of the variable"
					);
		}
    }

    /**
     * {@inheritDoc}
     */
    @Override public final Class<T> type() { return _type; }

    /**
     * {@inheritDoc}
     */
    @Override public final String id() { return _id; }

    /**
     * {@inheritDoc}
     */
    @Override
    public final T orElseThrow() {
        // This class is similar to optional, so if the value is null, we throw an exception!
        if ( _value == null )
            throw new NoSuchElementException("No value present");

        return _value;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isPresent() { return _value != null; }

    /**
     * {@inheritDoc}
     */
    public T orElseNullable( T other ) { return _value != null ? _value : other; }

    /**
     * {@inheritDoc}
     */
    @Override public Val<T> onShow( Action<ValDelegate<T>> displayAction ) {
        _viewActions.add(displayAction);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Val<T> show() { _triggerActions( _viewActions, true ); return this; }

    protected void _triggerActions(List<Action<ValDelegate<T>>> actions, boolean runInGUIThread ) {
        List<Action<ValDelegate<T>>> removableActions = new ArrayList<>();
        for ( Action<ValDelegate<T>> action : new ArrayList<>(actions) ) // We copy the list to avoid concurrent modification
            try {
                if ( action.canBeRemoved() )
                    removableActions.add(action);
                else {
                    ValDelegate<T> delegate = _createDelegate();
                    if ( runInGUIThread )
                        UI.run( () -> action.accept(delegate) );
                    else
                        action.accept(delegate);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        actions.removeAll(removableActions);
    }


    protected abstract ValDelegate<T> _createDelegate();


    protected abstract AbstractValue<T> _clone();


    /**
     * {@inheritDoc}
     */
    @Override public final boolean allowsNull() { return _allowsNull; }

    @Override
    public final String toString() {
        String value = this.mapTo(String.class, Object::toString).orElse("null");
        String id = this.id() == null ? "?" : this.id();
        if ( id.equals(NO_ID) ) id = "?";
        String type = ( type() == null ? "?" : type().getSimpleName() );
        if ( type.equals("Object") ) type = "?";
        if ( type.equals("String") && this.isPresent() ) value = "\"" + value + "\"";
        if ( _allowsNull ) type = type + "?";
        return value + " ( " +
                            "type = "+type+", " +
                            "id = \""+ id+"\" " +
                        ")";
    }

    @Override
    public final boolean equals( Object obj ) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj instanceof Val ) {
            Val<?> other = (Val<?>) obj;
            if ( other.type() != _type) return false;
            if ( other.orElseNull() == null ) return _value == null;
            return Val.equals( other.orElseThrow(), _value); // Arrays are compared with Arrays.equals
        }
        return false;
    }

    @Override
    public final int hashCode() {
        int hash = 7;
        hash = 31 * hash + ( _value == null ? 0 : Val.hashCode(_value) );
        hash = 31 * hash + ( _type == null ? 0 : _type.hashCode() );
        hash = 31 * hash + ( _id == null ? 0 : _id.hashCode() );
        return hash;
    }
}
