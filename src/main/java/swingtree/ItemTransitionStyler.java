package swingtree;

import org.jspecify.annotations.Nullable;
import swingtree.animation.*;
import swingtree.api.AnimatedItemStyler;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import java.awt.event.ActionEvent;
import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 *  Models a transition animation towards the current item of a bound property:
 *  every item change restarts the animation, whose {@link AnimationStatus#progress()}
 *  runs from 0 to 1 over the configured lifetime, while the item itself switches
 *  immediately. This is the machinery behind
 *  {@link UIForAnySwing#withStyle(sprouts.Val, LifeTime, AnimatedItemStyler)}
 *  and the item flavoured sibling of the {@link FlipFlopStyler}. <br>
 *  All state in here is UI thread owned: the item is refreshed through captured
 *  property event values (never by reading the property, which belongs to the
 *  application thread) and the animation status is written by the animation
 *  dispatcher, which also runs on the UI thread.
 *
 * @param <T> The type of the item of the bound property.
 * @param <C> The type of the component which this transition animation is applied to.
 */
final class ItemTransitionStyler<T, C extends JComponent>
{
    private final LifeTime                 _lifetime;
    private final AnimatedItemStyler<T, C> _styler;
    private final WeakReference<C>         _owner;

    private @Nullable T _item;
    private @Nullable AnimationStatus _status = null;
    private FlipFlopStyler.@Nullable DisposableAnimation _animation = null;


    ItemTransitionStyler( @Nullable T initialItem, C owner, LifeTime lifetime, AnimatedItemStyler<T, C> styler ) {
        _item     = initialItem;
        _owner    = new WeakReference<>(Objects.requireNonNull(owner));
        _lifetime = Objects.requireNonNull(lifetime);
        _styler   = Objects.requireNonNull(styler);
    }


    ComponentStyleDelegate<C> style( ComponentStyleDelegate<C> delegate ) throws Exception {
        AnimationStatus status = _status;
        if ( status == null ) // Nothing has changed yet, so the initial item is considered fully settled.
            status = AnimationStatus.endOf(
                            LifeSpan.startingNowWith(_lifetime),
                            Stride.PROGRESSIVE,
                            new ActionEvent(this, 0, null),
                            1
                        );
        _status = status;
        return _styler.style(NullUtil.fakeNonNull(_item), status, delegate);
    }

    void set( final @Nullable T newItem ) {
        if ( Objects.equals(_item, newItem) )
            return; // The same item arrived again; there is nothing to transition to.
        C owner = _owner.get();
        if ( owner == null )
            return;
        _item = newItem; // The item switches immediately, only the progress towards it is animated...
        if ( _animation != null ) {
            _animation.dispose();
            _animation = null;
        }
        _status = AnimationStatus.startOf(
                        LifeSpan.startingNowWith(_lifetime),
                        Stride.PROGRESSIVE,
                        new ActionEvent(this, 0, null)
                    );
        _animation = new FlipFlopStyler.DisposableAnimation(new Animation() {
            @Override public void run( AnimationStatus status )    { _status = status; }
            @Override public void finish( AnimationStatus status ) { _status = status; _animation = null; }
        });
        AnimationDispatcher.animateFor(_lifetime, Stride.PROGRESSIVE, owner).go(_animation);
    }

}
