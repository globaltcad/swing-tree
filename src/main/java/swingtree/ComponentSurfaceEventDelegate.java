package swingtree;

import sprouts.Action;

import javax.swing.*;
import java.awt.event.MouseEvent;


/**
 * A {@link JComponent}} and {@link  MouseEvent} delegate that provides useful context information to various
 * {@link sprouts.Action} listeners registered through {@link UIForAnySwing#onMouseEnter(Action)}} and
 * {@link UIForAnySwing#onMouseExit(Action)}.
 * <p>
 * The {@link ComponentSurfaceEventDelegate#getSource()} method returns the {@link MouseEvent#getSource()} of the root
 * {@link MouseEvent} that triggered the action.
 */
public final class ComponentSurfaceEventDelegate<C extends JComponent> extends ComponentMouseEventDelegate<C> {
    private final Object _source;

    /**
     * Constructs a new {@code ComponentSurfaceEventDelegate}.
     *
     * @param component The component on which the event occurred.
     * @param source    The source of the root {@link MouseEvent} that triggered this delegate.
     * @param event     The local {@link MouseEvent} that triggered this delegate.
     */
    ComponentSurfaceEventDelegate(
            C component,
            Object source,
            MouseEvent event
    ) {
        super(component, event);
        _source = source;
    }

    /**
     * Returns the {@link MouseEvent#getSource()} of the root {@link MouseEvent}.
     *
     * @return The source associated with this delegate.
     */
    public Object getSource() {
        return _source;
    }

    /**
     * Determines if the current component is the {@link MouseEvent#getSource()} of the root {@link MouseEvent}.
     *
     * @return {@code true} if the component associated with this delegate is the source, {@code false} otherwise.
     */
    public boolean isSource() {
        return _component() == _source;
    }

}
