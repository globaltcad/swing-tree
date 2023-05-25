package swingtree.animation;

import swingtree.ComponentDelegate;

import java.awt.*;
import java.util.concurrent.TimeUnit;
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
 *           it.animate( 100, TimeUnit.MILLISECONDS )
 *           .goOnce( state -> {
 *               int width = (int) (100 * state.progress());
 *               it.getComponent().setSize( width, 100 );
 *           });
 *       })
 *   }</pre>
 */
public class Animate
{
    private final Component _component;
    private final Schedule _schedule;
    private final StopCondition _condition;

    /**
     * Creates an {@link Animate} instance which allows you to define the stop condition
     * for an animation as well as an {@link Animation} that will be executed
     * when passed to the {@link #go(Animation)} or {@link #goOnce(Animation)} methods.
     *
     * @param schedule The schedule that defines when the animation should be executed and for how long.
     * @return An {@link Animate} instance that can be used to define how the animation should be executed.
     */
    public static Animate on( Schedule schedule ) {
        return new Animate( null, schedule, state -> true );
    }

    /**
     * Creates an {@link Animate} instance which allows you to define the stop condition
     * for an animation as well as an {@link Animation} that will be executed
     * when passed to the {@link #go(Animation)} or {@link #goOnce(Animation)} methods.
     *
     * @param component The component that should be repainted after each animation step.
     * @param schedule The schedule that defines when the animation should be executed and for how long.
     * @return An {@link Animate} instance that can be used to define how the animation should be executed.
     */
    public static Animate on( Component component, Schedule schedule ) {
        return new Animate( component, schedule, state -> true );
    }

    private Animate( Component component, Schedule schedule, StopCondition animation ) {
        _component = component;
        _schedule = schedule;
        _condition = animation;
    }

    /**
     *  Creates a new {@link Animate} instance that will be executed after the given delay.
     *
     * @param delay The delay after which the animation should be executed.
     * @param unit The time unit of the delay.
     * @return A new {@link Animate} instance that will be executed after the given delay.
     */
    public Animate startingIn( long delay, TimeUnit unit ) {
        long offset = unit.toMillis( delay );
        Schedule schedule = Schedule.of(
                offset,                                         TimeUnit.MILLISECONDS,
                _schedule.getDurationIn(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS
        );

        return new Animate( _component, schedule, _condition );
    }

    /**
     *  Runs the given animation once
     *  based on the stop condition {@code state -> state.currentIteration() == 0}
     *
     * @param animation The animation that should be executed.
     */
    public void goOnce( Animation animation ) {
        this.asLongAs( state -> state.currentIteration() == 0 ).go( animation );
    }

    /**
     *  Runs the given animation twice
     *  based on the stop condition {@code state -> state.currentIteration() < 2}
     *
     * @param animation The animation that should be executed.
     */
    public void goTwice( Animation animation ) {
        this.asLongAs( state -> state.currentIteration() < 2 ).go( animation );
    }

    /**
     *  Use this to define a stop condition for the animation.
     *
     * @param shouldStop The stop condition for the animation, i.e. the animation will be executed
     *                   until this condition is true.
     */
    public Animate until( Predicate<AnimationState> shouldStop ) {
        return this.asLongAs( shouldStop.negate() );
    }

    /**
     *  Use this to define a running condition for the animation.
     *
     * @param shouldRun The running condition for the animation, i.e. the animation will be executed
     *                  as long as this condition is true.
     */
    public Animate asLongAs( Predicate<AnimationState> shouldRun ) {
        return new Animate( _component, _schedule, state -> {
            if ( shouldRun.test(state) )
                return _condition.check(state);
            return false;
        });
    }

    /**
     *  Runs the given animation based on the stop condition defined by {@link #until(Predicate)} or {@link #asLongAs(Predicate)}.
     *  If no stop condition was defined, the animation will be executed forever.
     *
     * @param animation The animation that should be executed.
     */
    public void go( Animation animation ) {
        AnimationRunner.add( new ComponentAnimator( _component, _schedule, _condition, animation ) );
    }

}
