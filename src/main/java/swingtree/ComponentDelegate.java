package swingtree;

import sprouts.Action;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
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
 *          it.animateFor(0.5, TimeUnit.SECONDS, state -> {
 *              double highlight = 1 - state.progress() * 0.5;
 *              it.setBackgroundColor(highlight, 1, highlight);
 *          })
 *      )
 *      .onMouseExit( it ->
 *          it.animateFor(0.5, TimeUnit.SECONDS, state -> {
 *              double highlight = 0.5 + state.progress() * 0.5;
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
    private final E event;
    private final Supplier<List<JComponent>> siblingSource;

    public ComponentDelegate(
        C component, E event, Supplier<List<JComponent>> siblingSource
    ) {
        super(component, component);
        this.event         = Objects.requireNonNull(event);
        this.siblingSource = Objects.requireNonNull(siblingSource);
    }

    /**
     * @return The component for which the current {@link Action} originated.
     */
    public final C getComponent() {
        // We make sure that only the Swing thread can access the component:
        if ( UI.thisIsUIThread() ) return _component();
        else
            throw new IllegalStateException(
                    "Component can only be accessed by the Swing thread."
                );
    }

    /**
     *  Use this to access the component of this delegate in the swing thread.
     *  This method will make sure that the passed lambda will
     *  be executed by the Swing thread.
     *  <br><br>
     * @param action The action consuming the component,
     *               which will be executed by the Swing thread.
     */
    public final void forComponent( Consumer<C> action ) {
        if ( UI.thisIsUIThread() )
            action.accept(_component());
        else
            UI.run( () -> action.accept(_component()) );
    }

    public final E getEvent() { return event; }

    /**
     * @return A list of all siblings excluding the component from which this instance originated.
     */
    public final List<JComponent> getSiblings() {
        // We make sure that only the Swing thread can access the sibling components:
        if ( !UI.thisIsUIThread() )
            throw new IllegalStateException(
                    "Sibling components can only be accessed by the Swing thread. " +
                    "Please use 'forSiblings(..)' methods instead."
                );
        return siblingSource.get().stream().filter( s -> _component() != s ).collect(Collectors.toList());
    }
    
    /**
     *  Use this to access the sibling components of this delegate in the swing thread.
     *  This method will make sure that the passed lambda will
     *  be executed by the Swing thread.
     *  <br><br>
     * @param action The action consuming a list of all siblings (excluding the
     *               component from which this instance originated),
     *               which will be executed by the Swing thread.
     */
    public final void forSiblings( Consumer<List<JComponent>> action ) {
        if ( UI.thisIsUIThread() )
            action.accept(getSiblings());
        else
            UI.run( () -> action.accept(getSiblings()) );
    }

    /**
     * @param type The type class of the sibling components to return.
     * @param <T> The type of the sibling components to return.
     * @return A list of all siblings of the specified type, excluding the component from which this instance originated.
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
     *  Use this to access the sibling components of this delegate
     *  of the specified type in the swing thread.
     *  This method will make sure that the passed lambda will
     *  be executed by the Swing thread.
     *  <br><br>
     * @param type The type class of the sibling components to return.
     * @param <T> The {@link JComponent} type of the sibling components to return.
     * @param action The action consuming a list of all siblings of the specified type,
     *               excluding the component from which this instance originated,
     *               which will be executed by the Swing thread.
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
     * @return A list of all siblings including the component from which this instance originated.
     */
    public final List<JComponent> getSiblinghood() {
        // We make sure that only the Swing thread can access the sibling components:
        if ( !UI.thisIsUIThread() )
            throw new IllegalStateException(
                    "Sibling components can only be accessed by the Swing thread. " +
                    "Please use 'forSiblinghood(..)' methods instead."
            );
        return new ArrayList<>(siblingSource.get());
    }

    /**
     *  Use this to access the sibling components of this delegate in the swing thread.
     *  This method will make sure that the passed lambda will
     *  be executed by the Swing thread.
     *  <br><br>
     * @param action The action consuming a list of all siblings (including the
     *               component from which this instance originated),
     *               which will be executed by the Swing thread.
     */
    public final void forSiblinghood( Consumer<List<JComponent>> action ) {
        if ( UI.thisIsUIThread() )
            action.accept(getSiblinghood());
        else
            UI.run( () -> action.accept(getSiblinghood()) );
    }

    /**
     * @param type The type of the sibling components to return.
     * @param <T> The {@link JComponent} type of the sibling components to return.
     * @return A list of all siblings of the specified type, including the component from which this instance originated.
     */
    public final <T extends JComponent> List<T> getSiblinghoodOfType(Class<T> type) {
        // We make sure that only the Swing thread can access the sibling components:
        if ( !UI.thisIsUIThread() )
            throw new IllegalStateException(
                "Sibling components can only be accessed by the Swing thread. " +
                "Please use 'forSiblinghoodOfType(..)' methods instead."
            );
        return new ArrayList<>(siblingSource.get())
                .stream()
                .filter( s -> type.isAssignableFrom(s.getClass()) )
                .map( s -> (T) s )
                .collect(Collectors.toList());
    }

    /**
     *  Use this to access all sibling components (including the one represented by this delegate)
     *  of the specified type in the swing thread.
     *  This method will make sure that the passed lambda will
     *  be executed by the Swing thread.
     *  <br><br>
     * @param type The type of the sibling components to return.
     * @param <T> The {@link JComponent} type of the sibling components to return.
     * @param action The action consuming a list of all siblings of the specified type,
     *               including the component from which this instance originated,
     *               which will be executed by the Swing thread.
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

