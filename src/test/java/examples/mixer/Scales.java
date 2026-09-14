package examples.mixer;

import swingtree.api.IconDeclaration;
import swingtree.api.model.SliderTicks;

import java.util.Locale;
import java.util.function.Function;

/**
 *  <b>Every scale printed on the console, as {@link SliderTicks} values.</b>
 *  <p>
 *  Keeping them in one place makes the variety easy to see. Between them they use every way of
 *  describing tick marks and labels:
 *  <ul>
 *      <li>numbered major tick marks with minor ones between them ({@link #tempo()}),</li>
 *      <li>labels written by a function ({@link #eqGain(boolean)}, {@link #timeline}),</li>
 *      <li>labels at numbers which are not tick marks, and one which renames a tick mark
 *          ({@link #fader(boolean, boolean)}),</li>
 *      <li>words instead of numbers ({@link #pan()}, {@link #stereoWidth()}),</li>
 *      <li>icons instead of words, at a position another slider decides ({@link #monitorVolume(int)}),</li>
 *      <li>tick marks which are not drawn but still snap ({@link #trim()}),</li>
 *      <li>numbers written in the conventions of a locale ({@link #midFrequency(LabelLanguage)},
 *          {@link #limiterCeiling(LabelLanguage)}),</li>
 *      <li>a last tick mark which does not meet the maximum ({@link #send()}, {@link #pitchWheel()}),</li>
 *      <li>and scales which are computed from the view model, so the tick marks change while the
 *          console runs.</li>
 *  </ul>
 *  A scale which never changes is a constant. A scale which depends on the view model is a method,
 *  and the view calls it from a {@code viewAs(..)} mapper on every change of the view model. That
 *  is cheap, because a new {@code SliderTicks} equal to the previous one changes nothing. But
 *  equality compares a label function by identity, so every label function here is a constant too,
 *  and never a lambda written inside a method.
 */
final class Scales
{
    private Scales() {}

    private static final Function<Double, String>  SIGNED_DECIBELS = db -> db > 0 ? String.format(Locale.ROOT, "+%.0f", db) : String.format(Locale.ROOT, "%.0f", db);
    private static final Function<Double, String>  FADER_DECIBELS  = db -> db > 0 ? String.format(Locale.ROOT, "+%.0f", db) : String.format(Locale.ROOT, "%.0f", -db);
    private static final Function<Integer, String> SIGNED_WHOLE    = n -> n > 0 ? "+" + n : String.valueOf(n);
    private static final Function<Integer, String> PERCENT         = n -> n + "%";
    private static final Function<Double, String>  RATIO           = r -> String.format(Locale.ROOT, "%.0f:1", r);

    private static final IconDeclaration SPEAKER_QUIET = IconDeclaration.of("img/mixer/speaker-quiet.svg").withSize(16, 16);
    private static final IconDeclaration SPEAKER_LOUD  = IconDeclaration.of("img/mixer/speaker-loud.svg").withSize(16, 16);

    /**
     *  A channel or master fader from -60 dB, which the console treats as silence, to +6 dB.
     *  Major tick marks every 12 dB are numbered the way fader scales are printed on consoles,
     *  without the minus sign below zero: "0", "12", "24" and so on. The one at -60 is renamed to
     *  "∞", and "+6" is a label beyond the last major tick mark at 0. With snapping on, a minor
     *  tick mark every decibel is what the knob snaps to.
     */
    static SliderTicks<Double> fader( boolean labelled, boolean snapping ) {
        SliderTicks<Double> ticks = SliderTicks.of(Double.class).withMajorSpacing(12.0);
        ticks = snapping ? ticks.withMinorTicksBetween(11).withSnapToTicks(true) : ticks.withMinorTicksBetween(1);
        if ( labelled )
            ticks = ticks.withLabelsAtMajorTicks(FADER_DECIBELS)
                         .withLabelAt(Channel.SILENCE_DB, "∞")
                         .withLabelAt(Channel.FADER_MAX_DB, "+6");
        return ticks;
    }

    /** The input trim, ±20 dB: tick marks every decibel which are not drawn, but snap, and five labels. */
    static SliderTicks<Integer> trim() {
        return TRIM;
    }
    private static final SliderTicks<Integer> TRIM =
            SliderTicks.of(Integer.class)
            .withMajorSpacing(10)
            .withMinorTicksBetween(9)
            .withTickMarksVisible(false)
            .withSnapToTicks(true)
            .withLabelsAtMajorTicks(SIGNED_WHOLE);

    /**
     *  An aux send, a MIDI value from 0 to 127. Tick marks every 8 end at 120, so 121 to 127 lie
     *  beyond the last one, and 127 gets a label of its own.
     */
    static SliderTicks<Byte> send() {
        return SEND;
    }
    private static final SliderTicks<Byte> SEND =
            SliderTicks.of(Byte.class)
            .withMajorSpacing((byte) 32)
            .withMinorTicksBetween(3)
            .withSnapToTicks(true)
            .withLabelsAtMajorTicks()
            .withLabelAt((byte) 127, "127");

    /** Pan from left to right, snapping to tenths, with words at both ends and in the middle. */
    static SliderTicks<Float> pan() {
        return PAN;
    }
    private static final SliderTicks<Float> PAN =
            SliderTicks.of(Float.class)
            .withMajorSpacing(1.0f)
            .withMinorTicksBetween(9)
            .withSnapToTicks(true)
            .withLabelAt(-1.0f, "L")
            .withLabelAt( 0.0f, "C")
            .withLabelAt( 1.0f, "R");

    /** An EQ band's gain, ±12 dB, snapping to whole decibels. */
    static SliderTicks<Double> eqGain( boolean labelled ) {
        return labelled ? EQ_GAIN.withLabelsAtMajorTicks(SIGNED_DECIBELS) : EQ_GAIN;
    }
    private static final SliderTicks<Double> EQ_GAIN =
            SliderTicks.of(Double.class)
            .withMajorSpacing(6.0)
            .withMinorTicksBetween(5)
            .withSnapToTicks(true);

    /**
     *  The centre frequency of the mid band, 200 Hz to 5 kHz, snapping every 200 Hz. The labels
     *  are plain numbers, written in the conventions of the chosen label language.
     */
    static SliderTicks<Integer> midFrequency( LabelLanguage language ) {
        return MID_FREQUENCY.withLabelLocale(language.locale());
    }
    private static final SliderTicks<Integer> MID_FREQUENCY =
            SliderTicks.of(Integer.class)
            .withMajorSpacing(1200)
            .withMinorTicksBetween(5)
            .withSnapToTicks(true)
            .withLabelsAtMajorTicks();

    /** The tempo, 40 to 240 BPM, numbered every 40 and snapping to steps of 5. */
    static SliderTicks<Integer> tempo() {
        return TEMPO;
    }
    private static final SliderTicks<Integer> TEMPO =
            SliderTicks.of(Integer.class)
            .withMajorSpacing(40)
            .withMinorTicksBetween(7)
            .withSnapToTicks(true)
            .withLabelsAtMajorTicks();

    /**
     *  The timeline of the loaded song. A major tick mark every four bars is labelled with its bar
     *  number, a minor tick mark marks every bar, and the end of the song is labelled "End", which
     *  replaces the bar number there when the song is a whole number of four-bar phrases long.
     *  The whole scale depends on the time signature and on the length of the song.
     */
    static SliderTicks<Long> timeline( TimeSignature signature, long end, boolean snapping ) {
        return SliderTicks.of(Long.class)
                .withMajorSpacing(4 * signature.ticksPerBar())
                .withMinorTicksBetween(3)
                .withSnapToTicks(snapping)
                .withLabelsAtMajorTicks(signature.barNumberAtTick())
                .withLabelAt(end, "End");
    }

    /** A loop point: a tick mark on every bar, which it always snaps to, and no labels. */
    static SliderTicks<Long> loopPoint( TimeSignature signature ) {
        return SliderTicks.of(Long.class)
                .withMajorSpacing(signature.ticksPerBar())
                .withSnapToTicks(true);
    }

    /** The stereo width of the master bus: no tick marks drawn, three words, no snapping. */
    static SliderTicks<Float> stereoWidth() {
        return STEREO_WIDTH;
    }
    private static final SliderTicks<Float> STEREO_WIDTH =
            SliderTicks.of(Float.class)
            .withMajorSpacing(0.5f)
            .withTickMarksVisible(false)
            .withLabelAt(0.0f, "Mono")
            .withLabelAt(1.0f, "Stereo")
            .withLabelAt(2.0f, "Wide");

    /** The ceiling of the master limiter, -3.0 to 0.0 dB, snapping to tenths, numbered every one and a half decibels. */
    static SliderTicks<Double> limiterCeiling( LabelLanguage language ) {
        return LIMITER_CEILING.withLabelLocale(language.locale());
    }
    private static final SliderTicks<Double> LIMITER_CEILING =
            SliderTicks.of(Double.class)
            .withMajorSpacing(1.5)
            .withMinorTicksBetween(14)
            .withSnapToTicks(true)
            .withLabelsAtMajorTicks();

    /**
     *  The headphone volume. A quiet speaker sits at 0 and a loud speaker at the hearing protection
     *  limit, which is the maximum of this slider and set by another slider, so the loud speaker
     *  travels along the scale while the user moves the limit.
     */
    static SliderTicks<Integer> monitorVolume( int limit ) {
        return MONITOR_VOLUME.withLabelAt(limit, SPEAKER_LOUD);
    }
    private static final SliderTicks<Integer> MONITOR_VOLUME =
            SliderTicks.of(Integer.class)
            .withMajorSpacing(25)
            .withMinorTicksBetween(4)
            .withLabelAt(0, SPEAKER_QUIET);

    /** The hearing protection limit, 50% to 100%, snapping to steps of 5. */
    static SliderTicks<Integer> monitorLimit() {
        return MONITOR_LIMIT;
    }
    private static final SliderTicks<Integer> MONITOR_LIMIT =
            SliderTicks.of(Integer.class)
            .withMajorSpacing(10)
            .withMinorTicksBetween(1)
            .withSnapToTicks(true)
            .withLabelsAtMajorTicks(PERCENT);

    /**
     *  The pitch wheel, the 14-bit MIDI range from -8192 to 8191. Tick marks every 1024 end at
     *  7168, and the label "+2 st" sits at 8191, which is not a tick mark. No snapping: a wheel
     *  bends smoothly.
     */
    static SliderTicks<Short> pitchWheel() {
        return PITCH_WHEEL;
    }
    private static final SliderTicks<Short> PITCH_WHEEL =
            SliderTicks.of(Short.class)
            .withMajorSpacing((short) 4096)
            .withMinorTicksBetween(3)
            .withLabelAt((short) -8192, "-2 st")
            .withLabelAt((short) 0, "0")
            .withLabelAt((short) 8191, "+2 st");

    /** The modulation wheel, 0 to 127, numbered every 32. */
    static SliderTicks<Byte> modWheel() {
        return MOD_WHEEL;
    }
    private static final SliderTicks<Byte> MOD_WHEEL =
            SliderTicks.of(Byte.class)
            .withMajorSpacing((byte) 32)
            .withMinorTicksBetween(1)
            .withLabelsAtMajorTicks();

    /** The threshold of the vintage compressor, -40 to 0 dB, numbered every 10. */
    static SliderTicks<Integer> compressorThreshold() {
        return COMPRESSOR_THRESHOLD;
    }
    private static final SliderTicks<Integer> COMPRESSOR_THRESHOLD =
            SliderTicks.of(Integer.class)
            .withMajorSpacing(10)
            .withMinorTicksBetween(4)
            .withLabelsAtMajorTicks();

    /** The ratio of the vintage compressor, 1:1 to 10:1, snapping to halves. */
    static SliderTicks<Double> compressorRatio() {
        return COMPRESSOR_RATIO;
    }
    private static final SliderTicks<Double> COMPRESSOR_RATIO =
            SliderTicks.of(Double.class)
            .withMajorSpacing(3.0)
            .withMinorTicksBetween(5)
            .withSnapToTicks(true)
            .withLabelsAtMajorTicks(RATIO);
}
