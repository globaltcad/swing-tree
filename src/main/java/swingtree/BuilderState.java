package swingtree;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
final class BuilderState<C extends Component>
{
    private static final Logger log = LoggerFactory.getLogger(BuilderState.class);

    static final String WHY_A_BUILDER_IS_DISPOSED =
                    "\nA builder is automatically disposed when it is being superseded by a\n" +
                    "new builder instance through a subsequent call to the next builder method\n" +
                    "in the chain of builder method calls.\n" +
                    "The SwingTree API only allows for writing declarative code,\n" +
                    "and the use of procedural GUI code is largely forbidden " +
                    "to ensure readability and prevent side effects.\n" +
                    "In practise, this means that you may not store and reuse references to spent builders.\n" +
                    "This is a similar design choice as in Java's Stream API,\n" +
                    "where an exception is thrown when trying to reuse a stream after it has already been consumed.\n";

    enum Mode
    {
        DECLARATIVE, // Builder states get disposed after being used for building.
        PROCEDURAL   // Builder states do not get disposed after being used for building.
    }

    /**
     *  A builder can either be used in declarative or procedural mode.
     *  In declarative mode, builder nodes get disposed after being used for building.
     *  In procedural mode, builder nodes do not get disposed after being used for building,
     *  meaning that a builder node can be reused for component mutations.
     */
    private final Mode _mode;
    /**
     * The event processor determines the thread execution mode of the component and view model events.
     * And also which type of thread can access the component.
     */
    private final EventProcessor _eventProcessor;
    /**
     *  The type class of the component wrapped by this builder node.
     */
    private final Class<C> _componentType;

    /**
     *  A supplier for the component wrapped by this builder node.
     *  The supplier is null when the builder is disposed.
     */
    private Supplier<C> _componentFetcher; // Is null when the builder is disposed.


    BuilderState( C component )
    {
        Objects.requireNonNull(component, "component");
        if ( component instanceof JComponent)
            ComponentExtension.initializeFor( (JComponent) component );

        _eventProcessor   = SwingTree.get().getEventProcessor();
        _mode             = Mode.DECLARATIVE;
        _componentType    = (Class<C>) component.getClass();
        _componentFetcher = () -> component;
    }

    BuilderState( Class<C> type, Supplier<C> componentSource )
    {
        this(type.cast(componentSource.get()));
    }

    private BuilderState(
        EventProcessor eventProcessor,
        Mode           mode,
        Class<C>       type,
        Supplier<C>    componentFetcher
    ) {
        Objects.requireNonNull(eventProcessor,   "eventProcessor");
        Objects.requireNonNull(mode,             "mode");
        Objects.requireNonNull(type,             "type");
        Objects.requireNonNull(componentFetcher, "componentFetcher");

        _eventProcessor   = eventProcessor;
        _mode             = mode;
        _componentType    = type;
        _componentFetcher = componentFetcher;
    }

    /**
     *  @return The component wrapped by this builder node.
     *  @throws IllegalStateException If this builder state is disposed (it's reference to the component is null).
     */
    public C component()
    {
        if ( this.isDisposed() )
            throw new IllegalStateException(
                    "Trying to access the component of a spent and disposed builder!" +
                    WHY_A_BUILDER_IS_DISPOSED +
                    "If you need to access the component of a builder node, " +
                    "you may only do so through the builder instance returned by the most recent builder method call."
                );

        return _componentType.cast(_componentFetcher.get());
    }

    /**
     * The thread mode determines how events are dispatched to the component.
     * And also which type of thread can access the component. <br>
     * <b>This will never return null.</b>
     */
    public EventProcessor eventProcessor() {
        return _eventProcessor;
    }

    /**
     *  The type class of the component wrapped by this builder node. <br>
     *  <b>This will never return null.</b>
     */
    public Class<C> componentType() {
        return _componentType;
    }

    /**
     *  Cut off the strong reference to the component wrapped by this builder node
     *  and dispose this builder node, meaning it is no longer usable for building. <br>
     *  <b>Only call this method from the UI thread (AWT's EDT thread) as builder states are not thread safe.</b>
     */
    public void dispose() {
        if ( !UI.thisIsUIThread() ) {
            Thread currentThread = Thread.currentThread();
            if ( !currentThread.getName().startsWith("Test worker") )
                log.warn(
                    "The builder state for component type '" + _componentType.getSimpleName() + "' " +
                    "is being disposed from thread '" + currentThread.getName() + "', which is problematic!" +
                    "Builder states should only be disposed by the UI thread (AWT's EDT thread) because " +
                    "they lack thread safety. Furthermore, it is important to note that GUI components " +
                    "should only be assembled in the frontend layer of the application, and not in the backend layer " +
                    "and one of its threads.",
                    new Throwable()
                );
        }
        _componentFetcher = null;
    }

    /**
     *  @return True if this builder node has already been disposed.
     */
    public boolean isDisposed() {
        return _componentFetcher == null;
    }

    /**
     * @param componentMutation A consumer which mutates the component wrapped by this builder node.
     * @return In procedural mode, this very builder node is returned.
     *         In declarative mode, a new builder node is returned which is a copy of this builder node,
     *         and this builder node is disposed.
     *         Either way, the component wrapped by this builder node is mutated by the provided consumer.
     */
    BuilderState<C> with( Consumer<C> componentMutation )
    {
        if ( this.isDisposed() )
            throw new IllegalStateException(
                    "Trying to build using a builder which has already been spent and disposed!" +
                    WHY_A_BUILDER_IS_DISPOSED +
                    "Make sure to only use the builder instance returned by the most recent builder method call."
                );

        try {
            componentMutation.accept(_componentFetcher.get());
        } catch ( Exception e ) {
            e.printStackTrace();
            log.error(
                "Exception while building component of type '" + _componentType.getSimpleName() + "'.", e
            );
            /*
                If individual steps in the builder chain throw exceptions,
                we do not want the entire GUI declaration to fail
                so that only the GUI of the failing component is not built.
            */
        }

        switch ( _mode) 
        {
            case DECLARATIVE :
            {
                Supplier<C> componentFactory = _componentFetcher;
                this.dispose(); // detach strong reference to the component to allow it to be garbage collected.
                return new BuilderState<>(
                        _eventProcessor,
                        _mode,
                        _componentType,
                        componentFactory
                    );
            }
            case PROCEDURAL :
            {
                return this;
            }
            default:
                throw new IllegalStateException("Unknown mode: " + _mode);
        }
    }

    BuilderState<C> procedural()
    {
        if ( this.isDisposed() )
            throw new IllegalStateException(
                    "Trying to build using a builder which is already spent and disposed!" +
                    WHY_A_BUILDER_IS_DISPOSED
                );
        return new BuilderState<>(
            _eventProcessor,
            Mode.PROCEDURAL,
            _componentType,
            _componentFetcher
        );
    }

    BuilderState<C> supersede( BuilderState<C> other )
    {
        if ( this.isDisposed() )
            throw new IllegalStateException(
                    "Trying to build using a builder which has already been spent and disposed!\n" +
                    WHY_A_BUILDER_IS_DISPOSED
                );
        other.dispose();
        return new BuilderState<>(
                _eventProcessor,
                _mode,
                _componentType,
                _componentFetcher
            );
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" + _componentType.getSimpleName() + "]";
    }

    @Override
    public boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( o == null || getClass() != o.getClass() ) return false;

        BuilderState<?> that = (BuilderState<?>) o;

        return _componentType.equals(that._componentType) && _mode == that._mode;
    }

    @Override
    public int hashCode() {
        return _componentType.hashCode();
    }
}
