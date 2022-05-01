package com.globaltcad.swingtree;

import javax.swing.*;

/**
 *  A swing tree builder for {@link JSpinner} instances.
 */
public class UIForSpinner extends UIForAbstractSwing<UIForSpinner, JSpinner>
{
    /**
     * Instances of the BasicBuilder as well as its sub types always wrap
     * a single component for which they are responsible.
     *
     * @param component The JComponent type which will be wrapped by this builder node.
     */
    protected UIForSpinner(JSpinner component) { super(component); }
}
