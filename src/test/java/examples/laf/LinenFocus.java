package examples.laf;

import javax.swing.*;

import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 *  Bridges focus transitions to a repaint of a styled target component.
 *
 *  <p>The SwingTree style engine re-evaluates a component's
 *  {@link SwingTreeStyledComponentUI#style style(..)} only while that
 *  component repaints (the style is gathered inside
 *  {@code ComponentExtension.paintBackground(..)}). Swing itself does not
 *  repaint a {@link javax.swing.text.JTextComponent} when it gains or loses
 *  focus &mdash; only the caret repaints its own narrow rectangle &mdash; so a
 *  focus-dependent border declared in {@code style(..)} would stay frozen in
 *  its resting state until some unrelated repaint (a resize, a hover) happens
 *  to occur. Buttons, toggles and the slider are unaffected because their
 *  {@code BasicXxxListener}s already repaint on focus.
 *
 *  <p>For composite controls the focus does not even live on the styled
 *  component: a {@link javax.swing.JSpinner}'s (and an editable
 *  {@link javax.swing.JComboBox}'s) focus sits on a descendant editor, so the
 *  {@code focusSource} and the repaint {@code target} differ.
 */
final class LinenFocus {

    private LinenFocus() {}

    /** Stores the installed listener on the {@code target}, both as a guard
     *  against a second {@code installUI(..)} stacking duplicate listeners and
     *  as the handle {@link #uninstall} needs to remove it again. Keyed on the
     *  target rather than the source because one source (a spinner's inner
     *  editor) legitimately feeds two targets: the editor field itself and the
     *  surrounding spinner. */
    private static final String LISTENER = "linen.focusRepaint.listener";

    /**
     *  Ensures {@code target} repaints whenever {@code focusSource} gains or
     *  loses focus, re-running the SwingTree styler so a focus-dependent
     *  border updates immediately instead of on the next unrelated repaint.
     *  Pair every call with {@link #uninstall} from the delegate's
     *  {@code uninstallUI(..)} so the listener does not outlive the look and
     *  feel.
     *
     * @param target      the styled component to repaint (never {@code null})
     * @param focusSource the component that actually owns focus; the same as
     *                    {@code target} for plain fields, or a descendant
     *                    editor for composite controls
     */
    static void repaintOnFocus(JComponent target, Component focusSource) {
        if (focusSource == null)
            return;

        if (target.getClientProperty(LISTENER) != null)
            return; // already wired (e.g. a redundant updateUI())

        FocusListener listener = new FocusListener() {
            @Override public void focusGained(FocusEvent e) { repaint(target); }
            @Override public void focusLost(FocusEvent e)   { repaint(target); }
        };
        focusSource.addFocusListener(listener);
        target.putClientProperty(LISTENER, listener);
    }

    private static void repaint(JComponent target) {
        Container parent = target.getParent();
        if ( parent instanceof JSpinner.NumberEditor ) {
            parent.repaint();
            Container grandParent = parent.getParent();
            if ( grandParent instanceof JSpinner ) {
                grandParent.repaint();
            }
        } else {
            target.repaint();
        }
    }

    /**
     *  Removes the focus listener installed by {@link #repaintOnFocus} so it
     *  does not survive a switch to another look and feel. Safe to call even
     *  if nothing was installed. Pass the same {@code focusSource} that was
     *  used to install it.
     *
     * @param target      the styled component the listener repainted
     * @param focusSource the component the listener was registered on
     */
    static void uninstall(JComponent target, Component focusSource) {
        Object stored = target.getClientProperty(LISTENER);
        if (stored instanceof FocusListener && focusSource != null)
            focusSource.removeFocusListener((FocusListener) stored);
        target.putClientProperty(LISTENER, null);
    }
}
