package swingtree;

import net.miginfocom.swing.MigLayout;
import sprouts.Val;
import swingtree.layout.LayoutAttr;

import javax.swing.*;
import java.awt.*;

/**
 *  A swing tree builder node for {@link JPanel} instances.
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
    public final UIForPanel<P> withLayout( Val<LayoutAttr> attr ) {
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

    @Override protected void _setEnabled( boolean isEnabled ) {
        getComponent().setEnabled( isEnabled );
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
        _traverseEnable( getComponent(), isEnabled );
        /*
            Note:
            If you really only want to disable the JPanel itself, then you can
            simply peek into the tree and disable the JPanel directly.
            Or, a better idea, is to simply change color and event handling
            of the JPanel to make it look and behave like a disabled component,
            but still allow the user to interact with the components it contains.
         */
    }

    private void _traverseEnable( Component c, boolean isEnabled ) {
        c.setEnabled( isEnabled );
        if ( c.getClass() == JPanel.class )
            for ( Component c2 : ((JPanel)c).getComponents() )
                _traverseEnable( c2, isEnabled );
        /*
            Note:
                We use getClass() here, because we want to stop at subclasses of
                JPanel because they are likely user defined components and not dumb
                wrappers for other components.
        */
    }

}
