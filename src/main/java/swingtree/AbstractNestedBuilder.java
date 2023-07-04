package swingtree;

import swingtree.threading.EventProcessor;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *  This class is a conceptual extension of the {@link AbstractBuilder} which expects
 *  implementations to define how to compose {@link Component}s into a GUI tree.
 *  This is primarily expressed by the "{@link #add(AbstractNestedBuilder[])}" method which takes an arbitrary number of
 *  builder instances to form said GUI tree structure...
 *
 * @param <I> The concrete implementation type of this abstract class, "I" stands for "Implementation".
 * @param <C> The component type parameter which ought to be built in some way.
 */
abstract class AbstractNestedBuilder<I, C extends E, E extends Component> extends AbstractBuilder<I, C>
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
     *  A list of all the siblings of the component wrapped by this builder,
     *  which includes the current component itself.
     *
     * @return A list of all the siblings of the component wrapped by this builder.
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
        NullUtil.nullArgCheck(components, "components", Object[].class);
        for ( E c : components ) _doAdd(UI.of((JComponent) c), null);
        return _this();
    }

    /**
     * @param builder A builder for another {@link JComponent} instance which ought to be added to the wrapped component type.
     * @param <T> The type of the {@link JComponent} which is wrapped by the provided builder.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final <T extends JComponent> I add( UIForAnySwing<?, T> builder ) {
        this.add(new AbstractNestedBuilder[]{builder});
        return _this();
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
        NullUtil.nullArgCheck(builder, "builder", AbstractNestedBuilder.class);

        boolean isCoupled       = _eventProcessor == EventProcessor.COUPLED;
        boolean isCoupledStrict = _eventProcessor == EventProcessor.COUPLED_STRICT;

        if ( !isCoupled && !isCoupledStrict && !UI.thisIsUIThread() )
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

        E component = (E) builder.getComponent();

        builder._parent = this;

        _add( component, conf );

        if ( component instanceof JComponent )
            ComponentExtension.from((JComponent) component).establishStyle();

        _detachStrongRef(); // Detach strong reference to the component to allow it to be garbage collected.
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

        return _this();
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
        for ( E component : components )
            _doAdd(UI.of((JComponent) component), null);

        return _this();
    }

    protected final int _childCount() { return _children.size(); }

}
