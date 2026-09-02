package examples.laf;

import examples.laf.SwingTreeLookAndFeel.Palette;
import swingtree.UI;
import swingtree.api.laf.OptimizedShapeRendering;

import javax.swing.SwingConstants;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

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
final class GlassSymbols implements Symbols
{
    static final Symbols INSTANCE = new GlassSymbols();

    private GlassSymbols() {}

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
    private static void rim( Graphics2D g, Palette p, java.awt.Shape shape, boolean enabled, boolean focused ) {
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
