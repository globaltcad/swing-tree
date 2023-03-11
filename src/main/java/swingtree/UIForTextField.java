package swingtree;

import sprouts.Action;
import sprouts.Val;
import sprouts.Var;
import sprouts.Vars;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

/**
 *  A swing tree builder node for {@link JTextField} instances.
 * 	<p>
 * 	<b>Take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class or other classes.</b>
 */
public class UIForTextField<F extends JTextField> extends UIForAbstractTextComponent<UIForTextField<F>, F>
{
    protected UIForTextField( F component ) { super( component ); }

    /**
     *  Allows you to register an action to be performed when the user presses the enter key.
     *
     * @param action The action to be performed.
     * @return This very instance, which enables builder-style method chaining.
     */
    public UIForTextField<F> onEnter( Action<SimpleDelegate<F, ActionEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        F field = getComponent();
        _onEnter( e -> _doApp( () -> action.accept(new SimpleDelegate<>( field, e, this::getSiblinghood )) ) );
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

    /**
     *  Effectively bind this text field to a numeric {@link Var} property
     *  which will only accept numbers as input.
     *
     * @param number The numeric {@link Var} property to bind to.
     * @return This builder node.
     * @param <N> The numeric type of the {@link Var} property.
     */
    public final <N extends Number> UIForTextField<F> withNumber( Var<N> number ) {
        NullUtil.nullArgCheck(number, "number", Var.class);
        number.onSet( n -> _doUI(()->getComponent().setText( n.toString() )) );
        Var<String> text = Var.of( getComponent().getText() );
        text.onSet( s -> {
            try {
                if ( number.type() == Integer.class )
                    number.set( (N) Integer.valueOf(Integer.parseInt(s.get())) );
                else if ( number.type() == Long.class )
                    number.set( (N) Long.valueOf(Long.parseLong(s.get())) );
                else if ( number.type() == Float.class )
                    number.set( (N) Float.valueOf(Float.parseFloat(s.get())) );
                else if ( number.type() == Double.class )
                    number.set( (N) Double.valueOf(Double.parseDouble(s.get())) );
                else if ( number.type() == Short.class )
                    number.set( (N) Short.valueOf(Short.parseShort(s.get())) );
                else if ( number.type() == Byte.class )
                    number.set( (N) Byte.valueOf(Byte.parseByte(s.get())) );
                else
                    throw new IllegalStateException("Unsupported number type: " + number.type());
            } catch (NumberFormatException e) {
                // ignore
            }
        } );
        return withText( text );
    }

    /**
     *  Effectively bind this text field to a numeric {@link Val} property
     *  but only for reading purposes.
     *  So the text field will be updated when the {@link Val} property changes
     *  but the user will not be able to change the {@link Val} property
     *  since the {@link Val} property is read-only.
     *
     * @param number The numeric {@link Val} property to bind to.
     * @return This builder node.
     * @param <N> The numeric type of the {@link Val} property.
     */
    public final <N extends Number> UIForTextField<F> withNumber( Val<N> number ) {
        NullUtil.nullArgCheck(number, "number", Var.class);
        number.onSet( n -> _doUI(()->getComponent().setText( n.toString() )) );
        return this;
    }

}
