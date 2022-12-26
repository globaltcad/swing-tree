package swingtree.api.mvvm;

import swingtree.api.UIAction;

import java.util.Optional;
import java.util.function.Function;

/**
 * 	A mutable wrapper for a value which can be observed by the Swing-Tree UI
 * 	to dynamically update UI components for you, as well
 * 	as trigger an action inside your view model.
 *  <p>
 * 	So for example if you have a {@link Var} which represents the username
 * 	of a form, in your Swing-Tree UI you can register a callback which will update the UI
 * 	accordingly or trigger a view model action. <br>
 * 	Consider the following example property in your view model:
 * 	<pre>{@code
 * 	    // A username property with a validation action:
 * 		private final Var<String> username = Var.of("").withAction( v -> validateUser(v) );
 * 	}</pre>
 * 	And the following Swing-Tree UI:
 * 	<pre>{@code
 * 	    UI.textField()
 * 	    .peek( ta -> vm.getUsername().onShow( t -> ta.setText(t) ) )
 * 	    .onKeyReleased( e -> vm.getUsername().act( ta.getText() ) );
 * 	}</pre>
 * 	Your view will automatically update the text field with the value of the property
 * 	and inside your view model you can also update the value of the property
 * 	to be shown in the UI:
 * 	<pre>{@code
 * 	    // Initially empty username:
 * 		username.set( "" ).show();
 * 	}</pre>
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 *
 * @param <T> The type of the wrapped value.
 */
public interface Var<T> extends Val<T>
{
	/**
	 *  Use this factory method to create a new {@link Var} instance
	 *  whose value may or may not be null.
	 *  <p>
	 *  <b>Example:</b>
	 *  <pre>{@code
	 *      Var.ofNullable(String.class, null);
	 *  }</pre>
	 *  <p>
	 * @param type The type of the value wrapped by the property.
	 *             This is used to check if the value is of the correct type.
	 * @param value The initial value of the property.
	 *              This may be null.
	 * @param <T> The type of the wrapped value.
	 * @return A new {@link Var} instance.
	 */
	static <T> Var<T> ofNullable( Class<T> type, T value ) {
		return AbstractVariable.ofNullable( false, type, value );
	}

	/**
	 *  This factory method will expose a builder which will create a very simple type of Property,
	 *  namely: {@link AbstractVariable}!
	 *  It has a simple implementation for everything defined in the {@link Var} interface.
	 *  So it can be touched, untouched, validated, initialized and applied.
	 *  One may or may not use these features depending on the use case, however in most cases
	 *  {@link Var} implementation are simple view properties which will need these functionalities.
	 *
	 * @param iniValue The initial value set to the instance built by this builder. Note: An initialization will overwrite this.
	 * @param <T> The type of the value held by the {@link Var}!
	 * @return The builder for a {@link AbstractVariable}.
	 */
	static <T> Var<T> of( T iniValue ) {
		return AbstractVariable.of( false, iniValue );
	}

	static Var<Viewable> of( Viewable iniValue ) {
		return AbstractVariable.of( false, iniValue );
	}
	
	/**
	 *  This method provides the ability to change the state of the wrapper.
	 *  It might have several side effects depending on the implementation.
	 *
	 * @param newValue The new value which ought to replace the old one.
	 * @return This very wrapper instance, in order to enable method chaining.
	 */
	Var<T> set( T newValue );

	/**
	 *  Use this method to create a new property with an id.
	 *  This id is used to identify the property in the UI
	 *  or as a key in a map, which is useful when converting your
	 *  view model to a JSON object, or similar formats.
	 *
	 * @param id The id of the property.
	 * @return A new {@link Var} instance with the given id.
	 */
	@Override Var<T> withID( String id );

	/**
	 *  Use this method to create a new property with an action which is supposed to be triggered
	 *  when the UI changes the value of this property through
	 *  the {@code Var::act(T)} method, or simply when it is explicitly
	 *  triggered by the {@code Var::act(T)} method.
	 *
	 * @param action The action to be triggered when {@code Var::act()} or {@code Var::act(T)} is called.
	 * @return A new {@link Var} instance which is identical to this one, except that it has the given action.
	 */
	Var<T> withAction( UIAction<ValDelegate<T>> action );

	/**
	 *  Triggers the action associated with this property, if one was
	 *  set using {@link #withAction(UIAction)}.
	 *  This method is intended to be used in the UI
	 *  to indicate that the user has changed the value of the property
	 *  not your view model.
	 *
	 * @return This very wrapper instance, in order to enable method chaining.
	 */
	Var<T> act();

	/**
	 *  Updates the state of this property and then, if the state has changed,
	 *  trigger its action using the {@link #act()} method. <br>
	 *  This method is intended to be used in the UI.
	 *  If you want to modify the state of the property from the view model,
	 *  as part of your business logic, you should use the {@code Var::set(T)} method instead.
	 *
	 * @param newValue The new value which ought to replace the old one.
	 * @return This very wrapper instance, in order to enable method chaining.
	 */
	Var<T> act(T newValue);


	/**
	 *  Essentially the same as {@link Optional#map(Function)}. but with a {@link Val} as return type.
	 *
	 * @param mapper the mapping function to apply to a value, if present
	 * @return the result of applying an {@code Optional}-bearing mapping
	 * @param <V> The type of the value returned from the mapping function
	 */
	@Override default <V> Var<V> map( java.util.function.Function<T, V> mapper ) {
		if ( !isPresent() )
			return Var.ofNullable( (Class<V>) Void.class, null );

		V newValue = mapper.apply( orElseNull() );
		if ( newValue == null )
			return Var.ofNullable( (Class<V>) Void.class, null );
		return Var.of( newValue );
	}

	/**
	 * @param type The type of the value returned from the mapping function
	 * @param mapper the mapping function to apply to a value, if present
	 * @return the result of applying an {@code Optional}-bearing mapping
	 * @param <U> The type of the value returned from the mapping function
	 */
	@Override default <U> Var<U> mapTo( Class<U> type, java.util.function.Function<T, U> mapper ) {
		if ( !isPresent() )
			return Var.ofNullable( type, null );

		U newValue = mapper.apply( orElseNull() );
		if ( newValue == null )
			return Var.ofNullable( type, null );
		return Var.of( newValue );
	}


}
