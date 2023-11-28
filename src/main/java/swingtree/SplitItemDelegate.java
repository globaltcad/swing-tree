package swingtree;

import sprouts.Action;
import swingtree.components.JSplitButton;

import javax.swing.JMenuItem;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Instances of this are delegated to the individual {@link JSplitButton} items
 * and their {@link ActionEvent}s exposed to you inside your {@link Action} handlers,
 * like for example one you would supply to {@link SplitItem#onSelection(Action)}.
 * This class exists to give said actions all the necessary context
 * they need to perform their tasks.
 *
 * @param <I> The {@link JMenuItem} subtype for which this context was created.
 */
public final class SplitItemDelegate<I extends JMenuItem> extends AbstractDelegate<I> {
    private final ActionEvent event;
    private final JSplitButton splitButton;
    private final Supplier<List<I>> siblingsSource;

    SplitItemDelegate(
            ActionEvent event,
            JSplitButton splitButton,
            Supplier<List<I>> siblingsSource,
            I currentItem
    ) {
        super(true, currentItem, splitButton);
        this.event = Objects.requireNonNull(event);
        this.splitButton = Objects.requireNonNull(splitButton);
        this.siblingsSource = Objects.requireNonNull(siblingsSource);
    }

    /**
     * @return The {@link ActionEvent} which caused this action to be executed.
     */
    public ActionEvent getEvent() {
        return event;
    }

    /**
     * @return The {@link JSplitButton} to which this {@link SplitItem} (and its {@link JMenuItem}) belongs.
     */
    public JSplitButton getSplitButton() {
        // We make sure that only the Swing thread can access the component:
        if (UI.thisIsUIThread()) return splitButton;
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
        if (UI.thisIsUIThread()) return _component();
        else
            throw new IllegalStateException(
                    "The current button item can only be accessed by the Swing thread."
            );
    }

    /**
     * @return A list of all the {@link JMenuItem} which constitute the options exposed by the {@link JSplitButton}.
     */
    public List<I> getSiblinghood() {
        // We make sure that only the Swing thread can access the sibling components:
        if (!UI.thisIsUIThread())
            throw new IllegalStateException(
                    "Sibling components can only be accessed by the Swing thread."
            );
        return this.siblingsSource.get();
    }

    /**
     * @return A list of all the {@link JMenuItem} which constitute the options exposed by the {@link JSplitButton}
     * except the current {@link JMenuItem} exposed by {@link #getCurrentItem()}.
     */
    public List<I> getSiblings() {
        // We make sure that only the Swing thread can access the sibling components:
        if (!UI.thisIsUIThread())
            throw new IllegalStateException(
                    "Sibling components can only be accessed by the Swing thread."
            );
        return this.siblingsSource.get()
                .stream()
                .filter(s -> s != getCurrentItem())
                .collect(Collectors.toList());
    }

    /**
     * Selects the current {@link JMenuItem} by passing {@code true}
     * to the {@link JMenuItem#setSelected(boolean)} method.
     *
     * @return This {@link SplitItemDelegate} instance to allow for method chaining.
     */
    public SplitItemDelegate<I> selectCurrentItem() {
        // We make sure that only the Swing thread can modify components:
        if (!UI.thisIsUIThread()) {
            UI.run(this::selectCurrentItem);
            return this;
        }
        this.getCurrentItem().setSelected(true);
        return this;
    }

    /**
     * Selects only the current {@link JMenuItem} by passing {@code true}
     * to the {@link JMenuItem#setSelected(boolean)} method.
     * All other {@link JMenuItem}s will be unselected.
     *
     * @return This {@link SplitItemDelegate} instance to allow for method chaining.
     */
    public SplitItemDelegate<I> selectOnlyCurrentItem() {
        // We make sure that only the Swing thread can modify components:
        if (!UI.thisIsUIThread()) {
            UI.run(this::selectOnlyCurrentItem);
            return this;
        }
        this.unselectAllItems();
        this.getCurrentItem().setSelected(true);
        return this;
    }

    /**
     * Unselects the current {@link JMenuItem} by passing {@code false}
     * to the {@link JMenuItem#setSelected(boolean)} method.
     *
     * @return This {@link SplitItemDelegate} instance to allow for method chaining.
     */
    public SplitItemDelegate<I> unselectCurrentItem() {
        // We make sure that only the Swing thread can modify components:
        if (!UI.thisIsUIThread()) {
            UI.run(this::unselectCurrentItem);
            return this;
        }
        this.getCurrentItem().setSelected(false);
        return this;
    }

    /**
     * Unselects all {@link JMenuItem}s by passing {@code false}
     * to their {@link JMenuItem#setSelected(boolean)} methods.
     *
     * @return This {@link SplitItemDelegate} instance to allow for method chaining.
     */
    public SplitItemDelegate<I> unselectAllItems() {
        // We make sure that only the Swing thread can modify components:
        if (!UI.thisIsUIThread()) {
            UI.run(this::unselectAllItems);
            return this;
        }
        getSiblinghood().forEach(it -> it.setSelected(false));
        return this;
    }

    /**
     * Selects all {@link JMenuItem}s by passing {@code true}
     * to their {@link JMenuItem#setSelected(boolean)} methods.
     *
     * @return This {@link SplitItemDelegate} instance to allow for method chaining.
     */
    public SplitItemDelegate<I> selectAllItems() {
        // We make sure that only the Swing thread can modify components:
        if (!UI.thisIsUIThread()) {
            UI.run(this::selectAllItems);
            return this;
        }
        getSiblinghood().forEach(it -> it.setSelected(true));
        return this;
    }

    /**
     * Use this to conveniently make the {@link JSplitButton} display the text
     * of the currently selected {@link JMenuItem} (button item).
     *
     * @return This {@link SplitItemDelegate} instance to allow for method chaining.
     */
    public SplitItemDelegate<I> displayCurrentItemText() {
        // We make sure that only the Swing thread can modify components:
        if (!UI.thisIsUIThread()) {
            UI.run(this::displayCurrentItemText);
            return this;
        }
        if (getCurrentItem() != null)
            getSplitButton().setText(getCurrentItem().getText());
        return this;
    }

    /**
     * @param text The text which should be displayed on the {@link JSplitButton}.
     * @return This {@link SplitItemDelegate} instance to allow for method chaining.
     */
    public SplitItemDelegate<I> setButtonText(String text) {
        // We make sure that only the Swing thread can modify components:
        if (!UI.thisIsUIThread()) {
            UI.run(() -> setButtonText(text));
            return this;
        }
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
     * @return This {@link SplitItemDelegate} instance to allow for method chaining.
     */
    public SplitItemDelegate<I> appendToButtonText(String postfix) {
        NullUtil.nullArgCheck(postfix, "postfix", String.class);
        // We make sure that only the Swing thread can modify components:
        if (!UI.thisIsUIThread()) {
            UI.run(() -> appendToButtonText(postfix));
            return this;
        }
        this.splitButton.setText(this.getButtonText() + postfix);
        return this;
    }

    /**
     * @param prefix The text which should be prepended to the text displayed on the {@link JSplitButton}.
     * @return This {@link SplitItemDelegate} instance to allow for method chaining.
     */
    public SplitItemDelegate<I> prependToButtonText(String prefix) {
        NullUtil.nullArgCheck(prefix, "postfix", String.class);
        // We make sure that only the Swing thread can modify components:
        if (!UI.thisIsUIThread()) {
            UI.run(() -> prependToButtonText(prefix));
            return this;
        }
        this.splitButton.setText(prefix + this.getButtonText());
        return this;
    }

    /**
     * Selects the targeted split item ({@link JMenuItem}).
     *
     * @param i The item index of the {@link JMenuItem} which should be selected.
     * @return This {@link SplitItemDelegate} instance to allow for method chaining.
     */
    public SplitItemDelegate<I> selectItem(int i) {
        // We make sure that only the Swing thread can modify components:
        if (!UI.thisIsUIThread()) {
            UI.run(() -> selectItem(i));
            return this;
        }
        getSiblinghood().get(i).setSelected(true);
        return this;
    }

    /**
     * Selects the targeted split item ({@link JMenuItem}) and unselects all other items.
     *
     * @param i The item index of the {@link JMenuItem} which should be selected exclusively.
     * @return This {@link SplitItemDelegate} instance to allow for method chaining.
     */
    public SplitItemDelegate<I> selectOnlyItem(int i) {
        // We make sure that only the Swing thread can modify components:
        if (!UI.thisIsUIThread()) {
            UI.run(() -> selectOnlyItem(i));
            return this;
        }
        unselectAllItems().getSiblinghood().get(i).setSelected(true);
        return this;
    }

    /**
     * Unselects the targeted split item ({@link JMenuItem}).
     *
     * @param i The item index of the {@link JMenuItem} which should be unselected.
     * @return This {@link SplitItemDelegate} instance to allow for method chaining.
     */
    public SplitItemDelegate<I> unselectItem(int i) {
        // We make sure that only the Swing thread can modify components:
        if (!UI.thisIsUIThread()) {
            UI.run(() -> unselectItem(i));
            return this;
        }
        getSiblinghood().get(i).setSelected(false);
        return this;
    }

}
