package swingtree.style;

import swingtree.animation.LifeSpan;

import java.util.Objects;

/**
 *  A wrapper class for a payload that has a lifetime which
 *  is used to determine if the payload is expired or not.
 **/
final class Expirable<T>
{
    private final LifeSpan _lifeSpan;
    private final T        _value;


    Expirable( LifeSpan lifeSpan, T payload ) {
        _lifeSpan = Objects.requireNonNull(lifeSpan);
        _value    = Objects.requireNonNull(payload);
    }

    /**
     *  Determines if the lifetime of this instance is expired,
     *  which is based on the current time and the start and end
     *  of the lifespan.
     *  @return True if the lifetime of this instance is expired,
     *          false otherwise.
     **/
    boolean isExpired() { return _lifeSpan.isExpired(); }

    /**
     *  Exposes the thing that can expire.
     *  @return The payload of this instance.
     **/
    T get() { return _value; }

    /**
     *  Exposes the lifespan of this instance,
     *  which is used to determine if the payload is expired.
     *  @return The time span of this instance.
     **/
    LifeSpan getLifeSpan() { return _lifeSpan; }
}
