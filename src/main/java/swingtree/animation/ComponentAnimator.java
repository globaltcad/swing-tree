package swingtree.animation;

import org.slf4j.Logger;

import javax.swing.JComponent;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 *  Runs an {@link Animation} on a {@link Component} according to a {@link LifeTime} and a {@link RunCondition}.
 */
class ComponentAnimator
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ComponentAnimator.class);

    private final WeakReference<Component> _compRef;
    private final LifeTime      _lifeTime;
    private final RunCondition _condition;
    private final Animation     _animation;


    ComponentAnimator(
        Component     component, // may be null if the animation is not associated with a specific component
        LifeTime      lifeTime,
        RunCondition condition,
        Animation     animation
    ) {
        _compRef   = component == null ? null : new WeakReference<>(component);
        _lifeTime  = Objects.requireNonNull(lifeTime);
        _condition = Objects.requireNonNull(condition);
        _animation = Objects.requireNonNull(animation);
    }

    public Optional<JComponent> component() {
        if ( _compRef == null ) return Optional.empty();
        Component _component = this._compRef.get();
        return Optional.ofNullable( _component instanceof JComponent ? (JComponent) _component : null );
    }

    private AnimationState _createState( long now, ActionEvent event ) {
        long duration = _lifeTime.getDurationIn(TimeUnit.MILLISECONDS);
        long howLongIsRunning = Math.max(0, now - _lifeTime.getStartTimeIn(TimeUnit.MILLISECONDS));
        long howLongCurrentLoop = howLongIsRunning % duration;
        long howManyLoops       = howLongIsRunning / duration;
        double progress         = howLongCurrentLoop / (double) duration;
        return new AnimationState() {
            @Override public double     progress()  { return progress;     }
            @Override public long        repeats()  { return howManyLoops; }
            @Override public LifeTime    lifetime() { return _lifeTime;    }
            @Override public ActionEvent event()    { return event;        }
        };
    }

    boolean run( long now, ActionEvent event )
    {
        if ( now < _lifeTime.getStartTimeIn(TimeUnit.MILLISECONDS) )
            return true;

        AnimationState state = _createState(now, event);
        boolean shouldContinue = false;

        try {
            shouldContinue = _condition.shouldContinue(state);
        } catch ( Exception e ) {
            log.error("An exception occurred while checking if an animation should continue!", e);
            /*
                 If exceptions happen in user provided animation stop conditions,
                 then we don't want to mess up the rest of the animation logic, so we catch
                 any exceptions right here!

                 Ideally this would be logged in the logging framework of a user of the SwingTree
                 library, but we don't know which logging framework that is, so we just log
                 the error through SLF4J so that developers can see what went wrong through their logging framework...

                 Hi there! If you are reading this, you are probably a developer using the SwingTree
                 library, thank you for using it! Good luck finding out what went wrong! :)
            */
        }

        Component component = _compRef == null ? null : _compRef.get();

        if ( _compRef != null && component == null )
            return false; // There was a component but it has been garbage collected.

        if ( !shouldContinue ) {
            try {
                _animation.finish(state); // An animation may want to do something when it is finished (e.g. reset the component to its original state).
            } catch ( Exception e ) {
                log.error("An exception occurred while executing the finish procedure of an animation!", e);
                /*
                     If exceptions happen in the finishing procedure of animations provided by the user,
                     then we don't want to mess up the execution of the rest of the animations,
                     so we catch any exceptions right here!

                     Ideally this would be logged in the logging framework of a user of the SwingTree
                     library, but we don't know which logging framework that is, so we just log
                     the error to SLF4J so that developers can see what went wrong in their own logs.

                     Hi there! If you are reading this, you are probably a developer using the SwingTree
                     library, thank you for using it! Good luck finding out what went wrong! :)
                */
            }
            return false;
        }

        try {
            _animation.run(state);
        } catch ( Exception e ) {
            log.error("An exception occurred while executing an animation!", e);
            /*
                 If exceptions happen in the animations provided by the user,
                 then we don't want to mess up the execution of the rest of the animations,
                 so we catch any exceptions right here!

                 Ideally this would be logged in the logging framework of a user of the SwingTree
                 library, but we don't know which logging framework that is, so we just log
                 the error to SLF4J so that developers can see what went wrong in their own logs.

                 Hi there! If you are reading this, you are probably a developer using the SwingTree
                 library, thank you for using it! Good luck finding out what went wrong! :)
            */
        }

        if ( component != null ) {
            component.revalidate();
            component.repaint();
        }

        return true;
    }
}
