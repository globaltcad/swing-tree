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
        return _with( thisComponent -> {
                   thisComponent.setBorderPaintedFlat(borderPainted);
               })
               ._this();
    }

    public UIForCheckBox<B> borderIsPaintedFlatIf(Val<Boolean> val ) {
        return _with( thisComponent -> {
                   _onShow(val, v -> thisComponent.setBorderPaintedFlat(v) );
                   thisComponent.setBorderPaintedFlat( val.get() );
               })
               ._this();
    }

}
