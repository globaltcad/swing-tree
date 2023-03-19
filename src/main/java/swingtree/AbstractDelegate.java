package swingtree;

import swingtree.animation.Animate;
import swingtree.animation.Animation;
import swingtree.animation.Schedule;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

class AbstractDelegate<C extends Component>
{
    private final Query _query;
    private final C _component;

    AbstractDelegate(C component, Component handle) {
        _query = new Query(handle);
        _component = component;
    }

    protected C _component() { return (C) _component; }

    /**
     *  Use this to query the UI tree and find any {@link JComponent}.
     *
     * @param type The {@link JComponent} type which should be found in the swing tree.
     * @param id The ide of the {@link JComponent} which should be found in the swing tree.
     * @return An {@link Optional} instance which may or may not contain the requested component.
     * @param <T> The type parameter of the component which should be found.
     */
    public final <T extends JComponent> OptionalUI<T> find( Class<T> type, String id ) {
        return _query.find(type, id);
    }


    public final Animate animate( double duration, TimeUnit unit ) {
        return Animate.on( _component(), Schedule.of(duration, unit) );
    }

    public final void animateOnce( double duration, TimeUnit unit, Animation animation ) {
        this.animate(duration, unit).runOnce(animation);
    }
}
