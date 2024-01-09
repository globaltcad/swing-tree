package swingtree;

import javax.swing.*;
import java.util.Objects;

/**
 *  A SwingTree builder node designed for configuring {@link JEditorPane} instances.
 * 	<p>
 * 	<b>Take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class or other classes.</b>
 */
public final class UIForEditorPane<P extends JEditorPane> extends UIForAnyEditorPane<UIForEditorPane<P>, P>
{
    private final BuilderState<P> _state;

    /**
     * {@link UIForAnySwing} (sub)types always wrap
     * a single component for which they are responsible.
     *
     * @param state The {@link BuilderState} containing the component which will be managed by this builder.
     */
    UIForEditorPane( BuilderState<P> state ) {
        Objects.requireNonNull(state, "state");
        _state = state;
    }

    @Override
    protected BuilderState<P> _state() {
        return _state;
    }
    
    @Override
    protected UIForEditorPane<P> _newBuilderWithState(BuilderState<P> newState ) {
        return new UIForEditorPane<>(newState);
    }
}
