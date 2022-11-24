package com.globaltcad.swingtree.api.mvvm;

import java.util.*;
import java.util.function.Consumer;

public abstract class AbstractVariable<T> implements Var<T>
{
	private final List<Consumer<Val<T>>> viewActions = new ArrayList<>();

	private final List<Val<T>> history = new ArrayList<>(17);

	private T value;
	private final Class<T> type;
	private final String name;
	private final PropertyAction<T> action;


	protected AbstractVariable( Class<T> type, T iniValue, String name, PropertyAction<T> action ) {
		this( type, iniValue, name, action, Collections.emptyList() );
	}

	protected AbstractVariable(
			Class<T> type, T iniValue, String name, PropertyAction<T> action, List<Consumer<Val<T>>> viewActions
	) {
		Objects.requireNonNull(name);
		this.value = iniValue;
		this.type = ( iniValue == null ? type : (Class<T>) iniValue.getClass());
		this.name = name;
		this.action = ( action == null ? v -> {} : action );
		if ( this.value != null ) {
			// We check if the type is correct
			if ( !type.isAssignableFrom(this.value.getClass()) )
				throw new IllegalArgumentException(
						"The provided type of the initial value is not compatible with the actual type of the variable"
					);
		}
		this.viewActions.addAll(viewActions);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override public Class<T> type() { return type; }

	/**
	 * {@inheritDoc}
	 */
	@Override public String id() { return name; }

	/**
	 * {@inheritDoc}
	 */
	@Override public Var<T> withID( String id ) {
		AbstractVariable<T> newVar = new AbstractVariable<T>(type, value, id, null){};
		newVar.viewActions.addAll(viewActions);
		return newVar;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override public Var<T> withAction(PropertyAction<T> action ) {
		Objects.requireNonNull(action);
		AbstractVariable<T> newVar = new AbstractVariable<T>(type, value, name, action){};
		newVar.viewActions.addAll(viewActions);
		return newVar;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override public Var<T> act() {
		List<Val<T>> reverseHistory = new ArrayList<>(AbstractVariable.this.history);
		Collections.reverse(reverseHistory);
		action.act(new ActionDelegate<T>() {
			@Override public Var<T> current() { return AbstractVariable.this; }
			@Override
			public Val<T> previous() {
				if ( AbstractVariable.this.history.isEmpty() )
					return Val.of(AbstractVariable.this.type, null);
				return reverseHistory.get(0);
			}
			@Override
			public List<Val<T>> history() {
				return Collections.unmodifiableList(reverseHistory);
			}
		});
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override public Var<T> act(T newValue) {
		_setInternal(newValue);
		return act();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public T orElseThrow() {
		// This class is similar to optional, so if the value is null, we throw an exception!
		if ( value == null )
			throw new NoSuchElementException("No value present");

		return value;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isPresent() { return value != null; }

	/**
	 * {@inheritDoc}
	 */
	public T orElseNullable(T other) {
		return value != null ? value : other;
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
		if ( !Objects.equals( this.value, newValue ) ) {
			// First we check if the value is compatible with the type
			if ( newValue != null && !type.isAssignableFrom(newValue.getClass()) )
				throw new IllegalArgumentException(
						"The provided type of the new value is not compatible with the type of this property"
					);

			history.add(Val.of(this.type(), this.value).withID(this.id()));
			if ( history.size() > 16 )
				history.remove(0);
			value = newValue;
			return true;
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override public Val<T> onShowThis( Consumer<Val<T>> displayAction ) {
		this.viewActions.add(displayAction);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Val<T> show() {
		for ( Consumer<Val<T>> action : this.viewActions )
			try {
				action.accept(this);
			} catch (Exception e) {
				e.printStackTrace();
			}
		return this;
	}

	@Override
	public String toString() {
		String asString = ( this.orElseNull() == null ? "null" : this.orElseNull().toString() );
		if ( id() == null ) return asString;
		else return
				asString +
						" ( " +
						"type='"+( type() == null ? "?" : type().getSimpleName() )+"', " +
						"name='"+ id()+"' " +
						")";
	}

	@Override
	public boolean equals( Object obj ) {
		if ( obj == null ) return false;
		if ( obj == this ) return true;
		if ( obj instanceof Val ) {
			Val<?> other = (Val<?>) obj;
			if ( other.type() != this.type ) return false;
			if ( other.orElseNull() == null ) return this.value == null;
			return Val.equals( other.orElseThrow(), this.value ); // Arrays are compared with Arrays.equals
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 31 * hash + ( this.value == null ? 0 : Val.hashCode(this.value) );
		hash = 31 * hash + ( this.type  == null ? 0 : this.type.hashCode() );
		hash = 31 * hash + ( this.name  == null ? 0 : this.name.hashCode() );
		return hash;
	}
}
