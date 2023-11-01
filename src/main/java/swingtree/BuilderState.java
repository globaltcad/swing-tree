package swingtree;

import swingtree.style.ComponentExtension;
import swingtree.threading.EventProcessor;

import javax.swing.JComponent;
import java.awt.Component;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 *  A library internal object for modelling the state of a builder node,
 *  in particular the component wrapped by the builder node.
 *
 * @param <C> The type of the component wrapped by the builder node.
 */
class BuilderState<C extends Component>
{
    enum Mode {EAGER_MUTATION, MUTATION_BUILDING, INLINE_MUTATION_BUILDING}

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

    private Supplier<C> componentFactory;

    private Function<C, C> componentMutator;


    BuilderState( C component )
    {
        Objects.requireNonNull(component, "component");
        if ( component instanceof JComponent)
            ComponentExtension.initializeFor( (JComponent) component );

        this.eventProcessor    = SwingTree.get().getEventProcessor();
        this.mode              = Mode.EAGER_MUTATION;
        this.componentType     = (Class<C>) component.getClass();
        this.componentFactory  = () -> component;
        this.componentMutator  = c -> c;
    }

    BuilderState( Class<C> type, Supplier<C> componentSource )
    {
        this(type.cast(componentSource.get()));
        /*
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(componentSource, "componentSource");
        this.eventProcessor    = SwingTree.get().getEventProcessor();
        this.mode              = Mode.MUTATION_BUILDING;
        this.componentType     = type;
        this.componentFactory  = componentSource;*/
    }

    private BuilderState(
        EventProcessor eventProcessor,
        Mode           mode,
        Class<C>       type,
        Supplier<C>    componentFactory,
        Function<C, C> componentMutator
    ) {
        Objects.requireNonNull(eventProcessor,  "eventProcessor");
        Objects.requireNonNull(mode,            "mode");
        Objects.requireNonNull(type,            "type");
        Objects.requireNonNull(componentFactory, "componentFactory");
        Objects.requireNonNull(componentMutator, "componentMutator");

        this.eventProcessor    = eventProcessor;
        this.mode              = mode;
        this.componentType     = type;
        this.componentFactory  = componentFactory;
        this.componentMutator  = componentMutator;
    }

    public C component() {
        return Objects.requireNonNull(componentFactory.get());
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

    public void dispose() {
        this.componentFactory = null;
        this.componentMutator = null;
    }

    BuilderState<C> with( Consumer<C> action ) {
        if ( mode == Mode.EAGER_MUTATION || true ) {
            action.accept(component());
            return this;
        }
        else if ( mode == Mode.INLINE_MUTATION_BUILDING) {
            this.componentMutator = this.componentMutator.andThen( c -> {
                                                            action.accept(c);
                                                            return c;
                                                        });
            return this;
        } else  if ( mode == Mode.MUTATION_BUILDING )
            return new BuilderState<>(
                this.eventProcessor,
                this.mode,
                this.componentType,
                this.componentFactory,
                this.componentMutator.andThen(c -> {
                    action.accept(c);
                    return c;
                })
            );
        else
            throw new IllegalStateException("Unknown mode: " + mode);
    }

    BuilderState<C> mutable() {
        return new BuilderState<>(
            this.eventProcessor,
            Mode.INLINE_MUTATION_BUILDING,
            this.componentType,
            this.componentFactory,
            this.componentMutator
        );
    }

    BuilderState<C> incorporate( BuilderState<C> other ) {
        return new BuilderState<>(
            this.eventProcessor,
            this.mode,
            this.componentType,
            this.componentFactory,
            this.componentMutator.andThen(other.componentMutator)
        );
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
