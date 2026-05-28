package examples.sequencer;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.With;
import lombok.experimental.Accessors;
import sprouts.HasId;
import sprouts.Tuple;
import sprouts.ValueSet;
import swingtree.animation.Animatable;
import swingtree.animation.AnimationStatus;
import swingtree.animation.AnimationTransformation;
import swingtree.animation.LifeTime;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 *  <b>PULSE — the view model of a neumorphic step sequencer.</b>
 *  <p>
 *  This is a fully immutable, value-based view model — the heart of the
 *  <i>Model-View-Lenses</i> (MVL/MVI) pattern. It holds the <b>entire</b> state
 *  of a little drum machine: the {@link Tuple} of {@link Track}s (each owning a
 *  {@link ValueSet} of the step indices on which it fires), where the playhead
 *  currently is ({@link #currentStep()} and {@link #stepProgress()}), whether the
 *  loop is {@link #playing()}, the tempo ({@link #bpm()}), the {@link #swing()}
 *  amount and the {@link #masterVolume()}. There is not a single {@code setXyz}
 *  anywhere — every change produces a brand new view model through a Lombok
 *  {@code @With} "wither".
 *  <p>
 *  <b>How the playhead is modelled.</b> Just like the "Tranquil" breathing demo,
 *  the moving playhead is expressed as a <i>pure function</i> rather than
 *  imperative timer code. {@link #stepAnimation()} returns an {@link Animatable}
 *  describing how this view model should evolve over the lifetime of the
 *  <i>current</i> sixteenth note: {@link AnimationTransformation#run run(..)}
 *  projects the animation progress into {@link #stepProgress()} every frame, and
 *  {@link AnimationTransformation#finish finish(..)} advances the playhead one
 *  step via {@link #tick()}. The view simply re-arms the next step whenever the
 *  playhead moves — so a continuous, tempo-accurate loop emerges from chaining
 *  one tiny pure animation per step. Because each step recomputes its own
 *  {@link #stepSeconds() duration} from the live {@link #bpm()} and
 *  {@link #swing()}, tempo and groove changes take effect on the very next step.
 */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor @EqualsAndHashCode @ToString
public final class SequencerViewModel {

    /** The number of sixteenth-note steps in one bar. */
    public static final int STEPS = 16;

    /**
     *  One percussion voice of the kit. Each carries a display label and the hue
     *  (in degrees, 0..360) that tints its row and pads in the view.
     */
    public enum Voice {
        KICK ("Kick",   16),
        SNARE("Snare",  45),
        HAT  ("Hats",  186),
        CLAP ("Clap",  286);

        private final String label;
        private final double hue;
        Voice(String label, double hue) { this.label = label; this.hue = hue; }
        public String label() { return label; }
        public double hue()   { return hue;   }
    }

    private final Tuple<Track> tracks;
    private final int          currentStep;   // 0..STEPS-1, where the playhead is
    private final double       stepProgress;  // 0..1 within the current step
    private final boolean      playing;
    private final int          bpm;           // beats per minute (quarter notes)
    private final double       swing;         // 0..1, how much the off-beats are delayed
    private final double       masterVolume;  // 0..1

    /** A fresh sequencer with a classic four-on-the-floor starter groove. */
    public SequencerViewModel() {
        this(defaultTracks(), 0, 0.0, false, 110, 0.18, 0.85);
    }

    // ── Playback ─────────────────────────────────────────────────────────────

    /** Advances the playhead one step, wrapping around the bar (the loop never ends on its own). */
    public SequencerViewModel tick() {
        return withCurrentStep((currentStep + 1) % STEPS).withStepProgress(0);
    }

    /**
     *  The duration of the <i>current</i> step in seconds, derived from the live
     *  tempo and swing. Swing lengthens the on-beat sixteenths and shortens the
     *  off-beat ones, giving the loop a human, shuffled feel.
     */
    public double stepSeconds() {
        double sixteenth = 60.0 / Math.max(1, bpm) / 4.0;
        double shuffle   = (currentStep % 2 == 0) ? (1 + swing * 0.5) : (1 - swing * 0.5);
        return sixteenth * shuffle;
    }

    /**
     *  The pure animation of a <b>single step</b>. The returned {@link Animatable}
     *  runs for exactly this step's {@link #stepSeconds() duration}; every frame
     *  it projects the progress into {@link #stepProgress()} (which drives the
     *  pulsing tempo LED), and when the step ends it hands over to {@link #tick()}
     *  — but only while still {@link #playing()}, so pausing simply lets the
     *  current step run out and stop. The view re-arms this for the next step.
     */
    public Animatable<SequencerViewModel> stepAnimation() {
        return Animatable.of(LifeTime.of(stepSeconds(), TimeUnit.SECONDS), this,
            new AnimationTransformation<SequencerViewModel>() {
                @Override
                public SequencerViewModel run(AnimationStatus status, SequencerViewModel model) {
                    return model.withStepProgress(status.progress());
                }
                @Override
                public SequencerViewModel finish(AnimationStatus status, SequencerViewModel model) {
                    return model.playing() ? model.tick() : model.withStepProgress(0);
                }
            });
    }

    // ── Editing (every method returns a new, immutable view model) ─────────────

    /** Flips the pad of the given track at the given step on or off. */
    public SequencerViewModel toggleStep(UUID trackId, int step) {
        return mapTrack(trackId, t -> t.toggle(step));
    }

    /** Mutes or un-mutes a whole track. */
    public SequencerViewModel toggleMute(UUID trackId) {
        return mapTrack(trackId, Track::toggleMuted);
    }

    /** Wipes every pattern, keeping the tracks, tempo and groove. */
    public SequencerViewModel clearPattern() {
        return withTracks(tracks.map(Track::cleared));
    }

    private SequencerViewModel mapTrack(UUID id, Function<Track, Track> f) {
        for (int i = 0; i < tracks.size(); i++) {
            Track t = tracks.get(i);
            if (t.id().equals(id))
                return withTracks(tracks.setAt(i, f.apply(t)));
        }
        return this;
    }

    private static Tuple<Track> defaultTracks() {
        return Tuple.of(Track.class,
            Track.of(Voice.KICK ).withActiveSteps(stepsOf(0, 4, 8, 10, 12)),
            Track.of(Voice.SNARE).withActiveSteps(stepsOf(4, 12)),
            Track.of(Voice.HAT  ).withActiveSteps(stepsOf(0, 2, 4, 6, 8, 10, 12, 14)),
            Track.of(Voice.CLAP ).withActiveSteps(stepsOf(7, 15))
        );
    }

    private static ValueSet<Integer> stepsOf(int... xs) {
        ValueSet<Integer> set = ValueSet.of(Integer.class);
        for (int x : xs)
            set = set.add(x);
        return set;
    }

    /**
     *  A single percussion lane. It implements {@link HasId} with a stable
     *  {@link UUID} so the view can bind a per-track lens through
     *  {@code scrollPanels().addAll(Var<Tuple<Track>>, entry -> ..)} (value
     *  records define identity by content, so SwingTree needs a stable id to know
     *  which sub-view maps to which track). The set of {@link #activeSteps()}
     *  is a persistent {@link ValueSet} — every toggle returns a new set, and so
     *  a new {@code Track}, and so a new {@code SequencerViewModel}.
     */
    @With @Getter @Accessors(fluent = true) @AllArgsConstructor @EqualsAndHashCode @ToString
    public static final class Track implements HasId<UUID> {

        private final UUID              id;
        private final Voice             voice;
        private final ValueSet<Integer> activeSteps;
        private final boolean           muted;
        private final double            volume;   // 0..1, per-track level

        public static Track of(Voice voice) {
            return new Track(UUID.randomUUID(), voice, ValueSet.of(Integer.class), false, 0.85);
        }

        public boolean isActive(int step) {
            return activeSteps.contains(step);
        }

        public Track toggle(int step) {
            return isActive(step)
                    ? withActiveSteps(activeSteps.remove(step))
                    : withActiveSteps(activeSteps.add(step));
        }

        public Track toggleMuted() {
            return withMuted(!muted);
        }

        public Track cleared() {
            return withActiveSteps(ValueSet.of(Integer.class));
        }

        public String name() { return voice.label(); }
        public double hue()  { return voice.hue();   }
    }
}