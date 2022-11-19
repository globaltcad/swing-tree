package com.globaltcad.swingtree.api.mvvm;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface Val<T>
{
	String UNNAMED = "UNNAMED"; // This is the default name for properties

	static <T> Val<T> of( Class<T> type, T value ) { return Var.of( type, value ); }

	/**
	 *  This factory method will expose a builder which will create a very simple type of Property,
	 *  namely: {@link AbstractVariable}!
	 *  It has a simple implementation for everything defined in the {@link Var} interface.
	 *  It is similar to an {@link Optional} with the additional feature of being mutable
	 *  as well as being able to be observed for changes.
	 *
	 * @param iniValue The initial value set to the instance built by this builder. Note: An initialization will overwrite this.
	 * @param <T> The type of the value held by the {@link Var}!
	 * @return The builder for a {@link AbstractVariable}.
	 */
	static <T> Val<T> of( T iniValue ) { return Var.of( iniValue ); }

	/**
	 * If a value is present, returns the value, otherwise returns
	 * {@code other}.
	 *
	 * @param other the value to be returned, if no value is present.
	 *        May be {@code null}.
	 * @return the value, if present, otherwise {@code other}
	 */
	T orElseNullable(T other);

	/**
	 * If a value is present, returns the value, otherwise returns
	 * {@code other}.
	 *
	 * @param other the value to be returned, if no value is present.
	 *        May not be {@code null}.
	 * @return the value, if present, otherwise {@code other}
	 */
	default T orElse( T other ) { return orElseNullable( Objects.requireNonNull(other) ); }

	/**
	 * If a value is present, returns the value, otherwise returns the result
	 * produced by the supplying function.
	 *
	 * @param supplier the supplying function that produces a value to be returned
	 * @return the value, if present, otherwise the result produced by the
	 *         supplying function
	 * @throws NullPointerException if no value is present and the supplying
	 *         function is {@code null}
	 */
	default T orElseGet( Supplier<? extends T> supplier ) {
		return this.isPresent() ? orElseThrow() : supplier.get();
	}

	/**
	 * If a value is present, returns the value, otherwise returns
	 * {@code null}.
	 *
	 * @return the value, if present, otherwise {@code null}
	 */
	default T orElseNull() { return orElseNullable(null); }

	/**
	 * If a value is present, returns the value, otherwise throws
	 * {@code NoSuchElementException}.
	 *
	 * @return the non-{@code null} value described by this {@code Val}
	 * @throws NoSuchElementException if no value is present
	 */
	T orElseThrow();

	/**
	 * If a value is present, returns {@code true}, otherwise {@code false}.
	 *
	 * @return {@code true} if a value is present, otherwise {@code false}
	 */
	boolean isPresent();

	/**
	 * If a value is  not present, returns {@code true}, otherwise
	 * {@code false}.
	 *
	 * @return  {@code true} if a value is not present, otherwise {@code false}
	 */
	default boolean isEmpty() { return !isPresent(); }

	/**
	 * If a value is present, performs the given action with the value,
	 * otherwise does nothing.
	 *
	 * @param action the action to be performed, if a value is present
	 * @throws NullPointerException if value is present and the given action is
	 *         {@code null}
	 */
	default void ifPresent( Consumer<T> action ) {
		if ( this.isPresent() )
			action.accept( orElseThrow() );
	}

	/**
	 * If a value is present, performs the given action with the value,
	 * otherwise performs the given empty-based action.
	 *
	 * @param action the action to be performed, if a value is present
	 * @param emptyAction the empty-based action to be performed, if no value is
	 *        present
	 * @throws NullPointerException if a value is present and the given action
	 *         is {@code null}, or no value is present and the given empty-based
	 *         action is {@code null}.
	 */
	default void ifPresentOrElse( Consumer<? super T> action, Runnable emptyAction ) {
		if ( isPresent() )
			action.accept(orElseThrow());
		else
			emptyAction.run();
	}

	/**
	 * If a value is present, returns a {@code Val} describing the value,
	 * otherwise returns a {@code Val} produced by the supplying function.
	 *
	 * @param supplier the supplying function that produces a {@code Val}
	 *        to be returned
	 * @return returns a {@code Val} describing the value of this
	 *         {@code Val}, if a value is present, otherwise a
	 *         {@code Val} produced by the supplying function.
	 * @throws NullPointerException if the supplying function is {@code null} or
	 *         produces a {@code null} result
	 */
	default Val<T> or(Supplier<? extends Val<? extends T>> supplier) {
		Objects.requireNonNull(supplier);
		if ( isPresent() ) return this;
		else {
			@SuppressWarnings("unchecked")
			Val<T> r = (Val<T>) supplier.get();
			return Objects.requireNonNull(r);
		}
	}

	default <V> Val<V> map( java.util.function.Function<T, V> mapper ) {
		if ( !isPresent() )
			return Val.of( (Class<V>) Void.class, null );
		return Val.of( mapper.apply( orElseNull() ) );
	}

	/**
	 *  This method simply returns a {@link String} representation of the wrapped value
	 *  which would otherwise be accessed via the {@link #orElseThrow()} method.
	 *  Calling it should not have any side effects.
	 *
	 * @return The {@link String} representation of the value wrapped by an implementation of this interface.
	 */
	default String valueAsString() { return this.map(String::valueOf).orElseNullable("EMPTY"); }

	/**
	 *  This method returns a {@link String} representation of the type of the wrapped value.
	 *  Calling it should not have any side effects.
	 *
	 * @return A simple {@link String} representation of the type of the value wrapped by an implementation of this interface.
	 */
	default String typeAsString() { return this.type().getName(); }

	/**
	 *  This method check if the provided value is equal to the value wrapped by this {@link Var} instance.
	 *
	 * @param otherValue The other value of the same type as is wrapped by this.
	 * @return The truth value determining if the provided value is equal to the wrapped value.
	 */
	default boolean is( T otherValue ) {
		T current = this.orElseNullable(null);
		return equals(current, otherValue);
	}

	/**
	 *  This returns the name/id of the {@link Var} which is useful for debugging as well as
	 *  persisting their state by using them as keys for whatever storage data structure one chooses.
	 *
	 * @return The id which is assigned to this {@link Var}.
	 */
	String id();

	Val<T> withID( String id );

	default boolean isUnnamed() { return UNNAMED.equals(id()); }

	/**
	 *  This returns the type of the value wrapped by this {@link Var}
	 *  which can be accessed by calling the {@link Var#orElseThrow()} method.
	 *
	 * @return The type of the value wrapped by the {@link Var}.
	 */
	Class<T> type();

	default Optional<T> toOptional() { return Optional.ofNullable(this.orElseNull()); }

	Val<T> onShowThis( Consumer<Val<T>> displayAction );

	default Val<T> onShow( Consumer<T> displayAction ) {
		return onShowThis( property -> displayAction.accept( property.orElseNullable(null)) );
	}

	Val<T> show();

	/**
	 *  The values of {@link Var} implementations ought to be viewed
	 *  as wrapper for data centric quasi value types!
	 *  Two arrays of integer for example would not be recognized as
	 *  equal when calling one of their {@link int[]#equals(Object, Object)} methods.
	 *  This is because the method does not compare the contents of the two arrays!
	 *  In order to ensure that two {@link Var} values are viewed as the same values
	 *  simply when the data is identical we use the following utility method to
	 *  account for these exceptional situation...
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
