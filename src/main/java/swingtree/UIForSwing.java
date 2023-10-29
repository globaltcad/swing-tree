package swingtree;

import javax.swing.*;

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
     * @param component The {@link JComponent} type which will be wrapped by this builder node.
     */
    protected UIForSwing( C component ) {
        _state = new BuilderState<>(component);
    }


    @Override
    protected BuilderState<C> _state() {
        return _state;
    }
}
