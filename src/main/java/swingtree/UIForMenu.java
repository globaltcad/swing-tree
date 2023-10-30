package swingtree;

import sprouts.Val;

import javax.swing.JMenu;
import java.util.Objects;

/**
 *  A SwingTree builder node designed for configuring {@link JMenu} instances.
 */
public final class UIForMenu<M extends JMenu> extends UIForAnyMenuItem<UIForMenu<M>, M>
{
    private final BuilderState<M> _state;

    UIForMenu( BuilderState<M> state ) {
        Objects.requireNonNull(state);
        _state = state;
    }

    @Override
    protected BuilderState<M> _state() {
        return _state;
    }

    /**
     *  Sets the popup menu visibility for this {@link JMenu}
     *  through the {@link JMenu#setPopupMenuVisible(boolean)} method.
     *
     * @param popupMenuVisible The popup menu visibility to set.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForMenu<M> popupMenuIsVisibleIf( boolean popupMenuVisible ) {
        return _with( thisComponent -> {
                    thisComponent.setPopupMenuVisible(popupMenuVisible);
                })
                ._this();
    }

    /**
     *  Binds to a popup menu visibility property for this {@link JMenu}
     *  through the {@link JMenu#setPopupMenuVisible(boolean)} method.
     *
     * @param val The popup menu visibility property to set.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForMenu<M> popupMenuIsVisibleIf( Val<Boolean> val ) {
        return _withOnShow( val, (thisComponent, it) -> {
                    thisComponent.setPopupMenuVisible( it );
                })
                ._with( thisComponent -> {
                    thisComponent.setPopupMenuVisible( val.get() );
                })
                ._this();
    }

}