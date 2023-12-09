package swingtree;

import javax.swing.*;
import java.util.Objects;

public final class UIForCheckBoxMenuItem<M extends JCheckBoxMenuItem>
extends UIForAnyMenuItem<UIForCheckBoxMenuItem<M>, M>
{
    private final BuilderState<M> _state;

    UIForCheckBoxMenuItem( BuilderState<M> state ) {
        Objects.requireNonNull(state, "state");
        _state = state;
    }

    @Override
    protected BuilderState<M> _state() {
        return _state;
    }
    
    @Override
    protected UIForCheckBoxMenuItem<M> _newBuilderWithState(BuilderState<M> newState ) {
        return new UIForCheckBoxMenuItem<>(newState);
    }
}
