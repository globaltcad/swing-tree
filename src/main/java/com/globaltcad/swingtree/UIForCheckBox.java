package com.globaltcad.swingtree;

import javax.swing.*;

/**
 *  A swing tree builder node for {@link JCheckBox} instances.
 */
public class UIForCheckBox<B extends JCheckBox> extends UIForAbstractButton<UIForCheckBox<B>, B>
{
    protected UIForCheckBox(B component) { super(component); }

}
