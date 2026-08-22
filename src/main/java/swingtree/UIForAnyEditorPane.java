package swingtree;

import javax.swing.*;

/**
 * A base builder type for configuring any kind of {@link JEditorPane} component.
 * @param <I> The concrete extension of this class.
 * @param <C> The type of JEditorPane being wrapped.
 */
public abstract class UIForAnyEditorPane<I, C extends JEditorPane> extends UIForAnyTextComponent<I, C>
{
}
