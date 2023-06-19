package swingtree;

import javax.swing.*;

public class UIForCheckBoxMenuItem<M extends JCheckBoxMenuItem>
extends UIForAnyMenuItem<UIForCheckBoxMenuItem<M>, M>
{
    protected UIForCheckBoxMenuItem(M component) { super(component); }
}
