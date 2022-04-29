package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.UIAction;

import javax.swing.*;
import javax.swing.event.ChangeEvent;

/**
 *  A swing tree builder for {@link JSlider} instances.
 */
public class UIForSlider extends UIForSwing<UIForSlider, JSlider>
{
    protected UIForSlider(JSlider component) { super(component); }


    public final UIForSlider onChange(UIAction<JSlider, ChangeEvent> action) {
        LogUtil.nullArgCheck(action, "action", UIAction.class);
        this.component.addChangeListener( e -> action.accept(new EventContext<>(this.component, e)) );
        return this;
    }

}