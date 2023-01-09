package swingtree;


import com.alexandriasoftware.swing.JSplitButton;
import swingtree.api.mvvm.Action;
import swingtree.api.mvvm.Val;
import swingtree.api.mvvm.Var;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 *  An immutable data carrier exposing everything needed to configure an item of a {@link JSplitButton}.
 *  {@link SplitItem}s will be turned into button options for the {@link JSplitButton}
 *  which can be supplied to a split button through the API exposed by {@link UIForSplitButton} like so:
 *  <pre>{@code
 *      UI.splitButton("Hey!")
 *      .add(UI.splitItem("first"))
 *      .add(UI.splitItem("second").onClick( it -> ... ))
 *      .add(UI.splitItem("third").onClick( it -> ... ).onSelected( it -> ... ))
 *  }</pre>
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 *
 * @param <I> The type of the item which will be passed to the {@link Action}s.
 */
public final class SplitItem<I extends JMenuItem>
{
    /**
     * @param text The text which should be displayed on the {@link SplitItem} (and its underlying {@link JMenuItem}).
     * @return A {@link SplitItem} wrapping a simple {@link JMenuItem} displaying the provided text.
     */
    public static SplitItem<JMenuItem> of( String text ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        return new SplitItem<>(new JMenuItem(text));
    }

    /**
     * @param text The text which should be displayed on the {@link SplitItem} (and its underlying {@link JMenuItem}).
     * @return A {@link SplitItem} wrapping a simple {@link JMenuItem} displaying the provided text.
     */
    public static SplitItem<JMenuItem> of( Val<String> text ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        return new SplitItem<>(UI.of(new JMenuItem()).withText(text).getComponent());
    }

    /**
     * @param item The {@link JMenuItem} subtype for which a {@link SplitItem} (for {@link JSplitButton}) should be created.
     * @return A {@link SplitItem} wrapping the provided {@link JMenuItem} type.
     * @param <I> The type parameter for the provided item type.
     */
    public static <I extends JMenuItem> SplitItem<I> of( I item ) {
        NullUtil.nullArgCheck(item, "item", JMenuItem.class);
        return new SplitItem<>(item);
    }

    /**
     * @param item The {@link UIForMenuItem} which wraps a {@link  JMenuItem} for which a {@link SplitItem} should be created.
     * @return A {@link SplitItem} wrapping {@link JMenuItem} represented by the provided UI builder.
     */
    public static <M extends JMenuItem> SplitItem<M> of( UIForMenuItem<M> item ) {
        NullUtil.nullArgCheck(item, "item", UIForMenuItem.class);
        return new SplitItem<>(item.getComponent());
    }

    private final I _item;
    private final Action<Delegate<I>> _onButtonClick;
    private final Action<Delegate<I>> _onItemSelected;
    private final Val<Boolean> _isEnabled;

    private SplitItem( I item ) {
        _item = item; _onButtonClick = null; _onItemSelected = null; _isEnabled = null;
    }

    private SplitItem(
        I item,
        Action<Delegate<I>> onClick,
        Action<Delegate<I>> onSelected,
        Val<Boolean> isEnabled
    ) {
        _item = item; _onButtonClick = onClick; _onItemSelected = onSelected; _isEnabled = isEnabled;
    }

    public SplitItem<I> makeSelected() {
        _item.setSelected(true);
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
    public SplitItem<I> onButtonClick(Action<Delegate<I>> action) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        if ( _onButtonClick != null )
            throw new IllegalArgumentException("Property already specified!");
        return new SplitItem<>(_item, action, _onItemSelected, _isEnabled);
    }

    /**
     *  Use this to perform some action when the user selects a {@link SplitItem} among all other
     *  split button items.
     *  A common use case would be to set the text of the {@link JSplitButton} by calling
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
    public SplitItem<I> onSelection(Action<Delegate<I>> action) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        if ( _onItemSelected != null ) throw new IllegalArgumentException("Property already specified!");
        return new SplitItem<>(_item, _onButtonClick, action, _isEnabled);
    }

    public SplitItem<I> isEnabledIf( Var<Boolean> isEnabled ) {
        NullUtil.nullArgCheck(isEnabled, "isEnabled", Var.class);
        if ( _isEnabled != null ) throw new IllegalArgumentException("Property already specified!");
        return new SplitItem<>(_item, _onButtonClick, _onItemSelected, isEnabled);
    }

    I getItem() { return _item; }

    Action<Delegate<I>> getOnClick() { return _onButtonClick == null ? it -> {} : _onButtonClick; }

    Action<Delegate<I>> getOnSelected() { return _onItemSelected == null ? c -> {} : _onItemSelected; }

    Val<Boolean> getIsEnabled(){ return _isEnabled; }

    /**
     *  Instances of this are exposed as delegates through the {@link SimpleDelegate} passed
     *  to the actions supplied to {@link #onSelection(Action)} in order
     *  to give said actions all the necessary context they need to perform whatever action they so desire.
     *
     * @param <I> The {@link JMenuItem} subtype for which this context was created.
     */
    public final static class Delegate<I extends JMenuItem> extends AbstractDelegate
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
            super(currentItem);
            this.event = event;
            this.splitButton = splitButton;
            this.siblingsSource = siblingsSource;
            this.currentItem = currentItem;
        }

        public ActionEvent getEvent() { return event; }

        /**
         * @return The {@link JSplitButton} to which this {@link SplitItem} (and its {@link JMenuItem}) belongs.
         */
        public JSplitButton getSplitButton() {
            // We make sure that only the Swing thread can access the component:
            if ( UI.thisIsUIThread() ) return splitButton;
            else
                throw new IllegalStateException(
                        "Split button can only be accessed by the Swing thread."
                    );
        }

        /**
         * @return The {@link JMenuItem} which caused this action to be executed.
         */
        public final I getCurrentItem() {
            // We make sure that only the Swing thread can access the component:
            if ( UI.thisIsUIThread() ) return this.currentItem;
            else
                throw new IllegalStateException(
                        "The current button item can only be accessed by the Swing thread."
                    );
        }

        /**
         *
         * @return A list of all the {@link JMenuItem} which constitute the options exposed by the {@link JSplitButton}.
         */
        public List<I> getSiblinghood() {
            // We make sure that only the Swing thread can access the sibling components:
            if ( !UI.thisIsUIThread() )
                throw new IllegalStateException(
                        "Sibling components can only be accessed by the Swing thread."
                    );
            return this.siblingsSource.get();
        }

        /**
         *
         * @return A list of all the {@link JMenuItem} which constitute the options exposed by the {@link JSplitButton}
         *          except the current {@link JMenuItem} exposed by {@link #getCurrentItem()}.
         */
        public List<I> getSiblings() {
            // We make sure that only the Swing thread can access the sibling components:
            if ( !UI.thisIsUIThread() )
                throw new IllegalStateException(
                        "Sibling components can only be accessed by the Swing thread."
                    );
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
            // We make sure that only the Swing thread can modify components:
            if ( !UI.thisIsUIThread() )
                UI.run(this::selectCurrentItem);
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
            // We make sure that only the Swing thread can modify components:
            if ( !UI.thisIsUIThread() )
                UI.run(this::selectOnlyCurrentItem);
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
            // We make sure that only the Swing thread can modify components:
            if ( !UI.thisIsUIThread() )
                UI.run(this::unselectCurrentItem);
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
            // We make sure that only the Swing thread can modify components:
            if ( !UI.thisIsUIThread() )
                UI.run(this::unselectAllItems);
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
            // We make sure that only the Swing thread can modify components:
            if ( !UI.thisIsUIThread() )
                UI.run(this::selectAllItems);
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
            // We make sure that only the Swing thread can modify components:
            if ( !UI.thisIsUIThread() )
                UI.run(this::displayCurrentItemText);
            if ( getCurrentItem() != null )
                getSplitButton().setText(getCurrentItem().getText());
            return this;
        }

        /**
         * @param text The text which should be displayed on the {@link JSplitButton}.
         * @return This {@link Delegate} instance to allow for method chaining.
         */
        public Delegate<I> setButtonText( String text ) {
            // We make sure that only the Swing thread can modify components:
            if ( !UI.thisIsUIThread() )
                UI.run(() -> setButtonText(text) );
            NullUtil.nullArgCheck(text, "text", String.class);
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
            NullUtil.nullArgCheck(postfix, "postfix", String.class);
            // We make sure that only the Swing thread can modify components:
            if ( !UI.thisIsUIThread() )
                UI.run(() -> appendToButtonText(postfix) );
            this.splitButton.setText(this.getButtonText()+postfix);
            return this;
        }

        /**
         * @param prefix The text which should be prepended to the text displayed on the {@link JSplitButton}.
         * @return This {@link Delegate} instance to allow for method chaining.
         */
        public Delegate<I> prependToButtonText( String prefix ) {
            NullUtil.nullArgCheck(prefix, "postfix", String.class);
            // We make sure that only the Swing thread can modify components:
            if ( !UI.thisIsUIThread() )
                UI.run(() -> prependToButtonText(prefix) );
            this.splitButton.setText(prefix+this.getButtonText());
            return this;
        }

        /**
         *  Selects the targeted split item ({@link JMenuItem}).
         *
         * @param i The item index of the {@link JMenuItem} which should be selected.
         * @return This {@link Delegate} instance to allow for method chaining.
         */
        public Delegate<I> selectItem( int i ) {
            // We make sure that only the Swing thread can modify components:
            if ( !UI.thisIsUIThread() )
                UI.run(() -> selectItem(i) );
            getSiblinghood().get(i).setSelected(true);
            return this;
        }

        /**
         *  Selects the targeted split item ({@link JMenuItem}) and unselects all other items.
         *
         * @param i The item index of the {@link JMenuItem} which should be selected exclusively.
         * @return This {@link Delegate} instance to allow for method chaining.
         */
        public Delegate<I> selectOnlyItem( int i ) {
            // We make sure that only the Swing thread can modify components:
            if ( !UI.thisIsUIThread() )
                UI.run(() -> selectOnlyItem(i) );
            unselectAllItems().getSiblinghood().get(i).setSelected(true);
            return this;
        }

        /**
         *  Unselects the targeted split item ({@link JMenuItem}).
         *
         * @param i The item index of the {@link JMenuItem} which should be unselected.
         * @return This {@link Delegate} instance to allow for method chaining.
         */
        public Delegate<I> unselectItem( int i ) {
            // We make sure that only the Swing thread can modify components:
            if ( !UI.thisIsUIThread() )
                UI.run(() -> unselectItem(i) );
            getSiblinghood().get(i).setSelected(false);
            return this;
        }

    }

}

