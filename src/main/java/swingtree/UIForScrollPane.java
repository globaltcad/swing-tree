package swingtree;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import java.util.Objects;

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
    UIForScrollPane( BuilderState<P> state ) {
        Objects.requireNonNull(state);
        _state = state;
    }

    @Override
    protected BuilderState<P> _state() {
        return _state;
    }
    
    @Override
    protected UIForScrollPane<P> _with( BuilderState<P> newState ) {
        return new UIForScrollPane<>(newState);
    }
}
