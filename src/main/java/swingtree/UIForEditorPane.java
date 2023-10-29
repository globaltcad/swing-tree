package swingtree;

import javax.swing.*;

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
     * @param component The {@link JComponent} type which will be wrapped by this builder node.
     */
    UIForEditorPane( P component ) {
        _state = new BuilderState<>(component);
    }

    @Override
    protected BuilderState<P> _state() {
        return _state;
    }
}
