package com.globaltcad.swingtree;

import javax.swing.*;

/**
 *  A swing tree builder for {@link JPanel} instances.
 */
public class UIForPanel<P extends JPanel> extends UIForAbstractSwing<UIForPanel<P>, P>
{
    protected UIForPanel(P component) { super(component); }
}
