package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.UIAction;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

/**
 *  A swing tree builder node for {@link JTextField} instances.
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class or other classes.</b>
 */
public class UIForTextField<F extends JTextField> extends UIForAbstractTextComponent<UIForTextField<F>, F>
{
    protected UIForTextField( F component ) { super(component); }

    public UIForTextField<F> onEnter( UIAction<SimpleDelegate<F, ActionEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", UIAction.class);
        F field = getComponent();
        _onEnter( e -> _doApp(()->action.accept(new SimpleDelegate<>( field, e, this::getSiblinghood )) ) );
        return this;
    }

    private void _onEnter(Consumer<ActionEvent> consumer ) {
        getComponent().addActionListener(consumer::accept);
    }

}
