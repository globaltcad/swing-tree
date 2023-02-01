package swingtree;

import sprouts.Action;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

public class UIForFormattedTextField extends UIForAbstractTextComponent<UIForFormattedTextField, JFormattedTextField>
{
    protected UIForFormattedTextField( JFormattedTextField component ) { super(component); }

    public UIForFormattedTextField onEnter( Action<SimpleDelegate<JFormattedTextField, ActionEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        JFormattedTextField field = getComponent();
        _onEnter( e -> _doApp(()->action.accept(new SimpleDelegate<>( field, e, this::getSiblinghood )) ) );
        return this;
    }

    private void _onEnter( Consumer<ActionEvent> action ) {
        /*
            When an action event is fired, Swing will go through all the listeners
            from the most recently added to the first added. This means that if we simply add
            a listener through the "addActionListener" method, we will be the last to be notified.
            This is problematic because it is built on the assumption that the last listener
            added is more interested in the event than the first listener added.
            This however is an unintuitive assumption, meaning a user would expect
            the first listener added to be the most interested in the event
            simply because it was added first.
            This is especially true in the context of declarative UI design.
        */
        ActionListener[] listeners = getComponent().getActionListeners();
        for (ActionListener listener : listeners)
            getComponent().removeActionListener(listener);

        getComponent().addActionListener(action::accept);

        for ( int i = listeners.length - 1; i >= 0; i-- ) // reverse order because swing does not give us the listeners in the order they were added!
            getComponent().addActionListener(listeners[i]);
    }

}
