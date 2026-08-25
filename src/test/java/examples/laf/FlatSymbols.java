package examples.laf;

import examples.laf.SwingTreeLookAndFeel.Palette;
import swingtree.UI;

import javax.swing.SwingConstants;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;

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
final class FlatSymbols implements Symbols
{
    static final Symbols INSTANCE = new FlatSymbols();

    private FlatSymbols() {}

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
