package com.globaltcad.swingtree;


import com.globaltcad.swingtree.api.mvvm.Val;

import javax.swing.*;

/**
 *  A swing tree builder node for {@link JMenuItem} instances.
 */
public class UIForMenuItem<M extends JMenuItem> extends UIForAbstractButton<UIForMenuItem<M>, M>
{
    protected UIForMenuItem(M component) { super(component); }

    /**
     * Sets the key combination which invokes the wrapped {@link JMenuItem}'s
     * action listeners without navigating the menu hierarchy. It is the
     * UI's responsibility to install the correct action.  Note that
     * when the keyboard accelerator is typed, it will work whether or
     * not the menu is currently displayed.
     *
     * @param keyStroke the <code>KeyStroke</code> which will
     *          serve as an accelerator
     * @return This very builder to allow for method chaining.
     */
    public UIForMenuItem<M> withKeyStroke( KeyStroke keyStroke ) {
        getComponent().setAccelerator(keyStroke);
        return this;
    }

    /**
     * Sets the key combination property which invokes the wrapped {@link JMenuItem}'s
     * action listeners without navigating the menu hierarchy. It is the
     * UI's responsibility to install the correct action.  Note that
     * when the keyboard accelerator is typed, it will work whether or
     * not the menu is currently displayed.
     *
     * @param val the dynamically set <code>KeyStroke</code> property which will
     *          serve as an accelerator
     * @return This very builder to allow for method chaining.
     */
    public UIForMenuItem<M> withKeyStroke( Val<KeyStroke> val ) {
        val.onShow(v -> _doUI(() -> withKeyStroke(v)));
        getComponent().setAccelerator(val.get());
        return this;
    }
}
