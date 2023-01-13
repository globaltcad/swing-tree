package swingtree.api.mvvm;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * 	A read only view on an item which can be observed by the Swing-Tree UI
 * 	to dynamically update UI components for you.
 * 	The API of this is very similar to the {@link Optional} API in the
 * 	sense that it is a wrapper around a single item, which may also be missing (null).
 * 	Use the {@link #onShowItem(Action)} method to register a callbacks which
 * 	will be called when the {@link #show()} method is called.
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 *
 * @param <T> The type of the item held by this {@link Val}.
 */
public interface Val<T>
{
	String NO_ID = ""; // This is the default id for properties
	String EMPTY = "EMPTY"; // This is the default string for empty properties
	Pattern ID_PATTERN = Pattern.compile("[a-zA-Z0-9_]*");

	/**
	 *  Use this factory method to create a new {@link Val} instance
	 *  whose item may or may not be null.
	 *  <p>
	 *  <b>Example:</b>
	 *  <pre>{@code
	 *      Val.ofNullable(String.class, null);
	 *  }</pre>
	 *  <p>
	 * @param type The type of the item wrapped by the property.
	 *             This is used to check if the item is of the correct type.
	 * @param item The initial item of the property.
	 *              This may be null.
	 * @param <T> The type of the wrapped item.
	 * @return A new {@link Val} instance.
	 */
	static <T> Val<T> ofNullable( Class<T> type, T item ) { return AbstractVariable.ofNullable( true, type, item ); }

	/**
	 * 	This factory method returns a {@code Val} describing the given non-{@code null}
	 * 	item similar to {@link Optional#of(Object)}, but specifically
	 * 	designed for use with Swing-Tree.
	 *
	 * @param item The initial item of the property which must not be null.
	 * @param <T> The type of the item held by the {@link Val}!
	 * @return A new {@link Val} instance wrapping the given item.
	 */
	static <T> Val<T> of( T item ) { return AbstractVariable.of( true, item ); }

	/**
	 * This method is intended to be used for when you want to wrap non-nullable types.
	 * So if an item is present (not null), it returns the item, otherwise however
	 * {@code NoSuchElementException} will be thrown.
	 * If you simply want to get the item of this property irrespective of
	 * it being null or not, use {@link #orElseNull()} to avoid an exception.
	 * However, if this property wraps a nullable type, which is not intended to be null,
	 * please use {@link #orElseThrow()} to make this intention clear.
	 * The {@link #orElseThrow()} method is functionally identical to this method.
	 *
	 * @return the non-{@code null} item described by this {@code Val}
	 * @throws NoSuchElementException if no item is present
	 */
	default T get() { return orElseThrow(); }

	/**
	 * If an item is present, returns the item, otherwise returns
	 * {@code other}.
	 *
	 * @param other the item to be returned, if no item is present.
	 *        May be {@code null}.
	 * @return the item, if present, otherwise {@code other}
	 */
	T orElseNullable( T other );

	/**
	 * If an item is present, returns the item, otherwise returns
	 * {@code other}.
	 *
	 * @param other the item to be returned, if no item is present.
	 *        May not be {@code null}.
	 * @return the item, if present, otherwise {@code other}
	 */
	default T orElse( T other ) { return orElseNullable( Objects.requireNonNull(other) ); }

	/**
	 * If an item is present, returns the item, otherwise returns the result
	 * produced by the supplying function.
	 *
	 * @param supplier the supplying function that produces an item to be returned
	 * @return the item, if present, otherwise the result produced by the
	 *         supplying function
	 * @throws NullPointerException if no item is present and the supplying
	 *         function is {@code null}
	 */
	default T orElseGet( Supplier<? extends T> supplier ) { return this.isPresent() ? orElseThrow() : supplier.get(); }

	/**
	 * If an item is present, returns the item, otherwise returns
	 * {@code null}.
	 *
	 * @return the item, if present, otherwise {@code null}
	 */
	default T orElseNull() { return orElseNullable(null); }

	/**
	 * If an item is present, returns the item, otherwise throws
	 * {@code NoSuchElementException}.
	 *
	 * @return the non-{@code null} item described by this {@code Val}
	 * @throws NoSuchElementException if no item is present
	 */
	T orElseThrow();

	/**
	 * If an item is present, returns {@code true}, otherwise {@code false}.
	 *
	 * @return {@code true} if an item is present, otherwise {@code false}
	 */
	boolean isPresent();

	/**
	 * If an item is  not present, returns {@code true}, otherwise
	 * {@code false}.
	 *
	 * @return  {@code true} if an item is not present, otherwise {@code false}
	 */
	default boolean isEmpty() { return !isPresent(); }

	/**
	 * If an item is present, performs the given action with the item,
	 * otherwise does nothing.
	 *
	 * @param action the action to be performed, if an item is present
	 * @throws NullPointerException if item is present and the given action is
	 *         {@code null}
	 */
	default void ifPresent( Consumer<T> action ) {
		if ( this.isPresent() )
			action.accept( orElseThrow() );
	}

	/**
	 * If an item is present, performs the given action with the item,
	 * otherwise performs the given empty-based action.
	 *
	 * @param action the action to be performed, if an item is present
	 * @param emptyAction the empty-based action to be performed, if no item is
	 *        present
	 * @throws NullPointerException if an item is present and the given action
	 *         is {@code null}, or no item is present and the given empty-based
	 *         action is {@code null}.
	 */
	default void ifPresentOrElse( Consumer<? super T> action, Runnable emptyAction ) {
		if ( isPresent() )
			action.accept(orElseThrow());
		else
			emptyAction.run();
	}

	/**
	 * If an item is present, returns a {@code Val} describing the item,
	 * otherwise returns a {@code Val} produced by the supplying function.
	 *
	 * @param supplier the supplying function that produces a {@code Val}
	 *        to be returned
	 * @return returns a {@code Val} describing the item of this
	 *         {@code Val}, if an item is present, otherwise a
	 *         {@code Val} produced by the supplying function.
	 * @throws NullPointerException if the supplying function is {@code null} or
	 *         produces a {@code null} result
	 */
	default Val<T> or( Supplier<? extends Val<? extends T>> supplier ) {
		Objects.requireNonNull(supplier);
		if ( isPresent() ) return this;
		else {
			@SuppressWarnings("unchecked")
			Val<T> r = (Val<T>) supplier.get();
			return Objects.requireNonNull(r);
		}
	}

	/**
	 *  Essentially the same as {@link Optional#map(Function)}. but with a {@link Val} as return type.
	 *
	 * @param mapper the mapping function to apply to an item, if present
	 * @return A property that is created from this property based on the provided mapping function.
	 */
	Val<T> map( java.util.function.Function<T, T> mapper );

	/**
	 * @param type The type of the item returned from the mapping function
	 * @param mapper the mapping function to apply to an item, if present
	 * @return A property that is created from this property based on the provided mapping function.
	 * @param <U> The type of the item returned from the mapping function
	 */
	<U> Val<U> mapTo( Class<U> type, java.util.function.Function<T, U> mapper );

	/**
	 * 	Use this to create a live view of this property
	 * 	through a new property based on the provided mapping function.
	 * @param type The type of the item returned from the mapping function
 	 * @param mapper the mapping function to apply to an item, if present
	 * @return A property that is a live view of this property based on the provided mapping function.
	 * @param <U> The type of the item returned from the mapping function
	 */
	<U> Val<U> viewAs( Class<U> type, java.util.function.Function<T, U> mapper );

	/**
	 * 	Use this to create a live view of this property
	 * 	based on a mapping function.
	 * 	Note that the mapping function is not allowed to return {@code null}.
	 * 	Instead, use {@link #viewAs(Class, Function)}.
	 *
	 * @param mapper the mapping function to apply to an item, if present
	 * @return A property that is a live view of this property based on the provided mapping function.
	 */
	default Val <T> view( java.util.function.Function<T, T> mapper ) { return viewAs( type(), mapper ); }

	/**
	 * 	Use this to create a String based live view of this property
	 * 	through a new property based on the provided mapping function.
	 * @param mapper the mapping function to turn the item of this property to a String, if present
	 * @return A property that is a live view of this property based on the provided mapping function.
	 */
	default Val<String> viewAsString( Function<T, String> mapper ) {
		return viewAs( String.class, mapper );
	}

	/**
	 * 	Use this to create a String based live view of this property
	 * 	through a new property based on the "toString" called on the item of this property.
	 *
	 * @return A String property that is a live view of this property.
	 */
	default Val<String> viewAsString() { return viewAsString( Objects::toString ); }

	/**
	 * 	Use this to create a Double based live view of this property
	 * 	through a new property based on the provided mapping function.
	 *
	 * @param mapper the mapping function to turn the item of this property to a Double, if present
	 * @return A property that is a live view of this property based on the provided mapping function.
	 */
	default Val<Double> viewAsDouble( java.util.function.Function<T, Double> mapper ) {
		return viewAs( Double.class, v -> {
			try {
				return mapper.apply(v);
			} catch (Exception e) {
				return Double.NaN;
			}
		});
	}

	/**
	 * 	Use this to create a Double based live view of this property
	 * 	through a new property based on the "toString" and "parseDouble(String)" methods.
	 * 	If the String cannot be parsed to a Double, the item of the property will be Double.NaN.
	 *
	 * @return A Double property that is a live view of this property.
	 */
	default Val<Double> viewAsDouble() {
		return viewAsDouble( v -> {
			try {
				return Double.parseDouble( v.toString() );
			} catch ( NumberFormatException e ) {
				return Double.NaN;
			}
		});
	}

	/**
	 * 	Use this to create an Integer based live view of this property
	 * 	through a new property based on the provided mapping function.
	 *
	 * @param mapper the mapping function to turn the item of this property to a Integer, if present
	 * @return A property that is a live view of this property based on the provided mapping function.
	 */
	default Val<Integer> viewAsInt( java.util.function.Function<T, Integer> mapper ) {
		return viewAs( Integer.class, v -> {
			try {
				return mapper.apply(v);
			} catch (Exception e) {
				return Integer.MIN_VALUE;
			}
		});
	}

	/**
	 * 	Use this to create an Integer based live view of this property
	 * 	through a new property based on the "toString" and "parseInt(String)" methods.
	 * 	If the String cannot be parsed to an Integer, the item of the property will be Integer.MIN_VALUE.
	 *
	 * @return An Integer property that is a live view of this property.
	 */
	default Val<Integer> viewAsInt() {
		return viewAsInt( v -> {
			try {
				return Integer.parseInt( v.toString() );
			} catch ( NumberFormatException e ) {
				return Integer.MIN_VALUE;
			}
		});
	}

	/**
	 *  This method simply returns a {@link String} representation of the wrapped item
	 *  which would otherwise be accessed via the {@link #orElseThrow()} method.
	 *  Calling it should not have any side effects.
	 *
	 * @return The {@link String} representation of the item wrapped by an implementation of this interface.
	 */
	default String itemAsString() { return this.mapTo(String.class, String::valueOf).orElseNullable(EMPTY); }

	/**
	 *  This method returns a {@link String} representation of the type of the wrapped item.
	 *  Calling it should not have any side effects.
	 *
	 * @return A simple {@link String} representation of the type of the item wrapped by an implementation of this interface.
	 */
	default String typeAsString() { return this.type().getName(); }

	/**
	 *  This method check if the provided item is equal to the item wrapped by this {@link Var} instance.
	 *
	 * @param otherItem The other item of the same type as is wrapped by this.
	 * @return The truth value determining if the provided item is equal to the wrapped item.
	 */
	default boolean is( T otherItem ) {
		T current = this.orElseNullable(null);
		return equals(current, otherItem);
	}

	/**
	 *  This method check if the item by the provided property
	 *  is equal to the item wrapped by this {@link Var} instance.
	 *
	 * @param other The other property of the same type as is wrapped by this.
	 * @return The truth value determining if the item of the supplied property is equal to the wrapped item.
	 */
	default boolean is(  Val<T> other ) {
		Objects.requireNonNull(other);
		return this.is( other.orElseNullable(null) );
	}

	/**
	 *  This method check if the provided item is not equal to the item wrapped by this {@link Val} instance.
	 *  This is the opposite of {@link #is(Object)} which returns true if the items are equal.
	 *
	 * @param otherItem The other item of the same type as is wrapped by this.
	 * @return The truth value determining if the provided item is not equal to the wrapped item.
	 */
	default boolean isNot( T otherItem ) { return !this.is(otherItem); }

	/**
	 *  This method check if the item of the provided property
	 *  is not equal to the item wrapped by this {@link Val} instance.
	 *  This is the opposite of {@link #is(Val)} which returns true if the items are equal.
	 *
	 * @param other The other property of the same type as is wrapped by this.
	 * @return The truth value determining if the item of the supplied property is not equal to the wrapped item.
	 */
	default boolean isNot( Val<T> other ) { return !this.is(other); }

	/**
	 *  This method check if at least one of the provided items is equal to
	 *  the item wrapped by this {@link Var} instance.
	 *
	 * @param first The first item of the same type as is wrapped by this.
	 * @param second The second item of the same type as is wrapped by this.
	 * @param otherValues The other items of the same type as is wrapped by this.
	 * @return The truth value determining if the provided item is equal to the wrapped item.
	 */
	default boolean isOneOf( T first, T second, T... otherValues ) {
		if ( this.is(first) ) return true;
		if ( this.is(second) ) return true;
		for ( T otherValue : otherValues )
			if ( this.is(otherValue) ) return true;
		return false;
	}

	/**
	 *  This returns the name/id of the property which is useful for debugging as well as
	 *  persisting their state by using them as keys for whatever storage data structure one chooses.
	 *
	 * @return The id which is assigned to this property.
	 */
	String id();

	/**
	 *  Use this method to create a new property with an id.
	 *  This id is used to identify the property in the UI
	 *  or as a key in a map, which is useful when converting your
	 *  view model to a JSON object, or similar formats.
	 *
	 * @param id The id of the property.
	 * @return A new {@link Val} instance with the given id.
	 */
	Val<T> withId( String id );

	/**
	 * @return True when this property has not been assigned an id.
	 */
	default boolean hasNoID() { return !hasID(); }

	/**
	 * @return The truth value determining if this property has been assigned an id.
	 */
	default boolean hasID() { return !NO_ID.equals(id()); }

	/**
	 *  This returns the type of the item wrapped by this {@link Var}
	 *  which can be accessed by calling the {@link Var#orElseThrow()} method.
	 *
	 * @return The type of the item wrapped by the {@link Var}.
	 */
	Class<T> type();

	/**
	 *  Use this to turn this property to an {@link Optional} which can be used to
	 *  interact with the item wrapped by this {@link Val} in a more functional way.
	 * @return An {@link Optional} wrapping the item wrapped by this {@link Val}.
	 */
	default Optional<T> toOptional() { return Optional.ofNullable(this.orElseNull()); }

	/**
	 *  Use this to register an observer lambda which will be called whenever the item
	 *  wrapped by this {@link Val} changes through the {@code Var::set(T)} method.
	 *  The lambda will receive a delegate which not only exposes the current
	 *  item of this property, but also a fixed number of previous items.
	 *
	 * @param displayAction The lambda which will be called whenever the item wrapped by this {@link Var} changes.
	 * @return The {@link Val} instance itself.
	 */
	Val<T> onShow( Action<ValDelegate<T>> displayAction );

	/**
	 *  Use this to register an observer lambda which will be called whenever the item
	 *  wrapped by this {@link Val} changes through the {@code Var::set(T)} method.
	 *  The lambda will receive the current item of this property.
	 *
	 * @param displayAction The lambda which will be called whenever the item wrapped by this {@link Var} changes.
	 * @return The {@link Val} instance itself.
	 */
	default Val<T> onShowItem( Action<T> displayAction ) {
		return onShow(new Action<ValDelegate<T>>() {
			@Override
			public void accept(ValDelegate<T> delegate) {
				displayAction.accept( delegate.current().orElseNullable(null));
			}
			@Override public boolean canBeRemoved() { return displayAction.canBeRemoved(); }
		});
	}

	/**
	 *  Triggers the observer lambdas registered through the {@link #onShowItem(Action)}
	 *  as well as the {@link #onShow(Action)} methods.
	 *  This method is called automatically by the {@code Var::set(T)} method,
	 *  and it is supposed to be used by the UI to update the UI components.
	 *  This is in essence how binding works in Swing-Tree.
	 *
	 * @return The {@link Val} instance itself.
	 */
	Val<T> show();

	/**
	 * @return If this property can contain null.
	 */
	boolean allowsNull();

	/**
	 *  {@link Val} and {@link Var} implementations are expected to represent
	 *  simple wrappers for data centric quasi value types!
	 *  So two primitive arrays of integers for example would not be recognized as
	 *  equal when calling one of their {@link Object#equals(Object)} methods
	 *  because the method does not compare the contents of the two arrays, it compares the
	 *  identities of the arrays!
	 *  This method defines what it means for 2 property items to be equal.
	 *  So in this example it ensures that two {@link Var} instances wrapping
	 *  different arrays but with the same contents are treated as the same items.
	 *
	 * @param o1 The first object which ought to be compared to the second one.
	 * @param o2 The second object which ought to be compared to the first one.
	 * @return The truth value determining if the objects are equal in terms of their state!
	 */
	static boolean equals( Object o1, Object o2 ) {
		if ( o1 instanceof float[]   ) return Arrays.equals( (float[] )  o1, (float[] )  o2 );
		if ( o1 instanceof int[]     ) return Arrays.equals( (int[]   )  o1, (int[]   )  o2 );
		if ( o1 instanceof char[]    ) return Arrays.equals( (char[]  )  o1, (char[]  )  o2 );
		if ( o1 instanceof double[]  ) return Arrays.equals( (double[])  o1, (double[])  o2 );
		if ( o1 instanceof long[]    ) return Arrays.equals( (long[]  )  o1, (long[]  )  o2 );
		if ( o1 instanceof byte[]    ) return Arrays.equals( (byte[]  )  o1, (byte[]  )  o2 );
		if ( o1 instanceof short[]   ) return Arrays.equals( (short[] )  o1, (short[] )  o2 );
		if ( o1 instanceof boolean[] ) return Arrays.equals( (boolean[]) o1, (boolean[]) o2 );
		if ( o1 instanceof Object[]  ) return Arrays.equals( (Object[])  o1, (Object[])  o2 );
		return Objects.equals( o1, o2 );
	}

	/**
	 * 	{@link Val} and {@link Var} implementations require their own {@link Object#hashCode()}
	 * 	method because they are supposed to be viewed as data centric quasi value types!
	 * 	So two arrays of integer for example would not have the same hash code when calling
	 * 	{@link Object#hashCode()} on them.
	 * 	This is because the method does not compare the contents of the two arrays!
	 *
	 * @param o The object for which a hash code is required.
	 * @return The hash code of the object.
	 */
	static int hashCode( Object o ) {
		if ( o instanceof float[]   ) return Arrays.hashCode( (float[] )  o );
		if ( o instanceof int[]     ) return Arrays.hashCode( (int[]   )  o );
		if ( o instanceof char[]    ) return Arrays.hashCode( (char[]  )  o );
		if ( o instanceof double[]  ) return Arrays.hashCode( (double[])  o );
		if ( o instanceof long[]    ) return Arrays.hashCode( (long[]  )  o );
		if ( o instanceof byte[]    ) return Arrays.hashCode( (byte[]  )  o );
		if ( o instanceof short[]   ) return Arrays.hashCode( (short[] )  o );
		if ( o instanceof boolean[] ) return Arrays.hashCode( (boolean[]) o );
		if ( o instanceof Object[]  ) return Arrays.hashCode( (Object[])  o );
		return Objects.hashCode( o );
	}

}
