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
    enum Mode { EAGER_BUILDER, FACTORY_BUILDER }

    private final Mode mode;
    /**
     * The thread mode determines how events are dispatched to the component.
     * And also which type of thread can access the component.
     */
    private final EventProcessor eventProcessor;
    /**
     *  The type class of the component wrapped by this builder node.
     */
    private final Class<C> componentType;

    private final DetachableReference<Supplier<C>> componentSupplier;


    BuilderState( C component )
    {
        Objects.requireNonNull(component, "component");
        if ( component instanceof JComponent)
            ComponentExtension.initializeFor( (JComponent) component );

        this.eventProcessor    = SwingTree.get().getEventProcessor();
        this.mode              = Mode.EAGER_BUILDER;
        this.componentType     = (Class<C>) component.getClass();
        this.componentSupplier = new DetachableReference<>( () -> component );
    }

    BuilderState( Class<C> type, Supplier<C> componentSource )
    {
        this(type.cast(componentSource.get()));
        /*
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(componentSource, "componentSource");
            this.eventProcessor    = SwingTree.get().getEventProcessor();
            this.mode              = Mode.FACTORY_BUILDER;
            this.componentType     = type;
            this.componentSupplier = new DetachableReference<>( () -> {
                C component = componentSource.get();
                if ( component instanceof JComponent)
                    ComponentExtension.makeSureComponentHasExtension( (JComponent) component );
                return component;
            } );
        */
    }

    private BuilderState(
        EventProcessor eventProcessor,
        Mode mode,
        Class<C> type,
        Supplier<C> componentSource
    ) {
        Objects.requireNonNull(eventProcessor,  "eventProcessor");
        Objects.requireNonNull(type,            "type");
        Objects.requireNonNull(componentSource, "componentSource");

        this.eventProcessor    = eventProcessor;
        this.mode              = mode;
        this.componentType     = type;
        this.componentSupplier = new DetachableReference<>(componentSource::get);
    }

    BuilderState<C> with( Consumer<C> action ) {
        if ( mode == Mode.EAGER_BUILDER ) {
            action.accept(component());
            return this;
        }
        else if ( mode == Mode.FACTORY_BUILDER )
            return new BuilderState<>(
                this.eventProcessor,
                this.mode,
                this.componentType,
                () -> {
                    C component = Objects.requireNonNull(componentSupplier.get()).get();
                    action.accept(component);
                    return component;
                }
            );
        else
            throw new IllegalStateException("Unknown mode: " + mode);
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
