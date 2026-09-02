package examples.laf;

import examples.laf.SwingTreeLookAndFeel.Palette;
import swingtree.UI;

import javax.swing.SwingConstants;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

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
final class NimbusSymbols implements Symbols
{
    static final Symbols INSTANCE = new NimbusSymbols();

    private NimbusSymbols() {}

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
