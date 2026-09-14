package examples.mixer;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.With;
import lombok.experimental.Accessors;
import sprouts.Tuple;

import java.util.Locale;

/**
 *  <b>The whole state of the Harbor Room console, as one immutable value.</b>
 *  <p>
 *  {@link MixerView} holds this in a single {@code Var} and zooms lenses into its fields, one per
 *  slider. A slider writes through its lens, the lens produces a new {@code MixerViewModel}
 *  through a Lombok wither, and every other lens fires only if its own slice changed. There is
 *  no listener wiring between controls anywhere: when one slider moves another, as the linked
 *  faders, the loop points or the monitor limit do, that is a rule in a method of this class,
 *  and the other slider simply follows the value.
 *  <p>
 *  Under {@link swingtree.threading.EventProcessor#DECOUPLED}, which the console runs in, this
 *  value belongs to the application thread. The sliders live on the UI thread and hand their
 *  numbers over, and {@link MixerEngine} advances the playhead from the application thread too.
 */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor @EqualsAndHashCode @ToString
public final class MixerViewModel
{
    private static final int MIDI_LOG_LINES = 12;

    private final Transport      transport;
    private final Tuple<Channel> channels;
    private final int            selectedIndex;

    private final double         masterDb;
    private final AutomationMode automation;
    private final float          stereoWidth;
    private final double         limiterCeilingDb;

    private final int            monitorVolume;
    private final int            monitorLimit;

    private final short          pitchBend;
    private final byte           modulation;
    private final boolean        pitchWheelHeld;
    private final WheelStyle     wheelStyle;
    private final Tuple<String>  midiLog;

    private final int            compressorThresholdDb;
    private final double         compressorRatio;

    private final boolean        scalesVisible;
    private final boolean        faderSnap;
    private final LabelLanguage  language;

    private final boolean        engineUnderLoad;
    private final double         dspLoad;
    private final String         status;

    public static MixerViewModel initial() {
        return new MixerViewModel(
                    Transport.of(Song.HARBOR_LIGHTS),
                    Tuple.of(Channel.class,
                        Channel.named("Vocals", 0xE0574B, -14.0, 0.25,  0.10),
                        Channel.named("Guitar", 0xE0A32E, -16.0, 0.50,  0.35),
                        Channel.named("Keys",   0x3D8BFD, -18.0, 0.125, 0.60),
                        Channel.named("Drums",  0x17A67B, -10.0, 1.00,  0.00)
                    ),
                    0,
                    -4.0, AutomationMode.MANUAL, 1.0f, -1.0,
                    60, 85,
                    (short) 0, (byte) 0, false, WheelStyle.WHEELS, Tuple.of(String.class),
                    -18, 4.0,
                    true, false, LabelLanguage.ENGLISH,
                    false, 0.14,
                    "Ready. Press play, then grab any slider while the song runs."
                );
    }

    // ── Channels ────────────────────────────────────────────────────────────

    public Channel channel( int index ) {
        return channels.get(index);
    }

    public Channel selectedChannel() {
        return channels.get(selectedIndex);
    }

    public MixerViewModel withSelectedChannelChanged( Channel changed ) {
        return withChannelAt(selectedIndex, changed);
    }

    /**
     *  Replaces the strip at the given index. When its fader moved and the strip is linked, every
     *  other linked strip's fader moves by the same number of decibels, which is how a fader
     *  group works on a real console. The other faders keep their distance to each other even
     *  when that leaves them between two tick marks of a snapping scale: only what the user does
     *  snaps, and this is the application moving them.
     */
    public MixerViewModel withChannelAt( int index, Channel changed ) {
        Channel before = channels.get(index);
        double delta = changed.faderDb() - before.faderDb();
        Tuple<Channel> updated = channels.setAt(index, changed);
        if ( changed.linked() && delta != 0 ) {
            for ( int other = 0; other < updated.size(); other++ ) {
                Channel strip = updated.get(other);
                if ( other != index && strip.linked() )
                    updated = updated.setAt(other, strip.withFaderDb(clampDb(strip.faderDb() + delta)));
            }
        }
        return withChannels(updated);
    }

    /** Replaces the strip at the given index as it is, without moving the faders of its group. */
    public MixerViewModel withStripAt( int index, Channel strip ) {
        return withChannels(channels.setAt(index, strip));
    }

    public boolean anySoloed() {
        return channels.any(Channel::soloed);
    }

    public boolean isAudible( int index ) {
        Channel strip = channels.get(index);
        return !strip.muted() && (!anySoloed() || strip.soloed());
    }

    public double meterDbOf( int index ) {
        if ( !transport.playing() || !isAudible(index) )
            return Channel.SILENCE_DB;
        return channels.get(index).levelDbAt(transport.position());
    }

    // ── Master bus ──────────────────────────────────────────────────────────

    /**
     *  The recorded fade the master fader plays back in {@link AutomationMode#READ}: up from
     *  silence over the first two bars, a steady -6 dB, and down to silence over the last four.
     */
    public double automationDb() {
        long bar = transport.signature().ticksPerBar();
        long position = transport.position();
        long end = transport.end();
        double fadeIn  = Math.min(1.0, position / (2.0 * bar));
        double fadeOut = Math.min(1.0, Math.max(0.0, (end - position) / (4.0 * bar)));
        double gain = Math.min(fadeIn, fadeOut);
        return gain <= 0 ? Channel.SILENCE_DB : Math.max(Channel.SILENCE_DB, -6.0 + 20.0 * Math.log10(gain));
    }

    public double effectiveMasterDb() {
        return automation == AutomationMode.READ ? automationDb() : masterDb;
    }

    public double masterMeterDb() {
        if ( !transport.playing() || effectiveMasterDb() <= Channel.SILENCE_DB )
            return Channel.SILENCE_DB;
        double power = 0;
        for ( int index = 0; index < channels.size(); index++ )
            if ( isAudible(index) )
                power += Math.pow(10, channels.get(index).levelDbAt(transport.position()) / 10.0);
        if ( power <= 0 )
            return Channel.SILENCE_DB;
        double level = 10.0 * Math.log10(power) + effectiveMasterDb();
        return Math.max(Channel.SILENCE_DB, Math.min(limiterCeilingDb, level));
    }

    // ── Monitoring ──────────────────────────────────────────────────────────

    /** Lowering the hearing protection limit below the headphone volume pulls the volume down with it. */
    public MixerViewModel withMonitorLimitAt( int limit ) {
        return withMonitorLimit(limit).withMonitorVolume(Math.min(monitorVolume, limit));
    }

    public MixerViewModel dimmed() {
        return withMonitorVolume(Math.round(monitorVolume * 0.3f))
                .withStatus("Monitors dimmed to " + Math.round(monitorVolume * 0.3f) + "%.");
    }

    // ── Keyboard controller ─────────────────────────────────────────────────

    /**
     *  A pitch wheel springs back to the middle when you let go of it, so a bend only counts while
     *  the wheel is held. Writes that arrive while it is not held are ignored. That keeps the
     *  spring reliable however the release of the mouse button and the slider's last write are
     *  ordered on the application thread.
     */
    public MixerViewModel withPitchBendFromWheel( short bend ) {
        return pitchWheelHeld ? withPitchBend(bend) : this;
    }

    public MixerViewModel grabbingPitchWheel() {
        return withPitchWheelHeld(true);
    }

    public MixerViewModel releasingPitchWheel() {
        return withPitchWheelHeld(false).withPitchBend((short) 0).loggingMidi("Pitch bend  →  0 (sprang back)");
    }

    public MixerViewModel loggingSendOf( int index ) {
        Channel strip = channels.get(index);
        return loggingMidi(String.format(Locale.ROOT, "CC 91  ch %d  %-6s →  %3d", index + 1, strip.name(), strip.send()));
    }

    public MixerViewModel loggingPitchBend() {
        return loggingMidi(String.format(Locale.ROOT, "Pitch bend  →  %+d", pitchBend));
    }

    public MixerViewModel loggingModulation() {
        return loggingMidi(String.format(Locale.ROOT, "CC 1  mod wheel  →  %3d", modulation));
    }

    /** Appends a message unless it repeats the newest one, which a press or release of a knob that did not move would do. */
    private MixerViewModel loggingMidi( String message ) {
        if ( !midiLog.isEmpty() && midiLog.last().equals(message) )
            return this;
        Tuple<String> log = midiLog.add(message);
        if ( log.size() > MIDI_LOG_LINES )
            log = log.removeFirst();
        return withMidiLog(log);
    }

    // ── Session ─────────────────────────────────────────────────────────────

    public MixerViewModel withSongLoaded( Song song ) {
        return withTransport(transport.withSongLoaded(song)).withStatus("Loaded \"" + song + "\", " + song.bars() + " bars in " + song.signature() + ".");
    }

    public MixerViewModel applying( SessionPreset preset ) {
        return preset.applyTo(this).withStatus("Recalled the \"" + preset + "\" snapshot.");
    }

    /** One step of the audio engine's clock. */
    public MixerViewModel advancedBy( double seconds ) {
        if ( !transport.playing() )
            return this;
        double load = (engineUnderLoad ? 0.78 : 0.14) + 0.06 * Math.sin(transport.position() / 1700.0);
        return withTransport(transport.advancedBy(seconds)).withDspLoad(load);
    }

    public MixerViewModel withEngineLoad( boolean underLoad ) {
        return withEngineUnderLoad(underLoad)
                .withDspLoad(underLoad ? 0.78 : 0.14)
                .withStatus(underLoad
                    ? "Heavy DSP load: the application thread now takes ~30 ms per change. Drag a fader and let go."
                    : "DSP load back to normal.");
    }

    private static double clampDb( double db ) {
        return Math.max(Channel.SILENCE_DB, Math.min(Channel.FADER_MAX_DB, db));
    }
}
