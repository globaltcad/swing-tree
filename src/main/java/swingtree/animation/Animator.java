package swingtree.animation;

import org.jspecify.annotations.Nullable;
import swingtree.ComponentDelegate;

import java.awt.*;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 *  An API for creating an {@link Animation} and defining how it should be executed.
 *  Instances of this class are intended to be created and used either by the
 *  {@link swingtree.UI} API or the user event delegation API (see {@link ComponentDelegate}). <br>
 *  The UI API can be used like so:
 *  <pre>{@code
 *    UI.animateFor( 100, TimeUnit.MILLISECONDS ) // returns an Animate instance
 *       .until( it -> it.progress() >= 0.75 && someOtherCondition() )
 *       .go( status -> {
 *          // do something
 *          someComponent.setValue( it.progress() );
 *          // ...
 *          someComponent.repaint();
 *       });
 *   }</pre>
 *   The user event delegation API can be used like this:
 *   <pre>{@code
 *       panel()
 *       .onMouseClick( it -> {
 *           it.animateFor( 100, TimeUnit.MILLISECONDS )
 *           .goOnce( status -> {
 *               int width = (int) (100 * state.progress());
 *               it.getComponent().setSize( width, 100 );
 *           });
 *       })
 *   }</pre>
 */
public class Animator
{
    private final LifeTime                _lifeTime;
    private final Stride                  _stride;
    private final @Nullable Component     _component;
    private final @Nullable RunCondition  _condition;


    /**
     * Creates an {@link Animator} instance which allows you to define the stop condition
     * for an animation as well as an {@link Animation} that will be executed
     * when passed to the {@link #go(Animation)} method.
     *
     * @param lifeTime The schedule that defines when the animation should be executed and for how long.
     * @return An {@link Animator} instance that can be used to define how the animation should be executed.
     */
    public static Animator animateFor( LifeTime lifeTime ) {
        return animateFor( lifeTime, Stride.PROGRESSIVE );
    }

    /**
     * Creates an {@link Animator} instance which allows you to define the stop condition
     * for an animation as well as an {@link Animation} that will be executed
     * when passed to the {@link #go(Animation)} method.
     *
     * @param lifeTime The schedule that defines when the animation should be executed and for how long.
     * @param stride   The stride of the animation, i.e. whether it should be executed progressively or regressively.
     * @return An {@link Animator} instance that can be used to define how the animation should be executed.
     */
    public static Animator animateFor( LifeTime lifeTime, Stride stride ) {
        return new Animator( lifeTime, stride, null, null );
    }

    /**
     * Creates an {@link Animator} instance which allows you to define the stop condition
     * for an animation as well as an {@link Animation} that will be executed
     * when passed to the {@link #go(Animation)} method.
     *
     * @param lifeTime  The schedule that defines when the animation should be executed and for how long.
     * @param component The component that should be repainted after each animation step.
     * @return An {@link Animator} instance that can be used to define how the animation should be executed.
     */
    public static Animator animateFor( LifeTime lifeTime, Component component ) {
        return animateFor( lifeTime, Stride.PROGRESSIVE, component );
    }

    /**
     * Creates an {@link Animator} instance which allows you to define the stop condition
     * for an animation as well as an {@link Animation} that will be executed
     * when passed to the {@link #go(Animation)} method.
     *
     * @param lifeTime The schedule that defines when the animation should be executed and for how long.
     * @param stride   The stride of the animation, i.e. whether it should be executed progressively or regressively.
     *                 See {@link Stride} for more information.
     * @param component The component that should be repainted after each animation step.
     * @return An {@link Animator} instance that can be used to define how the animation should be executed.
     */
    public static Animator animateFor( LifeTime lifeTime, Stride stride, Component component ) {
        return new Animator( lifeTime, stride, component, null );
    }


    private Animator(
        LifeTime               lifeTime,
        Stride                 stride,
        @Nullable Component    component,
        @Nullable RunCondition animation
    ) {
        _lifeTime  = Objects.requireNonNull(lifeTime);
        _stride    = Objects.requireNonNull(stride);
        _component = component; // may be null
        _condition = animation; // may be null
    }

    /**
     *  Use this to define a stop condition for the animation.
     *
     * @param shouldStop The stop condition for the animation, i.e. the animation will be executed
     *                   until this condition is true.
     * @return A new {@link Animator} instance that will be executed until the given stop condition is true.
     */
    public Animator until( Predicate<AnimationStatus> shouldStop ) {
        return this.asLongAs( shouldStop.negate() );
    }

    /**
     *  Use this to define a running condition for the animation.
     *
     * @param shouldRun The running condition for the animation, i.e. the animation will be executed
     *                  as long as this condition is true.
     * @return A new {@link Animator} instance that will be executed as long as the given running condition is true.
     */
    public Animator asLongAs( Predicate<AnimationStatus> shouldRun ) {
        return new Animator(_lifeTime, _stride, _component, status -> {
                    if ( shouldRun.test(status) )
                        return _condition == null || _condition.shouldContinue(status);

                    return false;
                });
    }

    /**
     *  Runs the given animation based on the stop condition defined by {@link #until(Predicate)} or {@link #asLongAs(Predicate)}.
     *  If no stop condition was defined, the animation will be executed once.
     *  If you want to run an animation forever, simply pass {@code status -> true} to
     *  the {@link #asLongAs(Predicate)} method, or {@code status -> false} to the {@link #until(Predicate)} method.
     *
     * @param animation The animation that should be executed.
     */
    public void go( Animation animation ) {
        RunCondition shouldRun = Optional.ofNullable(_condition).orElse( status -> status.repeats() == 0 );
        AnimationRunner.add( new ComponentAnimator(
                _component,
                LifeSpan.startingNowWith(Objects.requireNonNull(_lifeTime)),
                _stride,
                shouldRun,
                animation
            ));
    }

    /**
     *  Runs the given animation based on a time offset in the given time unit
     *  and the stop condition defined by {@link #until(Predicate)} or {@link #asLongAs(Predicate)}.
     *  If no stop condition was defined, the animation will be executed once.
     *  If you want to run an animation forever, simply pass {@code status -> true} to
     *  the {@link #asLongAs(Predicate)} method, or {@code status -> false} to the {@link #until(Predicate)} method.
     *  <p>
     *  This method is useful in cases where you want an animation to start in the future,
     *  or somewhere in the middle of their lifespan progress (see {@link AnimationStatus#progress()}).
     *
     * @param offset The offset in the given time unit after which the animation should be executed.
     *               This number may also be negative, in which case the animation will be executed
     *               immediately, and with a {@link AnimationStatus#progress()} value that is
     *               advanced according to the offset.
     *
     * @param unit The time unit in which the offset is specified.
     * @param animation The animation that should be executed.
     */
    public void goWithOffset( long offset, TimeUnit unit, Animation animation ) {
        RunCondition shouldRun = Optional.ofNullable(_condition).orElse( status -> status.repeats() == 0 );
        AnimationRunner.add( new ComponentAnimator(
                _component,
                LifeSpan.startingNowWithOffset(offset, unit, Objects.requireNonNull(_lifeTime)),
                _stride,
                shouldRun,
                animation
            ));
    }

}
