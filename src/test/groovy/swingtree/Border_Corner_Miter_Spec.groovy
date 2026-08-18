package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Timeout
import spock.lang.Title
import swingtree.components.JBox
import swingtree.style.ComponentExtension
import swingtree.threading.EventProcessor
import utility.Utility

import java.awt.image.BufferedImage
import java.util.concurrent.TimeUnit

@Title("Border Corner Miter Joints")
@Narrative('''

    A SwingTree border has four sides, and each of them may be given its own
    colour and its own thickness. Where two sides meet, one colour has to stop
    and the other has to start, and the line along which that happens is called
    a **miter joint**. Everybody has seen one: it is the diagonal seam in the
    corner of a picture frame.

    A miter joint belongs to its corner. Which pixels of the top-left corner are
    painted in the top colour and which in the left colour is a question about
    the two border widths meeting there and about nothing else. So making a
    component wider has to leave its corners exactly as they were and lengthen
    only the straight stretches of border between them. Where that fails,
    dragging a window wider quietly redraws the corner of every component in it.

    Concretely, four equally thick sides put the miter joint on the corner's diagonal,
    which cuts a square component into four triangles, and cuts a wide one into
    two triangles at the left and right ends and two trapeziums along the top
    and bottom.

    The scenarios below pin that down without looking at how any of it is
    computed. They paint ordinary styled components through the ordinary paint
    pipeline, and then ask three separate questions of the pixels that come out:

    - **Does a corner change when the component's proportions change?** It must
      not: the two corners are compared against each other, pixel for pixel.
    - **Do two equally thick sides get equal halves of the corner between
      them?** They must, since neither has the better claim to it. Here the
      pixels of each colour are counted and the counts compared.
    - **Does a border of four colours cover the same pixels a border of one
      colour covers?** It must, or a miter joint has a gap running along it. Here the
      two are painted separately and compared on coverage alone.

    The four garish colours used throughout are a measuring instrument rather
    than a design. With every side painted in a colour no other side uses, each
    pixel says out loud which side painted it.

''')
@Subject([UI, SwingTree])
@Timeout(value = 90, unit = TimeUnit.SECONDS)
class Border_Corner_Miter_Spec extends Specification
{
    /** Four maximally distinguishable colours, so that a pixel names the side which painted it. */
    private static final String TOP_COLOR    = "#ff0000"
    private static final String RIGHT_COLOR  = "#00ff00"
    private static final String BOTTOM_COLOR = "#0000ff"
    private static final String LEFT_COLOR   = "#00ffff"

    def setup() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
        SwingTree.get().setUiScaleFactor(1f)
        ComponentExtension.updateAllCachesFromLibraryConfig() // Every scenario starts with empty caches.
    }

    def cleanup() {
        SwingTree.clear()
    }

    /**
     *  How many pixels of the given image one of the four border colours painted. Only pixels
     *  it covered completely are counted: a pixel the miter joint runs through is painted partly by
     *  each of two sides, and would otherwise be counted once for both of them.
     */
    private static int pixelsPaintedIn( BufferedImage image, String color ) {
        int count = 0
        for ( int y = 0; y < image.height; y++ )
            for ( int x = 0; x < image.width; x++ ) {
                int pixel = image.getRGB(x, y)
                if ( ((pixel >>> 24) & 0xff) < 250 )
                    continue
                int red = (pixel >> 16) & 0xff, green = (pixel >> 8) & 0xff, blue = pixel & 0xff
                boolean matches
                switch ( color ) {
                    case TOP_COLOR   : matches = red > 200 && green < 100 && blue < 100; break
                    case RIGHT_COLOR : matches = red < 100 && green > 200 && blue < 100; break
                    case BOTTOM_COLOR: matches = red < 100 && green < 100 && blue > 200; break
                    case LEFT_COLOR  : matches = red < 100 && green > 200 && blue > 200; break
                    default          : matches = false
                }
                if ( matches )
                    count++
            }
        return count
    }

    def 'A corner of a multi colored border is unaffected by the proportions of the component. (#description)'(
        String description, int width, int height, Closure styler
    ) {
        reportInfo """
            A component of 240 by 240 pixels is the yardstick here, and every row of the
            table below is that very same style painted at some other size. What gets
            compared are the **corners**: the top-left 40 by 40 pixels of the component
            under test against the top-left 40 by 40 pixels of the yardstick, and the
            same for the other three corners.

            Making a component wider adds pixels to its top and bottom sides, and making
            it taller adds pixels to its left and right sides. Neither is a reason for a
            corner to be painted differently, so not one pixel of a corner may change
            hands from one side to the other.

            Rounded corners are what make this a real question. Where a corner is square,
            the miter joint between the two sides lies wholly inside it and there is nothing to
            argue about. Where it is rounded, the border curves away from the corner
            point, so the miter joint has to reach further inwards to cross that curve.
            Reaching *towards the middle of the component* is the tempting way to make it
            reach far enough, and it is the wrong one: from the corner of a wide
            component, the middle lies in quite a different direction than it does from
            the corner of a square one. Every row of this table would then come out
            different from every other.
        """
        given : """
            A way of comparing two equally sized images: the number of pixels which differ by
            more than a quarter of full scale in any one channel, alpha included.

            A quarter, because the one thing two JDKs do not agree on to the last bit is how
            a pixel the miter joint runs through is divided between the two sides sharing it. That
            fraction is the rasterizer's business, and JDK 8 rounds it differently than JDK 9
            and later do — by up to a sixth of full scale along a diagonal seam. A pixel which
            genuinely changed hands is not a rounding difference: it trades one of the four
            measuring colours for another, and those are the full 255 apart.
        """
            var pixelsPaintedDifferentlyIn = { BufferedImage a, BufferedImage b ->
                int count = 0
                for ( int y = 0; y < a.height; y++ )
                    for ( int x = 0; x < a.width; x++ )
                        for ( int channelShift : [0, 8, 16, 24] ) // blue, green, red and alpha
                            if ( Math.abs(
                                    ((a.getRGB(x, y) >> channelShift) & 0xff) -
                                    ((b.getRGB(x, y) >> channelShift) & 0xff)
                                 ) > 64 ) {
                                count++
                                break
                            }
                return count
            }
        and : 'The yardstick, which is the style of this row painted onto a 240 by 240 component.'
            var yardstickBox =
                        UI.box()
                        .withStyle( conf -> styler(conf) )
                        .get(JBox)
            yardstickBox.setSize(240, 240)
            var yardstick = Utility.renderSingleComponent(yardstickBox)
            assert yardstick.width == 240 && yardstick.height == 240

        when : 'The very same style is painted onto a component of the size under test:'
            var candidateBox =
                        UI.box()
                        .withStyle( conf -> styler(conf) )
                        .get(JBox)
            candidateBox.setSize(width, height)
            var candidate = Utility.renderSingleComponent(candidateBox)
            assert candidate.width == width && candidate.height == height

        and : """
            And the four corner squares are cut out of both images. A corner radius reaches
            half its own value inwards, so squares of 40 by 40 pixels hold the entire curve
            of even the roundest style in the table below (a radius of 34, reaching 17 pixels
            in), plus a stretch of the straight border past the end of it.
        """
            int cornerSize = 40
            var yardstickTopLeft     = yardstick.getSubimage(0,                0,                cornerSize, cornerSize)
            var yardstickTopRight    = yardstick.getSubimage(240 - cornerSize, 0,                cornerSize, cornerSize)
            var yardstickBottomLeft  = yardstick.getSubimage(0,                240 - cornerSize, cornerSize, cornerSize)
            var yardstickBottomRight = yardstick.getSubimage(240 - cornerSize, 240 - cornerSize, cornerSize, cornerSize)
            var candidateTopLeft     = candidate.getSubimage(0,                  0,                   cornerSize, cornerSize)
            var candidateTopRight    = candidate.getSubimage(width - cornerSize, 0,                   cornerSize, cornerSize)
            var candidateBottomLeft  = candidate.getSubimage(0,                  height - cornerSize, cornerSize, cornerSize)
            var candidateBottomRight = candidate.getSubimage(width - cornerSize, height - cornerSize, cornerSize, cornerSize)

        then : 'Not one pixel of any of the four corners is painted differently from the yardstick:'
            pixelsPaintedDifferentlyIn(candidateTopLeft,     yardstickTopLeft    ) == 0
            pixelsPaintedDifferentlyIn(candidateTopRight,    yardstickTopRight   ) == 0
            pixelsPaintedDifferentlyIn(candidateBottomLeft,  yardstickBottomLeft ) == 0
            pixelsPaintedDifferentlyIn(candidateBottomRight, yardstickBottomRight) == 0

        where :
            description                           | width | height | styler
            "the yardstick compared to itself"    | 240   | 240    | { it.borderRadius(24).borderWidths(10, 10, 10, 10)
                                                                         .borderColors(TOP_COLOR, RIGHT_COLOR, BOTTOM_COLOR, LEFT_COLOR) }
            "a wide component"                    | 640   | 240    | { it.borderRadius(24).borderWidths(10, 10, 10, 10)
                                                                         .borderColors(TOP_COLOR, RIGHT_COLOR, BOTTOM_COLOR, LEFT_COLOR) }
            "a tall component"                    | 240   | 640    | { it.borderRadius(24).borderWidths(10, 10, 10, 10)
                                                                         .borderColors(TOP_COLOR, RIGHT_COLOR, BOTTOM_COLOR, LEFT_COLOR) }
            "a very wide, flat component"         | 900   | 160    | { it.borderRadius(24).borderWidths(10, 10, 10, 10)
                                                                         .borderColors(TOP_COLOR, RIGHT_COLOR, BOTTOM_COLOR, LEFT_COLOR) }
            "a very tall, narrow component"       | 160   | 900    | { it.borderRadius(24).borderWidths(10, 10, 10, 10)
                                                                         .borderColors(TOP_COLOR, RIGHT_COLOR, BOTTOM_COLOR, LEFT_COLOR) }
            "a wide component, thin border"       | 500   | 200    | { it.borderRadius(30).borderWidths(2, 2, 2, 2)
                                                                         .borderColors(TOP_COLOR, RIGHT_COLOR, BOTTOM_COLOR, LEFT_COLOR) }
            "a wide component, thick border"      | 500   | 200    | { it.borderRadius(30).borderWidths(22, 22, 22, 22)
                                                                         .borderColors(TOP_COLOR, RIGHT_COLOR, BOTTOM_COLOR, LEFT_COLOR) }
            "square corners, no rounding"         | 500   | 200    | { it.borderRadius(0).borderWidths(12, 12, 12, 12)
                                                                         .borderColors(TOP_COLOR, RIGHT_COLOR, BOTTOM_COLOR, LEFT_COLOR) }
            "a barely rounded corner"             | 500   | 200    | { it.borderRadius(6).borderWidths(12, 12, 12, 12)
                                                                         .borderColors(TOP_COLOR, RIGHT_COLOR, BOTTOM_COLOR, LEFT_COLOR) }
            "sides of four different thicknesses" | 620   | 180    | { it.borderRadius(20).borderWidths(4, 9, 16, 25)
                                                                         .borderColors(TOP_COLOR, RIGHT_COLOR, BOTTOM_COLOR, LEFT_COLOR) }
            "a margin around the border"          | 620   | 180    | { it.borderRadius(20).margin(12).borderWidths(10, 10, 10, 10)
                                                                         .borderColors(TOP_COLOR, RIGHT_COLOR, BOTTOM_COLOR, LEFT_COLOR) }
            "a different radius per corner"       | 620   | 180    | { it.borderRadiusAt(UI.Corner.TOP_LEFT,     4,  4 )
                                                                         .borderRadiusAt(UI.Corner.TOP_RIGHT,    14, 14)
                                                                         .borderRadiusAt(UI.Corner.BOTTOM_LEFT,  24, 24)
                                                                         .borderRadiusAt(UI.Corner.BOTTOM_RIGHT, 34, 34)
                                                                         .borderWidths(8, 8, 8, 8)
                                                                         .borderColors(TOP_COLOR, RIGHT_COLOR, BOTTOM_COLOR, LEFT_COLOR) }
    }

    def 'Two equally thick sides receive equal halves of the corner they share. (#description)'(
        String description, int width, int height, float scale
    ) {
        reportInfo """
            When the two sides meeting at a corner are equally thick, neither has a claim
            on more of that corner than the other, so the miter joint between them is the
            corner's own diagonal. That is something the pixels can be asked about
            directly: count how many of them each of the two sides painted, and the two
            counts have to agree — give or take the couple of pixels sitting squarely on
            the diagonal itself, which the two sides divide between them and which the
            rasterizers of different JDKs therefore hand out slightly differently.

            This is the scenario which says out loud what shape the four regions are.
            Equal halves at all four corners of a component 900 pixels wide and 160 tall
            leaves only one possibility for what lies between those corners: the left and
            right sides are triangles, and the top and bottom are trapeziums stretching
            the long way across. A miter joint aimed at the middle of the component instead
            would hand the top and bottom the lion's share of every corner and leave the
            left and right sides slivers, and the counts below would be nowhere near
            equal.

            The style is chosen to make that difference as plain as it can be: a generous
            corner radius with a thin border. The wider the curve and the thinner the
            border, the further the curve sweeps away from the corner point and the
            longer the stretch of it the miter joint has to cross — so the more of the corner
            there is for the two sides to disagree over. A thick border on a tight radius
            hides the question, because there the curve is over and done with before the
            two sides have finished meeting.
        """
        given : """
            A display scale, of the kind a high resolution screen imposes. It multiplies
            every length in the style, so it is what decides whether the border widths
            land on whole pixels or between two of them.
        """
            SwingTree.get().setUiScaleFactor(scale)
        and : 'A component whose four sides are equally thick, each painted in its own colour.'
            var box =
                        UI.box()
                        .withStyle( conf -> conf
                            .borderRadius(56)
                            .borderWidths(6, 6, 6, 6)
                            .borderColors(TOP_COLOR, RIGHT_COLOR, BOTTOM_COLOR, LEFT_COLOR)
                        )
                        .get(JBox)
            box.setSize(width, height)
            var painted = Utility.renderSingleComponent(box)
            assert painted.width == width && painted.height == height

        and : """
            Its four corner squares, 40 by 40 pixels each. A corner radius reaches half its
            own value inwards, so 40 holds the entire curve of the radius of 56 used here,
            which reaches 28 pixels in, plus a stretch of the straight border past its end.
        """
            int cornerSize = 40
            var topLeftCorner     = painted.getSubimage(0,                  0,                   cornerSize, cornerSize)
            var topRightCorner    = painted.getSubimage(width - cornerSize, 0,                   cornerSize, cornerSize)
            var bottomLeftCorner  = painted.getSubimage(0,                  height - cornerSize, cornerSize, cornerSize)
            var bottomRightCorner = painted.getSubimage(width - cornerSize, height - cornerSize, cornerSize, cornerSize)

        when : 'In each corner we count the pixels painted by each of the two sides meeting there:'
            int topPixelsInTopLeft        = pixelsPaintedIn(topLeftCorner,     TOP_COLOR   )
            int leftPixelsInTopLeft       = pixelsPaintedIn(topLeftCorner,     LEFT_COLOR  )
            int topPixelsInTopRight       = pixelsPaintedIn(topRightCorner,    TOP_COLOR   )
            int rightPixelsInTopRight     = pixelsPaintedIn(topRightCorner,    RIGHT_COLOR )
            int bottomPixelsInBottomLeft  = pixelsPaintedIn(bottomLeftCorner,  BOTTOM_COLOR)
            int leftPixelsInBottomLeft    = pixelsPaintedIn(bottomLeftCorner,  LEFT_COLOR  )
            int bottomPixelsInBottomRight = pixelsPaintedIn(bottomRightCorner, BOTTOM_COLOR)
            int rightPixelsInBottomRight  = pixelsPaintedIn(bottomRightCorner, RIGHT_COLOR )

        then : 'The two sides sharing a corner painted equally much of it, give or take the few pixels on the diagonal:'
            Math.abs( topPixelsInTopLeft        - leftPixelsInTopLeft      ) <= 3
            Math.abs( topPixelsInTopRight       - rightPixelsInTopRight    ) <= 3
            Math.abs( bottomPixelsInBottomLeft  - leftPixelsInBottomLeft   ) <= 3
            Math.abs( bottomPixelsInBottomRight - rightPixelsInBottomRight ) <= 3

        and : 'And each of them painted a real share of it, so that the comparisons above are not pairs of zeroes:'
            topPixelsInTopLeft  > 50
            leftPixelsInTopLeft > 50

        where :
            description                           | width | height | scale
            "a square component"                  | 240   | 240    | 1f
            "a wide component"                    | 640   | 240    | 1f
            "a tall component"                    | 240   | 640    | 1f
            "a very wide flat component"          | 900   | 160    | 1f
            "a very tall narrow one"              | 160   | 900    | 1f
            "a wide component on a 1.5x display"  | 900   | 200    | 1.5f
            "a wide component on a 1.25x display" | 700   | 220    | 1.25f
    }

    def 'A border of four colors is painted as solidly as a border of one color. (#description)'(
        String description, int width, int height, float scale, Closure geometry
    ) {
        reportInfo """
            Giving the four sides four different colours changes which colour lands
            where. It must not change *whether* anything lands there: a border made of
            four colours has to cover exactly the border a single colour covers, with no
            thread of background showing through along a miter joint.

            So the same component is painted twice, once with four colours and once with
            one, and the two are compared on their alpha channel alone. Alpha is how much
            of a pixel got painted at all, from nothing to fully — so comparing on it,
            and on nothing else, asks purely about coverage and leaves the question of
            which colour arrived deliberately out of it.

            The comparison allows the four coloured border to fall a quarter short along
            a miter joint, and no further. A quarter, because a miter joint is a diagonal
            drawn across a grid of square pixels, and the pixels it passes through belong to both sides
            at once. Each side covers part of such a pixel and paints it only that much,
            which is how a diagonal edge is kept from looking like a staircase — anti
            aliasing. Two half covered paints laid over one another come to three quarters
            rather than to one, so those pixels end up a hairline lighter than their
            surroundings. That is the ordinary price of the technique.

            Three sides can also meet in a single point, and there the price is higher. It
            happens where two opposite border widths together exceed the box they are drawn
            in, which leaves no interior between them: the seam separating those two then
            runs *through* the border instead of through the interior, and each of its two
            ends lands on the seam of a third side. Three third-covered paints laid over
            one another come to a little over seven tenths, so such a point can fall short
            by three tenths rather than a quarter. There is at most one of them per pair of
            opposite sides, so at most four in a component, and the count below holds them
            to that.

            A **gap** is neither of those, and it is what this scenario is on watch for.
            Two sides which disagree about where their shared miter joint lies leave a thread of
            pixels that neither of them paints at all — hundreds of them, running the
            length of the miter joint, and losing coverage by far more than a third.

            Display scales that put the border widths *between* whole pixels are where
            such a disagreement is likely, which is why they are in the table below. A
            miter joint drawn at a rounded off position and a miter joint drawn at the true one part
            company by well under a pixel — invisible as a shift, and glaring as a gap.
        """
        given : """
            A display scale, of the kind a high resolution screen imposes. It multiplies
            every length in the style, so it is what decides whether the border widths
            land on whole pixels or between two of them.
        """
            SwingTree.get().setUiScaleFactor(scale)
        and : 'The component painted with a different colour on each of its four sides.'
            var fourColoredBox =
                        UI.box()
                        .withStyle( conf -> geometry(conf)
                            .borderColors(TOP_COLOR, RIGHT_COLOR, BOTTOM_COLOR, LEFT_COLOR)
                        )
                        .get(JBox)
            fourColoredBox.setSize(width, height)
            var fourColors = Utility.renderSingleComponent(fourColoredBox)

        and : 'And the very same component painted in a single colour on all four sides.'
            var oneColoredBox =
                        UI.box()
                        .withStyle( conf -> geometry(conf)
                            .borderColors(TOP_COLOR, TOP_COLOR, TOP_COLOR, TOP_COLOR)
                        )
                        .get(JBox)
            oneColoredBox.setSize(width, height)
            var oneColor = Utility.renderSingleComponent(oneColoredBox)

        when : """
            Both images are walked pixel by pixel, and of every pixel only the alpha channel
            is read, which is how much of it got painted at all. Wherever the four coloured
            border falls short of the single coloured one, that difference is a loss of
            coverage.
        """
            int paintedPixels = 0
            int worstCoverageLoss = 0
            int pixelsLosingMoreThanAQuarter = 0
            for ( int y = 0; y < oneColor.height; y++ )
                for ( int x = 0; x < oneColor.width; x++ ) {
                    int coverageOfOneColor   = (oneColor.getRGB(x, y)   >>> 24) & 0xff
                    int coverageOfFourColors = (fourColors.getRGB(x, y) >>> 24) & 0xff
                    int coverageLost = coverageOfOneColor - coverageOfFourColors
                    if ( coverageOfOneColor > 0 )
                        paintedPixels++
                    if ( coverageLost > 64 ) // A quarter of full coverage.
                        pixelsLosingMoreThanAQuarter++
                    worstCoverageLoss = Math.max(worstCoverageLoss, coverageLost)
                }

        then : 'There was a painted border to compare against in the first place:'
            paintedPixels > 1000

        and : 'And not one of its pixels lost more coverage than three sides meeting in a point can account for:'
            worstCoverageLoss <= 76 // Three tenths of full coverage.

        and : 'And such points are all there is: no more of them than the four a component can have.'
            pixelsLosingMoreThanAQuarter <= 4

        where :
            description                          | width | height | scale | geometry
            "a wide rounded border"              | 400   | 140    | 1f    | { it.borderRadius(24).borderWidths(10, 10, 10, 10) }
            "a tall rounded border"              | 140   | 400    | 1f    | { it.borderRadius(24).borderWidths(10, 10, 10, 10) }
            "a square component"                 | 240   | 240    | 1f    | { it.borderRadius(24).borderWidths(10, 10, 10, 10) }
            "square corners"                     | 400   | 140    | 1f    | { it.borderWidths(10, 10, 10, 10) }
            "a thin border"                      | 400   | 140    | 1f    | { it.borderRadius(20).borderWidths(2, 2, 2, 2) }
            "a thick border"                     | 400   | 140    | 1f    | { it.borderRadius(30).borderWidths(24, 24, 24, 24) }
            "four thicknesses on a 5.5x display" | 286   | 165    | 5.5f  | { it.margin(4).padding(2).borderWidths(3, 6, 15, 14) }
            "three thicknesses on a 5.5x display"| 286   | 165    | 5.5f  | { it.margin(4, 6, 3, 12).padding(2).borderWidths(15, 6, 9, 15) }
            "four thicknesses on a 1.5x display" | 500   | 220    | 1.5f  | { it.borderRadius(18).margin(3).borderWidths(3, 6, 15, 14) }
            "equal thicknesses, 1.25x display"   | 500   | 220    | 1.25f | { it.borderRadius(18).borderWidths(7, 7, 7, 7) }
    }
}
