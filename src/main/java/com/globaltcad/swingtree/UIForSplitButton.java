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
    private final Map<JMenuItem, Runnable> options = new HashMap<>();

    protected UIForSplitButton(B component) {
        super(component);
        this.component.setPopupMenu(popupMenu);
        this.component.addButtonClickedActionListener( e -> {
            for ( JMenuItem item : options.keySet() ) {
                if ( item.getText().equals(component.getText()) ) {
                    Runnable action = options.get(item);
                    if ( action != null ) action.run();
                    break;
                }
            }
        });
    }

    public UIForSplitButton<B> onSplitClick(Consumer<EventContext<B, ActionEvent>> action ) {
        LogUtil.nullArgCheck(action, "action", Consumer.class);
        this.component.addSplitButtonClickedActionListener(
                e -> action.accept(new EventContext<>(this.component,e))
        );
        return this;
    }

    @Override
    public UIForSplitButton<B> onClick(Consumer<EventContext<B, ActionEvent>> action) {
        LogUtil.nullArgCheck(action, "action", Consumer.class);
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

    public UIForSplitButton<B> addOption(String option, Runnable action) {
        LogUtil.nullArgCheck(option, "option", String.class);
        LogUtil.nullArgCheck(action, "action", Runnable.class);
        JMenuItem item = new JMenuItem(option);
        popupMenu.add(item);
        options.put(item, action);
        item.addActionListener( e -> component.setText(item.getText()) );
        return this;
    }

    public UIForSplitButton<B> addOption(UIForMenuItem option, Runnable action) {
        LogUtil.nullArgCheck(option, "option", UIForMenuItem.class);
        LogUtil.nullArgCheck(action, "action", Runnable.class);
        JMenuItem item = option.component;
        popupMenu.add(item);
        options.put(item, action);
        item.addActionListener( e -> component.setText(item.getText()) );
        return this;
    }


    public <I extends JMenuItem> UIForSplitButton<B> add(Option<I> option) {
        LogUtil.nullArgCheck(option, "option", Option.class);
        I item = option.getItem();
        popupMenu.add(item);
        options.put(item, option.onButtonClick);
        item.addActionListener(
            e -> option.onItemSelected.accept(
                new Option.Context<>(new EventContext<>(component, e), item)
            )
        );
        return this;
    }

    public static class Option<I extends JMenuItem>
    {
        public static Option<JMenuItem> saying(String text) {
            return new Option<>(new JMenuItem(text));
        }
        public static <I extends JMenuItem> Option<I> of(I item) {
            return new Option<>(item);
        }
        public static Option<JMenuItem> of(UIForMenuItem item) {
            return new Option<>(item.component);
        }

        private final I item;
        private final Runnable onButtonClick;
        private final Consumer<Context<I>> onItemSelected;

        private Option( I item ) {
            this.item = item; this.onButtonClick = null; this.onItemSelected = null;
        }
        private Option( I item, Runnable action, Consumer<Context<I>> onSelected ) {
            this.item = item; this.onButtonClick = action; this.onItemSelected = onSelected;
        }

        public I getItem() { return item; }

        public Runnable getOnButtonClick() { return onButtonClick == null ? ()->{} : onButtonClick; }

        public Consumer<Context<I>> getOnItemSelected() { return onItemSelected == null ? c->{} : onItemSelected; }

        public Option<I> withOnButtonClick(Runnable action) {
            if ( this.onButtonClick != null ) throw new IllegalArgumentException("Property already specified!");
            return new Option<>(item, action, onItemSelected);
        }

        public Option<I> withOnItemSelected(Consumer<Context<I>> action) {
            if ( this.onItemSelected != null ) throw new IllegalArgumentException("Property already specified!");
            return new Option<>(item, onButtonClick, action);
        }

        public static class Context<I extends JMenuItem>
        {
            private final EventContext<JSplitButton, ActionEvent> eventContext;
            private final I currentItem;

            public Context(EventContext<JSplitButton, ActionEvent> eventContext, I currentItem) {
                this.eventContext = eventContext;
                this.currentItem = currentItem;
            }

            public EventContext<JSplitButton, ActionEvent> getEventContext() {
                return eventContext;
            }

            public I getCurrentItem() {
                return currentItem;
            }
        }

    }

}