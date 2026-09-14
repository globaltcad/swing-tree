package examples.mixer;

import sprouts.From;
import sprouts.Var;
import sprouts.Viewable;
import swingtree.threading.EventProcessor;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *  The pretend audio engine: a clock which moves the playhead, and a switch which makes the
 *  application thread slow.
 *  <p>
 *  <b>The clock</b> runs on a timer thread of its own but never touches the view model there,
 *  because under {@link EventProcessor#DECOUPLED} the view model belongs to the application
 *  thread. Every 20 milliseconds it asks the application thread to advance the playhead by the
 *  time that really passed. It asks for the next step only once the previous one has run, so a
 *  busy application thread is never buried under a pile of clock steps.
 *  <p>
 *  <b>The load switch</b> makes every change of the view model cost the application thread
 *  about 30 milliseconds, the way recomputing filter coefficients on a weak machine might. With
 *  it on, the application thread falls behind the sliders: while the user drags a fader, the
 *  numbers the fader wrote come back to it late, and the knob must neither jump back to them
 *  nor forget where the user let go.
 */
final class MixerEngine
{
    private static final long STEP_MILLIS = 20;
    private static final long LOAD_MILLIS = 30;

    private final Var<MixerViewModel>      vm;
    private final EventProcessor           applicationThread;
    private final ScheduledExecutorService clock;
    private final Viewable<MixerViewModel> observedViewModel;
    private long lastStepNanos;

    MixerEngine( Var<MixerViewModel> vm, EventProcessor applicationThread ) {
        this.vm                = vm;
        this.applicationThread = applicationThread;
        this.clock             = Executors.newSingleThreadScheduledExecutor(runnable -> {
                                    Thread thread = new Thread(runnable, "Harbor Room engine clock");
                                    thread.setDaemon(true);
                                    return thread;
                                });
        this.observedViewModel = Viewable.cast(vm);
        this.observedViewModel.onChange(From.ALL, it -> {
            MixerViewModel current = it.currentValue().orElseNull();
            if ( current != null && current.engineUnderLoad() )
                simulateHeavyProcessing();
        });
    }

    void start() {
        lastStepNanos = System.nanoTime();
        clock.schedule(this::requestStep, STEP_MILLIS, TimeUnit.MILLISECONDS);
    }

    private void requestStep() {
        applicationThread.registerAppEvent(this::step);
    }

    private void step() {
        long now = System.nanoTime();
        double seconds = Math.min(0.25, (now - lastStepNanos) / 1e9);
        lastStepNanos = now;
        try {
            vm.update(m -> m.advancedBy(seconds));
        } finally {
            clock.schedule(this::requestStep, STEP_MILLIS, TimeUnit.MILLISECONDS);
        }
    }

    private static void simulateHeavyProcessing() {
        try {
            Thread.sleep(LOAD_MILLIS);
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
        }
    }
}
