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
    private static long _instances = 0;

    private final long _id = _instances++;
    private final long _delay; // in milliseconds
    private final long _duration;

    /**
     *  Creates a new schedule that will start immediately and run for the given duration.
     * @param time The duration of the animation.
     * @param unit The time unit of the duration.
     * @return A new schedule that will start immediately and run for the given duration.
     */
    public static LifeTime of( long time, TimeUnit unit ) {
        Objects.requireNonNull(unit);
        return new LifeTime(0, unit.toMillis(time));
    }

    /**
     *  Creates a new schedule that will start immediately and run for the given duration.
     * @param time The duration of the animation.
     * @param unit The time unit of the duration.
     * @return A new schedule that will start immediately and run for the given duration.
     */
    public static LifeTime of( double time, TimeUnit unit ) {
        long millis = _convertTimeFromDoublePrecisely(time, unit, TimeUnit.MILLISECONDS);
        return new LifeTime(0, millis);
    }

    /**
     *  Creates a new schedule that will start after the given delay and run for the given duration.
     * @param startDelay The delay after which the animation should start.
     * @param startUnit The time unit of the delay.
     * @param duration The duration of the animation.
     * @param durationUnit The time unit of the duration.
     * @return A new schedule that will start after the given delay and run for the given duration.
     */
    public static LifeTime of( long startDelay, TimeUnit startUnit, long duration, TimeUnit durationUnit ) {
        return new LifeTime(startUnit.toMillis(startDelay), durationUnit.toMillis(duration));
    }

    /**
     *  Creates a new schedule that will start after the given delay and run for the given duration.
     * @param startDelay The delay after which the animation should start.
     * @param startUnit The time unit of the delay.
     * @param duration The duration of the animation.
     * @param durationUnit The time unit of the duration.
     * @return A new schedule that will start after the given delay and run for the given duration.
     */
    public static LifeTime of( double startDelay, TimeUnit startUnit, double duration, TimeUnit durationUnit ) {
        long startMillis    = _convertTimeFromDoublePrecisely(startDelay, startUnit, TimeUnit.MILLISECONDS);
        long durationMillis = _convertTimeFromDoublePrecisely(duration, durationUnit, TimeUnit.MILLISECONDS);
        return new LifeTime(startMillis, durationMillis);
    }

    private static long _convertTimeFromDoublePrecisely( double time, TimeUnit from, TimeUnit to ) {
        long millis = (long) (time * from.toMillis(1));
        long remainderMillis = (long) (time * from.toMillis(1) - millis);
        return to.convert(millis + remainderMillis, TimeUnit.MILLISECONDS);
    }


    private LifeTime( long delay, long duration ) {
        _delay     = delay;
        _duration  = duration;
    }

    /**
     *  Creates a new schedule that will start after the given delay and run for the given duration.
     * @param delay The delay after which the animation should start.
     * @param unit The time unit of the delay.
     * @return A new schedule that will start after the given delay.
     */
    public LifeTime startingIn( long delay, TimeUnit unit ) {
        long offset = unit.toMillis( delay );
        return LifeTime.of(
                    offset,    TimeUnit.MILLISECONDS,
                    _duration, TimeUnit.MILLISECONDS
                );
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
