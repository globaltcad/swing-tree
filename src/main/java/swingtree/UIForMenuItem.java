package swingtree;


import javax.swing.*;
import java.util.Objects;

/**
 *  A SwingTree builder node designed for configuring {@link JMenuItem} instances.
 */
public final class UIForMenuItem<M extends JMenuItem> extends UIForAnyMenuItem<UIForMenuItem<M>, M>
{
    private final BuilderState<M> _state;

    UIForMenuItem( BuilderState<M> state ) {
        Objects.requireNonNull(state);
        _state = state;
    }

    @Override
    protected BuilderState<M> _state() {
        return _state;
    }
    
    @Override
    protected UIForMenuItem<M> _newBuilderWithState(BuilderState<M> newState ) {
        return new UIForMenuItem<>(newState);
    }
}
