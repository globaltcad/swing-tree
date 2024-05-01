package swingtree;


import org.jspecify.annotations.Nullable;
import sprouts.Action;
import sprouts.Val;
import sprouts.Var;
import swingtree.components.JSplitButton;

import javax.swing.*;
import java.util.Objects;
import java.util.Optional;

/**
 *  An immutable data carrier exposing everything needed to configure an item of a {@link JSplitButton}.
 *  {@link SplitItem}s will be turned into button options for the {@link JSplitButton}
 *  which can be supplied to a split button through the builder API exposed by {@link UIForSplitButton} like so:
 *  <pre>{@code
 *      UI.splitButton("Hey!")
 *      .add(UI.splitItem("first"))
 *      .add(UI.splitItem("second").onClick( it -> ... ))
 *      .add(UI.splitItem("third").onClick( it -> ... ).onSelected( it -> ... ))
 *  }</pre>
 * 	<p>
 * 	<b>For more information, please take a look at the
 * 	<a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 *
 * @param <I> The type of the item which will be passed to the {@link Action}s.
 */
public final class SplitItem<I extends JMenuItem>
{
    /**
     *  A factory method to create a {@link SplitItem} for a {@link JSplitButton} from a simple text string
     *  which will be displayed on the {@link SplitItem} when it is part of a clicked {@link JSplitButton}.
     *
     * @param text The text which should be displayed on the {@link SplitItem} (and its underlying {@link JMenuItem}).
     * @return A {@link SplitItem} wrapping a simple {@link JMenuItem} displaying the provided text.
     */
    public static SplitItem<JMenuItem> of( String text ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        return new SplitItem<>(new JMenuItem(text));
    }

    /**
     *  A factory method to create a {@link SplitItem} for a {@link JSplitButton} from a {@link Val} property
     *  which will be used to dynamically determine the text displayed on the {@link SplitItem}.
     *
     * @param text The text property whose text should be displayed on the {@link SplitItem} (and its underlying {@link JMenuItem}).
     *             When the text of the property changes, then the text of the {@link JMenuItem} will be updated accordingly.
     * @return A {@link SplitItem} wrapping a simple {@link JMenuItem} displaying the provided text.
     */
    public static SplitItem<JMenuItem> of( Val<String> text ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        return new SplitItem<>(UI.of(new JMenuItem()).withText(text).getComponent());
    }

    /**
     *  A factory method to create a {@link SplitItem} for a {@link JSplitButton} from a {@link JMenuItem} subtype.
     *
     * @param item The {@link JMenuItem} subtype for which a {@link SplitItem} (for {@link JSplitButton}) should be created.
     * @return A {@link SplitItem} wrapping the provided {@link JMenuItem} type.
     * @param <I> The type parameter for the provided item type.
     */
    public static <I extends JMenuItem> SplitItem<I> of( I item ) {
        NullUtil.nullArgCheck(item, "item", JMenuItem.class);
        return new SplitItem<>(item);
    }

    /**
     *  Use this to create a {@link SplitItem} for a {@link JSplitButton} from a {@link UIForMenuItem}
     *  UI declaration.
     *  This is useful when you want to create a {@link SplitItem} from a {@link JMenuItem} which is configured
     *  using the declarative UI builder API exposed by {@link UIForMenuItem}.
     *  See {@link UI#menuItem()} for more information.
     *
     * @param item The {@link UIForMenuItem} which wraps a {@link  JMenuItem} for which a {@link SplitItem} should be created.
     * @param <M> The type parameter for the provided item type, a subtype of {@link JMenuItem}.
     * @return A {@link SplitItem} wrapping {@link JMenuItem} represented by the provided UI builder.
     */
    public static <M extends JMenuItem> SplitItem<M> of( UIForMenuItem<M> item ) {
        NullUtil.nullArgCheck(item, "item", UIForMenuItem.class);
        return new SplitItem<>(item.getComponent());
    }

    private final I _item;
    private final @Nullable Action<SplitItemDelegate<I>> _onButtonClick;
    private final @Nullable Action<SplitItemDelegate<I>> _onItemSelected;
    private final @Nullable Val<Boolean> _isEnabled;


    private SplitItem( I item ) {
        _item           = Objects.requireNonNull(item);
        _onButtonClick  = null;
        _onItemSelected = null;
        _isEnabled      = null;
    }

    private SplitItem(
        I item,
        @Nullable Action<SplitItemDelegate<I>> onClick,
        @Nullable Action<SplitItemDelegate<I>> onSelected,
        @Nullable Val<Boolean> isEnabled
    ) {
        _item           = Objects.requireNonNull(item);
        _onButtonClick  = onClick;
        _onItemSelected = onSelected;
        _isEnabled      = isEnabled;
    }

    /**
     *  Sets the {@link JMenuItem#setSelected(boolean)} flag of the underlying {@link JMenuItem} to {@code true}.
     *
     * @return An immutable copy of this with the provided text set.
     */
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
    public SplitItem<I> onButtonClick( Action<SplitItemDelegate<I>> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        if ( _onButtonClick != null )
            throw new IllegalArgumentException("Property already specified!");
        return new SplitItem<>(_item, action, _onItemSelected, _isEnabled);
    }

    /**
     *  Use this to perform some action when the user selects a {@link SplitItem} among all other
     *  split button items.
     *  A common use case would be to set the text of the {@link JSplitButton} by calling
     *  the {@link SplitItemDelegate#getSplitButton()} method on the context object supplied to the
     *  provided action lambda like so:
     *  <pre>{@code
     *      UI.splitButton("Hey!")
     *      .add(UI.splitItem("first"))
     *      .add(UI.splitItem("second").onSelected( it -> it.getSplitButton().setText("Hey hey!") ))
     *      .add(UI.splitItem("third"))
     *  }</pre>
     *
     * @param action The action which will be called when the button was selected and which will
     *               receive some context information in the form of a {@link SplitItemDelegate} instance.
     * @return An immutable copy of this with the provided lambda set.
     */
    public SplitItem<I> onSelection( Action<SplitItemDelegate<I>> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        if ( _onItemSelected != null ) throw new IllegalArgumentException("Property already specified!");
        return new SplitItem<>(_item, _onButtonClick, action, _isEnabled);
    }

    /**
     *  Dynamically determines whether this {@link SplitItem} is enabled or not based on the value of the provided
     *  observable boolean property. This means that whenever the value of the property changes, the enabled state
     *  of this {@link SplitItem} will change accordingly. This is done by calling {@link JMenuItem#setEnabled(boolean)}
     *  on the underlying {@link JMenuItem} with the value of the property.
     *
     * @param isEnabled An observable boolean property which will dynamically determine whether this {@link SplitItem}
     *                  is enabled or not. So when the property changes, the enabled state of this {@link SplitItem}
     *                  will be updated accordingly.
     * @return An immutable copy of this with the provided property set.
     */
    public SplitItem<I> isEnabledIf( Var<Boolean> isEnabled ) {
        NullUtil.nullArgCheck(isEnabled, "isEnabled", Var.class);
        if ( _isEnabled != null ) throw new IllegalArgumentException("Property already specified!");
        return new SplitItem<>(_item, _onButtonClick, _onItemSelected, isEnabled);
    }

    I getItem() { return _item; }

    Action<SplitItemDelegate<I>> getOnClick() { return _onButtonClick == null ? it -> {} : _onButtonClick; }

    Action<SplitItemDelegate<I>> getOnSelected() { return _onItemSelected == null ? c -> {} : _onItemSelected; }

    Optional<Val<Boolean>> getIsEnabled() { return Optional.ofNullable(_isEnabled); }

}

