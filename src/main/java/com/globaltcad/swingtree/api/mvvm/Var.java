package com.globaltcad.swingtree.api.mvvm;

import java.util.Objects;

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
	static <T> Var<T> of( Class<T> type, T value ) {
		return new AbstractVariable<T>( type, value, UNNAMED, null, true ){};
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
		Objects.requireNonNull(iniValue);
		return new AbstractVariable<T>( (Class<T>) iniValue.getClass(), iniValue, UNNAMED, null, false ){};
	}

	
	/**
	 *  This method provides the ability to change the state of the wrapper.
	 *  It might have several side effects depending on the implementation.
	 *
	 * @param newValue The new value which ought to replace the old one.
	 * @return This very wrapper instance, in order to enable method chaining.
	 */
	Var<T> set( T newValue );

	@Override Var<T> withID( String id );

	Var<T> withAction( PropertyAction<T> action );

	Var<T> act();

	Var<T> act(T newValue);
}
