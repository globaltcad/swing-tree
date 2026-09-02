package examples.laf;

import examples.laf.SwingTreeLookAndFeel.Palette;
import swingtree.UI;
import swingtree.api.laf.OptimizedShapeRendering;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 *  How a look and feel draws the small pieces of geometry that no style rule can express - a
 *  check mark, a radio dot, a drop-down arrow, a slider handle, a scroll bar thumb, the grip on a
 *  split-pane divider - and how much room they need. The two are one decision: a 16-pixel round
 *  slider handle wants a slider at least 20 pixels tall, so splitting them apart would let a
 *  symbol set be swapped into a layout it does not fit. Every metric is in <b>developer pixels</b>
 *  and is scaled by the caller through {@link swingtree.UI#scale(int)}.
 *  <p>
 *  Every painting method is handed a scratch {@link Graphics2D} it may configure freely, the
 *  {@link Palette} to take its colours from, the geometry in <b>component</b> pixels (already
 *  scaled), and the component state as plain flags. Nothing here reads a Swing component, so a
 *  symbol set is a pure function of its arguments and can be exercised without a GUI.
 *
 *  @see SwingTreeLookAndFeel.SymbolPreset
 */
interface Symbols
{
    /**
     *  Whether this set draws and sizes the chrome itself. Answering {@code false} - the blank set
     *  does - makes every delegate fall through to its inherited {@code Basic*UI}, so nothing else
     *  here is asked. The two exceptions are {@link #paintCheckGlyph} and {@link #paintRadioGlyph}:
     *  the basic look and feel's own versions of those are empty stubs, and a control nobody can
     *  read is not what "no chrome" is supposed to mean.
     */
    boolean drawsItsOwnChrome();

    // ── Metrics, in developer pixels ─────────────────────────────────────

    /** @return the side length of the check-box and radio glyphs. */
    int checkGlyphSize();

    /** @return the side length of a tree disclosure handle and a submenu arrow. */
    int arrowGlyphSize();

    /** @return the side length of a combo box's drop-down button. */
    int comboArrowButtonSize();

    /** @return the width of one of a spinner's two stepper buttons. */
    int spinnerButtonWidth();

    /** @return the height of one of a spinner's two stepper buttons. */
    int spinnerButtonHeight();

    /** @return the diameter of a slider's handle. */
    int sliderThumbDiameter();

    /** @return the thickness of a slider's track groove. */
    int sliderTrackThickness();

    /** @return the thickness of a scroll bar across its short axis. */
    int scrollBarThickness();

    /** @return the thickness of a split pane's divider. */
    int splitDividerThickness();

    /** @return the smallest thickness a progress bar is laid out at across its short axis. */
    int progressBarThickness();

    /** @return the thickness of the hairline a separator draws. */
    int separatorThickness();

    /** @return the height of one table row. */
    int tableRowHeight();

    /** @return the height of one tree row. */
    int treeRowHeight();

    /** @return the padding above and below a tab's label. */
    int tabPaddingVertical();

    /** @return the padding left and right of a tab's label. */
    int tabPaddingHorizontal();

    /** @return the gap between the edge of a tabbed pane and its strip of tabs. */
    int tabAreaGap();

    // ── Glyphs in front of a label ───────────────────────────────────────

    /** Draws the glyph in front of a check box or a check-box menu item. */
    void paintCheckGlyph(
        Graphics2D g, Palette p, int x, int y, int w, int h,
        boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
    );

    /** Draws the glyph in front of a radio button or a radio menu item. */
    void paintRadioGlyph(
        Graphics2D g, Palette p, int x, int y, int w, int h,
        boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
    );

    // ── Arrows ───────────────────────────────────────────────────────────

    /** Draws a tree node's disclosure handle. */
    void paintDisclosure(
        Graphics2D g, Palette p, int x, int y, int w, int h, boolean expanded, boolean enabled
    );

    /** Draws the arrow at the right edge of a menu entry that opens a submenu. */
    void paintSubmenuArrow( Graphics2D g, Palette p, int x, int y, int w, int h, boolean enabled );

    /** Draws the arrow on a combo box's drop-down button. */
    void paintComboArrow(
        Graphics2D g, Palette p, int w, int h, boolean enabled, boolean rollover, boolean pressed
    );

    /** Draws the arrow on one of a spinner's two stepper buttons. */
    void paintSpinnerArrow(
        Graphics2D g, Palette p, int w, int h, boolean up,
        boolean enabled, boolean rollover, boolean pressed
    );

    // ── Chrome ───────────────────────────────────────────────────────────

    /** Draws a slider's groove and the part of it that lies on the filled side of the handle. */
    void paintSliderTrack(
        Graphics2D g, Palette p, Rectangle track, int thumbCentre,
        boolean horizontal, boolean inverted, boolean enabled
    );

    /** Draws a slider's handle. */
    void paintSliderThumb( Graphics2D g, Palette p, Rectangle thumb, boolean enabled, boolean focused );

    /**
     *  Draws a scroll bar's thumb. The groove it slides along is not a symbol: a flat fill with a
     *  radius is something a style rule already says, and the scroll bar's own styled background
     *  covers the whole bar, so a symbol set drawing it again would rasterize the same colour
     *  twice - see {@link SwingTreeScrollBarUI#paintTrack}.
     */
    void paintScrollThumb( Graphics2D g, Palette p, Rectangle thumb, boolean active );

    /** Draws the centre line and grip of a split pane's divider. */
    void paintSplitGrip( Graphics2D g, Palette p, int w, int h, boolean horizontalSplit, boolean enabled );

    /** Draws the handle a floatable tool bar is dragged by. */
    void paintDragHandle( Graphics2D g, Palette p, int w, int h, boolean horizontal );

    /**
     *  Draws the filled part of a determinate progress bar. The trough underneath it is a
     *  style rule, not a symbol.
     */
    void paintProgressFill(
        Graphics2D g, Palette p, int w, int h, double ratio, boolean horizontal, boolean enabled
    );

    /** Draws what lies behind one tab's label. */
    void paintTabSurface(
        Graphics2D g, Palette p, int x, int y, int w, int h, boolean selected, boolean rollover
    );

    /**
     *  Marks the selected tab on the edge nearest its page, so that tab and page read as one.
     *  {@code tabPlacement} is one of the {@link javax.swing.SwingConstants} edges.
     */
    void paintTabAccent(
        Graphics2D g, Palette p, int x, int y, int w, int h, int tabPlacement, boolean enabled
    );

    // IMPLEMENTATIONS:

    /**
     *  The symbol set with no opinions: it answers {@link #drawsItsOwnChrome()} with {@code false} and
     *  is never asked anything else.
     *  <p>
     *  Every call site checks that answer first and falls through to the painting and the sizing its
     *  inherited {@code Basic*UI} would do, and the look and feel installs none of the glyph icons. So
     *  a check box gets Swing's own check box, a scroll bar its own arrows and thumb, a tabbed pane its
     *  own tabs. Paired with {@link SwingTreeLookAndFeel.StylePreset#BLANK} the result is plain Swing
     *  with the SwingTree style engine wired into every component and nothing painted on top - the
     *  starting point for an application that wants to build its whole appearance itself.
     *  <p>
     *  Two glyphs are drawn anyway. A check box and a radio button are drawn through an icon rather
     *  than by their delegate, and {@code BasicIconFactory}'s versions of both are <b>empty stubs</b>,
     *  so falling through would leave a control that cannot be read - which is not what "no styling"
     *  means: a browser showing a page with no stylesheet still draws a real check box. Everything else
     *  here is unreachable, and returns nothing rather than throwing, so a lost guard leaves a control
     *  undecorated instead of taking the window down.
     */
    final class Blank implements Symbols
    {
        static final Symbols INSTANCE = new Blank();

        private Blank() {}

        @Override public boolean drawsItsOwnChrome() { return false; }

        /** The one metric a blank set still has to answer: the two glyphs below are drawn. */
        @Override public int checkGlyphSize()        { return 13; }
        @Override public int arrowGlyphSize()        { return 0; }
        @Override public int comboArrowButtonSize()  { return 0; }
        @Override public int spinnerButtonWidth()    { return 0; }
        @Override public int spinnerButtonHeight()   { return 0; }
        @Override public int sliderThumbDiameter()   { return 0; }
        @Override public int sliderTrackThickness()  { return 0; }
        @Override public int scrollBarThickness()    { return 0; }
        @Override public int splitDividerThickness() { return 0; }
        @Override public int progressBarThickness()  { return 0; }
        @Override public int separatorThickness()    { return 0; }
        @Override public int tableRowHeight()        { return 0; }
        @Override public int treeRowHeight()         { return 0; }
        @Override public int tabPaddingVertical()    { return 0; }
        @Override public int tabPaddingHorizontal()  { return 0; }
        @Override public int tabAreaGap()            { return 0; }

        @Override public void paintCheckGlyph(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
        ) {
            LafUtilities.antialiasShapes(g);
            g.setColor(enabled ? p.surfaceField() : p.surfaceDisabled());
            g.fillRect(x, y, w - 1, h - 1);
            g.setStroke(new BasicStroke(1f));
            g.setColor(enabled ? p.border() : p.borderSoft());
            g.drawRect(x, y, w - 1, h - 1);
            if ( !selected )
                return;
            g.setColor(enabled ? p.text() : p.textDisabled());
            g.setStroke(new BasicStroke(Math.max(1.5f, UI.scale(1.8f)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(LafUtilities.tickShape(x, y, w, h));
        }

        @Override public void paintRadioGlyph(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
        ) {
            LafUtilities.antialiasShapes(g);
            g.setColor(enabled ? p.surfaceField() : p.surfaceDisabled());
            g.fill(new Ellipse2D.Float(x, y, w - 1, h - 1));
            g.setStroke(new BasicStroke(1f));
            g.setColor(enabled ? p.border() : p.borderSoft());
            g.draw(new Ellipse2D.Float(x, y, w - 1, h - 1));
            if ( !selected )
                return;
            float dot = UI.scale(4f);
            g.setColor(enabled ? p.text() : p.textDisabled());
            g.fill(new Ellipse2D.Float(x + dot, y + dot, w - 1 - 2 * dot, h - 1 - 2 * dot));
        }

        @Override public void paintDisclosure(
            Graphics2D g, Palette p, int x, int y, int w, int h, boolean expanded, boolean enabled
        ) {}

        @Override public void paintSubmenuArrow( Graphics2D g, Palette p, int x, int y, int w, int h, boolean enabled ) {}

        @Override public void paintComboArrow(
            Graphics2D g, Palette p, int w, int h, boolean enabled, boolean rollover, boolean pressed
        ) {}

        @Override public void paintSpinnerArrow(
            Graphics2D g, Palette p, int w, int h, boolean up,
            boolean enabled, boolean rollover, boolean pressed
        ) {}

        @Override public void paintSliderTrack(
            Graphics2D g, Palette p, Rectangle track, int thumbCentre,
            boolean horizontal, boolean inverted, boolean enabled
        ) {}

        @Override public void paintSliderThumb( Graphics2D g, Palette p, Rectangle thumb, boolean enabled, boolean focused ) {}

        @Override public void paintScrollThumb( Graphics2D g, Palette p, Rectangle thumb, boolean active ) {}

        @Override public void paintSplitGrip( Graphics2D g, Palette p, int w, int h, boolean horizontalSplit, boolean enabled ) {}

        @Override public void paintDragHandle( Graphics2D g, Palette p, int w, int h, boolean horizontal ) {}

        @Override public void paintProgressFill(
            Graphics2D g, Palette p, int w, int h, double ratio, boolean horizontal, boolean enabled
        ) {}

        @Override public void paintTabSurface(
            Graphics2D g, Palette p, int x, int y, int w, int h, boolean selected, boolean rollover
        ) {}

        @Override public void paintTabAccent(
            Graphics2D g, Palette p, int x, int y, int w, int h, int tabPlacement, boolean enabled
        ) {}
    }

    /**
     *  Symbols drawn with thin strokes, round caps, no bevels and no gradients: the geometry reads
     *  at a glance and stays crisp at any scale factor, because every stroke and radius is derived
     *  from {@link UI#scale(float)} rather than from a fixed bitmap.
     *  <p>
     *  This is the set behind {@link SwingTreeLookAndFeel.SymbolPreset#LINEN}. It is
     *  stateless, so a single instance serves the whole application.
     */
    final class Linen implements Symbols
    {
        static final Symbols INSTANCE = new Linen();

        private Linen() {}

        @Override public boolean drawsItsOwnChrome() { return true; }

        @Override public int checkGlyphSize()        { return 14; }
        @Override public int arrowGlyphSize()        { return 12; }
        @Override public int comboArrowButtonSize()  { return 20; }
        @Override public int spinnerButtonWidth()    { return 18; }
        @Override public int spinnerButtonHeight()   { return 11; }
        @Override public int sliderThumbDiameter()   { return 16; }
        @Override public int sliderTrackThickness()  { return  4; }
        @Override public int scrollBarThickness()    { return 12; }
        @Override public int splitDividerThickness() { return  8; }
        @Override public int progressBarThickness()  { return 14; }
        @Override public int separatorThickness()    { return  1; }
        @Override public int tableRowHeight()        { return 24; }
        @Override public int treeRowHeight()         { return 22; }
        @Override public int tabPaddingVertical()    { return  6; }
        @Override public int tabPaddingHorizontal()  { return 14; }
        @Override public int tabAreaGap()            { return  4; }

        @Override
        public void paintCheckGlyph(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
        ) {
            antialias(g);
            int   arc = UI.scale(4);
            float pad = UI.scale(0.5f);
            g.setColor(glyphSurface(p, enabled, pressed, rollover, selected));
            OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, y, w - 1, h - 1, arc, arc));
            g.setStroke(new BasicStroke(Math.max(1f, UI.scale(1f))));
            g.setColor(glyphBorder(p, enabled, focused));
            g.draw(new RoundRectangle2D.Float(x + pad, y + pad, w - 1 - pad, h - 1 - pad, arc, arc));
            if ( !selected )
                return;
            g.setColor(glyphMark(p, enabled));
            g.setStroke(new BasicStroke(Math.max(1.5f, UI.scale(1.8f)),
                                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D.Float check = new Path2D.Float();
            check.moveTo(x + w * 0.22f, y + h * 0.52f);
            check.lineTo(x + w * 0.43f, y + h * 0.73f);
            check.lineTo(x + w * 0.78f, y + h * 0.30f);
            g.draw(check);
        }

        @Override
        public void paintRadioGlyph(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
        ) {
            antialias(g);
            float pad = UI.scale(0.5f);
            g.setColor(glyphSurface(p, enabled, pressed, rollover, selected));
            g.fill(new Ellipse2D.Float(x, y, w - 1, h - 1));
            g.setStroke(new BasicStroke(Math.max(1f, UI.scale(1f))));
            g.setColor(glyphBorder(p, enabled, focused));
            g.draw(new Ellipse2D.Float(x + pad, y + pad, w - 1 - pad, h - 1 - pad));
            if ( !selected )
                return;
            g.setColor(glyphMark(p, enabled));
            float dotPad = UI.scale(4f);
            g.fill(new Ellipse2D.Float(x + dotPad, y + dotPad,
                                       w - 1 - 2 * dotPad, h - 1 - 2 * dotPad));
        }

        @Override
        public void paintDisclosure(
            Graphics2D g, Palette p, int x, int y, int w, int h, boolean expanded, boolean enabled
        ) {
            chevron(g, x, y, w, h, expanded ? 90 : 0, enabled ? p.textMuted() : p.textDisabled());
        }

        @Override
        public void paintSubmenuArrow( Graphics2D g, Palette p, int x, int y, int w, int h, boolean enabled ) {
            chevron(g, x, y, w, h, 0, enabled ? p.textMuted() : p.textDisabled());
        }

        @Override
        public void paintComboArrow(
            Graphics2D g, Palette p, int w, int h, boolean enabled, boolean rollover, boolean pressed
        ) {
            antialias(g);
            g.setColor(arrowColour(p, enabled, rollover, pressed));
            float cx = w / 2f;
            float cy = h / 2f + UI.scale(1f);
            float a  = UI.scale(4f);
            Path2D.Float arrow = new Path2D.Float();
            arrow.moveTo(cx - a, cy - a * 0.5f);
            arrow.lineTo(cx,     cy + a * 0.5f);
            arrow.lineTo(cx + a, cy - a * 0.5f);
            g.setStroke(new BasicStroke(Math.max(1.4f, UI.scale(1.6f)),
                                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(arrow);
        }

        @Override
        public void paintSpinnerArrow(
            Graphics2D g, Palette p, int w, int h, boolean up,
            boolean enabled, boolean rollover, boolean pressed
        ) {
            antialias(g);
            g.setColor(arrowColour(p, enabled, rollover, pressed));
            float cx = w / 2f, cy = h / 2f;
            float a  = UI.scale(3f);
            Path2D.Float arrow = new Path2D.Float();
            if ( up ) {
                arrow.moveTo(cx - a, cy + a * 0.5f);
                arrow.lineTo(cx,     cy - a * 0.5f);
                arrow.lineTo(cx + a, cy + a * 0.5f);
            } else {
                arrow.moveTo(cx - a, cy - a * 0.5f);
                arrow.lineTo(cx,     cy + a * 0.5f);
                arrow.lineTo(cx + a, cy - a * 0.5f);
            }
            g.setStroke(new BasicStroke(Math.max(1.2f, UI.scale(1.4f)),
                                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(arrow);
        }

        @Override
        public void paintSliderTrack(
            Graphics2D g, Palette p, Rectangle track, int thumbCentre,
            boolean horizontal, boolean inverted, boolean enabled
        ) {
            antialias(g);
            int   t    = Math.max(2, UI.scale(sliderTrackThickness()));
            int   arc  = t;
            Color fill = enabled ? p.accent() : p.textDisabled();
            if ( horizontal ) {
                int y = track.y + (track.height - t) / 2;
                g.setColor(p.borderSoft());
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(track.x, y, track.width, t, arc, arc));
                if ( !inverted ) {
                    int filled = Math.max(0, thumbCentre - track.x);
                    if ( filled > 0 ) {
                        g.setColor(fill);
                        OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(track.x, y, filled, t, arc, arc));
                    }
                } else {
                    int filled = Math.max(0, track.x + track.width - thumbCentre);
                    if ( filled > 0 ) {
                        g.setColor(fill);
                        OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(thumbCentre, y, filled, t, arc, arc));
                    }
                }
            } else {
                int x = track.x + (track.width - t) / 2;
                g.setColor(p.borderSoft());
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, track.y, t, track.height, arc, arc));
                // A vertical slider defaults to "max at top", so the un-inverted fill covers the
                // area below the handle, down to the bottom of the track.
                if ( !inverted ) {
                    int filled = Math.max(0, track.y + track.height - thumbCentre);
                    if ( filled > 0 ) {
                        g.setColor(fill);
                        OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, thumbCentre, t, filled, arc, arc));
                    }
                } else {
                    int filled = Math.max(0, thumbCentre - track.y);
                    if ( filled > 0 ) {
                        g.setColor(fill);
                        OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, track.y, t, filled, arc, arc));
                    }
                }
            }
        }

        @Override
        public void paintSliderThumb( Graphics2D g, Palette p, Rectangle r, boolean enabled, boolean focused ) {
            antialias(g);
            float stroke = Math.max(1f, UI.scale(1f));
            float half   = stroke / 2f;
            g.setColor(enabled ? p.surfaceField() : p.surfaceDisabled());
            g.fill(new Ellipse2D.Float(r.x, r.y, r.width - 1, r.height - 1));
            g.setStroke(new BasicStroke(stroke));
            g.setColor(enabled ? ( focused ? p.accent() : p.border() ) : p.borderSoft());
            // Inset by half a stroke so the outline lands inside the bounds at any scale.
            g.draw(new Ellipse2D.Float(r.x + half, r.y + half,
                                       r.width - 1 - stroke, r.height - 1 - stroke));
            if ( !enabled )
                return;
            float dotPad = UI.scale(5f);
            g.setColor(p.accent());
            g.fill(new Ellipse2D.Float(r.x + dotPad, r.y + dotPad,
                                       r.width - 1 - 2 * dotPad, r.height - 1 - 2 * dotPad));
        }

        @Override
        public void paintScrollThumb( Graphics2D g, Palette p, Rectangle r, boolean active ) {
            antialias(g);
            int pad = UI.scale(2);
            int arc = UI.scale(8);
            g.setColor(active ? p.accent() : p.border());
            OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(r.x + pad, r.y + pad,
                                              r.width - 2 * pad, r.height - 2 * pad, arc, arc));
        }

        @Override
        public void paintSplitGrip( Graphics2D g, Palette p, int w, int h, boolean horizontalSplit, boolean enabled ) {
            antialias(g);
            g.setColor(p.borderSoft());
            if ( horizontalSplit )
                g.fillRect(w / 2, 0, Math.max(1, UI.scale(1)), h);
            else
                g.fillRect(0, h / 2, w, Math.max(1, UI.scale(1)));

            g.setColor(enabled ? p.border() : p.borderSoft());
            float r = UI.scale(1.5f);
            int   d = UI.scale(5);
            if ( horizontalSplit ) {
                float cx  = w / 2f;
                float cy0 = h / 2f - d;
                for ( int i = 0; i < 3; i++ )
                    g.fill(new Ellipse2D.Float(cx - r, cy0 + i * d - r, 2 * r, 2 * r));
            } else {
                float cy  = h / 2f;
                float cx0 = w / 2f - d;
                for ( int i = 0; i < 3; i++ )
                    g.fill(new Ellipse2D.Float(cx0 + i * d - r, cy - r, 2 * r, 2 * r));
            }
        }

        @Override
        public void paintDragHandle( Graphics2D g, Palette p, int w, int h, boolean horizontal ) {
            antialias(g);
            Color border = p.border();
            g.setColor(new Color(border.getRed(), border.getGreen(), border.getBlue(), 160));
            float r = UI.scale(1.4f);
            int   d = UI.scale(4);
            if ( horizontal ) {
                float cx  = UI.scale(4);
                float cy0 = h / 2f - d;
                for ( int i = 0; i < 3; i++ )
                    g.fill(new Ellipse2D.Float(cx - r, cy0 + i * d - r, 2 * r, 2 * r));
            } else {
                float cy  = UI.scale(4);
                float cx0 = w / 2f - d;
                for ( int i = 0; i < 3; i++ )
                    g.fill(new Ellipse2D.Float(cx0 + i * d - r, cy - r, 2 * r, 2 * r));
            }
        }

        @Override
        public void paintProgressFill(
            Graphics2D g, Palette p, int w, int h, double ratio, boolean horizontal, boolean enabled
        ) {
            if ( ratio <= 0 )
                return;
            antialias(g);
            int arc = UI.scale(6);
            int pad = UI.scale(2);
            g.setColor(enabled ? p.accent() : p.textDisabled());
            if ( horizontal ) {
                int fillW = Math.max(arc, (int) Math.round((w - 2 * pad) * ratio));
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(pad, pad, fillW, h - 2 * pad, arc, arc));
            } else {
                int fillH = Math.max(arc, (int) Math.round((h - 2 * pad) * ratio));
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(pad, h - pad - fillH, w - 2 * pad, fillH, arc, arc));
            }
        }

        @Override
        public void paintTabSurface(
            Graphics2D g, Palette p, int x, int y, int w, int h, boolean selected, boolean rollover
        ) {
            Color fill = selected ? p.surfaceField() : ( rollover ? p.surfaceHover() : null );
            if ( fill == null )
                return;
            antialias(g);
            g.setColor(fill);
            int arc = UI.scale(8);
            OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, y, w, h, arc, arc));
        }

        @Override
        public void paintTabAccent(
            Graphics2D g, Palette p, int x, int y, int w, int h, int tabPlacement, boolean enabled
        ) {
            antialias(g);
            int stripe = Math.max(2, UI.scale(2));
            int arc    = UI.scale(2);
            g.setColor(enabled ? p.accent() : p.textDisabled());
            switch ( tabPlacement ) {
                case SwingConstants.BOTTOM:
                    OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, y, w, stripe, arc, arc));
                    break;
                case SwingConstants.LEFT:
                    OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x + w - stripe, y, stripe, h, arc, arc));
                    break;
                case SwingConstants.RIGHT:
                    OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, y, stripe, h, arc, arc));
                    break;
                case SwingConstants.TOP:
                default:
                    OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, y + h - stripe, w, stripe, arc, arc));
                    break;
            }
        }

        // ── Internals ────────────────────────────────────────────────────────

        private static void antialias( Graphics2D g ) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }

        /** The chevron shared by tree handles and submenu arrows: symmetric, centred, rotated
         *  clockwise by {@code rotationDegrees} from its resting right-pointing shape. */
        private static void chevron( Graphics2D g, int x, int y, int w, int h, double rotationDegrees, Color colour ) {
            antialias(g);
            g.translate(x + w / 2.0, y + h / 2.0);
            g.rotate(Math.toRadians(rotationDegrees));
            g.setColor(colour);
            g.setStroke(new BasicStroke(Math.max(1.2f, UI.scale(1.3f)),
                                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float a = UI.scale(2.5f);
            Path2D.Float shape = new Path2D.Float();
            shape.moveTo(-a, -a);
            shape.lineTo( a,  0);
            shape.lineTo(-a,  a);
            g.draw(shape);
        }

        private static Color arrowColour( Palette p, boolean enabled, boolean rollover, boolean pressed ) {
            if ( !enabled )
                return p.textDisabled();
            return ( rollover || pressed ) ? p.accent() : p.textMuted();
        }

        private static Color glyphSurface( Palette p, boolean enabled, boolean pressed, boolean rollover, boolean selected ) {
            if ( !enabled ) return p.surfaceDisabled();
            if ( pressed )  return p.surfacePressed();
            if ( selected ) return p.accentSoft();
            if ( rollover ) return p.surfaceHover();
            return p.surfaceField();
        }

        private static Color glyphBorder( Palette p, boolean enabled, boolean focused ) {
            if ( !enabled ) return p.borderSoft();
            if ( focused )  return p.accent();
            return p.border();
        }

        private static Color glyphMark( Palette p, boolean enabled ) {
            return enabled ? p.accent() : p.textDisabled();
        }
    }

    /**
     *  Symbols for {@link SwingTreeLookAndFeel.StylePreset#SOFT_UI}: every glyph is the surface colour,
     *  and is told apart from the panel behind it only by its rim, which runs from a near-white
     *  highlight at the top to a soft shadow at the bottom. Selecting something turns that rim around,
     *  so a ticked box reads as pressed into the clay rather than standing on it.
     *  <p>
     *  Arrows are embossed the same way: drawn once in the highlight colour a pixel down and right,
     *  then again in their own colour on top, which is the oldest trick there is for making a mark
     *  look carved rather than printed.
     */
    final class Soft implements Symbols
    {
        static final Symbols INSTANCE = new Soft();

        private Soft() {}

        /**
         *  How far the light and the shadow move from the surface they fall on, in channel steps.
         *  Fixed steps rather than fractions, for the reason {@link SoftUiPreset} spells out: a
         *  fraction of the way to white lifts a dark palette five times as far as a light one, which
         *  is what turns every thumb and rim into a glowing bar on Midnight.
         */
        private static final int RIM_LIGHT    =  18;
        private static final int RIM_DARK     = -34;
        private static final int GROOVE_LIGHT =  16;
        private static final int GROOVE_DARK  = -26;
        private static final int EMBOSS_LIGHT =  19;
        private static final int EMBOSS_DARK  = -34;

        @Override public boolean drawsItsOwnChrome() { return true; }

        @Override public int checkGlyphSize()        { return 17; }
        @Override public int arrowGlyphSize()        { return 13; }
        @Override public int comboArrowButtonSize()  { return 22; }
        @Override public int spinnerButtonWidth()    { return 20; }
        @Override public int spinnerButtonHeight()   { return 12; }
        @Override public int sliderThumbDiameter()   { return 20; }
        @Override public int sliderTrackThickness()  { return  8; }
        @Override public int scrollBarThickness()    { return 14; }
        @Override public int splitDividerThickness() { return 10; }
        @Override public int progressBarThickness()  { return 16; }
        @Override public int separatorThickness()    { return  1; }
        @Override public int tableRowHeight()        { return 26; }
        @Override public int treeRowHeight()         { return 24; }
        @Override public int tabPaddingVertical()    { return  8; }
        @Override public int tabPaddingHorizontal()  { return 16; }
        @Override public int tabAreaGap()            { return  6; }

        // ── Glyphs ───────────────────────────────────────────────────────────

        @Override
        public void paintCheckGlyph(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
        ) {
            LafUtilities.antialiasShapes(g);
            int arc = UI.scale(6);
            RoundRectangle2D.Float body = new RoundRectangle2D.Float(x, y, w - 1, h - 1, arc, arc);
            g.setColor(surface(p, enabled, rollover));
            g.fill(body);
            strokeRim(g, body, p, y, h, selected || pressed);
            if ( !selected )
                return;
            g.setColor(enabled ? p.accent() : p.textDisabled());
            g.setStroke(new BasicStroke(Math.max(1.6f, UI.scale(2f)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(LafUtilities.tickShape(x, y, w, h));
        }

        @Override
        public void paintRadioGlyph(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
        ) {
            LafUtilities.antialiasShapes(g);
            Ellipse2D.Float body = new Ellipse2D.Float(x, y, w - 1, h - 1);
            g.setColor(surface(p, enabled, rollover));
            g.fill(body);
            strokeRim(g, body, p, y, h, selected || pressed);
            if ( !selected )
                return;
            float dot = UI.scale(5f);
            g.setColor(enabled ? p.accent() : p.textDisabled());
            g.fill(new Ellipse2D.Float(x + dot, y + dot, w - 1 - 2 * dot, h - 1 - 2 * dot));
        }

        // ── Arrows ───────────────────────────────────────────────────────────

        @Override
        public void paintDisclosure(
            Graphics2D g, Palette p, int x, int y, int w, int h, boolean expanded, boolean enabled
        ) {
            embossedArrow(g, p, x + w / 2f, y + h / 2f, UI.scale(3.2f),
                          expanded ? LafUtilities.Direction.DOWN : LafUtilities.Direction.RIGHT, enabled);
        }

        @Override
        public void paintSubmenuArrow( Graphics2D g, Palette p, int x, int y, int w, int h, boolean enabled ) {
            embossedArrow(g, p, x + w / 2f, y + h / 2f, UI.scale(3.2f), LafUtilities.Direction.RIGHT, enabled);
        }

        @Override
        public void paintComboArrow(
            Graphics2D g, Palette p, int w, int h, boolean enabled, boolean rollover, boolean pressed
        ) {
            embossedArrow(g, p, w / 2f, h / 2f, UI.scale(3.6f), LafUtilities.Direction.DOWN, enabled && !pressed);
        }

        @Override
        public void paintSpinnerArrow(
            Graphics2D g, Palette p, int w, int h, boolean up,
            boolean enabled, boolean rollover, boolean pressed
        ) {
            embossedArrow(g, p, w / 2f, h / 2f, UI.scale(2.8f),
                          up ? LafUtilities.Direction.UP : LafUtilities.Direction.DOWN, enabled && !pressed);
        }

        // ── Chrome ───────────────────────────────────────────────────────────

        @Override
        public void paintSliderTrack(
            Graphics2D g, Palette p, Rectangle track, int thumbCentre,
            boolean horizontal, boolean inverted, boolean enabled
        ) {
            LafUtilities.antialiasShapes(g);
            int t   = Math.max(3, UI.scale(sliderTrackThickness()));
            int arc = t;
            Color fill = enabled ? p.accent() : p.textDisabled();
            if ( horizontal ) {
                int y = track.y + (track.height - t) / 2;
                g.setPaint(LafUtilities.verticalGradient(y, t, LafUtilities.shadeBySteps(p.background(), GROOVE_DARK), LafUtilities.shadeBySteps(p.background(), GROOVE_LIGHT)));
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(track.x, y, track.width, t, arc, arc));
                int filled = inverted ? Math.max(0, track.x + track.width - thumbCentre)
                                      : Math.max(0, thumbCentre - track.x);
                if ( filled > 0 ) {
                    g.setColor(fill);
                    OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(inverted ? thumbCentre : track.x, y, filled, t, arc, arc));
                }
            } else {
                int x = track.x + (track.width - t) / 2;
                g.setPaint(LafUtilities.verticalGradient(track.y, track.height,
                                       LafUtilities.shadeBySteps(p.background(), GROOVE_DARK), LafUtilities.shadeBySteps(p.background(), GROOVE_LIGHT)));
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, track.y, t, track.height, arc, arc));
                int filled = inverted ? Math.max(0, thumbCentre - track.y)
                                      : Math.max(0, track.y + track.height - thumbCentre);
                if ( filled > 0 ) {
                    g.setColor(fill);
                    OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, inverted ? track.y : thumbCentre, t, filled, arc, arc));
                }
            }
        }

        @Override
        public void paintSliderThumb( Graphics2D g, Palette p, Rectangle r, boolean enabled, boolean focused ) {
            LafUtilities.antialiasShapes(g);
            Ellipse2D.Float body = new Ellipse2D.Float(r.x, r.y, r.width - 1, r.height - 1);
            g.setColor(enabled ? p.surface() : p.surfaceDisabled());
            g.fill(body);
            strokeRim(g, body, p, r.y, r.height, false);
            if ( !enabled )
                return;
            float dot = UI.scale(6f);
            g.setColor(focused ? p.accent() : LafUtilities.shadeTowards(p.accent(), p.surface(), 0.35));
            g.fill(new Ellipse2D.Float(r.x + dot, r.y + dot, r.width - 1 - 2 * dot, r.height - 1 - 2 * dot));
        }

        @Override
        public void paintScrollThumb( Graphics2D g, Palette p, Rectangle r, boolean active ) {
            LafUtilities.antialiasShapes(g);
            int pad = UI.scale(3);
            int arc = UI.scale(10);
            RoundRectangle2D.Float body = new RoundRectangle2D.Float(
                    r.x + pad, r.y + pad, r.width - 2 * pad, r.height - 2 * pad, arc, arc);
            g.setColor(active ? LafUtilities.shadeTowards(p.surface(), p.accent(), 0.22) : p.surface());
            g.fill(body);
            strokeRim(g, body, p, r.y + pad, r.height - 2 * pad, false);
        }

        @Override
        public void paintSplitGrip( Graphics2D g, Palette p, int w, int h, boolean horizontalSplit, boolean enabled ) {
            LafUtilities.antialiasShapes(g);
            float radius = UI.scale(2f);
            int   step   = UI.scale(6);
            for ( int i = 0; i < 3; i++ ) {
                float cx = horizontalSplit ? w / 2f          : w / 2f - step + i * step;
                float cy = horizontalSplit ? h / 2f - step + i * step : h / 2f;
                embossedDot(g, p, cx, cy, radius, enabled);
            }
        }

        @Override
        public void paintDragHandle( Graphics2D g, Palette p, int w, int h, boolean horizontal ) {
            LafUtilities.antialiasShapes(g);
            float radius = UI.scale(1.8f);
            int   step   = UI.scale(5);
            for ( int i = 0; i < 3; i++ ) {
                float cx = horizontal ? UI.scale(6)                : w / 2f - step + i * step;
                float cy = horizontal ? h / 2f - step + i * step   : UI.scale(6);
                embossedDot(g, p, cx, cy, radius, true);
            }
        }

        @Override
        public void paintProgressFill(
            Graphics2D g, Palette p, int w, int h, double ratio, boolean horizontal, boolean enabled
        ) {
            if ( ratio <= 0 )
                return;
            LafUtilities.antialiasShapes(g);
            int   pad  = UI.scale(3);
            Color base = enabled ? p.accent() : p.textDisabled();
            if ( horizontal ) {
                int inner = h - 2 * pad;
                int fillW = Math.max(inner, (int) Math.round((w - 2 * pad) * ratio));
                g.setPaint(LafUtilities.verticalGradient(pad, inner, LafUtilities.shadeTowardsWhite(base, 0.22), LafUtilities.shadeTowardsBlack(base, 0.10)));
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(pad, pad, fillW, inner, inner, inner));
            } else {
                int inner = w - 2 * pad;
                int fillH = Math.max(inner, (int) Math.round((h - 2 * pad) * ratio));
                g.setPaint(LafUtilities.verticalGradient(h - pad - fillH, fillH, LafUtilities.shadeTowardsWhite(base, 0.22), LafUtilities.shadeTowardsBlack(base, 0.10)));
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(pad, h - pad - fillH, inner, fillH, inner, inner));
            }
        }

        @Override
        public void paintTabSurface(
            Graphics2D g, Palette p, int x, int y, int w, int h, boolean selected, boolean rollover
        ) {
            if ( !selected && !rollover )
                return;
            LafUtilities.antialiasShapes(g);
            int arc = UI.scale(14);
            RoundRectangle2D.Float body = new RoundRectangle2D.Float(x, y, w, h, arc, arc);
            g.setColor(selected ? p.surface() : p.surfaceHover());
            g.fill(body);
            if ( selected )
                strokeRim(g, body, p, y, h, false);
        }

        @Override
        public void paintTabAccent(
            Graphics2D g, Palette p, int x, int y, int w, int h, int tabPlacement, boolean enabled
        ) {
            LafUtilities.antialiasShapes(g);
            int stripe = Math.max(2, UI.scale(3));
            int arc    = stripe;
            g.setColor(enabled ? p.accent() : p.textDisabled());
            switch ( tabPlacement ) {
                case SwingConstants.BOTTOM:
                    OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x + w * 0.25f, y, w * 0.5f, stripe, arc, arc)); break;
                case SwingConstants.LEFT:
                    OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x + w - stripe, y + h * 0.25f, stripe, h * 0.5f, arc, arc)); break;
                case SwingConstants.RIGHT:
                    OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, y + h * 0.25f, stripe, h * 0.5f, arc, arc)); break;
                case SwingConstants.TOP:
                default:
                    OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x + w * 0.25f, y + h - stripe, w * 0.5f, stripe, arc, arc)); break;
            }
        }

        // ── Internals ────────────────────────────────────────────────────────

        private static Color surface( Palette p, boolean enabled, boolean rollover ) {
            if ( !enabled ) return p.surfaceDisabled();
            return rollover ? p.surfaceHover() : p.surface();
        }

        /** The lit rim that is the whole idiom: highlight at the top, shadow at the bottom - or the
         *  other way around, for something that is meant to look pressed in. */
        private static void strokeRim(
            Graphics2D g, Shape body, Palette p, float y, float h, boolean inverted
        ) {
            Color light = LafUtilities.shadeBySteps(p.background(), RIM_LIGHT);
            Color dark  = LafUtilities.shadeBySteps(p.background(), RIM_DARK);
            g.setStroke(new BasicStroke(Math.max(1.4f, UI.scale(1.6f))));
            g.setPaint(LafUtilities.verticalGradient(y, h, inverted ? dark : light, inverted ? light : dark));
            g.draw(body);
        }

        private static void embossedArrow(
            Graphics2D g, Palette p, float cx, float cy, float size, LafUtilities.Direction direction, boolean enabled
        ) {
            LafUtilities.antialiasShapes(g);
            float lift = UI.scale(1f);
            Path2D.Float shape = LafUtilities.arrowShape(cx, cy, size, size * 0.62f, direction);
            g.translate(lift, lift);
            g.setColor(LafUtilities.shadeBySteps(p.background(), EMBOSS_LIGHT));
            g.fill(shape);
            g.translate(-lift, -lift);
            g.setColor(enabled ? p.textMuted() : p.textDisabled());
            g.fill(shape);
        }

        private static void embossedDot( Graphics2D g, Palette p, float cx, float cy, float radius, boolean enabled ) {
            float lift = UI.scale(1f);
            g.setColor(LafUtilities.shadeBySteps(p.background(), EMBOSS_LIGHT));
            g.fill(new Ellipse2D.Float(cx - radius + lift, cy - radius + lift, 2 * radius, 2 * radius));
            g.setColor(enabled ? LafUtilities.shadeBySteps(p.background(), EMBOSS_DARK) : p.borderSoft());
            g.fill(new Ellipse2D.Float(cx - radius, cy - radius, 2 * radius, 2 * radius));
        }
    }

    /**
     *  Symbols for {@link SwingTreeLookAndFeel.StylePreset#FRUTIGER_AERO}: everything is a piece of wet
     *  glass. A glyph is a saturated fill under a highlight that breaks on a hard line just above the
     *  middle, wrapped in a crisp outline a few shades darker than the fill, with a white sheen along
     *  the top edge.
     *  <p>
     *  Arrows are solid rather than stroked, and carry a pale copy of themselves one pixel below - the
     *  drop shadow every toolbar icon of the period had.
     */
    final class Glossy implements Symbols
    {
        static final Symbols INSTANCE = new Glossy();

        private Glossy() {}

        @Override public boolean drawsItsOwnChrome() { return true; }

        @Override public int checkGlyphSize()        { return 15; }
        @Override public int arrowGlyphSize()        { return 12; }
        @Override public int comboArrowButtonSize()  { return 20; }
        @Override public int spinnerButtonWidth()    { return 18; }
        @Override public int spinnerButtonHeight()   { return 11; }
        @Override public int sliderThumbDiameter()   { return 18; }
        @Override public int sliderTrackThickness()  { return  7; }
        @Override public int scrollBarThickness()    { return 14; }
        @Override public int splitDividerThickness() { return  9; }
        @Override public int progressBarThickness()  { return 16; }
        @Override public int separatorThickness()    { return  1; }
        @Override public int tableRowHeight()        { return 24; }
        @Override public int treeRowHeight()         { return 22; }
        @Override public int tabPaddingVertical()    { return  6; }
        @Override public int tabPaddingHorizontal()  { return 16; }
        @Override public int tabAreaGap()            { return  4; }

        // ── Glyphs ───────────────────────────────────────────────────────────

        @Override
        public void paintCheckGlyph(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
        ) {
            LafUtilities.antialiasShapes(g);
            int arc = UI.scale(4);
            Color base = glyphFill(p, enabled, rollover, pressed, selected);
            RoundRectangle2D.Float body = new RoundRectangle2D.Float(x, y, w - 1, h - 1, arc, arc);
            g.setPaint(LafUtilities.glossGradient(y, h - 1, base));
            g.fill(body);
            outline(g, body, p, enabled, focused, base);
            sheen(g, x + UI.scale(1.5f), y + UI.scale(1.5f), w - 1 - UI.scale(3f), (h - 1) * 0.38f, arc);
            if ( !selected )
                return;
            g.setColor(enabled ? p.onFilled() : p.textDisabled());
            g.setStroke(new BasicStroke(Math.max(1.5f, UI.scale(1.9f)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(LafUtilities.tickShape(x, y, w, h));
        }

        @Override
        public void paintRadioGlyph(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
        ) {
            LafUtilities.antialiasShapes(g);
            Color base = glyphFill(p, enabled, rollover, pressed, selected);
            Ellipse2D.Float body = new Ellipse2D.Float(x, y, w - 1, h - 1);
            g.setPaint(LafUtilities.glossGradient(y, h - 1, base));
            g.fill(body);
            outline(g, body, p, enabled, focused, base);
            g.setColor(new Color(255, 255, 255, 130));
            g.fill(new Ellipse2D.Float(x + UI.scale(2f), y + UI.scale(1.5f),
                                       (w - 1) - UI.scale(4f), (h - 1) * 0.42f));
            if ( !selected )
                return;
            float dot = UI.scale(4.5f);
            g.setColor(enabled ? p.onFilled() : p.textDisabled());
            g.fill(new Ellipse2D.Float(x + dot, y + dot, w - 1 - 2 * dot, h - 1 - 2 * dot));
        }

        // ── Arrows ───────────────────────────────────────────────────────────

        @Override
        public void paintDisclosure(
            Graphics2D g, Palette p, int x, int y, int w, int h, boolean expanded, boolean enabled
        ) {
            droppedArrow(g, p, x + w / 2f, y + h / 2f, UI.scale(3.2f),
                         expanded ? LafUtilities.Direction.DOWN : LafUtilities.Direction.RIGHT, enabled);
        }

        @Override
        public void paintSubmenuArrow( Graphics2D g, Palette p, int x, int y, int w, int h, boolean enabled ) {
            droppedArrow(g, p, x + w / 2f, y + h / 2f, UI.scale(3.2f), LafUtilities.Direction.RIGHT, enabled);
        }

        @Override
        public void paintComboArrow(
            Graphics2D g, Palette p, int w, int h, boolean enabled, boolean rollover, boolean pressed
        ) {
            droppedArrow(g, p, w / 2f, h / 2f + UI.scale(0.5f), UI.scale(4f), LafUtilities.Direction.DOWN,
                         enabled && !pressed);
        }

        @Override
        public void paintSpinnerArrow(
            Graphics2D g, Palette p, int w, int h, boolean up,
            boolean enabled, boolean rollover, boolean pressed
        ) {
            droppedArrow(g, p, w / 2f, h / 2f, UI.scale(3f),
                         up ? LafUtilities.Direction.UP : LafUtilities.Direction.DOWN, enabled && !pressed);
        }

        // ── Chrome ───────────────────────────────────────────────────────────

        @Override
        public void paintSliderTrack(
            Graphics2D g, Palette p, Rectangle track, int thumbCentre,
            boolean horizontal, boolean inverted, boolean enabled
        ) {
            LafUtilities.antialiasShapes(g);
            int   t     = Math.max(3, UI.scale(sliderTrackThickness()));
            int   arc   = t;
            Color fill  = enabled ? p.primary() : p.textDisabled();
            Color empty = p.surfaceDisabled();
            if ( horizontal ) {
                int y = track.y + (track.height - t) / 2;
                g.setPaint(LafUtilities.verticalGradient(y, t, LafUtilities.shadeTowardsBlack(empty, 0.16), LafUtilities.shadeTowardsWhite(empty, 0.30)));
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(track.x, y, track.width, t, arc, arc));
                g.setColor(p.border());
                g.setStroke(new BasicStroke(Math.max(1f, UI.scale(1f))));
                g.draw(new RoundRectangle2D.Float(track.x, y, track.width - 1, t - 1, arc, arc));
                int filled = inverted ? Math.max(0, track.x + track.width - thumbCentre)
                                      : Math.max(0, thumbCentre - track.x);
                if ( filled > 0 ) {
                    g.setPaint(LafUtilities.glossGradient(y, t, fill));
                    OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(inverted ? thumbCentre : track.x, y, filled, t, arc, arc));
                }
            } else {
                int x = track.x + (track.width - t) / 2;
                g.setColor(LafUtilities.shadeTowardsBlack(empty, 0.08));
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, track.y, t, track.height, arc, arc));
                g.setColor(p.border());
                g.setStroke(new BasicStroke(Math.max(1f, UI.scale(1f))));
                g.draw(new RoundRectangle2D.Float(x, track.y, t - 1, track.height - 1, arc, arc));
                int filled = inverted ? Math.max(0, thumbCentre - track.y)
                                      : Math.max(0, track.y + track.height - thumbCentre);
                if ( filled > 0 ) {
                    int top = inverted ? track.y : thumbCentre;
                    g.setPaint(LafUtilities.glossGradient(top, filled, fill));
                    OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, top, t, filled, arc, arc));
                }
            }
        }

        @Override
        public void paintSliderThumb( Graphics2D g, Palette p, Rectangle r, boolean enabled, boolean focused ) {
            LafUtilities.antialiasShapes(g);
            Color base = enabled ? ( focused ? LafUtilities.shadeTowardsWhite(p.accent(), 0.30) : p.surfaceField() ) : p.surfaceDisabled();
            Ellipse2D.Float body = new Ellipse2D.Float(r.x, r.y, r.width - 1, r.height - 1);
            g.setPaint(LafUtilities.glossGradient(r.y, r.height - 1, base));
            g.fill(body);
            g.setColor(enabled ? ( focused ? p.accent() : p.border() ) : p.borderSoft());
            g.setStroke(new BasicStroke(Math.max(1f, UI.scale(1.2f))));
            g.draw(body);
            g.setColor(new Color(255, 255, 255, 150));
            g.fill(new Ellipse2D.Float(r.x + UI.scale(3f), r.y + UI.scale(2f),
                                       (r.width - 1) - UI.scale(6f), (r.height - 1) * 0.38f));
        }

        @Override
        public void paintScrollThumb( Graphics2D g, Palette p, Rectangle r, boolean active ) {
            LafUtilities.antialiasShapes(g);
            int   pad  = UI.scale(2);
            int   arc  = UI.scale(8);
            Color base = active ? p.accent() : LafUtilities.shadeTowards(p.surface(), p.border(), 0.45);
            RoundRectangle2D.Float body = new RoundRectangle2D.Float(
                    r.x + pad, r.y + pad, r.width - 2 * pad, r.height - 2 * pad, arc, arc);
            g.setPaint(LafUtilities.glossGradient(r.y + pad, r.height - 2 * pad, base));
            g.fill(body);
            g.setColor(LafUtilities.shadeTowardsBlack(base, 0.25));
            g.setStroke(new BasicStroke(Math.max(1f, UI.scale(1f))));
            g.draw(body);
        }

        @Override
        public void paintSplitGrip( Graphics2D g, Palette p, int w, int h, boolean horizontalSplit, boolean enabled ) {
            LafUtilities.antialiasShapes(g);
            float radius = UI.scale(1.8f);
            int   step   = UI.scale(5);
            for ( int i = 0; i < 3; i++ ) {
                float cx = horizontalSplit ? w / 2f                    : w / 2f - step + i * step;
                float cy = horizontalSplit ? h / 2f - step + i * step  : h / 2f;
                glassDot(g, p, cx, cy, radius, enabled);
            }
        }

        @Override
        public void paintDragHandle( Graphics2D g, Palette p, int w, int h, boolean horizontal ) {
            LafUtilities.antialiasShapes(g);
            float radius = UI.scale(1.6f);
            int   step   = UI.scale(4);
            for ( int i = 0; i < 3; i++ ) {
                float cx = horizontal ? UI.scale(5)                  : w / 2f - step + i * step;
                float cy = horizontal ? h / 2f - step + i * step     : UI.scale(5);
                glassDot(g, p, cx, cy, radius, true);
            }
        }

        @Override
        public void paintProgressFill(
            Graphics2D g, Palette p, int w, int h, double ratio, boolean horizontal, boolean enabled
        ) {
            if ( ratio <= 0 )
                return;
            LafUtilities.antialiasShapes(g);
            int   pad  = UI.scale(2);
            Color base = enabled ? p.primary() : p.textDisabled();
            if ( horizontal ) {
                int inner = h - 2 * pad;
                int fillW = Math.max(inner, (int) Math.round((w - 2 * pad) * ratio));
                g.setPaint(LafUtilities.glossGradient(pad, inner, base));
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(pad, pad, fillW, inner, inner, inner));
            } else {
                int inner = w - 2 * pad;
                int fillH = Math.max(inner, (int) Math.round((h - 2 * pad) * ratio));
                g.setPaint(LafUtilities.glossGradient(h - pad - fillH, fillH, base));
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(pad, h - pad - fillH, inner, fillH, inner, inner));
            }
        }

        @Override
        public void paintTabSurface(
            Graphics2D g, Palette p, int x, int y, int w, int h, boolean selected, boolean rollover
        ) {
            if ( !selected && !rollover )
                return;
            LafUtilities.antialiasShapes(g);
            int   arc  = UI.scale(8);
            Color base = selected ? p.surfaceField() : p.surfaceHover();
            RoundRectangle2D.Float body = new RoundRectangle2D.Float(x, y, w, h, arc, arc);
            g.setPaint(LafUtilities.glossGradient(y, h, base));
            g.fill(body);
            if ( !selected )
                return;
            g.setColor(p.border());
            g.setStroke(new BasicStroke(Math.max(1f, UI.scale(1f))));
            g.draw(new RoundRectangle2D.Float(x, y, w - 1, h - 1, arc, arc));
        }

        @Override
        public void paintTabAccent(
            Graphics2D g, Palette p, int x, int y, int w, int h, int tabPlacement, boolean enabled
        ) {
            LafUtilities.antialiasShapes(g);
            int stripe = Math.max(2, UI.scale(3));
            int arc    = UI.scale(2);
            g.setPaint(enabled ? LafUtilities.verticalGradient(y, h, LafUtilities.shadeTowardsWhite(p.accent(), 0.25), p.accent())
                               : p.textDisabled());
            switch ( tabPlacement ) {
                case SwingConstants.BOTTOM:
                    OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, y, w, stripe, arc, arc)); break;
                case SwingConstants.LEFT:
                    OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x + w - stripe, y, stripe, h, arc, arc)); break;
                case SwingConstants.RIGHT:
                    OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, y, stripe, h, arc, arc)); break;
                case SwingConstants.TOP:
                default:
                    OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, y + h - stripe, w, stripe, arc, arc)); break;
            }
        }

        // ── Internals ────────────────────────────────────────────────────────

        private static Color glyphFill( Palette p, boolean enabled, boolean rollover, boolean pressed, boolean selected ) {
            if ( !enabled )  return p.surfaceDisabled();
            if ( selected )  return pressed ? p.primaryPressed() : rollover ? p.primaryHover() : p.primary();
            if ( pressed )   return p.surfacePressed();
            return rollover ? p.surfaceHover() : p.surfaceField();
        }

        private static void outline( Graphics2D g, Shape body, Palette p, boolean enabled, boolean focused, Color base ) {
            g.setColor(!enabled ? p.borderSoft() : focused ? p.accent() : LafUtilities.shadeTowardsBlack(base, 0.30));
            g.setStroke(new BasicStroke(Math.max(1f, UI.scale(1.1f))));
            g.draw(body);
        }

        /** The white sheen along the top edge, which is what makes a fill read as wet. */
        private static void sheen( Graphics2D g, float x, float y, float w, float h, int arc ) {
            if ( w <= 0 || h <= 0 )
                return;
            g.setColor(new Color(255, 255, 255, 120));
            OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, y, w, h, arc, arc));
        }

        private static void droppedArrow(
            Graphics2D g, Palette p, float cx, float cy, float size, LafUtilities.Direction direction, boolean enabled
        ) {
            LafUtilities.antialiasShapes(g);
            float lift = UI.scale(1f);
            g.setColor(new Color(255, 255, 255, 170));
            g.fill(LafUtilities.arrowShape(cx, cy + lift, size, size * 0.6f, direction));
            g.setColor(enabled ? LafUtilities.shadeTowardsBlack(p.accent(), 0.15) : p.textDisabled());
            g.fill(LafUtilities.arrowShape(cx, cy, size, size * 0.6f, direction));
        }

        private static void glassDot( Graphics2D g, Palette p, float cx, float cy, float radius, boolean enabled ) {
            g.setColor(new Color(255, 255, 255, 190));
            g.fill(new Ellipse2D.Float(cx - radius, cy - radius + UI.scale(1f), 2 * radius, 2 * radius));
            g.setColor(enabled ? p.border() : p.borderSoft());
            g.fill(new Ellipse2D.Float(cx - radius, cy - radius, 2 * radius, 2 * radius));
        }
    }

    /**
     *  Symbols for {@link SwingTreeLookAndFeel.StylePreset#MATERIAL}: bold, flat and geometric.
     *  <p>
     *  Nothing is shaded or outlined-and-filled at the same time. A control is either an outline in the
     *  muted text colour, meaning "off", or a solid shape in the accent colour, meaning "on" - the tick
     *  and the dot are then punched out of it in white. Arrows are solid triangles rather than strokes,
     *  and thumbs are plain accent shapes with no rim at all, which is what lets a slider read at a
     *  glance from across a room.
     *  <p>
     *  The metrics are the idiom's larger touch targets: taller rows, a fatter slider handle, and a
     *  scroll bar wide enough to grab.
     */
    final class Material implements Symbols
    {
        static final Symbols INSTANCE = new Material();

        private Material() {}

        @Override public boolean drawsItsOwnChrome() { return true; }

        @Override public int checkGlyphSize()        { return 18; }
        @Override public int arrowGlyphSize()        { return 14; }
        @Override public int comboArrowButtonSize()  { return 22; }
        @Override public int spinnerButtonWidth()    { return 20; }
        @Override public int spinnerButtonHeight()   { return 12; }
        @Override public int sliderThumbDiameter()   { return 16; }
        @Override public int sliderTrackThickness()  { return  4; }
        @Override public int scrollBarThickness()    { return 12; }
        @Override public int splitDividerThickness() { return  8; }
        @Override public int progressBarThickness()  { return  6; }
        @Override public int separatorThickness()    { return  1; }
        @Override public int tableRowHeight()        { return 32; }
        @Override public int treeRowHeight()         { return 28; }
        @Override public int tabPaddingVertical()    { return 10; }
        @Override public int tabPaddingHorizontal()  { return 18; }
        @Override public int tabAreaGap()            { return  0; }

        // ── Glyphs ───────────────────────────────────────────────────────────

        @Override
        public void paintCheckGlyph(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
        ) {
            LafUtilities.antialiasShapes(g);
            int   arc    = UI.scale(3);
            float stroke = Math.max(1.6f, UI.scale(2f));
            Color mark   = enabled ? p.accent() : p.textDisabled();
            halo(g, p, x, y, w, h, enabled, focused, rollover, pressed);
            if ( selected ) {
                g.setColor(mark);
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, y, w - 1, h - 1, arc, arc));
                g.setColor(p.onFilled());
                g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(LafUtilities.tickShape(x, y, w, h));
            } else {
                g.setColor(enabled ? p.textMuted() : p.textDisabled());
                g.setStroke(new BasicStroke(stroke));
                float inset = stroke / 2f;
                g.draw(new RoundRectangle2D.Float(x + inset, y + inset, w - 1 - stroke, h - 1 - stroke, arc, arc));
            }
        }

        @Override
        public void paintRadioGlyph(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
        ) {
            LafUtilities.antialiasShapes(g);
            float stroke = Math.max(1.6f, UI.scale(2f));
            float inset  = stroke / 2f;
            Color mark   = enabled ? p.accent() : p.textDisabled();
            halo(g, p, x, y, w, h, enabled, focused, rollover, pressed);
            g.setStroke(new BasicStroke(stroke));
            g.setColor(selected ? mark : ( enabled ? p.textMuted() : p.textDisabled() ));
            g.draw(new Ellipse2D.Float(x + inset, y + inset, w - 1 - stroke, h - 1 - stroke));
            if ( !selected )
                return;
            float dot = UI.scale(5f);
            g.setColor(mark);
            g.fill(new Ellipse2D.Float(x + dot, y + dot, w - 1 - 2 * dot, h - 1 - 2 * dot));
        }

        // ── Arrows ───────────────────────────────────────────────────────────

        @Override
        public void paintDisclosure(
            Graphics2D g, Palette p, int x, int y, int w, int h, boolean expanded, boolean enabled
        ) {
            solidArrow(g, p, x + w / 2f, y + h / 2f, UI.scale(3.6f),
                       expanded ? LafUtilities.Direction.DOWN : LafUtilities.Direction.RIGHT, enabled);
        }

        @Override
        public void paintSubmenuArrow( Graphics2D g, Palette p, int x, int y, int w, int h, boolean enabled ) {
            solidArrow(g, p, x + w / 2f, y + h / 2f, UI.scale(3.4f), LafUtilities.Direction.RIGHT, enabled);
        }

        @Override
        public void paintComboArrow(
            Graphics2D g, Palette p, int w, int h, boolean enabled, boolean rollover, boolean pressed
        ) {
            solidArrow(g, p, w / 2f, h / 2f, UI.scale(4.2f), LafUtilities.Direction.DOWN, enabled);
        }

        @Override
        public void paintSpinnerArrow(
            Graphics2D g, Palette p, int w, int h, boolean up,
            boolean enabled, boolean rollover, boolean pressed
        ) {
            solidArrow(g, p, w / 2f, h / 2f, UI.scale(3.2f), up ? LafUtilities.Direction.UP : LafUtilities.Direction.DOWN, enabled);
        }

        // ── Chrome ───────────────────────────────────────────────────────────

        @Override
        public void paintSliderTrack(
            Graphics2D g, Palette p, Rectangle track, int thumbCentre,
            boolean horizontal, boolean inverted, boolean enabled
        ) {
            LafUtilities.antialiasShapes(g);
            int   t     = Math.max(2, UI.scale(sliderTrackThickness()));
            int   arc   = t;
            Color fill  = enabled ? p.accent() : p.textDisabled();
            Color empty = enabled ? p.accentSoft() : p.surfaceDisabled();
            if ( horizontal ) {
                int y = track.y + (track.height - t) / 2;
                g.setColor(empty);
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(track.x, y, track.width, t, arc, arc));
                int filled = inverted ? Math.max(0, track.x + track.width - thumbCentre)
                                      : Math.max(0, thumbCentre - track.x);
                if ( filled > 0 ) {
                    g.setColor(fill);
                    OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(inverted ? thumbCentre : track.x, y, filled, t, arc, arc));
                }
            } else {
                int x = track.x + (track.width - t) / 2;
                g.setColor(empty);
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, track.y, t, track.height, arc, arc));
                int filled = inverted ? Math.max(0, thumbCentre - track.y)
                                      : Math.max(0, track.y + track.height - thumbCentre);
                if ( filled > 0 ) {
                    g.setColor(fill);
                    OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, inverted ? track.y : thumbCentre, t, filled, arc, arc));
                }
            }
        }

        @Override
        public void paintSliderThumb( Graphics2D g, Palette p, Rectangle r, boolean enabled, boolean focused ) {
            LafUtilities.antialiasShapes(g);
            if ( enabled && focused ) {
                // The halo a Material handle grows under the pointer, and keeps while it has focus.
                g.setColor(LafUtilities.withOpacity(p.accent(), 46));
                float grow = UI.scale(5f);
                g.fill(new Ellipse2D.Float(r.x - grow, r.y - grow, r.width + 2 * grow, r.height + 2 * grow));
            }
            g.setColor(enabled ? p.accent() : p.textDisabled());
            g.fill(new Ellipse2D.Float(r.x, r.y, r.width - 1, r.height - 1));
        }

        @Override
        public void paintScrollThumb( Graphics2D g, Palette p, Rectangle r, boolean active ) {
            LafUtilities.antialiasShapes(g);
            int pad = UI.scale(3);
            int arc = Math.min(r.width, r.height) - 2 * pad;
            g.setColor(active ? p.accent() : LafUtilities.withOpacity(p.text(), 70));
            OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(r.x + pad, r.y + pad, r.width - 2 * pad, r.height - 2 * pad, arc, arc));
        }

        @Override
        public void paintSplitGrip( Graphics2D g, Palette p, int w, int h, boolean horizontalSplit, boolean enabled ) {
            LafUtilities.antialiasShapes(g);
            g.setColor(p.borderSoft());
            if ( horizontalSplit )
                g.fillRect(w / 2, 0, Math.max(1, UI.scale(1)), h);
            else
                g.fillRect(0, h / 2, w, Math.max(1, UI.scale(1)));

            g.setColor(enabled ? LafUtilities.withOpacity(p.text(), 90) : p.borderSoft());
            float radius = UI.scale(1.5f);
            int   step   = UI.scale(5);
            for ( int i = 0; i < 3; i++ ) {
                float cx = horizontalSplit ? w / 2f                   : w / 2f - step + i * step;
                float cy = horizontalSplit ? h / 2f - step + i * step : h / 2f;
                g.fill(new Ellipse2D.Float(cx - radius, cy - radius, 2 * radius, 2 * radius));
            }
        }

        @Override
        public void paintDragHandle( Graphics2D g, Palette p, int w, int h, boolean horizontal ) {
            LafUtilities.antialiasShapes(g);
            g.setColor(LafUtilities.withOpacity(p.text(), 70));
            float radius = UI.scale(1.4f);
            int   step   = UI.scale(4);
            for ( int i = 0; i < 3; i++ ) {
                float cx = horizontal ? UI.scale(5)                : w / 2f - step + i * step;
                float cy = horizontal ? h / 2f - step + i * step   : UI.scale(5);
                g.fill(new Ellipse2D.Float(cx - radius, cy - radius, 2 * radius, 2 * radius));
            }
        }

        @Override
        public void paintProgressFill(
            Graphics2D g, Palette p, int w, int h, double ratio, boolean horizontal, boolean enabled
        ) {
            if ( ratio <= 0 )
                return;
            LafUtilities.antialiasShapes(g);
            g.setColor(enabled ? p.accent() : p.textDisabled());
            if ( horizontal ) {
                int fillW = Math.max(h, (int) Math.round(w * ratio));
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(0, 0, fillW, h, h, h));
            } else {
                int fillH = Math.max(w, (int) Math.round(h * ratio));
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(0, h - fillH, w, fillH, w, w));
            }
        }

        @Override
        public void paintTabSurface(
            Graphics2D g, Palette p, int x, int y, int w, int h, boolean selected, boolean rollover
        ) {
            // A selected tab is said with the rule underneath it, not with a fill; only the pointer
            // gets a wash, which is the closest a static paint comes to the idiom's ripple.
            if ( !rollover )
                return;
            LafUtilities.antialiasShapes(g);
            g.setColor(LafUtilities.withOpacity(p.accent(), 26));
            g.fillRect(x, y, w, h);
        }

        @Override
        public void paintTabAccent(
            Graphics2D g, Palette p, int x, int y, int w, int h, int tabPlacement, boolean enabled
        ) {
            LafUtilities.antialiasShapes(g);
            int stripe = Math.max(2, UI.scale(3));
            g.setColor(enabled ? p.accent() : p.textDisabled());
            switch ( tabPlacement ) {
                case SwingConstants.BOTTOM: g.fillRect(x, y, w, stripe);                  break;
                case SwingConstants.LEFT:   g.fillRect(x + w - stripe, y, stripe, h);     break;
                case SwingConstants.RIGHT:  g.fillRect(x, y, stripe, h);                  break;
                case SwingConstants.TOP:
                default:                    g.fillRect(x, y + h - stripe, w, stripe);     break;
            }
        }

        // ── Internals ────────────────────────────────────────────────────────

        /** The translucent disc a check box or radio grows under the pointer. */
        private static void halo(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean enabled, boolean focused, boolean rollover, boolean pressed
        ) {
            if ( !enabled || !( focused || rollover || pressed ) )
                return;
            g.setColor(LafUtilities.withOpacity(p.accent(), pressed ? 56 : 30));
            float grow = UI.scale(5f);
            g.fill(new Ellipse2D.Float(x - grow, y - grow, w + 2 * grow, h + 2 * grow));
        }

        private static void solidArrow(
            Graphics2D g, Palette p, float cx, float cy, float size, LafUtilities.Direction direction, boolean enabled
        ) {
            LafUtilities.antialiasShapes(g);
            g.setColor(enabled ? p.textMuted() : p.textDisabled());
            g.fill(LafUtilities.arrowShape(cx, cy, size, size * 0.62f, direction));
        }
    }

    /**
     *  Symbols for {@link SwingTreeLookAndFeel.StylePreset#FLAT}: rectangles, solid triangles and
     *  nothing else.
     *  <p>
     *  There is no radius, no rim, no halo and no shade anywhere. A control that is off is a
     *  two-pixel outline in the border colour; a control that is on is the same shape filled solid
     *  with the accent and its mark punched out in white. The one shape left round is the radio
     *  button, because a radio that is not round has stopped being a radio.
     *
     *  @see SwingTreeLookAndFeel.SymbolPreset#FLAT
     */
    final class Flat implements Symbols
    {
        static final Symbols INSTANCE = new Flat();

        private Flat() {}

        @Override public boolean drawsItsOwnChrome() { return true; }

        @Override public int checkGlyphSize()        { return 16; }
        @Override public int arrowGlyphSize()        { return 12; }
        @Override public int comboArrowButtonSize()  { return 20; }
        @Override public int spinnerButtonWidth()    { return 18; }
        @Override public int spinnerButtonHeight()   { return 11; }
        @Override public int sliderThumbDiameter()   { return 14; }
        @Override public int sliderTrackThickness()  { return  4; }
        @Override public int scrollBarThickness()    { return 12; }
        @Override public int splitDividerThickness() { return  6; }
        @Override public int progressBarThickness()  { return  6; }
        @Override public int separatorThickness()    { return  1; }
        @Override public int tableRowHeight()        { return 26; }
        @Override public int treeRowHeight()         { return 22; }
        @Override public int tabPaddingVertical()    { return  8; }
        @Override public int tabPaddingHorizontal()  { return 16; }
        @Override public int tabAreaGap()            { return  0; }

        // ── Glyphs ───────────────────────────────────────────────────────────

        @Override
        public void paintCheckGlyph(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
        ) {
            LafUtilities.antialiasShapes(g);
            int stroke = Math.max(1, UI.scale(2));
            if ( selected ) {
                g.setColor(mark(p, enabled, rollover));
                g.fillRect(x, y, w, h);
                g.setColor(p.onFilled());
                g.setStroke(new BasicStroke(Math.max(1.6f, UI.scale(2.2f)), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
                g.draw(LafUtilities.tickShape(x, y, w, h));
            } else {
                g.setColor(outline(p, enabled, rollover));
                g.fillRect(x, y, w, stroke);
                g.fillRect(x, y + h - stroke, w, stroke);
                g.fillRect(x, y, stroke, h);
                g.fillRect(x + w - stroke, y, stroke, h);
            }
        }

        @Override
        public void paintRadioGlyph(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
        ) {
            LafUtilities.antialiasShapes(g);
            float stroke = Math.max(1.6f, UI.scale(2f));
            float inset  = stroke / 2f;
            g.setStroke(new BasicStroke(stroke));
            g.setColor(selected ? mark(p, enabled, rollover) : outline(p, enabled, rollover));
            g.draw(new Ellipse2D.Float(x + inset, y + inset, w - 1 - stroke, h - 1 - stroke));
            if ( !selected )
                return;
            float dot = UI.scale(4f);
            g.setColor(mark(p, enabled, rollover));
            g.fill(new Ellipse2D.Float(x + dot, y + dot, w - 1 - 2 * dot, h - 1 - 2 * dot));
        }

        // ── Arrows ───────────────────────────────────────────────────────────

        @Override
        public void paintDisclosure(
            Graphics2D g, Palette p, int x, int y, int w, int h, boolean expanded, boolean enabled
        ) {
            wedge(g, p, x + w / 2f, y + h / 2f, UI.scale(3.4f),
                  expanded ? LafUtilities.Direction.DOWN : LafUtilities.Direction.RIGHT, enabled);
        }

        @Override
        public void paintSubmenuArrow( Graphics2D g, Palette p, int x, int y, int w, int h, boolean enabled ) {
            wedge(g, p, x + w / 2f, y + h / 2f, UI.scale(3.2f), LafUtilities.Direction.RIGHT, enabled);
        }

        @Override
        public void paintComboArrow(
            Graphics2D g, Palette p, int w, int h, boolean enabled, boolean rollover, boolean pressed
        ) {
            wedge(g, p, w / 2f, h / 2f, UI.scale(4f), LafUtilities.Direction.DOWN, enabled);
        }

        @Override
        public void paintSpinnerArrow(
            Graphics2D g, Palette p, int w, int h, boolean up,
            boolean enabled, boolean rollover, boolean pressed
        ) {
            wedge(g, p, w / 2f, h / 2f, UI.scale(3f),
                  up ? LafUtilities.Direction.UP : LafUtilities.Direction.DOWN, enabled);
        }

        // ── Chrome ───────────────────────────────────────────────────────────

        @Override
        public void paintSliderTrack(
            Graphics2D g, Palette p, Rectangle track, int thumbCentre,
            boolean horizontal, boolean inverted, boolean enabled
        ) {
            int   t     = Math.max(2, UI.scale(sliderTrackThickness()));
            Color fill  = enabled ? p.accent() : p.textDisabled();
            Color empty = enabled ? p.accentSoft() : p.surfaceDisabled();
            if ( horizontal ) {
                int y = track.y + (track.height - t) / 2;
                g.setColor(empty);
                g.fillRect(track.x, y, track.width, t);
                int filled = inverted ? Math.max(0, track.x + track.width - thumbCentre)
                                      : Math.max(0, thumbCentre - track.x);
                g.setColor(fill);
                g.fillRect(inverted ? thumbCentre : track.x, y, filled, t);
            } else {
                int x = track.x + (track.width - t) / 2;
                g.setColor(empty);
                g.fillRect(x, track.y, t, track.height);
                int filled = inverted ? Math.max(0, thumbCentre - track.y)
                                      : Math.max(0, track.y + track.height - thumbCentre);
                g.setColor(fill);
                g.fillRect(x, inverted ? track.y : thumbCentre, t, filled);
            }
        }

        /** A bar rather than a knob, because a knob would need a rim to read as one. */
        @Override
        public void paintSliderThumb( Graphics2D g, Palette p, Rectangle r, boolean enabled, boolean focused ) {
            g.setColor(enabled ? ( focused ? p.accent() : p.text() ) : p.textDisabled());
            int narrow = Math.max(2, r.width / 3);
            g.fillRect(r.x + (r.width - narrow) / 2, r.y, narrow, r.height);
        }

        @Override
        public void paintScrollThumb( Graphics2D g, Palette p, Rectangle r, boolean active ) {
            int pad = UI.scale(3);
            g.setColor(active ? p.accent() : LafUtilities.withOpacity(p.text(), 90));
            g.fillRect(r.x + pad, r.y + pad, r.width - 2 * pad, r.height - 2 * pad);
        }

        @Override
        public void paintSplitGrip( Graphics2D g, Palette p, int w, int h, boolean horizontalSplit, boolean enabled ) {
            g.setColor(p.borderSoft());
            if ( horizontalSplit )
                g.fillRect(w / 2, 0, Math.max(1, UI.scale(1)), h);
            else
                g.fillRect(0, h / 2, w, Math.max(1, UI.scale(1)));
            dots(g, p, w, h, horizontalSplit, UI.scale(5));
        }

        @Override
        public void paintDragHandle( Graphics2D g, Palette p, int w, int h, boolean horizontal ) {
            int size = Math.max(2, UI.scale(2));
            g.setColor(LafUtilities.withOpacity(p.text(), 90));
            for ( int i = 0; i < 4; i++ ) {
                int step = i * UI.scale(4);
                if ( horizontal )
                    g.fillRect(UI.scale(4), h / 2 - UI.scale(6) + step, size, size);
                else
                    g.fillRect(w / 2 - UI.scale(6) + step, UI.scale(4), size, size);
            }
        }

        @Override
        public void paintProgressFill(
            Graphics2D g, Palette p, int w, int h, double ratio, boolean horizontal, boolean enabled
        ) {
            if ( ratio <= 0 )
                return;
            g.setColor(enabled ? p.accent() : p.textDisabled());
            if ( horizontal )
                g.fillRect(0, 0, (int) Math.round(w * ratio), h);
            else {
                int fillH = (int) Math.round(h * ratio);
                g.fillRect(0, h - fillH, w, fillH);
            }
        }

        @Override
        public void paintTabSurface(
            Graphics2D g, Palette p, int x, int y, int w, int h, boolean selected, boolean rollover
        ) {
            if ( selected )
                g.setColor(p.surface());
            else if ( rollover )
                g.setColor(p.accentSoft());
            else
                return;
            g.fillRect(x, y, w, h);
        }

        @Override
        public void paintTabAccent(
            Graphics2D g, Palette p, int x, int y, int w, int h, int tabPlacement, boolean enabled
        ) {
            int stripe = Math.max(2, UI.scale(3));
            g.setColor(enabled ? p.accent() : p.textDisabled());
            switch ( tabPlacement ) {
                case SwingConstants.BOTTOM: g.fillRect(x, y, w, stripe);              break;
                case SwingConstants.LEFT:   g.fillRect(x + w - stripe, y, stripe, h); break;
                case SwingConstants.RIGHT:  g.fillRect(x, y, stripe, h);              break;
                case SwingConstants.TOP:
                default:                    g.fillRect(x, y + h - stripe, w, stripe); break;
            }
        }

        // ── Internals ────────────────────────────────────────────────────────

        private static Color mark( Palette p, boolean enabled, boolean rollover ) {
            if ( !enabled )
                return p.textDisabled();
            return rollover ? p.primaryHover() : p.accent();
        }

        private static Color outline( Palette p, boolean enabled, boolean rollover ) {
            if ( !enabled )
                return p.textDisabled();
            return rollover ? p.accent() : p.border();
        }

        private static void wedge(
            Graphics2D g, Palette p, float cx, float cy, float size, LafUtilities.Direction direction, boolean enabled
        ) {
            LafUtilities.antialiasShapes(g);
            g.setColor(enabled ? p.text() : p.textDisabled());
            g.fill(LafUtilities.arrowShape(cx, cy, size, size * 0.55f, direction));
        }

        private static void dots( Graphics2D g, Palette p, int w, int h, boolean vertical, int step ) {
            int size = Math.max(2, UI.scale(2));
            g.setColor(LafUtilities.withOpacity(p.text(), 90));
            for ( int i = 0; i < 3; i++ ) {
                int x = vertical ? w / 2 - size / 2                : w / 2 - step + i * step - size / 2;
                int y = vertical ? h / 2 - step + i * step - size / 2 : h / 2 - size / 2;
                g.fillRect(x, y, size, size);
            }
        }
    }

    /**
     *  Symbols for {@link SwingTreeLookAndFeel.StylePreset#SKEUOMORPHIC}: everything is either
     *  cut into the surface or screwed onto it.
     *  <p>
     *  A mark cut into a surface is drawn twice - once dark on the line itself, once light one pixel
     *  below it, where the far wall of the groove catches the light. A mark standing on the surface is
     *  the same two copies the other way round. That one trick, and a vertical gradient on anything
     *  wide enough to show one, is what every glyph here is made of.
     *
     *  @see SwingTreeLookAndFeel.SymbolPreset#CARVED
     */
    final class Carved implements Symbols
    {
        static final Symbols INSTANCE = new Carved();

        private Carved() {}

        /** How opaque the light that spills over the far lip of a groove is. */
        private static final int LIP = 150;
        /** How opaque the shadow inside a groove is. */
        private static final int GROOVE = 120;

        @Override public boolean drawsItsOwnChrome() { return true; }

        @Override public int checkGlyphSize()        { return 16; }
        @Override public int arrowGlyphSize()        { return 13; }
        @Override public int comboArrowButtonSize()  { return 20; }
        @Override public int spinnerButtonWidth()    { return 18; }
        @Override public int spinnerButtonHeight()   { return 11; }
        @Override public int sliderThumbDiameter()   { return 17; }
        @Override public int sliderTrackThickness()  { return  6; }
        @Override public int scrollBarThickness()    { return 14; }
        @Override public int splitDividerThickness() { return  8; }
        @Override public int progressBarThickness()  { return 12; }
        @Override public int separatorThickness()    { return  1; }
        @Override public int tableRowHeight()        { return 24; }
        @Override public int treeRowHeight()         { return 22; }
        @Override public int tabPaddingVertical()    { return  7; }
        @Override public int tabPaddingHorizontal()  { return 15; }
        @Override public int tabAreaGap()            { return  2; }

        // ── Glyphs ───────────────────────────────────────────────────────────

        @Override
        public void paintCheckGlyph(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
        ) {
            LafUtilities.antialiasShapes(g);
            int   arc  = UI.scale(4);
            Color face = enabled ? ( selected ? p.accentSoft() : p.surfaceField() ) : p.surfaceDisabled();
            RoundRectangle2D.Float box = new RoundRectangle2D.Float(x, y, w - 1, h - 1, arc, arc);

            // The lip of light under the whole recess, drawn first so the recess sits on top of it.
            g.setColor(LafUtilities.withOpacity(Color.WHITE, LIP));
            OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, y + 1, w - 1, h - 1, arc, arc));

            g.setPaint(LafUtilities.verticalGradient(y, h, LafUtilities.shadeBySteps(face, -14), face));
            g.fill(box);
            g.setColor(enabled ? p.border() : p.textDisabled());
            g.setStroke(new BasicStroke(1f));
            g.draw(box);
            if ( !selected )
                return;
            engrave(g, enabled ? p.text() : p.textDisabled(), () -> {
                g.setStroke(new BasicStroke(Math.max(1.6f, UI.scale(2.1f)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(LafUtilities.tickShape(x, y, w, h));
            });
        }

        @Override
        public void paintRadioGlyph(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
        ) {
            LafUtilities.antialiasShapes(g);
            Color face = enabled ? p.surfaceField() : p.surfaceDisabled();
            g.setColor(LafUtilities.withOpacity(Color.WHITE, LIP));
            g.fill(new Ellipse2D.Float(x, y + 1, w - 1, h - 1));
            g.setPaint(LafUtilities.verticalGradient(y, h, LafUtilities.shadeBySteps(face, -14), face));
            g.fill(new Ellipse2D.Float(x, y, w - 1, h - 1));
            g.setColor(enabled ? p.border() : p.textDisabled());
            g.setStroke(new BasicStroke(1f));
            g.draw(new Ellipse2D.Float(x, y, w - 1, h - 1));
            if ( !selected )
                return;
            float dot = UI.scale(4f);
            Color bead = enabled ? p.accent() : p.textDisabled();
            g.setPaint(LafUtilities.glossGradient(y + dot, h - 2 * dot, bead));
            g.fill(new Ellipse2D.Float(x + dot, y + dot, w - 1 - 2 * dot, h - 1 - 2 * dot));
        }

        // ── Arrows ───────────────────────────────────────────────────────────

        @Override
        public void paintDisclosure(
            Graphics2D g, Palette p, int x, int y, int w, int h, boolean expanded, boolean enabled
        ) {
            embossedArrow(g, p, x + w / 2f, y + h / 2f, UI.scale(3.4f),
                          expanded ? LafUtilities.Direction.DOWN : LafUtilities.Direction.RIGHT, enabled);
        }

        @Override
        public void paintSubmenuArrow( Graphics2D g, Palette p, int x, int y, int w, int h, boolean enabled ) {
            embossedArrow(g, p, x + w / 2f, y + h / 2f, UI.scale(3.2f), LafUtilities.Direction.RIGHT, enabled);
        }

        @Override
        public void paintComboArrow(
            Graphics2D g, Palette p, int w, int h, boolean enabled, boolean rollover, boolean pressed
        ) {
            embossedArrow(g, p, w / 2f, h / 2f + ( pressed ? 1 : 0 ), UI.scale(4f),
                          LafUtilities.Direction.DOWN, enabled);
        }

        @Override
        public void paintSpinnerArrow(
            Graphics2D g, Palette p, int w, int h, boolean up,
            boolean enabled, boolean rollover, boolean pressed
        ) {
            embossedArrow(g, p, w / 2f, h / 2f + ( pressed ? 1 : 0 ), UI.scale(3f),
                          up ? LafUtilities.Direction.UP : LafUtilities.Direction.DOWN, enabled);
        }

        // ── Chrome ───────────────────────────────────────────────────────────

        @Override
        public void paintSliderTrack(
            Graphics2D g, Palette p, Rectangle track, int thumbCentre,
            boolean horizontal, boolean inverted, boolean enabled
        ) {
            LafUtilities.antialiasShapes(g);
            int   t    = Math.max(3, UI.scale(sliderTrackThickness()));
            int   arc  = t;
            Color fill = enabled ? p.accent() : p.textDisabled();
            if ( horizontal ) {
                int y = track.y + (track.height - t) / 2;
                groove(g, track.x, y, track.width, t, arc, p);
                int filled = inverted ? Math.max(0, track.x + track.width - thumbCentre)
                                      : Math.max(0, thumbCentre - track.x);
                if ( filled <= 0 )
                    return;
                g.setPaint(LafUtilities.glossGradient(y, t, fill));
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(inverted ? thumbCentre : track.x, y, filled, t, arc, arc));
            } else {
                int x = track.x + (track.width - t) / 2;
                groove(g, x, track.y, t, track.height, arc, p);
                int filled = inverted ? Math.max(0, thumbCentre - track.y)
                                      : Math.max(0, track.y + track.height - thumbCentre);
                if ( filled <= 0 )
                    return;
                g.setColor(fill);
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, inverted ? track.y : thumbCentre, t, filled, arc, arc));
            }
        }

        @Override
        public void paintSliderThumb( Graphics2D g, Palette p, Rectangle r, boolean enabled, boolean focused ) {
            LafUtilities.antialiasShapes(g);
            knob(g, p, r, enabled, focused);
        }

        @Override
        public void paintScrollThumb( Graphics2D g, Palette p, Rectangle r, boolean active ) {
            LafUtilities.antialiasShapes(g);
            int pad = UI.scale(2);
            Rectangle body = new Rectangle(r.x + pad, r.y + pad, r.width - 2 * pad, r.height - 2 * pad);
            int arc = Math.min(body.width, body.height);
            Color base = active ? p.accent() : p.surface();
            g.setColor(LafUtilities.withOpacity(Color.BLACK, 60));
            OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(body.x, body.y + 1, body.width, body.height, arc, arc));
            g.setPaint(LafUtilities.glossGradient(body.y, body.height, base));
            OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(body.x, body.y, body.width, body.height, arc, arc));
            g.setColor(p.border());
            g.setStroke(new BasicStroke(1f));
            g.draw(new RoundRectangle2D.Float(body.x, body.y, body.width - 1, body.height - 1, arc, arc));
        }

        @Override
        public void paintSplitGrip( Graphics2D g, Palette p, int w, int h, boolean horizontalSplit, boolean enabled ) {
            LafUtilities.antialiasShapes(g);
            int step = UI.scale(4);
            for ( int i = 0; i < 3; i++ ) {
                float cx = horizontalSplit ? w / 2f                   : w / 2f - step + i * step;
                float cy = horizontalSplit ? h / 2f - step + i * step : h / 2f;
                engravedDot(g, cx, cy, UI.scale(1.4f));
            }
        }

        @Override
        public void paintDragHandle( Graphics2D g, Palette p, int w, int h, boolean horizontal ) {
            LafUtilities.antialiasShapes(g);
            int step = UI.scale(4);
            for ( int i = 0; i < 3; i++ ) {
                float cx = horizontal ? UI.scale(5)                  : w / 2f - step + i * step;
                float cy = horizontal ? h / 2f - step + i * step     : UI.scale(5);
                engravedDot(g, cx, cy, UI.scale(1.4f));
            }
        }

        @Override
        public void paintProgressFill(
            Graphics2D g, Palette p, int w, int h, double ratio, boolean horizontal, boolean enabled
        ) {
            if ( ratio <= 0 )
                return;
            LafUtilities.antialiasShapes(g);
            Color base = enabled ? p.accent() : p.textDisabled();
            if ( horizontal ) {
                int fillW = Math.max(h, (int) Math.round(w * ratio));
                g.setPaint(LafUtilities.glossGradient(0, h, base));
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(0, 0, fillW, h, h, h));
            } else {
                int fillH = Math.max(w, (int) Math.round(h * ratio));
                g.setColor(base);
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(0, h - fillH, w, fillH, w, w));
            }
        }

        @Override
        public void paintTabSurface(
            Graphics2D g, Palette p, int x, int y, int w, int h, boolean selected, boolean rollover
        ) {
            LafUtilities.antialiasShapes(g);
            if ( !selected && !rollover )
                return;
            Color base = selected ? p.surface() : p.surfaceHover();
            int   arc  = UI.scale(5);
            g.setColor(LafUtilities.withOpacity(Color.BLACK, selected ? 60 : 30));
            OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, y + 1, w, h, arc, arc));
            g.setPaint(LafUtilities.verticalGradient(y, h, LafUtilities.shadeBySteps(base, 16), base));
            OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, y, w, h, arc, arc));
        }

        @Override
        public void paintTabAccent(
            Graphics2D g, Palette p, int x, int y, int w, int h, int tabPlacement, boolean enabled
        ) {
            int stripe = Math.max(2, UI.scale(3));
            g.setColor(enabled ? p.accent() : p.textDisabled());
            switch ( tabPlacement ) {
                case SwingConstants.BOTTOM: g.fillRect(x, y, w, stripe);              break;
                case SwingConstants.LEFT:   g.fillRect(x + w - stripe, y, stripe, h); break;
                case SwingConstants.RIGHT:  g.fillRect(x, y, stripe, h);              break;
                case SwingConstants.TOP:
                default:                    g.fillRect(x, y + h - stripe, w, stripe); break;
            }
        }

        // ── Internals ────────────────────────────────────────────────────────

        /** A hole with a dark wall along the top and the light spilling over the far lip. */
        private static void groove( Graphics2D g, int x, int y, int w, int h, int arc, Palette p ) {
            g.setColor(LafUtilities.withOpacity(Color.WHITE, LIP));
            OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, y + 1, w, h, arc, arc));
            g.setPaint(LafUtilities.verticalGradient(y, h,
                            LafUtilities.withOpacity(Color.BLACK, GROOVE), p.surfaceDisabled()));
            OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, y, w, h, arc, arc));
        }

        /** A milled knob: a gloss down its face, a dark rim, and its own shadow underneath. */
        private static void knob( Graphics2D g, Palette p, Rectangle r, boolean enabled, boolean focused ) {
            Color base = enabled ? p.surface() : p.surfaceDisabled();
            g.setColor(LafUtilities.withOpacity(Color.BLACK, 80));
            g.fill(new Ellipse2D.Float(r.x, r.y + 1.5f, r.width - 1, r.height - 1));
            g.setPaint(LafUtilities.glossGradient(r.y, r.height, base));
            g.fill(new Ellipse2D.Float(r.x, r.y, r.width - 1, r.height - 1));
            g.setStroke(new BasicStroke(1f));
            g.setColor(focused ? p.accent() : p.border());
            g.draw(new Ellipse2D.Float(r.x, r.y, r.width - 1, r.height - 1));
        }

        /** Draws a mark twice: light one pixel below the line, then the line itself. */
        private static void engrave( Graphics2D g, Color ink, Runnable mark ) {
            g.translate(0, 1);
            g.setColor(LafUtilities.withOpacity(Color.WHITE, LIP));
            mark.run();
            g.translate(0, -1);
            g.setColor(ink);
            mark.run();
        }

        private static void engravedDot( Graphics2D g, float cx, float cy, float radius ) {
            g.setColor(LafUtilities.withOpacity(Color.WHITE, LIP));
            g.fill(new Ellipse2D.Float(cx - radius, cy - radius + 1, 2 * radius, 2 * radius));
            g.setColor(LafUtilities.withOpacity(Color.BLACK, GROOVE));
            g.fill(new Ellipse2D.Float(cx - radius, cy - radius, 2 * radius, 2 * radius));
        }

        private static void embossedArrow(
            Graphics2D g, Palette p, float cx, float cy, float size, LafUtilities.Direction direction, boolean enabled
        ) {
            LafUtilities.antialiasShapes(g);
            g.setColor(LafUtilities.withOpacity(Color.WHITE, LIP));
            g.fill(LafUtilities.arrowShape(cx, cy + 1, size, size * 0.6f, direction));
            g.setColor(enabled ? p.text() : p.textDisabled());
            g.fill(LafUtilities.arrowShape(cx, cy, size, size * 0.6f, direction));
        }
    }

    /**
     *  Symbols for {@link SwingTreeLookAndFeel.StylePreset#GLASSMORPHIC}: small pieces of the same
     *  glass everything else is cut from.
     *  <p>
     *  Nothing is drawn in a solid colour. A shape that is off is a wash of white you can see the
     *  ground through; a shape that is on is the accent at about three quarters, with a rim one shade
     *  brighter along its edge. Marks - the tick, the dot, the arrows - are the one thing painted at
     *  full strength, because a mark you cannot read is not a mark.
     *
     *  @see SwingTreeLookAndFeel.SymbolPreset#GLASS
     */
    final class Glass implements Symbols
    {
        static final Symbols INSTANCE = new Glass();

        private Glass() {}

        /** How much white an unlit piece of glass carries, out of 255. */
        private static final int PANE = 44;
        /** How opaque a lit piece is. */
        private static final int LIT  = 190;
        /** How bright the rim along a piece's edge is. */
        private static final int RIM  = 120;

        @Override public boolean drawsItsOwnChrome() { return true; }

        @Override public int checkGlyphSize()        { return 17; }
        @Override public int arrowGlyphSize()        { return 13; }
        @Override public int comboArrowButtonSize()  { return 22; }
        @Override public int spinnerButtonWidth()    { return 20; }
        @Override public int spinnerButtonHeight()   { return 12; }
        @Override public int sliderThumbDiameter()   { return 18; }
        @Override public int sliderTrackThickness()  { return  6; }
        @Override public int scrollBarThickness()    { return 13; }
        @Override public int splitDividerThickness() { return  8; }
        @Override public int progressBarThickness()  { return  8; }
        @Override public int separatorThickness()    { return  1; }
        @Override public int tableRowHeight()        { return 28; }
        @Override public int treeRowHeight()         { return 24; }
        @Override public int tabPaddingVertical()    { return  9; }
        @Override public int tabPaddingHorizontal()  { return 18; }
        @Override public int tabAreaGap()            { return  4; }

        // ── Glyphs ───────────────────────────────────────────────────────────

        @Override
        public void paintCheckGlyph(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
        ) {
            LafUtilities.antialiasShapes(g);
            int   arc = UI.scale(5);
            Color face = face(p, enabled, selected, rollover);
            RoundRectangle2D.Float box = new RoundRectangle2D.Float(x, y, w - 1, h - 1, arc, arc);
            g.setColor(face);
            g.fill(box);
            rim(g, p, box, enabled, focused);
            if ( !selected )
                return;
            g.setColor(enabled ? p.onFilled() : p.textDisabled());
            g.setStroke(new BasicStroke(Math.max(1.6f, UI.scale(2f)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(LafUtilities.tickShape(x, y, w, h));
        }

        @Override
        public void paintRadioGlyph(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
        ) {
            LafUtilities.antialiasShapes(g);
            Ellipse2D.Float disc = new Ellipse2D.Float(x, y, w - 1, h - 1);
            g.setColor(face(p, enabled, selected, rollover));
            g.fill(disc);
            rim(g, p, disc, enabled, focused);
            if ( !selected )
                return;
            float dot = UI.scale(4.5f);
            g.setColor(enabled ? p.onFilled() : p.textDisabled());
            g.fill(new Ellipse2D.Float(x + dot, y + dot, w - 1 - 2 * dot, h - 1 - 2 * dot));
        }

        // ── Arrows ───────────────────────────────────────────────────────────

        @Override
        public void paintDisclosure(
            Graphics2D g, Palette p, int x, int y, int w, int h, boolean expanded, boolean enabled
        ) {
            mark(g, p, x + w / 2f, y + h / 2f, UI.scale(3.4f),
                 expanded ? LafUtilities.Direction.DOWN : LafUtilities.Direction.RIGHT, enabled);
        }

        @Override
        public void paintSubmenuArrow( Graphics2D g, Palette p, int x, int y, int w, int h, boolean enabled ) {
            mark(g, p, x + w / 2f, y + h / 2f, UI.scale(3.2f), LafUtilities.Direction.RIGHT, enabled);
        }

        @Override
        public void paintComboArrow(
            Graphics2D g, Palette p, int w, int h, boolean enabled, boolean rollover, boolean pressed
        ) {
            mark(g, p, w / 2f, h / 2f, UI.scale(4f), LafUtilities.Direction.DOWN, enabled);
        }

        @Override
        public void paintSpinnerArrow(
            Graphics2D g, Palette p, int w, int h, boolean up,
            boolean enabled, boolean rollover, boolean pressed
        ) {
            mark(g, p, w / 2f, h / 2f, UI.scale(3f),
                 up ? LafUtilities.Direction.UP : LafUtilities.Direction.DOWN, enabled);
        }

        // ── Chrome ───────────────────────────────────────────────────────────

        @Override
        public void paintSliderTrack(
            Graphics2D g, Palette p, Rectangle track, int thumbCentre,
            boolean horizontal, boolean inverted, boolean enabled
        ) {
            LafUtilities.antialiasShapes(g);
            int   t     = Math.max(3, UI.scale(sliderTrackThickness()));
            int   arc   = t;
            Color fill  = LafUtilities.withOpacity(enabled ? p.accent() : p.textDisabled(), LIT);
            Color empty = LafUtilities.withOpacity(p.surface(), PANE);
            if ( horizontal ) {
                int y = track.y + (track.height - t) / 2;
                g.setColor(empty);
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(track.x, y, track.width, t, arc, arc));
                int filled = inverted ? Math.max(0, track.x + track.width - thumbCentre)
                                      : Math.max(0, thumbCentre - track.x);
                if ( filled <= 0 )
                    return;
                g.setColor(fill);
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(inverted ? thumbCentre : track.x, y, filled, t, arc, arc));
            } else {
                int x = track.x + (track.width - t) / 2;
                g.setColor(empty);
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, track.y, t, track.height, arc, arc));
                int filled = inverted ? Math.max(0, thumbCentre - track.y)
                                      : Math.max(0, track.y + track.height - thumbCentre);
                if ( filled <= 0 )
                    return;
                g.setColor(fill);
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, inverted ? track.y : thumbCentre, t, filled, arc, arc));
            }
        }

        @Override
        public void paintSliderThumb( Graphics2D g, Palette p, Rectangle r, boolean enabled, boolean focused ) {
            LafUtilities.antialiasShapes(g);
            Ellipse2D.Float bead = new Ellipse2D.Float(r.x, r.y, r.width - 1, r.height - 1);
            g.setColor(LafUtilities.withOpacity(Color.BLACK, 70));
            g.fill(new Ellipse2D.Float(r.x, r.y + 2, r.width - 1, r.height - 1));
            g.setColor(LafUtilities.withOpacity(enabled ? p.surface() : p.surfaceDisabled(), 150));
            g.fill(bead);
            rim(g, p, bead, enabled, focused);
        }

        @Override
        public void paintScrollThumb( Graphics2D g, Palette p, Rectangle r, boolean active ) {
            LafUtilities.antialiasShapes(g);
            int pad = UI.scale(3);
            int arc = Math.min(r.width, r.height) - 2 * pad;
            g.setColor(LafUtilities.withOpacity(active ? p.accent() : p.surface(), active ? LIT : 96));
            OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(r.x + pad, r.y + pad, r.width - 2 * pad, r.height - 2 * pad, arc, arc));
        }

        @Override
        public void paintSplitGrip( Graphics2D g, Palette p, int w, int h, boolean horizontalSplit, boolean enabled ) {
            LafUtilities.antialiasShapes(g);
            g.setColor(LafUtilities.withOpacity(p.border(), 60));
            if ( horizontalSplit )
                g.fillRect(w / 2, 0, Math.max(1, UI.scale(1)), h);
            else
                g.fillRect(0, h / 2, w, Math.max(1, UI.scale(1)));
            beads(g, p, w, h, horizontalSplit, UI.scale(5));
        }

        @Override
        public void paintDragHandle( Graphics2D g, Palette p, int w, int h, boolean horizontal ) {
            LafUtilities.antialiasShapes(g);
            beads(g, p, horizontal ? UI.scale(10) : w, horizontal ? h : UI.scale(10), horizontal, UI.scale(4));
        }

        @Override
        public void paintProgressFill(
            Graphics2D g, Palette p, int w, int h, double ratio, boolean horizontal, boolean enabled
        ) {
            if ( ratio <= 0 )
                return;
            LafUtilities.antialiasShapes(g);
            g.setColor(LafUtilities.withOpacity(enabled ? p.accent() : p.textDisabled(), LIT));
            if ( horizontal ) {
                int fillW = Math.max(h, (int) Math.round(w * ratio));
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(0, 0, fillW, h, h, h));
            } else {
                int fillH = Math.max(w, (int) Math.round(h * ratio));
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(0, h - fillH, w, fillH, w, w));
            }
        }

        @Override
        public void paintTabSurface(
            Graphics2D g, Palette p, int x, int y, int w, int h, boolean selected, boolean rollover
        ) {
            if ( !selected && !rollover )
                return;
            LafUtilities.antialiasShapes(g);
            int arc = UI.scale(10);
            g.setColor(LafUtilities.withOpacity(p.surface(), selected ? 58 : 28));
            OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, y, w, h, arc, arc));
        }

        @Override
        public void paintTabAccent(
            Graphics2D g, Palette p, int x, int y, int w, int h, int tabPlacement, boolean enabled
        ) {
            LafUtilities.antialiasShapes(g);
            int stripe = Math.max(2, UI.scale(3));
            g.setColor(LafUtilities.withOpacity(enabled ? p.accent() : p.textDisabled(), LIT));
            switch ( tabPlacement ) {
                case SwingConstants.BOTTOM: g.fillRect(x, y, w, stripe);              break;
                case SwingConstants.LEFT:   g.fillRect(x + w - stripe, y, stripe, h); break;
                case SwingConstants.RIGHT:  g.fillRect(x, y, stripe, h);              break;
                case SwingConstants.TOP:
                default:                    g.fillRect(x, y + h - stripe, w, stripe); break;
            }
        }

        // ── Internals ────────────────────────────────────────────────────────

        private static Color face( Palette p, boolean enabled, boolean selected, boolean rollover ) {
            if ( !enabled )
                return LafUtilities.withOpacity(p.surfaceDisabled(), 40);
            if ( selected )
                return LafUtilities.withOpacity(p.accent(), LIT);
            return LafUtilities.withOpacity(p.surface(), rollover ? PANE + 30 : PANE);
        }

        /** The hairline of brighter glass along a piece's edge, and the accent when it has focus. */
        private static void rim( Graphics2D g, Palette p, Shape shape, boolean enabled, boolean focused ) {
            g.setStroke(new BasicStroke(focused ? Math.max(1.4f, UI.scale(2f)) : 1f));
            g.setColor(focused ? LafUtilities.withOpacity(p.accent(), 220)
                               : LafUtilities.withOpacity(p.border(), enabled ? RIM : 50));
            g.draw(shape);
        }

        private static void mark(
            Graphics2D g, Palette p, float cx, float cy, float size, LafUtilities.Direction direction, boolean enabled
        ) {
            LafUtilities.antialiasShapes(g);
            g.setColor(enabled ? p.text() : p.textDisabled());
            g.fill(LafUtilities.arrowShape(cx, cy, size, size * 0.62f, direction));
        }

        private static void beads( Graphics2D g, Palette p, int w, int h, boolean vertical, int step ) {
            float radius = UI.scale(1.5f);
            g.setColor(LafUtilities.withOpacity(p.surface(), 130));
            for ( int i = 0; i < 3; i++ ) {
                float cx = vertical ? w / 2f                   : w / 2f - step + i * step;
                float cy = vertical ? h / 2f - step + i * step : h / 2f;
                g.fill(new Ellipse2D.Float(cx - radius, cy - radius, 2 * radius, 2 * radius));
            }
        }
    }

    /**
     *  Symbols for {@link SwingTreeLookAndFeel.StylePreset#NIMBUS}: the same moulded plastic the style
     *  rules paint, cut into the shapes no style rule can express.
     *  <p>
     *  Nothing here has an appearance of its own. A check box is a small rounded square, a radio a
     *  small circle, a slider handle a round knob and a scroll thumb a pill, and every one of them is
     *  the surface colour under {@link NimbusRelief#LIT} inside the same outline a button wears - so
     *  they follow the palette and the theme's state colours without being told about either. The
     *  arrows are solid triangles in the text colour, which is what the original draws and what keeps
     *  them legible at any scale.
     *  <p>
     *  Two things here deliberately do not do what the other symbol sets do. A slider's track is not
     *  filled up to the handle, because the original does not fill it and a filled track would say the
     *  value twice. And a progress bar's fill is the one place a different relief is used -
     *  {@link NimbusRelief#GLOSS} - because that bar is the one wet thing in a dry theme.
     *
     *  @see SwingTreeLookAndFeel.SymbolPreset#NIMBUS
     */
    final class Nimbus implements Symbols
    {
        static final Symbols INSTANCE = new Nimbus();

        private Nimbus() {}

        /** The corner radius of the small rounded squares, in developer pixels. */
        private static final float GLYPH_ARC = 4f;

        @Override public boolean drawsItsOwnChrome() { return true; }

        // The metrics the original lays out with, read out of its own UIDefaults.
        @Override public int checkGlyphSize()        { return 18; }
        @Override public int arrowGlyphSize()        { return 12; }
        @Override public int comboArrowButtonSize()  { return 19; }
        @Override public int spinnerButtonWidth()    { return 18; }
        @Override public int spinnerButtonHeight()   { return 11; }
        @Override public int sliderThumbDiameter()   { return 17; }
        @Override public int sliderTrackThickness()  { return  5; }
        @Override public int scrollBarThickness()    { return 15; }
        @Override public int splitDividerThickness() { return 10; }
        @Override public int progressBarThickness()  { return 19; }
        @Override public int separatorThickness()    { return  1; }
        @Override public int tableRowHeight()        { return 20; }
        @Override public int treeRowHeight()         { return 20; }
        @Override public int tabPaddingVertical()    { return  4; }
        @Override public int tabPaddingHorizontal()  { return 12; }
        @Override public int tabAreaGap()            { return  3; }

        // ── Glyphs in front of a label ───────────────────────────────────────

        @Override
        public void paintCheckGlyph(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
        ) {
            float arc = UI.scale(GLYPH_ARC);
            mould(g, p, new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, w - 1, h - 1, arc, arc),
                  y, h, enabled, selected, pressed, rollover);
            if ( !selected )
                return;
            g.setColor(enabled ? p.text() : p.textDisabled());
            g.setStroke(new BasicStroke(Math.max(1.8f, UI.scale(2.4f)), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            float inset = w * 0.20f;
            g.draw(LafUtilities.tickShape(x + inset, y + inset, w - 2 * inset, h - 2 * inset));
        }

        @Override
        public void paintRadioGlyph(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
        ) {
            mould(g, p, new Ellipse2D.Float(x + 0.5f, y + 0.5f, w - 1, h - 1),
                  y, h, enabled, selected, pressed, rollover);
            if ( !selected )
                return;
            float dot = w * 0.29f;
            g.setColor(enabled ? p.text() : p.textDisabled());
            g.fill(new Ellipse2D.Float(x + dot, y + dot, w - 2 * dot, h - 2 * dot));
        }

        // ── Arrows ───────────────────────────────────────────────────────────

        @Override
        public void paintDisclosure(
            Graphics2D g, Palette p, int x, int y, int w, int h, boolean expanded, boolean enabled
        ) {
            wedge(g, p, x + w / 2f, y + h / 2f, UI.scale(3.6f),
                  expanded ? LafUtilities.Direction.DOWN : LafUtilities.Direction.RIGHT, enabled);
        }

        @Override
        public void paintSubmenuArrow( Graphics2D g, Palette p, int x, int y, int w, int h, boolean enabled ) {
            wedge(g, p, x + w / 2f, y + h / 2f, UI.scale(3.2f), LafUtilities.Direction.RIGHT, enabled);
        }

        @Override
        public void paintComboArrow(
            Graphics2D g, Palette p, int w, int h, boolean enabled, boolean rollover, boolean pressed
        ) {
            stepper(g, p, w, h, enabled, rollover, pressed);
            wedge(g, p, w / 2f, h / 2f, UI.scale(3.6f), LafUtilities.Direction.DOWN, enabled);
        }

        @Override
        public void paintSpinnerArrow(
            Graphics2D g, Palette p, int w, int h, boolean up,
            boolean enabled, boolean rollover, boolean pressed
        ) {
            stepper(g, p, w, h, enabled, rollover, pressed);
            wedge(g, p, w / 2f, h / 2f, UI.scale(2.8f),
                  up ? LafUtilities.Direction.UP : LafUtilities.Direction.DOWN, enabled);
        }

        /**
         *  The small button a drop-down arrow or a stepper arrow stands on. It is drawn here rather
         *  than by a style rule because the rule governs the whole combo box or spinner: a second
         *  styled surface inside the first would draw a box around the arrow instead of beside it.
         *  The divider down its left edge is what separates the button from the value.
         */
        private static void stepper(
            Graphics2D g, Palette p, int w, int h, boolean enabled, boolean rollover, boolean pressed
        ) {
            Color tone = enabled ? NimbusPreset.accentedTone(p, pressed, rollover) : p.surfaceDisabled();
            g.setPaint(NimbusPreset.relief(enabled, true).paint(0, h, tone));
            g.fillRect(0, 0, w, h);
            g.setColor(enabled ? NimbusPreset.accentedEdge(p) : NimbusPreset.surfaceEdge(p, false, false, false));
            g.fillRect(0, 0, 1, h);
        }

        // ── Chrome ───────────────────────────────────────────────────────────

        /**
         *  A groove cut across the slider, the same colour the whole way along. The original leaves it
         *  unfilled: the handle already says where the value is, and a coloured run behind it would
         *  make a slider look like a progress bar.
         */
        @Override
        public void paintSliderTrack(
            Graphics2D g, Palette p, Rectangle track, int thumbCentre,
            boolean horizontal, boolean inverted, boolean enabled
        ) {
            LafUtilities.antialiasShapes(g);
            int   t    = Math.max(3, UI.scale(sliderTrackThickness()));
            Color tone = enabled ? p.border() : LafUtilities.shiftHsb(p.border(), 0, +0.200);
            float arc  = t;
            Shape groove = horizontal
                    ? new RoundRectangle2D.Float(track.x, track.y + (track.height - t) / 2f, track.width, t, arc, arc)
                    : new RoundRectangle2D.Float(track.x + (track.width - t) / 2f, track.y, t, track.height, arc, arc);
            java.awt.geom.Rectangle2D span = groove.getBounds2D();
            g.setPaint(NimbusRelief.CUT.paint((float) span.getY(), (float) span.getHeight(), tone));
            g.fill(groove);
        }

        @Override
        public void paintSliderThumb( Graphics2D g, Palette p, Rectangle r, boolean enabled, boolean focused ) {
            Shape knob = new Ellipse2D.Float(r.x + 0.5f, r.y + 0.5f, r.width - 1, r.height - 1);
            mould(g, p, knob, r.y, r.height, enabled, false, false, focused);
        }

        @Override
        public void paintScrollThumb( Graphics2D g, Palette p, Rectangle r, boolean active ) {
            LafUtilities.antialiasShapes(g);
            int   pad  = UI.scale(1);
            float arc  = Math.min(r.width, r.height) - 2 * pad;
            Shape pill = new RoundRectangle2D.Float(
                                r.x + pad + 0.5f, r.y + pad + 0.5f,
                                r.width - 2 * pad - 1, r.height - 2 * pad - 1, arc, arc
                            );
            Color tone = NimbusPreset.accentedTone(p, active, false);
            g.setPaint(NimbusRelief.LIT_ACCENTED.paint(r.y + pad, r.height - 2f * pad, tone));
            g.fill(pill);
            g.setColor(NimbusPreset.accentedEdge(p));
            g.setStroke(new BasicStroke(1f));
            g.draw(pill);
        }

        @Override
        public void paintSplitGrip( Graphics2D g, Palette p, int w, int h, boolean horizontalSplit, boolean enabled ) {
            g.setColor(p.borderSoft());
            if ( horizontalSplit )
                g.fillRect(w / 2, 0, Math.max(1, UI.scale(1)), h);
            else
                g.fillRect(0, h / 2, w, Math.max(1, UI.scale(1)));
            dots(g, p, w, h, horizontalSplit, UI.scale(4));
        }

        @Override
        public void paintDragHandle( Graphics2D g, Palette p, int w, int h, boolean horizontal ) {
            int size = Math.max(2, UI.scale(2));
            g.setColor(p.border());
            for ( int i = 0; i < 4; i++ ) {
                int step = i * UI.scale(4);
                if ( horizontal )
                    g.fillRect(UI.scale(4), h / 2 - UI.scale(6) + step, size, size);
                else
                    g.fillRect(w / 2 - UI.scale(6) + step, UI.scale(4), size, size);
            }
        }

        /**
         *  The one wet thing in the theme: a saturated bar under a hard sheen, closed top and bottom by
         *  a line of its own colour darkened, so the bar reads as a filled tube rather than as a
         *  painted rectangle.
         */
        @Override
        public void paintProgressFill(
            Graphics2D g, Palette p, int w, int h, double ratio, boolean horizontal, boolean enabled
        ) {
            if ( ratio <= 0 )
                return;
            LafUtilities.antialiasShapes(g);
            Color tone = enabled ? p.primary() : p.surfaceDisabled();
            int   fillW = horizontal ? (int) Math.round(w * ratio) : w;
            int   fillH = horizontal ? h : (int) Math.round(h * ratio);
            int   fillY = horizontal ? 0 : h - fillH;
            g.setPaint(NimbusRelief.GLOSS.paint(fillY, fillH, tone));
            g.fillRect(0, fillY, fillW, fillH);
            g.setColor(LafUtilities.shiftHsb(tone, 0, -0.153));
            g.fillRect(0, fillY, fillW, 1);
            g.setColor(LafUtilities.shiftHsb(tone, -0.082, -0.224));
            g.fillRect(0, fillY + fillH - 1, fillW, 1);
        }

        @Override
        public void paintTabSurface(
            Graphics2D g, Palette p, int x, int y, int w, int h, boolean selected, boolean rollover
        ) {
            LafUtilities.antialiasShapes(g);
            float arc = UI.scale(7f);
            // Rounded at the top only: the bottom edge has to meet the page squarely, or the tab and
            // the page it belongs to read as two separate things.
            Shape tab = new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, w - 1, h - 1 + arc, arc, arc);
            Color tone = selected ? NimbusPreset.accentedTone(p, false, false)
                                  : rollover ? p.surfaceHover() : p.surface();
            // A tab that is not the one you are on has no bottom lip to catch the light: it runs under
            // the page rather than standing beside it.
            g.setPaint(( selected ? NimbusRelief.LIT_ACCENTED : NimbusRelief.STRIP ).paint(y, h, tone));
            g.fill(tab);
            g.setColor(selected ? NimbusPreset.accentedEdge(p) : p.border());
            g.setStroke(new BasicStroke(1f));
            g.draw(tab);
        }

        @Override
        public void paintTabAccent(
            Graphics2D g, Palette p, int x, int y, int w, int h, int tabPlacement, boolean enabled
        ) {
            int line = Math.max(1, UI.scale(2));
            g.setColor(enabled ? LafUtilities.shiftHsb(p.accent(), 0, -0.180) : p.border());
            switch ( tabPlacement ) {
                case SwingConstants.BOTTOM: g.fillRect(x, y, w, line);            break;
                case SwingConstants.LEFT:   g.fillRect(x + w - line, y, line, h); break;
                case SwingConstants.RIGHT:  g.fillRect(x, y, line, h);            break;
                case SwingConstants.TOP:
                default:                    g.fillRect(x, y + h - line, w, line); break;
            }
        }

        // ── Internals ────────────────────────────────────────────────────────

        /**
         *  Fills a shape the way the style rules fill a button - the relief over the state's own
         *  colour, inside the state's own outline - so that a check box and the button beside it are
         *  visibly the same material.
         *
         * @param on whether the control is ticked, filled or otherwise affirmative, which is what
         *           moves it onto the accented material
         * @param y the top of the shape, which the relief has to be anchored to rather than to the
         *          component, since a glyph sits somewhere inside a taller row
         * @param h how tall the shape is
         */
        private static void mould(
            Graphics2D g, Palette p, Shape shape, int y, int h,
            boolean enabled, boolean on, boolean pressed, boolean rollover
        ) {
            LafUtilities.antialiasShapes(g);
            boolean accented = enabled && on;
            Color   tone     = accented ? NimbusPreset.accentedTone(p, pressed, rollover)
                                        : NimbusPreset.surfaceTone(p, enabled, pressed, rollover);
            g.setPaint(NimbusPreset.relief(enabled, accented).paint(y, h, tone));
            g.fill(shape);
            Color edge = accented ? NimbusPreset.accentedEdge(p)
                                  : NimbusPreset.surfaceEdge(p, enabled, pressed, rollover);
            g.setStroke(new BasicStroke(1f));
            g.setColor(edge);
            g.draw(shape);
            if ( !enabled )
                return;
            // The bottom of the outline again, darker, clipped to the lower third so that the sides
            // keep the colour they had. Drawing it as an arc instead would have to know the shape.
            Rectangle bounds = shape.getBounds();
            Shape     clip   = g.getClip();
            g.clipRect(bounds.x, y + h - Math.max(1, h / 3), bounds.width + 1, h);
            g.setColor(NimbusPreset.contactEdge(edge));
            g.draw(shape);
            g.setClip(clip);
        }

        private static void wedge(
            Graphics2D g, Palette p, float cx, float cy, float size, LafUtilities.Direction direction, boolean enabled
        ) {
            LafUtilities.antialiasShapes(g);
            g.setColor(enabled ? p.text() : p.textDisabled());
            g.fill(LafUtilities.arrowShape(cx, cy, size, size * 0.6f, direction));
        }

        private static void dots( Graphics2D g, Palette p, int w, int h, boolean vertical, int step ) {
            int size = Math.max(2, UI.scale(2));
            g.setColor(p.border());
            for ( int i = 0; i < 3; i++ ) {
                int x = vertical ? w / 2 - size / 2                   : w / 2 - step + i * step - size / 2;
                int y = vertical ? h / 2 - step + i * step - size / 2 : h / 2 - size / 2;
                g.fillRect(x, y, size, size);
            }
        }
    }

    /**
     *  Symbols for {@link SwingTreeLookAndFeel.StylePreset#POLYMORPHIC}: not a set of its own, but a
     *  choice between three of the others, remade on every call from the palette in force.
     *  <p>
     *  The preset it belongs to reads its whole idiom off the palette (see {@link Mood}), and the
     *  small geometry has to follow, or a theme that separates its surfaces with light would end up
     *  with flat glyphs drawn onto them. So the same question is asked here, and answered with
     *  whichever existing set was designed for that answer.
     *
     *  @see SwingTreeLookAndFeel.SymbolPreset#ADAPTIVE
     */
    final class Adaptive implements Symbols
    {
        static final Symbols INSTANCE = new Adaptive();

        private Adaptive() {}

        /** @return the set designed for what the palette in force leaves a theme to work with. */
        private static Symbols chosen() {
            switch ( Mood.of(SwingTreeLookAndFeel.palette()) ) {
                case RELIEF:   return Soft.INSTANCE;
                case LUMINOUS: return Glass.INSTANCE;
                case SHEET:
                default:       return Material.INSTANCE;
            }
        }

        @Override public boolean drawsItsOwnChrome() { return chosen().drawsItsOwnChrome(); }
        @Override public int checkGlyphSize() { return chosen().checkGlyphSize(); }
        @Override public int arrowGlyphSize() { return chosen().arrowGlyphSize(); }
        @Override public int comboArrowButtonSize() { return chosen().comboArrowButtonSize(); }
        @Override public int spinnerButtonWidth() { return chosen().spinnerButtonWidth(); }
        @Override public int spinnerButtonHeight() { return chosen().spinnerButtonHeight(); }
        @Override public int sliderThumbDiameter() { return chosen().sliderThumbDiameter(); }
        @Override public int sliderTrackThickness() { return chosen().sliderTrackThickness(); }
        @Override public int scrollBarThickness() { return chosen().scrollBarThickness(); }
        @Override public int splitDividerThickness() { return chosen().splitDividerThickness(); }
        @Override public int progressBarThickness() { return chosen().progressBarThickness(); }
        @Override public int separatorThickness() { return chosen().separatorThickness(); }
        @Override public int tableRowHeight() { return chosen().tableRowHeight(); }
        @Override public int treeRowHeight() { return chosen().treeRowHeight(); }
        @Override public int tabPaddingVertical() { return chosen().tabPaddingVertical(); }
        @Override public int tabPaddingHorizontal() { return chosen().tabPaddingHorizontal(); }
        @Override public int tabAreaGap() { return chosen().tabAreaGap(); }
        @Override public void paintCheckGlyph( Graphics2D g, Palette p, int x, int y, int w, int h, boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected ) { chosen().paintCheckGlyph(g, p, x, y, w, h, enabled, focused, rollover, pressed, selected); }
        @Override public void paintRadioGlyph( Graphics2D g, Palette p, int x, int y, int w, int h, boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected ) { chosen().paintRadioGlyph(g, p, x, y, w, h, enabled, focused, rollover, pressed, selected); }
        @Override public void paintDisclosure( Graphics2D g, Palette p, int x, int y, int w, int h, boolean expanded, boolean enabled ) { chosen().paintDisclosure(g, p, x, y, w, h, expanded, enabled); }
        @Override public void paintSubmenuArrow( Graphics2D g, Palette p, int x, int y, int w, int h, boolean enabled ) { chosen().paintSubmenuArrow(g, p, x, y, w, h, enabled); }
        @Override public void paintComboArrow( Graphics2D g, Palette p, int w, int h, boolean enabled, boolean rollover, boolean pressed ) { chosen().paintComboArrow(g, p, w, h, enabled, rollover, pressed); }
        @Override public void paintSpinnerArrow( Graphics2D g, Palette p, int w, int h, boolean up, boolean enabled, boolean rollover, boolean pressed ) { chosen().paintSpinnerArrow(g, p, w, h, up, enabled, rollover, pressed); }
        @Override public void paintSliderTrack( Graphics2D g, Palette p, Rectangle track, int thumbCentre, boolean horizontal, boolean inverted, boolean enabled ) { chosen().paintSliderTrack(g, p, track, thumbCentre, horizontal, inverted, enabled); }
        @Override public void paintSliderThumb( Graphics2D g, Palette p, Rectangle thumb, boolean enabled, boolean focused ) { chosen().paintSliderThumb(g, p, thumb, enabled, focused); }
        @Override public void paintScrollThumb( Graphics2D g, Palette p, Rectangle thumb, boolean active ) { chosen().paintScrollThumb(g, p, thumb, active); }
        @Override public void paintSplitGrip( Graphics2D g, Palette p, int w, int h, boolean horizontalSplit, boolean enabled ) { chosen().paintSplitGrip(g, p, w, h, horizontalSplit, enabled); }
        @Override public void paintDragHandle( Graphics2D g, Palette p, int w, int h, boolean horizontal ) { chosen().paintDragHandle(g, p, w, h, horizontal); }
        @Override public void paintProgressFill( Graphics2D g, Palette p, int w, int h, double ratio, boolean horizontal, boolean enabled ) { chosen().paintProgressFill(g, p, w, h, ratio, horizontal, enabled); }
        @Override public void paintTabSurface( Graphics2D g, Palette p, int x, int y, int w, int h, boolean selected, boolean rollover ) { chosen().paintTabSurface(g, p, x, y, w, h, selected, rollover); }
        @Override public void paintTabAccent( Graphics2D g, Palette p, int x, int y, int w, int h, int tabPlacement, boolean enabled ) { chosen().paintTabAccent(g, p, x, y, w, h, tabPlacement, enabled); }
    }
}
