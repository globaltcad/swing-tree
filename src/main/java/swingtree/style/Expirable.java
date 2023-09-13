package swingtree.style;

import swingtree.animation.LifeTime;

/**
 *  A wrapper class for a payload that has a lifetime which
 *  is used to determine if the payload is expired or not.
 **/
class Expirable<B>
{
    private final LifeTime _lifetime;
    private final B _value;


    Expirable( LifeTime lifetime, B payload ) {
        _lifetime = lifetime;
        _value = payload;
    }

    /**
     *  @return True if the lifetime of this instance is expired,
     *          false otherwise.
     **/
    boolean isExpired() { return _lifetime.isExpired(); }

    /**
     *  @return The payload of this instance.
     **/
    B get() { return _value; }
}
