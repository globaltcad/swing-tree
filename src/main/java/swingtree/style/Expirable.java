package swingtree.style;

import swingtree.animation.LifeTime;

import java.util.Objects;

/**
 *  A wrapper class for a payload that has a lifetime which
 *  is used to determine if the payload is expired or not.
 **/
class Expirable<T>
{
    private final LifeTime _lifetime;
    private final T        _value;


    Expirable( LifeTime lifetime, T payload ) {
        _lifetime = Objects.requireNonNull(lifetime);
        _value    = Objects.requireNonNull(payload);
    }

    /**
     *  @return True if the lifetime of this instance is expired,
     *          false otherwise.
     **/
    boolean isExpired() { return _lifetime.isExpired(); }

    /**
     *  @return The payload of this instance.
     **/
    T get() { return _value; }

    /**
     *  @return The lifetime of this instance.
     **/
    LifeTime getLifeTime() { return _lifetime; }
}
