package com.globaltcad.swingtree;


import javax.swing.*;

/**
 *  A swing tree builder for {@link JMenuItem} instances.
 */
public class UIForMenuItem<M extends JMenuItem> extends UIForAbstractButton<UIForMenuItem<M>, M>
{
    protected UIForMenuItem(M component) { super(component); }

    public UIForMenuItem<M> withKeyStroke(KeyStroke keyStroke) {
        this.component.setAccelerator(keyStroke);
        return this;
    }
}
