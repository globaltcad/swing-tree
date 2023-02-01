package swingtree.api.mvvm;

import java.util.ArrayList;
import java.util.List;

public interface Event extends Noticeable
{

    void fire();


    static Event of( Runnable listener ) {
        Event event = of();
        event.subscribe( listener );
        return event;
    }

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
