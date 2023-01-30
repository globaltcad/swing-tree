package swingtree.api.mvvm;

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
		return new AbstractVariable<T>( immutable, type, value, NO_ID, Collections.emptyList(), true ){};
	}

	static <T> Var<T> of( boolean immutable, Class<T> type, T value ) {
		return new AbstractVariable<T>( immutable, type, value, NO_ID, Collections.emptyList(), false ){};
	}

	static <T> Var<T> of( boolean immutable, T iniValue ) {
		Objects.requireNonNull(iniValue);
		return new AbstractVariable<T>( immutable, (Class<T>) iniValue.getClass(), iniValue, NO_ID, Collections.emptyList(), false ){};
	}

	static Var<Viewable> of( boolean immutable, Viewable iniValue ) {
		Objects.requireNonNull(iniValue);
		return new AbstractVariable<Viewable>( immutable, Viewable.class, iniValue, NO_ID, Collections.emptyList(), false ){};
	}

	static Var<Border> of( boolean immutable, Border iniValue ) {
		Objects.requireNonNull(iniValue);
		return new AbstractVariable<Border>( immutable, Border.class, iniValue, NO_ID, Collections.emptyList(), false ){};
	}

	private final boolean _isImmutable;
	private final List<Val<T>> _history = new ArrayList<>(17);
	protected final List<Action<Val<T>>> _actions = new ArrayList<>();
	private final List<Consumer<T>> _viewers = new ArrayList<>(0);



	protected AbstractVariable(
			boolean immutable,
			Class<T> type,
			T iniValue,
			String name,
			List<Action<Val<T>>> actions,
			boolean allowsNull
	) {
		this( immutable, type, iniValue, name, actions, Collections.emptyList(), allowsNull );
	}

	protected AbstractVariable(
		boolean immutable,
		Class<T> type,
		T iniValue,
		String id,
		List<Action<Val<T>>> actions,
		List<Action<Val<T>>> viewActions,
		boolean allowsNull
	) {
		super( type, id, allowsNull, iniValue );
		Objects.requireNonNull(id);
		Objects.requireNonNull(actions);
		_isImmutable = immutable;
		_viewActions.addAll(viewActions);
		_actions.addAll(actions);
	}

	/** {@inheritDoc} */
	@Override public Var<T> withId( String id ) {
		AbstractVariable<T> newVar = new AbstractVariable<T>( _isImmutable, _type, _value, id, _actions, _allowsNull ){};
		newVar._viewActions.addAll(_viewActions);
		return newVar;
	}

	/** {@inheritDoc} */
	@Override public Var<T> onAct( Action<Val<T>> action ) {
		Objects.requireNonNull(action);
		_actions.add(action);
		return this;
	}

	/** {@inheritDoc} */
	@Override public Var<T> fireAct() {
		_triggerActions( _actions);
		_viewers.forEach( v -> v.accept(_value) );
		return this;
	}

	/** {@inheritDoc} */
	@Override public Var<T> act( T newValue ) {
		if ( _isImmutable )
			throw new UnsupportedOperationException("This variable is immutable!");
		if ( _setInternal(newValue) )
			return fireAct();
		return this;
	}

	/** {@inheritDoc} */
	@Override
	public Var<T> set( T newItem) {
		if ( _isImmutable )
			throw new UnsupportedOperationException("This variable is immutable!");
		if ( _setInternal(newItem) ) this.fireSet();
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

	/** {@inheritDoc} */
	@Override public final <U> Val<U> viewAs( Class<U> type, java.util.function.Function<T, U> mapper ) {
		Var<U> var = mapTo(type, mapper);
		// Now we register a live update listener to this property
		this.onSet(v -> var.set( mapper.apply( v.orElseNull() ) ));
		_viewers.add( v -> var.act( mapper.apply( v ) ) );
		return var;
	}

}
