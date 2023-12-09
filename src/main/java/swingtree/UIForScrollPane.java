package swingtree;

import swingtree.components.JScrollPanels;
import swingtree.components.listener.NestedJScrollPanelScrollCorrection;

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
     * @param state The {@link BuilderState} modelling how the underlying component is build.
     */
    UIForScrollPane( BuilderState<P> state ) {
        Objects.requireNonNull(state);
        _state = state.withMutator(thisComponent -> {
           if ( !(thisComponent instanceof UI.ScrollPane) && !(thisComponent instanceof JScrollPanels) )
               thisComponent.addMouseWheelListener(new NestedJScrollPanelScrollCorrection(thisComponent));
        });
    }

    @Override
    protected BuilderState<P> _state() {
        return _state;
    }
    
    @Override
    protected UIForScrollPane<P> _newBuilderWithState(BuilderState<P> newState ) {
        return new UIForScrollPane<>(newState);
    }
}
