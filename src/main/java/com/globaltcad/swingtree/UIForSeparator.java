package com.globaltcad.swingtree;

import javax.swing.*;
import java.awt.*;

/**
 *  A swing tree builder node for {@link JSeparator} instances.
 */
public class UIForSeparator<S extends JSeparator> extends UIForAbstractSwing<UIForSeparator<S>, S>
{
    /**
     * Instances of ths {@link UIForSeparator} always wrap
     * a single {@link JSeparator} for which they are responsible and expose helpful utility methods.
     *
     * @param component The JComponent type which will be wrapped by this builder node.
     */
    protected UIForSeparator(S component) { super(component); }

    /**
     * @param separatorLength The length of the separation line.
     * @return This very builder to allow for method chaining.
     */
    public UIForSeparator<S> withLength(int separatorLength) {
        S separator = getComponent();
        Dimension d = separator.getPreferredSize();
        if ( separator.getOrientation() == JSeparator.VERTICAL ) d.height = separatorLength;
        else if ( separator.getOrientation() == JSeparator.HORIZONTAL ) d.width = separatorLength;
        separator.setPreferredSize(d);
        return this;
    }
}
