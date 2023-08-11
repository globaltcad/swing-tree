package swingtree;

import sprouts.Action;
import sprouts.Val;
import sprouts.Var;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

/**
 *  A swing tree builder node for {@link JTextField} instances.
 * 	<p>
 * 	<b>Take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class or other classes.</b>
 */
public class UIForTextField<F extends JTextField> extends UIForAnyTextComponent<UIForTextField<F>, F>
{
    protected UIForTextField( F component ) { super( component ); }

    /**
     *  Allows you to register an action to be performed when the user presses the enter key.
     *
     * @param action The action to be performed.
     * @return This very instance, which enables builder-style method chaining.
     */
    public UIForTextField<F> onEnter( Action<ComponentDelegate<F, ActionEvent>> action ) {
        NullUtil.nullArgCheck(action, "action", Action.class);
        F field = getComponent();
        _onEnter( e -> _doApp( () -> action.accept(new ComponentDelegate<>( field, e, this::getSiblinghood )) ) );
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
        Var<Boolean> isValid = Var.of(true);
        return this.withNumber( number, isValid );
    }

    /**
     *  Effectively bind this text field to a numeric {@link Var} property
     *  which will only accept numbers as input.
     *
     * @param number The numeric {@link Var} property to bind to.
     * @param isValid A {@link Var} property which will be set to {@code true} if the input is valid, and {@code false} otherwise.
     * @return This builder node.
     * @param <N> The numeric type of the {@link Var} property.
     */
    public final <N extends Number> UIForTextField<F> withNumber( Var<N> number, Var<Boolean> isValid ) {
        NullUtil.nullArgCheck(number, "number", Var.class);
        NullUtil.nullArgCheck(isValid, "isValid", Var.class);
        NullUtil.nullPropertyCheck(number, "number", "Null is not a valid value for a numeric property.");
        NullUtil.nullPropertyCheck(isValid, "isValid", "Null is not a valid value for a boolean property.");
        _onShow( number, n -> _setTextSilently( getComponent(), n.toString() ) );
        Var<String> text = Var.of( number.get().toString() );
        text.onAct( s -> {
            try {
                if ( number.type() == Integer.class )
                    number.act( (N) Integer.valueOf(Integer.parseInt(s.get())) );
                else if ( number.type() == Long.class )
                    number.act( (N) Long.valueOf(Long.parseLong(s.get())) );
                else if ( number.type() == Float.class )
                    number.act( (N) Float.valueOf(Float.parseFloat(s.get())) );
                else if ( number.type() == Double.class )
                    number.act( (N) Double.valueOf(Double.parseDouble(s.get())) );
                else if ( number.type() == Short.class )
                    number.act( (N) Short.valueOf(Short.parseShort(s.get())) );
                else if ( number.type() == Byte.class )
                    number.act( (N) Byte.valueOf(Byte.parseByte(s.get())) );
                else
                    throw new IllegalStateException("Unsupported number type: " + number.type());

                if ( isValid.is(false) ) {
                    isValid.set(true);
                    isValid.fireAct();
                }
            } catch (NumberFormatException e) {
                // ignore
                if ( isValid.is(true) ) {
                    isValid.set(false);
                    isValid.fireAct();
                }
            }
        });
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
        _onShow( number, n -> _setTextSilently( getComponent(), n.toString() ) );
        return this;
    }

    /**
     * The provided {@link UI.HorizontalAlignment} translates to {@link JTextField#setHorizontalAlignment(int)}
     * instances which are used to align the elements or text within the wrapped {@link JTextComponent}.
     * {@link LayoutManager} and {@link Component}
     * subclasses will use this property to
     * determine how to lay out and draw components.
     * <p>
     * Note: This method indirectly changes layout-related information, and therefore,
     * invalidates the component hierarchy.
     *
     * @param direction The text orientation type which should be used.
     * @return This very builder to allow for method chaining.
     */
    public final UIForTextField<F> withTextOrientation( UI.HorizontalAlignment direction ) {
        NullUtil.nullArgCheck(direction, "direction", UI.HorizontalAlignment.class);
        getComponent().setHorizontalAlignment(direction.forSwing());
        return _this();
    }

}
