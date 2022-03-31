package com.globaltcad.swingtree;

import java.util.ArrayList;
import java.util.List;

/**
 *  This class is a conceptual extension of the AbstractBuilder which expects implementations
 *  to be builder for nested / tree like data structures.
 *  This is primarily expressed by the "add" method which takes an arbitrary number of
 *  builder instances to form this nested / tree like structure.
 *
 * @param <InstanceType> The concrete type of an implementation of this abstract class.
 * @param <C> The component type parameter which ought to be built in some way.
 */
abstract class AbstractNestedBuilder<InstanceType, C> extends AbstractBuilder<InstanceType, C>
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
    public abstract InstanceType add(C... components);

    /**
     *  This builder class expects its implementations to be builder types
     *  for anything which can be built in a nested tree-like structure.
     *  Implementations of this abstract method ought to enable support for nested building.
     *  <br><br>
     *
     * @param components A list of component instances which ought to be added to the wrapped component type.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final InstanceType add(List<C> components) {
        final C[] array = (C[]) new Object[components.size()];
        for ( int i = 0; i < array.length; i++ ) array[i] = components.get(i);
        return add(array);
    }

}
