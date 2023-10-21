package swingtree;


import javax.swing.*;

/**
 *  A SwingTree builder node designed for configuring {@link JToggleButton} instances.
 * 	<p>
 * 	<b>Take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class or other classes.</b>
 */
public class UIForToggleButton<B extends JToggleButton> extends UIForAnyToggleButton<UIForToggleButton<B>, B>
{
    protected UIForToggleButton(B component) { super(component); }
}
