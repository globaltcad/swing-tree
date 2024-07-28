package swingtree.animation;

import org.slf4j.Logger;
import swingtree.style.ComponentExtension;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 *  This is a singleton class responsible for running {@link ComponentAnimator}
 *  instances (which are wrapper classes for {@link Animation} instances)
 *  in a regular interval based on a Swing {@link Timer}.
 *  The timer is started when the first animation is scheduled and stopped when the last animation is finished.
 */
class AnimationRunner
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AnimationRunner.class);

    private static final Map<Integer,AnimationRunner> _INSTANCES = new HashMap<>();


    public static void add( ComponentAnimator animator ) {
        Objects.requireNonNull(animator);
        int interval = (int) animator.lifeSpan().lifeTime().getIntervalIn(TimeUnit.MILLISECONDS);
        AnimationRunner runner = _INSTANCES.computeIfAbsent(interval, it -> new AnimationRunner(interval));
        runner._add(animator);
    }


    private final Timer _timer;

    private final List<ComponentAnimator> _animators = new ArrayList<>();
    private final List<JComponent> _toBeCleaned = new ArrayList<>();


    private AnimationRunner( int delay ) {
         _timer = new Timer( delay, this::_run );
    }

    private void _run( ActionEvent event ) {

        for ( JComponent component : new ArrayList<>(_toBeCleaned) ) {
            ComponentExtension.from(component).clearAnimations();
            _toBeCleaned.remove(component);
        }

        if ( _animators.isEmpty() && _toBeCleaned.isEmpty() ) {
            _timer.stop();
            // We can remove the instance from the map since it's not needed anymore
            _INSTANCES.remove(_timer.getDelay());
            return;
        }

        for ( ComponentAnimator animator : new ArrayList<>(_animators) )
            animator.component().ifPresent( component -> {
                ComponentExtension.from(component).clearAnimations();
            });

        long now = System.currentTimeMillis();

        for ( ComponentAnimator animator : new ArrayList<>(_animators) )
            if ( !_run(animator, now, event) ) {
                _animators.remove(animator);
                animator.component().ifPresent( _toBeCleaned::add );
            }
    }

    private void _add( ComponentAnimator toBeRun ) {
        Objects.requireNonNull(toBeRun, "Null is not a valid animator!");
        _animators.add(toBeRun);
        if ( !_timer.isRunning() )
            _timer.start();
    }

    boolean _run( ComponentAnimator toBeRun, long now, ActionEvent event )
    {
        if ( now < toBeRun.lifeSpan().getStartTimeIn(TimeUnit.MILLISECONDS) )
            return true;

        AnimationStatus status = AnimationStatus.of(toBeRun.lifeSpan(), toBeRun.stride(), event, now);
        boolean shouldContinue = false;

        try {
            long duration = status.lifeSpan().lifeTime().getDurationIn(TimeUnit.MILLISECONDS);
            shouldContinue = toBeRun.condition().shouldContinue(status) && duration > 0;
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

        Component component = toBeRun.compRef() == null ? null : toBeRun.compRef().get();

        if ( toBeRun.compRef() != null && component == null )
            return false; // There was a component, but it has been garbage collected.

        Runnable requestComponentRepaint = () -> {
                                                if ( component != null ) {
                                                    if ( component.getParent() == null ) {
                                                        ComponentExtension.from((JComponent) component).gatherApplyAndInstallStyle(false);
                                                        // There will be no repaint if the component is not visible.
                                                        // So we have to manually apply the style.
                                                    }
                                                    component.revalidate();
                                                    component.repaint();
                                                }
                                            };

        if ( !shouldContinue ) {
            try {
                status = AnimationStatus.endOf(status.lifeSpan(), toBeRun.stride(), status.event(), toBeRun.currentRepeat());
                toBeRun.animation().run(status); // We run the animation one last time to make sure the component is in its final state.
                toBeRun.animation().finish(status); // This method may or may not be overridden by the user.
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
            toBeRun.setCurrentRepeat(status.repeats());
            toBeRun.animation().run(status);
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
