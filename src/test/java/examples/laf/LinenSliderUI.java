package examples.laf;

import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/**
 *  The {@link JSlider} UI delegate of the {@link LinenLookAndFeel}.
 *  <p>
 *  The slider is split visually into three pieces:
 *  <ul>
 *      <li>a thin <b>track groove</b> in {@link LinenPalette#BORDER_SOFT},</li>
 *      <li>a coloured <b>filled track</b> in {@link LinenPalette#ACCENT}
 *          covering the portion before the thumb, and</li>
 *      <li>a circular <b>thumb</b> with a taupe border and an accent-coloured
 *          centre — the same colour vocabulary as the rest of Linen.</li>
 *  </ul>
 *  <p>
 *  The class is {@code final}; customise via stylesheet or inline
 *  {@code .withStyle(..)}.
 */
public final class LinenSliderUI
        extends    BasicSliderUI
        implements SwingTreeStyledComponentUI<JSlider>
{
    private static final int THUMB_DIAMETER = 16;
    private static final int TRACK_THICKNESS = 4;

    public LinenSliderUI() { super(null); }

    /** Called by Swing reflectively to obtain the UI delegate. */
    public static ComponentUI createUI(JComponent c) { return new LinenSliderUI(); }

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
    public Dimension getPreferredHorizontalSize() {
        Dimension d = super.getPreferredHorizontalSize();
        d.height = Math.max(d.height, UI.scale(THUMB_DIAMETER + 4));
        return d;
    }

    @Override
    public Dimension getPreferredVerticalSize() {
        Dimension d = super.getPreferredVerticalSize();
        d.width = Math.max(d.width, UI.scale(THUMB_DIAMETER + 4));
        return d;
    }

    /**
     *  {@link javax.swing.plaf.basic.BasicSliderUI#getPreferredSize(JComponent)}
     *  recomputes the dimension perpendicular to the slider from
     *  {@code trackRect + tickRect + labelRect}, ignoring the floor set in
     *  {@link #getPreferredHorizontalSize()} / {@link #getPreferredVerticalSize()}.
     *  We re-apply the floor here so the slider always has room for the
     *  HiDPI-scaled thumb on both axes.
     */
    @Override
    public Dimension getPreferredSize(JComponent c) {
        Dimension d = super.getPreferredSize(c);
        int floor = UI.scale(THUMB_DIAMETER + 4);
        if (((JSlider) c).getOrientation() == SwingConstants.HORIZONTAL)
            d.height = Math.max(d.height, floor);
        else
            d.width  = Math.max(d.width,  floor);
        return d;
    }

    @Override
    protected Dimension getThumbSize() {
        int s = UI.scale(THUMB_DIAMETER);
        return new Dimension(s, s);
    }

    @Override
    public void paintFocus(Graphics g) { /* the thumb itself indicates focus */ }

    @Override
    public void paintTrack(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int t   = Math.max(2, UI.scale(TRACK_THICKNESS));
            int arc = t;
            if (slider.getOrientation() == SwingConstants.HORIZONTAL) {
                int y = trackRect.y + (trackRect.height - t) / 2;
                g2.setColor(LinenPalette.BORDER_SOFT);
                g2.fill(new RoundRectangle2D.Float(trackRect.x, y, trackRect.width, t, arc, arc));
                int thumbCx = thumbRect.x + thumbRect.width / 2;
                int filled  = Math.max(0, thumbCx - trackRect.x);
                if (filled > 0) {
                    g2.setColor(slider.isEnabled() ? LinenPalette.ACCENT : LinenPalette.TEXT_DISABLED);
                    g2.fill(new RoundRectangle2D.Float(trackRect.x, y, filled, t, arc, arc));
                }
            } else {
                int x = trackRect.x + (trackRect.width - t) / 2;
                g2.setColor(LinenPalette.BORDER_SOFT);
                g2.fill(new RoundRectangle2D.Float(x, trackRect.y, t, trackRect.height, arc, arc));
                int thumbCy = thumbRect.y + thumbRect.height / 2;
                int filled  = Math.max(0, (trackRect.y + trackRect.height) - thumbCy);
                if (filled > 0) {
                    g2.setColor(slider.isEnabled() ? LinenPalette.ACCENT : LinenPalette.TEXT_DISABLED);
                    g2.fill(new RoundRectangle2D.Float(x, thumbCy, t, filled, arc, arc));
                }
            }
        } finally {
            g2.dispose();
        }
    }

    @Override
    public void paintThumb(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Rectangle r        = thumbRect;
            boolean   enabled  = slider.isEnabled();
            boolean   focused  = slider.isFocusOwner();
            float     stroke   = Math.max(1f, UI.scale(1f));
            float     half     = stroke / 2f;
            g2.setColor(enabled ? LinenPalette.SURFACE_FIELD : LinenPalette.SURFACE_DISABLED);
            g2.fill(new Ellipse2D.Float(r.x, r.y, r.width - 1, r.height - 1));
            g2.setStroke(new BasicStroke(stroke));
            g2.setColor(enabled
                    ? (focused ? LinenPalette.ACCENT : LinenPalette.BORDER)
                    : LinenPalette.BORDER_SOFT);
            // Inset by half-stroke so the outline lands inside the bounds at any scale.
            g2.draw(new Ellipse2D.Float(r.x + half, r.y + half,
                                        r.width  - 1 - stroke,
                                        r.height - 1 - stroke));
            if (enabled) {
                float dotPad = UI.scale(5f);
                g2.setColor(LinenPalette.ACCENT);
                g2.fill(new Ellipse2D.Float(r.x + dotPad, r.y + dotPad,
                                            r.width - 1 - 2 * dotPad, r.height - 1 - 2 * dotPad));
            }
        } finally {
            g2.dispose();
        }
    }

    @Override
    public ComponentStyleDelegate<JSlider> style(ComponentStyleDelegate<JSlider> it) {
        return it.backgroundColor(LinenPalette.BACKGROUND)
                 .foregroundColor(LinenPalette.TEXT);
    }
}