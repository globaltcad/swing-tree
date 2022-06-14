package com.globaltcad.swingtree;

import javax.swing.*;

/**
 *  A swing tree builder node for {@link JTabbedPane} instances.
 */
public class UIForTabbedPane<P extends JTabbedPane> extends UIForAbstractSwing<UIForTabbedPane<P>, P>
{
    /**
     * {@link UIForAbstractSwing} (sub)types always wrap
     * a single component for which they are responsible.
     *
     * @param component The {@link JComponent} type which will be wrapped by this builder node.
     */
    public UIForTabbedPane(P component) { super(component); }

    public final UIForTabbedPane<P> add(Tab tab) {
        _component.addTab(tab.title(), tab.icon(), tab.contents(), tab.tip());
        return this;
    }

}
