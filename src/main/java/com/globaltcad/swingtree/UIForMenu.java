package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.mvvm.Val;

import javax.swing.*;

/**
 *  A swing tree builder node for {@link JMenu} instances.
 */
public class UIForMenu<M extends JMenu> extends UIForAbstractButton<UIForMenu<M>, M>
{
    protected UIForMenu( M component ) { super(component); }


    public final UIForMenu<M> isPopupMenuVisibleIf( boolean popupMenuVisible ) {
        getComponent().setPopupMenuVisible(popupMenuVisible);
        return this;
    }

    public final UIForMenu<M> isPopupMenuVisibleIf( Val<Boolean> val ) {
        val.onShow(v -> _doUI(() -> isPopupMenuVisibleIf(v)));
        return isPopupMenuVisibleIf( val.get() );
    }

}