package swingtree;

import sprouts.Val;

import javax.swing.*;

/**
 *  A SwingTree builder node designed for configuring {@link JCheckBox} instances.
 */
public class UIForCheckBox<B extends JCheckBox> extends UIForAnyButton<UIForCheckBox<B>, B>
{
    protected UIForCheckBox(B component) { super(component); }


    public UIForCheckBox<B> borderIsPaintedFlatIf(boolean borderPainted ) {
        getComponent().setBorderPaintedFlat(borderPainted);
        return this;
    }

    public UIForCheckBox<B> borderIsPaintedFlatIf(Val<Boolean> val ) {
        _onShow(val, v -> borderIsPaintedFlatIf(v) );
        return borderIsPaintedFlatIf( val.get() );
    }

}
