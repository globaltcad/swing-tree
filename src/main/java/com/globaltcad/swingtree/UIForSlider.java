package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.UIAction;
import com.globaltcad.swingtree.api.mvvm.Val;

import javax.swing.*;
import javax.swing.event.ChangeEvent;

/**
 *  A swing tree builder node for {@link JSlider} instances.
 */
public class UIForSlider<S extends JSlider> extends UIForAbstractSwing<UIForSlider<S>, S>
{
    protected UIForSlider(S component) { super(component); }

    /**
     * Adds an {@link UIAction} to the underlying {@link JSlider}
     * through an {@link javax.swing.event.ChangeListener},
     * which will be called when the state of the slider changes.
     * For more information see {@link JSlider#addChangeListener(javax.swing.event.ChangeListener)}.
     *
     * @param action The {@link UIAction} that will be called through the underlying change event.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForSlider<S> onChange(UIAction<SimpleDelegate<JSlider, ChangeEvent>> action) {
        LogUtil.nullArgCheck(action, "action", UIAction.class);
        _component.addChangeListener(e -> action.accept(new SimpleDelegate<>(_component, e, ()->getSiblinghood())) );
        return this;
    }

    public final UIForSlider<S> withValue( Val<Integer> val ) {
        val.onView(_component::setValue);
        return this;
    }

    public final UIForSlider<S> withMin( Val<Integer> val ) {
        val.onView(_component::setMinimum);
        return this;
    }

    public final UIForSlider<S> withMax( Val<Integer> val ) {
        val.onView(_component::setMaximum);
        return this;
    }

    public final UIForSlider<S> withMin( int min ) {
        _component.setMinimum( min );
        return this;
    }

    public final UIForSlider<S> withMax( int max ) {
        _component.setMaximum( max );
        return this;
    }

    public final UIForSlider<S> withValue( int value ) {
        _component.setValue( value );
        return this;
    }

}