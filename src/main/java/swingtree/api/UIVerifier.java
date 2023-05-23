package swingtree.api;

import swingtree.ComponentDelegate;
import swingtree.UIForAnySwing;

import javax.swing.*;
import java.awt.event.ComponentEvent;

/**
 *  A functional interface for doing swing component validity verification.
 *  Passing this to the {@link UIForAnySwing#isValidIf(UIVerifier)} method will
 *  lead to the creation and attachment of a {@link InputVerifier} to the component, which will
 *  be called when the component is validated.
 *
 * @param <C> The component type which should be verified.
 */
@FunctionalInterface
public interface UIVerifier<C extends JComponent>
{
    /**
     * @param delegate The delegate to the component to be verified.
     * @return true if the component is valid, false otherwise.
     */
    boolean isValid( ComponentDelegate<C, ComponentEvent> delegate );
}
