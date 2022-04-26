package com.globaltcad.swingtree;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.util.function.Consumer;

/**
 *  A UI make for {@link JSlider} instances.
 */
public class UIForSlider extends UIForSwing<UIForSlider, JSlider>
{
    protected UIForSlider(JSlider component) { super(component); }

    public UIForSlider onChange(UIAction<JSlider, ChangeEvent> action) {
        LogUtil.nullArgCheck(action, "action", UIAction.class);
        this.component.addChangeListener( e -> action.accept(this.component, e) );
        return this;
    }

}