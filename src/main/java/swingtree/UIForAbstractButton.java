package swingtree;

import sprouts.Action;
import sprouts.Val;
import sprouts.Var;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.function.Consumer;

/**
 *  The following is a more specialized type of builder node based on the {@link UIForAbstractSwing} builder type,
 *  and provides additional features associated with the more specialized
 *  {@link AbstractButton}" Swing component type.
 *  One of such features is the {@link #onClick(Action)} method allowing for a more readable way of adding
 *  {@link ActionListener} instances to button types...
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 *  <br><br>
 *
 * @param <B> The type parameter for the component wrapped by an instance of this class.
 */
public abstract class UIForAbstractButton<I, B extends AbstractButton> extends UIForAbstractSwing<I, B>
{
    protected UIForAbstractButton( B component ) { super(component); }

    /**
     * Defines the single line of text the wrapped button type will display.
     * If the value of text is null or empty string, nothing is displayed.
     *
     * @param text The new text to be set for the wrapped button type.
     * @return This very builder to allow for method chaining.
     */
    public final I withText( String text ) {
        getComponent().setText(text);
        return _this();
    }

    /**
     *  Binds the provided {@link Val} property to the wrapped button type
     *  and sets the text of the button to the value of the property.
     * <i>Hint: Use {@code myProperty.fireSet()} in your view model to send the property value to this view component.</i>
     * @param text The view model property which should be bound to this UI.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code text} is {@code null}.
     */
    public final I withText( Val<String> text ) {
        NullUtil.nullArgCheck(text, "val", Val.class);
        NullUtil.nullPropertyCheck(text, "text");
        _onShow(text, v -> getComponent().setText(v) );
        return withText( text.orElseThrow() );
    }
    
    /**
     *  Use this to set the icon for the wrapped button type. 
     *  This is in essence a convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.button("Something")
     *         .peek( button -> button.setIcon(...) );
     *  }</pre>
     *
     *
     * @param icon The {@link Icon} which should be displayed on the button.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code icon} is {@code null}.
     */
    public I with( Icon icon ) {
        NullUtil.nullArgCheck(icon,"icon",Icon.class);
        getComponent().setIcon(icon);
        return _this();
    }

    /**
     *  Use this to dynamically set the icon property for the wrapped button type.
     *  When the icon wrapped by the provided property changes,
     *  then so does the icon displayed on this button.
     *
     * @param icon The {@link Icon} property which should be displayed on the button.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code icon} is {@code null}.
     */
    public I withIcon( Val<Icon> icon ) {
        NullUtil.nullArgCheck(icon, "icon", Val.class);
        NullUtil.nullPropertyCheck(icon, "icon");
        _onShow( icon, i -> getComponent().setIcon(i) );
        return with(icon.orElseThrow());
    }

    /**
     *  Use this to set the size of the font of the wrapped button type.
     * @param size The size of the font which should be displayed on the button.
     * @return This very builder to allow for method chaining.
     */
    public I withFontSize( int size ) {
        B button = getComponent();
        Font old = button.getFont();
        button.setFont(new Font(old.getName(), old.getStyle(), size));
        return _this();
    }

    /**
     *  Use this to dynamically set the size of the font of the wrapped button type
     *  through the provided view model property.
     *  When the integer wrapped by the provided property changes,
     *  then so does the font size of the text displayed on this button.
     *
     * @param size The size property of the font which should be displayed on the button.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code size} is {@code null}.
     */
    public I withFontSize( Val<Integer> size ) {
        NullUtil.nullArgCheck(size, "val", Val.class);
        NullUtil.nullPropertyCheck(size, "size", "Null is not a sensible value for a font size.");
        _onShow( size, this::withFontSize );
        return withFontSize(size.orElseThrow());
    }

    public final I isSelectedIf( boolean isSelected ) {
        _setSelectedSilently(isSelected);
        return _this();
    }

    private void _setSelectedSilently( boolean isSelected ) {
        /*
            This is used to change the selection state of the button without triggering
            any action listeners. We need this because we want to construct the
            GUI and the state of its properties without side effects.
         */
        ItemListener[] listeners = getComponent().getItemListeners();
        for ( ItemListener l : listeners )
            getComponent().removeItemListener(l);

        getComponent().setSelected(isSelected);

        for ( ItemListener l : listeners )
            getComponent().addItemListener(l);

    }

    /**
     *  Use this to dynamically bind to a {@link sprouts.Var}
     *  instance which will be used to dynamically model the selection state of the
     *  wrapped {@link AbstractButton} type.
     * @throws IllegalArgumentException if {@code selected} is {@code null}.
     */
    public final I isSelectedIf( Val<Boolean> selected ) {
        NullUtil.nullArgCheck(selected, "selected", Val.class);
        NullUtil.nullPropertyCheck(selected, "selected", "Null can not be used to model the selection state of a button type.");
        _onShow(selected, v -> _setSelectedSilently(v) );
        return isSelectedIf( selected.orElseThrow() );
    }

    /**
     *  Use this to dynamically bind to a {@link sprouts.Var}
     *  instance which will be used to dynamically model the inverse selection state of the
     *  wrapped {@link AbstractButton} type.
     *
     * @param selected The {@link sprouts.Var} instance which will be used to
     *                 model the inverse selection state of the wrapped button type.
     * @throws IllegalArgumentException if {@code selected} is {@code null}.
     */
    public final I isSelectedIfNot( Val<Boolean> selected ) {
        NullUtil.nullArgCheck(selected, "selected", Val.class);
        NullUtil.nullPropertyCheck(selected, "selected", "Null can not be used to model the selection state of a button type.");
        _onShow(selected, v -> _setSelectedSilently(!v) );
        return isSelectedIf( !selected.orElseThrow() );
    }

    /**
     *  Use this to dynamically bind to a {@link sprouts.Var}
     *  instance which will be used to dynamically model the selection state of the
     *  wrapped {@link AbstractButton} type.
     *
     * @param selected The {@link sprouts.Var} instance which will be used to model the selection state of the wrapped button type.
     * @throws IllegalArgumentException if {@code selected} is {@code null}.
     */
    public final I isSelectedIf( Var<Boolean> selected ) {
        NullUtil.nullArgCheck(selected, "selected", Var.class);
        NullUtil.nullPropertyCheck(selected, "selected", "Null can not be used to model the selection state of a button type.");
        _onShow(selected, v -> _setSelectedSilently(v) );
        _onClick(
            e -> _doApp(getComponent().isSelected(), selected::act)
        );
        _onChange(
            v -> _doApp(getComponent().isSelected(), selected::act)
        );
        return isSelectedIf( selected.orElseThrow() );
    }

    /**
     *  Use this to dynamically bind to a {@link sprouts.Var}
     *  instance which will be used to dynamically model the inverse selection state of the
     *  wrapped {@link AbstractButton} type.
     *
     * @param selected The {@link sprouts.Var} instance which will be used to
     *                 model the inverse selection state of the wrapped button type.
     * @throws IllegalArgumentException if {@code selected} is {@code null}.
     */
    public final I isSelectedIfNot( Var<Boolean> selected ) {
        NullUtil.nullArgCheck(selected, "selected", Var.class);
        NullUtil.nullPropertyCheck(selected, "selected", "Null can not be used to model the selection state of a button type.");
        _onShow(selected, v -> _setSelectedSilently(!v) );
        _onClick(
            e -> _doApp(!getComponent().isSelected(), selected::act)
        );
        _onChange(
            v -> _doApp(!getComponent().isSelected(), selected::act)
        );
        return isSelectedIf( !selected.orElseThrow() );
    }

    /**
     *  Use this to dynamically bind to a {@link sprouts.Var}
     *  instance which will be used to dynamically model the pressed state of the
     *  wrapped {@link AbstractButton} type.
     * @throws IllegalArgumentException if {@code var} is {@code null}.
     */
    public final I isPressedIf( Var<Boolean> var ) {
        NullUtil.nullArgCheck(var, "var", Var.class);
        _onShow( var, v -> getComponent().getModel().setPressed(v) );
        _onClick(
            e -> _doApp(getComponent().getModel().isPressed(), pressed->{
                var.act(true);
                var.act(pressed);
            })
        );
        // on change is not needed because the pressed state is only changed by the user.
        return isSelectedIf( var.orElseThrow() );
    }

    /**
     *  Effectively removes the native style of this button.
     *  Without an icon or text, one will not be able to recognize the button.
     *  Use this for buttons with a custom icon or clickable text!
     *
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I makePlain() {
        peek( it -> {
            it.setBorderPainted(false);
            it.setContentAreaFilled(false);
            it.setOpaque(false);
            it.setFocusPainted(false);
            it.setMargin(new Insets(0,0,0,0));
        });
        return _this();
    }

    /**
     *  This method adds the provided
     *  {@link ItemListener} instance to the wrapped button component.
     *  <br><br>
     *
     * @param action The change action lambda which will be passed to the button component.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     */
    public final I onChange( Action<SimpleDelegate<B, ItemEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        _onChange( e -> _doApp(()->action.accept(new SimpleDelegate<>(getComponent(), e, this::getSiblinghood))) );
        return _this();
    }

    protected final void _onChange( Consumer<ItemEvent> action ) {
        /*
            When an item event is fired Swing will go through all the listeners
            from the most recently added to the first added. This means that if we simply add
            a listener through the "addItemListener" method, we will be the last to be notified.
            This is problematic because the first listeners we register are usually
            the ones that are responsible for updating properties.
            This means that when the item listener events of the user
            are fired, the properties will not be updated yet.
            To solve this problem, we do the revers by making sure that our listener is added
            at the first position in the list of listeners inside the button.
        */
        ItemListener[] listeners = getComponent().getItemListeners();
        for (ItemListener listener : listeners)
            getComponent().removeItemListener(listener);

        getComponent().addItemListener(action::accept);

        for ( int i = listeners.length - 1; i >= 0; i-- ) // reverse order because swing does not give us the listeners in the order they were added!
            getComponent().addItemListener(listeners[i]);
        /*
            The reasoning behind why Swing calls item listeners from last to first is
            the assumption that the last listener added is more interested in the event
            than the first listener added.
            This however is an unintuitive assumption, meaning a user would expect
            the first listener added to be the most interested in the event
            simply because it was added first.
            This is especially true in the context of declarative UI design.
         */
    }

    /**
     *  This method enables a more readable way of adding
     *  {@link ActionListener} instances to button types.
     *  Additionally, to the other "onClick" method this method enables the involvement of the
     *  {@link JComponent} itself into the action supplied to it.
     *  This is very useful for changing the state of the JComponent when the action is being triggered.
     *  <br><br>
     *
     * @param action an {@link Action} instance which will receive an {@link SimpleDelegate} containing important context information.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     */
    public I onClick( Action<SimpleDelegate<B, ActionEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        B button = getComponent();
        _onClick(
           e -> _doApp(()->action.accept(
               new SimpleDelegate<>(button, e, this::getSiblinghood)
           ))
        );
        return _this();
    }

    protected final void _onClick( Consumer<ActionEvent> action ) {
        /*
            When an action event is fired, Swing will go through all the listeners
            from the most recently added to the first added. This means that if we simply add
            a listener through the "addActionListener" method, we will be the last to be notified.
            This is problematic because it is built on the assumption that the last listener
            added is more interested in the event than the first listener added.
            This however is an unintuitive assumption, meaning a user would expect
            the first listener added to be the most interested in the event
            simply because it was added first.
            This is especially true in the context of declarative UI design.
        */
        ActionListener[] listeners = getComponent().getActionListeners();
        for (ActionListener listener : listeners)
            getComponent().removeActionListener(listener);

        getComponent().addActionListener(action::accept);

        for ( int i = listeners.length - 1; i >= 0; i-- ) // reverse order because swing does not give us the listeners in the order they were added!
            getComponent().addActionListener(listeners[i]);
    }

    /**
     *  A convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.button("Clickable!")
     *         .peek( button -> button.setHorizontalAlignment(...) );
     *  }</pre>
     * This sets the horizontal alignment of the icon and text.
     *
     * @param horizontalAlign The horizontal alignment which should be applied to the underlying component.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code horizontalAlign} is {@code null}.
     */
    public final I with( UI.HorizontalAlignment horizontalAlign ) {
        NullUtil.nullArgCheck(horizontalAlign, "horizontalAlign", UI.HorizontalAlignment.class);
        getComponent().setHorizontalAlignment(horizontalAlign.forSwing());
        return _this();
    }

    /**
     *  A convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.button("Clickable!")
     *     .peek( button -> {
     *          property.onSetItem(v->button.setHorizontalAlignment(v.forSwing()));
     *          button.setHorizontalAlignment(property.get().forSwing());
     *     });
     *  }</pre>
     * This sets the horizontal alignment of the icon and text
     * and also binds the provided property to the underlying component. <br>
     * <i>Hint: Use {@code myProperty.fireSet()} in your view model to send the property value to this view component.</i>
     *
     * @param horizontalAlign The horizontal alignment property which should be bound to the underlying component.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code horizontalAlign} is {@code null}.
     */
    public final I withHorizontalAlignment( Val<UI.HorizontalAlignment> horizontalAlign ) {
        NullUtil.nullArgCheck(horizontalAlign, "horizontalAlign", Val.class);
        _onShow(horizontalAlign, align -> getComponent().setHorizontalAlignment(align.forSwing()) );
        return with(horizontalAlign.orElseThrow());
    }

    /**
     *  A convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.button("Clickable!")
     *         .peek( button -> button.setVerticalAlignment(...) );
     *  }</pre>
     * This sets the vertical alignment of the icon and text.
     *
     * @param verticalAlign The vertical alignment which should be applied to the underlying component.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code verticalAlign} is {@code null}.
     */
    public final I with( UI.VerticalAlignment verticalAlign ) {
        NullUtil.nullArgCheck(verticalAlign, "verticalAlign", UI.VerticalAlignment.class);
        getComponent().setVerticalAlignment(verticalAlign.forSwing());
        return _this();
    }

    /**
     *  A convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.button("Clickable!")
     *     .peek( button -> {
     *          property.onSetItem(v->button.setVerticalAlignment(v.forSwing()));
     *          button.setVerticalAlignment(property.get().forSwing());
     *     });
     *  }</pre>
     * This sets the vertical alignment of the icon and text
     * and also binds the provided property to the underlying component. <br>
     * <i>Hint: Use {@code myProperty.fireSet()} in your view model to send the property value to this view component.</i>
     *
     * @param verticalAlign The vertical alignment property which should be bound to the underlying component.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code verticalAlign} is {@code null}.
     */
    public final I withVerticalAlignment( Val<UI.VerticalAlignment> verticalAlign ) {
        NullUtil.nullArgCheck(verticalAlign, "verticalAlign", Val.class);
        _onShow(verticalAlign, align -> getComponent().setVerticalAlignment(align.forSwing()) );
        return with(verticalAlign.orElseThrow());
    }

    /**
     *  A convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.button("Clickable!")
     *         .peek( button -> button.setHorizontalTextPosition(...) );
     *  }</pre>
     * This sets the horizontal position of the text relative to the icon.
     *
     * @param horizontalAlign The horizontal text alignment relative to the icon which should be applied to the underlying component.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code horizontalAlign} is {@code null}.
     */
    public final I withImageRelative( UI.HorizontalAlignment horizontalAlign ) {
        NullUtil.nullArgCheck(horizontalAlign, "horizontalAlign", UI.HorizontalAlignment.class);
        getComponent().setHorizontalTextPosition(horizontalAlign.forSwing());
        return _this();
    }

    /**
     *  A convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.button("Clickable!")
     *     .peek( button -> {
     *          property.onSetItem(v->button.setHorizontalTextPosition(v.forSwing()));
     *          button.setHorizontalTextPosition(property.get().forSwing());
     *     });
     *  }</pre>
     * This sets the horizontal position of the text relative to the icon
     * and also binds the provided property to the underlying component. <br>
     * <i>Hint: Use {@code myProperty.fireSet()} in your view model to send the property value to this view component.</i>
     *
     * @param horizontalAlign The horizontal text alignment property relative to the icon which should be bound to the underlying component.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code horizontalAlign} is {@code null}.
     */
    public final I withImageRelativeHorizontalAlignment( Val<UI.HorizontalAlignment> horizontalAlign ) {
        NullUtil.nullArgCheck(horizontalAlign, "horizontalAlign", Val.class);
        _onShow(horizontalAlign, align -> getComponent().setHorizontalTextPosition(align.forSwing()) );
        return withImageRelative(horizontalAlign.orElseThrow());
    }

    /**
     *  A convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.button("Clickable!")
     *         .peek( button -> button.setVerticalTextPosition(...) );
     *  }</pre>
     * This sets the vertical position of the text relative to the icon.
     *
     * @param verticalAlign The vertical text alignment relative to the icon which should be applied to the underlying component.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code verticalAlign} is {@code null}.
     */
    public final I withImageRelative( UI.VerticalAlignment verticalAlign ) {
        NullUtil.nullArgCheck(verticalAlign, "verticalAlign", UI.VerticalAlignment.class);
        getComponent().setVerticalTextPosition(verticalAlign.forSwing());
        return _this();
    }

    /**
     *  A convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.button("Clickable!")
     *     .peek( button -> {
     *          property.onSetItem(v->button.setVerticalTextPosition(v.forSwing()));
     *          button.setVerticalTextPosition(property.get().forSwing());
     *     });
     *  }</pre>
     * This sets the vertical position of the text relative to the icon
     * and also binds the provided property to the underlying component. <br>
     * <i>Hint: Use {@code myProperty.fireSet()} in your view model to send the property value to this view component.</i>
     *
     * @param verticalAlign The vertical text alignment property relative to the icon which should be bound to the underlying component.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code verticalAlign} is {@code null}.
     */
    public final I withImageRelativeVerticalAlignment( Val<UI.VerticalAlignment> verticalAlign ) {
        NullUtil.nullArgCheck(verticalAlign, "verticalAlign", Val.class);
        _onShow( verticalAlign, align -> getComponent().setVerticalTextPosition(align.forSwing()) );
        return withImageRelative(verticalAlign.orElseThrow());
    }

    /**
     *  Use this to attach this button type to a button group.
     *
     * @param group The button group to which this button should be attached.
     * @return This very builder to allow for method chaining.
     */
    public final I withButtonGroup( ButtonGroup group ) {
        NullUtil.nullArgCheck(group, "group", ButtonGroup.class);
        group.add(getComponent());
        return _this();
    }

    public final I withMargin( Insets insets ) {
        NullUtil.nullArgCheck(insets, "insets", Insets.class);
        getComponent().setMargin( insets );
        return _this();
    }

    public final I withMargin( int top, int left, int bottom, int right ) {
        return withMargin( new Insets(top, left, bottom, right) );
    }
}