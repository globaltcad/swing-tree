package examples.mixer;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.With;
import lombok.experimental.Accessors;

/**
 *  One input strip of the console, from the gain stage at the top to the fader at the bottom.
 *  <p>
 *  Every control lives in the number type its hardware counterpart speaks, which is why the
 *  strip is a tour through all the number types a SwingTree slider supports:
 *  <ul>
 *      <li>{@link #trimDb()} is an {@code int}: preamps step in whole decibels.</li>
 *      <li>{@link #send()} is a {@code byte} from 0 to 127: aux sends are driven by MIDI
 *          controller messages, which carry seven bits.</li>
 *      <li>{@link #pan()} is a {@code float} from -1 (left) to 1 (right).</li>
 *      <li>{@link #faderDb()} and the three EQ gains are {@code double} decibels.</li>
 *  </ul>
 *  The last three fields do not belong to any control. They describe the recorded signal on
 *  this track, so the meters have something plausible to show while the song plays.
 */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor @EqualsAndHashCode @ToString
public final class Channel
{
    public static final double SILENCE_DB = -60.0;
    public static final double FADER_MAX_DB = 6.0;

    private final String  name;
    private final int     colorRgb;

    private final int     trimDb;
    private final byte    send;
    private final float   pan;
    private final double  faderDb;
    private final boolean muted;
    private final boolean soloed;
    private final boolean linked;

    private final double  lowGainDb;
    private final int     midFrequencyHz;
    private final double  midGainDb;
    private final double  highGainDb;

    private final double  sourceLevelDb;
    private final double  pulsesPerBeat;
    private final double  phase;

    public static Channel named( String name, int colorRgb, double sourceLevelDb, double pulsesPerBeat, double phase ) {
        return new Channel(
                    name, colorRgb,
                    0, (byte) 24, 0f, -10.0, false, false, false,
                    0.0, 1000, 0.0, 0.0,
                    sourceLevelDb, pulsesPerBeat, phase
                );
    }

    /**
     *  The level this strip sends to the master bus at the given position of the song, in
     *  decibels below full scale. It is a made-up signal: a pulse on the rhythm of the track,
     *  a slow swell, shaped by the trim, the EQ and the fader.
     */
    public double levelDbAt( long tick ) {
        if ( faderDb <= SILENCE_DB )
            return SILENCE_DB;
        double beats = tick / (double) TimeSignature.TICKS_PER_QUARTER;
        double pulse = Math.pow(Math.abs(Math.cos(Math.PI * (beats * pulsesPerBeat + phase))), 8);
        double swell = 3.0 * Math.sin(beats * 0.21 + phase * 6.0);
        double eq    = (lowGainDb + midGainDb + highGainDb) / 3.0;
        double level = sourceLevelDb - 12.0 + 12.0 * pulse + swell + eq + trimDb + faderDb;
        return Math.max(SILENCE_DB, Math.min(FADER_MAX_DB, level));
    }
}
