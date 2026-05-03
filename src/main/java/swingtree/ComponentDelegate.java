package swingtree;

import sprouts.Action;

import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 *  Instances of this are delegates for a specific components and events that
 *  are passed to user event action handlers (see {@link Action}),
 *  with the purpose of providing useful context information to the action handler.
 *  <br>
 *  You would typically use this to access and change the state of the component, schedule animations
 *  for the component or query the tree of neighboring components. <br>
 *  Here a nice usage example where the delegate is used to animate a button:
 *  <pre>{@code
 *      button("I turn green when you hover over me")
 *      .onMouseEnter( it ->
 *          it.animateFor(0.5, TimeUnit.SECONDS, status -> {
 *              double highlight = 1 - status.progress() * 0.5;
 *              it.setBackgroundColor(highlight, 1, highlight);
 *          })
 *      )
 *      .onMouseExit( it ->
 *          it.animateFor(0.5, TimeUnit.SECONDS, status -> {
 *              double highlight = 0.5 + status.progress() * 0.5;
 *              it.setBackgroundColor(highlight, 1, highlight);
 *          })
 *      )
 *  }</pre>
 *  In this example the {@code it} parameter is a {@code ComponentDelegate<JButton,MouseEvent>}
 *  which can be used to access/modify the button, the event, the sibling components...
 *  ...but also exposes a nice API to schedule animations for the button.
 * 	<p>
 * 	For some more examples <b>please take a look at the
 * 	<a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 *
 * @param <C> The delegate (in most cases origin UI component) type parameter stored by this.
 * @param <E> The event type parameter of the event stored by this.
 */
public class ComponentDelegate<C extends JComponent, E> extends AbstractDelegate<C>
{
    private final E _event;

    /**
     *  Creates a new delegate for the specified component and event.
     *
     * @param component The component for which the delegate is created.
     * @param event The event that represents the action that was triggered either by the user or by the system.
     */
    public ComponentDelegate( C component, E event ) {
        super(false, component, component);
        _event = Objects.requireNonNull(event);
    }

    /**
     *  Exposes the underlying {@link JComponent} from which this delegate
     *  and user event actions originate.
     *  <p>
     *  <b>Threading:</b> A {@link JComponent} is owned by the GUI thread (the AWT
     *  Event Dispatch Thread). Under a non-coupled threading model
     *  (see {@link swingtree.threading.EventProcessor}), event actions may be invoked
     *  on the application thread rather than on the EDT. To prevent unsafe access
     *  to Swing state, this method <b>requires the calling thread to be the GUI
     *  thread</b> and throws an {@link IllegalStateException} otherwise.
     *  <p>
     *  If you need to access the component from the application thread,
     *  use {@link #forComponent(Consumer)} instead, which will safely dispatch
     *  the supplied lambda to the GUI thread for you.
     *
     * @return The component for which the current {@link Action} originated.
     * @throws IllegalStateException If this method is called from a non-Swing thread.
     * @see #forComponent(Consumer)
     */
    public final C getComponent() {
        // We make sure that only the Swing thread can access the component:
        if ( UI.thisIsUIThread() )
            return _component();
        else
            throw new IllegalStateException(
                    "Component can only be accessed by the Swing thread. " +
                    "Use 'forComponent(Consumer)' to access the component from the application thread."
                );
    }

    /**
     *  Provides a thread-safe way to access the underlying {@link JComponent} of this
     *  delegate by passing a {@link Consumer} lambda which will always be executed on
     *  the GUI thread (the AWT Event Dispatch Thread).
     *  <p>
     *  This is the recommended counterpart to {@link #getComponent()} when the calling
     *  code may run on the application thread (e.g. under a decoupled
     *  {@link swingtree.threading.EventProcessor}). If the current thread is already
     *  the GUI thread, the lambda is executed immediately; otherwise it is dispatched
     *  via {@link UI#run(Runnable)}.
     *  <p>
     *  Example:
     *  <pre>{@code
     *      it.forComponent( button -> button.setText("Updated on the EDT") );
     *  }</pre>
     *
     * @param action The action consuming the component,
     *               which will be executed by the Swing thread.
     * @see #getComponent()
     */
    public final void forComponent( Consumer<C> action ) {
        if ( UI.thisIsUIThread() )
            action.accept(_component());
        else
            UI.run( () -> action.accept(_component()) );
    }

    /**
     *  Exposes the event that represents the action that was triggered
     *  either by the user or by the system.
     *
     * @return An object holding relevant information about an event that was triggered.
     */
    public final E getEvent() { return _event; }

    /**
     *  Exposes the "siblings", which consist of all the child components
     *  of the parent of the delegated component, <b>except for the delegated
     *  component itself</b>.
     *  <p>
     *  <b>Threading:</b> Sibling components are part of the Swing component
     *  tree, which is owned by the GUI thread (the AWT Event Dispatch Thread).
     *  This method <b>requires the calling thread to be the GUI thread</b> and
     *  throws an {@link IllegalStateException} otherwise. Under a non-coupled
     *  {@link swingtree.threading.EventProcessor}, event actions are usually
     *  invoked on the application thread, in which case you must use
     *  {@link #forSiblings(Consumer)} instead, which dispatches the supplied
     *  lambda to the GUI thread for you.
     *
     * @return A list of all siblings excluding the component from which this instance originated.
     * @throws IllegalStateException If this method is called from a non-Swing thread.
     * @see #forSiblings(Consumer)
     */
    public final List<JComponent> getSiblings() {
        // We make sure that only the Swing thread can access the sibling components:
        if ( !UI.thisIsUIThread() )
            throw new IllegalStateException(
                    "Sibling components can only be accessed by the Swing thread. " +
                    "Please use 'forSiblings(..)' methods instead."
                );
        return _siblingsSource().stream().filter( s -> _component() != s ).collect(Collectors.toList());
    }

    /**
     *  Provides a thread-safe way to access the sibling components of this delegate
     *  by passing a {@link Consumer} lambda which will always be executed on the GUI
     *  thread (the AWT Event Dispatch Thread).
     *  <p>
     *  This is the recommended counterpart to {@link #getSiblings()} when the calling
     *  code may run on the application thread (e.g. under a decoupled
     *  {@link swingtree.threading.EventProcessor}). If the current thread is already
     *  the GUI thread, the lambda is executed immediately; otherwise it is dispatched
     *  via {@link UI#run(Runnable)}.
     *
     * @param action The action consuming a list of all siblings (excluding the
     *               component from which this instance originated),
     *               which will be executed by the Swing thread.
     * @see #getSiblings()
     */
    public final void forSiblings( Consumer<List<JComponent>> action ) {
        if ( UI.thisIsUIThread() )
            action.accept(getSiblings());
        else
            UI.run( () -> action.accept(getSiblings()) );
    }

    /**
     *  Allows you to query the sibling components of the delegated component
     *  of the specified type. So a list of all siblings which are of the specified type
     *  will be returned, <b>excluding</b> the currently delegated component itself.
     *  <p>
     *  <b>Threading:</b> Sibling components are part of the Swing component tree,
     *  which is owned by the GUI thread (the AWT Event Dispatch Thread). This method
     *  <b>requires the calling thread to be the GUI thread</b> and throws an
     *  {@link IllegalStateException} otherwise. Under a non-coupled
     *  {@link swingtree.threading.EventProcessor}, event actions are usually invoked
     *  on the application thread, in which case you must use
     *  {@link #forSiblingsOfType(Class, Consumer)} instead, which dispatches the
     *  supplied lambda to the GUI thread for you.
     *
     * @param type The type class of the sibling components to return.
     * @param <T> The type of the sibling components to return.
     * @return A list of all siblings of the specified type, excluding the component from which this instance originated.
     * @throws IllegalStateException If this method is called from a non-Swing thread.
     * @see #forSiblingsOfType(Class, Consumer)
     */
    public final <T extends JComponent> List<T> getSiblingsOfType(Class<T> type) {
        // We make sure that only the Swing thread can access the sibling components:
        if ( !UI.thisIsUIThread() )
            throw new IllegalStateException(
                    "Sibling components can only be accessed by the Swing thread. " +
                    "Please use 'forSiblingsOfType(..)' methods instead."
            );
        return getSiblings()
                .stream()
                .filter( s -> type.isAssignableFrom(s.getClass()) )
                .map( s -> (T) s )
                .collect(Collectors.toList());
    }

    /**
     *  Provides a thread-safe way to access the sibling components of this delegate
     *  filtered by the given type by passing a {@link Consumer} lambda which will
     *  always be executed on the GUI thread (the AWT Event Dispatch Thread).
     *  <p>
     *  This is the recommended counterpart to {@link #getSiblingsOfType(Class)} when
     *  the calling code may run on the application thread (e.g. under a decoupled
     *  {@link swingtree.threading.EventProcessor}). If the current thread is already
     *  the GUI thread, the lambda is executed immediately; otherwise it is dispatched
     *  via {@link UI#run(Runnable)}.
     *
     * @param type The type class of the sibling components to return.
     * @param <T> The {@link JComponent} type of the sibling components to return.
     * @param action The action consuming a list of all siblings of the specified type,
     *               excluding the component from which this instance originated,
     *               which will be executed by the Swing thread.
     * @see #getSiblingsOfType(Class)
     */
    public final <T extends JComponent> void forSiblingsOfType(
        Class<T> type, Consumer<List<T>> action
    ) {
        if ( UI.thisIsUIThread() )
            action.accept(getSiblingsOfType(type));
        else
            UI.run( () -> action.accept(getSiblingsOfType(type)) );
    }

    /**
     *  This method provides a convenient way to access all the children of the parent
     *  component of the component this delegate is for, <b>including the delegated
     *  component itself</b>.
     *  <p>
     *  <b>Threading:</b> Sibling components are part of the Swing component tree,
     *  which is owned by the GUI thread (the AWT Event Dispatch Thread). This method
     *  <b>requires the calling thread to be the GUI thread</b> and throws an
     *  {@link IllegalStateException} otherwise. Under a non-coupled
     *  {@link swingtree.threading.EventProcessor}, event actions are usually invoked
     *  on the application thread, in which case you must use
     *  {@link #forSiblinghood(Consumer)} instead, which dispatches the supplied
     *  lambda to the GUI thread for you.
     *
     * @return A list of all siblings including the component from which this instance originated.
     * @throws IllegalStateException If this method is called from a non-Swing thread.
     * @see #forSiblinghood(Consumer)
     */
    public final List<JComponent> getSiblinghood() {
        // We make sure that only the Swing thread can access the sibling components:
        if ( !UI.thisIsUIThread() )
            throw new IllegalStateException(
                    "Sibling components can only be accessed by the Swing thread. " +
                    "Please use 'forSiblinghood(..)' methods instead."
            );
        return new ArrayList<>(_siblingsSource());
    }

    /**
     *  Provides a thread-safe way to access the entire siblinghood of this delegate
     *  (the delegated component <i>and</i> its siblings) by passing a {@link Consumer}
     *  lambda which will always be executed on the GUI thread (the AWT Event Dispatch
     *  Thread).
     *  <p>
     *  This is the recommended counterpart to {@link #getSiblinghood()} when the
     *  calling code may run on the application thread (e.g. under a decoupled
     *  {@link swingtree.threading.EventProcessor}). If the current thread is already
     *  the GUI thread, the lambda is executed immediately; otherwise it is dispatched
     *  via {@link UI#run(Runnable)}.
     *
     * @param action The action consuming a list of all siblings (including the
     *               component from which this instance originated),
     *               which will be executed by the Swing thread.
     * @see #getSiblinghood()
     */
    public final void forSiblinghood( Consumer<List<JComponent>> action ) {
        if ( UI.thisIsUIThread() )
            action.accept(getSiblinghood());
        else
            UI.run( () -> action.accept(getSiblinghood()) );
    }

    /**
     *  Allows you to query the entire siblinghood of the delegated component
     *  filtered by the specified type. So a list of all siblings which are of the
     *  specified type will be returned, <b>possibly including the delegated component
     *  itself</b>.
     *  <p>
     *  <b>Threading:</b> Sibling components are part of the Swing component tree,
     *  which is owned by the GUI thread (the AWT Event Dispatch Thread). This method
     *  <b>requires the calling thread to be the GUI thread</b> and throws an
     *  {@link IllegalStateException} otherwise. Under a non-coupled
     *  {@link swingtree.threading.EventProcessor}, event actions are usually invoked
     *  on the application thread, in which case you must use
     *  {@link #forSiblinghoodOfType(Class, Consumer)} instead, which dispatches the
     *  supplied lambda to the GUI thread for you.
     *
     * @param type The type of the sibling components to return.
     * @param <T> The {@link JComponent} type of the sibling components to return.
     * @return A list of all siblings of the specified type, including the component from which this instance originated.
     * @throws IllegalStateException If this method is called from a non-Swing thread.
     * @see #forSiblinghoodOfType(Class, Consumer)
     */
    public final <T extends JComponent> List<T> getSiblinghoodOfType(Class<T> type) {
        // We make sure that only the Swing thread can access the sibling components:
        if ( !UI.thisIsUIThread() )
            throw new IllegalStateException(
                "Sibling components can only be accessed by the Swing thread. " +
                "Please use 'forSiblinghoodOfType(..)' methods instead."
            );
        return new ArrayList<>(_siblingsSource())
                .stream()
                .filter( s -> type.isAssignableFrom(s.getClass()) )
                .map( s -> (T) s )
                .collect(Collectors.toList());
    }

    /**
     *  Provides a thread-safe way to access the entire siblinghood of this delegate
     *  (including the delegated component itself) filtered by the given type, by
     *  passing a {@link Consumer} lambda which will always be executed on the GUI
     *  thread (the AWT Event Dispatch Thread).
     *  <p>
     *  This is the recommended counterpart to {@link #getSiblinghoodOfType(Class)}
     *  when the calling code may run on the application thread (e.g. under a
     *  decoupled {@link swingtree.threading.EventProcessor}). If the current thread
     *  is already the GUI thread, the lambda is executed immediately; otherwise it
     *  is dispatched via {@link UI#run(Runnable)}.
     *
     * @param type The type of the sibling components to return.
     * @param <T> The {@link JComponent} type of the sibling components to return.
     * @param action The action consuming a list of all siblings of the specified type,
     *               including the component from which this instance originated,
     *               which will be executed by the Swing thread.
     * @see #getSiblinghoodOfType(Class)
     */
    public final <T extends JComponent> void forSiblinghoodOfType(
        Class<T> type, Consumer<List<T>> action
    ) {
        if ( UI.thisIsUIThread() )
            action.accept(getSiblinghoodOfType(type));
        else
            UI.run( () -> action.accept(getSiblinghoodOfType(type)) );
    }

}

