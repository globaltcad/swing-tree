package swingtree;

import javax.swing.*;

/**
 *  A SwingTree builder node designed for configuring {@link UIForTextPane} instances.
 */
public final class UIForTextPane<P extends JTextPane> extends UIForAnyEditorPane<UIForTextPane<P>, P>
{
    private final BuilderState<P> _state;

    /**
     * {@link UIForAnySwing} (sub)types always wrap
     * a single component for which they are responsible.
     *
     * @param component The {@link JComponent} type which will be wrapped by this builder node.
     */
    UIForTextPane( P component ) {
        _state = new BuilderState<>(component);
    }

    @Override
    protected BuilderState<P> _state() {
        return _state;
    }
}
