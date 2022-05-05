package com.globaltcad.swingtree;

import javax.swing.*;

public class UIForSplitPane extends UIForAbstractSwing<UIForSplitPane, JSplitPane>
{
    /**
     * Instances of the BasicBuilder as well as its sub types always wrap
     * a single component for which they are responsible.
     *
     * @param component The JComponent type which will be wrapped by this builder node.
     */
    public UIForSplitPane(JSplitPane component) { super(component); }

    public final UIForSplitPane withDividerAt(int location) {
        this.component.setDividerLocation(location);
        return this;
    }

    public final UIForSplitPane withDividerSize(int size) {
        this.component.setDividerSize(size);
        return this;
    }


}
