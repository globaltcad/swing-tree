package com.globaltcad.swingtree;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *  This class is a conceptual extension of the AbstractBuilder which expects implementations
 *  to be builder for nested / tree like data structures.
 *  This is primarily expressed by the "add" method which takes an arbitrary number of
 *  builder instances to form this nested / tree like structure.
 *
 * @param <I> The concrete implementation type of this abstract class.
 * @param <C> The component type parameter which ought to be built in some way.
 */
abstract class AbstractNestedBuilder<I, C extends E, E> extends AbstractBuilder<I, C>
{
    /**
     *  A list of all the child builders.
     */
    private final List<AbstractNestedBuilder<?,?,?>> _children = new ArrayList<>();
    private AbstractNestedBuilder<?,?,?> _parent; // The parent builder (This may be null if no parent present or provided)

    /**
     * Instances of the AbstractNestedBuilder as well as its sub types always wrap
     * a single component for which they are responsible.
     * In addition to the AbstractBuilder this builder also requires nesting.
     *
     * @param component The component type which will be wrapped by this builder node.
     */
    public AbstractNestedBuilder( C component ) { super(component); }

    /**
     *  A list of all the siblings of the component wrapped by this builder.
     */
    protected final List<E> getSiblinghood() {
        if ( _parent == null ) return new ArrayList<>();
        return _parent._children.stream().map(c -> (E) c.getComponent()).collect(Collectors.toList());
    }

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
    public final I add( E... components ) {
        LogUtil.nullArgCheck(components, "components", Object[].class);
        for ( E c : components ) _doAdd(UI.of((JComponent) c), null);
        return (I) this;
    }

    /**
     *  This builder class expects its implementations to be builder types
     *  for anything which can be built in a nested tree-like structure.
     *  Implementations of this abstract method ought to enable support for nested building.
     *  <br><br>
     *
     * @param component A component instance which ought to be added to the wrapped component type.
     */
    protected abstract void _add( E component, Object conf );

    protected final void _doAdd( AbstractNestedBuilder<?, ?, ?> builder, Object conf)
    {
        LogUtil.nullArgCheck(builder, "builder", AbstractNestedBuilder.class);

        if ( _threadMode == ThreadMode.DECOUPLED && !UI.thisIsUIThread() )
            throw new IllegalStateException(
                    "This UI is configured to be decoupled from the application thread, " +
                    "which means that it can only be modified from the EDT. " +
                    "Please use 'UI.run(()->...)' method to execute your modifications on the EDT."
                );

        if ( _children.contains(builder) )
            throw new IllegalArgumentException("Builder already used!");

        _children.add(builder);

        if ( builder._parent != null )
            throw new IllegalArgumentException("Builder already used!");

        builder._parent = this;

        _add((E) builder.getComponent(), conf);
    }

    /**
     *  This method provides the same functionality as the other "add" methods.
     *  However, it bypasses the necessity to call the "get" method by
     *  calling it internally for you. <br>
     *  This helps to improve readability, especially when the degree of nesting is very low.
     *
     * @param builders An array of builder instances whose JComponents ought to be added to the one wrapped by this builder.
     * @return This very instance, which enables builder-style method chaining.
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public final <X, Y extends E, B extends AbstractNestedBuilder<X, Y, E>> I add( B... builders ) {
        if ( builders == null )
            throw new IllegalArgumentException("Swing tree builders may not be null!");

        for ( AbstractNestedBuilder<?, ?, ?> b : builders )
            _doAdd( b, null );

        return (I) this;
    }


    /**
     *  This builder class expects its implementations to be builder types
     *  for anything which can be built in a nested tree-like structure.
     *  Implementations of this abstract method ought to enable support for nested building.
     *  <br><br>
     *
     * @param components A list of component instances which ought to be added to the wrapped component type.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I add( List<E> components ) {
        final C[] array = (C[]) new Object[components.size()];
        for ( int i = 0; i < array.length; i++ )
            _doAdd(UI.of((JComponent) components.get(i)), null);

        return (I) this;
    }

}
