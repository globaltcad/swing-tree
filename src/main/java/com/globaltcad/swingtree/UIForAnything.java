package com.globaltcad.swingtree;

public class UIForAnything<T> extends AbstractBuilder<UIForAnything<T>, T>
{
    /**
     * Instances of the ForAnything builder do not support the
     * "add" method as defined inside the AbstractNestedBuilder.
     *
     * @param component The component type which will be wrapped by this builder node.
     */
    public UIForAnything(T component) { super(component); }
}
