package swingtree.api;


import swingtree.style.NoiseConf;

/**
 * A noise function is used to render {@link NoiseConf} styles
 * by taking a float based
 * coordinate and returning a gradient value between 0 and 1.
 * The value is used to determine the color of a pixel in virtual space.
 * Conceptually speaking, this maps the translated, scaled and rotated virtual space
 * to a gradient value of a pixel in the color space / view space of the screen.
 * <p>
 * <b>Please take a look at {@link swingtree.UI.NoiseType} for a rich set of
 * predefined noise function implementations.<br>
 * There you will most likely find a noise function that fits your needs.</b>
 */
public interface NoiseFunction
{
    /**
     * Get the noise value at the given coordinate.
     * @param x The x coordinate in translated, scaled and rotated virtual space.
     * @param y The y coordinate in translated, scaled and rotated virtual space.
     * @return The gradient value between 0 and 1 used to determine the color of a pixel
     *          in the color space / view space of the screen.
     */
    float getFractionAt( float x, float y );

    /**
     * Compose this noise function with another (scalar) function.
     * @param after The function to apply after this function.
     * @return A new noise function that applies this noise function and then the given scalar function,
     *          which takes the result of this noise function as input.
     */
    default NoiseFunction andThen( FloatFunction after ) {
        return (x, y) -> after.from(getFractionAt(x, y));
    }

    /**
     * Compose this noise function with another (scalar) function.
     * @param before The function to apply to the input of this function,
     *               which is the x and y coordinate in translated, scaled and rotated virtual space.
     * @return A new noise function that applies the given scalar function and then this noise function,
     *          which takes the result of the given scalar function as input.
     */
    default NoiseFunction compose( FloatFunction before ) {
        return (x, y) -> getFractionAt(before.from(x), before.from(y));
    }

}
