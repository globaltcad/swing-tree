package com.globaltcad.swingtree.api.mvvm;

import java.util.Objects;

public interface Var<T> extends Val<T>
{

	static <T> Var<T> of( Class<T> type, T value, String id ) {
		return new AbstractVariable<T>(type, value, id){};
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
		return Var.of( (Class<T>) iniValue.getClass(), iniValue, UNNAMED );
	}

	
	/**
	 *  This method provides the ability to change the state of the wrapper.
	 *  It might have several side effects depending on the implementation.
	 *
	 * @param newValue The new value which ought to replace the old one.
	 * @return This very wrapper instance, in order to enable method chaining.
	 */
	Var<T> set(T newValue );

}
