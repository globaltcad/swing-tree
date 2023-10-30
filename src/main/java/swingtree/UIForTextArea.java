package swingtree;

import javax.swing.*;

/**
 *  A SwingTree builder node designed for configuring {@link JTextArea} instances.
 */
public class UIForTextArea<A extends JTextArea> extends UIForAnyTextComponent<UIForTextArea<A>, A>
{
    protected UIForTextArea( A component ) { super(component); }
}
