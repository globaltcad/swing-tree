package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.mvvm.Val;
import com.globaltcad.swingtree.layout.LayoutAttr;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/**
 *  A swing tree builder node for {@link JPanel} instances.
 */
public class UIForPanel<P extends JPanel> extends UIForAbstractSwing<UIForPanel<P>, P>
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
}
