package swingtree;

import swingtree.animation.*;
import swingtree.api.AnimatedStyler;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import java.awt.event.ActionEvent;
import java.lang.ref.WeakReference;
import java.util.Objects;

public class FlipFlopStyler<C extends JComponent>
{
    private final LifeTime          _lifetime;
    private final AnimatedStyler<C> _styler;
    private final WeakReference<C>  _owner;

    private AnimationState _state = null;
    private boolean _isOn = false;


    FlipFlopStyler( C owner, LifeTime lifetime, AnimatedStyler<C> styler ) {
        _owner     = new WeakReference<>(Objects.requireNonNull(owner));
        _lifetime = Objects.requireNonNull(lifetime);
        _styler    = Objects.requireNonNull(styler);
    }


    ComponentStyleDelegate<C> style(ComponentStyleDelegate<C> delegate ) {
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

    void set( boolean isOn ) {
        if ( _isOn == isOn ) return;
        C owner = _owner.get();
        if ( owner == null )
            return;
        if ( isOn ) {
            Animator.animateFor(_lifetime, Stride.PROGRESSIVE, owner)
                    .go(new Animation() {
                        @Override
                        public void run( AnimationState state ) {
                            _state = state;
                            _isOn = true;
                        }
                        @Override
                        public void finish( AnimationState state ) {
                            _state = AnimationState.endOf(state.lifeSpan(), Stride.REGRESSIVE, state.event());
                        }
                    });
        } else {
            Animator.animateFor(_lifetime, Stride.REGRESSIVE, owner)
                    .go(new Animation() {
                        @Override
                        public void run( AnimationState state ) {
                            _state = state;
                            _isOn = false;
                        }
                        @Override
                        public void finish( AnimationState state ) {
                            _state = AnimationState.endOf(state.lifeSpan(), Stride.PROGRESSIVE, state.event());
                        }
                    });
        }
    }

}
