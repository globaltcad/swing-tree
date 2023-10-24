package swingtree;

import net.miginfocom.swing.MigLayout;
import sprouts.Val;
import swingtree.layout.LayoutConstraint;

import javax.swing.*;
import java.awt.*;

/**
 *  A SwingTree builder node designed for configuring {@link JPanel} instances.
 */
public class UIForPanel<P extends JPanel> extends UIForAnySwing<UIForPanel<P>, P>
{
    protected UIForPanel( P component ) { super(component); }

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
        _onShow(attr, it -> {
            // Every time the value changes, we need to re-layout the panel.
            // Note that this is for mig layout:
            LayoutManager lm = getComponent().getLayout();
            if (lm instanceof MigLayout) {
                ((MigLayout)lm).setLayoutConstraints(it.toString());
                getComponent().revalidate();
                getComponent().repaint();
            }
            else
                throw new IllegalStateException(
                        "Cannot set layout mig-layout specific constraints on a panel with a non-mig layout."
                    );
        });
        return _this();
    }

    @Override protected void _setEnabled( P component, boolean isEnabled ) {
        component.setEnabled( isEnabled );
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
        InternalUtil._traverseEnable( component, isEnabled );
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
