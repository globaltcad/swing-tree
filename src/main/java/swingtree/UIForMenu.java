package swingtree;

import sprouts.Val;

import javax.swing.*;

/**
 *  A swing tree builder node for {@link JMenu} instances.
 */
public class UIForMenu<M extends JMenu> extends UIForAbstractMenuItem<UIForMenu<M>, M>
{
    protected UIForMenu( M component ) { super(component); }


    public final UIForMenu<M> popupMenuIsVisibleIf( boolean popupMenuVisible ) {
        getComponent().setPopupMenuVisible(popupMenuVisible);
        return this;
    }

    public final UIForMenu<M> popupMenuIsVisibleIf( Val<Boolean> val ) {
        _onShow(val, v -> popupMenuIsVisibleIf(v));
        return popupMenuIsVisibleIf( val.get() );
    }

}