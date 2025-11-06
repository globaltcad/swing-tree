package swingtree.animation;

import org.slf4j.Logger;
import swingtree.SwingTree;
import swingtree.style.ComponentExtension;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 *  This is a singleton class responsible for running {@link RunningAnimation}
 *  instances (which are wrapper classes for {@link Animation} instances)
 *  in a regular interval based on a Swing {@link Timer}.
 *  The timer is started when the first animation is scheduled and stopped when the last animation is finished.
 */
final class AnimationRunner
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AnimationRunner.class);

    private static final Map<Integer,AnimationRunner> _INSTANCES = new HashMap<>();


    public static void add( RunningAnimation toBeRun ) {
        Objects.requireNonNull(toBeRun);
        int interval = (int) toBeRun.lifeSpan().lifeTime().getIntervalIn(TimeUnit.MILLISECONDS);
        AnimationRunner runner = _INSTANCES.computeIfAbsent(interval, it -> new AnimationRunner(interval));
        runner._add(toBeRun);
    }


    private final Timer _timer;

    private final List<RunningAnimation> _runningAnimations = new ArrayList<>();
    private final List<Runnable>         _toBeFinished = new ArrayList<>();
    private final List<JComponent>       _toBeCleaned = new ArrayList<>();


    private AnimationRunner( int delay ) {
         _timer = new Timer( delay, this::_run );
    }

    private void _run( ActionEvent event ) {

        // We call "Animation.finish(..)" and trigger the last repaint cycle for components with terminated animations:
        for ( Runnable finisher : _toBeFinished )
            try {
                finisher.run();
            } catch ( Exception e ) {
                log.warn( "Error finishing animation!", e );
            }
        _toBeFinished.clear();

        // In a previous run the animation terminated, so we remove animations from the component state:
        for ( JComponent component : _toBeCleaned )
            ComponentExtension.from(component).clearAnimations();
        _toBeCleaned.clear();

        if ( _runningAnimations.isEmpty() ) {
            _timer.stop();
            // We can remove the instance from the map since it's not needed anymore
            _INSTANCES.remove(_timer.getDelay());
            return;
        }

        for ( RunningAnimation running : _runningAnimations )
            running.component().ifPresent( component -> {
                ComponentExtension.from(component).clearAnimations();
            });

        long now = System.currentTimeMillis();

        for ( RunningAnimation running : new ArrayList<>(_runningAnimations) )
            if ( !_runAndCheck(running, now, event) ) {
                _runningAnimations.remove(running);
                running.component().ifPresent( _toBeCleaned::add );
            }
    }

    private void _add( RunningAnimation runningAnimation ) {
        Objects.requireNonNull(runningAnimation, "Null is not a valid animator!");
        _runningAnimations.add(runningAnimation);
        if ( !_timer.isRunning() )
            _timer.start();
    }

    boolean _runAndCheck( RunningAnimation runningAnimation, long now, ActionEvent event )
    {
        if ( now < runningAnimation.lifeSpan().getStartTimeIn(TimeUnit.MILLISECONDS) )
            return true;

        AnimationStatus status = AnimationStatus.of(runningAnimation.lifeSpan(), runningAnimation.stride(), event, now);
        boolean shouldContinue = false;

        try {
            long duration = status.lifeSpan().lifeTime().getDurationIn(TimeUnit.MILLISECONDS);
            shouldContinue = runningAnimation.condition().shouldContinue(status) && duration > 0;
        } catch ( Exception e ) {
            log.warn(SwingTree.get().logMarker(), "An exception occurred while checking if an animation should continue!", e);
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

        Component component = runningAnimation.compRef() == null ? null : runningAnimation.compRef().get();

        if ( runningAnimation.compRef() != null && component == null )
            return false; // There was a component, but it has been garbage collected.

        Runnable requestComponentRepaint = () -> {
                                                if ( component != null ) {
                                                    if ( component.getParent() == null || !_isVisible(component) ) {
                                                        ComponentExtension.from((JComponent) component).gatherApplyAndInstallStyle(false);
                                                        /*
                                                            There will be no repaint if the component is not visible.
                                                            If the paint method encounters a component
                                                            without size or parent, it will return early,
                                                            and SwingTree code will not be reached.
                                                            So we have to regather and apply the style information manually.
                                                         */
                                                    }
                                                    component.revalidate();
                                                    component.repaint();
                                                }
                                            };

        if ( !shouldContinue ) {
            try {
                status = AnimationStatus.endOf(status.lifeSpan(), runningAnimation.stride(), status.event(), runningAnimation.currentRepeat());
                runningAnimation.animation().run(status); // We run the animation one last time to make sure the component is in its final state.
                AnimationStatus finishingStatus = status;
                _toBeFinished.add(()->{
                    try {
                        runningAnimation.animation().finish(finishingStatus); // This method may or may not be overridden by the user.
                        // An animation may want to do something when it is finished (e.g. reset the component to its original state).
                    } catch ( Exception e ) {
                        log.error(SwingTree.get().logMarker(), "An exception occurred while finishing an animation!", e);
                        /*
                             If exceptions happen in the finishing procedure of animations provided by the user,
                             then we don't want to mess up the execution of the rest of the animations,
                             so we catch any exceptions right here!

                             We log as warning because exceptions during rendering are not considered
                             as harmful as elsewhere!
                        */
                    }
                    requestComponentRepaint.run();
                });
            } catch ( Exception e ) {
                log.warn(SwingTree.get().logMarker(), "An exception occurred while executing the finish procedure of an animation!", e);
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
            runningAnimation.setCurrentRepeat(status.repeats());
            runningAnimation.animation().run(status);
        } catch ( Exception e ) {
            log.warn(SwingTree.get().logMarker(), "An exception occurred while executing an animation!", e);
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

    /**
     *  Determines the actual visibility of a component.
     *  Merely checking {@link Component#isVisible()} is not enough, because
     *  a component may be invisible if one of its parents is invisible
     *  or if it has no size (width or height are 0).<br>
     *  If a component is not visible, its paint method will not be called,
     *  and so we have to manually gather and apply the style information.
     *
     * @param component The component to check for visibility to the user on the screen.
     * @return True if the component is visible, false otherwise.
     */
    private static boolean _isVisible( Component component ) {
        boolean hasSize = component.getWidth() > 0 && component.getHeight() > 0;
        if ( !hasSize )
            return false;

        boolean isVisible = true;
        for ( Component current = component; current != null; current = current.getParent() ) {
            if ( !current.isVisible() ) {
                isVisible = false;
                break;
            }
        }
        return isVisible;
    }

}
