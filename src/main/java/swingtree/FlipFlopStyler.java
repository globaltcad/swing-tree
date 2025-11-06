package swingtree;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
final class FlipFlopStyler<C extends JComponent>
{
    private static final Logger log = LoggerFactory.getLogger(FlipFlopStyler.class);

    private final LifeTime          _lifetime;
    private final AnimatedStyler<C> _styler;
    private final WeakReference<C>  _owner;

    private @Nullable AnimationStatus _status = null;
    private boolean _isOn;
    private boolean _isCurrentlyRunningAnimation = false;
    private @Nullable DisposableAnimation _animation = null;


    FlipFlopStyler( boolean isOnInitially, C owner, LifeTime lifetime, AnimatedStyler<C> styler ) {
        _isOn      = isOnInitially;
        _owner     = new WeakReference<>(Objects.requireNonNull(owner));
        _lifetime  = Objects.requireNonNull(lifetime);
        _styler    = Objects.requireNonNull(styler);
    }


    ComponentStyleDelegate<C> style( ComponentStyleDelegate<C> delegate ) throws Exception {
        AnimationStatus status = _status;
        if ( status == null )
            status = AnimationStatus.startOf(
                                LifeSpan.startingNowWith(_lifetime),
                                _isOn ? Stride.REGRESSIVE : Stride.PROGRESSIVE,
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
        AnimationDispatcher.animateFor(lifetime, isOn ? Stride.PROGRESSIVE : Stride.REGRESSIVE, owner)
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
            try {
                if ( _animation != null )
                    _animation.run(status);
            } catch ( Exception e ) {
                log.error(SwingTree.get().logMarker(), "Error while running animation.", e);
            }
        }

        @Override
        public void finish( AnimationStatus status ) {
            try {
                if (_animation != null)
                    _animation.finish(status);
            } catch (Exception e) {
                log.error(SwingTree.get().logMarker(), "Error while finishing animation.", e);
            } finally {
                dispose();
            }
        }

        public void dispose() {
            _animation = null;
        }
    }

}
