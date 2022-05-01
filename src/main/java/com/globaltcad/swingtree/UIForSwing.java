package com.globaltcad.swingtree;

import javax.swing.*;

public class UIForSwing<C extends JComponent> extends UIForAbstractSwing<UIForSwing<C>, C>
{
    /**
     * Instances of the BasicBuilder as well as its sub types always wrap
     * a single component for which they are responsible.
     *
     * @param component The JComponent type which will be wrapped by this builder node.
     */
    public UIForSwing(C component) {
        super(component);
    }
}
