package swingtree;

import sprouts.Val;

import javax.swing.*;

/**
 * A swing tree builder node for {@link AbstractButton} sub-type instances,
 * usually the {@link JButton} type.
 */
public class UIForButton<B extends AbstractButton> extends UIForAbstractButton<UIForButton<B>, B>
{
    protected UIForButton( B component ) { super(component); }

    public UIForButton<B> isBorderPaintedIf( boolean borderPainted ) {
        getComponent().setBorderPainted(borderPainted);
        return this;
    }

    public UIForButton<B> isBorderPaintedIf( Val<Boolean> val ) {
        _onShow(val, v -> isBorderPaintedIf(v) );
        return isBorderPaintedIf( val.get() );
    }

}
