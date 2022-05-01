package com.globaltcad.swingtree;

import javax.swing.*;
import java.awt.*;

/**
 *  A swing tree builder for {@link JSeparator} instances.
 */
public class UIForSeparator extends UIForAbstractSwing<UIForSeparator, JSeparator>
{
    /**
     * Instances of ths {@link UIForSeparator} always wrap
     * a single {@link JSeparator} for which they are responsible and expose helpful utility methods.
     *
     * @param component The JComponent type which will be wrapped by this builder node.
     */
    protected UIForSeparator(JSeparator component) { super(component); }

    /**
     * @param separatorLength The length of the separation line.
     * @return This very builder to allow for method chaining.
     */
    public UIForSeparator withLength(int separatorLength) {
        Dimension d = component.getPreferredSize();
        if ( component.getOrientation() == JSeparator.VERTICAL ) d.height = separatorLength;
        else if ( component.getOrientation() == JSeparator.HORIZONTAL ) d.width = separatorLength;
        component.setPreferredSize(d);
        return this;
    }
}
