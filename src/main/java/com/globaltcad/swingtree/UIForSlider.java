package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.UIAction;

import javax.swing.*;
import javax.swing.event.ChangeEvent;

/**
 *  A swing tree builder for {@link JSlider} instances.
 */
public class UIForSlider<S extends JSlider> extends UIForAbstractSwing<UIForSlider<S>, S>
{
    protected UIForSlider(S component) { super(component); }


    public final UIForSlider<S> onChange(UIAction<SimpleDelegate<JSlider, ChangeEvent>> action) {
        LogUtil.nullArgCheck(action, "action", UIAction.class);
        this.component.addChangeListener( e -> action.accept(new SimpleDelegate<>(this.component, e, ()->getSiblinghood())) );
        return this;
    }

}