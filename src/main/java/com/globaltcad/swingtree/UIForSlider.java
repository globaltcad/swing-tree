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
    public final UIForSlider<S> onChange( UIAction<SimpleDelegate<JSlider, ChangeEvent>> action ) {
        LogUtil.nullArgCheck(action, "action", UIAction.class);
        S slider = getComponent();
        _onChange( e -> _doApp(()->action.accept(new SimpleDelegate<>(slider, e, ()->getSiblinghood()))) );
        return this;
    }

    private void _onChange( Consumer<ChangeEvent> action ) {
        getComponent().addChangeListener(e -> action.accept(e) );
    }

    public final UIForSlider<S> withMin( int min ) {
        getComponent().setMinimum( min );
        return this;
    }

    public final UIForSlider<S> withMin( Val<Integer> val ) {
        val.onShow(v-> _doUI(()->getComponent().setMinimum(v)));
        return this;
    }

    public final UIForSlider<S> withMax( int max ) {
        getComponent().setMaximum( max );
        return this;
    }

    public final UIForSlider<S> withMax( Val<Integer> val ) {
        val.onShow(v-> _doUI(()->getComponent().setMaximum(v)));
        return this;
    }

    public final UIForSlider<S> withValue( int value ) {
        getComponent().setValue( value );
        return this;
    }

    public final UIForSlider<S> withValue( Val<Integer> val ) {
        val.onShow(v-> _doUI(()->getComponent().setValue(v)));
        return this;
    }

    public final UIForSlider<S> withValue( Var<Integer> var ) {
        _onChange( e -> _doApp(getComponent().getValue(), v -> var.set(v).act()));
        return this.withValue((Val<Integer>) var);
    }

}