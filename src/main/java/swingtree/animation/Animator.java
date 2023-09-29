package swingtree.animation;

import swingtree.ComponentDelegate;

import java.awt.*;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 *  An API for creating an {@link Animation} and defining how it should be executed.
 *  Instances of this class are intended to be created and used either by the
 *  {@link swingtree.UI} API or the user event delegation API (see {@link ComponentDelegate}). <br>
 *  The UI API can be used like so:
 *  <pre>{@code
 *    UI.schedule( 100, TimeUnit.MILLISECONDS ) // returns an Animate instance
 *       .until( it -> it.progress() >= 0.75 && someOtherCondition() )
 *       .go( state -> {
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
 *           .goOnce( state -> {
 *               int width = (int) (100 * state.progress());
 *               it.getComponent().setSize( width, 100 );
 *           });
 *       })
 *   }</pre>
 */
public class Animator
{
    private final LifeTime      _lifeTime;  // Never null
    private final Component     _component; // may be null
    private final RunCondition _condition; // may be null

    /**
     * Creates an {@link Animator} instance which allows you to define the stop condition
     * for an animation as well as an {@link Animation} that will be executed
     * when passed to the {@link #go(Animation)} method.
     *
     * @param lifeTime The schedule that defines when the animation should be executed and for how long.
     * @return An {@link Animator} instance that can be used to define how the animation should be executed.
     */
    public static Animator animateFor( LifeTime lifeTime ) {
        return new Animator( lifeTime, null, null );
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
        return new Animator( lifeTime, component, null );
    }

    private Animator( LifeTime lifeTime, Component component, RunCondition animation ) {
        _lifeTime  = Objects.requireNonNull(lifeTime);
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
    public Animator until( Predicate<AnimationState> shouldStop ) {
        return this.asLongAs( shouldStop.negate() );
    }

    /**
     *  Use this to define a running condition for the animation.
     *
     * @param shouldRun The running condition for the animation, i.e. the animation will be executed
     *                  as long as this condition is true.
     * @return A new {@link Animator} instance that will be executed as long as the given running condition is true.
     */
    public Animator asLongAs( Predicate<AnimationState> shouldRun ) {
        return new Animator(_lifeTime, _component, state -> {
                    if ( shouldRun.test(state) )
                        return _condition == null || _condition.shouldContinue(state);

                    return false;
                });
    }

    /**
     *  Runs the given animation based on the stop condition defined by {@link #until(Predicate)} or {@link #asLongAs(Predicate)}.
     *  If no stop condition was defined, the animation will be executed once.
     *  If you want to run an animation forever, simply pass {@code state -> true} to
     *  the {@link #asLongAs(Predicate)} method, or {@code state -> false} to the {@link #until(Predicate)} method.
     *
     * @param animation The animation that should be executed.
     */
    public void go( Animation animation ) {
        RunCondition shouldRun = Optional.ofNullable(_condition).orElse( state -> state.repeats() == 0 );
        AnimationRunner.add( new ComponentAnimator( _component, _lifeTime, shouldRun, animation ) );
    }

}
