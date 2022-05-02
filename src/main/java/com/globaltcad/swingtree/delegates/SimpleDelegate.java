package com.globaltcad.swingtree.delegates;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 *  Instances of this are passed to action lambdas for UI components
 *  to give an action more context information.
 *
 * @param <C> The delegate (in most cases origin UI component) type parameter stored by this.
 * @param <E> The event type parameter of the event stored by this.
 */
public final class SimpleDelegate<C extends JComponent,E>
{
    private final C component;
    private final E event;
    private final Supplier<List<JComponent>> siblingSource;

    public SimpleDelegate(
        C component, E event, Supplier<List<JComponent>> siblingSource
    ) {
        this.component = component;
        this.event = event;
        this.siblingSource = siblingSource;
    }

    /**
     * @return The component for which the current {@link com.globaltcad.swingtree.api.UIAction} originated.
     */
    public C getComponent() { return component; }

    public E getEvent() { return event; }

    /**
     * @return A list of all siblings excluding the component from which this instance originated.
     */
    public List<JComponent> getSiblings() {
        return siblingSource.get().stream().filter( s -> component != s ).collect(Collectors.toList());
    }

    /**
     * @return A list of all siblings including the component from which this instance originated.
     */
    public List<JComponent> getSiblinghood() {
        return new ArrayList<>(siblingSource.get());
    }

}

