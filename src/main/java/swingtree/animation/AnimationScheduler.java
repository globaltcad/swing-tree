package swingtree.animation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 *  This class is responsible for scheduling and running animations.
 *  It uses a timer to run the animations at a fixed rate.
 *  The timer is started when the first animation is scheduled and stopped when the last animation is finished.
 */
class AnimationScheduler
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

    private static final AnimationScheduler _INSTANCE = new AnimationScheduler();


    public static void schedule( Animator animator ) { _INSTANCE._schedule(Objects.requireNonNull(animator)); }


    private final Timer _timer = new Timer( TIMER_DELAY, this::_run );


    private final List<Animator> _animators = new ArrayList<>();


    private AnimationScheduler() {}

    private void _run( ActionEvent event ) {
        if ( _animators.isEmpty() ) {
            _timer.stop();
            return;
        }

        for ( Animator animator : new ArrayList<>(_animators) )
            clearRenderers(animator.component());

        long now = System.currentTimeMillis();
        List<Animator> toRemove = new ArrayList<>();
        for ( Animator animator : new ArrayList<>(_animators) )
            if ( !animator.run(now, event) )
                toRemove.add(animator);

        _animators.removeAll(toRemove);
    }

    private void clearRenderers( JComponent comp ) {
        if ( comp == null )
            return;
        List<Consumer<Graphics2D>> renderer = (List<Consumer<Graphics2D>>) comp.getClientProperty(Animate.class);
        if ( renderer != null )
            renderer.clear();
    }

    private void _schedule( Animator animator ) {
        Objects.requireNonNull(animator, "Null is not a valid animator!");
        _animators.add(animator);
        if ( !_timer.isRunning() )
            _timer.start();
    }

}
