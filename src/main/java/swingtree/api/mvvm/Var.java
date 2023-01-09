package swingtree.api.mvvm;

import java.util.Optional;
import java.util.function.Function;

/**
 * 	A mutable wrapper for an item which can be observed by the Swing-Tree UI
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
 * 	Your view will automatically update the text field with the item of the property
 * 	and inside your view model you can also update the item of the property
 * 	to be shown in the UI:
 * 	<pre>{@code
 * 	    // Initially empty username:
 * 		username.set( "" ).show();
 * 	}</pre>
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 *
 * @param <T> The type of the wrapped item.
 */
public interface Var<T> extends Val<T>
{
	/**
	 *  Use this factory method to create a new {@link Var} instance
	 *  whose item may or may not be null.
	 *  <p>
	 *  <b>Example:</b>
	 *  <pre>{@code
	 *      Var.ofNullable(String.class, null);
	 *  }</pre>
	 *  <p>
	 * @param type The type of the item wrapped by the property.
	 *             This is used to check if the item is of the correct type.
	 * @param item The initial item of the property.
	 *              This may be null.
	 * @param <T> The type of the wrapped item.
	 * @return A new {@link Var} instance.
	 */
	static <T> Var<T> ofNullable( Class<T> type, T item ) {
		return AbstractVariable.ofNullable( false, type, item );
	}

	/**
	 * 	This factory method returns a {@code Var} describing the given non-{@code null}
	 * 	item similar to {@link Optional#of(Object)}, but specifically
	 * 	designed for use with Swing-Tree.
	 *
	 * @param item The initial item of the property which must not be null.
	 * @param <T> The type of the item held by the {@link Var}!
	 * @return A new {@link Var} instance wrapping the given item.
	 */
	static <T> Var<T> of( T item ) { return AbstractVariable.of( false, item ); }

	/**
	 * 	This factory method returns a {@code Var} describing the given non-{@code null}
	 * 	item similar to {@link Optional#of(Object)} and its type which
	 * 	may also be a super type of the item.
	 *
	 * @param type The type of the item wrapped by the property.
	 * @param item The initial item of the property which must not be null.
	 * @param <T> The type of the item held by the {@link Var}!
	 * @return A new {@link Var} instance wrapping the given item.
	 */
	static <T, V extends T> Var<T> of( Class<T> type, V item ) { return AbstractVariable.of( false, type, item ); }


	/**
	 * 	This factory method returns a {@code Var} describing the given non-{@code null}
	 * 	{@link Viewable} type. A {@link Var} is similar to {@link Optional#of(Object)}, but specifically
	 * 	designed for use with Swing-Tree.
	 *
	 * @param item The initial item of the property which must not be null.
	 * @return A new {@link Var} instance wrapping the given {@link Viewable} type.
	 */
	static Var<Viewable> of(Viewable item ) { return AbstractVariable.of( false, item ); }

	/**
	 *  This method provides the ability to change the state of the wrapper.
	 *  It might have several side effects depending on the implementation.
	 *
	 * @param newItem The new item which ought to replace the old one.
	 * @return This very wrapper instance, in order to enable method chaining.
	 */
	Var<T> set( T newItem );

	/**
	 *  Use this method to create a new property with an id.
	 *  This id is used to identify the property in the UI
	 *  or as a key in a map, which is useful when converting your
	 *  view model to a JSON object, or similar formats.
	 *
	 * @param id The id of the property.
	 * @return A new {@link Var} instance with the given id.
	 */
	@Override Var<T> withId( String id );

	/**
	 *  Use this method to add an action to this property which is supposed to be triggered
	 *  when the UI changes the item of this property through
	 *  the {@code Var::act(T)} method, or simply when it is explicitly
	 *  triggered by the {@code Var::act(T)} method.
	 *
	 * @param action The action to be triggered when {@code Var::act()} or {@code Var::act(T)} is called.
	 * @return This very property instance, in order to enable method chaining.
	 */
	Var<T> onAct( Action<ValDelegate<T>> action );

	/**
	 *  Triggers the action associated with this property, if one was
	 *  set using {@link #onAct(Action)}.
	 *  This method is intended to be used in the UI
	 *  to indicate that the user has changed the item of the property
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
	 * @param newValue The new item which ought to replace the old one.
	 * @return This very wrapper instance, in order to enable method chaining.
	 */
	Var<T> act( T newValue );

	/**
	 *  Essentially the same as {@link Optional#map(Function)}. but with a {@link Val} as return type.
	 *
	 * @param mapper the mapping function to apply to an item, if present
	 * @return the result of applying an {@code Optional}-bearing mapping
	 */
	@Override default Var<T> map( java.util.function.Function<T, T> mapper ) {
		if ( !isPresent() )
			return Var.ofNullable( type(), null );

		T newValue = mapper.apply( orElseNull() );
		if ( newValue == null )
			return Var.ofNullable( type(), null );

		return Var.of( newValue );
	}

	/**
	 * @param type The type of the item returned from the mapping function
	 * @param mapper the mapping function to apply to an item, if present
	 * @return the result of applying an {@code Optional}-bearing mapping
	 * @param <U> The type of the item returned from the mapping function
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
