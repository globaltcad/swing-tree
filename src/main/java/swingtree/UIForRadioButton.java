package swingtree;

import javax.swing.*;

/**
 *  A swing tree builder node for {@link JRadioButton} instances.
 */
public class UIForRadioButton<R extends JRadioButton> extends UIForAbstractToggleButton<UIForRadioButton<R>, R>
{
    protected UIForRadioButton( R component ) { super(component); }
}
