package swingtree;

import javax.swing.*;
import java.util.Objects;

/**
 *  A SwingTree builder node designed for configuring {@link JTextArea} instances.
 *
 * @param <A> The type of {@link JTextArea} that this {@link UIForTextArea} is configuring.
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
    
    @Override
    protected UIForTextArea<A> _newBuilderWithState(BuilderState<A> newState ) {
        return new UIForTextArea<>(newState);
    }
}
