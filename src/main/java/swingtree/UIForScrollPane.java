package swingtree;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

/**
 *  A SwingTree builder node designed for configuring {@link JScrollPane} instances.
 */
public final class UIForScrollPane<P extends JScrollPane> extends UIForAnyScrollPane<UIForScrollPane<P>,P>
{
    private final BuilderState<P> _state;

    /**
     * {@link UIForAnySwing} (sub)types always wrap
     * a single component for which they are responsible.
     *
     * @param component The {@link JComponent} type which will be wrapped by this builder node.
     */
    UIForScrollPane( P component ) {
        _state = new BuilderState<>(component);
    }

    @Override
    protected BuilderState<P> _state() {
        return _state;
    }
}
