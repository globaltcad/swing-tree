package swingtree;

import swingtree.animation.*;
import swingtree.api.AnimatedStyler;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import java.awt.event.ActionEvent;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class FlipFlopStyler<C extends JComponent>
{

    private final LifeTime          _lifetime;
    private final AnimatedStyler<C> _styler;
    private final WeakReference<C>  _owner;

    private AnimationState _state = null;
    private boolean _isOn = false;
    private boolean _isCurrentlyRunningAnimation = false;
    private DisposableAnimation _animation = null;


    FlipFlopStyler( C owner, LifeTime lifetime, AnimatedStyler<C> styler ) {
        _owner     = new WeakReference<>(Objects.requireNonNull(owner));
        _lifetime = Objects.requireNonNull(lifetime);
        _styler    = Objects.requireNonNull(styler);
    }


    ComponentStyleDelegate<C> style( ComponentStyleDelegate<C> delegate ) {
        AnimationState state = _state;
        if ( state == null )
            state = AnimationState.startOf(
                                LifeSpan.startingNowWith(_lifetime),
                                Stride.PROGRESSIVE,
                                new ActionEvent(this, 0, null)
                            );

        _state = state;
        return _styler.style(state, delegate);
    }

    void set( final boolean isOn ) {
        if ( _isOn == isOn ) return;
        C owner = _owner.get();
        if ( owner == null )
            return;

        LifeTime lifetime = _lifetime;
        long offset = 0;

        if ( _isCurrentlyRunningAnimation ) {
            if ( _animation != null ) {
                _animation.dispose();
                _animation = null;
            }
            /*
                Now this is tricky! We are in the middle of an animation transitioning between
                the on and off states. What we want is to start a new animation from the progress
                of the current animation. So we need to calculate the time offset for the new animation
                based on the progress of the current animation.
            */
            double progress = _state.progress();
            if ( _isOn )
                progress = 1 - progress;
            long animationDuration = lifetime.getDurationIn(TimeUnit.MILLISECONDS);
            offset = -(long) (animationDuration * progress);
        }

        _isCurrentlyRunningAnimation = true;

        _animation = new DisposableAnimation(new Animation() {
            @Override
            public void run( AnimationState state ) {
                _state = state;
                _isOn = isOn;
            }
            @Override
            public void finish( AnimationState state ) {
                _state = state;
                _isOn = isOn;
                _isCurrentlyRunningAnimation = false;
            }
        });
        Animator.animateFor(lifetime, isOn ? Stride.PROGRESSIVE : Stride.REGRESSIVE, owner)
                .goWithOffset(offset, TimeUnit.MILLISECONDS, _animation);
    }


    static class DisposableAnimation implements Animation
    {
        private Animation _animation;

        DisposableAnimation( Animation animation ) {
            _animation = Objects.requireNonNull(animation);
        }

        @Override
        public void run(AnimationState state) {
            if ( _animation != null )
                _animation.run(state);
        }

        @Override
        public void finish(AnimationState state) {
            if ( _animation != null )
                _animation.finish(state);

            dispose();
        }

        public void dispose() {
            _animation = null;
        }
    }

}
