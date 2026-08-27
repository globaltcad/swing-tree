package examples.laf;

import examples.laf.SwingTreeLookAndFeel.Palette;
import swingtree.UI;

import javax.swing.SwingConstants;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;


/**
 *  Symbols for {@link SwingTreeLookAndFeel.StylePreset#FRUTIGER_AERO}: everything is a piece of wet
 *  glass. A glyph is a saturated fill under a highlight that breaks on a hard line just above the
 *  middle, wrapped in a crisp outline a few shades darker than the fill, with a white sheen along
 *  the top edge.
 *  <p>
 *  Arrows are solid rather than stroked, and carry a pale copy of themselves one pixel below - the
 *  drop shadow every toolbar icon of the period had.
 */
final class GlossySymbols implements Symbols
{
    static final Symbols INSTANCE = new GlossySymbols();

    private GlossySymbols() {}

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
            g.fill(new RoundRectangle2D.Float(track.x, y, track.width, t, arc, arc));
            g.setColor(p.border());
            g.setStroke(new BasicStroke(Math.max(1f, UI.scale(1f))));
            g.draw(new RoundRectangle2D.Float(track.x, y, track.width - 1, t - 1, arc, arc));
            int filled = inverted ? Math.max(0, track.x + track.width - thumbCentre)
                                  : Math.max(0, thumbCentre - track.x);
            if ( filled > 0 ) {
                g.setPaint(LafUtilities.glossGradient(y, t, fill));
                g.fill(new RoundRectangle2D.Float(inverted ? thumbCentre : track.x, y, filled, t, arc, arc));
            }
        } else {
            int x = track.x + (track.width - t) / 2;
            g.setColor(LafUtilities.shadeTowardsBlack(empty, 0.08));
            g.fill(new RoundRectangle2D.Float(x, track.y, t, track.height, arc, arc));
            g.setColor(p.border());
            g.setStroke(new BasicStroke(Math.max(1f, UI.scale(1f))));
            g.draw(new RoundRectangle2D.Float(x, track.y, t - 1, track.height - 1, arc, arc));
            int filled = inverted ? Math.max(0, thumbCentre - track.y)
                                  : Math.max(0, track.y + track.height - thumbCentre);
            if ( filled > 0 ) {
                int top = inverted ? track.y : thumbCentre;
                g.setPaint(LafUtilities.glossGradient(top, filled, fill));
                g.fill(new RoundRectangle2D.Float(x, top, t, filled, arc, arc));
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
            g.fill(new RoundRectangle2D.Float(pad, pad, fillW, inner, inner, inner));
        } else {
            int inner = w - 2 * pad;
            int fillH = Math.max(inner, (int) Math.round((h - 2 * pad) * ratio));
            g.setPaint(LafUtilities.glossGradient(h - pad - fillH, fillH, base));
            g.fill(new RoundRectangle2D.Float(pad, h - pad - fillH, inner, fillH, inner, inner));
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
                g.fill(new RoundRectangle2D.Float(x, y, w, stripe, arc, arc)); break;
            case SwingConstants.LEFT:
                g.fill(new RoundRectangle2D.Float(x + w - stripe, y, stripe, h, arc, arc)); break;
            case SwingConstants.RIGHT:
                g.fill(new RoundRectangle2D.Float(x, y, stripe, h, arc, arc)); break;
            case SwingConstants.TOP:
            default:
                g.fill(new RoundRectangle2D.Float(x, y + h - stripe, w, stripe, arc, arc)); break;
        }
    }

    // ── Internals ────────────────────────────────────────────────────────

    private static Color glyphFill( Palette p, boolean enabled, boolean rollover, boolean pressed, boolean selected ) {
        if ( !enabled )  return p.surfaceDisabled();
        if ( selected )  return pressed ? p.primaryPressed() : rollover ? p.primaryHover() : p.primary();
        if ( pressed )   return p.surfacePressed();
        return rollover ? p.surfaceHover() : p.surfaceField();
    }

    private static void outline( Graphics2D g, java.awt.Shape body, Palette p, boolean enabled, boolean focused, Color base ) {
        g.setColor(!enabled ? p.borderSoft() : focused ? p.accent() : LafUtilities.shadeTowardsBlack(base, 0.30));
        g.setStroke(new BasicStroke(Math.max(1f, UI.scale(1.1f))));
        g.draw(body);
    }

    /** The white sheen along the top edge, which is what makes a fill read as wet. */
    private static void sheen( Graphics2D g, float x, float y, float w, float h, int arc ) {
        if ( w <= 0 || h <= 0 )
            return;
        g.setColor(new Color(255, 255, 255, 120));
        g.fill(new RoundRectangle2D.Float(x, y, w, h, arc, arc));
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
