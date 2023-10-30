package swingtree;

import java.awt.*;
import java.util.Objects;

/**
 *  A SwingTree builder node for configuring anything which is a sub-type of {@link Component}
 *  Note: This does not support nesting.
 */
public class UIForAnything<T extends Component> extends AbstractBuilder<UIForAnything<T>, T>
{
    private final BuilderState<T> _state;

    /**
     * Instances of the {@link UIForAnything} builder do not support the
     * "add" method as defined inside the {@link AbstractNestedBuilder}.
     *
     * @param state The {@link BuilderState} modelling the component which will be wrapped by this builder node.
     */
    protected UIForAnything( BuilderState<T> state ) {
        Objects.requireNonNull(state, "state");
        _state = state;
    }

    @Override
    protected BuilderState<T> _state() {
        return _state;
    }
}
