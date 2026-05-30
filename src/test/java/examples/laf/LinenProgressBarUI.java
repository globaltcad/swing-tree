package examples.laf;

import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 *  The {@link JProgressBar} UI delegate of the {@link LinenLookAndFeel}.
 *  <p>
 *  The <i>track</i> is rendered by the SwingTree style engine as a rounded
 *  inset trough. The <i>fill</i> is drawn on top by
 *  {@link #paintDeterminate(Graphics, JComponent)} in
 *  {@link LinenPalette#ACCENT}, with rounded edges that match the track.
 *  Indeterminate progress bars fall through to the basic LAF's animated
 *  bouncing block.
 *  <p>
 *  The class is {@code final}; customise via stylesheet or inline
 *  {@code .withStyle(..)}.
 */
public final class LinenProgressBarUI
        extends    BasicProgressBarUI
        implements SwingTreeStyledComponentUI<JProgressBar>
{
    /** Called by Swing reflectively to obtain the UI delegate. */
    public static ComponentUI createUI(JComponent c) { return new LinenProgressBarUI(); }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        ComponentExtension.from(c).gatherApplyAndInstallStyle(true);
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        Dimension d = super.getPreferredSize(c);
        if (((JProgressBar) c).getOrientation() == SwingConstants.HORIZONTAL)
            d.height = Math.max(d.height, UI.scale(14));
        else
            d.width  = Math.max(d.width,  UI.scale(14));
        return d;
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        ComponentExtension.from(c).paintBackground(g, g2 -> {
            LinenPaint.applyAaHints((Graphics2D) g2);
            super.paint(g2, c);
        });
    }

    @Override
    public void update(Graphics g, JComponent c) { paint(g, c); }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    @Override
    protected void paintDeterminate(Graphics g, JComponent c) {
        JProgressBar bar = (JProgressBar) c;
        paintRoundedFill((Graphics2D) g, bar, fillRatio(bar));
        if (bar.isStringPainted())
            paintStringAtCentre((Graphics2D) g, bar);
    }

    @Override
    public ComponentStyleDelegate<JProgressBar> style(ComponentStyleDelegate<JProgressBar> it) {
        return it
                .borderRadius(7)
                .borderWidth(1)
                .borderColor(LinenPalette.BORDER)
                .backgroundColor(LinenPalette.SURFACE_DISABLED)
                .foregroundColor(LinenPalette.ACCENT);
    }

    // ── Internal helpers ─────────────────────────────────────────────────

    private static double fillRatio(JProgressBar bar) {
        int range = Math.max(1, bar.getMaximum() - bar.getMinimum());
        return Math.max(0, Math.min(1, (bar.getValue() - bar.getMinimum()) / (double) range));
    }

    private static void paintRoundedFill(Graphics2D g, JProgressBar bar, double ratio) {
        if (ratio <= 0) return;
        int w = bar.getWidth(), h = bar.getHeight();
        int arc = UI.scale(6);
        int pad = UI.scale(2);

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bar.isEnabled() ? LinenPalette.ACCENT : LinenPalette.TEXT_DISABLED);
            if (bar.getOrientation() == SwingConstants.HORIZONTAL) {
                int fillW = Math.max(arc, (int) Math.round((w - 2 * pad) * ratio));
                g2.fill(new RoundRectangle2D.Float(pad, pad, fillW, h - 2 * pad, arc, arc));
            } else {
                int fillH = Math.max(arc, (int) Math.round((h - 2 * pad) * ratio));
                g2.fill(new RoundRectangle2D.Float(pad, h - pad - fillH, w - 2 * pad, fillH, arc, arc));
            }
        } finally {
            g2.dispose();
        }
    }

    private static void paintStringAtCentre(Graphics2D g, JProgressBar bar) {
        String s = bar.getString();
        if (s == null || s.isEmpty()) return;
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(bar.getFont());
            Color text = fillRatio(bar) > 0.45
                    ? new Color(0xFA, 0xF6, 0xEC)
                    : LinenPalette.TEXT;
            g2.setColor(text);
            java.awt.FontMetrics fm = g2.getFontMetrics();
            int x = (bar.getWidth()  - fm.stringWidth(s)) / 2;
            int y = (bar.getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(s, x, y);
        } finally {
            g2.dispose();
        }
    }
}