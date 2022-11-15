package com.globaltcad.swingtree.api.mvvm;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public interface Val<T>
{

	static <T> Val<T> of( Class<T> type, T value, String id ) {
		return Var.of( type, value, id );
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
	static <T> Val<T> of( T iniValue ) {
		return Var.of( iniValue );
	}


	String UNNAMED = "UNNAMED"; // This is the default name for properties

	/**
	 *  This method simply returns the wrapped value.
	 *  Calling it should not have any side effects.
	 *
	 * @return The value wrapped by an implementation of this interface.
	 */
	T get();

	/**
	 *  This method simply returns a {@link String} representation of the wrapped value
	 *  which would otherwise be accessed via the {@link #get()} method.
	 *  Calling it should not have any side effects.
	 *
	 * @return The {@link String} representation of the value wrapped by an implementation of this interface.
	 */
	default String valueAsString() { return String.valueOf(this.get()); }

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
		T current = this.get();
		return equals(current, otherValue);
	}

	/**
	 *  This returns the name of the {@link Var} which is useful for debugging as well as
	 *  persisting their state by using the names as keys for whatever storage medium one chooses.
	 *
	 * @return The name which is assigned to this {@link Var}.
	 */
	String id();

	default boolean isUnnamed() { return UNNAMED.equals(id()); }

	/**
	 *  This returns the type of the value wrapped by this {@link Var}
	 *  which can be accessed by calling the {@link Var#get()} method.
	 *
	 * @return The type of the value wrapped by the {@link Var}.
	 */
	Class<T> type();

	default Optional<T> toOptional() {
		return Optional.of(this.get());
	}

	Val<T> onViewThis( Consumer<Val<T>> displayAction );

	default Val<T> onView( Consumer<T> displayAction ) {
		return onViewThis( v -> displayAction.accept(v.get()) );
	}

	Val<T> view();

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
	static boolean equals(Object o1, Object o2) {
		if ( o1 instanceof float[]  ) return Arrays.equals( (float[] ) o1, (float[] ) o2 );
		if ( o1 instanceof int[]    ) return Arrays.equals( (int[]   ) o1, (int[]   ) o2 );
		if ( o1 instanceof char[]   ) return Arrays.equals( (char[]  ) o1, (char[]  ) o2 );
		if ( o1 instanceof double[] ) return Arrays.equals( (double[]) o1, (double[]) o2 );
		if ( o1 instanceof long[]   ) return Arrays.equals( (long[]  ) o1, (long[]  ) o2 );
		if ( o1 instanceof byte[]   ) return Arrays.equals( (byte[]  ) o1, (byte[]  ) o2 );
		if ( o1 instanceof short[]  ) return Arrays.equals( (short[] ) o1, (short[] ) o2 );
		if ( o1 instanceof Object[] ) return Arrays.equals( (Object[]) o1, (Object[]) o2 );
		return Objects.equals(o1, o2);
	}

}
