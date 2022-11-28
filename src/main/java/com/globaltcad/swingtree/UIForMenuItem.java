package com.globaltcad.swingtree;


import javax.swing.*;

/**
 *  A swing tree builder node for {@link JMenuItem} instances.
 */
public class UIForMenuItem<M extends JMenuItem> extends UIForAbstractMenuItem<UIForMenuItem<M>, M>
{
    protected UIForMenuItem(M component) { super(component); }
}
