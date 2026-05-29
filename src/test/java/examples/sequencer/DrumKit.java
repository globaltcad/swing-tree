package examples.sequencer;

import examples.sequencer.SequencerViewModel.Voice;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.util.EnumMap;
import java.util.Map;

/**
 *  A tiny, dependency-free percussion synthesizer — the sequencer's
 *  <b>Swing-free side-effect layer</b>. It mirrors the role of {@code TransitClient}
 *  in the trains example: the view model knows nothing about it, and the view
 *  merely fires a voice whenever the playhead lands on an active pad.
 *  <p>
 *  Each voice is synthesized once into a short PCM buffer (no sample files needed)
 *  and held in an open {@link Clip} for instant, low-latency re-triggering. The
 *  whole thing is defensively wrapped: on a headless box or a machine with no
 *  audio mixer it simply reports {@link #isAvailable() unavailable} and the
 *  sequencer keeps working as a purely visual instrument.
 */
public final class DrumKit {

    private static final float SAMPLE_RATE = 44_100f;

    private final Map<Voice, Clip> clips = new EnumMap<>(Voice.class);
    private final boolean available;

    public DrumKit() {
        boolean ok = true;
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            for (Voice voice : Voice.values()) {
                byte[] pcm = toPcm16(render(voice));
                Clip clip = AudioSystem.getClip();
                clip.open(format, pcm, 0, pcm.length);
                clips.put(voice, clip);
            }
        } catch (Throwable failure) {
            ok = false; // no mixer / headless / unsupported — degrade to silence
        }
        this.available = ok;
    }

    public boolean isAvailable() {
        return available;
    }

    /** Plays one hit of the given voice at the given linear gain (0..1). */
    public void trigger(Voice voice, double gain) {
        if (!available)
            return;
        Clip clip = clips.get(voice);
        if (clip == null)
            return;
        try {
            applyGain(clip, gain);
            clip.stop();
            clip.setFramePosition(0);
            clip.start();
        } catch (Throwable ignored) {
            // a single dropped hit must never tear down the loop
        }
    }

    private static void applyGain(Clip clip, double gain) {
        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN))
            return;
        FloatControl control = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        double clamped = Math.max(0.0001, Math.min(1.0, gain));
        float db = (float) (20.0 * Math.log10(clamped));
        control.setValue(Math.max(control.getMinimum(), Math.min(control.getMaximum(), db)));
    }

    // ── Synthesis ──────────────────────────────────────────────────────────────
    //   Each voice is a few hundred milliseconds of hand-rolled PCM. Nothing fancy,
    //   just enough character to tell the drums apart.

    private static float[] render(Voice voice) {
        switch (voice) {
            case KICK:  return kick();
            case SNARE: return snare();
            case HAT:   return hat();
            case CLAP:  return clap();
            default:    return new float[0];
        }
    }

    /** A sine whose pitch and amplitude both swoop downward — a punchy kick. */
    private static float[] kick() {
        float[] out = samples(0.30);
        double phase = 0;
        for (int i = 0; i < out.length; i++) {
            double t    = i / (double) out.length;
            double freq = 120 * Math.exp(-3.2 * t) + 42;       // 120 Hz → ~42 Hz
            double amp  = Math.exp(-4.5 * t);
            phase += 2 * Math.PI * freq / SAMPLE_RATE;
            out[i] = (float) (Math.sin(phase) * amp * 0.95);
        }
        return out;
    }

    /** A short 180 Hz body mixed with a fat noise burst — a snappy snare. */
    private static float[] snare() {
        float[] out = samples(0.20);
        for (int i = 0; i < out.length; i++) {
            double t    = i / (double) out.length;
            double amp  = Math.exp(-9.0 * t);
            double body = Math.sin(2 * Math.PI * 180 * i / SAMPLE_RATE) * 0.4;
            double snap = (Math.random() * 2 - 1) * 0.6;
            out[i] = (float) ((body + snap) * amp * 0.8);
        }
        return out;
    }

    /** A very short, bright noise tick — a closed hi-hat. */
    private static float[] hat() {
        float[] out = samples(0.06);
        double prev = 0;
        for (int i = 0; i < out.length; i++) {
            double t     = i / (double) out.length;
            double amp   = Math.exp(-32.0 * t);
            double white = Math.random() * 2 - 1;
            double hi    = white - prev;                       // crude high-pass for sizzle
            prev = white;
            out[i] = (float) (hi * amp * 0.55);
        }
        return out;
    }

    /** Three quick noise bursts and a tail — a hand clap. */
    private static float[] clap() {
        float[] out = samples(0.22);
        int n = out.length;
        for (int i = 0; i < n; i++) {
            double t     = i / (double) n;
            double burst = Math.exp(-120 * t) + Math.exp(-120 * Math.abs(t - 0.012))
                         + Math.exp(-120 * Math.abs(t - 0.026));
            double tail  = Math.exp(-16 * t) * 0.5;
            double noise = Math.random() * 2 - 1;
            out[i] = (float) (noise * Math.min(1.0, burst + tail) * 0.7);
        }
        return out;
    }

    private static float[] samples(double seconds) {
        return new float[(int) (SAMPLE_RATE * seconds)];
    }

    /** Converts a -1..1 float buffer into little-endian signed 16-bit PCM bytes. */
    private static byte[] toPcm16(float[] data) {
        byte[] pcm = new byte[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            int s = (int) (Math.max(-1f, Math.min(1f, data[i])) * Short.MAX_VALUE);
            pcm[i * 2]     = (byte) (s & 0xff);
            pcm[i * 2 + 1] = (byte) ((s >> 8) & 0xff);
        }
        return pcm;
    }
}