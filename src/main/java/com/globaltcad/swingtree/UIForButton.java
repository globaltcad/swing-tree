package com.globaltcad.swingtree;

import javax.swing.*;

public class UIForButton<B extends AbstractButton>
        extends UIForAbstractButton<UIForButton<B>, B>
{
    protected UIForButton(B component) {
        super(component);
    }
}
