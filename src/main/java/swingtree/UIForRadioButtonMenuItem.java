package swingtree;

import javax.swing.*;

public class UIForRadioButtonMenuItem<M extends JRadioButtonMenuItem>
extends UIForAnyMenuItem<UIForRadioButtonMenuItem<M>, M>
{
    protected UIForRadioButtonMenuItem(M component) { super(component); }
}
