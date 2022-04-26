package com.globaltcad.swingtree;

import javax.swing.*;

/**
 *  A UI make for {@link JPanel} instances.
 */
public class UIForPanel<P extends JPanel> extends UIForSwing<UIForPanel<P>, P>
{
    protected UIForPanel(P component) { super(component); }
}
