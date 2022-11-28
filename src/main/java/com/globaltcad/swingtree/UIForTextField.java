package com.globaltcad.swingtree;

import javax.swing.*;

/**
 *  A swing tree builder node for {@link JTextField} instances.
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class or other classes.</b>
 */
public class UIForTextField<F extends JTextField> extends UIForAbstractTextComponent<UIForTextField<F>, F>
{
    protected UIForTextField( F component ) { super(component); }
}
