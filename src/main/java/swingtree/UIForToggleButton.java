package swingtree;


import javax.swing.*;
import java.util.Objects;

/**
 *  A SwingTree builder node designed for configuring {@link JToggleButton} instances.
 * 	<p>
 * 	<b>Take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class or other classes.</b>
 *
 * @param <B> The type of the {@link JToggleButton} being built by this builder.
 */
public final class UIForToggleButton<B extends JToggleButton> extends UIForAnyToggleButton<UIForToggleButton<B>, B>
{
    private final BuilderState<B> _state;

    UIForToggleButton( BuilderState<B> state ) {
        Objects.requireNonNull(state);
        _state = state;
    }

    @Override
    protected BuilderState<B> _state() {
        return _state;
    }
    
    @Override
    protected UIForToggleButton<B> _newBuilderWithState(BuilderState<B> newState ) {
        return new UIForToggleButton<>(newState);
    }
}
