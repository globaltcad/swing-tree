package com.globaltcad.swingtree.api;

/**
 *  Applies an action to the current component.
 *  See {@link com.globaltcad.swingtree.UIForAbstractSwing#peek(Peeker)}.
 * 	<p>
 * 	<b>Consider taking a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of Swing-Tree in general.</b>
 *
 * @param <C> The component type which should be modified.
 */
public interface Peeker<C>
{
    /**
     * Applies an action to the current component.
     * @param component The component to be modified.
     */
    void accept(C component);
}
