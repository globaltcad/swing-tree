package com.globaltcad.swingtree.api;

/**
 *  Applies an action to the current component.
 *
 * @param <C> The component type which should be modified.
 */
public interface Peeker<C>
{
    void accept(C component);
}
