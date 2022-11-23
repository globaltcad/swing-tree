package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.UIAction;
import com.globaltcad.swingtree.api.mvvm.Val;
import com.globaltcad.swingtree.api.mvvm.Var;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.util.function.Consumer;

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
    public final UIForSpinner<S> onChange( UIAction<SimpleDelegate<JSpinner, ChangeEvent>> action ) {
        LogUtil.nullArgCheck(action, "action", UIAction.class);
        S spinner = getComponent();
        _onChange(e -> _doApp(()->action.accept(new SimpleDelegate<>(spinner, e, ()->getSiblinghood()))) );
        return this;
    }

    private void _onChange( Consumer<ChangeEvent> consumer ) {
        getComponent().addChangeListener( e -> consumer.accept(e) );
    }

    /**
     * Sets the value of the spinner.
     *
     * @param value The value to set.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForSpinner<S> withValue( Object value ) {
        getComponent().setValue(value);
        return this;
    }

    /**
     * Sets the value of the spinner and also binds to said value.
     *
     * @param val The {@link com.globaltcad.swingtree.api.mvvm.Val} wrapper whose value should be set.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForSpinner<S> withValue( Val<?> val ) {
        val.onShow(v-> _doUI(()->getComponent().setValue(v)));
        getComponent().setValue(val.get());
        return this;
    }

    /**
     * Sets the value of the spinner and also binds to the provided property.
     *
     * @param var The {@link com.globaltcad.swingtree.api.mvvm.Var} wrapper whose value should be set.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForSpinner<S> withValue( Var<?> var ) {
        var.onShow(v -> _doUI(() -> getComponent().setValue(v)));
        _onChange(e -> _doApp(() -> ((Var<Object>)var).act(getComponent().getValue())));
        getComponent().setValue(var.get());
        return this;
    }

    /**
     * Sets the numeric step size of the value of the spinner.
     * This expects your spinner to be based on the {@link SpinnerNumberModel}.
     *
     * @param n The {@link Number} which should be set as step size.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForSpinner<S> withStepSize( Number n ) {
        SpinnerModel model = getComponent().getModel();
        if ( !(model instanceof SpinnerNumberModel) )
            throw new IllegalArgumentException(
                    "This JSpinner can not have a numeric step size as it is not based on the SpinnerNumberModel!"
            );
        SpinnerNumberModel numberModel = (SpinnerNumberModel) model;
        numberModel.setStepSize(n);
        return this;
    }

    /**
     * Sets the numeric step size of the value of the spinner and also binds to said value.
     * This expects your spinner to be based on the {@link SpinnerNumberModel}.
     *
     * @param val The {@link com.globaltcad.swingtree.api.mvvm.Val} wrapper whose step size should be set.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final <N extends Number> UIForSpinner<S> withStepSize( Val<N> val ) {
        SpinnerModel model = getComponent().getModel();
        if ( !(model instanceof SpinnerNumberModel) )
            throw new IllegalArgumentException(
                    "This JSpinner can not have a numeric step size as it is not based on the SpinnerNumberModel!"
                );
        SpinnerNumberModel numberModel = (SpinnerNumberModel) model;
        val.onShow(v -> _doUI(() -> numberModel.setStepSize(v)));
        numberModel.setStepSize(val.get());
        return this;
    }
}
