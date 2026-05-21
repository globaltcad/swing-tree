package examples.breathing.mvi;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.With;
import lombok.experimental.Accessors;
import sprouts.Tuple;
import swingtree.animation.Animatable;
import swingtree.animation.AnimationStatus;
import swingtree.animation.AnimationTransformation;
import swingtree.animation.LifeTime;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 *  <b>The view model of the "Tranquil" breathing companion.</b>
 *  <p>
 *  This is a fully immutable, value-based view model — the heart of the
 *  <i>Model-View-Lenses</i> (MVL) pattern. It holds the <b>entire</b> state of
 *  the application: which {@link BreathPhase} we are in, how far that phase has
 *  progressed, the current visual {@link #orbScale() orb scale}, how many
 *  cycles have been completed, whether the session is {@link #running()}, the
 *  {@link BreathSettings timing settings} and the {@link Tuple} of
 *  {@link CompletedCycle completed cycles}.
 *  <p>
 *  <b>How the animation is modelled.</b> Following the
 *  <i>"Animations and View Models"</i> guide, the animation is expressed as a
 *  pure function rather than imperative timer code. {@link #breathAnimation()}
 *  returns an {@link Animatable} describing how <i>this</i> view model should
 *  be transformed over the {@link LifeTime} of the <i>current</i> breathing
 *  phase. The view simply hands that {@code Animatable} to
 *  {@code UI.animate(..)} and re-arms it whenever the phase changes — so a full
 *  guided session emerges from chaining one tiny pure animation per phase.
 *  <p>
 *  Notice that there is not a single {@code setXyz(..)} anywhere: every state
 *  change — a slider drag, a button press, an animation frame — produces a
 *  brand new {@code BreathingViewModel} through a "wither".
 */
@With @Getter @Accessors(fluent = true) @AllArgsConstructor @EqualsAndHashCode @ToString
public final class BreathingViewModel {

    private final BreathPhase           phase;
    private final double                phaseProgress;   // 0..1 within the current phase
    private final double                orbScale;        // 0..1 visual size of the breathing orb
    private final int                   cyclesCompleted;
    private final boolean               running;
    private final BreathSettings        settings;
    private final Tuple<CompletedCycle> log;

    /** A fresh, idle view model using the default "box breathing" preset. */
    public BreathingViewModel() {
        this(BreathPhase.INHALE, 0, 0, 0, false, BreathSettings.box(), Tuple.of(CompletedCycle.class));
    }

    /**
     *  The pure animation of a <b>single breathing phase</b>. <br>
     *  The returned {@link Animatable} captures the phase that is current
     *  <i>right now</i> and runs for exactly that phase's configured duration:
     *  <ul>
     *      <li>{@link AnimationTransformation#run run(..)} is called on every
     *          frame and is a <b>pure, idempotent</b> projection — it derives
     *          {@link #phaseProgress()} and {@link #orbScale()} from the
     *          animation {@code status} and nothing more;</li>
     *      <li>{@link AnimationTransformation#finish finish(..)} is called
     *          <b>exactly once</b> when the phase ends and is where the
     *          accumulating state change happens: it hands over to
     *          {@link #advancePhase()}, moving the model into the next phase.</li>
     *  </ul>
     *  The view observes that phase change and re-arms this {@code Animatable}
     *  for the new phase — that re-arming chain is what turns four little
     *  per-phase animations into one continuous, looping session.
     */
    public Animatable<BreathingViewModel> breathAnimation() {
        BreathPhase animatedPhase = this.phase;
        double      seconds       = settings.secondsFor(animatedPhase);
        return Animatable.of(LifeTime.of(seconds, TimeUnit.SECONDS), this,
            new AnimationTransformation<BreathingViewModel>() {
                @Override
                public BreathingViewModel run( AnimationStatus status, BreathingViewModel model ) {
                    return model.withPhase(animatedPhase)
                                .withPhaseProgress(status.progress())
                                .withOrbScale(animatedPhase.scaleAt(status));
                }
                @Override
                public BreathingViewModel finish( AnimationStatus status, BreathingViewModel model ) {
                    return model.advancePhase();
                }
            });
    }

    /**
     *  Moves the model from the current phase to the next one. <br>
     *  When a cycle wraps around (back to {@link BreathPhase#INHALE}) a
     *  {@link CompletedCycle} is appended to the {@link #log()} and, once the
     *  configured number of cycles has been reached, the session gently stops.
     */
    BreathingViewModel advancePhase() {
        BreathPhase next = phase.next();
        BreathingViewModel updated = this.withPhase(next)
                                         .withPhaseProgress(0)
                                         .withOrbScale(phase.endScale());
        if ( next == BreathPhase.INHALE ) {
            int completed = cyclesCompleted + 1;
            CompletedCycle entry = new CompletedCycle(UUID.randomUUID(), completed, settings.cycleSeconds());
            updated = updated.withCyclesCompleted(completed)
                             .withLog(log.add(entry));
            if ( completed >= settings.totalCycles() )
                updated = updated.withRunning(false);
        }
        return updated;
    }

    public boolean isSessionComplete() {
        return cyclesCompleted >= settings.totalCycles();
    }

    /** The cycle number the user is currently breathing through (1-based, for display). */
    public int currentCycle() {
        return Math.min(cyclesCompleted + (running ? 1 : 0), settings.totalCycles());
    }

    /** How far the whole session has progressed, as a {@code 0..1} fraction. */
    public double sessionProgress() {
        int total = Math.max(1, settings.totalCycles());
        return Math.min(1.0, cyclesCompleted / (double) total);
    }

    /** Begins (or resumes) the session, restarting first if it had already finished. */
    public BreathingViewModel begin() {
        BreathingViewModel base = isSessionComplete() ? freshSession() : this;
        return base.withRunning(true);
    }

    public BreathingViewModel pause() {
        return this.withRunning(false);
    }

    /** A brand new, idle session that keeps the current {@link #settings()}. */
    public BreathingViewModel freshSession() {
        return new BreathingViewModel(BreathPhase.INHALE, 0, 0, 0, false, settings, Tuple.of(CompletedCycle.class));
    }

    /** Switches to a different timing preset, which also resets the session. */
    public BreathingViewModel applyPreset( BreathSettings preset ) {
        return new BreathingViewModel(BreathPhase.INHALE, 0, 0, 0, false, preset, Tuple.of(CompletedCycle.class));
    }
}