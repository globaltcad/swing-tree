package swingtree;

import javax.swing.*;
import java.util.Objects;

/**
 *  A SwingTree builder node designed for configuring {@link JTextArea} instances.
 */
public final class UIForTextArea<A extends JTextArea> extends UIForAnyTextComponent<UIForTextArea<A>, A>
{
    private final BuilderState<A> _state;

    UIForTextArea( BuilderState<A> state ) {
        Objects.requireNonNull(state);
        _state = state;
    }

    @Override
    protected BuilderState<A> _state() {
        return _state;
    }
}
