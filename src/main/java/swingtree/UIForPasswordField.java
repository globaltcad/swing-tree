package swingtree;

import sprouts.Val;

import javax.swing.*;

/**
 *  A SwingTree builder node designed for configuring {@link JPasswordField} instances.
 */
public class UIForPasswordField<F extends JPasswordField> extends UIForAnyTextComponent<UIForPasswordField<F>, F>
{
    protected UIForPasswordField( F component ) { super(component); }

    /**
     * Sets the echo character for this {@link JPasswordField}.
     * Note that this is largely a suggestion, since the
     * view that gets installed can use whatever graphic techniques
     * it desires to represent the field.  Setting a value of 0 indicates
     * that you wish to see the text as it is typed, similar to
     * the behavior of a standard {@link JTextField}.
     *
     * @param echoChar The echo character to display.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForPasswordField<F> withEchoChar( char echoChar ) {
        return _with( thisComponent -> {
                    thisComponent.setEchoChar(echoChar);
                })
                ._this();
    }

    /**
     * Binds to a echo character property for this {@link JPasswordField}.
     * Note that this is largely a suggestion, since the
     * view that gets installed can use whatever graphic techniques
     * it desires to represent the field.  Setting a value of 0 indicates
     * that you wish to see the text as it is typed, similar to
     * the behavior of a standard {@link JTextField}.
     *
     * @param echoChar The echo character property to display dynamically.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForPasswordField<F> withEchoChar( Val<Character> echoChar ) {
        NullUtil.nullArgCheck( echoChar, "echoChar", Val.class );
        NullUtil.nullPropertyCheck( echoChar, "echoChar", "Null is not a valid echo character." );
        return _with( thisComponent -> {
                    _onShow( echoChar, it -> thisComponent.setEchoChar( it ) );
                    thisComponent.setEchoChar( echoChar.orElseThrow() );
                })
                ._this();
    }

}
