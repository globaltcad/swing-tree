package swingtree;

import swingtree.api.mvvm.Val;

import javax.swing.*;

/**
 *  A swing tree builder node for {@link JPasswordField} instances.
 */
public class UIForPasswordField<F extends JPasswordField> extends UIForAbstractTextComponent<UIForPasswordField<F>, F>
{
    protected UIForPasswordField(F component) { super(component); }

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
        getComponent().setEchoChar(echoChar);
        return this;
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
        _onShow(echoChar, v -> withEchoChar(v) );
        return this;
    }

}
