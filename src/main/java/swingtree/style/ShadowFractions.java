package swingtree.style;

import org.jspecify.annotations.Nullable;
import sprouts.Tuple;

/**
 *  A collection of shadow "falloff" functions that describe how a shadow color fades from
 *  its full strength into full transparency across the blur region of a {@link ShadowConf}.
 *  Each method here samples a particular falloff curve {@code f(t)} (shadow intensity as a
 *  function of the normalized distance {@code t} in {@code [0, 1]} away from the solid edge
 *  of the shadow) at evenly spaced positions and returns the resulting fractions as a
 *  {@link Tuple} of {@code float} values, as described by
 *  {@link swingtree.api.ShadowFractionsSupplier}.
 *  <p>
 *  The functions in this class are also supposed to serve as an example which demonstrates
 *  how to create custom shadow falloff curves yourself. See {@link swingtree.UI.ShadowFalloff}
 *  for the real world phenomenon and the exact math behind each curve.
 */
public final class ShadowFractions
{
    /**
     *  The number of sampling intervals used to approximate a smooth, monotonic shadow
     *  falloff curve as a piece-wise linear multi-stop gradient. A gradient interpolates
     *  linearly between its stops, so we sample the falloff curve at this many intervals to
     *  get a smooth looking curve while keeping the per-render allocation small.
     */
    private static final int SAMPLES = 12;

    /**
     *  The (higher) number of sampling intervals used for the eccentric, non-monotonic or
     *  sharp-edged falloff curves ({@link #stairs()}, {@link #ripple()}, {@link #sawtooth()},
     *  {@link #bounce()}), whose steps and waves would be smeared away by the coarser
     *  smooth-curve sampling.
     */
    private static final int SAMPLES_FINE = 64;


    // The falloff curves are constant, so each is sampled lazily on first use and the
    // resulting immutable tuple is then cached here to be shared across all subsequent
    // renders (see the accessors below). A benign data race may sample a curve more than
    // once, but as the tuples are immutable and value-equal that is harmless.
    private static @Nullable Tuple<Float> FLAT;
    private static @Nullable Tuple<Float> PENUMBRA;
    private static @Nullable Tuple<Float> BLUR;
    private static @Nullable Tuple<Float> GLOW;
    private static @Nullable Tuple<Float> CONTACT;
    private static @Nullable Tuple<Float> STAIRS;
    private static @Nullable Tuple<Float> RIPPLE;
    private static @Nullable Tuple<Float> SAWTOOTH;
    private static @Nullable Tuple<Float> BOUNCE;


    private ShadowFractions(){}


    /**
     *  A constant rate fade from full shadow color to transparency, producing a
     *  perfectly straight falloff. <b>Falloff:</b> {@code f(t) = 1 - t}.
     *  As a straight line is fully described by its two endpoints, this needs only two
     *  fractions and so allocates the smallest possible tuple.
     *
     *  @return A tuple of shadow falloff fractions sampled at evenly spaced positions.
     */
    public static Tuple<Float> flat() {
        Tuple<Float> fractions = FLAT;
        if ( fractions == null )
            FLAT = fractions = Tuple.of(new float[] {1f, 0f});
        return fractions;
    }

    /**
     *  A smooth, symmetric S-curve, a cheap polynomial approximation of {@link #blur()}.
     *  <b>Falloff (the "smoothstep" function):</b> {@code f(t) = 1 - t}<sup>2</sup>{@code (3 - 2t)}.
     *
     *  @return A tuple of shadow falloff fractions sampled at evenly spaced positions.
     */
    public static Tuple<Float> penumbra() {
        Tuple<Float> fractions = PENUMBRA;
        if ( fractions == null )
            PENUMBRA = fractions = _sample(SAMPLES, t -> 1f - (t * t * (3f - 2f * t)));
        return fractions;
    }

    /**
     *  The exact edge profile of a hard edge convolved with a Gaussian kernel, which is the
     *  normalized error function (NOT a bell shape). This is what a CSS box-shadow / Figma /
     *  Photoshop blur looks like.
     *  <p>
     *  <b>Falloff (normalized error function, with steepness {@code k}):</b><br>
     *  {@code f(t) = (erf(k/2) - erf(k(t - 1/2))) / (2 * erf(k/2))}
     *
     *  @return A tuple of shadow falloff fractions sampled at evenly spaced positions.
     */
    public static Tuple<Float> blur() {
        Tuple<Float> fractions = BLUR;
        if ( fractions == null )
            BLUR = fractions = _blur();
        return fractions;
    }

    /**
     *  A bell shaped (Gaussian) falloff, normalized so it reaches exactly 0 at {@code t == 1}.
     *  Mimics a diffuse glow / halo rather than a cast shadow edge.
     *  <p>
     *  <b>Falloff (normalized Gaussian bell, with width {@code k}):</b><br>
     *  {@code f(t) = (exp(-k t}<sup>2</sup>{@code ) - exp(-k)) / (1 - exp(-k))}
     *
     *  @return A tuple of shadow falloff fractions sampled at evenly spaced positions.
     */
    public static Tuple<Float> glow() {
        Tuple<Float> fractions = GLOW;
        if ( fractions == null )
            GLOW = fractions = _glow();
        return fractions;
    }

    /**
     *  A sharp drop near the edge with a long faint tail, normalized to 0 at {@code t == 1}.
     *  Mimics a contact shadow / ambient occlusion.
     *  <p>
     *  <b>Falloff (normalized exponential decay, with rate {@code k}):</b><br>
     *  {@code f(t) = (exp(-k t) - exp(-k)) / (1 - exp(-k))}
     *
     *  @return A tuple of shadow falloff fractions sampled at evenly spaced positions.
     */
    public static Tuple<Float> contact() {
        Tuple<Float> fractions = CONTACT;
        if ( fractions == null )
            CONTACT = fractions = _contact();
        return fractions;
    }

    /**
     *  Posterize the linear falloff into {@code N} discrete bands (cel-shaded look).
     *  <p>
     *  <b>Falloff (quantized linear, with {@code N} bands):</b><br>
     *  {@code f(t) = round((1 - t) * (N - 1)) / (N - 1)}
     *
     *  @return A tuple of shadow falloff fractions sampled at evenly spaced positions.
     */
    public static Tuple<Float> stairs() {
        Tuple<Float> fractions = STAIRS;
        if ( fractions == null )
            STAIRS = fractions = _stairs();
        return fractions;
    }

    /**
     *  A cosine wave under a linear decay envelope: fading concentric rings.
     *  <p>
     *  <b>Falloff (damped cosine, with {@code k} ripples):</b><br>
     *  {@code f(t) = (1 - t) * (1/2 + 1/2 * cos(2}&pi;{@code k t))}, with {@code k = 3}
     *
     *  @return A tuple of shadow falloff fractions sampled at evenly spaced positions.
     */
    public static Tuple<Float> ripple() {
        Tuple<Float> fractions = RIPPLE;
        if ( fractions == null )
            RIPPLE = fractions = _ripple();
        return fractions;
    }

    /**
     *  Repeating linear ramps under a decay envelope: fading louvers.
     *  <p>
     *  <b>Falloff (decaying sawtooth, with {@code k} louvers):</b><br>
     *  {@code f(t) = (1 - t) * (1 - frac(k t))}, with {@code k = 4}
     *
     *  @return A tuple of shadow falloff fractions sampled at evenly spaced positions.
     */
    public static Tuple<Float> sawtooth() {
        Tuple<Float> fractions = SAWTOOTH;
        if ( fractions == null )
            SAWTOOTH = fractions = _sawtooth();
        return fractions;
    }

    /**
     *  Ease-out-bounce, inverted so the shadow settles toward transparency.
     *  <b>Falloff:</b> {@code f(t) = 1 - easeOutBounce(t)}.
     *
     *  @return A tuple of shadow falloff fractions sampled at evenly spaced positions.
     */
    public static Tuple<Float> bounce() {
        Tuple<Float> fractions = BOUNCE;
        if ( fractions == null )
            BOUNCE = fractions = _sample(SAMPLES_FINE, t -> 1f - _easeOutBounce(t));
        return fractions;
    }

    private static Tuple<Float> _blur() {
        final double k = 3.6; // blur steepness
        final double half = _erf(k * 0.5);
        return _sample(SAMPLES, t -> (float) ((half - _erf(k * (t - 0.5))) / (2.0 * half)));
    }

    private static Tuple<Float> _glow() {
        final double k = 4.0; // bell width
        final double e = Math.exp(-k);
        return _sample(SAMPLES, t -> (float) ((Math.exp(-k * t * t) - e) / (1.0 - e)));
    }

    private static Tuple<Float> _contact() {
        final double k = 5.0; // decay rate
        final double e = Math.exp(-k);
        return _sample(SAMPLES, t -> (float) ((Math.exp(-k * t) - e) / (1.0 - e)));
    }

    private static Tuple<Float> _stairs() {
        final int n = 5; // number of bands
        return _sample(SAMPLES_FINE, t -> Math.round((1f - t) * (n - 1)) / (float) (n - 1));
    }

    private static Tuple<Float> _ripple() {
        final int k = 3; // number of ripples
        return _sample(SAMPLES_FINE, t -> (1f - t) * (0.5f + 0.5f * (float) Math.cos(2.0 * Math.PI * k * t)));
    }

    private static Tuple<Float> _sawtooth() {
        final int k = 4; // number of louvers
        return _sample(SAMPLES_FINE, t -> {
            final float saw = (float) (k * t - Math.floor(k * t)); // frac(k*t)
            return (1f - t) * (1f - saw);
        });
    }

    /**
     *  Samples the given falloff curve {@code f} at {@code samples + 1} evenly spaced
     *  positions {@code t = i / samples} for {@code i} in {@code [0, samples]} and returns
     *  the resulting fractions as a {@link Tuple}.
     */
    private static Tuple<Float> _sample( final int samples, final _Curve f ) {
        final float[] fractions = new float[samples + 1];
        for ( int i = 0; i <= samples; i++ )
            fractions[i] = f.at((float) i / samples);
        return Tuple.of(fractions);
    }

    /** A scalar falloff curve {@code f(t)}, used internally by {@link #_sample(int, _Curve)}. */
    @FunctionalInterface
    private interface _Curve { float at( float t ); }

    /**
     *  An approximation of the Gauss error function {@code erf(x)} used by the
     *  {@link #blur()} falloff, based on the Abramowitz &amp; Stegun formula 7.1.26
     *  (maximum absolute error around {@code 1.5e-7}), which is plenty accurate for
     *  deriving shadow gradient colors.
     */
    private static double _erf( final double x ) {
        final double sign = x < 0 ? -1.0 : 1.0;
        final double ax   = Math.abs(x);
        final double t    = 1.0 / (1.0 + 0.3275911 * ax);
        final double y    = 1.0 - (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t - 0.284496736) * t + 0.254829592) * t * Math.exp(-ax * ax);
        return sign * y;
    }

    /**
     *  The classic "ease out bounce" easing function, returning a value in {@code [0, 1]}
     *  that rises to {@code 1} at {@code x == 1} with a few diminishing rebounds along the
     *  way. Used (inverted) by the {@link #bounce()} falloff.
     */
    private static float _easeOutBounce( float x ) {
        final float n1 = 7.5625f;
        final float d1 = 2.75f;
        if ( x < 1f / d1 ) {
            return n1 * x * x;
        } else if ( x < 2f / d1 ) {
            x -= 1.5f / d1;
            return n1 * x * x + 0.75f;
        } else if ( x < 2.5f / d1 ) {
            x -= 2.25f / d1;
            return n1 * x * x + 0.9375f;
        } else {
            x -= 2.625f / d1;
            return n1 * x * x + 0.984375f;
        }
    }
}