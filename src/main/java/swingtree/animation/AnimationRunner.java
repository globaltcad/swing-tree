package swingtree.animation;

import swingtree.style.ComponentExtension;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *  This is a singleton class responsible for running {@link ComponentAnimator}
 *  instances (which are wrapper classes for {@link Animation} instances)
 *  in a regular interval based on a Swing {@link Timer}.
 *  The timer is started when the first animation is scheduled and stopped when the last animation is finished.
 */
class AnimationRunner
{
    /*
        We want the refresh rate to be as high as possible so that the animation
        looks smooth, but we don't want to use 100% of the CPU.
        The ideal refresh rate is 60 fps which is 16.6 ms per frame.
        So we set the timer to 16 ms.
        This does of course not account for the time it takes to run the animation
        code, but that should be negligible, and in the worst case
        the animation will be a bit slower than 60 fps.
    */
    private final static int TIMER_DELAY = 16;

    private static final AnimationRunner _INSTANCE = new AnimationRunner();


    public static void add( ComponentAnimator animator ) { _INSTANCE._add(Objects.requireNonNull(animator)); }


    private final Timer _timer = new Timer( TIMER_DELAY, this::_run );


    private final List<ComponentAnimator> _animators = new ArrayList<>();


    private AnimationRunner() {}

    private void _run( ActionEvent event ) {
        if ( _animators.isEmpty() ) {
            _timer.stop();
            return;
        }

        for ( ComponentAnimator animator : new ArrayList<>(_animators) )
            animator.component().ifPresent( component -> {
                ComponentExtension.from(component).clearAnimationRenderer();
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
