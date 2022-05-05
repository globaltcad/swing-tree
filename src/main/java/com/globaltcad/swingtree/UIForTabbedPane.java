package com.globaltcad.swingtree;

import javax.swing.*;

/**
 *  A swing tree builder for {@link JTabbedPane} instances.
 */
public class UIForTabbedPane extends UIForAbstractSwing<UIForTabbedPane, JTabbedPane>
{
    /**
     * {@link UIForAbstractSwing} types always wrap
     * a single component for which they are responsible.
     *
     * @param component The {@link JComponent} type which will be wrapped by this builder node.
     */
    public UIForTabbedPane(JTabbedPane component) {
        super(component);
    }

    public final UIForTabbedPane add(Tab tab) {
        this.component.addTab(tab.title(), tab.icon(), tab.contents(), tab.tip());
        return this;
    }

}
