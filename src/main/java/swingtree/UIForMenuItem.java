package swingtree;


import javax.swing.*;

/**
 *  A SwingTree builder node designed for configuring {@link JMenuItem} instances.
 */
public class UIForMenuItem<M extends JMenuItem> extends UIForAnyMenuItem<UIForMenuItem<M>, M>
{
    protected UIForMenuItem( M component ) { super(component); }
}
