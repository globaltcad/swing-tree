package swingtree;

import sprouts.Val;

import javax.swing.JMenu;

/**
 *  A SwingTree builder node designed for configuring {@link JMenu} instances.
 */
public class UIForMenu<M extends JMenu> extends UIForAnyMenuItem<UIForMenu<M>, M>
{
    protected UIForMenu( M component ) { super(component); }

    /**
     *  Sets the popup menu visibility for this {@link JMenu}
     *  through the {@link JMenu#setPopupMenuVisible(boolean)} method.
     *
     * @param popupMenuVisible The popup menu visibility to set.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForMenu<M> popupMenuIsVisibleIf( boolean popupMenuVisible ) {
        getComponent().setPopupMenuVisible(popupMenuVisible);
        return this;
    }

    /**
     *  Binds to a popup menu visibility property for this {@link JMenu}
     *  through the {@link JMenu#setPopupMenuVisible(boolean)} method.
     *
     * @param val The popup menu visibility property to set.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForMenu<M> popupMenuIsVisibleIf( Val<Boolean> val ) {
        _onShow(val, v -> popupMenuIsVisibleIf(v));
        return popupMenuIsVisibleIf( val.get() );
    }

}