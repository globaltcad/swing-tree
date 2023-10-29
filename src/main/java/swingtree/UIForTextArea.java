package swingtree;

import javax.swing.*;

/**
 *  A SwingTree builder node designed for configuring {@link JTextArea} instances.
 */
public final class UIForTextArea<A extends JTextArea> extends UIForAnyTextComponent<UIForTextArea<A>, A>
{
    private final BuilderState<A> _state;

    UIForTextArea( A component ) {
        _state = new BuilderState<>(component);
    }

    @Override
    protected BuilderState<A> _state() {
        return _state;
    }
}
