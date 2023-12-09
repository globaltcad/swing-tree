package swingtree;

import javax.swing.*;
import java.util.Objects;

/**
 *  A SwingTree builder node designed for configuring {@link JRadioButton} instances.
 */
public final class UIForRadioButton<R extends JRadioButton> extends UIForAnyToggleButton<UIForRadioButton<R>, R>
{
    private final BuilderState<R> _state;

    UIForRadioButton( BuilderState<R> state ) {
        Objects.requireNonNull(state);
        _state = state;
    }

    @Override
    protected BuilderState<R> _state() {
        return _state;
    }
    
    @Override
    protected UIForRadioButton<R> _newBuilderWithState(BuilderState<R> newState ) {
        return new UIForRadioButton<>(newState);
    }
}
