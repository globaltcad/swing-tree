package swingtree.api.mvvm;

import java.util.ArrayList;
import java.util.List;

/**
 *  This represents an occurrence that can be observed as well as triggered.
 *  It is used to register listeners so that they can be notified when the {@link #fire()} method is called.
 *  Contrary to events received by property observers, observing a {@link Event}
 *  does not involve any state.
 */
public interface Event extends Noticeable
{
    /**
     * Triggers this {@link Event}, which means that all registered listeners will be notified.
     */
    void fire();

    /**
     * Creates a new {@link Event} that can be observed and triggered.
     * @param listener The first listener to subscribe to the new {@link Event}.
     * @return A new {@link Event}.
     */
    static Event of( Runnable listener ) {
        Event event = of();
        event.subscribe( listener );
        return event;
    }

    /**
     * Creates a new empty {@link Event} that can be observed and triggered.
     * @return A new {@link Event}.
     */
    static Event of() {
        return new Event() {
            private final List<Runnable> listeners = new ArrayList<>();

            @Override
            public void fire() { listeners.forEach( Runnable::run ); }
            @Override
            public Noticeable subscribe( Runnable listener ) {
                listeners.add( listener );
                return this;
            }
            @Override
            public Noticeable unsubscribe( Runnable listener ) {
                listeners.remove( listener );
                return this;
            }
        };
    }

}
