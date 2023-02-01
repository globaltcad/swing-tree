package swingtree.api.mvvm;

/**
 *  This represents an occurrence that can be observed but not triggered.
 *  It is used to register listeners so that they can be notified.
 *  Contrary to events received by property observers, observing a {@link Noticeable}
 *  does not involve any state.
 */
public interface Noticeable
{
    /**
     * Subscribes the given listener to this {@link Noticeable}.
     * @param listener the listener to subscribe.
     * @return this {@link Noticeable} for chaining.
     */
    Noticeable subscribe( Runnable listener );

    /**
     * Unsubscribes the given listener from this {@link Noticeable}.
     * @param listener the listener to unsubscribe.
     * @return this {@link Noticeable} for chaining.
     */
    Noticeable unsubscribe( Runnable listener );
}
