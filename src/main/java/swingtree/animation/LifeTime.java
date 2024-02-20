package swingtree.animation;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 *  The lifetime defines for how long an {@link Animation} should run.
 *  It consists of a delay time and duration as well as a unique id
 *  which ensures that two instances of this class are never equal.
 */
public final class LifeTime
{
    private static final long DEFAULT_INTERVAL = 16;
    
    private static long _instances = 0;

    private final long _id = _instances++;
    private final long _delay; // in milliseconds
    private final long _duration;
    private final long _interval;

    /**
     *  Creates a new schedule that will start immediately and run for the given duration.
     * @param time The duration of the animation.
     * @param unit The time unit of the duration.
     * @return A new lifetime that will start immediately and run for the given duration.
     */
    public static LifeTime of( long time, TimeUnit unit ) {
        Objects.requireNonNull(unit);
        return new LifeTime(0, unit.toMillis(time), DEFAULT_INTERVAL);
    }

    /**
     *  Creates a new schedule that will start immediately and run for the given duration.
     * @param time The duration of the animation.
     * @param unit The time unit of the duration.
     * @return A new lifetime that will start immediately and run for the given duration.
     */
    public static LifeTime of( double time, TimeUnit unit ) {
        long millis = _convertTimeFromDoublePrecisely(time, unit, TimeUnit.MILLISECONDS);
        return new LifeTime(0, millis, DEFAULT_INTERVAL);
    }

    /**
     *  Creates a new schedule that will start after the given delay and run for the given duration.
     * @param startDelay The delay after which the animation should start.
     * @param startUnit The time unit of the delay.
     * @param duration The duration of the animation.
     * @param durationUnit The time unit of the duration.
     * @return A new lifetime that will start after the given delay and run for the given duration.
     */
    public static LifeTime of( long startDelay, TimeUnit startUnit, long duration, TimeUnit durationUnit ) {
        return new LifeTime(startUnit.toMillis(startDelay), durationUnit.toMillis(duration), DEFAULT_INTERVAL);
    }

    /**
     *  Creates a new schedule that will start after the given delay and run for the given duration.
     * @param startDelay The delay after which the animation should start.
     * @param startUnit The time unit of the delay.
     * @param duration The duration of the animation.
     * @param durationUnit The time unit of the duration.
     * @return A new lifetime that will start after the given delay and run for the given duration.
     */
    public static LifeTime of( double startDelay, TimeUnit startUnit, double duration, TimeUnit durationUnit ) {
        long startMillis    = _convertTimeFromDoublePrecisely(startDelay, startUnit, TimeUnit.MILLISECONDS);
        long durationMillis = _convertTimeFromDoublePrecisely(duration, durationUnit, TimeUnit.MILLISECONDS);
        return new LifeTime(startMillis, durationMillis, DEFAULT_INTERVAL);
    }

    private static long _convertTimeFromDoublePrecisely( double time, TimeUnit from, TimeUnit to ) {
        long millis = (long) (time * from.toMillis(1));
        long remainderMillis = (long) (time * from.toMillis(1) - millis);
        return to.convert(millis + remainderMillis, TimeUnit.MILLISECONDS);
    }


    private LifeTime(long delay, long duration, long interval) {
        _delay     = delay;
        _duration  = duration;
        _interval = interval;
    }

    /**
     *  Creates a new schedule that will start after the given delay and run for the given duration.
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
     * @param interval The interval in the given time unit.
     * @param unit The time unit of the interval.
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
     *  Returns the interval in the given time unit.
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
        return _id        == lifeTime._id       &&
               _delay     == lifeTime._delay    &&
               _duration  == lifeTime._duration;
    }

    @Override
    public int hashCode() {
        return Objects.hash(_id, _delay, _duration);
    }

}
