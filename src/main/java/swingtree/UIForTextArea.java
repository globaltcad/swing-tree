package swingtree;

import javax.swing.*;

/**
 *  A swing tree builder node for {@link JTextArea} instances.
 */
public class UIForTextArea<A extends JTextArea> extends UIForAnyTextComponent<UIForTextArea<A>, A>
{
    protected UIForTextArea( A component ) { super(component); }
}
