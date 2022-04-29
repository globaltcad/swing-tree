package com.globaltcad.swingtree;

/**
 *  Instances of this are passed to action lambdas for UI components
 *  to give an action more context information.
 *
 * @param <D> The delegate (in most cases origin UI component) type parameter stored by this.
 * @param <E> The event type parameter of the event stored by this.
 */
public final class EventContext<D,E>
{
    private final D delegate;
    private final E event;

    EventContext(D delegate, E event) { this.delegate = delegate; this.event = event; }

    public D getDelegate() { return delegate; }

    public E getEvent() { return event; }

}

