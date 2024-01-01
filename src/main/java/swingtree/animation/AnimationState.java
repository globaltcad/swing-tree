package swingtree.animation;

import java.awt.event.ActionEvent;

/**
 * The state of an animation at a given point in time describing how far the animation has progressed
 * using a number between 0 and 1 (see {@link #progress()}).
 * Use the numbers exposed by the methods of this interface to define how
 * your animation should progress over time.
 */
public interface AnimationState
{
    /**
     * @return The animation progress in terms of a number between 0 and 1,
     *         where 0.5 means the animation is halfway through, and 1 means the animation completed.
     */
    double progress();

    /**
     *  The animation progress in the form of a value linearly growing from {@code start} to {@code end}
     *  based on the equation {@code start + (end - start) * progress()}.
     *  At the beginning of the animation, the value is {@code start}, at the end of the animation, the value is {@code end}.
     *
     * @param start The start value of the animation.
     * @param end The end value of the animation.
     * @return The animation progress in terms of a number between {@code start} and {@code end}.
     */
    default double progress( double start, double end ) { return start + (end - start) * progress(); }

    /**
     *  The animation progress in the form of a value linearly growing from 1 to 0
     *  based on the equation {@code 1 - progress()}.
     *  At the beginning of the animation, the value is 1, at the end of the animation, the value is 0.
     *
     * @return The animation progress in terms of a number between 0 and 1, where 0.5 means the animation is halfway through.
     */
    default double regress() { return 1 - progress(); }

    /**
     *  The animation regression in the form of a value linearly growing from {@code start} to {@code end}.
     *  This method is equivalent to {@code progress(end, start)}.
     *
     * @param end The end value of the animation.
     * @param start The start value of the animation.
     * @return The animation progress in terms of a number between {@code start} and {@code end}.
     */
    default double regress( double end, double start ) { return start + (end - start) * regress(); }

    /**
     *  A sine wave oscillating between 0 and 1 and back to 0 once per iteration.
     *  At the beginning of the animation, the value is 0, at the end of the animation, the value is 0 again,
     *  and when the animation is halfway through, the value is 1.
     *
     * @return The animation progress in terms of a number between 0 and 1, where 1 means the animation is halfway through.
     */
    default double pulse() { return Math.sin(Math.PI * progress()); }

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
    default double pulse( double start, double peak ) { return start + (peak - start) * pulse(); }

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
    default double jumpIn() { return Math.sin(Math.PI * progress() / 2); }

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
    default double jumpIn( double start, double end ) { return start + (end - start) * jumpIn(); }

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
    default double jumpOut() { return Math.sin(Math.PI * (1 - progress()) / 2); }

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
    default double jumpOut( double end, double start ) { return end + (start - end) * jumpOut(); }

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
    default double fadeIn() {
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
    default double fadeIn( double start, double end ) { return start + (end - start) * fadeIn(); }

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
    default double fadeOut() { return 1 - fadeIn(); }

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
    default double fadeOut( double end, double start ) { return end + (start - end) * fadeOut(); }

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
    default double cycle() {
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
    default double cyclePlus( double offset ) {
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
    default double cycleMinus( double offset ) {
        double progress = ( progress() - offset ) % 1;
        if ( progress < 0 ) progress += 1;
        return 1 - Math.abs(2 * progress - 1);
    }

    /**
     * @return The number of times the animation has been repeated.
     *         This number is guaranteed to be 0 at the beginning of the animation,
     *         and for most animations it will be 0 at the end of the animation as well.
     *         An animation may be repeated if it is explicitly scheduled to run for a longer time.
     */
    long repeats();
    /**
     * @return The {@link LifeSpan} of the animation, i.e. the time when the animation started and how long it should run.
     */
    LifeSpan lifeSpan();
    /**
     * @return The timer event that triggered the animation.
     */
    ActionEvent event();
}
