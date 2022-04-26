package com.globaltcad.swingtree;

@FunctionalInterface
public interface UIAction<C, E>
{
    void accept(EventContext<C, E> context);

    default void accept(C component, E event) {
        this.accept(new EventContext<>(component, event));
    }
}
