package com.globaltcad.swingtree.api.mvvm;

import java.util.*;

/**
 * 	The base implementation for both {@link Var} and {@link Val} interfaces.
 * 	This also serves as a reference implementation for the concept of
 *  {@link Var}/{@link Val} properties in general.
 * 
 * @param <T> The type of the value wrapped by a given property...
 */
public abstract class AbstractVariable<T> implements Var<T>
{
	private final List<PropertyAction<T>> _viewActions = new ArrayList<>();

	private final List<Val<T>> _history = new ArrayList<>(17);

	private T _value;
	private final Class<T> _type;
	private final String _id;
	private final PropertyAction<T> _action;

	private final boolean _allowsNull;


	protected AbstractVariable( Class<T> type, T iniValue, String name, PropertyAction<T> action, boolean allowsNull ) {
		this( type, iniValue, name, action, Collections.emptyList(), allowsNull );
	}

	protected AbstractVariable(
			Class<T> type,
			T iniValue,
			String name,
			PropertyAction<T> action,
			List<PropertyAction<T>> viewActions,
			boolean allowsNull
	) {
		Objects.requireNonNull(name);
		_value = iniValue;
		_type = ( iniValue == null ? type : (Class<T>) iniValue.getClass());
		_id = name;
		_action = ( action == null ? v -> {} : action );
		if ( _value != null ) {
			// We check if the type is correct
			if ( !type.isAssignableFrom(_value.getClass()) )
				throw new IllegalArgumentException(
						"The provided type of the initial value is not compatible with the actual type of the variable"
					);
		}
		_viewActions.addAll(viewActions);
		_allowsNull = allowsNull;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override public Class<T> type() { return _type; }

	/**
	 * {@inheritDoc}
	 */
	@Override public String id() { return _id; }

	/**
	 * {@inheritDoc}
	 */
	@Override public Var<T> withID( String id ) {
		AbstractVariable<T> newVar = new AbstractVariable<T>(_type, _value, id, null, _allowsNull ){};
		newVar._viewActions.addAll(_viewActions);
		return newVar;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override public Var<T> withAction(PropertyAction<T> action ) {
		Objects.requireNonNull(action);
		AbstractVariable<T> newVar = new AbstractVariable<T>( _type, _value, _id, action, _allowsNull ){};
		newVar._viewActions.addAll(_viewActions);
		return newVar;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override public Var<T> act() {
		_action.act(_createDelegate());
		return this;
	}

	private ActionDelegate<T> _createDelegate() {
		List<Val<T>> reverseHistory = new ArrayList<>(AbstractVariable.this._history);
		Collections.reverse(reverseHistory);
		return new ActionDelegate<T>() {
			@Override public Var<T> current() { return AbstractVariable.this; }
			@Override
			public Val<T> previous() {
				if ( AbstractVariable.this._history.isEmpty() )
					return Val.ofNullable(AbstractVariable.this._type, null);
				return reverseHistory.get(0);
			}
			@Override
			public List<Val<T>> history() {
				return Collections.unmodifiableList(reverseHistory);
			}
		};
	}

	/**
	 * {@inheritDoc}
	 */
	@Override public Var<T> act(T newValue) {
		if ( _setInternal(newValue) )
			return act();
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public T orElseThrow() {
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
	public T orElseNullable(T other) {
		return _value != null ? _value : other;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Var<T> set( T newValue ) {
		if ( _setInternal(newValue) ) this.show();
		return this;
	}

	private boolean _setInternal( T newValue ) {
		if ( !_allowsNull && newValue == null )
			throw new NullPointerException(
					"This property is configured to not allow null values! " +
					"If you want your property to allow null values, use the 'of(Class, T)' factory method."
				);

		if ( !Objects.equals( _value, newValue ) ) {
			// First we check if the value is compatible with the type
			if ( newValue != null && !_type.isAssignableFrom(newValue.getClass()) )
				throw new IllegalArgumentException(
						"The provided type of the new value is not compatible with the type of this property"
					);

			_history.add(Val.ofNullable(this.type(), _value).withID(this.id()));
			if ( _history.size() > 16 )
				_history.remove(0);
			_value = newValue;
			return true;
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override public Val<T> onShowThis( PropertyAction<T> displayAction ) {
		_viewActions.add(displayAction);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Val<T> show() {
		for ( PropertyAction<T> action : _viewActions)
			try {
				action.act(_createDelegate());
			} catch (Exception e) {
				e.printStackTrace();
			}
		return this;
	}

	@Override
	public String toString() {
		String value = this.map(Object::toString).orElse("null");
		String id = this.id() == null ? "?" : this.id();
		if ( id.equals(UNNAMED) ) id = "?";
		String type = ( type() == null ? "?" : type().getSimpleName() );
		if ( type.equals("Object") ) type = "?";
		if ( type.equals("String") && this.isPresent() ) value = "\"" + value + "\"";
		if ( _allowsNull ) type = type + "?";
		return
			value +
			" ( " +
				"type = "+type+", " +
				"id = \""+ id+"\" " +
			")";
	}

	@Override
	public boolean equals( Object obj ) {
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
	public int hashCode() {
		int hash = 7;
		hash = 31 * hash + ( _value == null ? 0 : Val.hashCode(_value) );
		hash = 31 * hash + ( _type == null ? 0 : _type.hashCode() );
		hash = 31 * hash + ( _id == null ? 0 : _id.hashCode() );
		return hash;
	}
}
