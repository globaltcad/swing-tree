package com.globaltcad.swingtree;

import javax.swing.*;

/**
 *  A swing tree builder for {@link JMenu} instances.
 */
public class UIForMenu<M extends JMenu> extends UIForAbstractButton<UIForMenu<M>, M>
{
    protected UIForMenu(M component) { super(component); }
}