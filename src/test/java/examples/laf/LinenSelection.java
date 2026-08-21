package examples.laf;

import javax.swing.event.CaretListener;
import javax.swing.text.JTextComponent;

/**
 *  Bridges a selection change to a repaint of the whole text component.
 *
 *  <p>Swing repaints a selection change frugally: {@code BasicTextUI.damageRange}
 *  turns the changed offsets into one small rectangle and repaints only that.
 *  That is the right thing to do for a look-and-feel that paints a text
 *  component in one pass, and the wrong assumption for one that does not:
 *  {@linkplain swingtree.style.ComponentExtension the SwingTree style engine}
 *  re-gathers a component's style as part of <em>its</em> paint cycle, and the
 *  surface, border and insets it then installs are the coordinate system that
 *  damage rectangle was measured against. A repaint narrower than the component
 *  is therefore not reliably enough to make a new selection appear — it may
 *  land, and it may leave the selection invisible until something unrelated
 *  (a scroll, a hover, a resize) repaints the component in full.
 *
 *  <p>This is the same shape of problem {@link LinenFocus} exists for, and it
 *  takes the same answer: watch the state Swing will not repaint enough for, and
 *  ask for a full repaint when it changes. The listener is deliberately narrow —
 *  it fires only when a selection appears, disappears or changes extent, so
 *  ordinary typing and caret movement keep Swing's cheap damage-rectangle
 *  behaviour.
 *
 *  @see LinenFocus
 */
final class LinenSelection
{
    private LinenSelection() {}

    /** Stores the installed listener on the component, both as a guard against a
     *  second {@code installUI(..)} stacking duplicates and as the handle
     *  {@link #uninstall} needs to remove it again. */
    private static final String LISTENER = "linen.selectionRepaint.listener";

    /**
     *  Ensures {@code target} repaints in full whenever its selection changes.
     *  Pair every call with {@link #uninstall} from the delegate's
     *  {@code uninstallUI(..)} so the listener does not outlive the look and
     *  feel.
     *
     * @param target the text component to watch and repaint (never {@code null})
     */
    static void repaintOnSelectionChange( JTextComponent target ) {
        if ( target.getClientProperty(LISTENER) != null )
            return; // already wired (e.g. a redundant updateUI())

        int[] previous = { target.getCaret() == null ? 0 : target.getCaretPosition(),
                           target.getCaret() == null ? 0 : target.getCaret().getMark() };

        CaretListener listener = event -> {
            boolean had = previous[0] != previous[1];
            boolean has = event.getDot() != event.getMark();
            boolean moved = event.getDot() != previous[0] || event.getMark() != previous[1];
            previous[0] = event.getDot();
            previous[1] = event.getMark();
            // Only a change to the *selection* is worth a full repaint; a bare
            // caret move — which is what typing produces — is left to Swing.
            if ( moved && ( had || has ) )
                target.repaint();
        };
        target.addCaretListener(listener);
        target.putClientProperty(LISTENER, listener);
    }

    /**
     *  Removes the listener installed by {@link #repaintOnSelectionChange} so it
     *  does not survive a switch to another look and feel. Safe to call even if
     *  nothing was installed.
     *
     * @param target the text component the listener repainted
     */
    static void uninstall( JTextComponent target ) {
        Object stored = target.getClientProperty(LISTENER);
        if ( stored instanceof CaretListener )
            target.removeCaretListener((CaretListener) stored);
        target.putClientProperty(LISTENER, null);
    }
}
