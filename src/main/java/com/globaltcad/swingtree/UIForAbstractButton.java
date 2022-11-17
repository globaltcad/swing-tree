package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.UIAction;
import com.globaltcad.swingtree.api.mvvm.Val;
import com.globaltcad.swingtree.api.mvvm.Var;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 *  The following is a more specialized type of builder node based on the {@link UIForAbstractSwing} builder type,
 *  and provides additional features associated with the more specialized
 *  {@link AbstractButton}" Swing component type.
 *  One of such features is the {@link #onClick(UIAction)} method allowing for a more readable way of adding
 *  {@link ActionListener} instances to button types...
 *  <br><br>
 *
 * @param <B> The type parameter for the component wrapped by an instance of this class.
 */
public abstract class UIForAbstractButton<I, B extends AbstractButton> extends UIForAbstractSwing<I, B>
{
    protected UIForAbstractButton(B component) { super(component); }

    /**
     * Defines the single line of text the wrapped button type will display.
     * If the value of text is null or empty string, nothing is displayed.
     *
     * @param text The new text to be set for the wrapped button type.
     * @return This very builder to allow for method chaining.
     */
    public final I withText( String text ) {
        _component.setText(text);
        return (I) this;
    }

    public final I withText( Val<String> val ) {
        val.onView(_component::setText);
        return withText( val.get() );
    }

    public final I isSelectedIf(boolean isSelected) {
        _component.setSelected(isSelected);
        return (I) this;
    }

    /**
     *  Use this to dynamically bind to a {@link com.globaltcad.swingtree.api.mvvm.Var}
     *  instance which will be used to dynamically model the selection state of the
     *  wrapped {@link AbstractButton} type.
     */
    public final I isSelectedIf( Val<Boolean> val ) {
        val.onView(_component::setSelected);
        return isSelectedIf( val.get() );
    }

    /**
     *  Use this to dynamically bind to a {@link com.globaltcad.swingtree.api.mvvm.Var}
     *  instance which will be used to dynamically model the selection state of the
     *  wrapped {@link AbstractButton} type.
     */
    public final I isSelectedIf( Var<Boolean> var ) {
        var.onView(_component::setSelected);
        this.onClick( it -> var.set(it.getComponent().isSelected()).act() );
        return isSelectedIf( var.get() );
    }

    /**
     *  Effectively removes the native style of this button.
     *  Without an icon or text, one will not be able to recognize the button.
     *  Use this for buttons with a custom icon or clickable text!
     *
     * @return This very instance, which enables builder-style method chaining.
     */
    public I makePlain() {
        peek(it -> {
            it.setBorderPainted(false);
            it.setContentAreaFilled(false);
            it.setOpaque(false);
            it.setFocusPainted(false);
            it.setMargin(new Insets(0,0,0,0));
        });
        return (I) this;
    }

    /**
     *  This method adds the provided
     *  {@link ItemListener} instance to the wrapped button component.
     *  <br><br>
     *
     * @param action The change action lambda which will be passed to the button component.
     * @return This very instance, which enables builder-style method chaining.
     */
    public I onChange(UIAction<SimpleDelegate<B, ItemEvent>> action) {
        LogUtil.nullArgCheck(action, "action", UIAction.class);
        _component.addItemListener(e -> action.accept(new SimpleDelegate<>(_component, e, this::getSiblinghood)));
        return (I) this;
    }

    /**
     *  This method enables a more readable way of adding
     *  {@link ActionListener} instances to button types.
     *  Additionally, to the other "onClick" method this method enables the involvement of the
     *  {@link JComponent} itself into the action supplied to it.
     *  This is very useful for changing the state of the JComponent when the action is being triggered.
     *  <br><br>
     *
     * @param action an {@link UIAction} instance which will receive an {@link SimpleDelegate} containing important context information.
     * @return This very instance, which enables builder-style method chaining.
     */
    public I onClick(UIAction<SimpleDelegate<B, ActionEvent>> action) {
        LogUtil.nullArgCheck(action, "action", UIAction.class);
        _component.addActionListener(
           e -> action.accept(
               new SimpleDelegate<>(_component, e, this::getSiblinghood)
           )
        );
        return (I) this;
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
     */
    public I with(UI.HorizontalAlignment horizontalAlign) {
        LogUtil.nullArgCheck(horizontalAlign, "horizontalAlign", UI.HorizontalAlignment.class);
        _component.setHorizontalAlignment(horizontalAlign.forSwing());
        return (I) this;
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
     */
    public I with(UI.VerticalAlignment verticalAlign) {
        LogUtil.nullArgCheck(verticalAlign, "verticalAlign", UI.VerticalAlignment.class);
        _component.setVerticalAlignment(verticalAlign.forSwing());
        return (I) this;
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
     */
    public I withImageRelative(UI.HorizontalAlignment horizontalAlign) {
        LogUtil.nullArgCheck(horizontalAlign, "horizontalAlign", UI.HorizontalAlignment.class);
        _component.setHorizontalTextPosition(horizontalAlign.forSwing());
        return (I) this;
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
     */
    public I withImageRelative(UI.VerticalAlignment verticalAlign) {
        LogUtil.nullArgCheck(verticalAlign, "verticalAlign", UI.VerticalAlignment.class);
        _component.setVerticalTextPosition(verticalAlign.forSwing());
        return (I) this;
    }

}