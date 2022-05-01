package com.globaltcad.swingtree;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 *  This class is a conceptual extension of the AbstractBuilder which expects implementations
 *  to be builder for nested / tree like data structures.
 *  This is primarily expressed by the "add" method which takes an arbitrary number of
 *  builder instances to form this nested / tree like structure.
 *
 * @param <I> The concrete implementation type of this abstract class.
 * @param <C> The component type parameter which ought to be built in some way.
 */
abstract class AbstractNestedBuilder<I, C> extends AbstractBuilder<I, C>
{

    /**
     *  A list of all the siblings of the component wrapped by this builder.
     */
    protected final List<C> siblings = new ArrayList<>();


    /**
     * Instances of the AbstractNestedBuilder as well as its sub types always wrap
     * a single component for which they are responsible.
     * In addition to the AbstractBuilder this builder also requires nesting.
     *
     * @param component The component type which will be wrapped by this builder node.
     */
    public AbstractNestedBuilder(C component) {
        super(component);
    }

    /**
     *  A list of all the siblings of the component wrapped by this builder.
     */
    public final List<C> getSiblings() { return this.siblings; }

    /**
     *  This builder class expects its implementations to be builder types
     *  for anything which can be built in a nested tree-like structure.
     *  Implementations of this abstract method ought to enable support for nested building.
     *  <br><br>
     *
     * @param components An array of component instances which ought to be added to the wrapped component type.
     * @return This very instance, which enables builder-style method chaining.
     */
    @SafeVarargs
    public final I add(C... components) {
        LogUtil.nullArgCheck(components, "components", Object[].class);
        for( C c : components ) _add(c);
        return (I) this;
    }


    /**
     *  This builder class expects its implementations to be builder types
     *  for anything which can be built in a nested tree-like structure.
     *  Implementations of this abstract method ought to enable support for nested building.
     *  <br><br>
     *
     * @param component A component instance which ought to be added to the wrapped component type.
     * @return This very instance, which enables builder-style method chaining.
     */
    protected abstract I _add(C component);

    /**
     *  This builder class expects its implementations to be builder types
     *  for anything which can be built in a nested tree-like structure.
     *  Implementations of this abstract method ought to enable support for nested building.
     *  <br><br>
     *
     * @param components A list of component instances which ought to be added to the wrapped component type.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I add(List<C> components) {
        final C[] array = (C[]) new Object[components.size()];
        for ( int i = 0; i < array.length; i++ )
            _add(components.get(i));

        return (I) this;
    }

}
