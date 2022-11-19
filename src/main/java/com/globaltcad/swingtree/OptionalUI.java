/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.globaltcad.swingtree;

import java.awt.*;
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
 * <p>This is a <a href="../lang/doc-files/ValueBased.html">value-based</a>
 * class; use of identity-sensitive operations (including reference equality
 * ({@code ==}), identity hash code, or synchronization) on instances of
 * {@code OptionalUI} may have unpredictable results and should be avoided.
 *
 * @apiNote
 * {@code OptionalUI} is primarily intended for use as a Swing-Tree query return type where
 * there is a clear need to represent "no result," and where returning {@code null} as well
 * as expose the UI components to the application thread directly
 * is likely to cause errors. A variable whose type is {@code OptionalUI} should
 * never itself be {@code null}; it should always point to an {@code OptionalUI}
 * instance.
 *
 * @param <C> the type of component held by this instance
 */
public final class OptionalUI<C extends Component> {
    /**
     * Common instance for {@code empty()}.
     */
    private static final OptionalUI<?> EMPTY = new OptionalUI<>(null);

    /**
     * If non-null, the value; if null, indicates no value is present
     */
    private final C _component;

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
    private OptionalUI(C value) {
        this._component = value;
    }

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
    static <T extends Component> OptionalUI<T> ofNullable(T component) {
        return component == null ? (OptionalUI<T>) EMPTY
                : new OptionalUI<>(component);
    }

    /**
     * If a component is present, returns {@code true}, otherwise {@code false}.
     *
     * @return {@code true} if a component is present, otherwise {@code false}
     */
    public boolean isPresent() {
        return _component != null;
    }

    /**
     * If a component is  not present, returns {@code true}, otherwise
     * {@code false}.
     *
     * @return  {@code true} if a component is not present, otherwise {@code false}
     * @since   11
     */
    public boolean isEmpty() {
        return _component == null;
    }

    /**
     * If a component is present, performs the given action with the component,
     * otherwise does nothing.
     *
     * @param action the action to be performed, if a component is present
     * @throws NullPointerException if component is present and the given action is
     *         {@code null}
     */
    public void ifPresent(Consumer<? super C> action) {
        if ( _component != null ) {
            if ( !UI.thisIsUIThread() )
                UI.run(() -> action.accept(_component));
            else
                action.accept(_component);
        }
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
     * @since 9
     */
    public void ifPresentOrElse(Consumer<? super C> action, Runnable emptyAction) {
        if ( UI.thisIsUIThread() ) {
            if ( _component != null )
                action.accept(_component);
            else
                emptyAction.run();
        }
        else UI.run( () -> ifPresentOrElse(action, emptyAction) );
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
            if ( !UI.thisIsUIThread() ) {
                try {
                    return UI.runAndGet(() -> filter(predicate));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            else
                return predicate.test(_component) ? this : empty();
        }
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
    public <U> Optional<U> map(Function<? super C, ? extends U> mapper) {
        Objects.requireNonNull(mapper);
        if ( !this.isPresent() ) return Optional.empty();
        else {
            if ( UI.thisIsUIThread() )
                return Optional.ofNullable(mapper.apply(_component));
            else {
                try {
                    Optional<U> opt = UI.runAndGet(() -> map(mapper));
                    if ( opt.isPresent() && (opt.get() instanceof Component || opt.get() instanceof UIForAbstractSwing) )
                        throw new RuntimeException("A Swing component may not leak to another thread!");
                    else return opt;
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    /**
     * If a component is present, returns an {@code OptionalUI} describing the component,
     * otherwise returns an {@code OptionalUI} produced by the supplying function.
     * Use this to provide for alternative UI tree querying operations.
     *
     * @param supplier the supplying function that produces an {@code OptionalUI}
     *        to be returned
     * @return returns an {@code OptionalUI} describing the component of this
     *         {@code OptionalUI}, if a component is present, otherwise an
     *         {@code OptionalUI} produced by the supplying function.
     * @throws NullPointerException if the supplying function is {@code null} or
     *         produces a {@code null} result
     * @since 9
     */
    public OptionalUI<C> or(Supplier<? extends OptionalUI<? extends C>> supplier) {
        Objects.requireNonNull(supplier);
        if ( this.isPresent() ) return this;
        else {
            @SuppressWarnings("unchecked")
            OptionalUI<C> r = (OptionalUI<C>) supplier.get();
            return Objects.requireNonNull(r);
        }
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
    public C orElseNullable(C other) {
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
    public C orElse(C other) {
        if ( !UI.thisIsUIThread() )
            throw new RuntimeException("The UI component may only be accessed by the UI thread!");
        Objects.requireNonNull(other);
        return _component != null ? _component : other;
    }

    /**
     * @return The component wrapped in this OptionalUI, or null if no component is present.
     */
    public C orNull() {
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
     * @since 10
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
     *
     * @apiNote
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
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof OptionalUI)) {
            return false;
        }

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
    public int hashCode() {
        return Objects.hashCode(_component);
    }

    /**
     * Returns a non-empty string representation of this {@code OptionalUI}
     * suitable for debugging.  The exact presentation format is unspecified and
     * may vary between implementations and versions.
     *
     * @implSpec
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
