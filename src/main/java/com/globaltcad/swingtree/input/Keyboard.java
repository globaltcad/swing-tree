package com.globaltcad.swingtree.input;

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

    public static Keyboard get() { return INSTANCE; }

    public enum Key
    {
        A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z,
        SHIFT, CTRL, ALT, SPACE,
        ZERO, ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE,
        SEMICOLON,
        EQUALS;

        public boolean isPressed() {
            return Keyboard.get().isPressed(this);
        }

    }

    private final List<Key> pressed = new ArrayList<>();

    /**
     *  This method checks if the supplied {@link Key} is currently pressed on the keyboard.
     *
     * @param key An instance of the {@link Key} enum representing a keyboard key.
     * @return The truth value determining if the specified {@link Key} type is currently pressed by the user.
     */
    public boolean isPressed(Key key) { synchronized (this) { return pressed.contains(key); } }


    public Keyboard() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent ke) {
                synchronized (this) {
                    switch (ke.getID()) {
                        case KeyEvent.KEY_PRESSED:
                            pressed.add(fromKeyEvent(ke));
                            break;

                        case KeyEvent.KEY_RELEASED:
                            pressed.remove(fromKeyEvent(ke));
                            break;
                    }
                    return false;
                }
            }
        });
    }


    private static Key fromKeyEvent( KeyEvent keyEvent ) {
        switch (keyEvent.getKeyCode()) {
            case KeyEvent.VK_SHIFT    : return Key.SHIFT;
            case KeyEvent.VK_CONTROL  : return Key.CTRL;
            case KeyEvent.VK_ALT      : return Key.ALT;
            case KeyEvent.VK_SPACE    : return Key.SPACE;
            case KeyEvent.VK_0        : return Key.ZERO;
            case KeyEvent.VK_1        : return Key.ONE;
            case KeyEvent.VK_2        : return Key.TWO;
            case KeyEvent.VK_3        : return Key.THREE;
            case KeyEvent.VK_4        : return Key.FOUR;
            case KeyEvent.VK_5        : return Key.FIVE;
            case KeyEvent.VK_6        : return Key.SIX;
            case KeyEvent.VK_7        : return Key.SEVEN;
            case KeyEvent.VK_8        : return Key.EIGHT;
            case KeyEvent.VK_9        : return Key.NINE;
            case KeyEvent.VK_SEMICOLON: return Key.SEMICOLON;
            case KeyEvent.VK_EQUALS   : return Key.EQUALS   ;
            case KeyEvent.VK_A        : return Key.A        ;
            case KeyEvent.VK_B        : return Key.B        ;
            case KeyEvent.VK_C        : return Key.C        ;
            case KeyEvent.VK_D        : return Key.D        ;
            case KeyEvent.VK_E        : return Key.E        ;
            case KeyEvent.VK_F        : return Key.F        ;
            case KeyEvent.VK_G        : return Key.G        ;
            case KeyEvent.VK_H        : return Key.H        ;
            case KeyEvent.VK_I        : return Key.I        ;
            case KeyEvent.VK_J        : return Key.J        ;
            case KeyEvent.VK_K        : return Key.K        ;
            case KeyEvent.VK_L        : return Key.L        ;
            case KeyEvent.VK_M        : return Key.M        ;
            case KeyEvent.VK_N        : return Key.N        ;
            case KeyEvent.VK_O        : return Key.O        ;
            case KeyEvent.VK_P        : return Key.P        ;
            case KeyEvent.VK_Q        : return Key.Q        ;
            case KeyEvent.VK_R        : return Key.R        ;
            case KeyEvent.VK_S        : return Key.S        ;
            case KeyEvent.VK_T        : return Key.T        ;
            case KeyEvent.VK_U        : return Key.U        ;
            case KeyEvent.VK_V        : return Key.V        ;
            case KeyEvent.VK_W        : return Key.W        ;
            case KeyEvent.VK_X        : return Key.X        ;
            case KeyEvent.VK_Y        : return Key.Y        ;
            case KeyEvent.VK_Z        : return Key.Z        ;
            default:
                return Key.SHIFT;
        }
    }

}

