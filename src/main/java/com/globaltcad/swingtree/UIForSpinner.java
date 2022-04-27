package com.globaltcad.swingtree;

import javax.swing.*;

public class UIForSpinner extends UIForSwing<UIForSpinner, JSpinner>
{
    /**
     * Instances of the BasicBuilder as well as its sub types always wrap
     * a single component for which they are responsible.
     *
     * @param component The JComponent type which will be wrapped by this builder node.
     */
    public UIForSpinner(JSpinner component) {
        super(component);
    }
}
