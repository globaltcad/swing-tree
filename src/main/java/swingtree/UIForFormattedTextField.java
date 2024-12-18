package swingtree;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sprouts.Action;

import javax.swing.JFormattedTextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Objects;
import java.util.function.Consumer;

public final class UIForFormattedTextField extends UIForAnyTextComponent<UIForFormattedTextField, JFormattedTextField>
{
    private static final Logger log = LoggerFactory.getLogger(UIForFormattedTextField.class);
    private final BuilderState<JFormattedTextField> _state;

    UIForFormattedTextField( BuilderState<JFormattedTextField> state ) {
        Objects.requireNonNull(state);
        _state = state;
    }

    @Override
    protected BuilderState<JFormattedTextField> _state() {
        return _state;
    }
    
    @Override
    protected UIForFormattedTextField _newBuilderWithState(BuilderState<JFormattedTextField> newState ) {
        return new UIForFormattedTextField(newState);
    }

    public UIForFormattedTextField onEnter( Action<ComponentDelegate<JFormattedTextField, ActionEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        return _with( thisComponent -> {
                    _onEnter(thisComponent,
                        e -> _runInApp(()->{
                            try {
                                action.accept(new ComponentDelegate<>(thisComponent, e));
                            } catch (Exception ex) {
                                log.error("Error occurred while processing enter action event.", ex);
                            }
                        } )
                    );
                })
                ._this();
    }

    private void _onEnter( JFormattedTextField thisComponent, Consumer<ActionEvent> action ) {
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
        ActionListener[] listeners = thisComponent.getActionListeners();
        for (ActionListener listener : listeners)
            thisComponent.removeActionListener(listener);

        thisComponent.addActionListener(action::accept);

        for ( int i = listeners.length - 1; i >= 0; i-- ) // reverse order because swing does not give us the listeners in the order they were added!
            thisComponent.addActionListener(listeners[i]);
    }

}
