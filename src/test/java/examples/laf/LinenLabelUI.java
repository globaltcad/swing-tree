package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicLabelUI;
import java.awt.Graphics;

/**
 *  The {@link JLabel} UI delegate of the {@link LinenLookAndFeel}.
 *  <p>
 *  Labels have no model and very little state — Linen simply paints them in
 *  {@link LinenPalette#TEXT} (or {@link LinenPalette#TEXT_DISABLED} when
 *  disabled) on a transparent background, so that whatever surface is
 *  underneath shows through cleanly.
 *  <p>
 *  Because the engine renders the foreground colour declared in
 *  {@link #style(ComponentStyleDelegate)} <i>before</i> calling
 *  {@code super.paint(..)}, this is enough to skin every label without
 *  fighting Swing's own paint code.
 *  <p>
 *  The class is {@code final}; override per-label with
 *  {@code .withStyle(it -> it.foregroundColor(..))} or globally from a
 *  {@link swingtree.style.StyleSheet}.
 */
public final class LinenLabelUI
        extends    BasicLabelUI
        implements SwingTreeStyledComponentUI<JLabel>
{
    /** Called by Swing reflectively to obtain the UI delegate. */
    public static ComponentUI createUI(JComponent c) { return new LinenLabelUI(); }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        ComponentExtension.from(c).gatherApplyAndInstallStyle(true);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        ComponentExtension.from(c).paintBackground(g, g2 -> {
            LinenPaint.applyAaHints((java.awt.Graphics2D) g2);
            super.paint(g2, c);
        });
    }

    @Override
    public void update(Graphics g, JComponent c) { paint(g, c); }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    @Override
    @SuppressWarnings("deprecation") // component() is the documented hook for LAF state reads
    public ComponentStyleDelegate<JLabel> style(ComponentStyleDelegate<JLabel> it) {
        JLabel l = it.component();
        return it.foregroundColor(l.isEnabled() ? LinenPalette.TEXT : LinenPalette.TEXT_DISABLED);
    }
}