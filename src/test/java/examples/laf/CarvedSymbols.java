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
import java.awt.geom.RoundRectangle2D;

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
final class CarvedSymbols implements Symbols
{
    static final Symbols INSTANCE = new CarvedSymbols();

    private CarvedSymbols() {}

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
        ShapeRendering.fill(g, new RoundRectangle2D.Float(x, y + 1, w - 1, h - 1, arc, arc));

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
            ShapeRendering.fill(g, new RoundRectangle2D.Float(inverted ? thumbCentre : track.x, y, filled, t, arc, arc));
        } else {
            int x = track.x + (track.width - t) / 2;
            groove(g, x, track.y, t, track.height, arc, p);
            int filled = inverted ? Math.max(0, thumbCentre - track.y)
                                  : Math.max(0, track.y + track.height - thumbCentre);
            if ( filled <= 0 )
                return;
            g.setColor(fill);
            ShapeRendering.fill(g, new RoundRectangle2D.Float(x, inverted ? track.y : thumbCentre, t, filled, arc, arc));
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
        ShapeRendering.fill(g, new RoundRectangle2D.Float(body.x, body.y + 1, body.width, body.height, arc, arc));
        g.setPaint(LafUtilities.glossGradient(body.y, body.height, base));
        ShapeRendering.fill(g, new RoundRectangle2D.Float(body.x, body.y, body.width, body.height, arc, arc));
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
            ShapeRendering.fill(g, new RoundRectangle2D.Float(0, 0, fillW, h, h, h));
        } else {
            int fillH = Math.max(w, (int) Math.round(h * ratio));
            g.setColor(base);
            ShapeRendering.fill(g, new RoundRectangle2D.Float(0, h - fillH, w, fillH, w, w));
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
        ShapeRendering.fill(g, new RoundRectangle2D.Float(x, y + 1, w, h, arc, arc));
        g.setPaint(LafUtilities.verticalGradient(y, h, LafUtilities.shadeBySteps(base, 16), base));
        ShapeRendering.fill(g, new RoundRectangle2D.Float(x, y, w, h, arc, arc));
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
        ShapeRendering.fill(g, new RoundRectangle2D.Float(x, y + 1, w, h, arc, arc));
        g.setPaint(LafUtilities.verticalGradient(y, h,
                        LafUtilities.withOpacity(Color.BLACK, GROOVE), p.surfaceDisabled()));
        ShapeRendering.fill(g, new RoundRectangle2D.Float(x, y, w, h, arc, arc));
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
