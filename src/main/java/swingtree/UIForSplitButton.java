package swingtree;

import sprouts.Action;
import sprouts.Event;
import sprouts.From;
import sprouts.Var;
import swingtree.components.JSplitButton;

import javax.swing.AbstractButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 *  A SwingTree builder node designed for configuring {@link JSplitButton} instances.
 */
public final class UIForSplitButton<B extends JSplitButton> extends UIForAnyButton<UIForSplitButton<B>, B>
{
    private final BuilderState<B> _state;

    /**
     *  Creates a new instance wrapping the given {@link JSplitButton} component.
     *
     * @param component The {@link JSplitButton} instance to wrap.
     */
    protected UIForSplitButton( B component ) {
        _state = new BuilderState<>(component);
        ExtraState state = ExtraState.of(component);
        component.setPopupMenu(state.popupMenu);
        component.addButtonClickedActionListener(e -> _doApp(()->{
            List<JMenuItem> selected = _getSelected(component);
            for ( JMenuItem item : selected ) {
                Action<SplitItemDelegate<JMenuItem>> action = state.options.get(item);
                if ( action != null )
                    action.accept(
                        new SplitItemDelegate<>(
                            e,
                            component,
                            ()-> new ArrayList<>(state.options.keySet()),
                            item
                        )
                    );
            }
        }));
    }

    @Override
    protected BuilderState<B> _state() {
        return _state;
    }

    private List<JMenuItem> _getSelected(B component) {
        ExtraState state = ExtraState.of(component);
        return Arrays.stream(state.popupMenu.getComponents())
                .filter( c -> c instanceof JMenuItem )
                .map( c -> (JMenuItem) c )
                .filter(AbstractButton::isSelected)
                .collect(Collectors.toList());
    }

    /**
     *  Use this to build {@link JSplitButton}s where the selectable options
     *  are represented by an {@link Enum} type, and the click event is
     *  handles by an {@link Event} instance.
     *
     * @param selection The {@link Var} which holds the currently selected {@link Enum} value.
     *                  This will be updated when the user selects a new value.
     * @param clickEvent The {@link sprouts.Event} which will be fired when the user clicks on the button.
     * @return A UI builder instance wrapping a {@link JSplitButton}.
     * @param <E> The {@link Enum} type defining the selectable options.
     */
    public <E extends Enum<E>> UIForSplitButton<B> withSelection( Var<E> selection, Event clickEvent ) {
        NullUtil.nullArgCheck(selection, "selection", Var.class);
        NullUtil.nullArgCheck(clickEvent, "clickEvent", Event.class);
        return _withAndGet( thisComponent -> {
                    for ( E e : selection.type().getEnumConstants() )
                        this.add(
                            UI.splitItem(e.toString())
                            .onButtonClick( it -> clickEvent.fire() )
                            .onSelection( it -> {
                                it.selectOnlyCurrentItem();
                                it.setButtonText(e.toString());
                                selection.set(From.VIEW, e);
                            })
                        );

                    return this.withText(selection.viewAsString());
                })
                ._this();
    }

    /**
     *  Use this to build {@link JSplitButton}s where the selectable options
     *  are represented by an {@link Enum} type.
     *
     * @param selection The {@link Var} which holds the currently selected {@link Enum} value.
     *                  This will be updated when the user selects a new value.
     * @param <E> The {@link Enum} type defining the selectable options.
     * @return A UI builder instance wrapping a {@link JSplitButton}.
     */
    public <E extends Enum<E>> UIForSplitButton<B> withSelection( Var<E> selection ) {
        NullUtil.nullArgCheck(selection, "selection", Var.class);
        return _withAndGet( thisComponent -> {
                    for ( E e : selection.type().getEnumConstants() )
                        this.add(
                            UI.splitItem(e.toString())
                            .onSelection( it -> {
                                it.selectOnlyCurrentItem();
                                it.setButtonText(e.toString());
                                selection.set(From.VIEW, e);
                            })
                        );

                    return this.withText(selection.viewAsString());
                })
                ._this();
    }

    /**
     *  {@link Action}s registered here will be called when the split part of the
     *  {@link JSplitButton} was clicked.
     *  The provided lambda receives a delegate object with a rich API
     *  exposing a lot of context information including not
     *  only the current {@link JSplitButton} instance, but also
     *  the currently selected {@link JMenuItem} and a list of
     *  all other items.
     *
     * @param action The {@link Action} which will receive an {@link ComponentDelegate}
     *               exposing all essential components making up this {@link JSplitButton}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public UIForSplitButton<B> onSplitClick(
        Action<SplitButtonDelegate<JMenuItem>> action
    ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        return _with( thisComponent -> {
                    ExtraState state = ExtraState.of(thisComponent);
                    thisComponent.addSplitButtonClickedActionListener(
                        e -> _doApp(()->action.accept(
                                new SplitButtonDelegate<>(
                                     thisComponent,
                                     new SplitItemDelegate<>(
                                         e,
                                         thisComponent,
                                         () -> new ArrayList<>(state.options.keySet()),
                                         state.lastSelected[0]
                                     )
                                )
                            )
                        )
                    );
                })
                ._this();
    }

    /**
     * {@link Action}s registered here will be called when the
     * user selects a {@link JMenuItem} from the popup menu
     * of this {@link JSplitButton}.
     * The delegate passed to the provided action
     * lambda exposes a lot of context information including not
     * only the current {@link JSplitButton} instance, but also
     * the currently selected {@link JMenuItem} and a list of
     * all other items.
     *
     * @param action The {@link Action} which will receive an {@link SplitItemDelegate}
     *              exposing all essential components making up this {@link JSplitButton}.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if the provided action is null.
     */
    public UIForSplitButton<B> onSelection(
        Action<SplitButtonDelegate<JMenuItem>> action
    ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        return _with( thisComponent -> {
                    ExtraState state = ExtraState.of(thisComponent);
                    state.onSelections.add(action);
                })
                ._this();
    }

    /**
     *  Use this as an alternative to {@link #onClick(Action)} to register
     *  a button click action with an action lambda having
     *  access to a delegate with more context information including not
     *  only the current {@link JSplitButton} instance, but also
     *  the currently selected {@link JMenuItem} and a list of
     *  all other items.
     *
     * @param action The {@link Action} which will receive an {@link ComponentDelegate}
     *               exposing all essential components making up this {@link JSplitButton}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public UIForSplitButton<B> onButtonClick(
        Action<SplitItemDelegate<JMenuItem>> action
    ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        return _with( thisComponent -> {
                    ExtraState state = ExtraState.of(thisComponent);
                    thisComponent.addButtonClickedActionListener(
                        e -> _doApp(()->action.accept(
                                new SplitItemDelegate<>(
                                   e,
                                   thisComponent,
                                   () -> new ArrayList<>(state.options.keySet()),
                                   state.lastSelected[0]
                                )
                            ))
                    );
                })
                ._this();
    }

    /**
     *  Use this to register a basic action for when the
     *  {@link JSplitButton} button is being clicked (not the split part).
     *  If you need more context information delegated to the action
     *  then consider using {@link #onButtonClick(Action)}.
     *
     * @param action An {@link Action} instance which will be wrapped by an {@link ComponentDelegate} and passed to the button component.
     * @return This very instance, which enables builder-style method chaining.
     */
    @Override
    public UIForSplitButton<B> onClick( Action<ComponentDelegate<B, ActionEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        return _with( thisComponent ->
                    thisComponent.addButtonClickedActionListener(
                        e -> _doApp(()->action.accept(
                             new ComponentDelegate<>( thisComponent, e )
                        ))
                    )
                )
                ._this();
    }

    /**
     *  Registers a listener to be notified when the split button is opened,
     *  meaning its popup menu is shown after the user clicks on the split button drop
     *  down button.
     *
     * @param action the action to be executed when the split button is opened.
     * @return this very instance, which enables builder-style method chaining.
     */
    public UIForSplitButton<B> onOpen( Action<ComponentDelegate<B, PopupMenuEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        return _with( thisComponent -> {
                    _onPopupOpen(thisComponent, e ->
                        _doApp(()->action.accept(new ComponentDelegate<>( thisComponent, e )) )
                    );
                })
                ._this();
    }

    private void _onPopupOpen( B thisComponent, Consumer<PopupMenuEvent> consumer ) {
        thisComponent.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
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
    public UIForSplitButton<B> onClose( Action<ComponentDelegate<B, PopupMenuEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        return _with( thisComponent -> {
                    _onPopupClose(thisComponent,
                        e -> _doApp(()->action.accept(new ComponentDelegate<>( thisComponent, e )) )
                    );
                })
                ._this();
    }

    private void _onPopupClose( B thisComponent, Consumer<PopupMenuEvent> consumer ) {
        thisComponent.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
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
    public UIForSplitButton<B> onCancel( Action<ComponentDelegate<B, PopupMenuEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        return _with( thisComponent -> {
                    _onPopupCancel(thisComponent,
                        e -> _doApp(()->action.accept(new ComponentDelegate<>( thisComponent, e )) )
                    );
                })
                ._this();
    }

    private void _onPopupCancel( B thisComponent, Consumer<PopupMenuEvent> consumer ) {
        thisComponent.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {/* Not relevant here */}
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {/* Not relevant here */}
            public void popupMenuCanceled(PopupMenuEvent e) {
                // This method is called when the popup menu is canceled.
                consumer.accept(e);
            }
        });
    }

    /**
     *  Use this to add a {@link JMenuItem} to the {@link JSplitButton} popup menu.
     *
     * @param forItem The builder whose wrapped {@link JMenuItem} will be added to and exposed
     *                by the {@link JSplitButton} once the split part was pressed.
     * @param <M> The type of the {@link JMenuItem} wrapped by the given {@link UIForMenuItem} instance.
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
     * @param splitItem The {@link SplitItem} instance wrapping a {@link JMenuItem} as well as some associated {@link Action}s.
     * @param <I> The {@link JMenuItem} type which should be added to this {@link JSplitButton} builder.
     * @return This very instance, which enables builder-style method chaining.
     */
    public <I extends JMenuItem> UIForSplitButton<B> add( SplitItem<I> splitItem ) {
        NullUtil.nullArgCheck(splitItem, "buttonItem", SplitItem.class);
        return _with( thisComponent -> {
                    ExtraState state = ExtraState.of(thisComponent);
                    I item = splitItem.getItem();
                    splitItem.getIsEnabled().ifPresent( isEnabled -> {
                        _onShow( isEnabled, thisComponent, (c,v) -> item.setEnabled(v) );
                    });

                    if ( item.isSelected() )
                        state.lastSelected[0] = item;

                    state.popupMenu.add(item);
                    state.options.put(item, ( (SplitItem<JMenuItem>) splitItem).getOnClick());
                    item.addActionListener(
                        e -> _doApp(()->{
                            state.lastSelected[0] = item;
                            item.setSelected(true);
                            SplitItemDelegate<I> delegate =
                                    new SplitItemDelegate<>(
                                            e,
                                            thisComponent,
                                            () -> state.options.keySet().stream().map(o -> (I) o ).collect(Collectors.toList()),
                                            item
                                        );
                            state.onSelections.forEach(action -> {
                                try {
                                    action.accept(new SplitButtonDelegate<>( thisComponent,(SplitItemDelegate<JMenuItem>) delegate ));
                                } catch (Exception exception) {
                                    exception.printStackTrace();
                                }
                            });
                            splitItem.getOnSelected().accept(delegate);
                        })
                    );
                })
                ._this();
    }

    private static class ExtraState {
        static ExtraState of( JSplitButton pane ) {
            Object found = pane.getClientProperty(ExtraState.class);
            if ( found instanceof ExtraState ) return (ExtraState) found;
            ExtraState state = new ExtraState();
            pane.putClientProperty(ExtraState.class, state);
            return state;
        }

        final JPopupMenu popupMenu = new JPopupMenu();
        final Map<JMenuItem, Action<SplitItemDelegate<JMenuItem>>> options = new LinkedHashMap<>(16);
        final JMenuItem[] lastSelected = {null};
        final List<Action<SplitButtonDelegate<JMenuItem>>> onSelections = new ArrayList<>();
    }

}