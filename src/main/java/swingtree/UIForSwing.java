package swingtree;

import javax.swing.*;
import java.util.Objects;

/**
 *  A SwingTree builder node designed for configuring {@link JComponent} types.
 */
public class UIForSwing<C extends JComponent> extends UIForAnySwing<UIForSwing<C>, C>
{
    private final BuilderState<C> _state;

    /**
     * {@link UIForAnySwing} (sub)types always wrap
     * a single component for which they are responsible.
     *
     * @param state The {@link BuilderState} modelling how the component is built.
     */
    protected UIForSwing( BuilderState<C> state ) {
        Objects.requireNonNull(state);
        _state = state;
    }


    @Override
    protected BuilderState<C> _state() {
        return _state;
    }
    
    @Override
    protected UIForSwing<C> _with( BuilderState<C> newState ) {
        return new UIForSwing<>(newState);
    }
}
