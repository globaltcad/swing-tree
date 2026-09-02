package examples.laf;

import examples.laf.SwingTreeLookAndFeel.Palette;
import swingtree.UI;
import swingtree.api.laf.ShapeRendering;

import javax.swing.SwingConstants;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;


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
final class SoftSymbols implements Symbols
{
    static final Symbols INSTANCE = new SoftSymbols();

    private SoftSymbols() {}

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
            ShapeRendering.fill(g, new RoundRectangle2D.Float(track.x, y, track.width, t, arc, arc));
            int filled = inverted ? Math.max(0, track.x + track.width - thumbCentre)
                                  : Math.max(0, thumbCentre - track.x);
            if ( filled > 0 ) {
                g.setColor(fill);
                ShapeRendering.fill(g, new RoundRectangle2D.Float(inverted ? thumbCentre : track.x, y, filled, t, arc, arc));
            }
        } else {
            int x = track.x + (track.width - t) / 2;
            g.setPaint(LafUtilities.verticalGradient(track.y, track.height,
                                   LafUtilities.shadeBySteps(p.background(), GROOVE_DARK), LafUtilities.shadeBySteps(p.background(), GROOVE_LIGHT)));
            ShapeRendering.fill(g, new RoundRectangle2D.Float(x, track.y, t, track.height, arc, arc));
            int filled = inverted ? Math.max(0, thumbCentre - track.y)
                                  : Math.max(0, track.y + track.height - thumbCentre);
            if ( filled > 0 ) {
                g.setColor(fill);
                ShapeRendering.fill(g, new RoundRectangle2D.Float(x, inverted ? track.y : thumbCentre, t, filled, arc, arc));
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
            ShapeRendering.fill(g, new RoundRectangle2D.Float(pad, pad, fillW, inner, inner, inner));
        } else {
            int inner = w - 2 * pad;
            int fillH = Math.max(inner, (int) Math.round((h - 2 * pad) * ratio));
            g.setPaint(LafUtilities.verticalGradient(h - pad - fillH, fillH, LafUtilities.shadeTowardsWhite(base, 0.22), LafUtilities.shadeTowardsBlack(base, 0.10)));
            ShapeRendering.fill(g, new RoundRectangle2D.Float(pad, h - pad - fillH, inner, fillH, inner, inner));
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
                ShapeRendering.fill(g, new RoundRectangle2D.Float(x + w * 0.25f, y, w * 0.5f, stripe, arc, arc)); break;
            case SwingConstants.LEFT:
                ShapeRendering.fill(g, new RoundRectangle2D.Float(x + w - stripe, y + h * 0.25f, stripe, h * 0.5f, arc, arc)); break;
            case SwingConstants.RIGHT:
                ShapeRendering.fill(g, new RoundRectangle2D.Float(x, y + h * 0.25f, stripe, h * 0.5f, arc, arc)); break;
            case SwingConstants.TOP:
            default:
                ShapeRendering.fill(g, new RoundRectangle2D.Float(x + w * 0.25f, y + h - stripe, w * 0.5f, stripe, arc, arc)); break;
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
        Graphics2D g, java.awt.Shape body, Palette p, float y, float h, boolean inverted
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
