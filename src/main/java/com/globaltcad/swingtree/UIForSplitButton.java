package com.globaltcad.swingtree;

import com.alexandriasoftware.swing.JSplitButton;
import com.globaltcad.swingtree.api.UIAction;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 *  A swing tree builder node for {@link JSplitButton} instances.
 */
public class UIForSplitButton<B extends JSplitButton> extends UIForAbstractButton<UIForSplitButton<B>, B>
{
    private final JPopupMenu _popupMenu = new JPopupMenu();
    private final Map<JMenuItem, UIAction<SplitItem.Delegate<JMenuItem>>> _options = new LinkedHashMap<>(16);
    private final JMenuItem[] _lastSelected = {null};
    private final List<UIAction<SplitButtonDelegate<JMenuItem>>> _onSelections = new ArrayList<>();

    protected UIForSplitButton( B component ) {
        super(component);
        getComponent().setPopupMenu(_popupMenu);
        getComponent().addButtonClickedActionListener(e -> _doApp(()->{
            List<JMenuItem> selected = _getSelected();
            for ( JMenuItem item : selected ) {
                UIAction<SplitItem.Delegate<JMenuItem>> action = _options.get(item);
                if ( action != null )
                    action.accept(
                        new SplitItem.Delegate<>(
                            e,
                            component,
                            ()-> new ArrayList<>(_options.keySet()),
                            item
                        )
                    );
            }
        }));
    }

    private List<JMenuItem> _getSelected() {
        return Arrays.stream(_popupMenu.getComponents())
                .filter( c -> c instanceof JMenuItem )
                .map( c -> (JMenuItem) c )
                .filter(AbstractButton::isSelected)
                .collect(Collectors.toList());
    }

    /**
     *  {@link UIAction}s registered here will be called when the split part of the
     *  {@link JSplitButton} was clicked.
     *  The provided lambda receives a delegate object with a rich API
     *  exposing a lot of context information including not
     *  only the current {@link JSplitButton} instance, but also
     *  the currently selected {@link JMenuItem} and a list of
     *  all other items.
     *
     * @param action The {@link UIAction} which will receive an {@link SimpleDelegate}
     *               exposing all essential components making up this {@link JSplitButton}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public UIForSplitButton<B> onSplitClick(
        UIAction<SplitButtonDelegate<JMenuItem>> action
    ) {
        NullUtil.nullArgCheck(action, "action", UIAction.class);
        B button = getComponent();
        button.addSplitButtonClickedActionListener(
            e -> _doApp(()->action.accept(
                    new SplitButtonDelegate<>(
                         button,
                         new SplitItem.Delegate<>(
                             e,
                             button,
                             () -> new ArrayList<>(_options.keySet()),
                             _lastSelected[0]
                         ),
                        this::getSiblinghood
                    )
                )
            )
        );
        return this;
    }

    /**
     * {@link UIAction}s registered here will be called when the
     * user selects a {@link JMenuItem} from the popup menu
     * of this {@link JSplitButton}.
     * The delegate passed to the provided action
     * lambda exposes a lot of context information including not
     * only the current {@link JSplitButton} instance, but also
     * the currently selected {@link JMenuItem} and a list of
     * all other items.
     *
     * @param action The {@link UIAction} which will receive an {@link SplitItem.Delegate}
     *              exposing all essential components making up this {@link JSplitButton}.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if the provided action is null.
     */
    public UIForSplitButton<B> onSelection(
            UIAction<SplitButtonDelegate<JMenuItem>> action
    ) {
        NullUtil.nullArgCheck(action, "action", UIAction.class);
        _onSelections.add(action);
        return this;
    }

    /**
     *  Use this as an alternative to {@link #onClick(UIAction)} to register
     *  a button click action with an action lambda having
     *  access to a delegate with more context information including not
     *  only the current {@link JSplitButton} instance, but also
     *  the currently selected {@link JMenuItem} and a list of
     *  all other items.
     *
     * @param action The {@link UIAction} which will receive an {@link SimpleDelegate}
     *               exposing all essential components making up this {@link JSplitButton}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public UIForSplitButton<B> onButtonClick(
        UIAction<SplitItem.Delegate<JMenuItem>> action
    ) {
        NullUtil.nullArgCheck(action, "action", UIAction.class);
        B button = getComponent();
        button.addButtonClickedActionListener(
            e -> _doApp(()->action.accept(
                    new SplitItem.Delegate<>(
                       e,
                       button,
                       () -> new ArrayList<>(_options.keySet()),
                       _lastSelected[0]
                    )
                ))
        );
        return this;
    }

    /**
     *  Use this to register a basic action for when the
     *  {@link JSplitButton} button is being clicked (not the split part).
     *  If you need more context information delegated to the action
     *  then consider using {@link #onButtonClick(UIAction)}.
     *
     * @param action An {@link UIAction} instance which will be wrapped by an {@link SimpleDelegate} and passed to the button component.
     * @return This very instance, which enables builder-style method chaining.
     */
    @Override
    public UIForSplitButton<B> onClick( UIAction<SimpleDelegate<B, ActionEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", UIAction.class);
        B button = getComponent();
        button.addButtonClickedActionListener(
            e -> _doApp(()->action.accept(
                 new SimpleDelegate<>(button, e, this::getSiblinghood)
            ))
        );
        return this;
    }

    /**
     *  Registers a listener to be notified when the split button is opened,
     *  meaning its popup menu is shown after the user clicks on the split button drop
     *  down button.
     *
     * @param action the action to be executed when the split button is opened.
     * @return this very instance, which enables builder-style method chaining.
     */
    public UIForSplitButton<B> onOpen( UIAction<SimpleDelegate<B, PopupMenuEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", UIAction.class);
        _onPopupOpen( e -> _doApp(()->action.accept(new SimpleDelegate<>( getComponent(), e, this::getSiblinghood )) ) );
        return this;
    }

    private void _onPopupOpen( Consumer<PopupMenuEvent> consumer ) {
        getComponent().getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                // This method is called before the popup menu becomes visible.
                consumer.accept(e);
            }
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {/* Not relevant here */}
            public void popupMenuCanceled(PopupMenuEvent e) {/* Not relevant here */}
        });
    }

    /**
     *  Registers a listener to be notified when the split button is closed,
     *  meaning its popup menu is hidden after the user clicks on the split button drop
     *  down button.
     *
     * @param action the action to be executed when the split button is closed.
     * @return this very instance, which enables builder-style method chaining.
     */
    public UIForSplitButton<B> onClose( UIAction<SimpleDelegate<B, PopupMenuEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", UIAction.class);
        _onPopupClose( e -> _doApp(()->action.accept(new SimpleDelegate<>( getComponent(), e, this::getSiblinghood )) ) );
        return this;
    }

    private void _onPopupClose( Consumer<PopupMenuEvent> consumer ) {
        getComponent().getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {/* Not relevant here */}
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                // This method is called before the popup menu becomes invisible.
                consumer.accept(e);
            }
            public void popupMenuCanceled(PopupMenuEvent e) {/* Not relevant here */}
        });
    }

    /**
     *  Registers a listener to be notified when the split button options drop down popup is canceled,
     *  which typically happens when the user clicks outside the popup menu.
     *
     * @param action the action to be executed when the split button popup is canceled.
     * @return this very instance, which enables builder-style method chaining.
     */
    public UIForSplitButton<B> onCancel( UIAction<SimpleDelegate<B, PopupMenuEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", UIAction.class);
        _onPopupCancel( e -> _doApp(()->action.accept(new SimpleDelegate<>( getComponent(), e, this::getSiblinghood )) ) );
        return this;
    }

    private void _onPopupCancel( Consumer<PopupMenuEvent> consumer ) {
        getComponent().getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {/* Not relevant here */}
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {/* Not relevant here */}
            public void popupMenuCanceled(PopupMenuEvent e) {
                // This method is called when the popup menu is canceled.
                consumer.accept(e);
            }
        });
    }

    /**
     * @param forItem The builder whose wrapped {@link JMenuItem} will be added to and exposed
     *                by the {@link JSplitButton} once the split part was pressed.
     * @return This very instance, which enables builder-style method chaining.
     */
    public <M extends JMenuItem> UIForSplitButton<B> add( UIForMenuItem<M> forItem ) {
        NullUtil.nullArgCheck(forItem, "forItem", UIForMenuItem.class);
        return this.add(forItem.getComponent());
    }

    /**
     * @param item A {@link JMenuItem} which will be exposed by this {@link JSplitButton} once the split part was pressed.
     * @return This very instance, which enables builder-style method chaining.
     */
    public UIForSplitButton<B> add( JMenuItem item ) {
        NullUtil.nullArgCheck(item, "item", JMenuItem.class);
        return this.add(SplitItem.of(item));
    }

    /**
     * @param splitItem The {@link SplitItem} instance wrapping a {@link JMenuItem} as well as some associated {@link UIAction}s.
     * @param <I> The {@link JMenuItem} type which should be added to this {@link JSplitButton} builder.
     * @return This very instance, which enables builder-style method chaining.
     */
    public <I extends JMenuItem> UIForSplitButton<B> add( SplitItem<I> splitItem ) {
        NullUtil.nullArgCheck(splitItem, "buttonItem", SplitItem.class);
        I item = splitItem.getItem();
        if ( splitItem.getIsEnabled() != null )
            _onShow(splitItem.getIsEnabled(), v -> item.setEnabled(v) );

        if ( item.isSelected() )
            _lastSelected[0] = item;

        B button = getComponent();
        _popupMenu.add(item);
        _options.put(item, ( (SplitItem<JMenuItem>) splitItem).getOnClick());
        item.addActionListener(
            e -> _doApp(()->{
                _lastSelected[0] = item;
                item.setSelected(true);
                SplitItem.Delegate<I> delegate =
                        new SplitItem.Delegate<>(
                                e,
                                button,
                                () -> _options.keySet().stream().map(o -> (I) o ).collect(Collectors.toList()),
                                item
                            );
                _onSelections.forEach(action -> {
                    try {
                        action.accept(new SplitButtonDelegate<>(button,(SplitItem.Delegate<JMenuItem>) delegate, this::getSiblinghood));
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                });
                splitItem.getOnSelected().accept(delegate);
            })
        );
        return this;
    }

    JPopupMenu getPopupMenu() { return _popupMenu; }

}