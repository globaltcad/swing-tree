package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.mvvm.Var;

import javax.swing.*;

/**
 *  A swing tree builder node for {@link JRadioButton} instances.
 */
public class UIForRadioButton<R extends JRadioButton> extends UIForAbstractToggleButton<UIForRadioButton<R>, R>
{
    protected UIForRadioButton( R component ) { super(component); }

    /**
     *  Use this to dynamically bind to an enum based {@link com.globaltcad.swingtree.api.mvvm.Var}
     *  instance which will be used to dynamically model the selection state of the
     *  wrapped {@link JToggleButton} type by checking
     *  weather the property matches the provided enum or not.
     *
     * @param state The reference {@link Enum} which this {@link JToggleButton} should represent.
     * @param selection The {@link com.globaltcad.swingtree.api.mvvm.Var} instance which will be used
     *                  to dynamically model the selection state of the wrapped {@link JToggleButton} type.
     * @throws IllegalArgumentException if {@code selected} is {@code null}.
     */
    public final <E extends Enum<E>> UIForRadioButton<R> isSelectedIf( E state, Var<E> selection ) {
        NullUtil.nullArgCheck(state, "state", Enum.class);
        NullUtil.nullArgCheck(selection, "selection", Var.class);
        NullUtil.nullPropertyCheck(selection, "selection", "Null is not a valid selection state.");
        this.isSelectedIf( state.equals(selection.get()) );
        _onShow( selection, s -> isSelectedIf( state.equals(selection.get()) ) );
        R component = getComponent();
        String currentText = component.getText();
        if ( currentText == null || currentText.isEmpty() ) {
            component.setText( state.name() );
        }
        return this;
    }

}
