package com.globaltcad.swingtree;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

/**
 *  A UI maker for {@link JComboBox} instances.
 */
public class UIForCombo extends UIForSwing<UIForCombo, JComboBox>
{
    protected UIForCombo(JComboBox component) {
        super(component);
    }

    public UIForCombo onChange(UIAction<JComboBox, ActionEvent> action) {
        LogUtil.nullArgCheck(action, "action", UIAction.class);
        this.component.addActionListener( e -> action.accept(this.component, e) );
        return this;
    }

    /**
     *  Use this to enable or disable editing for the wrapped UI component.
     *
     * @param isEditable The truth value determining if the UI component should be editable or not.
     * @return This very instance, which enables builder-style method chaining.
     */
    public UIForCombo isEditableIf(boolean isEditable) {
        this.component.setEditable(isEditable);
        return this;
    }
}
