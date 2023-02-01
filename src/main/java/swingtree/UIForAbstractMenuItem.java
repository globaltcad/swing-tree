package swingtree;

import sprouts.Val;

import javax.swing.*;

public class UIForAbstractMenuItem<I, M extends JMenuItem> extends UIForAbstractButton<I, M>
{
    protected UIForAbstractMenuItem( M component ) { super(component); }

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
    public I withKeyStroke( KeyStroke keyStroke ) {
        getComponent().setAccelerator(keyStroke);
        @SuppressWarnings("unchecked") I self = (I) this;
        return self;
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
    public I withKeyStroke( Val<KeyStroke> val ) {
        _onShow(val, v -> withKeyStroke(v) );
        getComponent().setAccelerator(val.get());
        @SuppressWarnings("unchecked") I self = (I) this;
        return self;
    }
}
