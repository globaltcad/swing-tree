package swingtree.animation;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 *  A schedule is used to define when an animation should start and how long it should run.
 */
public class Schedule
{
    private final long _delay; // in milliseconds
    private final long _duration;
    private final long _startTime;

    /**
     *  Creates a new schedule that will start immediately and run for the given duration.
     * @param time The duration of the animation.
     * @param unit The time unit of the duration.
     * @return A new schedule that will start immediately and run for the given duration.
     */
    public static Schedule of( long time, TimeUnit unit ) {
        return new Schedule(0, unit.toMillis(time));
    }

    /**
     *  Creates a new schedule that will start immediately and run for the given duration.
     * @param time The duration of the animation.
     * @param unit The time unit of the duration.
     * @return A new schedule that will start immediately and run for the given duration.
     */
    public static Schedule of( double time, TimeUnit unit ) {
        long millis = _convertTimeFromDoublePrecisely(time, unit, TimeUnit.MILLISECONDS);
        return new Schedule(0, millis);
    }

    /**
     *  Creates a new schedule that will start after the given delay and run for the given duration.
     * @param startDelay The delay after which the animation should start.
     * @param startUnit The time unit of the delay.
     * @param duration The duration of the animation.
     * @param durationUnit The time unit of the duration.
     * @return A new schedule that will start after the given delay and run for the given duration.
     */
    public static Schedule of( long startDelay, TimeUnit startUnit, long duration, TimeUnit durationUnit ) {
        return new Schedule(startUnit.toMillis(startDelay), durationUnit.toMillis(duration));
    }

    /**
     *  Creates a new schedule that will start after the given delay and run for the given duration.
     * @param startDelay The delay after which the animation should start.
     * @param startUnit The time unit of the delay.
     * @param duration The duration of the animation.
     * @param durationUnit The time unit of the duration.
     * @return A new schedule that will start after the given delay and run for the given duration.
     */
    public static Schedule of( double startDelay, TimeUnit startUnit, double duration, TimeUnit durationUnit ) {
        long startMillis    = _convertTimeFromDoublePrecisely(startDelay, startUnit, TimeUnit.MILLISECONDS);
        long durationMillis = _convertTimeFromDoublePrecisely(duration, durationUnit, TimeUnit.MILLISECONDS);
        return new Schedule(startMillis, durationMillis);
    }

    private static long _convertTimeFromDoublePrecisely( double time, TimeUnit from, TimeUnit to ) {
        long millis = (long) (time * from.toMillis(1));
        long remainderMillis = (long) (time * from.toMillis(1) - millis);
        return to.convert(millis + remainderMillis, TimeUnit.MILLISECONDS);
    }


    private Schedule( long delay, long duration ) {
        _delay     = delay;
        _duration  = duration;
        _startTime = System.currentTimeMillis() + _delay;
    }

    /**
     *  Returns the duration of the animation in the given time unit.
     * @return The duration of the animation.
     */
    public long getDurationIn( TimeUnit unit ) {
        Objects.requireNonNull(unit);
        return unit.convert(_duration, TimeUnit.MILLISECONDS);
    }

    /**
     *  Returns the delay after which the animation should start in the given time unit.
     * @return The delay after which the animation should start.
     */
    public long getDelayIn( TimeUnit unit ) {
        Objects.requireNonNull(unit);
        return unit.convert(_delay, TimeUnit.MILLISECONDS);
    }

    /**
     *  Returns the time when the animation should start in the given time unit.
     * @return The time when the animation should start.
     */
    public long getStartTimeIn( TimeUnit unit ) {
        Objects.requireNonNull(unit);
        return unit.convert(_startTime, TimeUnit.MILLISECONDS);
    }

    /**
     *   Returns the end time of the specified iteration in the given time unit.
     *   The end time is the time when the animation is scheduled to be finished.
     *   This is essentially the start time plus the duration of the
     *   animation times the provided iteration number.
     *
     * @param unit The time unit in which the end time should be returned.
     * @param iteration The iteration for which the end time should be determined and returned.
     * @return The end time of the specified iteration in the given time unit.
     */
    public long getIterationEndTimeIn( TimeUnit unit, int iteration ) {
        Objects.requireNonNull(unit);
        return unit.convert(_startTime + _duration * iteration, TimeUnit.MILLISECONDS);
    }

}
