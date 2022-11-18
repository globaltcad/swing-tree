package com.globaltcad.swingtree;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 *  Instances of this are passed to action lambdas for UI components
 *  to give an action more context information.
 *
 * @param <C> The delegate (in most cases origin UI component) type parameter stored by this.
 * @param <E> The event type parameter of the event stored by this.
 */
public final class SimpleDelegate<C extends JComponent,E> extends AbstractDelegate
{
    private final C component;
    private final E event;
    private final Supplier<List<JComponent>> siblingSource;

    public SimpleDelegate(
        C component, E event, Supplier<List<JComponent>> siblingSource
    ) {
        super(component);
        this.component = component;
        this.event = event;
        this.siblingSource = siblingSource;
    }

    /**
     * @return The component for which the current {@link com.globaltcad.swingtree.api.UIAction} originated.
     */
    public C getComponent() {
        // We make sure that only the Swing thread can access the component:
        if ( SwingUtilities.isEventDispatchThread() ) return component;
        else
            throw new IllegalStateException(
                    "Component can only be accessed from the Swing thread."
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
    public void forComponent( Consumer<C> action ) {
        if ( SwingUtilities.isEventDispatchThread() )
            action.accept(component);
        else
            UI.run( () -> action.accept(component) );
    }

    public E getEvent() { return event; }

    /**
     * @return A list of all siblings excluding the component from which this instance originated.
     */
    public List<JComponent> getSiblings() {
        // We make sure that only the Swing thread can access the sibling components:
        if ( !SwingUtilities.isEventDispatchThread() )
            throw new IllegalStateException(
                    "Sibling components can only be accessed from the Swing thread."
                );
        return siblingSource.get().stream().filter( s -> component != s ).collect(Collectors.toList());
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
    public void forSiblings( Consumer<List<JComponent>> action ) {
        if ( SwingUtilities.isEventDispatchThread() )
            action.accept(getSiblings());
        else
            UI.run( () -> action.accept(getSiblings()) );
    }

    /**
     * @param type The type of the sibling components to return.
     * @return A list of all siblings of the specified type, excluding the component from which this instance originated.
     */
    public <T extends JComponent> List<T> getSiblingsOfType(Class<T> type) {
        // We make sure that only the Swing thread can access the sibling components:
        if ( !SwingUtilities.isEventDispatchThread() )
            throw new IllegalStateException(
                    "Sibling components can only be accessed from the Swing thread."
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
     * @param type The type of the sibling components to return.
     * @param action The action consuming a list of all siblings of the specified type,
     *               excluding the component from which this instance originated,
     *               which will be executed by the Swing thread.
     */
    public <T extends JComponent> void forSiblingsOfType(
        Class<T> type, Consumer<List<T>> action
    ) {
        if ( SwingUtilities.isEventDispatchThread() )
            action.accept(getSiblingsOfType(type));
        else
            UI.run( () -> action.accept(getSiblingsOfType(type)) );
    }

    /**
     * @return A list of all siblings including the component from which this instance originated.
     */
    public List<JComponent> getSiblinghood() {
        // We make sure that only the Swing thread can access the sibling components:
        if ( !SwingUtilities.isEventDispatchThread() )
            throw new IllegalStateException(
                    "Sibling components can only be accessed from the Swing thread."
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
    public void forSiblinghood( Consumer<List<JComponent>> action ) {
        if ( SwingUtilities.isEventDispatchThread() )
            action.accept(getSiblinghood());
        else
            UI.run( () -> action.accept(getSiblinghood()) );
    }

    /**
     * @param type The type of the sibling components to return.
     * @return A list of all siblings of the specified type, including the component from which this instance originated.
     */
    public <T extends JComponent> List<T> getSiblinghoodOfType(Class<T> type) {
        // We make sure that only the Swing thread can access the sibling components:
        if ( !SwingUtilities.isEventDispatchThread() )
            throw new IllegalStateException(
                "Sibling components can only be accessed from the Swing thread."
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
     * @param action The action consuming a list of all siblings of the specified type,
     *               including the component from which this instance originated,
     *               which will be executed by the Swing thread.
     */
    public <T extends JComponent> void forSiblinghoodOfType(
        Class<T> type, Consumer<List<T>> action
    ) {
        if ( SwingUtilities.isEventDispatchThread() )
            action.accept(getSiblinghoodOfType(type));
        else
            UI.run( () -> action.accept(getSiblinghoodOfType(type)) );
    }

}

