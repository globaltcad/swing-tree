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

    public UIForSlider onChange(Consumer<EventContext<JSlider, ChangeEvent>> action) {
        LogUtil.nullArgCheck(action, "action", Consumer.class);
        this.component.addChangeListener( e -> action.accept(new EventContext<>(this.component, e)) );
        return this;
    }

    public UIForSlider onChangeComponent(Consumer<JSlider> action) {
        LogUtil.nullArgCheck(action, "action", Consumer.class);
        return this.onChange( it -> action.accept(it.getComponent()) );
    }

    public UIForSlider onChangeEvent(Consumer<ChangeEvent> action) {
        LogUtil.nullArgCheck(action, "action", Consumer.class);
        return this.onChange( it -> action.accept(it.getEvent()) );
    }
}