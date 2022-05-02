package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.UIAction;
import com.globaltcad.swingtree.delegates.SimpleDelegate;

import javax.swing.*;
import javax.swing.event.ChangeEvent;

/**
 *  A swing tree builder for {@link JSlider} instances.
 */
public class UIForSlider extends UIForAbstractSwing<UIForSlider, JSlider>
{
    protected UIForSlider(JSlider component) { super(component); }


    public final UIForSlider onChange(UIAction<SimpleDelegate<JSlider, ChangeEvent>> action) {
        LogUtil.nullArgCheck(action, "action", UIAction.class);
        this.component.addChangeListener( e -> action.accept(new SimpleDelegate<>(this.component, e, ()->getSiblinghood())) );
        return this;
    }

}