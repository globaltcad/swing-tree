package com.globaltcad.swingtree;

/**
 *  Instances of this are passed to action lambdas for UI components
 *  to give an action more context information.
 *
 * @param <C> The UI component type parameter stored by this.
 * @param <E> The event type parameter of the event stored by this.
 */
public class EventContext<C,E>
{
    private final C component;
    private final E event;

    EventContext(C component, E event) { this.component = component; this.event = event; }

    public C getComponent() { return component; }

    public E getEvent() { return event; }

}

