package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.UIAction;
import com.globaltcad.swingtree.api.mvvm.Val;

import javax.swing.*;
import javax.swing.event.ChangeEvent;

/**
 *  A swing tree builder node for {@link JSpinner} instances.
 */
public class UIForSpinner<S extends JSpinner> extends UIForAbstractSwing<UIForSpinner<S>, S>
{
    /**
     * {@link UIForAbstractSwing} (sub)types always wrap
     * a single component for which they are responsible.
     *
     * @param component The {@link JComponent} type which will be wrapped by this builder node.
     */
    protected UIForSpinner(S component) { super(component); }

    /**
     * Adds an {@link UIAction} to the underlying {@link JSpinner}
     * through an {@link javax.swing.event.ChangeListener},
     * Use this to register an action to be performed when the spinner's value changes.
     *
     * @param action The action to be performed.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForSpinner<S> onChange(UIAction<SimpleDelegate<JSpinner, ChangeEvent>> action) {
        LogUtil.nullArgCheck(action, "action", UIAction.class);
        _component.addChangeListener(e -> doApp(()->action.accept(new SimpleDelegate<>(_component, e, ()->getSiblinghood()))) );
        return this;
    }

    /**
     * Sets the value of the spinner.
     *
     * @param value The value to set.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForSpinner<S> withValue( Object value ) {
        _component.setValue(value);
        return this;
    }

    /**
     * Sets the value of the spinner and also binds to said value.
     *
     * @param val The {@link com.globaltcad.swingtree.api.mvvm.Val} wrapper whose value should be set.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForSpinner<S> withValue( Val<Object> val ) {
        val.onView(v->doUI(()->_component.setValue(v)));
        return this;
    }

}
