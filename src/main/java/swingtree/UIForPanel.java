package swingtree;

import net.miginfocom.swing.MigLayout;
import sprouts.Val;
import swingtree.layout.LayoutConstraint;

import javax.swing.JPanel;
import java.awt.LayoutManager;
import java.util.Objects;

/**
 *  A SwingTree builder node designed for configuring {@link JPanel} instances.
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

    /**
     *  Use this to dynamically set the {@link MigLayout} attributes of the {@link MigLayout} of the {@link JPanel}.
     *
     * @param attr The layout attributes property which will be dynamically passed to the {@link MigLayout} constructor as first argument.
     * @return A builder instance for a new {@link JPanel}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code attr} is {@code null}.
     */
    public final UIForPanel<P> withLayout( Val<LayoutConstraint> attr ) {
        NullUtil.nullArgCheck(attr, "attr", Val.class);
        NullUtil.nullPropertyCheck(attr, "attr", "Null is not a valid layout attribute.");
        return _withOnShow( attr, (thisComponent,it) -> {
                    // Every time the value changes, we need to re-layout the panel.
                    // Note that this is for mig layout:
                    LayoutManager lm = thisComponent.getLayout();
                    if (lm instanceof MigLayout) {
                        ((MigLayout)lm).setLayoutConstraints(it.toString());
                        thisComponent.revalidate();
                        thisComponent.repaint();
                    }
                    else
                        throw new IllegalStateException(
                                "Cannot set layout mig-layout specific constraints on a panel with a non-mig layout."
                            );
                })
                ._this();
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
