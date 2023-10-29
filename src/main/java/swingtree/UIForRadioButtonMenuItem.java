package swingtree;

import javax.swing.*;

/**
 *  A SwingTree builder node designed for configuring {@link JRadioButtonMenuItem} instances.
 *  <p>
 *  This class is a {@link UIForAnyMenuItem} subtype, and as such, it inherits all of the
 *  builder methods from that class.
 */
public final class UIForRadioButtonMenuItem<M extends JRadioButtonMenuItem>
extends UIForAnyMenuItem<UIForRadioButtonMenuItem<M>, M>
{
    private final BuilderState<M> _state;

    UIForRadioButtonMenuItem(M component) {
        _state = new BuilderState<>(component);
    }

    @Override
    protected BuilderState<M> _state() {
        return _state;
    }
}
