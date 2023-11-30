package swingtree.input;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 *  This is a simple Singleton class representing the current state of the keyboard.
 *  <b>It is not a keyboard shortcut implementation</b> but merely a fast API to check if
 *  a certain key on the keyboard has been pressed...
 *  The keyboard class <b>can not be used to hook up lambda callbacks</b>, but merely ought
 *  to serve as <b>a common way to query the current state of the keyboard</b>!
 */
public class Keyboard
{
    private final static Keyboard INSTANCE = new Keyboard();

    /**
     * @return The singleton instance of the {@link Keyboard} class.
     */
    public static Keyboard get() { return INSTANCE; }

    /**
     *  This enum represents all the keys on the keyboard which can be queried for their current state.
     *  This is a mapping of the {@link KeyEvent} constants to a more readable enum.
     */
    public enum Key
    {
        A(KeyEvent.VK_A), B(KeyEvent.VK_B), C(KeyEvent.VK_C), D(KeyEvent.VK_D), E(KeyEvent.VK_E), F(KeyEvent.VK_F),
        G(KeyEvent.VK_G), H(KeyEvent.VK_H), I(KeyEvent.VK_I), J(KeyEvent.VK_J), K(KeyEvent.VK_K), L(KeyEvent.VK_L),
        M(KeyEvent.VK_M), N(KeyEvent.VK_N), O(KeyEvent.VK_O), P(KeyEvent.VK_P), Q(KeyEvent.VK_Q), R(KeyEvent.VK_R),
        S(KeyEvent.VK_S), T(KeyEvent.VK_T), U(KeyEvent.VK_U), V(KeyEvent.VK_V), W(KeyEvent.VK_W), X(KeyEvent.VK_X),
        Y(KeyEvent.VK_Y), Z(KeyEvent.VK_Z),

        SHIFT(KeyEvent.VK_SHIFT), CTRL(KeyEvent.VK_CONTROL), ALT(KeyEvent.VK_ALT), SPACE(KeyEvent.VK_SPACE),
        ESCAPE(KeyEvent.VK_ESCAPE), ENTER(KeyEvent.VK_ENTER), BACKSPACE(KeyEvent.VK_BACK_SPACE), TAB(KeyEvent.VK_TAB),

        ZERO(KeyEvent.VK_0), ONE(KeyEvent.VK_1), TWO(KeyEvent.VK_2), THREE(KeyEvent.VK_3), FOUR(KeyEvent.VK_4),
        FIVE(KeyEvent.VK_5), SIX(KeyEvent.VK_6), SEVEN(KeyEvent.VK_7), EIGHT(KeyEvent.VK_8), NINE(KeyEvent.VK_9),
        SEMICOLON(KeyEvent.VK_SEMICOLON),
        EQUALS(KeyEvent.VK_EQUALS), COMMA(KeyEvent.VK_COMMA), MINUS(KeyEvent.VK_MINUS), PERIOD(KeyEvent.VK_PERIOD),
        SLASH(KeyEvent.VK_SLASH), BACK_SLASH(KeyEvent.VK_BACK_SLASH),
        OPEN_BRACKET(KeyEvent.VK_OPEN_BRACKET), CLOSE_BRACKET(KeyEvent.VK_CLOSE_BRACKET),
        NUMPAD_0(KeyEvent.VK_NUMPAD0), NUMPAD_1(KeyEvent.VK_NUMPAD1), NUMPAD_2(KeyEvent.VK_NUMPAD2),
        NUMPAD_3(KeyEvent.VK_NUMPAD3), NUMPAD_4(KeyEvent.VK_NUMPAD4), NUMPAD_5(KeyEvent.VK_NUMPAD5),
        NUMPAD_6(KeyEvent.VK_NUMPAD6), NUMPAD_7(KeyEvent.VK_NUMPAD7), NUMPAD_8(KeyEvent.VK_NUMPAD8),
        NUMPAD_9(KeyEvent.VK_NUMPAD9),
        NUMPAD_MULTIPLY(KeyEvent.VK_MULTIPLY), NUMPAD_ADD(KeyEvent.VK_ADD),
        F1(KeyEvent.VK_F1), F2(KeyEvent.VK_F2), F3(KeyEvent.VK_F3), F4(KeyEvent.VK_F4), F5(KeyEvent.VK_F5),
        F6(KeyEvent.VK_F6), F7(KeyEvent.VK_F7), F8(KeyEvent.VK_F8), F9(KeyEvent.VK_F9), F10(KeyEvent.VK_F10),
        F11(KeyEvent.VK_F11), F12(KeyEvent.VK_F12), F13(KeyEvent.VK_F13), F14(KeyEvent.VK_F14), F15(KeyEvent.VK_F15),
        F16(KeyEvent.VK_F16), F17(KeyEvent.VK_F17), F18(KeyEvent.VK_F18), F19(KeyEvent.VK_F19), F20(KeyEvent.VK_F20),
        F21(KeyEvent.VK_F21), F22(KeyEvent.VK_F22), F23(KeyEvent.VK_F23), F24(KeyEvent.VK_F24),
        DELETE(KeyEvent.VK_DELETE), INSERT(KeyEvent.VK_INSERT), HOME(KeyEvent.VK_HOME), END(KeyEvent.VK_END),
        UP(KeyEvent.VK_UP), DOWN(KeyEvent.VK_DOWN), LEFT(KeyEvent.VK_LEFT), RIGHT(KeyEvent.VK_RIGHT);


        public final int code;

        Key( int keyCode ) { this.code = keyCode; }

        /**
         *  This method checks if this key is currently pressed on the keyboard
         *  by checking if it is in a list of currently pressed keys.
         *
         * @return The truth value determining if this key is currently pressed on the keyboard.
         */
        public boolean isPressed() {
            return Keyboard.get().isPressed(this);
        }

    }

    private final List<Key> _pressed = new ArrayList<>();

    /**
     *  This method checks if the supplied {@link Key} is currently pressed on the keyboard.
     *
     * @param key An instance of the {@link Key} enum representing a keyboard key.
     * @return The truth value determining if the specified {@link Key} type is currently pressed by the user.
     */
    public boolean isPressed(Key key) {
        synchronized ( this ) {
            return _pressed.contains(key);
        }
    }


    public Keyboard() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent ke) {
                synchronized (this) {
                    switch ( ke.getID() ) {
                        case KeyEvent.KEY_PRESSED:
                            _pressed.add(_fromKeyEvent(ke));
                            break;

                        case KeyEvent.KEY_RELEASED:
                            _pressed.remove(_fromKeyEvent(ke));
                            break;
                    }
                    return false;
                }
            }
        });
    }

    private static Key _fromKeyEvent( KeyEvent keyEvent ) {
        for ( Key key : Key.values() ) {
            if ( key.code == keyEvent.getKeyCode() ) {
                return key;
            }
        }
        return Key.SHIFT;
    }

}

