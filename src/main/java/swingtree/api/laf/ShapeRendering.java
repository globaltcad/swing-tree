package swingtree.api.laf;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 *  Fills shapes the way the SwingTree style engine fills its own surfaces: antialiasing stays on
 *  wherever the outline actually curves, and is switched off everywhere else.
 *  <p>
 *  A rasterizer asked to antialias computes a coverage value for every pixel of a shape, including
 *  the great majority which lie well inside it and come out fully covered. Over a rounded
 *  rectangle the size of a scroll bar's thumb or a progress bar's fill, that is a few hundred
 *  pixels of genuine curve and tens of thousands of pixels of arithmetic arriving at "opaque". A
 *  fill without antialiasing writes those directly.
 *  <p>
 *  This is worth having in a look and feel because a {@link javax.swing.plaf.ComponentUI} draws
 *  the same rounded chrome on every repaint, and Swing offers no cache for it. The one condition
 *  under which the shortcut is taken is that every cut between an antialiased part and a
 *  plain one lands on a whole device pixel, which is what makes the result identical to
 *  {@link Graphics2D#fill(Shape)} rather than merely similar - a cut falling between two pixels
 *  would leave a seam blended twice or not at all. Anything else, a fractional rectangle or a
 *  sheared transform among them, is filled in one antialiased go.
 *
 *  @see SwingTreeStyledComponentUI
 */
public final class ShapeRendering
{
    /**
     *  The smallest area, in device pixels, for which splitting a rounded fill into parts pays.
     */
    private static final int SMALLEST_AREA_WORTH_SPLITTING = 32768;

    private ShapeRendering() {}

    /**
     *  Fills a shape with the graphics context's current paint, in as few antialiased pixels as
     *  the shape allows, writing exactly the pixels {@link Graphics2D#fill(Shape)} would write.
     *  <p>
     *  Two kinds of shape get a faster treatment:
     *  <ul>
     *      <li>A {@link Rectangle} is integer valued by its very type and so has no soft edge
     *          anywhere. Only the transform could still put one between two pixels, which is
     *          why that is checked before filling it in one go, without antialiasing.</li>
     *      <li>A {@link RoundRectangle2D} curves only inside its four corner boxes, so it is
     *          filled as antialiasing-free bands plus antialiased corners.</li>
     *  </ul>
     *  Anything else, like a fractional {@link Rectangle2D} or a rotated transform, keeps
     *  antialiasing and is filled in one go.
     *
     * @param g2d the graphics context to fill on, whose paint, clip and transform are all used
     *            and none of which are changed by this call
     * @param shape the shape to fill
     */
    public static void fill( final Graphics2D g2d, final Shape shape ) {
        if ( !RenderingHints.VALUE_ANTIALIAS_ON.equals(g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING)) ) {
            g2d.fill(shape); // Nothing to gain, antialiasing is already off.
            return;
        }
        final AffineTransform transform = g2d.getTransform();

        if ( shape instanceof Rectangle && _mapsOntoWholeDevicePixels(transform, (Rectangle) shape) ) {
            fillWithoutAntialiasing(g2d, shape);
            return;
        }
        if ( shape instanceof RoundRectangle2D && _fillRoundRectangleInParts(g2d, (RoundRectangle2D) shape, transform) )
            return;

        g2d.fill(shape);
    }

    /**
     *  Fills the given shapes with antialiasing switched off and then switches it back on.
     *
     * @param g2d the graphics context to fill on
     * @param shapes the shapes to fill, none of which has a soft edge worth computing
     */
    public static void fillWithoutAntialiasing( final Graphics2D g2d, final Shape... shapes ) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        try {
            for ( Shape shape : shapes )
                _fillOneWithoutAntialiasing(g2d, shape);
        } finally {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
    }

    /**
     *  Fills one shape, asking for a rectangle by the four numbers wherever the shape is one.
     *  <p>
     *  {@link Graphics2D#fill(Shape)} reaches the rasterizer through the general shape pipeline,
     *  which walks the outline and hands the destination one span per scanline: filling a scroll
     *  bar thumb's 1290-row band that way pushes 1290 rectangles at the X server.
     *  {@link Graphics#fillRect(int, int, int, int)} states the same region as one rectangle and
     *  the destination fills it in one operation. Measured on an accelerated surface, a 53x1290
     *  fill costs 24.5 microseconds through {@code fill} and 0.52 through {@code fillRect}.
     *  <p>
     *  Only whole-numbered coordinates can take that route, because {@code fillRect} has no other
     *  kind. A rectangle whose corners sit between two user-space pixels - which the caller may
     *  still have found acceptable, if the transform happens to scale them onto whole device ones
     *  - is filled the general way.
     *  <p>
     *  So is one carrying a gradient or a texture. Java2D reaches the single-rectangle operation
     *  only while the paint is one colour; under any other paint it turns {@code fillRect} back
     *  into a shape and walks it anyway, and the round trip measured as a small loss on the
     *  gradient-heavy presets.
     */
    private static void _fillOneWithoutAntialiasing( final Graphics2D g2d, final Shape shape ) {
        if ( shape instanceof Rectangle2D && g2d.getPaint() instanceof Color ) {
            final Rectangle2D rectangle = (Rectangle2D) shape;
            final double x = rectangle.getX(),     y = rectangle.getY();
            final double w = rectangle.getWidth(), h = rectangle.getHeight();
            if ( _isWhole(x) && _isWhole(y) && _isWhole(w) && _isWhole(h) ) {
                g2d.fillRect(
                    (int) Math.rint(x), (int) Math.rint(y),
                    (int) Math.rint(w), (int) Math.rint(h)
                );
                return;
            }
        }
        g2d.fill(shape);
    }

    /**
     *  Tries to fill a rounded rectangle as three antialiasing-free bands plus four antialiased
     *  corners, and reports whether it succeeded or not.
     *
     * @return {@code true} when the shape was filled, {@code false} when the caller must fill it.
     */
    private static boolean _fillRoundRectangleInParts(
        final Graphics2D       g2d,
        final RoundRectangle2D round,
        final AffineTransform  transform
    ) {
        if ( transform.getShearX() != 0 || transform.getShearY() != 0 )
            return false; // The bands would not be axis aligned in device space.

        final double x = round.getX(),     y = round.getY();
        final double w = round.getWidth(), h = round.getHeight();
        if ( w <= 0 || h <= 0 )
            return false;

        final double scaleX = transform.getScaleX(), translateX = transform.getTranslateX();
        final double scaleY = transform.getScaleY(), translateY = transform.getTranslateY();

        final double deviceArea = Math.abs(w * scaleX * h * scaleY);
        if ( deviceArea < SMALLEST_AREA_WORTH_SPLITTING )
            return false; // The split would not pay for its six extra fills.

        // How far the curvature reaches in from each side, which is half of the arc:
        final double arcW = Math.min(Math.abs(round.getArcWidth()),  w) / 2d;
        final double arcH = Math.min(Math.abs(round.getArcHeight()), h) / 2d;
        if ( arcW <= 0 || arcH <= 0 )
            return false; // Not actually rounded; an undivided fill of it is already optimal.

        final double[] cutX = { x, x + arcW, x + w - arcW, x + w };
        final double[] cutY = { y, y + arcH, y + h - arcH, y + h };
        if ( !_allCutsAreWholeDevicePixels(cutX, scaleX, translateX) )
            return false;
        if ( !_allCutsAreWholeDevicePixels(cutY, scaleY, translateY) )
            return false;

        // The bands, which hold nearly all of the area and none of the curvature.
        // A band is empty when the arc spans the full width or height, which fills nothing.
        fillWithoutAntialiasing(g2d,
            new Rectangle2D.Double(cutX[0], cutY[1], w,                 cutY[2] - cutY[1]), // Between the corners.
            new Rectangle2D.Double(cutX[1], cutY[0], cutX[2] - cutX[1], arcH),              // Above them,
            new Rectangle2D.Double(cutX[1], cutY[2], cutX[2] - cutX[1], arcH)               // and below them.
        );
        // And then the four corner boxes, each filled with the whole shape clipped to it, so
        // that the curve is rasterized by exactly the code which would have drawn it anyway.
        for ( int corner = 0; corner < 4; corner++ ) {
            final double cornerX = ( corner == 1 || corner == 3 ) ? cutX[2] : cutX[0];
            final double cornerY = ( corner >= 2 )                ? cutY[2] : cutY[0];
            final Graphics2D cornerG2d = (Graphics2D) g2d.create();
            try {
                cornerG2d.clip(new Rectangle2D.Double(cornerX, cornerY, arcW, arcH));
                cornerG2d.fill(round);
            } finally {
                cornerG2d.dispose();
            }
        }
        return true;
    }

    /**
     *  Whether every cut line lands on a whole device pixel. A cut between two pixels would
     *  make the band and the corner clip meeting there disagree about which pixel they own,
     *  leaving a seam that is either blended twice or not covered at all.
     */
    private static boolean _allCutsAreWholeDevicePixels(
        final double[] cuts, final double scale, final double translate
    ) {
        for ( double cut : cuts )
            if ( !_isWhole(cut * scale + translate) )
                return false;
        return true;
    }

    /**
     *  Whether the given transform turns the corners of the given integer rectangle
     *  into whole device pixels, which requires it to be free of rotation and shear
     *  and to scale the corners onto integers.
     */
    private static boolean _mapsOntoWholeDevicePixels( final AffineTransform transform, final Rectangle rectangle ) {
        if ( transform.getShearX() != 0 || transform.getShearY() != 0 )
            return false;
        return _isWhole(transform.getScaleX() * rectangle.x                     + transform.getTranslateX())
            && _isWhole(transform.getScaleY() * rectangle.y                     + transform.getTranslateY())
            && _isWhole(transform.getScaleX() * (rectangle.x + rectangle.width ) + transform.getTranslateX())
            && _isWhole(transform.getScaleY() * (rectangle.y + rectangle.height) + transform.getTranslateY());
    }

    private static boolean _isWhole( final double value ) {
        return Math.abs(value - Math.rint(value)) < 1e-6;
    }
}
