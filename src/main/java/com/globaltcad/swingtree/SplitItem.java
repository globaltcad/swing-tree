package com.globaltcad.swingtree;


import com.alexandriasoftware.swing.JSplitButton;
import com.globaltcad.swingtree.api.UIAction;
import com.globaltcad.swingtree.delegates.SimpleDelegate;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 *  {@link SplitItem}s represent button options for the {@link JSplitButton}
 *  which can be supplied to a split button through the API exposed by {@link UIForSplitButton}.
 *  <pre>{@code
 *      UI.splitButton("Hey!")
 *      .add(UI.splitItem("first"))
 *      .add(UI.splitItem("second").onClick( it -> ... ))
 *      .add(UI.splitItem("third").onClick( it -> ... ).onSelected( it -> ... ))
 *  }</pre>
 *
 * @param <I>
 */
public final class SplitItem<I extends JMenuItem>
{
    /**
     * @param text The text which should be displayed on the {@link SplitItem} (and its underlying {@link JMenuItem}).
     * @return A {@link SplitItem} wrapping a simple {@link JMenuItem} displaying the provided text.
     */
    public static SplitItem<JMenuItem> of(String text) {
        LogUtil.nullArgCheck(text, "text", String.class);
        return new SplitItem<>(new JMenuItem(text));
    }

    /**
     * @param item The {@link JMenuItem} subtype for which a {@link SplitItem} (for {@link JSplitButton}) should be created.
     * @return A {@link SplitItem} wrapping the provided {@link JMenuItem} type.
     * @param <I> The type parameter for the provided item type.
     */
    public static <I extends JMenuItem> SplitItem<I> of(I item) {
        LogUtil.nullArgCheck(item, "item", JMenuItem.class);
        return new SplitItem<>(item);
    }

    /**
     * @param item The {@link UIForMenuItem} which wraps a {@link  JMenuItem} for which a {@link SplitItem} should be created.
     * @return A {@link SplitItem} wrapping {@link JMenuItem} represented by the provided UI builder.
     */
    public static SplitItem<JMenuItem> of(UIForMenuItem item) {
        LogUtil.nullArgCheck(item, "item", UIForMenuItem.class);
        return new SplitItem<>(item.component);
    }

    private final I item;
    private final UIAction<Delegate<I>> onButtonClick;
    private final UIAction<Delegate<I>> onItemSelected;

    private SplitItem(I item ) {
        this.item = item; this.onButtonClick = null; this.onItemSelected = null;
    }

    private SplitItem(
            I item,
            UIAction<Delegate<I>> onClick,
            UIAction<Delegate<I>> onSelected
    ) {
        this.item = item; this.onButtonClick = onClick; this.onItemSelected = onSelected;
    }

    public SplitItem<I> makeSelected() {
        this.item.setSelected(true);
        return this;
    }

    /**
     *  Use this to register an action which will be called when the {@link JSplitButton}
     *  is being pressed and this {@link SplitItem} was selected to be the primary button.
     *  <pre>{@code
     *      UI.splitButton("Hey!")
     *      .add(UI.splitItem("load"))
     *      .add(UI.splitItem("save").onClick( it -> doSaving() ))
     *      .add(UI.splitItem("delete"))
     *  }</pre>
     *
     * @param action The action lambda which will be called when the {@link JSplitButton} is being pressed
     *               and this {@link SplitItem} was selected.
     * @return An immutable copy of this with the provided lambda set.
     */
    public SplitItem<I> onButtonClick(UIAction<Delegate<I>> action) {
        LogUtil.nullArgCheck(action, "action", UIAction.class);
        if ( this.onButtonClick != null )
            throw new IllegalArgumentException("Property already specified!");
        return new SplitItem<>(item, action, onItemSelected);
    }

    /**
     *  Use this to perform some action when the user selects a {@link SplitItem} among all other
     *  split button items.
     *  A common usecase would be to set the text of the {@link JSplitButton} by calling
     *  the {@link Delegate#getSplitButton()} method on the context object supplied to the
     *  provided action lambda like so:
     *  <pre>{@code
     *      UI.splitButton("Hey!")
     *      .add(UI.splitItem("first"))
     *      .add(UI.splitItem("second").onSelected( it -> it.getSplitButton().setText("Hey hey!") ))
     *      .add(UI.splitItem("third"))
     *  }</pre>
     *
     * @param action The action which will be called when the button was selected and which will
     *               receive some context information in the form of a {@link Delegate} instance.
     * @return An immutable copy of this with the provided lambda set.
     */
    public SplitItem<I> onSelection(UIAction<Delegate<I>> action) {
        LogUtil.nullArgCheck(action, "action", UIAction.class);
        if ( this.onItemSelected != null ) throw new IllegalArgumentException("Property already specified!");
        return new SplitItem<>(item, onButtonClick, action);
    }

    I getItem() { return item; }

    UIAction<Delegate<I>> getOnClick() { return onButtonClick == null ? it -> {} : onButtonClick; }

    UIAction<Delegate<I>> getOnSelected() { return onItemSelected == null ? c -> {} : onItemSelected; }

    /**
     *  Instances of this are exposed as delegates through the {@link SimpleDelegate} passed
     *  to the actions supplied to {@link #onSelection(UIAction)} in order
     *  to give said actions all the necessary context they need to perform whatever action they so desire.
     *
     * @param <I> The {@link JMenuItem} subtype for which this context was created.
     */
    public final static class Delegate<I extends JMenuItem>
    {
        private final ActionEvent event;
        private final JSplitButton splitButton;
        private final Supplier<List<I>> siblingsSource;
        private final I currentItem;

        Delegate(
                ActionEvent event,
                JSplitButton splitButton, 
                Supplier<List<I>> siblingsSource,
                I currentItem
        ) {
            this.event = event;
            this.splitButton = splitButton;
            this.siblingsSource = siblingsSource;
            this.currentItem = currentItem;
        }

        public ActionEvent getEvent() { return event; }

        /**
         * @return The {@link JSplitButton} to which this {@link SplitItem} (and its {@link JMenuItem}) belongs.
         */
        public JSplitButton getSplitButton() { return splitButton; }

        /**
         * @return The {@link JMenuItem} which caused this action to be executed.
         */
        public I getCurrentItem() { return this.currentItem; }

        /**
         *
         * @return A list of all the {@link JMenuItem} which constitute the options exposed by the {@link JSplitButton}.
         */
        public List<I> getSiblinghood() { return this.siblingsSource.get(); }

        /**
         *
         * @return A list of all the {@link JMenuItem} which constitute the options exposed by the {@link JSplitButton}
         *          except the current {@link JMenuItem} exposed by {@link #getCurrentItem()}.
         */
        public List<I> getSiblings() {
            return this.siblingsSource.get()
                    .stream()
                    .filter( s -> s != getCurrentItem() )
                    .collect(Collectors.toList());
        }

        /**
         *  Selects the current {@link JMenuItem} by passing {@code true}
         *  to the {@link JMenuItem#setSelected(boolean)} method.
         *
         * @return This {@link Delegate} instance to allow for method chaining.
         */
        public Delegate<I> selectCurrentItem() {
            this.getCurrentItem().setSelected(true);
            return this;
        }

        /**
         *  Selects only the current {@link JMenuItem} by passing {@code true}
         *  to the {@link JMenuItem#setSelected(boolean)} method.
         *  All other {@link JMenuItem}s will be unselected.
         *
         * @return This {@link Delegate} instance to allow for method chaining.
         */
        public Delegate<I> selectOnlyCurrentItem() {
            this.unselectAllItems();
            this.getCurrentItem().setSelected(true);
            return this;
        }

        /**
         *  Unselects the current {@link JMenuItem} by passing {@code false}
         *  to the {@link JMenuItem#setSelected(boolean)} method.
         *
         * @return This {@link Delegate} instance to allow for method chaining.
         */
        public Delegate<I> unselectCurrentItem() {
            this.getCurrentItem().setSelected(false);
            return this;
        }

        /**
         *  Unselects all {@link JMenuItem}s by passing {@code false}
         *  to their {@link JMenuItem#setSelected(boolean)} methods.
         *
         * @return This {@link Delegate} instance to allow for method chaining.
         */
        public Delegate<I> unselectAllItems() {
            getSiblinghood().forEach(it -> it.setSelected(false) );
            return this;
        }

        /**
         *  Selects all {@link JMenuItem}s by passing {@code true}
         *  to their {@link JMenuItem#setSelected(boolean)} methods.
         *
         * @return This {@link Delegate} instance to allow for method chaining.
         */
        public Delegate<I> selectAllItems() {
            getSiblinghood().forEach(it -> it.setSelected(true) );
            return this;
        }

        /**
         *  Use this to conveniently make the {@link JSplitButton} display the text
         *  of the currently selected {@link JMenuItem} (button item).
         *
         * @return This {@link Delegate} instance to allow for method chaining.
         */
        public Delegate<I> displayCurrentItemText() {
            if ( getCurrentItem() != null )
                getSplitButton().setText(getCurrentItem().getText());
            return this;
        }

        /**
         * @param text The text which should be displayed on the {@link JSplitButton}.
         * @return This {@link Delegate} instance to allow for method chaining.
         */
        public Delegate<I> setButtonText(String text ) {
            LogUtil.nullArgCheck(text, "text", String.class);
            this.splitButton.setText(text);
            return this;
        }

        /**
         * @return The text displayed on the {@link JSplitButton}.
         */
        public String getButtonText() {
            return this.splitButton.getText();
        }

        /**
         * @param postfix The text which should be appended to the text displayed on the {@link JSplitButton}.
         * @return This {@link Delegate} instance to allow for method chaining.
         */
        public Delegate<I> appendToButtonText( String postfix ) {
            LogUtil.nullArgCheck(postfix, "postfix", String.class);
            this.splitButton.setText(this.getButtonText()+postfix);
            return this;
        }

        /**
         * @param prefix The text which should be prepended to the text displayed on the {@link JSplitButton}.
         * @return This {@link Delegate} instance to allow for method chaining.
         */
        public Delegate<I> prependToButtonText( String prefix ) {
            LogUtil.nullArgCheck(prefix, "postfix", String.class);
            this.splitButton.setText(prefix+this.getButtonText());
            return this;
        }

    }

}

