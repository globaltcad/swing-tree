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

    public UIForCombo onChangeEvent(Consumer<ActionEvent> action) {
        LogUtil.nullArgCheck(action, "action", Consumer.class);
        return this.onChange( it -> action.accept(it.getEvent()) );
    }

    public UIForCombo onChangeComponent(Consumer<JComboBox> action) {
        LogUtil.nullArgCheck(action, "action", Consumer.class);
        return this.onChange( it -> action.accept(it.getComponent()) );
    }
}
