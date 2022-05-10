package com.globaltcad.swingtree;

import javax.swing.*;

/**
 *  A swing tree builder for {@link JRadioButton} instances.
 */
public class UIForRadioButton<R extends JRadioButton> extends UIForAbstractButton<UIForRadioButton<R>, R>
{
    protected UIForRadioButton(R component) { super(component); }
}
