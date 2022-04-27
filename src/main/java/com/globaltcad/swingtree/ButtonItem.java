package com.globaltcad.swingtree;


import com.alexandriasoftware.swing.JSplitButton;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 *  {@link ButtonItem}s represent button options for the {@link JSplitButton}
 *  which can be supplied to a split button through the API exposed by {@link UIForSplitButton}.
 *  <pre>{@code
 *
 *      UI.splitButton("Hey!")
 *      .add(ButtonItem.saying("first"))
 *      .add(ButtonItem.saying("second").onClick( it -> ... ))
 *      .add(ButtonItem.saying("third").onClick( it -> ... ).onSelected( it -> ... ))
 *  }</pre>
 *
 * @param <I>
 */
public final class ButtonItem<I extends JMenuItem>
{
    /**
     *
     * @param text The text which should be displayed on the {@link ButtonItem} (and its underlying {@link JMenuItem}).
     * @return A {@link ButtonItem} wrapping a simple {@link JMenuItem} displaying the provided text.
     */
    public static ButtonItem<JMenuItem> saying(String text) {
        return new ButtonItem<>(new JMenuItem(text));
    }

    /**
     *
     * @param item The {@link JMenuItem} subtype for which a {@link ButtonItem} (for {@link JSplitButton}) should be created.
     * @return A {@link ButtonItem} wrapping the provided {@link JMenuItem} type.
     * @param <I> The type parameter for the provided item type.
     */
    public static <I extends JMenuItem> ButtonItem<I> of(I item) {
        return new ButtonItem<>(item);
    }

    /**
     * @param item The {@link UIForMenuItem} which wraps a {@link  JMenuItem} for which a {@link ButtonItem} should be created.
     * @return A {@link ButtonItem} wrapping {@link JMenuItem} represented by the provided UI builder.
     */
    public static ButtonItem<JMenuItem> of(UIForMenuItem item) {
        return new ButtonItem<>(item.component);
    }

    private final I item;
    private final UIAction<I, ActionEvent> onButtonClick;
    private final Consumer<Context<I>> onItemSelected;

    private ButtonItem(I item ) {
        this.item = item; this.onButtonClick = null; this.onItemSelected = null;
    }
    private ButtonItem(I item, UIAction<I, ActionEvent> action, Consumer<Context<I>> onSelected ) {
        this.item = item; this.onButtonClick = action; this.onItemSelected = onSelected;
    }

    public I getItem() { return item; }

    /**
     *  Use this to register an action which will be called when the {@link JSplitButton}
     *  is being pressed and this {@link ButtonItem} was selected to be the primary button.
     *  <pre>{@code
     *      UI.splitButton("Hey!")
     *      .add(ButtonItem.saying("load"))
     *      .add(ButtonItem.saying("save").onClick( it -> doSaving() ))
     *      .add(ButtonItem.saying("delete"))
     *  }</pre>
     *
     * @param action The action lambda which will be called when the {@link JSplitButton} is being pressed
     *               and this {@link ButtonItem} was selected.
     * @return An immutable copy of this with the provided lambda set.
     */
    public ButtonItem<I> onClick(UIAction<I, ActionEvent> action) {
        if ( this.onButtonClick != null ) throw new IllegalArgumentException("Property already specified!");
        return new ButtonItem<>(item, action, onItemSelected);
    }

    /**
     *  Use this to perform some action when the user selects a {@link ButtonItem} among all other
     *  split button items.
     *  A common usecase would be to set the text of the {@link JSplitButton} by calling
     *  the {@link Context#getSplitButton()} method on the context object supplied to the
     *  provided action lambda like so:
     *  <pre>{@code
     *      UI.splitButton("Hey!")
     *      .add(ButtonItem.saying("first"))
     *      .add(ButtonItem.saying("second").onSelected( it -> it.getSplitButton().setText("Hey hey!") ))
     *      .add(ButtonItem.saying("third"))
     *  }</pre>
     *
     * @param action The action which will be called when the button was selected and which will
     *               receive some context information in the form of a {@link Context} instance.
     * @return An immutable copy of this with the provided lambda set.
     */
    public ButtonItem<I> onSelected(Consumer<Context<I>> action) {
        if ( this.onItemSelected != null ) throw new IllegalArgumentException("Property already specified!");
        return new ButtonItem<>(item, onButtonClick, action);
    }

    UIAction<I, ActionEvent> getOnClick() { return onButtonClick == null ? it->{} : onButtonClick; }

    Consumer<Context<I>> getOnSelected() {
        return onItemSelected == null
                ? c-> c.getSplitButton().setText(c.getItem().getText())
                : onItemSelected;
    }

    /**
     *  Instances of this are passed to the actions supplied to {@link #onSelected(Consumer)} in order
     *  to give said actions all the necessary context they need to perform whatever action they so desire.
     *
     * @param <I> The {@link JMenuItem} subtype for which this context was created.
     */
    public final static class Context<I extends JMenuItem>
    {
        private final EventContext<I, ActionEvent> eventContext;
        private final JSplitButton splitButton;
        private final Supplier<List<I>> siblingsSource;

        Context(EventContext<I, ActionEvent> eventContext, JSplitButton splitButton, Supplier<List<I>> siblingsSource) {
            this.eventContext = eventContext;
            this.splitButton = splitButton;
            this.siblingsSource = siblingsSource;
        }

        /**
         * @return The {@link EventContext} containing the event of this action as well as the component from which it originates.
         */
        public EventContext<I, ActionEvent> getEventContext() { return eventContext; }

        /**
         * @return The {@link JSplitButton} to which this {@link ButtonItem} (and its {@link JMenuItem}) belongs.
         */
        public JSplitButton getSplitButton() { return splitButton; }

        /**
         * @return The {@link JMenuItem} which caused this action to be executed.
         */
        public I getItem() { return eventContext.getComponent(); }

        /**
         *
         * @return A list of all the {@link JMenuItem} which constitute the options exposed by the {@link JSplitButton}.
         */
        public List<I> getSiblings() { return this.siblingsSource.get(); }
    }

}

