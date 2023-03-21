package swingtree.animation;

import java.awt.event.ActionEvent;

/**
 * The state of an animation at a given point in time.
 */
public interface AnimationState
{
    /**
     * @return The animation progress in terms of a number between 0 and 1,
     *         where 1 means the animation completed.
     */
    double progress();

    /**
     *  A sine wave oscillating between 0 and 1 and back to 0 once per iteration.
     *  At the beginning of the animation, the value is 0, at the end of the animation, the value is 0 again,
     *  and when the animation is halfway through, the value is 1.
     *
     * @return The animation progress in terms of a number between 0 and 1, where 1 means the animation is halfway through.
     */
    default double pulse() {
        return Math.sin(Math.PI * progress());
    }

    /**
     *   The animation progress in the form of peaking sine wave growing from 0 to 1
     *   based on the equation {@code sin(PI * progress() / 2)}.
     *   Just like the value returned by {@link #progress()}
     *   the {@link #fadeIn()} value starts at 0 and ends at 1,
     *   however the crucial difference is that the {@link #fadeIn()} value
     *   grows according to a sine wave, which makes certain animations look more natural.
     *
     * @return The animation progress in the form of peaking sine wave growing from 0 to 1.
     */
    default double fadeIn() {
        return Math.sin(Math.PI * progress() / 2);
    }

    /**
     *   The animation progress in the form of peaking sine wave growing from 1 to 0
     *   based on the equation {@code sin(PI * (1 - progress()) / 2)}.
     *   Just like the value returned by {@link #progress()}
     *   the {@link #fadeOut()} value starts at 1 and ends at 0,
     *   however the crucial difference is that the {@link #fadeOut()} value
     *   grows according to a sine wave, which makes certain animations look more natural.
     *
     * @return The animation progress in the form of peaking sine wave growing from 1 to 0.
     */
    default double fadeOut() {
        return Math.sin(Math.PI * (1 - progress()) / 2);
    }

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
     */
    long currentIteration();
    /**
     * @return The schedule of the animation, i.e. the time when the animation started and how long it should run.
     */
    Schedule schedule();
    /**
     * @return The timer event that triggered the animation.
     */
    ActionEvent event();
}
