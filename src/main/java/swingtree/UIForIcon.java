package swingtree;

import swingtree.components.JIcon;

import java.util.Objects;

/**
 * A {@link UIForAnySwing} subclass specifically designed for adding icons to your SwingTree.
 *
 * @param <I> The type of {@link JIcon} that this {@link UIForIcon} is configuring.
 */
public final class UIForIcon<I extends JIcon> extends UIForAnySwing<UIForIcon<I>, I>
{
    private final BuilderState<I> _state;

    /**
     * Extensions of the {@link  UIForAnySwing} always wrap
     * a single component for which they are responsible.
     *
     * @param state The state of the builder used for creating an icon based component.
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
    protected UIForIcon<I> _newBuilderWithState( BuilderState<I> newState ) {
        return new UIForIcon<>(newState);
    }
}
