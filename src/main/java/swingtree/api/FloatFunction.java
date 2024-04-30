package swingtree.api;

import com.google.errorprone.annotations.Immutable;

/**
 *  Used by {@link NoiseFunction#andThen(FloatFunction)}
 *  to apply a scalar function to the result of a noise function
 *  or to the input of a noise function using {@link NoiseFunction#compose(FloatFunction)}.
 */
@Immutable
@FunctionalInterface
public interface FloatFunction
{
    /**
     * @param in The input float value.
     * @return The output float value.
     */
    float from( float in );

    /**
     *  Allows you to compose this function with another function
     *  so that the other function is applied after this function.
     *
     * @param after The function to apply after this function.
     * @return A new function that applies this function and then the given function.
     */
    default FloatFunction andThen( FloatFunction after ) {
        return (x) -> after.from(from(x));
    }

    /**
     *  Allows you to compose this function with another function
     *  so that the other function is applied before this function.
     *
     * @param before The function to apply before this function.
     * @return A new function that applies the given function and then this function.
     */
    default FloatFunction compose( FloatFunction before ) {
        return (x) -> from(before.from(x));
    }
}
