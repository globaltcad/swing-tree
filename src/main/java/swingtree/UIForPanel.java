package swingtree;

import javax.swing.JPanel;
import java.util.Objects;

/**
 *  A SwingTree builder node designed for configuring {@link JPanel} instances.
 *
 * @param <P> The type of {@link JPanel} that this {@link UIForPanel} is configuring.
 */
public final class UIForPanel<P extends JPanel> extends UIForAnySwing<UIForPanel<P>, P>
{
    private final BuilderState<P> _state;

    UIForPanel( BuilderState<P> state ) {
        Objects.requireNonNull(state);
        _state = state;
    }

    @Override
    protected BuilderState<P> _state() {
        return _state;
    }
    
    @Override
    protected UIForPanel<P> _newBuilderWithState(BuilderState<P> newState ) {
        return new UIForPanel<>(newState);
    }

    @Override protected void _setEnabled( P thisComponent, boolean isEnabled ) {
        thisComponent.setEnabled( isEnabled );
        /*
            In the vast vast majority of cases regular JPanels are simple wrappers for
            other components.
            They are mostly used to group things together, provide a border and
            position them using a fancy layout manager.
            Disabling only a JPanel is therefore not very useful, because it will not
            disable the components it contains.
            So what we want to do here is traverse the tree of JPanel instances
            and disable all the components that are contained in the tree
            except for the children of non JPanels.
        */
        InternalUtil._traverseEnable( thisComponent, isEnabled );
        /*
            Note:
            If you really only want to disable the JPanel itself, then you can
            simply peek into the tree and disable the JPanel directly.
            Or, a better idea, is to simply change color and event handling
            of the JPanel to make it look and behave like a disabled component,
            but still allow the user to interact with the components it contains.
         */
    }
}
