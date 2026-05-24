package swingtree.api;


import com.google.errorprone.annotations.Immutable;
import sprouts.Tuple;
import swingtree.style.ShadowConf;

/**
 * A shadow fractions supplier is used to render {@link ShadowConf} styles by supplying
 * the shape of a shadow's "falloff curve", that is to say, the way in which the shadow
 * color fades from its full strength into full transparency across the blur region.
 * <p>
 * It supplies a {@link Tuple} of {@code float} values, the <i>falloff fractions</i>, which
 * are the shadow intensities ({@code 1} = full shadow color, {@code 0} = fully transparent)
 * sampled at evenly spaced positions {@code t = i / n} for {@code i} in {@code [0, n]}, where
 * {@code n} is one less than the size of the returned tuple. The first fraction therefore
 * describes the intensity at the solid edge of the shadow (typically {@code 1}) and the last
 * one the intensity at the far, transparent end of the blur (typically {@code 0}). The number
 * of supplied fractions also determines how finely the curve is sampled: smooth curves need
 * only a few fractions, while stepped or oscillating curves need many more to stay crisp.
 * <p>
 * A gradient is built from at least two color stops, so the returned tuple <b>must contain at
 * least two fractions</b>. The renderer treats a smaller tuple as a configuration error and
 * defensively falls back to a plain linear falloff ({@code [1, 0]}) rather than failing.
 * <p>
 * <b>Please take a look at {@link swingtree.UI.ShadowType} for a rich set of
 * predefined shadow falloff implementations.<br>
 * There you will most likely find a shadow type that fits your needs.</b>
 */
@Immutable
@FunctionalInterface
public interface ShadowFractionsSupplier
{
    /**
     * Supplies the shadow falloff fractions, that is the shadow intensities
     * ({@code 1} = full shadow color, {@code 0} = fully transparent) sampled at
     * evenly spaced positions {@code t = i / n} away from the solid edge of the shadow,
     * where {@code n} is one less than the size of the returned tuple.
     *
     * @return A {@link Tuple} of at least two falloff fractions, ordered from the solid edge
     *          of the shadow ({@code t = 0}) to the far, transparent end of the blur ({@code t = 1}).
     */
    Tuple<Float> getFractions();
}