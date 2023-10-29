package swingtree;

import javax.swing.*;

/**
 *  A SwingTree builder node designed for configuring {@link JRadioButton} instances.
 */
public final class UIForRadioButton<R extends JRadioButton> extends UIForAnyToggleButton<UIForRadioButton<R>, R>
{
    private final BuilderState<R> _state;

    UIForRadioButton( R component ) {
        _state = new BuilderState<>(component);
    }

    @Override
    protected BuilderState<R> _state() {
        return _state;
    }
}
