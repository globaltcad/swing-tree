package com.globaltcad.swingtree;

import com.alexandriasoftware.swing.JSplitButton;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UIForSplitButton<B extends JSplitButton> extends UIForAbstractButton<UIForSplitButton<B>, B>
{
    private final JPopupMenu popupMenu = new JPopupMenu();
    private final Map<JMenuItem, UIAction<SplitItem.Delegate<JMenuItem>, ActionEvent>> options = new LinkedHashMap<>(16);
    private final JMenuItem[] selected = {null};
    private final List<UIAction<SplitItem.Delegate<JMenuItem>, ActionEvent>> onSelections = new ArrayList<>();

    protected UIForSplitButton(B component) {
        super(component);
        this.component.setPopupMenu(popupMenu);
        this.component.addButtonClickedActionListener( e -> {
            for ( JMenuItem item : options.keySet() ) {
                if ( item == selected[0] ) {
                    UIAction<SplitItem.Delegate<JMenuItem>, ActionEvent> action = options.get(item);
                    if ( action != null ) 
                        action.accept(
                            new EventContext<>(
                                new SplitItem.Delegate<>(
                                    component,
                                    ()-> new ArrayList<>(options.keySet()),
                                    item
                                ),
                                e
                            )
                        );
                    break;
                }
            }
        });
    }

    /**
     *  {@link UIAction}s registered here will be called when the split part of the
     *  {@link JSplitButton} was clicked.
     *  This exposes a delegate a lot of context information including not
     *  only the current {@link JSplitButton} instance, but also
     *  the currently selected {@link JMenuItem} and a list of
     *  all other items.
     *
     * @param action The {@link UIAction} which will receive an {@link EventContext}
     *               exposing all essential components making up this {@link JSplitButton}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public UIForSplitButton<B> onSplitClick(
            UIAction<SplitItem.Delegate<JMenuItem>, ActionEvent> action
    ) {
        LogUtil.nullArgCheck(action, "action", UIAction.class);
        this.component.addSplitButtonClickedActionListener(
                e -> action.accept(
                        new EventContext<>(
                            new SplitItem.Delegate<>(
                                    component,
                                    () -> new ArrayList<>(options.keySet()),
                                    selected[0]
                            ),
                            e
                        )
                )
        );
        return this;
    }

    public UIForSplitButton<B> onSelection(
            UIAction<SplitItem.Delegate<JMenuItem>, ActionEvent> action
    ) {
        LogUtil.nullArgCheck(action, "action", UIAction.class);
        this.onSelections.add(action);
        return this;
    }

    /**
     *  Use this as an alternative to {@link #onClick(UIAction)} to
     *  access a delegate with more context information inclusing not
     *  only the current {@link JSplitButton} instance, but also
     *  the currently selected {@link JMenuItem} and a list of
     *  all other items.
     *
     * @param action The {@link UIAction} which will receive an {@link EventContext}
     *               exposing all essential components making up this {@link JSplitButton}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public UIForSplitButton<B> onButtonClick(
        UIAction<SplitItem.Delegate<JMenuItem>, ActionEvent> action
    ) {
        LogUtil.nullArgCheck(action, "action", UIAction.class);
        this.component.addButtonClickedActionListener(
            e -> action.accept(
                    new EventContext<>(
                        new SplitItem.Delegate<>(
                           component,
                           () -> new ArrayList<>(options.keySet()),
                           selected[0]
                        ),
                        e
                    )
                )
        );
        return this;
    }

    /**
     *  Use this to register a basic action for when the
     *  {@link JSplitButton} button is being clicked (not the split part).
     *  If you need more context information delegated to the action
     *  then consider using {@link #onButtonClick(UIAction)}.
     *
     * @param action An {@link UIAction} instance which will be wrapped by an {@link EventContext} and passed to the button component.
     * @return This very instance, which enables builder-style method chaining.
     */
    @Override
    public UIForSplitButton<B> onClick(UIAction<B, ActionEvent> action) {
        LogUtil.nullArgCheck(action, "action", UIAction.class);
        this.component.addButtonClickedActionListener( e -> action.accept(new EventContext<>(this.component, e)) );
        return this;
    }

    public UIForSplitButton<B> add(UIForMenuItem forItem) {
        LogUtil.nullArgCheck(forItem, "forItem", UIForMenuItem.class);
        return this.add(forItem.component);
    }

    public UIForSplitButton<B> add(JMenuItem item) {
        LogUtil.nullArgCheck(item, "item", JMenuItem.class);
        return this.add(SplitItem.of(item));
    }

    public <I extends JMenuItem> UIForSplitButton<B> add(SplitItem<I> splitItem) {
        LogUtil.nullArgCheck(splitItem, "buttonItem", SplitItem.class);
        I item = splitItem.getItem();
        popupMenu.add(item);
        options.put(item, ( (SplitItem<JMenuItem>) splitItem).getOnClick());
        item.addActionListener(
            e -> {
                selected[0] = item;
                SplitItem.Delegate<I> delegate =
                        new SplitItem.Delegate<>(
                                component,
                                () -> options.keySet().stream().map( o -> (I) o ).collect(Collectors.toList()),
                                item
                            );
                onSelections.forEach(action -> {
                    try {
                        action.accept(new EventContext<>((SplitItem.Delegate<JMenuItem>) delegate, e));
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                });
                splitItem.getOnSelected().accept(new EventContext<>(delegate, e));
            }
        );
        return this;
    }
    
}