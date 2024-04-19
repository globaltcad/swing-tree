package swingtree.animation;

import org.slf4j.Logger;

import java.awt.event.ActionEvent;
import java.util.concurrent.TimeUnit;

/**
 * The state of an animation at a given point in time describing how far the animation has progressed
 * using a number between 0 and 1 (see {@link #progress()}).
 * Use the numbers exposed by the methods of this interface to define how
 * your animation should progress over time.
 */
public final class AnimationState
{
    private final static Logger log = org.slf4j.LoggerFactory.getLogger(AnimationState.class);

    public static AnimationState of( LifeSpan lifeSpan, Stride stride, ActionEvent event, long now ) {
        return _of(lifeSpan, stride, event, now, false);
    }

    public static AnimationState endOf(LifeSpan lifeSpan, Stride stride, ActionEvent event, long iteration) {
        return _of(lifeSpan, stride, event, lifeSpan.getEndTimeIn(TimeUnit.MILLISECONDS, iteration), true);
    }

    public static AnimationState startOf(LifeSpan lifeSpan, Stride stride, ActionEvent event) {
        return _of(lifeSpan, stride, event, lifeSpan.getStartTimeIn(TimeUnit.MILLISECONDS), false);
    }

    private static AnimationState _of( LifeSpan lifeSpan, Stride stride, ActionEvent event, long now, boolean isEnd ) {
        long duration = lifeSpan.lifeTime().getDurationIn(TimeUnit.MILLISECONDS);
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
                log.warn("Unknown stride: {}", stride);
        }
        return new AnimationState(progress, howManyLoops, lifeSpan, event);
    }


    private final double      progress;
    private final long        howManyLoops;
    private final LifeSpan    lifeSpan;
    private final ActionEvent event;


    private AnimationState( double progress, long howManyLoops, LifeSpan lifeSpan, ActionEvent event ) {
        this.progress     = progress;
        this.howManyLoops = howManyLoops;
        this.lifeSpan     = lifeSpan;
        this.event        = event;
    }

    /**
     * @return The animation progress in terms of a number between 0 and 1,
     *         where 0.5 means the animation is halfway through, and 1 means the animation completed.
     */
    public double progress() { return progress; }

    /**
     *  The animation progress in the form of a value linearly growing from {@code start} to {@code end}
     *  based on the equation {@code start + (end - start) * progress()}.
     *  At the beginning of the animation, the value is {@code start}, at the end of the animation, the value is {@code end}.
     *
     * @param start The start value of the animation.
     * @param end The end value of the animation.
     * @return The animation progress in terms of a number between {@code start} and {@code end}.
     */
    public double progress( double start, double end ) { return start + (end - start) * progress(); }

    /**
     *  The animation progress in the form of a value linearly growing from 1 to 0
     *  based on the equation {@code 1 - progress()}.
     *  At the beginning of the animation, the value is 1, at the end of the animation, the value is 0.
     *
     * @return The animation progress in terms of a number between 0 and 1, where 0.5 means the animation is halfway through.
     */
    public double regress() { return 1 - progress(); }

    /**
     *  The animation regression in the form of a value linearly growing from {@code start} to {@code end}.
     *  This method is equivalent to {@code progress(end, start)}.
     *
     * @param end The end value of the animation.
     * @param start The start value of the animation.
     * @return The animation progress in terms of a number between {@code start} and {@code end}.
     */
    public double regress( double end, double start ) { return start + (end - start) * regress(); }

    /**
     *  A sine wave oscillating between 0 and 1 and back to 0 once per iteration.
     *  At the beginning of the animation, the value is 0, at the end of the animation, the value is 0 again,
     *  and when the animation is halfway through, the value is 1.
     *
     * @return The animation progress in terms of a number between 0 and 1, where 1 means the animation is halfway through.
     */
    public double pulse() { return Math.sin(Math.PI * progress()); }

    /**
     *  A sine wave oscillating between {@code start} and {@code peak} and back to {@code start} once per iteration.
     *  At the beginning of the animation, the value is {@code start}, at the end of the animation,
     *  the value is {@code start} again,
     *  and when the animation is halfway through, the value is {@code peak}.
     *
     *  @param start The start value of the sine wave.
     *  @param peak The peak value of the sine wave.
     *  @return The animation progress in terms of a number between {@code start} and {@code end}, where {@code end} means the animation is halfway through.
     */
    public double pulse( double start, double peak ) { return start + (peak - start) * pulse(); }

    /**
     *   The animation progress in the form of quickly growing sine wave front going from 0 to 1
     *   based on the equation {@code sin(PI * progress() / 2)}.
     *   Just like the value returned by {@link #progress()}
     *   the {@link #jumpIn()} value starts at 0 and ends at 1,
     *   however the crucial difference is that the {@link #jumpIn()} value
     *   grows according to a sine wave, which makes certain animations look more natural. <br>
     *   The returned value will grow quickly at the beginning and slowly at the end, hence the name.
     *
     * @return The animation progress in the form of peaking sine wave growing from 0 to 1.
     */
    public double jumpIn() { return Math.sin(Math.PI * progress() / 2); }

    /**
     *   The animation progress in the form of peaking sine wave growing from {@code start} to {@code end}
     *   based on the equation {@code start + (end - start) * jumpIn()}.
     *   Just like the value returned by {@link #progress()}
     *   the {@link #jumpIn()} value starts at {@code start} and ends at {@code end},
     *   however the crucial difference is that the {@link #jumpIn()} value
     *   grows according to a sine wave, which makes certain animations look more natural. <br>
     *   The returned value will grow quickly at the beginning and slowly at the end, hence the name.
     *
     * @param start The start value of the animation.
     * @param end The end value of the animation.
     * @return The animation progress in the form of peaking sine wave growing from {@code start} to {@code end}.
     */
    public double jumpIn( double start, double end ) { return start + (end - start) * jumpIn(); }

    /**
     *   The animation progress in the form of peaking sine wave growing from 1 to 0
     *   based on the equation {@code sin(PI * (1 - progress()) / 2)}.
     *   Just like the value returned by {@link #progress()}
     *   the {@link #jumpOut()} value starts at 1 and ends at 0,
     *   however the crucial difference is that the {@link #fadeOut()} value
     *   grows according to a sine wave, which makes certain animations look more natural.
     *
     * @return The animation progress in the form of peaking sine wave growing from 1 to 0.
     */
    public double jumpOut() { return Math.sin(Math.PI * (1 - progress()) / 2); }

    /**
     *   The animation progress in the form of a initially quickly changing sine wave
     *   going from {@code start} to {@code end}
     *   based on the equation {@code end + (start - end) * jumpOut()}.
     *   Just like the value returned by {@link #progress(double, double)}
     *   the {@link #jumpOut(double, double)} value starts at {@code start} and ends at {@code end},
     *   however the crucial difference is that the {@link #jumpOut(double, double)} value
     *   changes according to a sine wave, which makes certain animations look more natural.
     *
     * @param end The end value of the animation,
     * @param start The start value of the animation.
     * @return The animation progress in the form of peaking sine wave growing from {@code start} to {@code end}.
     */
    public double jumpOut( double end, double start ) { return end + (start - end) * jumpOut(); }

    /**
     *   The animation progress in the form of peaking sine wave growing from 0 to 1
     *   based on the equation
     *   {@code 0.5 * (1 + Math.sin( Math.PI * (progress() - 0.5) ) )}
     *   Just like the value returned by {@link #progress()}
     *   the {@link #fadeIn()} value starts at 0 and ends at 1,
     *   however the crucial difference is that the {@link #fadeIn()} value
     *   grows according to a sine wave, which makes certain animations look more natural. <br>
     *   The difference between this method and {@link #jumpIn()} is that the returned
     *   value grows slower at the beginning, where {@link #jumpIn()} grows faster initially.
     *
     * @return The animation progress in the form of peaking sine wave growing from 0 to 1.
     */
    public double fadeIn() {
        return 0.5 * (1 + Math.sin( Math.PI * (progress() - 0.5) ));
    }

    /**
     *   The animation progress in the form of peaking sine wave growing from {@code start} to {@code end}
     *   based on the equation {@code start + (end - start) * fadeIn()}.
     *   Just like the value returned by {@link #progress()}
     *   the {@link #fadeIn()} value starts at {@code start} and ends at {@code end},
     *   however the crucial difference is that the {@link #fadeIn()} value
     *   grows according to a sine wave, which makes certain animations look more natural. <br>
     *   The difference between this method and {@link #jumpIn()} is that the returned
     *   value grows slower at the beginning, where {@link #jumpIn()} grows faster initially.
     *
     * @param start The start value of the animation.
     * @param end The end value of the animation.
     * @return The animation progress in the form of a wave growing from {@code start} to {@code end}.
     */
    public double fadeIn( double start, double end ) { return start + (end - start) * fadeIn(); }

    /**
     *   The animation progress in the form of peaking sine wave growing from 1 to 0
     *   based on the equation {@code 1 - fadeIn()}.
     *   Just like the value returned by {@link #progress()}
     *   the {@link #fadeOut()} value starts at 1 and ends at 0,
     *   however the crucial difference is that the {@link #fadeOut()} value
     *   grows according to a sine wave, which makes certain animations look more natural. <br>
     *   The difference between this method and {@link #jumpOut()} is that the returned
     *   value grows slower at the beginning, where {@link #jumpOut()} grows faster initially.
     *
     * @return The animation progress in the form of wave growing from 1 to 0.
     */
    public double fadeOut() { return 1 - fadeIn(); }

    /**
     *   The animation progress in the form of a sine wave going from {@code start} to {@code end}
     *   based on the equation {@code end + (start - end) * fadeOut()}.
     *   Just like the value returned by {@link #progress(double, double)}
     *   the {@link #fadeOut(double, double)} value starts at {@code start} and ends at {@code end},
     *   however the crucial difference is that the {@link #fadeOut(double, double)} value
     *   grows according to a sine wave, which makes certain animations look more natural. <br>
     *   The difference between this method and {@link #jumpOut()} is that the returned
     *   value grows slower at the beginning, whereas {@link #jumpOut()} grows faster initially.
     *
     * @param end The end value of the animation,
     * @param start The start value of the animation,
     * @return The animation progress in the form a sine wave going from {@code start} to {@code end}.
     */
    public double fadeOut( double end, double start ) { return end + (start - end) * fadeOut(); }

    /**
     *  Defines the animation progress in terms of a number oscillating between 0, 1 and 0 once per iteration,
     *  meaning that when the animation starts, the value is 0, when it is halfway through, the value is 1,
     *  and when it is finished, the value is 0 again.
     *  <p>
     *  This is especially useful for animations that are supposed to be repeated
     *  or whose start and end values are the same (e.g. a fade-in and fade-out animation).
     *
     *  @return The animation progress in terms of a number between 0 and 1,
     *          where 1 means the animation is halfway through and 0 means the animation started or finished.
     */
    public double cycle() {
        // progress() is guaranteed to be between 0 and 1, where 1 means the animation completed.
        return 1 - Math.abs(2 * progress() - 1);
        // The result is guaranteed to be between 0 and 1, where 1 means the animation is
        // halfway through and 0 means the animation started or finished.
    }

    /**
     *  Defines the animation progress in terms of a number oscillating between 0, 1 and 0 once per iteration,
     *  meaning that when the animation starts, the value is 0, when it is halfway through, the value is 1,
     *  and when it is finished, the value is 0 again.
     *  <p>
     *  This is especially useful for animations that are supposed to be repeated
     *  or whose start and end values are the same (e.g. a fade-in and fade-out animation).
     *  <p>
     *  This method is similar to {@link #cycle()}, but it allows to offset the animation progress.
     *  This is useful for animations that are supposed to be repeated and whose start and end values are different
     *  (e.g. a fade-in and fade-out animation).
     *
     *  @param offset The offset of the animation progress which may be any number.
     *                The modulo operator is used to offset the animation progress
     *                in a way that it is always between 0 and 1.
     *  @return The animation progress in terms of a number between 0 and 1,
     *          where 1 means the animation is halfway through and 0 means the animation started or finished.
     */
    public double cyclePlus( double offset ) {
        double progress = (progress() + offset) % 1;
        if ( progress < 0 ) progress += 1;
        return 1 - Math.abs(2 * progress - 1);
    }

    /**
     *  Defines the animation progress in terms of a number oscillating between 0, 1 and 0 once per iteration,
     *  meaning that when the animation starts, the value is 0, when it is halfway through, the value is 1,
     *  and when it is finished, the value is 0 again.
     *  <p>
     *  This is especially useful for animations that are supposed to be repeated
     *  or whose start and end values are the same (e.g. a fade-in and fade-out animation).
     *  <p>
     *  This method is similar to the of {@link #cyclePlus(double)} but
     *  with the offset being subtracted instead of added.
     *  The returned values is similar to the one returned by {@link #cycle()},
     *  with the simple difference to offset the animation progress.
     *  This is useful for animations that are supposed to be repeated and whose start and end values are different
     *  (e.g. a fade-in and fade-out animation).
     *
     *  @param offset The offset of the animation progress which may be any number.
     *                The modulo operator is used to offset the animation progress
     *                in a way that it is always between 0 and 1.
     *  @return The animation progress in terms of a number between 0 and 1,
     *          where 1 means the animation is halfway through and 0 means the animation started or finished.
     */
    public double cycleMinus( double offset ) {
        double progress = ( progress() - offset ) % 1;
        if ( progress < 0 ) progress += 1;
        return 1 - Math.abs(2 * progress - 1);
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

        AnimationState that = (AnimationState) o;

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
