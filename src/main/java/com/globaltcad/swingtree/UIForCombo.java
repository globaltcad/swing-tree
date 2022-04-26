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

    public UIForCombo onChange(Consumer<EventContext<JComboBox, ActionEvent>> action) {
        LogUtil.nullArgCheck(action, "action", Consumer.class);
        this.component.addActionListener( e -> action.accept(new EventContext<>(this.component, e)) );
        return this;
    }
}
