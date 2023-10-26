package swingtree;

import sprouts.Action;
import sprouts.Val;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.util.function.Consumer;

/**
 *  A SwingTree builder node designed for configuring {@link JPopupMenu} instances.
 * 	<p>
 * 	<b>Take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class or other classes.</b>
 */
public class UIForPopup<P extends JPopupMenu> extends UIForAnySwing<UIForPopup<P>, P>
{
    protected UIForPopup( P component ) { super(component); }

    /**
     *  Determines if the border is painted or not.
     *
     * @param borderPainted True if the border is painted, false otherwise
     * @return This builder node, to qllow for method chaining.
     */
    public final UIForPopup<P> borderIsPaintedIf( boolean borderPainted ) {
        return _with( thisComponent -> {
                    thisComponent.setBorderPainted( borderPainted );
                })
                ._this();
    }

    /**
     *  Determines if the border is painted or not
     *  based on the value of the given {@link Val}.
     *  If the value of the {@link Val} changes, the border will be painted or not.
     *
     * @param isPainted A {@link Val} which will be used to determine if the border is painted or not.
     * @return This builder node, to qllow for method chaining.
     */
    public final UIForPopup<P> borderIsPaintedIf( Val<Boolean> isPainted ) {
        return _withOnShow( isPainted, (thisComponent,it) -> {
                    thisComponent.setBorderPainted( it );
                })
                ._with( thisComponent -> {
                    thisComponent.setBorderPainted( isPainted.get() );
                })
                ._this();
    }

    /**
     *  Registers a listener to be notified when the popup is shown.
     *  This is typically triggered when {@link JPopupMenu#show(Component, int, int)} is called.
     *
     * @param action The action to be executed when the popup is shown.
     * @return this
     */
    public UIForPopup<P> onVisible( Action<ComponentDelegate<P, PopupMenuEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        return _with( thisComponent -> {
                    _onPopupOpen(thisComponent,
                        e -> _doApp(()->action.accept(new ComponentDelegate<>( thisComponent, e )) )
                    );
                })
                ._this();
    }

    private void _onPopupOpen( P thisComponent, Consumer<PopupMenuEvent> consumer ) {
        thisComponent.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                // This method is called before the popup menu becomes visible.
                consumer.accept(e);
            }
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {/* Not relevant here */}
            public void popupMenuCanceled(PopupMenuEvent e) {/* Not relevant here */}
        });
    }

    /**
     *  Registers a listener to be notified when the popup becomes invisible,
     *  meaning its popup menu is hidden.
     *
     * @param action The action to be executed when the popup becomes invisible.
     * @return This builder node, to allow for method chaining.
     */
    public UIForPopup<P> onInvisible( Action<ComponentDelegate<P, PopupMenuEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        return _with( thisComponent -> {
                    _onPopupClose(thisComponent,
                        e -> _doApp(()->action.accept(new ComponentDelegate<>( (P) thisComponent, e )) )
                    );
                })
                ._this();
    }

    private void _onPopupClose( P thisComponent, Consumer<PopupMenuEvent> consumer ) {
        thisComponent.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {/* Not relevant here */}
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                consumer.accept(e); // This method is called before the popup menu becomes invisible
            }
            public void popupMenuCanceled(PopupMenuEvent e) {/* Not relevant here */}
        });
    }

    /**
     *  Registers a listener to be notified when the popup is canceled.
     *  This is typically triggered when the user clicks outside the popup.
     *
     * @param action the action to be executed when the popup is canceled.
     * @return this
     */
    public UIForPopup<P> onCancel( Action<ComponentDelegate<P, PopupMenuEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        return _with( thisComponent -> {
                    _onPopupCancel(thisComponent,
                        e -> _doApp(()->action.accept(new ComponentDelegate<>( thisComponent, e )) )
                    );
                })
                ._this();
    }

    private void _onPopupCancel( P thisComponent, Consumer<PopupMenuEvent> consumer ) {
        thisComponent.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {/* Not relevant here */}
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {/* Not relevant here */}
            public void popupMenuCanceled(PopupMenuEvent e) {
                consumer.accept(e); // This method is called when the popup menu is canceled
            }
        });
    }


    public UIForPopup<P> add(JMenuItem item) { return this.add(UI.of(item)); }

    public UIForPopup<P> add(JSeparator separator) { return this.add(UI.of(separator)); }

    public UIForPopup<P> add(JPanel panel) { return this.add(UI.of(panel)); }
}

