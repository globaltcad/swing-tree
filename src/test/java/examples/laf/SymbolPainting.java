package examples.laf;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

/**
 *  The handful of drawing primitives every symbol set needs, in one place so that four of them do
 *  not each grow their own slightly different version.
 *  <p>
 *  Nothing here decides how anything looks - a caller passes the colours and the geometry. What it
 *  removes is the boilerplate that would otherwise be copied: turning antialiasing on, building a
 *  top-to-bottom paint without tripping over a zero-height shape, and describing a solid arrow as a
 *  path rather than as three {@code lineTo} calls at every call site.
 */
final class SymbolPainting
{
    private SymbolPainting() {}

    /** Which way a solid arrow points. */
    enum Direction { UP, DOWN, LEFT, RIGHT }

    /**
     *  Turns on shape antialiasing, which every symbol wants and none of them want to repeat.
     *
     * @param g the graphics context to configure
     */
    static void antialias( Graphics2D g ) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    /**
     *  A top-to-bottom two-stop paint, which is what a lit rim, a gloss and a groove all are.
     *  <p>
     *  A {@link GradientPaint} whose two points coincide is illegal, so a shape with no height
     *  falls back to its top colour rather than throwing.
     *
     * @param y      the top edge, in component pixels
     * @param height how tall the shape is, in component pixels
     * @param top    the colour at the top edge
     * @param bottom the colour at the bottom edge
     * @return the paint to fill or stroke with
     */
    static Paint topToBottom( float y, float height, Color top, Color bottom ) {
        if ( height <= 0.5f )
            return top;
        return new GradientPaint(0, y, top, 0, y + height, bottom);
    }

    /**
     *  A four-stop glass fill: bright at the top, dimming to a hard break just above the middle,
     *  then a jump back up and a gentle darkening to the bottom edge.
     *
     * @param y      the top edge, in component pixels
     * @param height how tall the shape is, in component pixels
     * @param base   the colour the surface is nominally painted in
     * @return the paint to fill with
     */
    static Paint gloss( float y, float height, Color base ) {
        if ( height <= 1f )
            return base;
        return new java.awt.LinearGradientPaint(
            new java.awt.geom.Point2D.Float(0, y),
            new java.awt.geom.Point2D.Float(0, y + height),
            new float[]{ 0f, 0.479f, 0.48f, 1f },
            new Color[]{
                Shades.lighter(base, 0.45),
                Shades.lighter(base, 0.16),
                base,
                Shades.darker(base, 0.14)
            }
        );
    }

    /**
     *  A solid arrow head, centred on a point.
     *
     * @param cx        the centre, horizontally, in component pixels
     * @param cy        the centre, vertically, in component pixels
     * @param halfSpan  half the width across the arrow's base, in component pixels
     * @param halfDepth half the distance from base to tip, in component pixels
     * @param direction which way it points
     * @return the arrow as a closed path
     */
    static Path2D.Float arrow( float cx, float cy, float halfSpan, float halfDepth, Direction direction ) {
        Path2D.Float path = new Path2D.Float();
        switch ( direction ) {
            case UP:
                path.moveTo(cx - halfSpan, cy + halfDepth);
                path.lineTo(cx,            cy - halfDepth);
                path.lineTo(cx + halfSpan, cy + halfDepth);
                break;
            case DOWN:
                path.moveTo(cx - halfSpan, cy - halfDepth);
                path.lineTo(cx,            cy + halfDepth);
                path.lineTo(cx + halfSpan, cy - halfDepth);
                break;
            case LEFT:
                path.moveTo(cx + halfDepth, cy - halfSpan);
                path.lineTo(cx - halfDepth, cy);
                path.lineTo(cx + halfDepth, cy + halfSpan);
                break;
            case RIGHT:
            default:
                path.moveTo(cx - halfDepth, cy - halfSpan);
                path.lineTo(cx + halfDepth, cy);
                path.lineTo(cx - halfDepth, cy + halfSpan);
                break;
        }
        path.closePath();
        return path;
    }

    /**
     *  A tick, drawn as an open path so the caller decides how thick it is.
     *
     * @param x the left edge of the box the tick sits in, in component pixels
     * @param y the top edge of that box, in component pixels
     * @param w the box width, in component pixels
     * @param h the box height, in component pixels
     * @return the tick as an open path
     */
    static Path2D.Float tick( float x, float y, float w, float h ) {
        Path2D.Float path = new Path2D.Float();
        path.moveTo(x + w * 0.22f, y + h * 0.52f);
        path.lineTo(x + w * 0.43f, y + h * 0.73f);
        path.lineTo(x + w * 0.78f, y + h * 0.30f);
        return path;
    }
}
