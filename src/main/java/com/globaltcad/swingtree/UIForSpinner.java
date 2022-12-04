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
        NullUtil.nullArgCheck(action, "action", UIAction.class);
        S spinner = getComponent();
        _onChange(e -> _doApp(()->action.accept(new SimpleDelegate<>(spinner, e, this::getSiblinghood))) );
        return this;
    }

    private void _onChange( Consumer<ChangeEvent> consumer ) {
        getComponent().addChangeListener(consumer::accept);
    }

    /**
     * Sets the value of the spinner.
     *
     * @param value The value to set.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code value} is {@code null}.
     */
    public final UIForSpinner<S> withValue( Object value ) {
        NullUtil.nullArgCheck(value, "value", Object.class);
        getComponent().setValue(value);
        return this;
    }

    /**
     * Sets the value of the spinner and also binds to said value.
     *
     * @param value The {@link com.globaltcad.swingtree.api.mvvm.Val} wrapper whose value should be set.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code value} is {@code null}.
     */
    public final UIForSpinner<S> withValue( Val<?> value ) {
        NullUtil.nullArgCheck(value, "value", Val.class);
        NullUtil.nullPropertyCheck(value, "value", "Null is not a valid spinner state!");
        _onShow(value, this::withValue);
        return withValue(value.get());
    }

    /**
     * Sets the value of the spinner and also binds to the provided property.
     *
     * @param value The {@link com.globaltcad.swingtree.api.mvvm.Var} wrapper whose value should be set.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code value} is {@code null}.
     */
    public final UIForSpinner<S> withValue( Var<?> value ) {
        NullUtil.nullArgCheck(value, "value", Var.class);
        NullUtil.nullPropertyCheck(value, "value", "Null is not a valid spinner state!");
        _onShow( value, this::withValue );
        _onChange( e -> _doApp(() -> {
            Object current = getComponent().getValue();
            if ( current != null && Number.class.isAssignableFrom(value.type()) ) {
                if ( Number.class.isAssignableFrom(current.getClass()) ) {
                    Number n = (Number) current;
                    if      ( value.type() == Integer.class ) current = n.intValue();
                    else if ( value.type() == Long.class    ) current = n.longValue();
                    else if ( value.type() == Float.class   ) current = n.floatValue();
                    else if ( value.type() == Double.class  ) current = n.doubleValue();
                    else if ( value.type() == Short.class   ) current = n.shortValue();
                    else if ( value.type() == Byte.class    ) current = n.byteValue();
                }
                if ( current.getClass() == String.class ) {
                    if      ( value.type() == Integer.class ) current = Integer.parseInt((String) current);
                    else if ( value.type() == Long.class    ) current = Long.parseLong((String) current);
                    else if ( value.type() == Float.class   ) current = Float.parseFloat((String) current);
                    else if ( value.type() == Double.class  ) current = Double.parseDouble((String) current);
                    else if ( value.type() == Short.class   ) current = Short.parseShort((String) current);
                    else if ( value.type() == Byte.class    ) current = Byte.parseByte((String) current);
                }
            }
            ((Var<Object>) value).act(current);
        }));
        getComponent().setValue(value.get());
        return withValue( value.get() );
    }

    /**
     * Sets the numeric step size of the value of the spinner.
     * This expects your spinner to be based on the {@link SpinnerNumberModel}.
     *
     * @param n The {@link Number} which should be set as step size.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForSpinner<S> withStepSize( Number n ) {
        NullUtil.nullArgCheck(n, "n", Number.class);
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
        NullUtil.nullArgCheck(val, "val", Val.class);
        NullUtil.nullPropertyCheck(val, "val", "Null is not a valid spinner step size!");
        SpinnerModel model = getComponent().getModel();
        if ( !(model instanceof SpinnerNumberModel) )
            throw new IllegalArgumentException(
                    "This JSpinner can not have a numeric step size as it is not based on the SpinnerNumberModel!"
                );
        SpinnerNumberModel numberModel = (SpinnerNumberModel) model;
        _onShow(val, numberModel::setStepSize);
        numberModel.setStepSize(val.get());
        return this;
    }
}
