package swingtree;

import javax.swing.*;
import java.util.Objects;

/**
 *  A declarative SwingTree builder designed for configuring {@link UIForTextPane} instances.
 *
 * @param <P> The type of {@link JTextPane} that this {@link UIForTextPane} configures.
 */
public final class UIForTextPane<P extends JTextPane> extends UIForAnyEditorPane<UIForTextPane<P>, P>
{
    private final BuilderState<P> _state;

    /**
     * {@link UIForAnySwing} (sub)types always wrap
     * a single component for which they are responsible.
     *
     * @param state The {@link BuilderState} modelling how the component is built.
     */
    UIForTextPane( BuilderState<P> state ) {
        Objects.requireNonNull(state);
        _state = state;
    }

    @Override
    protected BuilderState<P> _state() {
        return _state;
    }
    
    @Override
    protected UIForTextPane<P> _newBuilderWithState(BuilderState<P> newState ) {
        return new UIForTextPane<>(newState);
    }
}
