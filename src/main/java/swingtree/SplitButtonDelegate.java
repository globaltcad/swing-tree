package swingtree;

import swingtree.components.JSplitButton;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class SplitButtonDelegate<I extends JMenuItem> extends AbstractDelegate<JSplitButton>
{
        private final SplitItemDelegate<I> _itemsDelegate;

        SplitButtonDelegate(
            JSplitButton button,
            SplitItemDelegate<I> itemsDelegate
        ) {
            super(false, button, button);
            _itemsDelegate  = Objects.requireNonNull(itemsDelegate);
        }

        public ActionEvent getEvent() { return _itemsDelegate.getEvent(); }

        /**
         *  Exposes the underlying {@link SplitItemDelegate} instance.
         * @return The {@link JSplitButton} to which this {@link SplitItem} (and its {@link JMenuItem}) belongs.
         */
        public JSplitButton getSplitButton() {
            return _itemsDelegate.getSplitButton();
        }

        /**
         * @return A list of all split button items.
         */
        List<I> getItems() {
            return _itemsDelegate.getSiblinghood();
        }

        /**
         * @return The {@link JMenuItem} which caused this action to be executed.
         */
        public final I getCurrentItem() {
            return _itemsDelegate.getCurrentItem();
        }

        /**
         *
         * @return A list of all the {@link JComponent} siblings of the split button, including the split button itself.
         */
        public List<JComponent> getSiblinghood() {
            // We make sure that only the Swing thread can access the sibling components:
            if ( !UI.thisIsUIThread() )
                throw new IllegalStateException(
                        "Sibling components can only be accessed by the Swing thread."
                    );
            return _siblingsSource();
        }

        /**
         * @return A list of all the {@link JComponent} which constitute the neighbouring UI components of the split button.
         *          except the current {@link JSplitButton} itself.
         */
        public List<JComponent> getSiblings() {
            // We make sure that only the Swing thread can access the sibling components:
            if ( !UI.thisIsUIThread() )
                throw new IllegalStateException(
                        "Sibling components can only be accessed by the Swing thread."
                    );
            return _siblingsSource()
                    .stream()
                    .filter( s -> s != getCurrentItem() )
                    .collect(Collectors.toList());
        }

        /**
         *  Selects the current {@link JMenuItem} by passing {@code true}
         *  to the {@link JMenuItem#setSelected(boolean)} method.
         *
         * @return This delegate instance to allow for method chaining.
         */
        public SplitButtonDelegate<I> selectCurrentItem() {
            _itemsDelegate.selectCurrentItem();
            return this;
        }

        /**
         *  Selects only the current {@link JMenuItem} by passing {@code true}
         *  to the {@link JMenuItem#setSelected(boolean)} method.
         *  All other {@link JMenuItem}s will be unselected.
         *
         * @return This delegate instance to allow for method chaining.
         */
        public SplitButtonDelegate<I> selectOnlyCurrentItem() {
            _itemsDelegate.selectOnlyCurrentItem();
            return this;
        }

        /**
         *  Unselects the current {@link JMenuItem} by passing {@code false}
         *  to the {@link JMenuItem#setSelected(boolean)} method.
         *
         * @return This delegate instance to allow for method chaining.
         */
        public SplitButtonDelegate<I> unselectCurrentItem() {
            _itemsDelegate.unselectCurrentItem();
            return this;
        }

        /**
         *  Unselects all {@link JMenuItem}s by passing {@code false}
         *  to their {@link JMenuItem#setSelected(boolean)} methods.
         *
         * @return This delegate instance to allow for method chaining.
         */
        public SplitButtonDelegate<I> unselectAllItems() {
            _itemsDelegate.unselectAllItems();
            return this;
        }

        /**
         *  Selects all {@link JMenuItem}s by passing {@code true}
         *  to their {@link JMenuItem#setSelected(boolean)} methods.
         *
         * @return This delegate instance to allow for method chaining.
         */
        public SplitButtonDelegate<I> selectAllItems() {
            _itemsDelegate.selectAllItems();
            return this;
        }

        /**
         *  Use this to conveniently make the {@link JSplitButton} display the text
         *  of the currently selected {@link JMenuItem} (button item).
         *
         * @return This delegate instance to allow for method chaining.
         */
        public SplitButtonDelegate<I> displayCurrentItemText() {
            _itemsDelegate.displayCurrentItemText();
            return this;
        }

        /**
         * @param text The text which should be displayed on the {@link JSplitButton}.
         * @return This delegate instance to allow for method chaining.
         */
        public SplitButtonDelegate<I> setButtonText( String text ) {
            _itemsDelegate.setButtonText(text);
            return this;
        }

        /**
         * @return The text displayed on the {@link JSplitButton}.
         */
        public String getButtonText() {
            return _itemsDelegate.getButtonText();
        }

        /**
         * @param postfix The text which should be appended to the text displayed on the {@link JSplitButton}.
         * @return This delegate instance to allow for method chaining.
         */
        public SplitButtonDelegate<I> appendToButtonText( String postfix ) {
            _itemsDelegate.appendToButtonText(postfix);
            return this;
        }

        /**
         * @param prefix The text which should be prepended to the text displayed on the {@link JSplitButton}.
         * @return This delegate instance to allow for method chaining.
         */
        public SplitButtonDelegate<I> prependToButtonText( String prefix ) {
            _itemsDelegate.prependToButtonText(prefix);
            return this;
        }

        /**
         *  Selects the targeted split item ({@link JMenuItem}).
         *
         * @param i The item index of the {@link JMenuItem} which should be selected.
         * @return This delegate instance to allow for method chaining.
         */
        public SplitButtonDelegate<I> selectItem( int i ) {
            _itemsDelegate.selectItem(i);
            return this;
        }

        /**
         *  Selects the targeted split item ({@link JMenuItem}) and unselects all other items.
         *
         * @param i The item index of the {@link JMenuItem} which should be selected exclusively.
         * @return This delegate instance to allow for method chaining.
         */
        public SplitButtonDelegate<I> selectOnlyItem( int i ) {
            _itemsDelegate.selectOnlyItem(i);
            return this;
        }

        /**
         *  Unselects the targeted split item ({@link JMenuItem}).
         *
         * @param i The item index of the {@link JMenuItem} which should be unselected.
         * @return This delegate instance to allow for method chaining.
         */
        public SplitButtonDelegate<I> unselectItem( int i ) {
            _itemsDelegate.unselectItem(i);
            return this;
        }
}
