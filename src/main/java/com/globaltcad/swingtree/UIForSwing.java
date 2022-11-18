package com.globaltcad.swingtree;

import javax.swing.*;

/**
 *  A swing tree builder node for {@link JComponent} types.
 */
public class UIForSwing<C extends JComponent> extends UIForAbstractSwing<UIForSwing<C>, C>
{
    /**
     * {@link UIForAbstractSwing} (sub)types always wrap
     * a single component for which they are responsible.
     *
     * @param component The {@link JComponent} type which will be wrapped by this builder node.
     */
    public UIForSwing( C component ) { super(component); }
}
