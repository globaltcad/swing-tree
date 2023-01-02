package swingtree.api.mvvm;

import swingtree.api.UIAction;

import javax.swing.border.Border;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 	The base implementation for both {@link Var} and {@link Val} interfaces.
 * 	This also serves as a reference implementation for the concept of
 *  {@link Var}/{@link Val} properties in general.
 * 
 * @param <T> The type of the value wrapped by a given property...
 */
public abstract class AbstractVariable<T> extends AbstractValue<T> implements Var<T>
{
	static <T> Var<T> ofNullable( boolean immutable, Class<T> type, T value ) {
		return new AbstractVariable<T>( immutable, type, value, UNNAMED, null, true ){};
	}

	static <T> Var<T> of( boolean immutable, T iniValue ) {
		Objects.requireNonNull(iniValue);
		return new AbstractVariable<T>( immutable, (Class<T>) iniValue.getClass(), iniValue, UNNAMED, null, false ){};
	}

	static Var<Viewable> of( boolean immutable, Viewable iniValue ) {
		Objects.requireNonNull(iniValue);
		return new AbstractVariable<Viewable>( immutable, Viewable.class, iniValue, UNNAMED, null, false ){};
	}

	static Var<Border> of( boolean immutable, Border iniValue ) {
		Objects.requireNonNull(iniValue);
		return new AbstractVariable<Border>( immutable, Border.class, iniValue, UNNAMED, null, false ){};
	}

	private final boolean _isImmutable;
	private final List<Val<T>> _history = new ArrayList<>(17);
	private final UIAction<ValDelegate<T>> _action;
	private final List<Consumer<T>> _viewers = new ArrayList<>(0);



	protected AbstractVariable(
			boolean immutable,
			Class<T> type,
			T iniValue,
			String name,
			UIAction<ValDelegate<T>> action,
			boolean allowsNull
	) {
		this( immutable, type, iniValue, name, action, Collections.emptyList(), allowsNull );
	}

	protected AbstractVariable(
		boolean immutable,
		Class<T> type,
		T iniValue,
		String id,
		UIAction<ValDelegate<T>> action,
		List<UIAction<ValDelegate<T>>> viewActions,
		boolean allowsNull
	) {
		super( type, id, allowsNull, iniValue );
		Objects.requireNonNull(id);
		_isImmutable = immutable;
		_action = ( action == null ? v -> {} : action );
		_viewActions.addAll(viewActions);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override public Var<T> withId(String id ) {
		AbstractVariable<T> newVar = new AbstractVariable<T>( _isImmutable, _type, _value, id, null, _allowsNull ){};
		newVar._viewActions.addAll(_viewActions);
		return newVar;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override public Var<T> withAction( UIAction<ValDelegate<T>> action ) {
		Objects.requireNonNull(action);
		AbstractVariable<T> newVar = new AbstractVariable<T>( _isImmutable, _type, _value, _id, action, _allowsNull ){};
		newVar._viewActions.addAll(_viewActions);
		return newVar;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override public Var<T> act() {
		_action.accept(_createDelegate());
		_viewers.forEach( v -> v.accept(_value) );
		return this;
	}

	@Override
	protected ValDelegate<T> _createDelegate() {
		// We clone the current state of the variable because
		// it might be accessed from a different thread! (e.g. Swing EDT or Application Thread)
		AbstractVariable<T> clone = _clone();
		clone._viewActions.addAll(_viewActions);
		List<Val<T>> reverseHistory = new ArrayList<>(AbstractVariable.this._history);
		Collections.reverse(reverseHistory);
		return new ValDelegate<T>() {
			@Override public Val<T> current() { return clone; }
			@Override
			public Val<T> previous() {
				if ( reverseHistory.isEmpty() )
					return Val.ofNullable(clone._type, null);
				return reverseHistory.get(0);
			}
			@Override
			public List<Val<T>> history() { return Collections.unmodifiableList(reverseHistory); }
		};
	}

	@Override protected AbstractVariable<T> _clone() {
		return new AbstractVariable<T>( _isImmutable, _type, _value, _id, _action, _allowsNull ){};
	}

	/**
	 * {@inheritDoc}
	 */
	@Override public Var<T> act( T newValue ) {
		if ( _isImmutable )
			throw new UnsupportedOperationException("This variable is immutable!");
		if ( _setInternal(newValue) )
			return act();
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Var<T> set( T newValue ) {
		if ( _isImmutable )
			throw new UnsupportedOperationException("This variable is immutable!");
		if ( _setInternal(newValue) ) this.show();
		return this;
	}

	private boolean _setInternal( T newValue ) {
		if ( !_allowsNull && newValue == null )
			throw new NullPointerException(
					"This property is configured to not allow null values! " +
					"If you want your property to allow null values, use the 'ofNullable(Class, T)' factory method."
				);

		if ( !Objects.equals( _value, newValue ) ) {
			// First we check if the value is compatible with the type
			if ( newValue != null && !_type.isAssignableFrom(newValue.getClass()) )
				throw new IllegalArgumentException(
						"The provided type '"+newValue.getClass()+"' of the new value is not compatible " +
						"with the type '"+_type+"' of this property"
					);

			_history.add(Val.ofNullable(this.type(), _value).withId(this.id()));
			if ( _history.size() > 16 )
				_history.remove(0);
			_value = newValue;
			return true;
		}
		return false;
	}

	@Override public final <U> Val<U> viewAs( Class<U> type, java.util.function.Function<T, U> mapper ) {
		Var<U> var = mapTo(type, mapper);
		// Now we register a live update listener to this property
		this.onShow( v -> var.set( mapper.apply( v ) ));
		_viewers.add( v -> var.act( mapper.apply( v ) ) );
		return var;
	}

}
