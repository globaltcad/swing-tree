package swingtree.style;

import swingtree.animation.LifeTime;

class Expirable<B>
{
    private final LifeTime _lifetime;
    private final B _value;


    Expirable( LifeTime lifetime, B payload ) {
        _lifetime = lifetime;
        _value = payload;
    }

    boolean isExpired() { return _lifetime.isExpired(); }

    B get() { return _value; }
}
