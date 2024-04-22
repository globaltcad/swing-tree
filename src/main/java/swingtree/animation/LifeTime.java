package swingtree.animation;

import sprouts.Event;
import sprouts.Val;
import swingtree.SwingTree;
import swingtree.SwingTreeConfigurator;
import swingtree.api.AnimatedStyler;

import java.awt.Component;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 *  The lifetime is an immutable and thread safe value based object, which
 *  defines for how long an {@link Animation} should run.
 *  It consists of a delay, interval and duration as well as a unique id
 *  which ensures that two instances of this class are never equal.
 *  <br>
 *  You can create a new lifetime using the static factory methods {@link #of(long, TimeUnit)},
 *  {@link #of(double, TimeUnit)}, {@link #of(long, TimeUnit, long, TimeUnit)} and {@link #of(double, TimeUnit, double, TimeUnit)}.
 *  <br>
 *  Update an existing lifetime using the methods {@link #startingIn(long, TimeUnit)} and {@link #withInterval(long, TimeUnit)}.
 *  Note that the default interval of a newly created lifetime is always 16 ms which corresponds to 60 fps.
 *  <br><br>
 *  This class is typically used to schedule animations.
 *  The most straight forward way would be to call
 *  {@link swingtree.UI#animateFor(LifeTime)} or {@link swingtree.UI#animateFor(LifeTime, Component)}.
 *  But you may also schedule a style animation using {@link swingtree.UIForAnySwing#withTransitoryStyle(Event, LifeTime, AnimatedStyler)}
 *  or {@link swingtree.UIForAnySwing#withTransitionalStyle(Val, LifeTime, AnimatedStyler)}. <br>
 *  Another use case is to schedule an animation through the component event delegate
 *  as part of your event handling code using {@link swingtree.ComponentDelegate#animateFor(LifeTime)}. <br>
 *  This may look like this:
 *  <pre>{@code
 *  UI.button("I pop when you hover over me")
 *  .onMouseEnter( it -> it.animateFor(1, TimeUnit.SECONDS, state -> {
 *    it.style(state, conf -> conf
 *      .borderWidth( 10 * state.cycle() )
 *      .borderColor(UI.color(1,1,0,1-state.cycle()))
 *      .borderRadius( 100 * state.cycle() )
 *    );
 *  }))
 *  }</pre>
 */
public final class LifeTime
{
    private final long _delay; // in milliseconds
    private final long _duration;
    private final long _interval;

    /**
     *  Creates a new lifetime that will run for the given duration
     *  and without any start delay.
     * @param time The duration of the animation.
     * @param unit The time unit of the duration.
     * @return A new lifetime that will start immediately and run for the given duration.
     */
    public static LifeTime of( long time, TimeUnit unit ) {
        Objects.requireNonNull(unit);
        return new LifeTime(0, unit.toMillis(time), SwingTree.get().getDefaultAnimationInterval());
    }

    /**
     *  Creates a new lifetime that will run for the given duration
     *  in the given time unit and without any start delay. <br>
     *  Contrary to the {@link #of(long, TimeUnit)} method, this method
     *  uses a {@code double} type to allow for fractional time values.
     *
     * @param time The duration of the animation.
     * @param unit The time unit of the duration.
     * @return A new lifetime that will start immediately and run for the given duration.
     */
    public static LifeTime of( double time, TimeUnit unit ) {
        long millis = _convertTimeFromDoublePrecisely(time, unit, TimeUnit.MILLISECONDS);
        return new LifeTime(0, millis, SwingTree.get().getDefaultAnimationInterval());
    }

    /**
     *  Creates a new lifetime that will start after the given delay and run for the given duration.
     * @param startDelay The delay after which the animation should start.
     * @param startUnit The time unit of the delay.
     * @param duration The duration of the animation.
     * @param durationUnit The time unit of the duration.
     * @return A new lifetime that will start after the given delay and run for the given duration.
     */
    public static LifeTime of( long startDelay, TimeUnit startUnit, long duration, TimeUnit durationUnit ) {
        return new LifeTime(startUnit.toMillis(startDelay), durationUnit.toMillis(duration), SwingTree.get().getDefaultAnimationInterval());
    }

    /**
     *  Creates a new lifetime that will start after the given delay and run for the given duration.
     *  Contrary to the {@link #of(long, TimeUnit, long, TimeUnit)} method, this method
     *  uses a {@code double} type to allow for fractional time values.
     * @param startDelay The delay after which the animation should start.
     * @param startUnit The time unit of the delay.
     * @param duration The duration of the animation.
     * @param durationUnit The time unit of the duration.
     * @return A new lifetime that will start after the given delay and run for the given duration.
     */
    public static LifeTime of( double startDelay, TimeUnit startUnit, double duration, TimeUnit durationUnit ) {
        long startMillis    = _convertTimeFromDoublePrecisely(startDelay, startUnit, TimeUnit.MILLISECONDS);
        long durationMillis = _convertTimeFromDoublePrecisely(duration, durationUnit, TimeUnit.MILLISECONDS);
        return new LifeTime(startMillis, durationMillis, SwingTree.get().getDefaultAnimationInterval());
    }

    private static long _convertTimeFromDoublePrecisely( double time, TimeUnit from, TimeUnit to ) {
        long millis = (long) (time * from.toMillis(1));
        long remainderMillis = (long) (time * from.toMillis(1) - millis);
        return to.convert(millis + remainderMillis, TimeUnit.MILLISECONDS);
    }


    private LifeTime( long delay, long duration, long interval ) {
        _delay     = delay;
        _duration  = duration;
        _interval = interval;
    }


    /**
     *  Creates a new lifetime that will start after the given delay
     *  in the given time unit.
     * @param delay The delay after which the animation should start.
     * @param unit The time unit of the delay.
     * @return A new lifetime that will start after the given delay.
     */
    public LifeTime startingIn( long delay, TimeUnit unit ) {
        long offset = unit.toMillis( delay );
        return LifeTime.of(
                    offset,    TimeUnit.MILLISECONDS,
                    _duration, TimeUnit.MILLISECONDS
                );
    }

    /**
     *  Updates this lifetime with the given interval, which is a property that
     *  determines the delay between two consecutive animation steps.
     *  You can think of it as the time between the heartbeats of the animation.
     *  The smaller the interval, the higher the refresh rate and
     *  the smoother the animation will look.
     *  However, the smaller the interval, the more CPU time will be used.
     *  The default interval is 16 ms which corresponds to 60 fps.
     *  <br>
     *  If you want a custom interval default, you can configure it
     *  during library initialization through the {@link SwingTree#initialiseUsing(SwingTreeConfigurator)}
     *  method or change it at any other time using the
     *  {@link SwingTree#setDefaultAnimationInterval(long)} method.
     *  
     * @param interval The interval in the given time unit.
     * @param unit The time unit of the interval, typically {@link TimeUnit#MILLISECONDS}.
     * @return A new lifetime that will start after the given delay and run for the given duration.
     */
    public LifeTime withInterval( long interval, TimeUnit unit ) {
        return new LifeTime(_delay, _duration, unit.toMillis(interval));
    }

    /**
     *  Returns the duration of the animation in the given time unit.
     * @param unit The time unit in which the duration should be returned.
     * @return The duration of the animation.
     */
    public long getDurationIn( TimeUnit unit ) {
        Objects.requireNonNull(unit);
        return unit.convert(_duration, TimeUnit.MILLISECONDS);
    }

    /**
     *  Returns the delay after which the animation should start in the given time unit.
     * @param unit The time unit in which the delay should be returned.
     * @return The delay after which the animation should start.
     */
    public long getDelayIn( TimeUnit unit ) {
        Objects.requireNonNull(unit);
        return unit.convert(_delay, TimeUnit.MILLISECONDS);
    }

    /**
     *  Returns the interval in the given time unit,
     *  which is a number that determines the delay between two consecutive animation steps.
     *  You can think of it as the time between the heartbeats of an animation.
     *  The smaller the interval, the higher the refresh rate and
     *  the smoother the animation will look.
     *  However, the smaller the interval, the more CPU time will be used.
     *  The default interval is 16 ms which corresponds to 60 fps.
     *  <br>
     *  If you want a custom interval default, you can configure it
     *  during library initialization through the {@link SwingTree#initialiseUsing(SwingTreeConfigurator)}
     *  method or change it at any other time using the
     *  {@link SwingTree#setDefaultAnimationInterval(long)} method.
     *  
     * @param unit The time unit in which the interval should be returned.
     * @return The interval in the given time unit.
     */
    public long getIntervalIn( TimeUnit unit ) {
        Objects.requireNonNull(unit);
        return unit.convert(_interval, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( !(o instanceof LifeTime) ) return false;
        LifeTime lifeTime = (LifeTime) o;
        return _delay     == lifeTime._delay    &&
               _duration  == lifeTime._duration &&
               _interval  == lifeTime._interval;
    }

    @Override
    public int hashCode() {
        return Objects.hash(_delay, _duration, _interval);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"[" +
                    "delay="    + _delay    + ", "+
                    "duration=" + _duration + ", "+
                    "interval=" + _interval +
                "]";
    }
}
