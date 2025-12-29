package swingtree;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Component;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A container object for AWT {@link Component} types
 * which may or may not contain a non-{@code null} value.
 * If a value is present, {@code isPresent()} returns {@code true}. If no
 * value is present, the object is considered <i>empty</i> and
 * {@code isPresent()} returns {@code false}.
 *
 * <p>Additional methods that depend on the presence or absence of a contained
 * value are provided, such as {@link #orElse(Component) orElse()}
 * (returns a default value if no value is present) and
 * {@link #ifPresent(Consumer) ifPresent()} (performs an
 * action if a value is present).
 *
 * <p>This is a <b>value-based</b>
 * class; use of identity-sensitive operations (including reference equality
 * ({@code ==}), identity hash code, or synchronization) on instances of
 * {@code OptionalUI} may have unpredictable results and should be avoided.
 * <p>
 * Note that
 * {@code OptionalUI} is primarily intended for use as a SwingTree query return type where
 * there is a clear need to represent "no result," and where returning {@code null} as well
 * as expose the UI components to the application thread directly
 * is likely to cause errors. A variable whose type is {@code OptionalUI} should
 * never itself be {@code null}; it should always point to an {@code OptionalUI}
 * instance.
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 *
 * @param <C> the type of component held by this instance
 */
public final class OptionalUI<C extends Component> {

    private static final Logger log = LoggerFactory.getLogger(OptionalUI.class);

    /**
     * Common instance for {@code empty()}.
     */
    private static final OptionalUI<?> EMPTY = new OptionalUI<>(null);

    /**
     * If non-null, the value; if null, indicates no value is present
     */
    private final @Nullable C _component;

    /**
     * Returns an empty {@code OptionalUI} instance.  No value is present for this
     * {@code OptionalUI}.
     *
     * @apiNote
     * Though it may be tempting to do so, avoid testing if an object is empty
     * by comparing with {@code ==} against instances returned by
     * {@code OptionalUI.empty()}.  There is no guarantee that it is a singleton.
     * Instead, use {@link #isPresent()}.
     *
     * @param <T> The type of the non-existent value
     * @return an empty {@code OptionalUI}
     */
    static<T extends Component> OptionalUI<T> empty() {
        @SuppressWarnings("unchecked")
        OptionalUI<T> t = (OptionalUI<T>) EMPTY;
        return t;
    }

    /**
     * Constructs an instance with the described value.
     *
     * @param value the value to describe; it's the caller's responsibility to
     *        ensure the value is non-{@code null} unless creating the singleton
     *        instance returned by {@code empty()}.
     */
    private OptionalUI( @Nullable C value ) { this._component = value; }

    /**
     * Returns an {@code OptionalUI} describing the given value, if
     * non-{@code null}, otherwise returns an empty {@code OptionalUI}.
     *
     * @param component the possibly-{@code null} value to describe
     * @param <T> the type of the value
     * @return an {@code OptionalUI} with a present value if the specified value
     *         is non-{@code null}, otherwise an empty {@code OptionalUI}
     */
    @SuppressWarnings("unchecked")
    static <T extends Component> OptionalUI<T> ofNullable(@Nullable T component) {
        return component == null ? (OptionalUI<T>) EMPTY
                : new OptionalUI<>(component);
    }

    /**
     *  Allows you to resolve a SwingTree declaration on the GUI thread
     *  by providing a supplier of a {@link UIForAnything} builder node.
     * @param ui A supplier of a SwingTree declaration to be executed on the GUI thread.
     * @return An optional of a component.
     * @param <C> The component type to fetch.
     * @param <T> A generic supertype of the component, but usually the component type itself.
     */
    static <@Nullable C extends T, T extends Component> OptionalUI<C> of(Supplier<UIForAnything<?,C,T>> ui) {
        return UI.runAndGet(()-> {
            C component = (C) ui.get().get((Class) java.awt.Component.class);
            return component == null ? (OptionalUI<C>) EMPTY
                    : new OptionalUI<>(component);
        });
    }

    /**
     * If a component is present, returns {@code true}, otherwise {@code false}.
     *
     * @return {@code true} if a component is present, otherwise {@code false}
     */
    public boolean isPresent() { return _component != null; }

    /**
     * If a component is  not present, returns {@code true}, otherwise
     * {@code false}.
     *
     * @return  {@code true} if a component is not present, otherwise {@code false}
     */
    public boolean isEmpty() { return _component == null; }

    /**
     * If a component is present, performs the given action with the component,
     * otherwise does nothing.
     *
     * @param action the action to be performed, if a component is present
     * @throws NullPointerException if component is present and the given action is
     *         {@code null}
     */
    public void ifPresent( Consumer<? super C> action ) {
        if ( _component != null )
            UI.run(() -> action.accept(_component));
    }

    /**
     * If a component is present, performs the given action with the component,
     * otherwise performs the given empty-based action.
     *
     * @param action the action to be performed, if a component is present
     * @param emptyAction the empty-based action to be performed, if no component is
     *        present
     * @throws NullPointerException if a component is present and the given action
     *         is {@code null}, or no component is present and the given empty-based
     *         action is {@code null}.
     */
    public void ifPresentOrElse( Consumer<? super C> action, Runnable emptyAction ) {
        UI.run(()->{
            try {
                if (_component != null)
                    action.accept(_component);
                else
                    emptyAction.run();
            } catch (Exception e) {
                log.error(SwingTree.get().logMarker(), "Error performing action on UI component of OptionalUI instance.", e);
            }
        });
    }

    /**
     * If a component is present, and the component matches the given predicate,
     * returns an {@code OptionalUI} describing the component, otherwise returns an
     * empty {@code OptionalUI}.
     *
     * @param predicate the predicate to apply to a component, if present
     * @return an {@code OptionalUI} describing the component of this
     *         {@code OptionalUI}, if a component is present and the component matches the
     *         given predicate, otherwise an empty {@code OptionalUI}
     * @throws NullPointerException if the predicate is {@code null}
     */
    public OptionalUI<C> filter(Predicate<? super C> predicate) {
        Objects.requireNonNull(predicate);
        if (!isPresent()) {
            return this;
        } else {
            try {
                if (!UI.thisIsUIThread()) {
                    try {
                        return UI.runAndGet(() -> filter(predicate));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else
                    return predicate.test(_component) ? this : empty();
            } catch (Exception e) {
                log.error(SwingTree.get().logMarker(),
                    "Error filtering UI component of OptionalUI instance. " +
                    "Returning current OptionalUI instance instead.",
                    e
                );
            }
        }
        return this;
    }

    /**
     * If a component is present, returns an {@code OptionalUI} describing (as if by
     * {@link #ofNullable}) the result of applying the given mapping function to
     * the component, otherwise returns an empty {@code OptionalUI}.
     *
     * <p>If the mapping function returns a {@code null} result then this method
     * returns an empty {@code OptionalUI}.
     *
     * @param mapper the mapping function to apply to a component, if present
     * @param <U> The type of the component returned from the mapping function
     * @return an {@code Optional} describing the result of applying a mapping
     *         function to the UI component of this {@code OptionalUI}, if a component is
     *         present, otherwise an empty {@code OptionalUI}
     * @throws NullPointerException if the mapping function is {@code null}
     */
    public <U> Optional<U> map( Function<? super C, ? extends U> mapper ) {
        Objects.requireNonNull(mapper);
        if ( !this.isPresent() ) return Optional.empty();
        else {
            try {
                if (UI.thisIsUIThread())
                    return Optional.ofNullable(mapper.apply(_component));
                else {
                    try {
                        return UI.runAndGet(() -> map(mapper));
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            } catch (Exception ex) {
                log.error(SwingTree.get().logMarker(),
                    "Error mapping OptionalUI to Optional instance! " +
                    "Returning current OptionalUI instance instead.",
                    ex
                );
            }
        }
        return Optional.empty();
    }

    /**
     *  An alternative to {@link #map(Function)} that maps to the same type in yet another
     *  {@code OptionalUI} instance. This is useful for chaining UI centric operations.
     *  The mapping function should return an {@code OptionalUI} instance.
     *
     * @param mapper The mapping function to apply to a component, if present.
     * @return an {@code OptionalUI} describing the result of applying a mapping
     *         function to the UI component of this {@code OptionalUI}, if a component is
     *         present, otherwise an empty {@code OptionalUI}
     * @throws NullPointerException if the mapping function is {@code null}
     */
    public OptionalUI<C> update( Function<C, C> mapper ) {
        Objects.requireNonNull(mapper);
        if ( !this.isPresent() ) return this;
        else {
            try {
                if (UI.thisIsUIThread())
                    return OptionalUI.ofNullable(mapper.apply(_component));
                else {
                    try {
                        return UI.runAndGet(() -> update(mapper));
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            } catch (Exception ex) {
                log.error(SwingTree.get().logMarker(),
                    "Error creating an updated OptionalUI instance! " +
                    "Returning current OptionalUI instance instead.",
                    ex
                );
            }
            return this;
        }
    }

    /**
     *  An alternative to {@link #update(Function)} and {@link #map(Function)} that maps to
     *  the same type in yet another {@code OptionalUI} instance but with the
     *  difference that the mapping function is only applied if the component is
     *  present <b>and assignable to the given type</b>. <br>
     *  It is a type conditional mapping operation.
     *
     * @param type The type to check if the component is assignable to.
     * @param mapper The mapping function to apply to a component of the given type, if present.
     * @return An {@code OptionalUI} describing the result of applying a mapping
     *        function to the UI component of this {@code OptionalUI}, if a component is
     *        present and the component is assignable to the given type, otherwise an
     *        empty {@code OptionalUI}.
     * @param <U> The type of the component returned from the mapping function.
     * @throws NullPointerException if the mapping function is {@code null}
     * @throws NullPointerException if the given type is {@code null}
     */
    public <U extends C> OptionalUI<C> updateIf(Class<U> type, Function<U, U> mapper) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(mapper);
        if ( !this.isPresent() ) return this;
        else {
            try {
                if (UI.thisIsUIThread()) {
                    Objects.requireNonNull(_component);
                    // Check if the component is assignable to the given type
                    if (type.isAssignableFrom(_component.getClass())) {
                        @SuppressWarnings("unchecked")
                        U u = (U) _component;
                        return OptionalUI.ofNullable(mapper.apply(u));
                    } else {
                        return this;
                    }
                } else {
                    try {
                        return UI.runAndGet(() -> updateIf(type, mapper));
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            } catch (Exception ex) {
                log.error(SwingTree.get().logMarker(),
                    "Error creating an updated OptionalUI instance! " +
                    "Returning current OptionalUI instance instead.",
                    ex
                );
            }
        }
        return this;
    }

    /**
     *  An alternative to {@link #update(Function)} and {@link #map(Function)} that maps to
     *  the same type in yet another {@code OptionalUI} instance but with the
     *  difference that the mapping function is only applied if the component is
     *  present <b>and the supplied boolean is true</b>. <br>
     *  It is a conditional mapping operation.
     *
     * @param condition The boolean to check before applying the mapping function.
     * @param mapper The mapping function to apply to a component, if present and the condition is true.
     * @return An {@code OptionalUI} describing the result of applying a mapping
     *        function to the UI component of this {@code OptionalUI}, if a component is
     *        present and the condition is true, otherwise an empty {@code OptionalUI}.
     * @throws NullPointerException if the mapping function is {@code null}
     */
    public OptionalUI<C> updateIf( boolean condition, Function<C, C> mapper ) {
        Objects.requireNonNull(mapper);
        if ( !condition ) return this;
        else
            return update(mapper);
    }

    /**
     * If a component is present, returns an {@code OptionalUI} describing the component,
     * otherwise returns an {@code OptionalUI} produced by the supplying function.
     * Use this to provide alternative UI components.
     *
     * @param supplier the supplying function that produces an {@code OptionalUI}
     *        to be returned
     * @return returns an {@code OptionalUI} describing the component of this
     *         {@code OptionalUI}, if a component is present, otherwise an
     *         {@code OptionalUI} produced by the supplying function.
     * @throws NullPointerException if the supplying function is {@code null} or
     *         produces a {@code null} result
     */
    public OptionalUI<C> or( Supplier<? extends OptionalUI<? extends C>> supplier ) {
        Objects.requireNonNull(supplier);
        if ( this.isPresent() ) return this;
        else {
            try {
                @SuppressWarnings("unchecked")
                OptionalUI<C> r = (OptionalUI<C>) supplier.get();
                return Objects.requireNonNull(r);
            } catch (Exception e) {
                log.error(SwingTree.get().logMarker(),
                    "Error creating fetching alternative OptionalUI instance! " +
                    "Returning current OptionalUI instead.",
                    e
                );
            }
        }
        return this;
    }

    /**
     * If no component is present, the supplying function is called to provide an
     * alternative UI component to be used in place of the missing component.
     * Otherwise, returns a {@code OptionalUI} containing the current component
     * and the supplying function is not called.
     * Use this to define alternative UI components.
     *
     * @param supplier the supplying function that produces a UI component to be
     *                 used if no component is present.
     * @return returns an {@code OptionalUI} describing the component of this
     *         {@code OptionalUI}, if a component is present, otherwise an
     *         {@code OptionalUI} produced by the supplying function.
     * @throws NullPointerException if the supplying function is {@code null} or
     *         produces a {@code null} result
     */
    public OptionalUI<C> orGet( Supplier<? extends C> supplier ) {
        Objects.requireNonNull(supplier);
        if ( this.isPresent() ) return this;
        else {
            try {
                C c = supplier.get();
                return OptionalUI.ofNullable(c);
            } catch (Exception e) {
                log.error(SwingTree.get().logMarker(),
                    "Error creating fetching alternative UI component! " +
                    "Returning current OptionalUI instead.",
                    e
                );
            }
        }
        return this;
    }

    /**
     * If no component is present and the supplied boolean is true,
     * the supplying function is called to provide an
     * alternative UI component to be used in place of the missing component.
     * Otherwise, returns a {@code OptionalUI} containing the current component
     * and the supplying function is not called.
     * Use this to define alternative UI components if a condition is met.
     *
     * @param condition The boolean condition to check before calling the supplying function.
     *                  If false, the supplying function is simply ignored.
     * @param supplier the supplying function that produces a UI component to be
     *                 used if no component is present.
     * @return returns an {@code OptionalUI} describing the component of this
     *         {@code OptionalUI}, if a component is present, otherwise an
     *         {@code OptionalUI} produced by the supplying function.
     * @throws NullPointerException if the supplying function is {@code null} or
     *         produces a {@code null} result
     */
    public OptionalUI<C> orGetIf( boolean condition, Supplier<? extends C> supplier ) {
        Objects.requireNonNull(supplier);
        if ( !condition )
            return this;
        else
            if ( this.isPresent() )
                return this;
        else
        {
            try {
                C c = supplier.get();
                return OptionalUI.ofNullable(c);
            } catch (Exception e) {
                log.error(SwingTree.get().logMarker(),
                    "Error creating fetching alternative UI component! " +
                    "Returning current OptionalUI instead.",
                    e
                );
            }
        }
        return this;
    }

    /**
     * If a component is present, returns an {@code OptionalUI} describing the component,
     * otherwise returns a {@code OptionalUI} containing the component built by the UI declaration
     * inside the supplying function.
     * Use this to provide alternative UI components.
     *
     * @param <A> The type of the component to be built.
     * @param <B> The base type of the component to be built.
     * @param supplier the supplying function that produces a UI declaration
     *                 to be used if no component is present.
     * @return returns an {@code OptionalUI} describing the component of this
     *         {@code OptionalUI}, if a component is present, otherwise an
     *         {@code OptionalUI} produced by the supplying function.
     * @throws NullPointerException if the supplying function is {@code null} or
     *         produces a {@code null} result
     */
    public <A extends B, B extends C> OptionalUI<C> orGetUi( Supplier<UIForAnything<?,A,B>> supplier ) {
        Objects.requireNonNull(supplier);
        if ( this.isPresent() ) return this;
        else {
            try {
                UIForAnything<?, A, B> ui = supplier.get();
                return OptionalUI.ofNullable(ui.get(ui.getType()));
            } catch (Exception e) {
                log.error(SwingTree.get().logMarker(),
                    "Error creating fetching alternative UI component! " +
                    "Returning current OptionalUI instead.",
                    e
                );
            }
        }
        return this;
    }

    /**
     * If no component is present and the supplied boolean is true,
     * the supplying function is called to provide an
     * alternative UI declaration to be used to build the missing component.
     * Otherwise, returns the current {@code OptionalUI} and the supplying function is not called.
     * Use this to define alternative UI declaration if a condition is met.
     *
     * @param <A> The type of the component to be built.
     * @param <B> The base type of the component to be built.
     * @param condition The boolean condition to check before calling the supplying function.
     *                  If false, the supplying function is simply ignored.
     * @param supplier the supplying function that produces a UI declaration
     *                 to be used if no component is present.
     * @return returns an {@code OptionalUI} describing the component of this
     *         {@code OptionalUI}, if a component is present, otherwise an
     *         {@code OptionalUI} produced by the supplying function.
     * @throws NullPointerException if the supplying function is {@code null} or
     *         produces a {@code null} result
     */
    public <A extends B, B extends C> OptionalUI<C> orGetUiIf( boolean condition, Supplier<UIForAnything<?,A,B>> supplier ) {
        Objects.requireNonNull(supplier);
        if ( !condition )
            return this;
        else
            if ( this.isPresent() )
                return this;
        else
        {
            try {
                UIForAnything<?, A, B> ui = supplier.get();
                return OptionalUI.ofNullable(ui.get(ui.getType()));
            } catch (Exception e) {
                log.error(SwingTree.get().logMarker(),
                    "Error creating fetching alternative UI component! " +
                    "Returning current OptionalUI instead.",
                    e
                );
            }
        }
        return this;
    }

    /**
     * If a component is present, returns the component, otherwise returns
     * {@code other} or throws a null pointer exception if {@code other} is
     * {@code null}.
     *
     * @param other the component to be returned, if no component is present.
     *        May not be {@code null}.
     * @return the component, if present, otherwise {@code other}
     */
    public @Nullable C orElseNullable( @Nullable C other ) {
        if ( !UI.thisIsUIThread() )
            throw new RuntimeException("The UI component may only be accessed by the UI thread!");
        return _component != null ? _component : other;
    }

    /**
     * If a component is present, returns the component, otherwise returns
     * {@code other}.
     *
     * @param other the component to be returned, if no component is present.
     *        May not be {@code null}, use {@link #orElseNullable(Component)} if it can be null.
     * @return the component, if present, otherwise {@code other}
     */
    public C orElse( @NonNull C other ) {
        if ( !UI.thisIsUIThread() )
            throw new RuntimeException("The UI component may only be accessed by the UI thread!");
        Objects.requireNonNull(other);
        return _component != null ? _component : other;
    }

    /**
     *  If a component is present, returns the component, otherwise returns {@code null}.
     *
     * @return The component wrapped in this OptionalUI, or null if no component is present.
     */
    public @Nullable C orNull() {
        if ( !UI.thisIsUIThread() )
            throw new RuntimeException("The UI component may only be accessed by the UI thread!");
        return _component;
    }

    /**
     * If a component is present, returns the component, otherwise returns the result
     * produced by the supplying function.
     *
     * @param supplier the supplying function that produces a component to be returned
     * @return the component, if present, otherwise the result produced by the
     *         supplying function
     * @throws NullPointerException if no component is present and the supplying
     *         function is {@code null}
     */
    public C orElseGet(Supplier<? extends C> supplier) {
        if ( !UI.thisIsUIThread() )
            throw new RuntimeException("The UI component may only be accessed by the UI thread!");
        return _component != null ? _component : supplier.get();
    }

    /**
     * If a component is present, returns the component, otherwise throws
     * {@code NoSuchElementException}.
     *
     * @return the non-{@code null} component described by this {@code OptionalUI}
     * @throws NoSuchElementException if no component is present
     */
    public C orElseThrow() {
        if ( !UI.thisIsUIThread() )
            throw new RuntimeException("The UI component may only be accessed by the UI thread!");
        if ( _component == null )
            throw new NoSuchElementException("No component present");

        return _component;
    }

    /**
     * If a component is present, returns the component, otherwise throws an exception
     * produced by the exception supplying function.
     * <p>
     * Note that
     * A method reference to the exception constructor with an empty argument
     * list can be used as the supplier. For example,
     * {@code IllegalStateException::new}
     *
     * @param <X> Type of the exception to be thrown
     * @param exceptionSupplier the supplying function that produces an
     *        exception to be thrown
     * @return the component, if present
     * @throws X if no component is present
     * @throws NullPointerException if no component is present and the exception
     *          supplying function is {@code null}
     */
    public <X extends Throwable> C orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if ( !UI.thisIsUIThread() )
            throw new RuntimeException("The UI component may only be accessed by the UI thread!");
        if (_component != null) {
            return _component;
        } else {
            throw exceptionSupplier.get();
        }
    }

    /**
     * Indicates whether some other object is "equal to" this {@code OptionalUI}.
     * The other object is considered equal if:
     * <ul>
     * <li>it is also an {@code OptionalUI} and;
     * <li>both instances have no component present or;
     * <li>the present components are "equal to" each other via {@code equals()}.
     * </ul>
     *
     * @param obj an object to be tested for equality
     * @return {@code true} if the other object is "equal to" this object
     *         otherwise {@code false}
     */
    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( !(obj instanceof OptionalUI) ) return false;
        OptionalUI<?> other = (OptionalUI<?>) obj;
        return Objects.equals(_component, other._component);
    }

    /**
     * Returns the hash code of the component, if present, otherwise {@code 0}
     * (zero) if no component is present.
     *
     * @return hash code component of the present component or {@code 0} if no component is
     *         present
     */
    @Override
    public int hashCode() { return Objects.hashCode(_component); }

    /**
     * Returns a non-empty string representation of this {@code OptionalUI}
     * suitable for debugging.  The exact presentation format is unspecified and
     * may vary between implementations and versions.
     * <p>
     * If a component is present the result must include its string representation
     * in the result.  Empty and present {@code OptionalUI}s must be unambiguously
     * differentiable.
     *
     * @return the string representation of this instance
     */
    @Override
    public String toString() {
        return _component != null
                ? String.format("OptionalUI[%s]", _component)
                : "OptionalUI.empty";
    }

}
