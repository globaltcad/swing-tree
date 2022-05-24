package com.globaltcad.swingtree;

import javax.swing.*;

/**
 *  A swing tree builder for {@link JTextField} instances.
 */
public class UIForTextField<F extends JTextField> extends UIForAbstractTextComponent<UIForTextField<F>, F>
{
    protected UIForTextField(F component) { super(component); }
}
