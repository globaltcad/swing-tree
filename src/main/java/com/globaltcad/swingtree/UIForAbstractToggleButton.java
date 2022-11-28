package com.globaltcad.swingtree;

import javax.swing.*;

public abstract class UIForAbstractToggleButton<I, B extends JToggleButton> extends UIForAbstractButton<I, B>
{
    protected UIForAbstractToggleButton( B component ) { super(component); }
}