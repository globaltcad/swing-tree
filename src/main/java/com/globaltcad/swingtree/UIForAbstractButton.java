package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.UIAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 *  The following is a more specialized type of builder which extends the "BasicBuilder" class
 *  and provides additional features associated with the more specialized
 *  "AbstractButton" Swing component type.
 *  One of which is the "onClick" method allowing for a more readable way of adding
 *  ActionListener instances to buttons types...
 *  <br><br>
 *
 * @param <B> The type parameter for the component wrapped by an instance of this class.
 */
public abstract class UIForAbstractButton<I, B extends AbstractButton> extends UIForAbstractSwing<I, B>
{
    protected UIForAbstractButton(B component) { super(component); }

    public final I withText(String text) {
        _component.setText(text);
        return (I) this;
    }

    public final I isSelectedIf(boolean isSelected) {
        _component.setSelected(isSelected);
        return (I) this;
    }

    /**
     *  Effectively removes the native style of this button.
     *  Without an icon or text, one will not be able to recognize the button.
     *  Use this for buttons with a custom icon or clickable text!
     *
     * @return This very instance, which enables builder-style method chaining.
     */
    public I makePlain() {
        make( it -> {
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
        _component.addItemListener(e -> action.accept(new SimpleDelegate<>(_component, e, ()->getSiblinghood())));
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
               new SimpleDelegate<>(_component, e, ()->this.getSiblinghood())
           )
        );
        return (I) this;
    }


    public I withPosition(UI.HorizontalAlign horizontalAlign) {
        _component.setHorizontalAlignment(horizontalAlign.forSwing());
        return (I) this;
    }

    public I withTextPosition(UI.HorizontalAlign horizontalAlign) {
        _component.setHorizontalTextPosition(horizontalAlign.forSwing());
        return (I) this;
    }

    public I withPosition(UI.VerticalAlign horizontalAlign) {
        _component.setVerticalAlignment(horizontalAlign.forSwing());
        return (I) this;
    }

    public I withTextPosition(UI.VerticalAlign horizontalAlign) {
        _component.setVerticalTextPosition(horizontalAlign.forSwing());
        return (I) this;
    }

}