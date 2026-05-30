package examples.laf;

import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;

/**
 *  The {@link JSplitPane} UI delegate of the {@link LinenLookAndFeel}.
 *  <p>
 *  The split-pane itself receives only a transparent background; all of
 *  the visible chrome lives on the <i>divider</i> — a slim taupe strip
 *  with three small dots forming a grip handle in the middle. The
 *  divider thickness defaults to 8&nbsp;dev pixels.
 *  <p>
 *  The class is {@code final}; customise via stylesheet or inline
 *  {@code .withStyle(..)}.
 */
public final class LinenSplitPaneUI
        extends    BasicSplitPaneUI
        implements SwingTreeStyledComponentUI<JSplitPane>
{
    private static final int DIVIDER_THICKNESS = 8;

    /** Called by Swing reflectively to obtain the UI delegate. */
    public static ComponentUI createUI(JComponent c) { return new LinenSplitPaneUI(); }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        JSplitPane sp = (JSplitPane) c;
        sp.setBorder(null);                            // SwingTree draws the frame
        sp.setDividerSize(UI.scale(DIVIDER_THICKNESS));
        // BasicSplitPaneUI defaults continuousLayout to FALSE — i.e. dragging
        // the divider draws only a placeholder marker line and the actual
        // split only repositions on mouse-release. Switch it on so the
        // panes resize live while the user drags.
        sp.setContinuousLayout(true);
        ComponentExtension.from(c).gatherApplyAndInstallStyle(true);
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
    public BasicSplitPaneDivider createDefaultDivider() {
        return new LinenDivider(this);
    }

    @Override
    public ComponentStyleDelegate<JSplitPane> style(ComponentStyleDelegate<JSplitPane> it) {
        return it
                .backgroundColor(LinenPalette.BACKGROUND)
                .foregroundColor(LinenPalette.TEXT);
    }

    /** A slim taupe divider with a three-dot grip in the centre. */
    private static final class LinenDivider extends BasicSplitPaneDivider {
        LinenDivider(BasicSplitPaneUI ui) { super(ui); }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                // Track: a faint vertical/horizontal centre line.
                g2.setColor(LinenPalette.BORDER_SOFT);
                if (orientation == JSplitPane.HORIZONTAL_SPLIT) {
                    int x = w / 2;
                    g2.fillRect(x, 0, Math.max(1, UI.scale(1)), h);
                } else {
                    int y = h / 2;
                    g2.fillRect(0, y, w, Math.max(1, UI.scale(1)));
                }
                // Grip: three dots in the middle.
                Color dot = splitPane != null && splitPane.isEnabled()
                        ? LinenPalette.BORDER
                        : LinenPalette.BORDER_SOFT;
                g2.setColor(dot);
                float r = UI.scale(1.5f);
                int   d = UI.scale(5);
                if (orientation == JSplitPane.HORIZONTAL_SPLIT) {
                    float cx = w / 2f;
                    float cy0 = h / 2f - d;
                    for (int i = 0; i < 3; i++)
                        g2.fill(new Ellipse2D.Float(cx - r, cy0 + i * d - r, 2 * r, 2 * r));
                } else {
                    float cy = h / 2f;
                    float cx0 = w / 2f - d;
                    for (int i = 0; i < 3; i++)
                        g2.fill(new Ellipse2D.Float(cx0 + i * d - r, cy - r, 2 * r, 2 * r));
                }
            } finally {
                g2.dispose();
            }
            super.paint(g);
        }
    }
}