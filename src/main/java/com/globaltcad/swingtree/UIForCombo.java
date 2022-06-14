package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.UIAction;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 *  A swing tree builder node for {@link JComboBox} instances.
 */
public class UIForCombo<E> extends UIForAbstractSwing<UIForCombo<E>, JComboBox<E>>
{
    protected UIForCombo(JComboBox<E> component) { super(component); }

    /**
     * Adds an {@link UIAction} to the underlying {@link JComboBox}
     * through an {@link java.awt.event.ActionListener},
     * which will be called when a selection has been made. If the combo box is editable, then
     * an {@link ActionEvent} will be fired when editing has stopped.
     * For more information see {@link JComboBox#addActionListener(ActionListener)}.
     *
     * @param action The {@link UIAction} that will be notified.
     * @return This very instance, which enables builder-style method chaining.
     */
    public UIForCombo<E> onChange(UIAction<SimpleDelegate<JComboBox<E>, ActionEvent>> action) {
        LogUtil.nullArgCheck(action, "action", UIAction.class);
        _component.addActionListener(e -> action.accept(new SimpleDelegate<>(_component, e, ()->getSiblinghood())) );
        return this;
    }

    /**
     *  Use this to enable or disable editing for the wrapped UI component.
     *
     * @param isEditable The truth value determining if the UI component should be editable or not.
     * @return This very instance, which enables builder-style method chaining.
     */
    public UIForCombo<E> isEditableIf(boolean isEditable) {
        _component.setEditable(isEditable);
        return this;
    }
}
