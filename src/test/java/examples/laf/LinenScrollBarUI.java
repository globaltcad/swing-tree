package examples.laf;

import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 *  The {@link JScrollBar} UI delegate of the {@link LinenLookAndFeel}.
 *  <p>
 *  Linen replaces the classic Swing scrollbar — with its arrow buttons
 *  and chunky thumb — with a slim, modern variant:
 *  <ul>
 *      <li>no increment / decrement buttons,</li>
 *      <li>an almost-invisible track (parent surface shows through), and</li>
 *      <li>a pill-shaped thumb in {@link LinenPalette#BORDER} that
 *          deepens to {@link LinenPalette#ACCENT} on hover or drag.</li>
 *  </ul>
 *  <p>
 *  The class is {@code final}; customise via stylesheet or inline
 *  {@code .withStyle(..)}.
 */
public final class LinenScrollBarUI
        extends    BasicScrollBarUI
        implements SwingTreeStyledComponentUI<JScrollBar>
{
    private static final int THICKNESS = 12;

    /** Called by Swing reflectively to obtain the UI delegate. */
    public static ComponentUI createUI(JComponent c) { return new LinenScrollBarUI(); }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        ComponentExtension.from(c).gatherApplyAndInstallStyle(true);
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        int t = UI.scale(THICKNESS);
        return scrollbar.getOrientation() == JScrollBar.VERTICAL
                ? new Dimension(t, t * 4)
                : new Dimension(t * 4, t);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        ComponentExtension.from(c).paintBackground(g, g2 -> super.paint(g2, c));
    }

    @Override
    public void update(Graphics g, JComponent c) { paint(g, c); }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    @Override
    protected JButton createDecreaseButton(int orientation) { return zeroButton(); }

    @Override
    protected JButton createIncreaseButton(int orientation) { return zeroButton(); }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(LinenPalette.SURFACE_DISABLED);
            int arc = UI.scale(8);
            g2.fill(new RoundRectangle2D.Float(r.x, r.y, r.width, r.height, arc, arc));
        } finally {
            g2.dispose();
        }
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
        if (r.width <= 0 || r.height <= 0)
            return;
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int pad = UI.scale(2);
            int arc = UI.scale(8);
            Color thumb = (isThumbRollover() || isDragging) ? LinenPalette.ACCENT : LinenPalette.BORDER;
            g2.setColor(thumb);
            g2.fill(new RoundRectangle2D.Float(
                    r.x + pad, r.y + pad,
                    r.width - 2 * pad, r.height - 2 * pad,
                    arc, arc));
        } finally {
            g2.dispose();
        }
    }

    @Override
    public ComponentStyleDelegate<JScrollBar> style(ComponentStyleDelegate<JScrollBar> it) {
        return it.backgroundColor(LinenPalette.SURFACE_DISABLED)
                 .foregroundColor(LinenPalette.BORDER);
    }

    private static JButton zeroButton() {
        JButton b = new JButton();
        b.setPreferredSize(new Dimension(0, 0));
        b.setMinimumSize(new Dimension(0, 0));
        b.setMaximumSize(new Dimension(0, 0));
        b.setFocusable(false);
        b.setVisible(false);
        return b;
    }
}