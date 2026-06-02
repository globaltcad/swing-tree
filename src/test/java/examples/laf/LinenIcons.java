package examples.laf;

import swingtree.UI;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.plaf.UIResource;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 *  Lazy-initialised, package-private {@link Icon} instances that the
 *  {@link LinenCheckBoxUI} and {@link LinenRadioButtonUI} install as the
 *  default check / radio glyph through {@link javax.swing.UIManager}.
 *  <p>
 *  Each icon is a small custom renderer that:
 *  <ul>
 *      <li>scales itself to the current {@linkplain UI#scale() UI scale factor},</li>
 *      <li>derives surface and border colours from the
 *          {@link LinenPalette} based on the button's model state, and</li>
 *      <li>implements {@link UIResource} so Swing replaces it cleanly when the
 *          LAF changes at runtime.</li>
 *  </ul>
 *  <p>
 *  This class is {@code final} and not instantiable.
 */
final class LinenIcons
{
    private LinenIcons() {}

    /** Base side length in <i>developer</i> pixels — actual pixel size is
     *  {@code UI.scale(BASE)} so the glyph stays crisp on HiDPI screens. */
    private static final int BASE = 14;

    /** Default glyph for {@link javax.swing.JCheckBox}. */
    static Icon checkBox() { return CheckBoxIcon.INSTANCE; }

    /** Default glyph for {@link javax.swing.JRadioButton}. */
    static Icon radio()    { return RadioIcon.INSTANCE; }

    /** Tree disclosure chevron pointing right (collapsed node). */
    static Icon treeCollapsed() { return new ChevronIcon(0); }

    /** Tree disclosure chevron pointing down (expanded node). */
    static Icon treeExpanded()  { return new ChevronIcon(90); }

    /** Submenu arrow used by {@link LinenMenuUI} — chevron pointing right. */
    static Icon submenuArrow()  { return new ChevronIcon(0); }

    // ── State helpers ────────────────────────────────────────────────────

    private static Color glyphSurface(boolean enabled, boolean pressed, boolean rollover, boolean selected) {
        if (!enabled)  return LinenPalette.SURFACE_DISABLED;
        if (pressed)   return LinenPalette.SURFACE_PRESSED;
        if (selected)  return LinenPalette.ACCENT_SOFT;
        if (rollover)  return LinenPalette.SURFACE_HOVER;
        return LinenPalette.SURFACE_FIELD;
    }

    private static Color glyphBorder(boolean enabled, boolean focused) {
        if (!enabled) return LinenPalette.BORDER_SOFT;
        if (focused)  return LinenPalette.ACCENT;
        return LinenPalette.BORDER;
    }

    private static Color glyphMark(boolean enabled) {
        return enabled ? LinenPalette.ACCENT : LinenPalette.TEXT_DISABLED;
    }

    // ── Icon classes ─────────────────────────────────────────────────────

    /** The check-box glyph: a rounded square with a thick checkmark. */
    private static final class CheckBoxIcon implements Icon, UIResource {
        static final CheckBoxIcon INSTANCE = new CheckBoxIcon();
        @Override public int getIconWidth()  { return UI.scale(BASE); }
        @Override public int getIconHeight() { return UI.scale(BASE); }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            boolean enabled = c.isEnabled(), focused = c.hasFocus();
            boolean selected = false, rollover = false, pressed = false;
            if (c instanceof AbstractButton) {
                ButtonModel m = ((AbstractButton) c).getModel();
                selected = m.isSelected();
                rollover = m.isRollover();
                pressed  = m.isPressed() && m.isArmed();
            }
            int   w   = getIconWidth();
            int   h   = getIconHeight();
            int   arc = UI.scale(4);
            float pad = UI.scale(0.5f);

            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(glyphSurface(enabled, pressed, rollover, selected));
                g2.fill(new RoundRectangle2D.Float(x, y, w - 1, h - 1, arc, arc));
                g2.setStroke(new BasicStroke(Math.max(1f, UI.scale(1f))));
                g2.setColor(glyphBorder(enabled, focused));
                g2.draw(new RoundRectangle2D.Float(x + pad, y + pad, w - 1 - pad, h - 1 - pad, arc, arc));
                if (selected) {
                    g2.setColor(glyphMark(enabled));
                    g2.setStroke(new BasicStroke(Math.max(1.5f, UI.scale(1.8f)),
                            BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    Path2D.Float check = new Path2D.Float();
                    float lx = x + w * 0.22f, ly = y + h * 0.52f;
                    float mx = x + w * 0.43f, my = y + h * 0.73f;
                    float rx = x + w * 0.78f, ry = y + h * 0.30f;
                    check.moveTo(lx, ly);
                    check.lineTo(mx, my);
                    check.lineTo(rx, ry);
                    g2.draw(check);
                }
            } finally {
                g2.dispose();
            }
        }
    }

    /**
     *  A small chevron glyph used for tree disclosure and submenu arrows.
     *  The {@code rotationDegrees} parameter rotates the basic right-pointing
     *  chevron clockwise; pass {@code 0} for "▶" and {@code 90} for "▼".
     */
    private static final class ChevronIcon implements Icon, UIResource {
        private static final int SIDE = 12;
        private final int rotationDegrees;
        ChevronIcon(int rotationDegrees) { this.rotationDegrees = rotationDegrees; }
        @Override public int getIconWidth()  { return UI.scale(SIDE); }
        @Override public int getIconHeight() { return UI.scale(SIDE); }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getIconWidth(), h = getIconHeight();
                g2.translate(x + w / 2.0, y + h / 2.0);
                g2.rotate(Math.toRadians(rotationDegrees));
                g2.setColor(c != null && c.isEnabled() ? LinenPalette.TEXT_MUTED : LinenPalette.TEXT_DISABLED);
                g2.setStroke(new java.awt.BasicStroke(Math.max(1.2f, UI.scale(1.3f)),
                        java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                float a = UI.scale(2.5f);
                java.awt.geom.Path2D.Float p = new java.awt.geom.Path2D.Float();
                p.moveTo(-a, -a);
                p.lineTo( a,  0);
                p.lineTo(-a,  a);
                g2.draw(p);
            } finally {
                g2.dispose();
            }
        }
    }

    /** The radio glyph: a circle with a smaller filled dot when selected. */
    private static final class RadioIcon implements Icon, UIResource {
        static final RadioIcon INSTANCE = new RadioIcon();
        @Override public int getIconWidth()  { return UI.scale(BASE); }
        @Override public int getIconHeight() { return UI.scale(BASE); }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            boolean enabled = c.isEnabled(), focused = c.hasFocus();
            boolean selected = false, rollover = false, pressed = false;
            if (c instanceof AbstractButton) {
                ButtonModel m = ((AbstractButton) c).getModel();
                selected = m.isSelected();
                rollover = m.isRollover();
                pressed  = m.isPressed() && m.isArmed();
            }
            int   w   = getIconWidth();
            int   h   = getIconHeight();
            float pad = UI.scale(0.5f);

            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(glyphSurface(enabled, pressed, rollover, selected));
                g2.fill(new Ellipse2D.Float(x, y, w - 1, h - 1));
                g2.setStroke(new BasicStroke(Math.max(1f, UI.scale(1f))));
                g2.setColor(glyphBorder(enabled, focused));
                g2.draw(new Ellipse2D.Float(x + pad, y + pad, w - 1 - pad, h - 1 - pad));
                if (selected) {
                    g2.setColor(glyphMark(enabled));
                    float dotPad = UI.scale(4f);
                    g2.fill(new Ellipse2D.Float(x + dotPad, y + dotPad,
                                                w - 1 - 2 * dotPad, h - 1 - 2 * dotPad));
                }
            } finally {
                g2.dispose();
            }
        }
    }
}