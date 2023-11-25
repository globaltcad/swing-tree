package swingtree;

import sprouts.Action;
import sprouts.From;
import sprouts.Val;
import sprouts.Var;
import swingtree.api.IconDeclaration;
import swingtree.style.SvgIcon;

import javax.swing.*;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.function.Consumer;

/**
 *  The following is a more specialized type of builder node based on the {@link UIForAnySwing} builder type,
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
public abstract class UIForAnyButton<I, B extends AbstractButton> extends UIForAnySwing<I, B>
{
    /**
     * Defines the single line of text the wrapped button type will display.
     * If the value of text is null or empty string, nothing is displayed.
     *
     * @param text The new text to be set for the wrapped button type.
     * @return This very builder to allow for method chaining.
     */
    public final I withText( String text ) {
        return _with( thisComponent -> thisComponent.setText(text) )._this();
    }

    /**
     *  Binds the provided {@link Val} property to the wrapped button type
     *  and sets the text of the button to the value of the property.
     * <i>Hint: Use {@code myProperty.fire(From.VIEW_MODEL)} in your view model to manually
     * send the property value to this view component.</i>
     * @param text The view model property which should be bound to this UI.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code text} is {@code null}.
     */
    public final I withText( Val<String> text ) {
        NullUtil.nullArgCheck(text, "val", Val.class);
        NullUtil.nullPropertyCheck(text, "text");
        return _withOnShow( text, AbstractButton::setText )
               ._with( c -> c.setText(text.orElseThrow()) )
               ._this();
    }

    /**
     *  Use this to set the icon for the wrapped button type.
     *  This is in essence a convenience method to avoid peeking into this builder like so:
     *  <pre>{@code
     *     UI.button("Something")
     *     .peek( button -> button.setIcon(...) );
     *  }</pre>
     *
     *
     * @param icon The {@link Icon} which should be displayed on the button.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code icon} is {@code null}.
     */
    public I withIcon( Icon icon ) {
        NullUtil.nullArgCheck(icon,"icon",Icon.class);
        return _with( c -> c.setIcon(icon) )._this();
    }

    /**
     *  Takes the provided {@link Icon} and scales it to the provided width and height
     *  before displaying it on the wrapped button type.
     *
     * @param width The width of the icon.
     * @param height The height of the icon.
     * @param icon The {@link Icon} which should be scaled and displayed on the button.
     * @return This very builder to allow for method chaining.
     */
    public I withIcon( int width, int height, ImageIcon icon ) {
        NullUtil.nullArgCheck(icon,"icon",Icon.class);
        icon = _fitTo( width, height, icon );
        return withIcon(icon);
    }

    private ImageIcon _fitTo( int width, int height, ImageIcon icon ) {
        if ( icon instanceof SvgIcon)
        {
            SvgIcon svgIcon = (SvgIcon) icon;
            svgIcon = svgIcon.withIconWidth(width);
            icon    = svgIcon.withIconHeight(height);
        }
        else if ( width != icon.getIconWidth() || height != icon.getIconHeight() )
        {
            float scale = UI.scale();

            int scaleHint = Image.SCALE_SMOOTH;
            if (scale > 1.5f)
                scaleHint = Image.SCALE_FAST;

            width = (int) (width * scale);
            height = (int) (height * scale);

            icon = new ImageIcon(icon.getImage().getScaledInstance(width, height, scaleHint));
        }
        return icon;
    }

    /**
     *  Takes the provided {@link IconDeclaration} and scales it to the provided width and height
     *  before displaying it on the wrapped button type.
     *
     * @param width The width of the icon.
     * @param height The height of the icon.
     * @param icon The {@link IconDeclaration} which should be scaled and displayed on the button.
     * @return This very builder to allow for method chaining.
     */
    public I withIcon( int width, int height, IconDeclaration icon ) {
        NullUtil.nullArgCheck(icon,"icon",IconDeclaration.class);
        return icon.find()
                   .map( i -> withIcon(width, height, i) )
                   .orElseGet( this::_this );
    }

    /**
     *  Takes the provided {@link IconDeclaration} and scales the corresponding icon it
     *  to the provided width and height before displaying it on the wrapped button type.
     *
     * @param width The width of the icon.
     * @param height The height of the icon.
     * @param icon The {@link Icon} which should be scaled and displayed on the button.
     * @param fitComponent The {@link UI.FitComponent} which determines how the icon should be scaled relative to the button.
     * @return This very builder to allow for method chaining.
     */
    public I withIcon( int width, int height, IconDeclaration icon, UI.FitComponent fitComponent ) {
        NullUtil.nullArgCheck(icon,"icon",IconDeclaration.class);
        return icon.find()
                .map( i -> withIcon(_fitTo(width, height, i), fitComponent) )
                .orElseGet( this::_this );
    }

    /**
     * @param icon The {@link SvgIcon} which should be displayed on the button.
     * @param fitComponent The {@link UI.FitComponent} which determines how the icon should be scaled.
     * @return This very builder to allow for method chaining.
     */
    public I withIcon( ImageIcon icon, UI.FitComponent fitComponent ) {
        NullUtil.nullArgCheck(icon,"svgIcon", ImageIcon.class);
        NullUtil.nullArgCheck(fitComponent,"fitComponent", UI.FitComponent.class);
        if ( icon instanceof SvgIcon)
        {
            SvgIcon svgIcon = (SvgIcon) icon;
            return withIcon( svgIcon.withFitComponent(fitComponent) );
        }
        else
            return _with( thisComponent -> {
                       UI.runLater(()->{
                           int width  = thisComponent.getWidth();
                           int height = thisComponent.getHeight();
                           width  = Math.max(width,  thisComponent.getMinimumSize().width);
                           height = Math.max(height, thisComponent.getMinimumSize().height);
                           if ( width > 0 && height > 0 )
                               thisComponent.setIcon(_fitTo( width, height, icon ));
                       });
                   })
                   ._this();

    }

    public I withIcon( IconDeclaration icon, UI.FitComponent fitComponent ) {
        NullUtil.nullArgCheck(icon,"icon", IconDeclaration.class);
        NullUtil.nullArgCheck(fitComponent,"fitComponent", UI.FitComponent.class);
        return icon.find()
                   .map( i -> withIcon(i, fitComponent) )
                   .orElseGet( this::_this );
    }

    /**
     *  Use this to specify the icon for the wrapped button type.
     *  The icon is determined based on the provided {@link IconDeclaration}
     *  instance which is conceptually merely a resource path to the icon.
     *
     * @param icon The desired icon to be displayed on top of the button.
     * @return This very builder to allow for method chaining.
     */
    public I withIcon( IconDeclaration icon ) {
        NullUtil.nullArgCheck(icon,"icon", IconDeclaration.class);
        return _with( c -> icon.find().ifPresent(c::setIcon) )._this();
    }

    /**
     *  Use this to dynamically set the icon property for the wrapped button type.
     *  When the icon wrapped by the provided property changes,
     *  then so does the icon displayed on this button.
     *  <p>
     *  But note that you may not use the {@link Icon} or {@link ImageIcon} classes directly,
     *  instead <b>you must use implementations of the {@link IconDeclaration} interface</b>,
     *  which merely models the resource location of the icon, but does not load
     *  the whole icon itself.
     *  <p>
     *  The reason for this distinction is the fact that traditional Swing icons
     *  are heavy objects whose loading may or may not succeed, and so they are
     *  not suitable for direct use in a property as part of your view model.
     *  Instead, you should use the {@link IconDeclaration} interface, which is a
     *  lightweight value object that merely models the resource location of the icon
     *  even if it is not yet loaded or even does not exist at all.
     *  <p>
     *  This is especially useful in case of unit tests for you view model,
     *  where the icon may not be available at all, but you still want to test
     *  the behaviour of your view model.
     *
     * @param icon The {@link Icon} property which should be displayed on the button.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code icon} is {@code null}.
     */
    public I withIcon( Val<IconDeclaration> icon ) {
        NullUtil.nullArgCheck(icon, "icon", Val.class);
        NullUtil.nullPropertyCheck(icon, "icon");
        return _withOnShow( icon, (c, v) -> v.find().ifPresent(c::setIcon) )
               ._with( c -> icon.orElseThrow().find().ifPresent(c::setIcon) )
               ._this();
    }

    /**
     *  Use this to set the size of the font of the wrapped button type.
     * @param size The size of the font which should be displayed on the button.
     * @return This very builder to allow for method chaining.
     */
    public I withFontSize( int size ) {
        return _with( button -> {
                   Font old = button.getFont();
                   button.setFont(old.deriveFont(UI.scale((float)size)));
               })
               ._this();
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
        return _withOnShow( size, (c,v)->{
                    c.setFont(c.getFont().deriveFont(UI.scale((float)v)));
                })
                ._with( c -> {
                    c.setFont(c.getFont().deriveFont(UI.scale((float)size.orElseThrow())));
                })
                ._this();
    }

    /**
     *  Use this to set the font of the wrapped button type.
     * @param font The font of the text which should be displayed on the button.
     * @return This builder instance, to allow for method chaining.
     * @throws IllegalArgumentException if {@code font} is {@code null}.
     */
    public final I withFont( Font font ) {
        NullUtil.nullArgCheck(font, "font", Font.class);
        return _with( button -> button.setFont(font) )._this();
    }

    /**
     *  Use this to dynamically set the font of the wrapped button type
     *  through the provided view model property.
     *  When the font wrapped by the provided property changes,
     *  then so does the font of the text displayed on this button.
     *
     * @param font The font property of the text which should be displayed on the button type.
     * @return This builder instance, to allow for method chaining.
     * @throws IllegalArgumentException if {@code font} is {@code null}.
     * @throws IllegalArgumentException if {@code font} is a property which can wrap {@code null}.
     */
    public final I withFont( Val<Font> font ) {
        NullUtil.nullArgCheck(font, "font", Val.class);
        NullUtil.nullPropertyCheck(font, "font", "Use the default font of this component instead of null!");
        return _withOnShow( font, JComponent::setFont )
               ._with( button -> button.setFont(font.orElseThrow()) )
               ._this();
    }

    /**
     *  Use this to set the selection state of the wrapped button type.
     *
     * @param isSelected The selection state of the wrapped button type, which translates to {@link AbstractButton#setSelected(boolean)}.
     * @return This builder instance, to allow for method chaining.
     */
    public final I isSelectedIf( boolean isSelected ) {
        return _with( button -> _setSelectedSilently(button, isSelected) )._this();
    }

    void _setSelectedSilently( B thisButton, boolean isSelected ) {
        /*
            This is used to change the selection state of the button without triggering
            any action listeners. We need this because we want to construct the
            GUI and the state of its properties without side effects.
         */
        ItemListener[] listeners = thisButton.getItemListeners();
        for ( ItemListener l : listeners )
            thisButton.removeItemListener(l);

        thisButton.setSelected(isSelected);

        for ( ItemListener l : listeners )
            thisButton.addItemListener(l);

    }

    /**
     *  Use this to bind to a {@link sprouts.Var}
     *  instance which will be used to dynamically model the selection state of the
     *  wrapped {@link AbstractButton} type.
     *
     * @param selected The {@link sprouts.Var} instance which will be used to model the selection state of the wrapped button type.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code selected} is {@code null}.
     */
    public final I isSelectedIf( Val<Boolean> selected ) {
        NullUtil.nullArgCheck(selected, "selected", Val.class);
        NullUtil.nullPropertyCheck(selected, "selected", "Null can not be used to model the selection state of a button type.");
        return _withOnShow(selected, this::_setSelectedSilently)
               ._with( button -> _setSelectedSilently(button, selected.get()) )._this();
    }

    /**
     *  Use this to bind to a {@link sprouts.Var}
     *  instance which will be used to dynamically model the inverse selection state of the
     *  wrapped {@link AbstractButton} type.
     *
     * @param selected The {@link sprouts.Var} instance which will be used to
     *                 model the inverse selection state of the wrapped button type.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code selected} is {@code null}.
     */
    public final I isSelectedIfNot( Val<Boolean> selected ) {
        NullUtil.nullArgCheck(selected, "selected", Val.class);
        NullUtil.nullPropertyCheck(selected, "selected", "Null can not be used to model the selection state of a button type.");
        return _withOnShow( selected, (c, v) -> _setSelectedSilently(c, !v) )
               ._with( button -> _setSelectedSilently(button, !selected.is(true)) )._this();
    }

    /**
     *  Use this to bind to a {@link sprouts.Var}
     *  instance which will be used to dynamically model the selection state of the
     *  wrapped {@link AbstractButton} type.
     *
     * @param selected The {@link sprouts.Var} instance which will be used to model the selection state of the wrapped button type.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code selected} is {@code null}.
     */
    public final I isSelectedIf( Var<Boolean> selected ) {
        NullUtil.nullArgCheck(selected, "selected", Var.class);
        NullUtil.nullPropertyCheck(selected, "selected", "Null can not be used to model the selection state of a button type.");
        return _withOnShow( selected, this::_setSelectedSilently )
                ._with( button -> {
                    _onClick(button,
                        e -> _doApp(button.isSelected(), newItem -> selected.set(From.VIEW, newItem) )
                    );
                    _onChange(button,
                        v -> _doApp(button.isSelected(), newItem -> selected.set(From.VIEW, newItem) )
                    );
                })
                ._with( button -> _setSelectedSilently(button, selected.get()) )
                ._this();
    }

    /**
     *  Use this to dynamically bind to a {@link sprouts.Var}
     *  instance which will be used to dynamically model the inverse selection state of the
     *  wrapped {@link AbstractButton} type.
     *
     * @param selected The {@link sprouts.Var} instance which will be used to
     *                 model the inverse selection state of the wrapped button type.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code selected} is {@code null}.
     */
    public final I isSelectedIfNot( Var<Boolean> selected ) {
        NullUtil.nullArgCheck(selected, "selected", Var.class);
        NullUtil.nullPropertyCheck(selected, "selected", "Null can not be used to model the selection state of a button type.");
        return _withOnShow( selected, (c, v) -> {
                    _setSelectedSilently(c, !v);
                })
                ._with( button -> {
                    _onClick(button,
                        e -> _doApp(!button.isSelected(), newItem -> selected.set(From.VIEW, newItem) )
                    );
                    _onChange(button,
                        v -> _doApp(!button.isSelected(), newItem -> selected.set(From.VIEW, newItem) )
                    );
                })
                ._with( button -> _setSelectedSilently(button, !selected.is(true)) )
                ._this();
    }

    /**
     *  Use this to make the wrapped UI component dynamically selected or deselected,
     *  based on the equality between the supplied enum value and enum property. <br>
     *  So to illustrate: <br>
     *  <pre>{@code
     *      // In your view model:
     *      enum Step { ONE, TWO, THREE }
     *      Var<Step> step = Var.of(Step.ONE);
     *
     *      // In your view:
     *      UI.radioButton("Two").isSelectedIf(Step.TWO, vm.getStep());
     *  }</pre>
     *  As you can see, the radio button will be selected if the enum property is equal to the supplied enum value
     *  and deselected otherwise. <br>
     *  <br>
     * <i>Hint: Use {@code myProperty.fire(From.VIEW_MODEL)} in your view model to send the property value to this view component.</i>
     *
     * @param enumValue The enum value which, if equal to the supplied enum property, makes the UI component selected.
     * @param enumProperty The enum property which, if equal to the supplied enum value, makes the UI component selected.
     * @param <E> The type of the enum.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final <E extends Enum<E>> I isSelectedIf( E enumValue, Val<E> enumProperty ) {
        NullUtil.nullArgCheck(enumValue, "enumValue", Enum.class);
        NullUtil.nullArgCheck(enumProperty, "enumProperty", Val.class);
        return _withOnShow( enumProperty, (c,v) -> _setSelectedSilently(c, enumValue.equals(v)) )
                ._with( button ->
                     _setSelectedSilently(button, enumValue.equals(enumProperty.orElseThrow()))
                )
                ._this();
    }

    /**
     *  This is the inverse of {@link #isSelectedIf(Enum, Val)}.
     *  Use this to make the wrapped UI component dynamically deselected or selected,
     *  based on the equality between the supplied enum value and enum property. <br>
     * <i>Hint: Use {@code myProperty.fire(From.VIEW_MODEL)} in your view model to send the property value to this view component.</i>
     *
     * @param enumValue The enum value which, if equal to the supplied enum property, makes the UI component deselected.
     * @param enumProperty The enum property which, if equal to the supplied enum value, makes the UI component deselected.
     * @param <E> The type of the enum.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final <E extends Enum<E>> I isSelectedIfNot( E enumValue, Val<E> enumProperty ) {
        NullUtil.nullArgCheck(enumValue, "enumValue", Enum.class);
        NullUtil.nullArgCheck(enumProperty, "enumProperty", Val.class);
        return _withOnShow( enumProperty, (c, v) -> _setSelectedSilently(c, !enumValue.equals(v)) )
                ._with( button ->
                     _setSelectedSilently(button, !enumValue.equals(enumProperty.orElseThrow()))
                )
                ._this();
    }

    /**
     *  Use this to dynamically bind to a {@link sprouts.Var}
     *  instance which will be used to dynamically model the pressed state of the
     *  wrapped {@link AbstractButton} type.
     *
     * @param var The {@link sprouts.Var} instance which will be used to model the pressed state of the wrapped button type.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code var} is {@code null}.
     */
    public final I isPressedIf( Var<Boolean> var ) {
        NullUtil.nullArgCheck(var, "var", Var.class);
        return _withOnShow( var, (c,v) -> {
                    c.getModel().setPressed(v);
                })
                ._with( button -> {
                    _onClick(button, e ->
                        _doApp(button.getModel().isPressed(), pressed->{
                            var.set(From.VIEW, true);
                            var.set(From.VIEW, pressed);
                        })
                    );
                })
                // on change is not needed because the pressed state is only changed by the user.
                ._with( button ->
                    button.setSelected(var.orElseThrow())
                )
                ._this();
    }

    /**
     * @param borderPainted Whether the border of the button should be painted.
     * @return This very instance, which enables builder-style method chaining.
     */
    public I isBorderPaintedIf( boolean borderPainted ) {
        return _with( button -> button.setBorderPainted(borderPainted) )._this();
    }

    /**
     * @param val Whether the border of the button should be painted.
     * @return This very instance, which enables builder-style method chaining.
     */
    public I isBorderPaintedIf( Val<Boolean> val ) {
        NullUtil.nullArgCheck(val, "val", Val.class);
        NullUtil.nullPropertyCheck(val, "val", "Null is not a valid value for the border painted property.");
        return _withOnShow( val, AbstractButton::setBorderPainted )
               ._with( button -> button.setBorderPainted(val.orElseThrow()) )
               ._this();
    }

    /**
     *  Effectively removes the native style of this button.
     *  Without an icon or text, one will not be able to recognize the button.
     *  Use this for buttons with a custom icon or clickable text!
     *
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I makePlain() {
        return peek( it -> {
                    it.setBorderPainted(false);
                    it.setContentAreaFilled(false);
                    it.setOpaque(false);
                    it.setFocusPainted(false);
                    it.setMargin(new Insets(0,0,0,0));
                });
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
    public final I onChange( Action<ComponentDelegate<B, ItemEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        return _with( button -> {
                    _onChange(button, e ->
                        _doApp(()->action.accept(new ComponentDelegate<>(button, e)))
                    );
                })
                ._this();
    }

    protected final void _onChange( B button, Consumer<ItemEvent> action ) {
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
        ItemListener[] listeners = button.getItemListeners();
        for (ItemListener listener : listeners)
            button.removeItemListener(listener);

        button.addItemListener(action::accept);

        for ( int i = listeners.length - 1; i >= 0; i-- ) // reverse order because swing does not give us the listeners in the order they were added!
            button.addItemListener(listeners[i]);
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
     * @param action an {@link Action} instance which will receive an {@link ComponentDelegate} containing important context information.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     */
    public I onClick( Action<ComponentDelegate<B, ActionEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        return _with( button -> {
                    _onClick(button, e ->
                        _doApp(() ->
                            action.accept(new ComponentDelegate<>(button, e))
                        )
                    );
                })
                ._this();
    }

    protected final void _onClick( B button, Consumer<ActionEvent> action ) {
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
        ActionListener[] listeners = button.getActionListeners();
        for (ActionListener listener : listeners)
            button.removeActionListener(listener);

        button.addActionListener(action::accept);

        for ( int i = listeners.length - 1; i >= 0; i-- ) // reverse order because swing does not give us the listeners in the order they were added!
            button.addActionListener(listeners[i]);
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
    public final I withHorizontalAlignment( UI.HorizontalAlignment horizontalAlign ) {
        NullUtil.nullArgCheck(horizontalAlign, "horizontalAlign", UI.HorizontalAlignment.class);
        return _with( c ->
                    c.setHorizontalAlignment(horizontalAlign.forSwing())
                )
                ._this();
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
     * <i>Hint: Use {@code myProperty.fire(From.VIEW_MODEL)} in your view model to send the property value to this view component.</i>
     *
     * @param horizontalAlign The horizontal alignment property which should be bound to the underlying component.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code horizontalAlign} is {@code null}.
     */
    public final I withHorizontalAlignment( Val<UI.HorizontalAlignment> horizontalAlign ) {
        NullUtil.nullArgCheck(horizontalAlign, "horizontalAlign", Val.class);
        NullUtil.nullPropertyCheck(horizontalAlign, "horizontalAlign");
        return _withOnShow( horizontalAlign, (c, align) -> c.setHorizontalAlignment(align.forSwing()) )
               ._with( c ->
                   c.setHorizontalAlignment(horizontalAlign.orElseThrow().forSwing())
               )
               ._this();
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
    public final I withVerticalAlignment( UI.VerticalAlignment verticalAlign ) {
        NullUtil.nullArgCheck(verticalAlign, "verticalAlign", UI.VerticalAlignment.class);
        return _with( c ->
                    c.setVerticalAlignment(verticalAlign.forSwing())
                )
                ._this();
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
     * <i>Hint: Use {@code myProperty.fire(From.VIEW_MODEL)} in your view model to send the property value to this view component.</i>
     *
     * @param verticalAlign The vertical alignment property which should be bound to the underlying component.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code verticalAlign} is {@code null}.
     */
    public final I withVerticalAlignment( Val<UI.VerticalAlignment> verticalAlign ) {
        NullUtil.nullArgCheck(verticalAlign, "verticalAlign", Val.class);
        NullUtil.nullPropertyCheck(verticalAlign, "verticalAlign");
        return _withOnShow( verticalAlign, (c, align) -> c.setVerticalAlignment(align.forSwing()) )
               ._with( c ->
                   c.setVerticalAlignment(verticalAlign.orElseThrow().forSwing())
               )
               ._this();
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
    public final I withHorizontalTextAlignment( UI.HorizontalAlignment horizontalAlign ) {
        NullUtil.nullArgCheck(horizontalAlign, "horizontalAlign", UI.HorizontalAlignment.class);
        return _with( c ->
                    c.setHorizontalTextPosition(horizontalAlign.forSwing())
                )
                ._this();
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
     * <i>Hint: Use {@code myProperty.fire(From.VIEW_MODEL)} in your view model to send the property value to this view component.</i>
     *
     * @param horizontalAlign The horizontal text alignment property relative to the icon which should be bound to the underlying component.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code horizontalAlign} is {@code null}.
     */
    public final I withHorizontalTextAlignment( Val<UI.HorizontalAlignment> horizontalAlign ) {
        NullUtil.nullArgCheck(horizontalAlign, "horizontalAlign", Val.class);
        return _withOnShow( horizontalAlign, (c, align) -> c.setHorizontalTextPosition(align.forSwing()) )
                ._with( c ->
                    c.setHorizontalTextPosition(horizontalAlign.orElseThrow().forSwing())
                )
                ._this();
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
    public final I withVerticalTextAlignment( UI.VerticalAlignment verticalAlign ) {
        NullUtil.nullArgCheck(verticalAlign, "verticalAlign", UI.VerticalAlignment.class);
        return _with( c ->
                    c.setVerticalTextPosition(verticalAlign.forSwing())
                )
                ._this();
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
     * <i>Hint: Use {@code myProperty.fire(From.VIEW_MODEL)} in your view model to send the property value to this view component.</i>
     *
     * @param verticalAlign The vertical text alignment property relative to the icon which should be bound to the underlying component.
     * @return This very builder to allow for method chaining.
     * @throws IllegalArgumentException if {@code verticalAlign} is {@code null}.
     */
    public final I withVerticalTextAlignment( Val<UI.VerticalAlignment> verticalAlign ) {
        NullUtil.nullArgCheck(verticalAlign, "verticalAlign", Val.class);
        return _withOnShow( verticalAlign, (c, align) -> c.setVerticalTextPosition(align.forSwing()) )
                ._with( c ->
                    c.setVerticalTextPosition(verticalAlign.orElseThrow().forSwing())
                )
                ._this();
    }

    /**
     *  Use this to attach this button type to a button group.
     *
     * @param group The button group to which this button should be attached.
     * @return This very builder to allow for method chaining.
     */
    public final I withButtonGroup( ButtonGroup group ) {
        NullUtil.nullArgCheck(group, "group", ButtonGroup.class);
        return _with(group::add)._this();
    }

    /**
     *  Use this to set the margin of the wrapped button type.
     *
     * @param insets The margin of the button.
     * @return This very builder to allow for method chaining.
     */
    public final I withMargin( Insets insets ) {
        NullUtil.nullArgCheck(insets, "insets", Insets.class);
        return _with( thisComponent ->
                    thisComponent.setMargin(insets)
                )
                ._this();
    }

    /**
     *  Use this to set the margin of the wrapped button type.
     *
     * @param top The top margin of the button.
     * @param left The left margin of the button.
     * @param bottom The bottom margin of the button.
     * @param right The right margin of the button.
     * @return This very builder to allow for method chaining.
     */
    public final I withMargin( int top, int left, int bottom, int right ) {
        return withMargin( new Insets(top, left, bottom, right) );
    }
}