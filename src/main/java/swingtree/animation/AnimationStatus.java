package swingtree.animation;

import org.slf4j.Logger;
import swingtree.SwingTree;

import java.awt.event.ActionEvent;
import java.util.concurrent.TimeUnit;

/**
 * The state of an animation at a given point in time describing how far the animation has progressed
 * using a number between 0 and 1 (see {@link #progress()}).
 * Use the numbers exposed by the methods of this value based class to define how
 * your animation should progress over time.
 */
public final class AnimationStatus implements Progress
{
    private final static Logger log = org.slf4j.LoggerFactory.getLogger(AnimationStatus.class);

    public static AnimationStatus of( LifeSpan lifeSpan, Stride stride, ActionEvent event, long now ) {
        return _of(lifeSpan, stride, event, now, false);
    }

    public static AnimationStatus endOf( LifeSpan lifeSpan, Stride stride, ActionEvent event, long iteration ) {
        return _of(lifeSpan, stride, event, lifeSpan.getEndTimeIn(TimeUnit.MILLISECONDS, iteration), true);
    }

    public static AnimationStatus startOf( LifeSpan lifeSpan, Stride stride, ActionEvent event ) {
        return _of(lifeSpan, stride, event, lifeSpan.getStartTimeIn(TimeUnit.MILLISECONDS), false);
    }

    private static AnimationStatus _of( LifeSpan lifeSpan, Stride stride, ActionEvent event, long now, boolean isEnd ) {
        long duration = lifeSpan.lifeTime().getDurationIn(TimeUnit.MILLISECONDS);
        long interval = lifeSpan.lifeTime().getIntervalIn(TimeUnit.MILLISECONDS);
        long howLongIsRunning = Math.max(0, now - lifeSpan.getStartTimeIn(TimeUnit.MILLISECONDS));
        long howLongCurrentLoop = duration <= 0 ? 0 : howLongIsRunning % duration;
        if ( isEnd && howLongCurrentLoop == 0 )
            howLongCurrentLoop = duration;
        long howManyLoops      = duration <= 0 ? 0 : howLongIsRunning / duration;
        double progress;
        if ( duration <= 0 ) {
            howManyLoops = ( isEnd ? 1 : 0 );
        }
        switch ( stride ) {
            case PROGRESSIVE:
                if ( duration <= 0 )
                    progress     = ( isEnd ? 1 : 0 );
                else
                    progress = howLongCurrentLoop / (double) duration;
                break;
            case REGRESSIVE:
                if ( duration <= 0 )
                    progress     = ( isEnd ? 0 : 1 );
                else
                    progress = 1 - howLongCurrentLoop / (double) duration;
                break;
            default:
                progress = howLongCurrentLoop / (double) duration;
                log.warn(SwingTree.get().logMarker(), "Unknown stride: {}", stride);
        }
        long steps = duration / interval;
        if ( steps > 0 )
            progress = Math.round( progress * steps ) / (double) steps;
        /*
            In the above line, we round the progress to the nearest step.
            This makes animations more deterministic and cache friendly.
        */
        return new AnimationStatus(progress, howManyLoops, lifeSpan, event);
    }


    private final double      progress;
    private final long        howManyLoops;
    private final LifeSpan    lifeSpan;
    private final ActionEvent event;


    private AnimationStatus(double progress, long howManyLoops, LifeSpan lifeSpan, ActionEvent event ) {
        this.progress     = progress;
        this.howManyLoops = howManyLoops;
        this.lifeSpan     = lifeSpan;
        this.event        = event;
    }

    /**
     *  Exposes the progress of the animation state, which is a number between 0 and 1
     *  that represents how far the animation has progressed between its start and end.
     *  Note that an animation may also regress, in which case the states will
     *  transition from 1 to 0 instead of from 0 to 1.
     *  See {@link Stride} for more information.
     *
     * @return The animation progress in terms of a number between 0 and 1,
     *         where 0.5 means the animation is halfway through, and 1 means the animation completed.
     */
    @Override
    public double progress() {
        return progress;
    }

    /**
     *  Slices the progress value of this animation state into a sub-{@link Progress} of the animation
     *  which starts with a value of {@code 0.0} when the animation reaches the progress value {@code from}
     *  and ends with a value of {@code 1.0} when the animation reaches the progress value {@code to}.
     *  If the {@code from} and {@code to} values are invalid, this method will correct them.
     *
     * @param from The progress value at which the sub-progress should start.
     *             This value must be between 0 and 1, otherwise it will be adjusted
     *             and a warning will be logged.
     * @param to The progress value at which the sub-progress should end.
     *           This value must be between 0 and 1, otherwise it will be adjusted
     *           and a warning will be logged.
     *
     * @return A {@link Progress} object that represents the sub-progress of the animation.
     *         This sub-progress will start with a value of {@code 0.0} when the animation reaches
     *         the progress value {@code from} and will end with a value of {@code 1.0} when the animation
     *         reaches the progress value {@code to}.
     */
    public Progress slice( double from, double to ) {
        if ( from == 0 && to == 1 ) {
            return this;
        }
        if ( from == to ) {
            log.warn(SwingTree.get().logMarker(), "Invalid slice from '"+from+"' to '"+to+"'", new Throwable());
        }
        if ( from < 0 || from > 1 || to < 0 || to > 1 ) {
            log.warn(SwingTree.get().logMarker(), "Invalid slice from '"+from+"' to '"+to+"'", new Throwable());
            from = Math.min(1, Math.max(0, from));
            to   = Math.min(1, Math.max(0, to));
        }
        if ( from > to ) {
            log.warn(SwingTree.get().logMarker(), "Invalid slice from '"+from+"' to '"+to+"'", new Throwable());
            double tmp = from;
            from = to;
            to = tmp;
        }
        double subProgress = Math.max(from, Math.min(to, progress));
        return new Progress() {
            @Override
            public double progress() {
                return subProgress;
            }
        };
    }

    /**
     *  A single iteration of an animation consists of its progress going from 0 to 1
     *  in case of it being progressive, or from 1 to 0 in case of it being regressive (see {@link Stride}).
     *  This method returns the number of times the animation has been repeated.
     *
     * @return The number of times the animation has been repeated.
     *         This number is guaranteed to be 0 at the beginning of the animation,
     *         and for most animations it will be 0 at the end of the animation as well.
     *         An animation may be repeated if it is explicitly scheduled to run for a longer time.
     */
    public long repeats() { return howManyLoops; }

    /**
     *  Exposes the {@link LifeSpan} of the animation, which defines
     *  when the animation starts, for how long it should run, how is should progress and
     *  the refresh rate of the animation.
     *
     * @return The {@link LifeSpan} of the animation, i.e. the time when the animation started and how long it should run.
     */
    public LifeSpan lifeSpan() { return lifeSpan; }

    /**
     *  Exposes the timer event that triggered the animation.
     *  Note that under the hood, all animations with the same refresh rate will be
     *  updated by the same timer and thus share the same event.
     *
     * @return The timer event that triggered the animation.
     */
    public ActionEvent event() { return event; }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"[" +
                "progress="   + progress +
                ", repeats="  + howManyLoops +
                ", lifeSpan=" + lifeSpan +
                ", event="    + event +
                "]";
    }

    @Override
    public boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( o == null || getClass() != o.getClass() ) return false;

        AnimationStatus that = (AnimationStatus) o;

        if ( Double.compare(that.progress, progress) != 0 ) return false;
        if ( howManyLoops != that.howManyLoops ) return false;
        if ( !lifeSpan.equals(that.lifeSpan) ) return false;
        return event.equals(that.event);
    }

    @Override
    public int hashCode() {
        int result;
        result = Double.hashCode(progress);
        result = 31 * result + Long.hashCode(howManyLoops);
        result = 31 * result + lifeSpan.hashCode();
        result = 31 * result + event.hashCode();
        return result;
    }
}
