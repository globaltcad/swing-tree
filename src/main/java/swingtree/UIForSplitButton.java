package swingtree;

import org.slf4j.Logger;
import sprouts.Action;
import sprouts.Event;
import sprouts.From;
import sprouts.Var;
import swingtree.components.JSplitButton;
import swingtree.style.ComponentExtension;

import javax.swing.AbstractButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.event.ActionEvent;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *  A SwingTree builder node designed for configuring {@link JSplitButton} instances.
 *
 * @param <B> The type of {@link JSplitButton} that this {@link UIForSplitButton} is configuring.
 */
public final class UIForSplitButton<B extends JSplitButton> extends UIForAnyButton<UIForSplitButton<B>, B>
{
    private static Logger log = org.slf4j.LoggerFactory.getLogger(UIForSplitButton.class);

    private final BuilderState<B> _state;

    /**
     *  Creates a new instance wrapping the given {@link JSplitButton} component.
     *
     * @param state The {@link BuilderState} modelling how the component is built.
     */
    UIForSplitButton( BuilderState<B> state ) {
        Objects.requireNonNull(state);
        _state = state.withMutator(this::_initialize);
    }

    private void _initialize( B thisComponent ) {
        ExtraState.of( thisComponent, extraState -> {
            thisComponent.setPopupMenu(extraState.popupMenu);
            thisComponent.addButtonClickedActionListener(e -> _runInApp(()->{
                List<JMenuItem> selected = _getSelected(thisComponent);
                for ( JMenuItem item : selected ) {
                    Action<SplitItemDelegate<JMenuItem>> action = extraState.options.get(item);
                    try {
                        if (action != null)
                            action.accept(
                                    new SplitItemDelegate<>(
                                            e,
                                            thisComponent,
                                            () -> new ArrayList<>(extraState.options.keySet()),
                                            item
                                    )
                            );
                    } catch (Exception exception) {
                        log.error("Error while executing split button action listener.", exception);
                    }
                }
            }));
        });
    }

    @Override
    protected BuilderState<B> _state() {
        return _state;
    }
    
    @Override
    protected UIForSplitButton<B> _newBuilderWithState(BuilderState<B> newState ) {
        return new UIForSplitButton<>(newState);
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
        return withSelection(selection, clickEvent, Enum::toString);
    }

    private static <E extends Enum<E>> Function<E, String> _exceptionSafeTextProvider( Function<E, String> textProvider ) {
        return e -> {
            try {
                return textProvider.apply(e);
            } catch (Exception ex) {
                log.error("Error while providing split button text for enum value.", ex);
            }
            try {
                return e.toString();
            } catch (Exception ex) {
                log.error("Error while providing split button text for enum value using 'toString()'.", ex);
            }
            try {
                return e.name();
            } catch (Exception ex) {
                log.error("Error while providing split button text for enum value using 'name()'.", ex);
            }
            return "";
        };
    }

    /**
     *  Allows you to build {@link JSplitButton}s where the selectable options
     *  are represented by an {@link Enum} type, and the click event is
     *  handled by an {@link Event} instance as well as a "text provider",
     *  which is a function that maps an enum value to a string to be
     *  used as the button text displayed to the user.
     *
     * @param selection The {@link Var} which holds the currently selected {@link Enum} value.
     *                  This will be updated when the user selects a new value.
     * @param clickEvent The {@link sprouts.Event} which will be fired when the user clicks on the button.
     * @param textProvider A function which provides the text representation of an enum value.
     *                     If this function throws an exception, the enum value's {@link Enum#toString()}
     *                     method will be used as a fallback.
     *                     Exceptions are logged as errors.
     * @return The next declarative UI builder for the {@link JSplitButton} type.
     * @param <E> The {@link Enum} type defining the selectable options.
     */
    public <E extends Enum<E>> UIForSplitButton<B> withSelection( Var<E> selection, Event clickEvent, Function<E, String> textProvider ) {
        NullUtil.nullArgCheck(selection, "selection", Var.class);
        NullUtil.nullArgCheck(clickEvent, "clickEvent", Event.class);
        Objects.requireNonNull(textProvider, "textProvider");
        Function<E, String> exceptionSafeTextProvider = _exceptionSafeTextProvider(textProvider);
        return withText(selection.viewAsString())
                ._with( thisComponent -> {
                    for ( E e : selection.type().getEnumConstants() )
                        _addSplitItem(
                            UI.splitItem(exceptionSafeTextProvider.apply(e))
                            .onButtonClick( it -> clickEvent.fire() )
                            .onSelection( it -> {
                                it.selectOnlyCurrentItem();
                                it.setButtonText(exceptionSafeTextProvider.apply(e));
                                selection.set(From.VIEW, e);
                            }),
                            thisComponent
                        );
                })
                ._this();
    }

    /**
     *  Use this to build {@link JSplitButton}s where the selectable options
     *  are represented by an {@link Enum} type. Changes to the selected value are
     *  propagated to the provided {@link Var} instance, and the text representation
     *  of the selected value is determined using the {@link Enum#toString()} method.
     *
     * @param selection The {@link Var} which holds the currently selected {@link Enum} value.
     *                  This will be updated when the user selects a new value.
     * @param <E> The {@link Enum} type defining the selectable options.
     * @return A UI builder instance wrapping a {@link JSplitButton}.
     */
    public <E extends Enum<E>> UIForSplitButton<B> withSelection( Var<E> selection ) {
        NullUtil.nullArgCheck(selection, "selection", Var.class);
        return withSelection(selection, Enum::toString);
    }

    /**
     *  Use this to build {@link JSplitButton}s where the selectable options
     *  are represented by an {@link Enum} type. Changes to the selected value are
     *  propagated to the provided {@link Var} instance, and the text representation
     *  of the selected value is dynamically determined through the supplied text provider function.
     *
     * @param selection The {@link Var} which holds the currently selected {@link Enum} value.
     *                  This will be updated when the user selects a new value.
     * @param textProvider A function which provides the text representation of an enum value.
     *                     If this function throws an exception, the enum value's {@link Enum#toString()}
     *                     method will be used as a fallback.
     *                     Exceptions are logged as errors.
     * @param <E> The {@link Enum} type defining the selectable options.
     * @return A UI builder instance wrapping a {@link JSplitButton}.
     */
    public <E extends Enum<E>> UIForSplitButton<B> withSelection( Var<E> selection, Function<E, String> textProvider ) {
        NullUtil.nullArgCheck(selection, "selection", Var.class);
        Objects.requireNonNull(textProvider, "textProvider");
        Function<E, String> exceptionSafeTextProvider = _exceptionSafeTextProvider(textProvider);
        return withText(selection.viewAsString())
                ._with( thisComponent -> {
                    for ( E e : selection.type().getEnumConstants() )
                        _addSplitItem(
                            UI.splitItem(exceptionSafeTextProvider.apply(e))
                            .onSelection( it -> {
                                it.selectOnlyCurrentItem();
                                it.setButtonText(exceptionSafeTextProvider.apply(e));
                                selection.set(From.VIEW, e);
                            }),
                            thisComponent
                        );
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
                        e -> _runInApp(()->{
                                    try {
                                        action.accept(
                                                new SplitButtonDelegate<>(
                                                        thisComponent,
                                                        new SplitItemDelegate<>(
                                                                e, thisComponent,
                                                                () -> new ArrayList<>(state.options.keySet()),
                                                                state.lastSelected[0]
                                                        )
                                                )
                                        );
                                    } catch (Exception exception) {
                                        log.error("Error while executing split button action listener.", exception);
                                    }
                                })
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
                        e -> _runInApp(()->{
                            try {
                                action.accept(
                                        new SplitItemDelegate<>(
                                                e,
                                                thisComponent,
                                                () -> new ArrayList<>(state.options.keySet()),
                                                state.lastSelected[0]
                                        )
                                );
                            } catch (Exception exception) {
                                log.error("Error while executing split button action listener.", exception);
                            }
                        })
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
                        e -> _runInApp(()->{
                            try {
                                action.accept(
                                        new ComponentDelegate<>(thisComponent, e)
                                );
                            } catch (Exception ex) {
                                log.error("Error while executing action on button click!", ex);
                            }
                        })
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
                        _runInApp(()->{
                            try {
                                action.accept(new ComponentDelegate<>(thisComponent, e));
                            } catch (Exception ex) {
                                log.error("Error while executing action on popup open!", ex);
                            }
                        })
                    );
                })
                ._this();
    }

    private void _onPopupOpen( B thisComponent, Consumer<PopupMenuEvent> consumer ) {
        JPopupMenu popupMenu = thisComponent.getPopupMenu();
        if ( popupMenu == null )
            return;
        popupMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                // This method is called before the popup menu becomes visible.
                consumer.accept(e);
            }
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {/* Not relevant here */}
            @Override
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
                        e -> _runInApp(()->{
                            try {
                                action.accept(new ComponentDelegate<>(thisComponent, e));
                            } catch (Exception ex) {
                                log.error("Error while executing action on popup close!", ex);
                            }
                        })
                    );
                })
                ._this();
    }

    private void _onPopupClose( B thisComponent, Consumer<PopupMenuEvent> consumer ) {
        JPopupMenu popupMenu = thisComponent.getPopupMenu();
        if ( popupMenu == null )
            return;
        popupMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {/* Not relevant here */}
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                // This method is called before the popup menu becomes invisible.
                consumer.accept(e);
            }
            @Override
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
                        e -> _runInApp(()->{
                            try {
                                action.accept(new ComponentDelegate<>(thisComponent, e));
                            } catch (Exception ex) {
                                log.error("Error while executing action on popup cancel!", ex);
                            }
                        })
                    );
                })
                ._this();
    }

    private void _onPopupCancel( B thisComponent, Consumer<PopupMenuEvent> consumer ) {
        JPopupMenu popupMenu = thisComponent.getPopupMenu();
        if ( popupMenu == null )
            return;
        popupMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {/* Not relevant here */}
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {/* Not relevant here */}
            @Override
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
     *  Use this to add a {@link JMenuItem} to the {@link JSplitButton} popup menu.
     * @param item A {@link JMenuItem} which will be exposed by this {@link JSplitButton} once the split part was pressed.
     * @return This very instance, which enables builder-style method chaining.
     */
    public UIForSplitButton<B> add( JMenuItem item ) {
        NullUtil.nullArgCheck(item, "item", JMenuItem.class);
        return this.add(SplitItem.of(item));
    }

    /**
     *  Use this to add a {@link SplitItem} to the {@link JSplitButton} popup menu.
     *
     * @param splitItem The {@link SplitItem} instance wrapping a {@link JMenuItem} as well as some associated {@link Action}s.
     * @param <I> The {@link JMenuItem} type which should be added to this {@link JSplitButton} builder.
     * @return This very instance, which enables builder-style method chaining.
     */
    public <I extends JMenuItem> UIForSplitButton<B> add( SplitItem<I> splitItem ) {
        NullUtil.nullArgCheck(splitItem, "buttonItem", SplitItem.class);
        return _with( thisComponent -> {
                    _addSplitItem(splitItem, thisComponent);
                })
                ._this();
    }

    private <I extends JMenuItem> void _addSplitItem( SplitItem<I> splitItem, B thisComponent ) {
        I item = splitItem.getItem();
        splitItem.getIsEnabled().ifPresent( isEnabled -> {
            WeakReference<I> weakItem = new WeakReference<>(item);
            _onShow( isEnabled, thisComponent, (scopedComponent,newIsSelected) -> {
                I strongItem = weakItem.get();
                if ( strongItem != null )
                    strongItem.setEnabled(newIsSelected);
            });
        });

        ExtraState state = ExtraState.of(thisComponent);
        if ( item.isSelected() )
            state.lastSelected[0] = item;

        state.popupMenu.add(item);
        state.options.put(item, ( (SplitItem<JMenuItem>) splitItem).getOnClick());
        item.addActionListener(
            e -> _runInApp(()->{
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
                        log.error("Error while executing selection action listener.", exception);
                    }
                });
                try {
                    splitItem.getOnSelected().accept(delegate);
                } catch (Exception exception) {
                    log.error("Error while executing split item selection action.", exception);
                }
            })
        );
    }

    private static class ExtraState
    {
        static ExtraState of( JSplitButton pane ) {
            return of(pane, state->{});
        }
        static ExtraState of( JSplitButton pane, Consumer<ExtraState> ini ) {
            return ComponentExtension.from(pane)
                                    .getOrSet(ExtraState.class, ()->{
                                        ExtraState s = new ExtraState();
                                        ini.accept(s);
                                        return s;
                                    });
        }

        final JPopupMenu popupMenu = new JPopupMenu();
        final Map<JMenuItem, Action<SplitItemDelegate<JMenuItem>>> options = new LinkedHashMap<>(16);
        final JMenuItem[] lastSelected = {null};
        final List<Action<SplitButtonDelegate<JMenuItem>>> onSelections = new ArrayList<>();
    }

}