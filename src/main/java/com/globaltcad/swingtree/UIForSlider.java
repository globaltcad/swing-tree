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
        S slider = getComponent();
        slider.addChangeListener(e -> doApp(()->action.accept(new SimpleDelegate<>(slider, e, ()->getSiblinghood()))) );
        return this;
    }

    public final UIForSlider<S> withValue( Val<Integer> val ) {
        S slider = getComponent();
        val.onShow(v->doUI(()->slider.setValue(v)));
        return this;
    }

    public final UIForSlider<S> withMin( Val<Integer> val ) {
        S slider = getComponent();
        val.onShow(v->doUI(()->slider.setMinimum(v)));
        return this;
    }

    public final UIForSlider<S> withMax( Val<Integer> val ) {
        S slider = getComponent();
        val.onShow(v->doUI(()->slider.setMaximum(v)));
        return this;
    }

    public final UIForSlider<S> withMin( int min ) {
        getComponent().setMinimum( min );
        return this;
    }

    public final UIForSlider<S> withMax( int max ) {
        getComponent().setMaximum( max );
        return this;
    }

    public final UIForSlider<S> withValue( int value ) {
        getComponent().setValue( value );
        return this;
    }

}