package com.globaltcad.swingtree.api;

/**
 *  A general purpose type for actions that are triggered by UI components.
 *
 * @param <D> The type of the delegate that will be passed to this event handler.
 */
@FunctionalInterface
public interface UIAction<D>
{
    void accept( D delegate );
}
