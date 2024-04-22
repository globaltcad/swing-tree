package swingtree.api;

import swingtree.ComponentDelegate;
import swingtree.UIForAnySwing;

import javax.swing.*;
import java.awt.event.ComponentEvent;

/**
 *  A functional interface for doing swing component validity verification.
 *  Passing this to the {@link UIForAnySwing#isValidIf(UIVerifier)} method will
 *  lead to the creation and attachment of an {@link InputVerifier} to the component, which will
 *  be called when the component is validated.
 *
 * @param <C> The component type which should be verified.
 */
@FunctionalInterface
public interface UIVerifier<C extends JComponent>
{
    /**
     *  This method is invoked by a UI component to
     *  check if its view model is in a valid state
     *  and then update the component accordingly.
     *  <b>This method should not have any side effects!</b>
     *
     * @param delegate The delegate to the component to be verified.
     * @return true if the component is valid, false otherwise.
     */
    boolean isValid( ComponentDelegate<C, ComponentEvent> delegate );
}
