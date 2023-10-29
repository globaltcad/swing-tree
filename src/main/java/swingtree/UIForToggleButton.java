package swingtree;


import javax.swing.*;

/**
 *  A SwingTree builder node designed for configuring {@link JToggleButton} instances.
 * 	<p>
 * 	<b>Take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class or other classes.</b>
 */
public final class UIForToggleButton<B extends JToggleButton> extends UIForAnyToggleButton<UIForToggleButton<B>, B>
{
    private final BuilderState<B> _state;

    UIForToggleButton( B component ) {
        _state = new BuilderState<>(component);
    }

    @Override
    protected BuilderState<B> _state() {
        return _state;
    }
}
