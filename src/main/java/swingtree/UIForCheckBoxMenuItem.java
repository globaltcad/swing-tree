package swingtree;

import javax.swing.*;

public final class UIForCheckBoxMenuItem<M extends JCheckBoxMenuItem>
extends UIForAnyMenuItem<UIForCheckBoxMenuItem<M>, M>
{
    private final BuilderState<M> _state;

    protected UIForCheckBoxMenuItem( M component ) {
        _state = new BuilderState<>(component);
    }

    @Override
    protected BuilderState<M> _state() {
        return _state;
    }
}
