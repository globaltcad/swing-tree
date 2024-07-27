package swingtree;

import org.jspecify.annotations.Nullable;
import swingtree.animation.*;
import swingtree.api.AnimatedStyler;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import java.awt.event.ActionEvent;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 *  Models an animation which can switch between two states.
 *
 * @param <C> The type of the component which this flip-flop animation is applied to.
 * @author Daniel Nepp
 */
class FlipFlopStyler<C extends JComponent>
{
    private final LifeTime          _lifetime;
    private final AnimatedStyler<C> _styler;
    private final WeakReference<C>  _owner;

    private @Nullable AnimationStatus _status = null;
    private boolean _isOn = false;
    private boolean _isCurrentlyRunningAnimation = false;
    private @Nullable DisposableAnimation _animation = null;


    FlipFlopStyler( C owner, LifeTime lifetime, AnimatedStyler<C> styler ) {
        _owner     = new WeakReference<>(Objects.requireNonNull(owner));
        _lifetime  = Objects.requireNonNull(lifetime);
        _styler    = Objects.requireNonNull(styler);
    }


    ComponentStyleDelegate<C> style( ComponentStyleDelegate<C> delegate ) {
        AnimationStatus status = _status;
        if ( status == null )
            status = AnimationStatus.startOf(
                                LifeSpan.startingNowWith(_lifetime),
                                Stride.PROGRESSIVE,
                                new ActionEvent(this, 0, null)
                            );

        _status = status;
        return _styler.style(status, delegate);
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
            double progress = _status == null ? 0 : _status.progress();
            if ( _isOn )
                progress = 1 - progress;
            long animationDuration = lifetime.getDurationIn(TimeUnit.MILLISECONDS);
            offset = -(long) (animationDuration * progress);
        }

        _isCurrentlyRunningAnimation = true;

        _animation = new DisposableAnimation(new Animation() {
            @Override
            public void run( AnimationStatus status ) {
                _status = status;
                _isOn = isOn;
            }
            @Override
            public void finish( AnimationStatus status ) {
                _status = status;
                _isOn = isOn;
                _isCurrentlyRunningAnimation = false;
            }
        });
        Animator.animateFor(lifetime, isOn ? Stride.PROGRESSIVE : Stride.REGRESSIVE, owner)
                .goWithOffset(offset, TimeUnit.MILLISECONDS, _animation);
    }


    static class DisposableAnimation implements Animation
    {
        private @Nullable Animation _animation;

        DisposableAnimation( Animation animation ) {
            _animation = Objects.requireNonNull(animation);
        }

        @Override
        public void run( AnimationStatus status ) {
            if ( _animation != null )
                _animation.run(status);
        }

        @Override
        public void finish( AnimationStatus status ) {
            if ( _animation != null )
                _animation.finish(status);

            dispose();
        }

        public void dispose() {
            _animation = null;
        }
    }

}
