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
 * See {@link swingtree.UI.NoiseType} for the different types of
 * default noise function implementations.
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
    double getFractionAt( float x, float y );
}
