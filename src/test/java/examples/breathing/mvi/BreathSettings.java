package examples.breathing.mvi;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.With;
import lombok.experimental.Accessors;

/**
 *  An immutable bundle of timing settings for a breathing session: how many
 *  seconds each of the four {@link BreathPhase}s should last and how many full
 *  cycles make up a session. <br>
 *  The {@code @With} annotation generates a "wither" for every field, which is
 *  exactly what the MVL property lenses in {@link BreathingView} bind their
 *  sliders to.
 */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor @EqualsAndHashCode @ToString
public final class BreathSettings {

    private final double inhaleSeconds;
    private final double holdInSeconds;
    private final double exhaleSeconds;
    private final double holdOutSeconds;
    private final int    totalCycles;

    /** The classic "box breathing" used by athletes and divers: 4·4·4·4. */
    public static BreathSettings box() {
        return new BreathSettings(4, 4, 4, 4, 6);
    }

    /** Dr. Weil's relaxing "4-7-8" pattern, with no pause after the exhale. */
    public static BreathSettings fourSevenEight() {
        return new BreathSettings(4, 7, 8, 0, 4);
    }

    /** A slow, deep pattern for winding down before sleep. */
    public static BreathSettings deepCalm() {
        return new BreathSettings(6, 2, 7, 2, 8);
    }

    /**
     *  The (clamped) duration of the given phase in seconds. A tiny floor is
     *  applied so that even a "zero second" hold still produces a valid,
     *  if instantaneous, animation life-time.
     */
    public double secondsFor( BreathPhase phase ) {
        double raw;
        switch ( phase ) {
            case INHALE:   raw = inhaleSeconds;  break;
            case HOLD_IN:  raw = holdInSeconds;  break;
            case EXHALE:   raw = exhaleSeconds;  break;
            case HOLD_OUT:
            default:       raw = holdOutSeconds; break;
        }
        return Math.max(0.25, raw);
    }

    /** The nominal length of one full breathing cycle, used for the session log. */
    public double cycleSeconds() {
        return inhaleSeconds + holdInSeconds + exhaleSeconds + holdOutSeconds;
    }
}