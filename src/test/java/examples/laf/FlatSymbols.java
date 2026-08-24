package examples.laf;

import examples.laf.SwingTreeLookAndFeel.Palette;
import swingtree.UI;

import javax.swing.SwingConstants;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 *  Symbols drawn with thin strokes, round caps, no bevels and no gradients: the geometry reads
 *  at a glance and stays crisp at any scale factor, because every stroke and radius is derived
 *  from {@link UI#scale(float)} rather than from a fixed bitmap.
 *  <p>
 *  This is the set behind {@link SwingTreeLookAndFeel.SymbolPreset#FLAT_AND_SIMPLE}. It is
 *  stateless, so a single instance serves the whole application.
 */
final class FlatSymbols implements Symbols
{
    static final Symbols INSTANCE = new FlatSymbols();

    private FlatSymbols() {}

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
        g.fill(new RoundRectangle2D.Float(x, y, w - 1, h - 1, arc, arc));
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
            g.fill(new RoundRectangle2D.Float(track.x, y, track.width, t, arc, arc));
            if ( !inverted ) {
                int filled = Math.max(0, thumbCentre - track.x);
                if ( filled > 0 ) {
                    g.setColor(fill);
                    g.fill(new RoundRectangle2D.Float(track.x, y, filled, t, arc, arc));
                }
            } else {
                int filled = Math.max(0, track.x + track.width - thumbCentre);
                if ( filled > 0 ) {
                    g.setColor(fill);
                    g.fill(new RoundRectangle2D.Float(thumbCentre, y, filled, t, arc, arc));
                }
            }
        } else {
            int x = track.x + (track.width - t) / 2;
            g.setColor(p.borderSoft());
            g.fill(new RoundRectangle2D.Float(x, track.y, t, track.height, arc, arc));
            // A vertical slider defaults to "max at top", so the un-inverted fill covers the
            // area below the handle, down to the bottom of the track.
            if ( !inverted ) {
                int filled = Math.max(0, track.y + track.height - thumbCentre);
                if ( filled > 0 ) {
                    g.setColor(fill);
                    g.fill(new RoundRectangle2D.Float(x, thumbCentre, t, filled, arc, arc));
                }
            } else {
                int filled = Math.max(0, thumbCentre - track.y);
                if ( filled > 0 ) {
                    g.setColor(fill);
                    g.fill(new RoundRectangle2D.Float(x, track.y, t, filled, arc, arc));
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
        g.fill(new RoundRectangle2D.Float(r.x + pad, r.y + pad,
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
            g.fill(new RoundRectangle2D.Float(pad, pad, fillW, h - 2 * pad, arc, arc));
        } else {
            int fillH = Math.max(arc, (int) Math.round((h - 2 * pad) * ratio));
            g.fill(new RoundRectangle2D.Float(pad, h - pad - fillH, w - 2 * pad, fillH, arc, arc));
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
        g.fill(new RoundRectangle2D.Float(x, y, w, h, arc, arc));
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
                g.fill(new RoundRectangle2D.Float(x, y, w, stripe, arc, arc));
                break;
            case SwingConstants.LEFT:
                g.fill(new RoundRectangle2D.Float(x + w - stripe, y, stripe, h, arc, arc));
                break;
            case SwingConstants.RIGHT:
                g.fill(new RoundRectangle2D.Float(x, y, stripe, h, arc, arc));
                break;
            case SwingConstants.TOP:
            default:
                g.fill(new RoundRectangle2D.Float(x, y + h - stripe, w, stripe, arc, arc));
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
