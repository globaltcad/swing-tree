package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.mvvm.Val;

import javax.swing.*;

/**
 *  A swing tree builder node for {@link JCheckBox} instances.
 */
public class UIForCheckBox<B extends JCheckBox> extends UIForAbstractButton<UIForCheckBox<B>, B>
{
    protected UIForCheckBox(B component) { super(component); }


    public UIForCheckBox<B> borderIsPaintedFlatIf(boolean borderPainted ) {
        getComponent().setBorderPaintedFlat(borderPainted);
        return this;
    }

    public UIForCheckBox<B> borderIsPaintedFlatIf(Val<Boolean> val ) {
        val.onShow(v -> _doUI(() -> borderIsPaintedFlatIf(v)));
        return borderIsPaintedFlatIf( val.get() );
    }

}
