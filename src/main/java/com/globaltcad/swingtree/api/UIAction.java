package com.globaltcad.swingtree.api;

import com.globaltcad.swingtree.EventContext;

@FunctionalInterface
public interface UIAction<C, E>
{
    void accept(EventContext<C, E> context);
}
