package swingtree.animation;

import com.google.errorprone.annotations.Immutable;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 *  The lifespan defines when an {@link Animation} starts and for how long it should run.
 *  It consists of a start time and {@link LifeTime}, which defines a delay, interval and duration
 *  as well as a unique id which ensures that two instances of this class are never equal.
 *  <br>
 *  This object is intended to be accessed as part of an {@link AnimationState} within
 *  an {@link Animation} and is typically used to decide how an animation should behave.
 */
@Immutable
public final class LifeSpan
{
    public static LifeSpan startingNowWith( LifeTime lifeTime ) {
        return new LifeSpan(lifeTime, System.currentTimeMillis() + lifeTime.getDelayIn(TimeUnit.MILLISECONDS));
    }

    public static LifeSpan startingNowWithOffset( long offset, TimeUnit unit, LifeTime lifeTime ) {
        long inMillis = unit.toMillis(offset);
        return new LifeSpan(lifeTime, System.currentTimeMillis() + inMillis);
    }

    public static LifeSpan endingNowWith( LifeTime lifeTime ) {
        return new LifeSpan(lifeTime, System.currentTimeMillis() - lifeTime.getDurationIn(TimeUnit.MILLISECONDS));
    }

    private final LifeTime _lifeTime;
    private final long _startTime;


    LifeSpan( LifeTime lifeTime, long startTime ) {
        _lifeTime = Objects.requireNonNull(lifeTime);
        _startTime = startTime;
    }

    public LifeTime lifeTime() {
        return _lifeTime;
    }

    public long startTime() {
        return _startTime;
    }

    /**
     *  Returns the time when the animation should start in the given time unit.
     * @param unit The time unit in which the start time should be returned.
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
    public long getEndTimeIn( TimeUnit unit, long iteration ) {
        Objects.requireNonNull(unit);
        return unit.convert(_startTime + _lifeTime.getDurationIn(TimeUnit.MILLISECONDS) * iteration, TimeUnit.MILLISECONDS);
    }

    /**
     *  Allows you to check whether the animation is expired or not.
     *  An animation is considered expired if the current time is greater than
     *  the start time plus the duration of the animation.
     *
     * @return {@code true} if the animation is expired, {@code false} otherwise.
     */
    public boolean isExpired() {
        return System.currentTimeMillis() >= _startTime + _lifeTime.getDurationIn(TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( !(o instanceof LifeSpan) ) return false;
        LifeSpan lifeSpan = (LifeSpan) o;
        return _startTime == lifeSpan._startTime &&
               Objects.equals(_lifeTime, lifeSpan._lifeTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_lifeTime, _startTime);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" +
                    "startTime=" + _startTime + ", " +
                    "lifeTime="  + _lifeTime  +
                "]";
    }
}
