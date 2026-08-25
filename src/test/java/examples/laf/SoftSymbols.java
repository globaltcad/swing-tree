package examples.laf;

import examples.laf.SwingTreeLookAndFeel.Palette;
import swingtree.UI;

import javax.swing.SwingConstants;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

import static examples.laf.SymbolPainting.Direction;
import static examples.laf.SymbolPainting.antialias;
import static examples.laf.SymbolPainting.arrow;
import static examples.laf.SymbolPainting.tick;
import static examples.laf.SymbolPainting.topToBottom;

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
        antialias(g);
        int arc = UI.scale(6);
        RoundRectangle2D.Float body = new RoundRectangle2D.Float(x, y, w - 1, h - 1, arc, arc);
        g.setColor(surface(p, enabled, rollover));
        g.fill(body);
        strokeRim(g, body, p, y, h, selected || pressed);
        if ( !selected )
            return;
        g.setColor(enabled ? p.accent() : p.textDisabled());
        g.setStroke(new BasicStroke(Math.max(1.6f, UI.scale(2f)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(tick(x, y, w, h));
    }

    @Override
    public void paintRadioGlyph(
        Graphics2D g, Palette p, int x, int y, int w, int h,
        boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
    ) {
        antialias(g);
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
                      expanded ? Direction.DOWN : Direction.RIGHT, enabled);
    }

    @Override
    public void paintSubmenuArrow( Graphics2D g, Palette p, int x, int y, int w, int h, boolean enabled ) {
        embossedArrow(g, p, x + w / 2f, y + h / 2f, UI.scale(3.2f), Direction.RIGHT, enabled);
    }

    @Override
    public void paintComboArrow(
        Graphics2D g, Palette p, int w, int h, boolean enabled, boolean rollover, boolean pressed
    ) {
        embossedArrow(g, p, w / 2f, h / 2f, UI.scale(3.6f), Direction.DOWN, enabled && !pressed);
    }

    @Override
    public void paintSpinnerArrow(
        Graphics2D g, Palette p, int w, int h, boolean up,
        boolean enabled, boolean rollover, boolean pressed
    ) {
        embossedArrow(g, p, w / 2f, h / 2f, UI.scale(2.8f),
                      up ? Direction.UP : Direction.DOWN, enabled && !pressed);
    }

    // ── Chrome ───────────────────────────────────────────────────────────

    @Override
    public void paintSliderTrack(
        Graphics2D g, Palette p, Rectangle track, int thumbCentre,
        boolean horizontal, boolean inverted, boolean enabled
    ) {
        antialias(g);
        int t   = Math.max(3, UI.scale(sliderTrackThickness()));
        int arc = t;
        Color fill = enabled ? p.accent() : p.textDisabled();
        if ( horizontal ) {
            int y = track.y + (track.height - t) / 2;
            g.setPaint(topToBottom(y, t, Shades.darker(p.background(), 0.14), Shades.lighter(p.background(), 0.5)));
            g.fill(new RoundRectangle2D.Float(track.x, y, track.width, t, arc, arc));
            int filled = inverted ? Math.max(0, track.x + track.width - thumbCentre)
                                  : Math.max(0, thumbCentre - track.x);
            if ( filled > 0 ) {
                g.setColor(fill);
                g.fill(new RoundRectangle2D.Float(inverted ? thumbCentre : track.x, y, filled, t, arc, arc));
            }
        } else {
            int x = track.x + (track.width - t) / 2;
            g.setPaint(topToBottom(track.y, track.height,
                                   Shades.darker(p.background(), 0.14), Shades.lighter(p.background(), 0.5)));
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
        antialias(g);
        Ellipse2D.Float body = new Ellipse2D.Float(r.x, r.y, r.width - 1, r.height - 1);
        g.setColor(enabled ? p.surface() : p.surfaceDisabled());
        g.fill(body);
        strokeRim(g, body, p, r.y, r.height, false);
        if ( !enabled )
            return;
        float dot = UI.scale(6f);
        g.setColor(focused ? p.accent() : Shades.mix(p.accent(), p.surface(), 0.35));
        g.fill(new Ellipse2D.Float(r.x + dot, r.y + dot, r.width - 1 - 2 * dot, r.height - 1 - 2 * dot));
    }

    @Override
    public void paintScrollThumb( Graphics2D g, Palette p, Rectangle r, boolean active ) {
        antialias(g);
        int pad = UI.scale(3);
        int arc = UI.scale(10);
        RoundRectangle2D.Float body = new RoundRectangle2D.Float(
                r.x + pad, r.y + pad, r.width - 2 * pad, r.height - 2 * pad, arc, arc);
        g.setColor(active ? Shades.mix(p.surface(), p.accent(), 0.22) : p.surface());
        g.fill(body);
        strokeRim(g, body, p, r.y + pad, r.height - 2 * pad, false);
    }

    @Override
    public void paintSplitGrip( Graphics2D g, Palette p, int w, int h, boolean horizontalSplit, boolean enabled ) {
        antialias(g);
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
        antialias(g);
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
        antialias(g);
        int   pad  = UI.scale(3);
        Color base = enabled ? p.accent() : p.textDisabled();
        if ( horizontal ) {
            int inner = h - 2 * pad;
            int fillW = Math.max(inner, (int) Math.round((w - 2 * pad) * ratio));
            g.setPaint(topToBottom(pad, inner, Shades.lighter(base, 0.22), Shades.darker(base, 0.10)));
            g.fill(new RoundRectangle2D.Float(pad, pad, fillW, inner, inner, inner));
        } else {
            int inner = w - 2 * pad;
            int fillH = Math.max(inner, (int) Math.round((h - 2 * pad) * ratio));
            g.setPaint(topToBottom(h - pad - fillH, fillH, Shades.lighter(base, 0.22), Shades.darker(base, 0.10)));
            g.fill(new RoundRectangle2D.Float(pad, h - pad - fillH, inner, fillH, inner, inner));
        }
    }

    @Override
    public void paintTabSurface(
        Graphics2D g, Palette p, int x, int y, int w, int h, boolean selected, boolean rollover
    ) {
        if ( !selected && !rollover )
            return;
        antialias(g);
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
        antialias(g);
        int stripe = Math.max(2, UI.scale(3));
        int arc    = stripe;
        g.setColor(enabled ? p.accent() : p.textDisabled());
        switch ( tabPlacement ) {
            case SwingConstants.BOTTOM:
                g.fill(new RoundRectangle2D.Float(x + w * 0.25f, y, w * 0.5f, stripe, arc, arc)); break;
            case SwingConstants.LEFT:
                g.fill(new RoundRectangle2D.Float(x + w - stripe, y + h * 0.25f, stripe, h * 0.5f, arc, arc)); break;
            case SwingConstants.RIGHT:
                g.fill(new RoundRectangle2D.Float(x, y + h * 0.25f, stripe, h * 0.5f, arc, arc)); break;
            case SwingConstants.TOP:
            default:
                g.fill(new RoundRectangle2D.Float(x + w * 0.25f, y + h - stripe, w * 0.5f, stripe, arc, arc)); break;
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
        Color light = Shades.lighter(p.background(), 0.55);
        Color dark  = Shades.darker(p.background(), 0.22);
        g.setStroke(new BasicStroke(Math.max(1.4f, UI.scale(1.6f))));
        g.setPaint(topToBottom(y, h, inverted ? dark : light, inverted ? light : dark));
        g.draw(body);
    }

    private static void embossedArrow(
        Graphics2D g, Palette p, float cx, float cy, float size, Direction direction, boolean enabled
    ) {
        antialias(g);
        float lift = UI.scale(1f);
        Path2D.Float shape = arrow(cx, cy, size, size * 0.62f, direction);
        g.translate(lift, lift);
        g.setColor(Shades.lighter(p.background(), 0.6));
        g.fill(shape);
        g.translate(-lift, -lift);
        g.setColor(enabled ? p.textMuted() : p.textDisabled());
        g.fill(shape);
    }

    private static void embossedDot( Graphics2D g, Palette p, float cx, float cy, float radius, boolean enabled ) {
        float lift = UI.scale(1f);
        g.setColor(Shades.lighter(p.background(), 0.6));
        g.fill(new Ellipse2D.Float(cx - radius + lift, cy - radius + lift, 2 * radius, 2 * radius));
        g.setColor(enabled ? Shades.darker(p.background(), 0.20) : p.borderSoft());
        g.fill(new Ellipse2D.Float(cx - radius, cy - radius, 2 * radius, 2 * radius));
    }
}
