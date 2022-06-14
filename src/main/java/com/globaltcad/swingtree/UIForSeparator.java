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
        Dimension d = _component.getPreferredSize();
        if ( _component.getOrientation() == JSeparator.VERTICAL ) d.height = separatorLength;
        else if ( _component.getOrientation() == JSeparator.HORIZONTAL ) d.width = separatorLength;
        _component.setPreferredSize(d);
        return this;
    }
}
