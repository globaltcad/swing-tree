package com.globaltcad.swingtree;


import javax.swing.*;

/**
 *  A swing tree builder node for {@link JToggleButton} instances.
 */
public class UIForToggleButton<B extends JToggleButton> extends UIForAbstractButton<UIForToggleButton<B>, B>
{
    protected UIForToggleButton(B component) { super(component); }
}
