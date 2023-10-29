package swingtree;


import javax.swing.*;

/**
 *  A SwingTree builder node designed for configuring {@link JMenuItem} instances.
 */
public final class UIForMenuItem<M extends JMenuItem> extends UIForAnyMenuItem<UIForMenuItem<M>, M>
{
    private final BuilderState<M> _state;

    UIForMenuItem( M component ) {
        _state = new BuilderState<>(component);
    }

    @Override
    protected BuilderState<M> _state() {
        return _state;
    }
}
