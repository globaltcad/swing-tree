package swingtree.animation;

import org.slf4j.Logger;

import javax.swing.JComponent;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 *  Runs an {@link Animation} on a {@link Component} according to a {@link LifeTime} and a {@link RunCondition}.
 */
class ComponentAnimator
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ComponentAnimator.class);

    private final WeakReference<Component> _compRef;
    private final LifeSpan      _lifeSpan;
    private final Stride        _stride;
    private final RunCondition _condition;
    private final Animation     _animation;
    private final AtomicLong    _currentRepeat = new AtomicLong(0);


    ComponentAnimator(
        Component     component, // may be null if the animation is not associated with a specific component
        LifeSpan      lifeSpan,
        Stride        stride,
        RunCondition  condition,
        Animation     animation
    ) {
        _compRef   = component == null ? null : new WeakReference<>(component);
        _lifeSpan  = Objects.requireNonNull(lifeSpan);
        _stride    = Objects.requireNonNull(stride);
        _condition = Objects.requireNonNull(condition);
        _animation = Objects.requireNonNull(animation);
    }

    public Optional<JComponent> component() {
        if ( _compRef == null ) return Optional.empty();
        Component _component = this._compRef.get();
        return Optional.ofNullable( _component instanceof JComponent ? (JComponent) _component : null );
    }

    boolean run( long now, ActionEvent event )
    {
        if ( now < _lifeSpan.getStartTimeIn(TimeUnit.MILLISECONDS) )
            return true;

        AnimationState state = AnimationState.of(_lifeSpan, _stride, event, now);
        boolean shouldContinue = false;

        try {
            shouldContinue = _condition.shouldContinue(state);
        } catch ( Exception e ) {
            log.warn("An exception occurred while checking if an animation should continue!", e);
            /*
                 If exceptions happen in user provided animation stop conditions,
                 then we don't want to mess up the rest of the animation logic, so we catch
                 any exceptions right here!

				 We log as warning because exceptions during rendering are not considered
				 as harmful as elsewhere!

                 Hi there! If you are reading this, you are probably a developer using the SwingTree
                 library, thank you for using it! Good luck finding out what went wrong! :)
            */
        }

        Component component = _compRef == null ? null : _compRef.get();

        if ( _compRef != null && component == null )
            return false; // There was a component, but it has been garbage collected.

        Runnable requestComponentRepaint = () -> {
                                                if ( component != null ) {
                                                    component.revalidate();
                                                    component.repaint();
                                                }
                                            };

        if ( !shouldContinue ) {
            try {
                state = AnimationState.endOf(state.lifeSpan(), _stride, state.event(), _currentRepeat.get());
                _animation.run(state); // We run the animation one last time to make sure the component is in its final state.
                _animation.finish(state); // This method may or may not be overridden by the user.
                // An animation may want to do something when it is finished (e.g. reset the component to its original state).
            } catch ( Exception e ) {
                log.warn("An exception occurred while executing the finish procedure of an animation!", e);
                /*
                     If exceptions happen in the finishing procedure of animations provided by the user,
                     then we don't want to mess up the execution of the rest of the animations,
                     so we catch any exceptions right here!

				     We log as warning because exceptions during rendering are not considered
				     as harmful as elsewhere!

                     Hi there! If you are reading this, you are probably a developer using the SwingTree
                     library, thank you for using it! Good luck finding out what went wrong! :)
                */
            }
            requestComponentRepaint.run();
            return false;
        }

        try {
            _currentRepeat.set(state.repeats());
            _animation.run(state);
        } catch ( Exception e ) {
            log.warn("An exception occurred while executing an animation!", e);
            /*
                 If exceptions happen in the animations provided by the user,
                 then we don't want to mess up the execution of the rest of the animations,
                 so we catch any exceptions right here!

				 We log as warning because exceptions during rendering are not considered
				 as harmful as elsewhere!

                 Hi there! If you are reading this, you are probably a developer using the SwingTree
                 library, thank you for using it! Good luck finding out what went wrong! :)
            */
        }

        requestComponentRepaint.run();
        return true;
    }
}
