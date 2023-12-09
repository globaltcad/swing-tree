package swingtree;

import swingtree.components.JIcon;

import java.util.Objects;

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
    UIForIcon( BuilderState<I> state ) {
        Objects.requireNonNull(state);
        _state = state;
    }

    @Override
    protected BuilderState<I> _state() {
        return _state;
    }
    
    @Override
    protected UIForIcon<I> _newBuilderWithState(BuilderState<I> newState ) {
        return new UIForIcon<>(newState);
    }
}
