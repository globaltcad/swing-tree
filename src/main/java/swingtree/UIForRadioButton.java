package swingtree;

import javax.swing.*;

/**
 *  A SwingTree builder node designed for configuring {@link JRadioButton} instances.
 */
public class UIForRadioButton<R extends JRadioButton> extends UIForAnyToggleButton<UIForRadioButton<R>, R>
{
    protected UIForRadioButton( R component ) { super(component); }
}
