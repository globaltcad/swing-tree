package com.globaltcad.swingtree;

import javax.swing.*;

public class UIForTabbedPane extends UIForAbstractSwing<UIForTabbedPane, JTabbedPane>
{
    /**
     * Instances of the BasicBuilder as well as its sub types always wrap
     * a single component for which they are responsible.
     *
     * @param component The JComponent type which will be wrapped by this builder node.
     */
    public UIForTabbedPane(JTabbedPane component) {
        super(component);
    }

    public final UIForTabbedPane add(Tab tab) {
        this.component.addTab(tab.title(), tab.icon(), tab.contents(), tab.tip());
        return this;
    }

}
