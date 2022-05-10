package com.globaltcad.swingtree;

import javax.swing.*;

/**
 *  A swing tree builder for {@link JTextArea} instances.
 */
public class UIForTextArea<A extends JTextArea> extends UIForAbstractTextComponent<UIForTextArea<A>, A>
{
    protected UIForTextArea(A component) { super(component); }
}
