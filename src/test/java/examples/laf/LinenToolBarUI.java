package examples.laf;

import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JToolBar;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToolBarUI;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 *  The {@link JToolBar} UI delegate of the {@link LinenLookAndFeel}.
 *  <p>
 *  Linen tool-bars are a flat strip in {@link LinenPalette#SURFACE} with
 *  a hairline {@link LinenPalette#BORDER_SOFT} divider on the side that
 *  abuts the content beneath. Floating mode (when the user undocks the
 *  bar) preserves the same painting; the floating window's frame is
 *  drawn by the LAF that owns the dialog. The drag-handle is replaced
 *  with a row of small accent dots on the leading edge — a calmer
 *  signal than the basic LAF's chunky knurled bar.
 *  <p>
 *  The class is {@code final}; customise via stylesheet or inline
 *  {@code .withStyle(..)}.
 */
public final class LinenToolBarUI
        extends    BasicToolBarUI
        implements SwingTreeStyledComponentUI<JToolBar>
{
    /** Called by Swing reflectively to obtain the UI delegate. */
    public static ComponentUI createUI(JComponent c) { return new LinenToolBarUI(); }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        ComponentExtension.from(c).gatherApplyAndInstallStyle(true);
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
    protected void paintDragWindow(Graphics g) {
        super.paintDragWindow(g);
    }

    @Override
    public ComponentStyleDelegate<JToolBar> style(ComponentStyleDelegate<JToolBar> it) {
        return it
                .backgroundColor(LinenPalette.SURFACE)
                .foregroundColor(LinenPalette.TEXT)
                .padding(4, 8, 4, 8)
                .borderRadius(6)
                .borderWidth(1)
                .borderColor(LinenPalette.BORDER_SOFT)
                .painter(UI.Layer.CONTENT, g -> paintDragHandle(g, it.component()));
    }

    private static void paintDragHandle(Graphics2D g, JToolBar bar) {
        if (!bar.isFloatable())
            return;
        // Three small accent dots along the leading edge — calmer than the
        // basic LAF's chunky knurl.
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(LinenPalette.BORDER.getRed(),
                                  LinenPalette.BORDER.getGreen(),
                                  LinenPalette.BORDER.getBlue(), 160));
            float r = UI.scale(1.4f);
            int   d = UI.scale(4);  // spacing between dots
            if (bar.getOrientation() == JToolBar.HORIZONTAL) {
                float cx = UI.scale(4);
                float cy0 = bar.getHeight() / 2f - d;
                for (int i = 0; i < 3; i++)
                    g2.fill(new java.awt.geom.Ellipse2D.Float(cx - r, cy0 + i * d - r, 2 * r, 2 * r));
            } else {
                float cy = UI.scale(4);
                float cx0 = bar.getWidth() / 2f - d;
                for (int i = 0; i < 3; i++)
                    g2.fill(new java.awt.geom.Ellipse2D.Float(cx0 + i * d - r, cy - r, 2 * r, 2 * r));
            }
        } finally {
            g2.dispose();
        }
    }
}