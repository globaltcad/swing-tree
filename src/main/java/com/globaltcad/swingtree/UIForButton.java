package com.globaltcad.swingtree;

import javax.swing.*;

/**
 * A swing tree builder node for {@link AbstractButton} sub-type instances,
 * usually the {@link JButton} type.
 */
public class UIForButton<B extends AbstractButton> extends UIForAbstractButton<UIForButton<B>, B>
{
    protected UIForButton(B component) { super(component); }

}
