package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Timeout
import spock.lang.Title
import swingtree.api.laf.OptimizedShapeRendering

import java.awt.Color
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.Shape
import java.awt.geom.Ellipse2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.util.concurrent.TimeUnit

@Title("Filling a Shape Without Antialiasing All of It")
@Narrative('''

    A rasterizer asked to antialias a shape computes a coverage value for every pixel it
    touches, including the great majority which lie well inside the outline and come out
    fully covered. Over a rounded rectangle the size of a scroll bar's thumb that is a few
    hundred pixels of genuine curve and tens of thousands of pixels of arithmetic arriving
    at "opaque".

    `ShapeRendering.fill(..)` is what the SwingTree style engine fills its own surfaces
    with, and what a look and feel delegate painting its own chrome can use for the same
    reason - Swing offers no cache for a scroll bar thumb or a progress bar fill, so both
    are drawn from scratch on every repaint. It splits a rounded rectangle into three plain
    bands and four antialiased corner boxes, and fills a plain rectangle with no coverage
    arithmetic at all.

    The whole value of that rests on one promise, which is what the scenarios below are
    about: **it writes the pixels `Graphics2D.fill` writes**. Not similar ones. A seam
    between a band and a corner box that fell between two device pixels would be blended
    twice or left uncovered, and the difference would be a hairline down the middle of every
    control in a theme. So the shortcut is taken only when every cut lands on a whole device
    pixel, and is given up otherwise - under a shear, under a scale which puts a corner half
    way into a pixel, or for a shape which is neither of the two kinds it knows.

''')
@Subject([OptimizedShapeRendering])
@Timeout(value = 90, unit = TimeUnit.SECONDS)
class Shape_Rendering_Spec extends Specification
{
    /**
     *  Fills the given shape twice into two images of the same size, once through
     *  {@link Graphics2D#fill(Shape)} and once through {@link OptimizedShapeRendering#fill}, and
     *  reports how many pixels of the two differ.
     */
    private static int differingPixels(
        Shape shape, double scale, double offset, boolean antialiasing, boolean shear
    ) {
        var renderings = (0..1).collect { variant ->
            var image = new BufferedImage(700, 700, BufferedImage.TYPE_INT_ARGB)
            Graphics2D g = image.createGraphics()
            g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                antialiasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF
            )
            g.translate(offset, offset)
            g.scale(scale, scale)
            if ( shear ) g.shear(0.2d, 0d)
            g.setColor(new Color(20, 90, 200))
            if ( variant == 0 ) g.fill(shape) else OptimizedShapeRendering.fill(g, shape)
            g.dispose()
            return image
        }
        int differing = 0
        for ( int y in 0..<700 )
            for ( int x in 0..<700 )
                if ( renderings[0].getRGB(x, y) != renderings[1].getRGB(x, y) )
                    differing++
        return differing
    }

    def 'A shape filled through `ShapeRendering` looks exactly like one filled by Java2D.'(
        String description, Shape shape, double scale, double offset
    ) {
        reportInfo """
            The cases below are the ones which decide whether the split is sound rather than
            merely plausible: a thumb-shaped rounded rectangle far taller than it is wide, a
            radius large enough that the four corner boxes meet in the middle and the bands
            between them are empty, a plain integer rectangle, and a shape which is neither.

            Each of them is filled at six graphics scales and four sub-pixel offsets, because
            the scale and the offset are what decide whether a cut lands on a whole device
            pixel: at 1.25 a whole-numbered coordinate lands on a quarter pixel three times
            out of four, and an offset of half a pixel moves every cut of every case.
        """
        expect : 'Not one pixel of the whole 700 by 700 canvas differs.'
            differingPixels(shape, scale, offset, true, false) == 0

        where :
            description             | shape                                             | scale  | offset
            'a scroll bar thumb'    | new RoundRectangle2D.Float(3, 4, 53, 290, 8, 8)   | 1.0d   | 0d
            'a scroll bar thumb'    | new RoundRectangle2D.Float(3, 4, 53, 290, 8, 8)   | 1.25d  | 0.25d
            'a scroll bar thumb'    | new RoundRectangle2D.Float(3, 4, 53, 290, 8, 8)   | 1.5d   | 0.5d
            'a scroll bar thumb'    | new RoundRectangle2D.Float(3, 4, 53, 290, 8, 8)   | 2.0d   | 0.75d
            'a wide bar'            | new RoundRectangle2D.Float(0, 0, 200, 40, 16, 16) | 1.75d  | 0d
            'a wide bar'            | new RoundRectangle2D.Float(0, 0, 200, 40, 16, 16) | 2.3333d| 0.5d
            'a radius past the box' | new RoundRectangle2D.Float(2, 7, 120, 120, 400, 400)| 1.5d | 0.25d
            'a tiny glyph'          | new RoundRectangle2D.Float(1, 1, 7, 9, 5, 5)      | 1.0d   | 0d
            'a plain rectangle'     | new Rectangle(5, 5, 400, 300)                     | 1.0d   | 0d
            'a plain rectangle'     | new Rectangle(5, 5, 400, 300)                     | 1.25d  | 0.5d
            'a fractional rectangle'| new Rectangle2D.Double(5.5d, 5.25d, 400, 300)     | 1.0d   | 0d
            'neither of the two'    | new Ellipse2D.Float(10, 10, 300, 200)             | 1.5d   | 0.25d
    }

    def 'A sheared graphics gives up the split, because its bands would not be axis aligned.'()
    {
        reportInfo """
            The bands and the corner boxes are stated in user space, and the split only makes
            sense if a horizontal line in user space is still horizontal on the destination.
            A shear breaks that, so the shape is filled in one antialiased go instead - and
            the pixels still have to match, which is what this scenario checks.
        """
        expect :
            differingPixels(new RoundRectangle2D.Float(3, 4, 53, 290, 8, 8), 1.0d, 0d, true, true) == 0
    }

    def 'With antialiasing already off, filling through `ShapeRendering` changes nothing.'()
    {
        reportInfo """
            There is nothing to gain by splitting a shape whose edges are not being softened
            in the first place, so the shape goes straight to Java2D. The one thing worth
            pinning is that the hint is left as it was found: a caller which had switched
            antialiasing off does not get it back on.
        """
        given :
            var image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB)
            var g = image.createGraphics()
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
        when :
            OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(3, 4, 53, 90, 8, 8))
        then :
            g.getRenderingHint(RenderingHints.KEY_ANTIALIASING) == RenderingHints.VALUE_ANTIALIAS_OFF
        and : 'The pixels are the ones Java2D writes for the same shape without antialiasing.'
            differingPixels(new RoundRectangle2D.Float(3, 4, 53, 290, 8, 8), 1.0d, 0d, false, false) == 0
        cleanup :
            g.dispose()
    }
}
