package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.UIAction;
import com.globaltcad.swingtree.api.mvvm.Val;
import com.globaltcad.swingtree.api.mvvm.Var;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.util.function.Consumer;

/**
 *  A swing tree builder node for {@link JSlider} instances.
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 */
public class UIForSlider<S extends JSlider> extends UIForAbstractSwing<UIForSlider<S>, S>
{
    protected UIForSlider( S component ) { super(component); }

    /**
     * Adds an {@link UIAction} to the underlying {@link JSlider}
     * through an {@link javax.swing.event.ChangeListener},
     * which will be called when the state of the slider changes.
     * For more information see {@link JSlider#addChangeListener(javax.swing.event.ChangeListener)}.
     *
     * @param action The {@link UIAction} that will be called through the underlying change event.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     */
    public final UIForSlider<S> onChange( UIAction<SimpleDelegate<JSlider, ChangeEvent>> action ) {
        NullUtil.nullArgCheck( action, "action", UIAction.class );
        S slider = getComponent();
        _onChange( e -> _doApp(()->action.accept(new SimpleDelegate<>(slider, e, this::getSiblinghood))) );
        return this;
    }

    private void _onChange( Consumer<ChangeEvent> action ) {
        getComponent().addChangeListener(action::accept);
    }

    public final UIForSlider<S> withMin( int min ) {
        getComponent().setMinimum( min );
        return this;
    }

    /**
     * @param min The min property used to dynamically update the min value of the slider.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code min} is {@code null}.
     */
    public final UIForSlider<S> withMin( Val<Integer> min ) {
        NullUtil.nullArgCheck( min, "min", Val.class );
        _onShow(min, v -> getComponent().setMinimum(v) );
        return this;
    }

    public final UIForSlider<S> withMax( int max ) {
        getComponent().setMaximum( max );
        return this;
    }

    /**
     * @param max An integer property used to dynamically update the max value of the slider.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code max} is {@code null}.
     */
    public final UIForSlider<S> withMax( Val<Integer> max ) {
        NullUtil.nullArgCheck( max, "max", Val.class );
        _onShow(max, v -> getComponent().setMaximum(v) );
        return this;
    }

    public final UIForSlider<S> withValue( int value ) {
        getComponent().setValue( value );
        return this;
    }

    public final UIForSlider<S> withValue( Val<Integer> val ) {
        NullUtil.nullArgCheck( val, "val", Val.class );
        _onShow(val, v -> getComponent().setValue(v) );
        return this;
    }

    public final UIForSlider<S> withValue( Var<Integer> var ) {
        NullUtil.nullArgCheck( var, "var", Var.class );
        _onChange( e -> _doApp(getComponent().getValue(), var::act) );
        return this.withValue((Val<Integer>) var);
    }

}