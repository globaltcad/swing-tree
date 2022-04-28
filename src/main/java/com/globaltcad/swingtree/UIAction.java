package com.globaltcad.swingtree;

@FunctionalInterface
public interface UIAction<C, E>
{
    void accept(EventContext<C, E> context);
}
