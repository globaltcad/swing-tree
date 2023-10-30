package swingtree;

import swingtree.style.ComponentExtension;
import swingtree.threading.EventProcessor;

import javax.swing.JComponent;
import java.awt.Component;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 *  A library internal object for modelling the state of a builder node,
 *  in particular the component wrapped by the builder node.
 *
 * @param <C> The type of the component wrapped by the builder node.
 */
class BuilderState<C extends Component>
{
    /**
     * The thread mode determines how events are dispatched to the component.
     * And also which type of thread can access the component.
     */
    private final EventProcessor eventProcessor = SwingTree.get().getEventProcessor();

    /**
     *  The type class of the component wrapped by this builder node.
     */
    private final Class<C> componentType;

    private final DetachableReference<Supplier<C>> componentSupplier;

    BuilderState( C component )
    {
        Objects.requireNonNull(component, "component");
        if ( component instanceof JComponent)
            ComponentExtension.makeSureComponentHasExtension( (JComponent) component );
        this.componentType = (Class<C>) component.getClass();
        this.componentSupplier = new DetachableReference<>( () -> component );
    }

    BuilderState<C> with( Consumer<C> action ) {
        action.accept(component());
        return this;
    }

    public C component() {
        return Objects.requireNonNull(componentSupplier.get()).get();
    }

    public void dispose() {
        componentSupplier.detach();
    }

    public EventProcessor eventProcessor() {
        return eventProcessor;
    }

    /**
     *  The type class of the component wrapped by this builder node.
     */
    public Class<C> componentType() {
        return componentType;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" + componentType.getSimpleName() + "]";
    }

    @Override
    public boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( o == null || getClass() != o.getClass() ) return false;

        BuilderState<?> that = (BuilderState<?>) o;

        return componentType.equals(that.componentType);
    }

    @Override
    public int hashCode() {
        return componentType.hashCode();
    }
}
