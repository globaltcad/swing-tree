package swingtree;

import swingtree.components.JIcon;

/**
 * A {@link UIForAnySwing} subclass specifically designed for adding icons to your SwingTree.
 */
public final class UIForIcon<I extends JIcon> extends UIForAnySwing<UIForIcon<I>, I>
{
    private final BuilderState<I> _state;

    /**
     * Extensions of the {@link  UIForAnySwing} always wrap
     * a single component for which they are responsible.
     *
     * @param component The JComponent type which will be wrapped by this builder node.
     */
    UIForIcon( I component ) {
        _state = new BuilderState<>(component);
    }

    @Override
    protected BuilderState<I> _state() {
        return _state;
    }
}
