package com.globaltcad.swingtree;

import java.util.function.Consumer;

/**
 *  This is the root builder for all specialized types of builder classes.
 *  It is a generic builder for anything which can be built in a tree like structure!
 *
 * @param <I> The concrete implementation type of this builder.
 * @param <C> The component type parameter.
 */
abstract class AbstractBuilder<I, C>
{
    /**
     *  The component wrapped by this builder node.
     */
    protected final C component;

    /**
     *  The type class of the component wrapped by this builder node.
     *  See documentation for method "build" for more information.
     */
    protected final Class<C> type;

    /**
     *  Instances of the {@link AbstractBuilder} as well as its sub types always wrap
     *  a single component for which they are responsible.
     *
     * @param component The component type which will be wrapped by this builder node.
     */
    public AbstractBuilder(C component) {
        this.type = (Class<C>) component.getClass();
        this.component = component;
    }

    /**
     *  The component wrapped by this builder node.
     */
    public final C getComponent() { return this.component; }

    /**
     *  The type class of the component wrapped by this builder node.
     *  See documentation for method "build" for more information.
     */
    public final Class<C> getType() { return this.type; }

    /**
     *  After having passed a new component type to the constructor of a builder type
     *  one might still wish to modify this component in some way by calling
     *  certain methods in it like "setName", "setTitle", and so on... <br>
     *  This method accepts a lambda to which the component wrapped by this builder will be supplied.
     *  The lambda can then call said methods or perform other tasks which
     *  might be relevant to the component while also not
     *  breaking the benefits of nesting and method chaining provided by this class...
     *  <br><br>
     *
     * @param action A Consumer lambda which simply returned the wrapped JComponent type for interacting it.
     * @return This very instance, which enables builder-style method chaining.
     */
    public I make(Consumer<C> action ) {
        action.accept(component);
        return (I) this;
    }


    /**
     *  Use this to build or not build UI based on a boolean condition value and a consumer
     *  lambda which continues the building process if the previous boolean is true.
     *  This builder will simply be supplied to the provided consumer lambda.
     *  Inside the second lambda, one can then continue building the UI while also not
     *  breaking the benefits of nesting and method chaining provided by this class...
     *  <br><br>
     *
     * @param condition The truth value which determines if the second consumer lambda will be executed.
     * @param building A Consumer lambda which simply consumes this builder.
     * @return This very instance, which enables builder-style method chaining.
     */
    public I doIf(boolean condition, Consumer<I> building) {
        LogUtil.nullArgCheck(building, "building", Consumer.class);
        I builder = (I) this;
        if ( condition ) building.accept(builder);
        return builder;
    }

    /**
     *  Use this to continue building UI inside a provided lambda.
     *  This is especially useful for when you need to build UI based on loops.
     *  This builder will simply be supplied to the provided consumer lambda.
     *  Inside this lambda, you can then continue building the UI while also not
     *  breaking the benefits of nesting and method chaining provided by this class...
     *  <br><br>
     *
     * @param building A Consumer lambda which simply consumes this builder.
     * @return This very instance, which enables builder-style method chaining.
     */
    public I code(Consumer<I> building ) {
        return doIf(true, building);
    }

    /**
     *  This method completes the building process for the wrapped
     *  {@link javax.swing.JComponent} type by returning it.
     *  However, it also expects the user to pass the class of the {@link javax.swing.JComponent}
     *  wrapped by this builder! This is not out of necessity but for better
     *  readability when using the builder in more extensive ways where
     *  the beginning and end of the method chaining and nesting of the builder does
     *  not fit on one screen. <br>
     *  In that case the expression "{@code .getResulting(JMenu.class)}" helps
     *  to identify which type of {@link javax.swing.JComponent} is currently being build on a given
     *  nesting layer... <br><br>
     *
     * @param type The type class of the component which this builder wraps.
     * @param <T> The type parameter of the component which this builder wraps.
     * @return The result of the building process, namely: a type of JComponent.
     */
    public <T extends C> T getResulting(Class<T> type) {
        assert type == this.type || type.isAssignableFrom(this.type);
        return (T)component;
    }
}
