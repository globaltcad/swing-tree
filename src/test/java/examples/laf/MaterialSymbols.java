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
final class MaterialSymbols implements Symbols
{
    static final Symbols INSTANCE = new MaterialSymbols();

    private MaterialSymbols() {}

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
            g.fill(new RoundRectangle2D.Float(x, y, w - 1, h - 1, arc, arc));
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
            g.fill(new RoundRectangle2D.Float(track.x, y, track.width, t, arc, arc));
            int filled = inverted ? Math.max(0, track.x + track.width - thumbCentre)
                                  : Math.max(0, thumbCentre - track.x);
            if ( filled > 0 ) {
                g.setColor(fill);
                g.fill(new RoundRectangle2D.Float(inverted ? thumbCentre : track.x, y, filled, t, arc, arc));
            }
        } else {
            int x = track.x + (track.width - t) / 2;
            g.setColor(empty);
            g.fill(new RoundRectangle2D.Float(x, track.y, t, track.height, arc, arc));
            int filled = inverted ? Math.max(0, thumbCentre - track.y)
                                  : Math.max(0, track.y + track.height - thumbCentre);
            if ( filled > 0 ) {
                g.setColor(fill);
                g.fill(new RoundRectangle2D.Float(x, inverted ? track.y : thumbCentre, t, filled, arc, arc));
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
        g.fill(new RoundRectangle2D.Float(r.x + pad, r.y + pad, r.width - 2 * pad, r.height - 2 * pad, arc, arc));
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
            g.fill(new RoundRectangle2D.Float(0, 0, fillW, h, h, h));
        } else {
            int fillH = Math.max(w, (int) Math.round(h * ratio));
            g.fill(new RoundRectangle2D.Float(0, h - fillH, w, fillH, w, w));
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
