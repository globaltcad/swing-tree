package swingtree.animation;

import swingtree.style.ComponentExtension;

import javax.swing.*;
import javax.swing.Timer;
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
    private static final Map<Integer,AnimationRunner> _INSTANCES = new HashMap<>();


    public static void add( ComponentAnimator animator ) {
        Objects.requireNonNull(animator);
        int interval = (int) animator.lifeSpan().lifeTime().getIntervalIn(TimeUnit.MILLISECONDS);
        AnimationRunner runner = _INSTANCES.computeIfAbsent(interval, it -> new AnimationRunner(interval));
        runner._add(animator);
    }


    private final Timer _timer;


    private final List<ComponentAnimator> _animators = new ArrayList<>();


    private AnimationRunner( int delay ) {
         _timer = new Timer( delay, this::_run );
    }

    private void _run( ActionEvent event ) {
        if ( _animators.isEmpty() ) {
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
        List<ComponentAnimator> toRemove = new ArrayList<>();
        for ( ComponentAnimator animator : new ArrayList<>(_animators) )
            if ( !animator.run(now, event) )
                toRemove.add(animator);

        _animators.removeAll(toRemove);
    }

    private void _add( ComponentAnimator animator ) {
        Objects.requireNonNull(animator, "Null is not a valid animator!");
        _animators.add(animator);
        if ( !_timer.isRunning() )
            _timer.start();
    }

}
