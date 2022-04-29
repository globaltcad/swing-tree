package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.UIAction;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 *  A UI maker for {@link JComboBox} instances.
 */
public class UIForCombo<E> extends UIForSwing<UIForCombo<E>, JComboBox<E>>
{
    protected UIForCombo(JComboBox<E> component) {
        super(component);
    }

    public UIForCombo<E> onChange(UIAction<JComboBox<E>, ActionEvent> action) {
        LogUtil.nullArgCheck(action, "action", UIAction.class);
        this.component.addActionListener( e -> action.accept(new EventContext<>(this.component, e)) );
        return this;
    }

    /**
     *  Use this to enable or disable editing for the wrapped UI component.
     *
     * @param isEditable The truth value determining if the UI component should be editable or not.
     * @return This very instance, which enables builder-style method chaining.
     */
    public UIForCombo<E> isEditableIf(boolean isEditable) {
        this.component.setEditable(isEditable);
        return this;
    }
}
