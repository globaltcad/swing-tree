package com.globaltcad.swingtree;


import com.alexandriasoftware.swing.JSplitButton;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

/**
 *  {@link ButtonItem}s represent button options for the {@link JSplitButton}
 *  which can be supplied to a split button through the API exposed by {@link UIForSplitButton}.
 *  <pre>{@code
 *
 *      UI.splitButton("Hey!")
 *      .add(ButtonItem.saying("first").onClick( it -> System.out.println("FIRST CLICKED")) )
 *      .add(ButtonItem.saying("second").onClick( it -> System.out.println("FIRST CLICKED")) )
 *      .add(ButtonItem.saying("third").onClick( it-> System.out.println("FIRST CLICKED")) )
 *  }</pre>
 *
 * @param <I>
 */
public class ButtonItem<I extends JMenuItem>
{
    public static ButtonItem<JMenuItem> saying(String text) {
        return new ButtonItem<>(new JMenuItem(text));
    }
    public static <I extends JMenuItem> ButtonItem<I> of(I item) {
        return new ButtonItem<>(item);
    }
    public static ButtonItem<JMenuItem> of(UIForMenuItem item) {
        return new ButtonItem<>(item.component);
    }

    private final I item;
    private final Consumer<EventContext<I, ActionEvent>> onButtonClick;
    private final Consumer<Context<I>> onItemSelected;

    private ButtonItem(I item ) {
        this.item = item; this.onButtonClick = null; this.onItemSelected = null;
    }
    private ButtonItem(I item, Consumer<EventContext<I, ActionEvent>> action, Consumer<Context<I>> onSelected ) {
        this.item = item; this.onButtonClick = action; this.onItemSelected = onSelected;
    }

    public I getItem() { return item; }

    public ButtonItem<I> onClick(Consumer<EventContext<I, ActionEvent>> action) {
        if ( this.onButtonClick != null ) throw new IllegalArgumentException("Property already specified!");
        return new ButtonItem<>(item, action, onItemSelected);
    }

    public ButtonItem<I> onSelection(Consumer<Context<I>> action) {
        if ( this.onItemSelected != null ) throw new IllegalArgumentException("Property already specified!");
        return new ButtonItem<>(item, onButtonClick, action);
    }


    Consumer<EventContext<I, ActionEvent>> getOnClick() { return onButtonClick == null ? it->{} : onButtonClick; }

    Consumer<Context<I>> getOnSelected() {
        return onItemSelected == null
                ? c-> c.getEventContext().getComponent().setText(c.getCurrentItem().getText())
                : onItemSelected;
    }


    public static class Context<I extends JMenuItem>
    {
        private final EventContext<JSplitButton, ActionEvent> eventContext;
        private final I currentItem;

        Context(EventContext<JSplitButton, ActionEvent> eventContext, I currentItem) {
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

