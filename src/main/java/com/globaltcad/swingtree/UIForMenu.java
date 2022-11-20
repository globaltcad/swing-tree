package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.mvvm.Val;

import javax.swing.*;

/**
 *  A swing tree builder node for {@link JMenu} instances.
 */
public class UIForMenu<M extends JMenu> extends UIForAbstractButton<UIForMenu<M>, M>
{
    protected UIForMenu( M component ) { super(component); }


    public final UIForMenu<M> popupMenuIsVisibleIf( boolean popupMenuVisible ) {
        getComponent().setPopupMenuVisible(popupMenuVisible);
        return this;
    }

    public final UIForMenu<M> popupMenuIsVisibleIf( Val<Boolean> val ) {
        val.onShow(v -> _doUI(() -> popupMenuIsVisibleIf(v)));
        return popupMenuIsVisibleIf( val.get() );
    }

}