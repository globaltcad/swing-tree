package examples.laf;

import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 *  The {@link JTabbedPane} UI delegate of the {@link LinenLookAndFeel}.
 *
 *  <h2>Visual behaviour</h2>
 *  <ul>
 *      <li><b>Inactive tabs</b> — flat, transparent rectangles labelled in
 *          {@link LinenPalette#TEXT_MUTED}.</li>
 *      <li><b>Hovered tab</b> — a soft {@link LinenPalette#SURFACE_HOVER}
 *          wash appears underneath.</li>
 *      <li><b>Selected tab</b> — sits on a {@link LinenPalette#SURFACE_FIELD}
 *          rounded pill with a 2&nbsp;dev-px {@link LinenPalette#ACCENT}
 *          stripe on the edge closest to the content area.</li>
 *      <li><b>Content area</b> — separated from the tab strip by a hairline
 *          in {@link LinenPalette#BORDER_SOFT}; no heavy box border.</li>
 *  </ul>
 *
 *  <p>All numeric inputs are in <i>developer</i> pixels and scaled up by
 *  {@link UI#scale(int)} so the tabs stay crisp on HiDPI displays.
 *
 *  <p>The class is {@code final}; customise via stylesheet or inline
 *  {@code .withStyle(..)}.
 */
public final class LinenTabbedPaneUI
        extends    BasicTabbedPaneUI
        implements SwingTreeStyledComponentUI<JTabbedPane>
{
    /** Called by Swing reflectively to obtain the UI delegate. */
    public static ComponentUI createUI(JComponent c) { return new LinenTabbedPaneUI(); }

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

    // ── Insets / spacing ─────────────────────────────────────────────────

    @Override
    protected Insets getTabInsets(int tabPlacement, int tabIndex) {
        return new Insets(UI.scale(6), UI.scale(14), UI.scale(6), UI.scale(14));
    }

    @Override
    protected Insets getContentBorderInsets(int tabPlacement) {
        // Hairline below/above/left/right of the content depending on placement.
        int n = UI.scale(1);
        switch (tabPlacement) {
            case SwingConstants.LEFT:   return new Insets(0, n, 0, 0);
            case SwingConstants.RIGHT:  return new Insets(0, 0, 0, n);
            case SwingConstants.BOTTOM: return new Insets(0, 0, n, 0);
            case SwingConstants.TOP:
            default:                    return new Insets(n, 0, 0, 0);
        }
    }

    @Override
    protected Insets getTabAreaInsets(int tabPlacement) {
        int g = UI.scale(4);
        return new Insets(g, g, 0, g);
    }

    @Override
    protected int calculateTabAreaHeight(int tabPlacement, int horizRunCount, int maxTabHeight) {
        return super.calculateTabAreaHeight(tabPlacement, horizRunCount, maxTabHeight) + UI.scale(2);
    }

    // ── Tab painting ─────────────────────────────────────────────────────

    @Override
    protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                                      int x, int y, int w, int h, boolean isSelected) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int arc = UI.scale(8);
            Color fill;
            if (isSelected)
                fill = LinenPalette.SURFACE_FIELD;
            else if (getRolloverTab() == tabIndex && tabPane.isEnabledAt(tabIndex))
                fill = LinenPalette.SURFACE_HOVER;
            else
                fill = null;
            if (fill != null) {
                g2.setColor(fill);
                g2.fill(new RoundRectangle2D.Float(x, y, w, h, arc, arc));
            }
        } finally {
            g2.dispose();
        }
    }

    @Override
    protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
                                  int x, int y, int w, int h, boolean isSelected) {
        if (!isSelected)
            return;
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int stripe = Math.max(2, UI.scale(2));
            int arc    = UI.scale(2);
            g2.setColor(tabPane.isEnabledAt(tabIndex)
                    ? LinenPalette.ACCENT
                    : LinenPalette.TEXT_DISABLED);
            // The stripe sits on the edge nearest the content area, so it
            // reads as a continuous accent between tab and panel.
            switch (tabPlacement) {
                case SwingConstants.BOTTOM:
                    g2.fill(new RoundRectangle2D.Float(x, y, w, stripe, arc, arc));
                    break;
                case SwingConstants.LEFT:
                    g2.fill(new RoundRectangle2D.Float(x + w - stripe, y, stripe, h, arc, arc));
                    break;
                case SwingConstants.RIGHT:
                    g2.fill(new RoundRectangle2D.Float(x, y, stripe, h, arc, arc));
                    break;
                case SwingConstants.TOP:
                default:
                    g2.fill(new RoundRectangle2D.Float(x, y + h - stripe, w, stripe, arc, arc));
                    break;
            }
        } finally {
            g2.dispose();
        }
    }

    @Override
    protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics,
                             int tabIndex, String title, Rectangle textRect, boolean isSelected) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(font);
            Color colour;
            if (!tabPane.isEnabledAt(tabIndex)) colour = LinenPalette.TEXT_DISABLED;
            else if (isSelected)                 colour = LinenPalette.TEXT;
            else                                 colour = LinenPalette.TEXT_MUTED;
            g2.setColor(colour);
            g2.drawString(title, textRect.x, textRect.y + metrics.getAscent());
        } finally {
            g2.dispose();
        }
    }

    @Override
    protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects,
                                       int tabIndex, Rectangle iconRect, Rectangle textRect,
                                       boolean isSelected) {
        /* The accent stripe already indicates which tab is active. */
    }

    // ── Content border ───────────────────────────────────────────────────

    @Override
    protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
        // A single hairline separating the tab strip from the content area.
        // We deliberately skip the four-sided box that BasicTabbedPaneUI draws
        // by default — it clashes with the rounded selected-tab pill.
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            int n = Math.max(1, UI.scale(1));
            g2.setColor(LinenPalette.BORDER_SOFT);
            int w = tabPane.getWidth(), h = tabPane.getHeight();
            Insets contentInsets = super.getContentBorderInsets(tabPlacement);
            int tabAreaH = calculateTabAreaHeight(tabPlacement,
                    runCount, maxTabHeight);
            int tabAreaW = calculateTabAreaWidth(tabPlacement,
                    runCount, maxTabWidth);
            switch (tabPlacement) {
                case SwingConstants.BOTTOM:
                    g2.fillRect(0, h - tabAreaH - n, w, n); break;
                case SwingConstants.LEFT:
                    g2.fillRect(tabAreaW, 0, n, h); break;
                case SwingConstants.RIGHT:
                    g2.fillRect(w - tabAreaW - n, 0, n, h); break;
                case SwingConstants.TOP:
                default:
                    g2.fillRect(0, tabAreaH, w, n); break;
            }
            // Suppress unused-warning for contentInsets (kept for parity with super).
            if (contentInsets == null) return;
        } finally {
            g2.dispose();
        }
    }

    // ── Style ────────────────────────────────────────────────────────────

    @Override
    public ComponentStyleDelegate<JTabbedPane> style(ComponentStyleDelegate<JTabbedPane> it) {
        return it
                .backgroundColor(LinenPalette.BACKGROUND)
                .foregroundColor(LinenPalette.TEXT);
    }
}