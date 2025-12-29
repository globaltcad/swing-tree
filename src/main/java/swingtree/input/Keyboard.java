package swingtree.input;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import swingtree.SwingTree;
import swingtree.UI;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *  This is a simple Singleton class representing the current state of the keyboard.
 *  <b>It is not a keyboard shortcut implementation</b> but merely a fast API to check if
 *  a certain key on the keyboard has been pressed...
 *  The keyboard class <b>can not be used to hook up lambda callbacks</b>, but merely ought
 *  to serve as <b>a convenient way to query the current state of the keyboard</b>!
 */
public final class Keyboard
{
    private final static Keyboard INSTANCE = new Keyboard();
    private static final Logger log = LoggerFactory.getLogger(Keyboard.class);

    /**
     *  Exposes the singleton instance of the {@link Keyboard} class.
     *
     * @return The singleton instance of the {@link Keyboard} class.
     */
    public static Keyboard get() {
        if ( !UI.thisIsUIThread() )
            log.warn(SwingTree.get().logMarker(),
                "Keyboard.get() should only be called from the UI thread (Swing Event Dispatch Thread).\n" +
                "Encountered thread '{}' instead!", Thread.currentThread().getName(),
                new Throwable() // Stack trace for debugging purposes
            );
        return INSTANCE;
    }

    /**
     *  This enum represents all the keys on the keyboard which can be queried for their current state.
     *  This is a mapping of the {@link KeyEvent} constants to a more readable and type safe enum.
     */
    public enum Key
    {
        /**
         * This value is used to indicate that the keyCode is unknown.
         * KEY_TYPED events do not have a keyCode value; this value
         * is used instead.
         * See {@link KeyEvent#VK_UNDEFINED} for more information.
         */
        NONE(KeyEvent.VK_UNDEFINED),
        /** Constant for the ENTER virtual key. See {@link KeyEvent#VK_ENTER} for the underlying key-code. */
        ENTER(KeyEvent.VK_ENTER),
        /** Constant for the BACK_SPACE virtual key. See {@link KeyEvent#VK_BACK_SPACE} for the underlying key-code.*/
        BACK_SPACE(KeyEvent.VK_BACK_SPACE),
        /** Constant for the TAB virtual key. See {@link KeyEvent#VK_TAB} for the underlying key-code.*/
        TAB(KeyEvent.VK_TAB),
        /** Constant for the CANCEL virtual key. See {@link KeyEvent#VK_CANCEL} for the underlying key-code.*/
        CANCEL(KeyEvent.VK_CANCEL),
        /** Constant for the CLEAR virtual key. See {@link KeyEvent#VK_CLEAR} for the underlying key-code.*/
        CLEAR(KeyEvent.VK_CLEAR),
        /** Constant for the SHIFT virtual key. See {@link KeyEvent#VK_SHIFT} for the underlying key-code.*/
        SHIFT(KeyEvent.VK_SHIFT),
        /** Constant for the CONTROL virtual key. See {@link KeyEvent#VK_CONTROL} for the underlying key-code.*/
        CONTROL(KeyEvent.VK_CONTROL),
        /** Constant for the ALT virtual key. See {@link KeyEvent#VK_ALT} for the underlying key-code.*/
        ALT(KeyEvent.VK_ALT),
        /** Constant for the PAUSE virtual key. See {@link KeyEvent#VK_PAUSE} for the underlying key-code.*/
        PAUSE(KeyEvent.VK_PAUSE),
        /** Constant for the CAPS_LOCK virtual key. See {@link KeyEvent#VK_CAPS_LOCK} for the underlying key-code.*/
        CAPS_LOCK(KeyEvent.VK_CAPS_LOCK),
        /** Constant for the ESCAPE virtual key. See {@link KeyEvent#VK_ESCAPE} for the underlying key-code.*/
        ESCAPE(KeyEvent.VK_ESCAPE),
        /** Constant for the SPACE virtual key. See {@link KeyEvent#VK_SPACE} for the underlying key-code.*/
        SPACE(KeyEvent.VK_SPACE),
        /** Constant for the PAGE_UP virtual key. See {@link KeyEvent#VK_PAGE_UP} for the underlying key-code.*/
        PAGE_UP(KeyEvent.VK_PAGE_UP),
        /** Constant for the PAGE_DOWN virtual key. See {@link KeyEvent#VK_PAGE_DOWN} for the underlying key-code.*/
        PAGE_DOWN(KeyEvent.VK_PAGE_DOWN),
        /** Constant for the END virtual key. See {@link KeyEvent#VK_END} for the underlying key-code.*/
        END(KeyEvent.VK_END),
        /** Constant for the HOME virtual key. See {@link KeyEvent#VK_HOME} for the underlying key-code.*/
        HOME(KeyEvent.VK_HOME),
        /**
         * Constant for the non-numpad <b>left</b> arrow key. See {@link KeyEvent#VK_LEFT} for the underlying key-code.
         * @see #LEFT
         */
        LEFT(KeyEvent.VK_LEFT),
        /**
         * Constant for the non-numpad <b>up</b> arrow key.See {@link KeyEvent#VK_U} for the underlying key-code.
         * @see #UP
         */
        UP(KeyEvent.VK_UP),
        /**
         * Constant for the non-numpad <b>right</b> arrow key. See {@link KeyEvent#VK_RIGHT} for the underlying key-code.
         * @see #RIGHT
         */
        RIGHT(KeyEvent.VK_RIGHT),
        /**
         * Constant for the non-numpad <b>down</b> arrow key.See {@link KeyEvent#VK_DOWN} for the underlying key-code.
         * @see #DOWN
         */
        DOWN(KeyEvent.VK_DOWN),
        /**
         * Constant for the comma key, ","
         */
        COMMA(KeyEvent.VK_COMMA),
        /**
         * Constant for the minus key, "-"
         */
        MINUS(KeyEvent.VK_MINUS),
        /**
         * Constant for the period key, "."
         */
        PERIOD(KeyEvent.VK_PERIOD),
        /**
         * Constant for the forward slash key, "/"
         */
        SLASH(KeyEvent.VK_SLASH),
        /** Constant for the "0" key. */
        ZERO(KeyEvent.VK_0),
        /** Constant for the "1" key. */
        ONE(KeyEvent.VK_1),
        /** Constant for the "2" key. */
        TWO(KeyEvent.VK_2),
        /** Constant for the "3" key. */
        THREE(KeyEvent.VK_3),
        /** Constant for the "4" key. */
        FOUR(KeyEvent.VK_4),
        /** Constant for the "5" key. */
        FIVE(KeyEvent.VK_5),
        /** Constant for the "6" key. */
        SIX(KeyEvent.VK_6),
        /** Constant for the "7" key. */
        SEVEN(KeyEvent.VK_7),
        /** Constant for the "8" key. */
        EIGHT(KeyEvent.VK_8),
        /** Constant for the "9" key. */
        NINE(KeyEvent.VK_9),

        /**
         * Constant for the semicolon key, ";"
         */
        SEMICOLON(KeyEvent.VK_SEMICOLON),
        /**
         * Constant for the equals key, "="
         */
        EQUALS(KeyEvent.VK_EQUALS),

        A(KeyEvent.VK_A), B(KeyEvent.VK_B), C(KeyEvent.VK_C), D(KeyEvent.VK_D), E(KeyEvent.VK_E), F(KeyEvent.VK_F),
        G(KeyEvent.VK_G), H(KeyEvent.VK_H), I(KeyEvent.VK_I), J(KeyEvent.VK_J), K(KeyEvent.VK_K), L(KeyEvent.VK_L),
        M(KeyEvent.VK_M), N(KeyEvent.VK_N), O(KeyEvent.VK_O), P(KeyEvent.VK_P), Q(KeyEvent.VK_Q), R(KeyEvent.VK_R),
        S(KeyEvent.VK_S), T(KeyEvent.VK_T), U(KeyEvent.VK_U), V(KeyEvent.VK_V), W(KeyEvent.VK_W), X(KeyEvent.VK_X),
        Y(KeyEvent.VK_Y), Z(KeyEvent.VK_Z),

        OPEN_BRACKET(KeyEvent.VK_OPEN_BRACKET),
        BACK_SLASH(KeyEvent.VK_BACK_SLASH),
        CLOSE_BRACKET(KeyEvent.VK_CLOSE_BRACKET),
        NUMPAD_0(KeyEvent.VK_NUMPAD0), NUMPAD_1(KeyEvent.VK_NUMPAD1), NUMPAD_2(KeyEvent.VK_NUMPAD2),
        NUMPAD_3(KeyEvent.VK_NUMPAD3), NUMPAD_4(KeyEvent.VK_NUMPAD4), NUMPAD_5(KeyEvent.VK_NUMPAD5),
        NUMPAD_6(KeyEvent.VK_NUMPAD6), NUMPAD_7(KeyEvent.VK_NUMPAD7), NUMPAD_8(KeyEvent.VK_NUMPAD8),
        NUMPAD_9(KeyEvent.VK_NUMPAD9),
        MULTIPLY(KeyEvent.VK_MULTIPLY), ADD(KeyEvent.VK_ADD), SEPARATOR(KeyEvent.VK_SEPARATOR),
        SUBTRACT(KeyEvent.VK_SUBTRACT), DECIMAL(KeyEvent.VK_DECIMAL), DIVIDE(KeyEvent.VK_DIVIDE),
        DELETE(KeyEvent.VK_DELETE), NUM_LOCK(KeyEvent.VK_NUM_LOCK), SCROLL_LOCK(KeyEvent.VK_SCROLL_LOCK),

        F1(KeyEvent.VK_F1), F2(KeyEvent.VK_F2), F3(KeyEvent.VK_F3), F4(KeyEvent.VK_F4), F5(KeyEvent.VK_F5),
        F6(KeyEvent.VK_F6), F7(KeyEvent.VK_F7), F8(KeyEvent.VK_F8), F9(KeyEvent.VK_F9), F10(KeyEvent.VK_F10),
        F11(KeyEvent.VK_F11), F12(KeyEvent.VK_F12), F13(KeyEvent.VK_F13), F14(KeyEvent.VK_F14), F15(KeyEvent.VK_F15),
        F16(KeyEvent.VK_F16), F17(KeyEvent.VK_F17), F18(KeyEvent.VK_F18), F19(KeyEvent.VK_F19), F20(KeyEvent.VK_F20),
        F21(KeyEvent.VK_F21), F22(KeyEvent.VK_F22), F23(KeyEvent.VK_F23), F24(KeyEvent.VK_F24),

        PRINTSCREEN(KeyEvent.VK_PRINTSCREEN),
        INSERT(KeyEvent.VK_INSERT),
        HELP(KeyEvent.VK_HELP), META(KeyEvent.VK_META), BACK_QUOTE(KeyEvent.VK_BACK_QUOTE),
        QUOTE(KeyEvent.VK_QUOTE);


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
     * @throws NullPointerException If the supplied {@link Key} is null,
     *                              use {@link Key#NONE} instead to check if no key is currently pressed.
     */
    public boolean isPressed( Key key ) {
        Objects.requireNonNull(key);
        synchronized ( this ) {
            if ( key == Key.NONE )
                return _pressed.isEmpty();

            return _pressed.contains(key);
        }
    }


    private Keyboard() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent ke) {
                synchronized (this) {
                    switch ( ke.getID() ) {
                        case KeyEvent.KEY_PRESSED: {
                                Key k = _fromKeyEvent(ke);
                                if ( k != Key.NONE && !_pressed.contains(k) )
                                    _pressed.add(k);
                            }
                            break;

                        case KeyEvent.KEY_RELEASED: {
                                Key k = _fromKeyEvent(ke);
                                if ( k != Key.NONE )
                                    _pressed.remove(k);
                            }
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
        return Key.NONE;
    }

}

