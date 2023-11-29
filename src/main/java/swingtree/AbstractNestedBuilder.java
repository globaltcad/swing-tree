package swingtree;

import swingtree.style.ComponentExtension;
import swingtree.style.Style;
import swingtree.threading.EventProcessor;

import javax.swing.JComponent;
import java.awt.Component;
import java.awt.Container;
import java.util.List;

/**
 *  This class is a conceptual extension of the {@link AbstractBuilder} which expects
 *  implementations to define how to compose {@link Component}s into a GUI tree.
 *  This is primarily expressed by the "{@link #add(AbstractNestedBuilder[])}" method which takes an arbitrary number of
 *  builder instances to form said GUI tree structure...
 *
 * @param <I> The type parameter representing the concrete subtype of this abstract class, "I" stands for "Implementation".
 * @param <C> The type parameter representing the concrete component type which this builder is responsible for building.
 * @param <E> The component type parameter which ought to be built in some way.
 */
abstract class AbstractNestedBuilder<I, C extends E, E extends Component> extends AbstractBuilder<I, C>
{
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
        return _with( thisComponent -> {
                    _addComponentsTo( thisComponent, components );
               })
               ._this();
    }

    @SafeVarargs
    protected final void _addComponentsTo( C thisComponent, E... componentsToBeAdded ) {
        for ( E other : componentsToBeAdded )
            _addBuilderTo(thisComponent, UI.of((JComponent) other), null);
    }

    /**
     * @param builder A builder for another {@link JComponent} instance which ought to be added to the wrapped component type.
     * @param <T> The type of the {@link JComponent} which is wrapped by the provided builder.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final <T extends JComponent> I add( UIForAnySwing<?, T> builder ) {
        return (I) this.add( new AbstractNestedBuilder[]{builder} );
    }

    /**
     * This builder class expects its implementations to be builder types
     * for anything which can be built in a nested tree-like structure.
     * Implementations of this abstract method ought to enable support for nested building.
     * <br><br>
     *
     * @param thisComponent The component which is wrapped by this builder.
     * @param component     A component instance which ought to be added to the wrapped component type.
     * @param conf          The layout constraint which ought to be used to add the component to the wrapped component type.
     */
    protected abstract void _addComponentTo( C thisComponent, E component, Object conf );

    protected final void _addBuilderTo( C thisComponent, AbstractNestedBuilder<?, ?, ?> builder, Object conf )
    {
        NullUtil.nullArgCheck(builder, "builder", AbstractNestedBuilder.class);

        boolean isCoupled       = _state().eventProcessor() == EventProcessor.COUPLED;
        boolean isCoupledStrict = _state().eventProcessor() == EventProcessor.COUPLED_STRICT;

        if ( !isCoupled && !isCoupledStrict && !UI.thisIsUIThread() )
            throw new IllegalStateException(
                    "This UI is configured to be decoupled from the application thread, " +
                    "which means that it can only be modified from the EDT. " +
                    "Please use 'UI.run(()->...)' method to execute your modifications on the EDT."
                );

        E childComponent = (E) builder.getComponent();

        if ( childComponent instanceof JComponent ) {
            JComponent child = (JComponent) childComponent;

            Style style = ( conf != null ? null : ComponentExtension.from(child).calculateStyle() );
            if ( style != null )
                conf = style.layout().constraint().orElse(null);

            _addComponentTo(thisComponent, childComponent, conf);

            if ( style != null )
                ComponentExtension.from(child).applyAndInstallStyle(style, true);
            else
                ComponentExtension.from(child).calculateApplyAndInstallStyle(true);
        }
        else
            _addComponentTo(thisComponent, childComponent, conf);

        builder._detachStrongRef(); // Detach strong reference to the component to allow it to be garbage collected.
    }

    /**
     *  This method provides the same functionality as the other "add" methods.
     *  However, it bypasses the necessity to call the "get" method by
     *  calling it internally for you. <br>
     *  This helps to improve readability, especially when the degree of nesting is very low.
     *
     * @param builders An array of builder instances whose JComponents ought to be added to the one wrapped by this builder.
     * @param <B> The type of the builder instances which are used to configure the components that will be added to the component wrapped by this builder.
     * @return This very instance, which enables builder-style method chaining.
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public final <B extends AbstractNestedBuilder<?, ?, JComponent>> I add( B... builders )
    {
        if ( builders == null )
            throw new IllegalArgumentException("Swing tree builders may not be null!");

        return _with( thisComponent -> {
                    _addBuildersTo( thisComponent, builders );
                })
                ._this();
    }

    @SafeVarargs
    protected final <B extends AbstractNestedBuilder<?, ?, JComponent>> void _addBuildersTo(
        C thisComponent,
        B... builders
    ) {
        for ( AbstractNestedBuilder<?, ?, ?> b : builders )
            _addBuilderTo(thisComponent, b, null);
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
        return _with( thisComponent -> {
                    for ( E component : components )
                        _addBuilderTo(thisComponent, UI.of((JComponent) component), null);
                })
                ._this();
    }

    protected final int _childCount( C c ) {
        return  ( c instanceof Container ? ( (Container) c ).getComponentCount() : 0 );
    }

}
