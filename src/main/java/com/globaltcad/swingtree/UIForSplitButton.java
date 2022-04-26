package com.globaltcad.swingtree;

import com.alexandriasoftware.swing.JSplitButton;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class UIForSplitButton<B extends JSplitButton> extends UIForAbstractButton<UIForSplitButton<B>, B>
{
    private final JPopupMenu popupMenu = new JPopupMenu();
    private final Map<JMenuItem, UIAction<JMenuItem, ActionEvent>> options = new HashMap<>();

    protected UIForSplitButton(B component) {
        super(component);
        this.component.setPopupMenu(popupMenu);
        this.component.addButtonClickedActionListener( e -> {
            for ( JMenuItem item : options.keySet() ) {
                if ( item.getText().equals(component.getText()) ) {
                    UIAction<JMenuItem, ActionEvent> action = options.get(item);
                    if ( action != null ) action.accept(new EventContext<>(item, e));
                    break;
                }
            }
        });
    }

    public UIForSplitButton<B> onSplitClick(UIAction<B, ActionEvent> action ) {
        LogUtil.nullArgCheck(action, "action", UIAction.class);
        this.component.addSplitButtonClickedActionListener(
                e -> action.accept(new EventContext<>(this.component,e))
        );
        return this;
    }

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
        popupMenu.add(item);
        return this;
    }

    public UIForSplitButton<B> addItem(String option, UIAction<JMenuItem, ActionEvent> action) {
        LogUtil.nullArgCheck(option, "option", String.class);
        LogUtil.nullArgCheck(action, "action", UIAction.class);
        return this.add(ButtonItem.saying(option).onClick(action));
    }

    public UIForSplitButton<B> addItem(UIForMenuItem option, UIAction<JMenuItem, ActionEvent> action) {
        LogUtil.nullArgCheck(option, "option", UIForMenuItem.class);
        LogUtil.nullArgCheck(action, "action", UIAction.class);
        return this.add(ButtonItem.of(option).onClick(action));
    }


    public <I extends JMenuItem> UIForSplitButton<B> add(ButtonItem<I> buttonItem) {
        LogUtil.nullArgCheck(buttonItem, "buttonItem", ButtonItem.class);
        I item = buttonItem.getItem();
        popupMenu.add(item);
        options.put(item, ( (ButtonItem<JMenuItem>) buttonItem ).getOnClick());
        item.addActionListener(
            e -> buttonItem.getOnSelected().accept(
                new ButtonItem.Context<>(new EventContext<>(component, e), item)
            )
        );
        return this;
    }
}