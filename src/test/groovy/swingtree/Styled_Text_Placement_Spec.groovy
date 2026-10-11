package swingtree.style

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.SwingTree
import swingtree.UI
import swingtree.components.JBox
import swingtree.threading.EventProcessor
import utility.SwingTreeTestConfigurator
import utility.Utility

import javax.swing.*
import java.awt.Color
import java.awt.Font
import java.awt.Rectangle
import java.awt.image.BufferedImage

@Title("Where Styled Text Is Painted")
@Narrative('''

    With `withStyle(it -> it.text(t -> t.content("...")))` you can paint text onto
    any component. SwingTree first breaks the text into lines. Then it decides
    where each line goes, and that is what this specification is about.

    The text is painted inside the *content area* of the component, which is the
    component minus its margin, its border and its padding. Every component in this
    specification has a padding of 10 pixels on every side and no margin and no border,
    so its content area starts 10 pixels from each edge of the component.

    You choose where the text sits in the content area with `placement(UI.Placement)`.
    A `UI.Placement` combines a vertical position (top, centre or bottom) with a
    horizontal position (left, centre or right). For example, `TOP_LEFT` puts the text
    into the top left corner, `LEFT` puts it on the left side halfway down the content
    area, and `CENTER` puts it into the middle in both directions.

    The scenarios check the rendered pixels, but they never compare against a reference
    image. A reference image breaks whenever a JDK version or an operating system draws
    a glyph slightly differently. Instead, every scenario paints each line of text in its
    own colour, finds the smallest rectangle around the pixels of that colour, and
    compares the rectangles of different lines with each other. Glyph edges are smoothed
    with partly transparent pixels, so only pixels which are at least 160 out of 255
    opaque count as painted in a colour.

''')
@Subject([TextConf, StyledString])
class Styled_Text_Placement_Spec extends Specification
{
    static final Color RED     = new Color(220,   0,   0)
    static final Color GREEN   = new Color(  0, 150,   0)
    static final Color BLUE    = new Color(  0,   0, 220)
    static final Color MAGENTA = new Color(200,   0, 200)
    static final Color ORANGE  = new Color(230, 140,   0)

    def setupSpec() {
        SwingTree.initializeUsing(SwingTreeTestConfigurator.get())
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
        SwingTree.get().setUiScaleFactor(1f)
    }

    def cleanupSpec() {
        SwingTree.clear()
    }

    def setup() {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())
    }

    def 'When the text is taller than the content area, SwingTree paints only the lines which fit completely, and with `placement(#placement)` these are #paintedLines.'(
        UI.Placement placement, String paintedLines, List<Color> colorsOfPaintedLines
    ) {
        reportInfo """
            A text of five lines is placed into a content area which is only two and a half
            line heights tall. SwingTree never paints part of a line, so it paints two lines
            completely, and the remaining height of half a line stays empty. The vertical part
            of the placement decides which two of the five lines are painted:

            - A top placement (`TOP_LEFT`, `TOP`, `TOP_RIGHT`) paints the first two lines.
            - A bottom placement (`BOTTOM_LEFT`, `BOTTOM`, `BOTTOM_RIGHT`) paints the last two lines.
            - A vertically centred placement (`LEFT`, `CENTER`, `RIGHT`) paints the second
              and the third line.

            The rule for a vertically centred placement needs a closer look, so let us measure
            everything in line heights. The text is 5 line heights tall and the content area is
            2.5 line heights tall, so 2.5 line heights of text do not fit. SwingTree wants to
            leave out as much text at the top as at the bottom, so it plans to leave out the
            top 1.25 line heights of the text. It cannot leave out a quarter of a line, so it
            leaves out only the lines which lie completely inside those top 1.25 line heights.
            The first line ends 1 line height below the top of the text, so it is left out.
            The second line ends 2 line heights below the top of the text, so it does not lie
            completely inside the top 1.25 line heights, and painting starts with it.
            From there on, SwingTree paints one line after the other as long as the next line
            still fits completely into the content area. The second and the third line together
            are 2 line heights tall and fit into the 2.5 line heights. Adding the fourth line
            would make 3 line heights, which do not fit, so the fourth and the fifth line are
            not painted.

            In this scenario the placement is `${placement}`, so the painted lines are ${paintedLines}.
        """
        given : """
            We read the height of one line from the `FontMetrics` of the font we paint with,
            the "Dialog" font at size 20. The content area is made two and a half lines tall
            by giving the component a height of 2.5 line heights plus the 20 pixels of
            padding above and below the content area:
        """
            var lineHeight = new JBox().getFontMetrics(new Font(Font.DIALOG, Font.PLAIN, 20)).getHeight()
        and : 'Each of the five lines has its own colour, from red in the first line to orange in the last:'
            var box =
                    UI.box().withStyle( it -> it
                        .size(300, (int) (2.5 * lineHeight + 20))
                        .padding(10)
                        .text( t -> t
                            .placement(placement)
                            .wrapLines(false)
                            .font( f -> f.family(Font.DIALOG).size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),     "HHHH\n" ),
                                StyledString.of( f -> f.color(GREEN),   "HHHH\n" ),
                                StyledString.of( f -> f.color(BLUE),    "HHHH\n" ),
                                StyledString.of( f -> f.color(MAGENTA), "HHHH\n" ),
                                StyledString.of( f -> f.color(ORANGE),  "HHHH"   )
                            )
                        )
                    )
                    .get(JBox)

        when : 'We render the component and collect the colours which appear in the image:'
            var image = Utility.renderSingleComponent(box)
            var colorsInTheImage = [RED, GREEN, BLUE, MAGENTA, ORANGE].findAll { boundsOfPixelsPaintedIn(image, it) != null }

        then : 'Exactly the colours of the expected two lines appear:'
            colorsInTheImage == colorsOfPaintedLines

        where :
            placement                  | paintedLines                       || colorsOfPaintedLines
            UI.Placement.TOP_LEFT      | "the first and the second line"    || [RED,     GREEN  ]
            UI.Placement.TOP           | "the first and the second line"    || [RED,     GREEN  ]
            UI.Placement.TOP_RIGHT     | "the first and the second line"    || [RED,     GREEN  ]
            UI.Placement.LEFT          | "the second and the third line"    || [GREEN,   BLUE   ]
            UI.Placement.CENTER        | "the second and the third line"    || [GREEN,   BLUE   ]
            UI.Placement.RIGHT         | "the second and the third line"    || [GREEN,   BLUE   ]
            UI.Placement.BOTTOM_LEFT   | "the fourth and the fifth line"    || [MAGENTA, ORANGE ]
            UI.Placement.BOTTOM        | "the fourth and the fifth line"    || [MAGENTA, ORANGE ]
            UI.Placement.BOTTOM_RIGHT  | "the fourth and the fifth line"    || [MAGENTA, ORANGE ]
    }

    def 'With `placement(#placement)`, the left edge of every line of the text is on the left edge of the content area.'(
        UI.Placement placement
    ) {
        reportInfo """
            `TOP_LEFT`, `LEFT` and `BOTTOM_LEFT` all put the text on the left side of the
            content area. SwingTree starts every line, short or long, at the left edge of the
            content area. The lines therefore have equal left edges and, because they are of
            different lengths, different right edges.

            The left edge of the content area is 10 pixels from the left edge of the
            component, because the component has a padding of 10 pixels.
            The painted pixels of a line start up to 4 pixels further right than that,
            because the letter "H" does not start with a stroke: in this font it has
            a few empty pixel columns to the left of its first stroke.

            In this scenario the placement is `${placement}`.
        """
        given : """
            A component which is much wider than the longest line, with three lines of different
            lengths in three colours. Every line consists only of the letter "H", so that every
            line starts with the same glyph and the empty pixels before the first stroke are the
            same in every line:
        """
            var box =
                    UI.box().withStyle( it -> it
                        .size(400, 200)
                        .padding(10)
                        .text( t -> t
                            .placement(placement)
                            .wrapLines(false)
                            .font( f -> f.family(Font.DIALOG).size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),   "HH\n"       ),
                                StyledString.of( f -> f.color(GREEN), "HHHHHHHH\n" ),
                                StyledString.of( f -> f.color(BLUE),  "HHHHH"      )
                            )
                        )
                    )
                    .get(JBox)

        when : 'We render the component and find the left and the right edge of the pixels of every line:'
            var image = Utility.renderSingleComponent(box)
            var lines = [RED, GREEN, BLUE].collect { boundsOfPixelsPaintedIn(image, it) }
            var leftEdges  = lines.collect { it.x }
            var rightEdges = lines.collect { it.x + it.width }

        then : 'All lines start at the same pixel column, give or take one pixel of rounding:'
            leftEdges.max() - leftEdges.min() <= 1
        and : 'The lines end far apart, because they are of different lengths:'
            rightEdges.max() - rightEdges.min() > 20
        and : 'The lines start at the left edge of the content area plus the empty pixels before the first stroke of an "H":'
            leftEdges.min() >= 10
            leftEdges.min() <= 10 + 4

        where :
            placement << [UI.Placement.TOP_LEFT, UI.Placement.LEFT, UI.Placement.BOTTOM_LEFT]
    }

    def 'With `placement(#placement)`, the right edge of every line of the text is on the right edge of the content area.'(
        UI.Placement placement
    ) {
        reportInfo """
            `TOP_RIGHT`, `RIGHT` and `BOTTOM_RIGHT` all put the text on the right side of the
            content area. SwingTree ends every line, short or long, at the right edge of the
            content area. The lines therefore have equal right edges and, because they are of
            different lengths, different left edges.

            The component is 400 pixels wide and has a padding of 10 pixels, so the right edge
            of the content area is at 390 pixels. The painted pixels of a line end up to 4 pixels
            further left than that, because the letter "H" does not end with a stroke: in this
            font it has a few empty pixel columns to the right of its last stroke.

            In this scenario the placement is `${placement}`.
        """
        given : """
            A component which is much wider than the longest line, with three lines of different
            lengths in three colours. Every line consists only of the letter "H", so that every
            line ends with the same glyph and the empty pixels after the last stroke are the
            same in every line:
        """
            var box =
                    UI.box().withStyle( it -> it
                        .size(400, 200)
                        .padding(10)
                        .text( t -> t
                            .placement(placement)
                            .wrapLines(false)
                            .font( f -> f.family(Font.DIALOG).size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),   "HH\n"       ),
                                StyledString.of( f -> f.color(GREEN), "HHHHHHHH\n" ),
                                StyledString.of( f -> f.color(BLUE),  "HHHHH"      )
                            )
                        )
                    )
                    .get(JBox)

        when : 'We render the component and find the left and the right edge of the pixels of every line:'
            var image = Utility.renderSingleComponent(box)
            var lines = [RED, GREEN, BLUE].collect { boundsOfPixelsPaintedIn(image, it) }
            var leftEdges  = lines.collect { it.x }
            var rightEdges = lines.collect { it.x + it.width }

        then : 'All lines end at the same pixel column, give or take one pixel of rounding:'
            rightEdges.max() - rightEdges.min() <= 1
        and : 'The lines start far apart, because they are of different lengths:'
            leftEdges.max() - leftEdges.min() > 20
        and : 'The lines end at the right edge of the content area minus the empty pixels after the last stroke of an "H":'
            rightEdges.max() <= 390
            rightEdges.max() >= 390 - 4

        where :
            placement << [UI.Placement.TOP_RIGHT, UI.Placement.RIGHT, UI.Placement.BOTTOM_RIGHT]
    }

    def 'With `placement(#placement)`, the middle of every line of the text is on the vertical centre line of the content area.'(
        UI.Placement placement
    ) {
        reportInfo """
            `TOP`, `CENTER` and `BOTTOM` all centre the text horizontally. SwingTree places
            every line, short or long, so that the empty space between the left edge of the
            content area and the line is as wide as the empty space between the line and the
            right edge of the content area. The middle of every line is then on the vertical
            line through the middle of the content area.

            The component is 400 pixels wide and has a padding of 10 pixels on both sides,
            so the content area reaches from 10 to 390 pixels and its middle is at 200 pixels.

            In this scenario the placement is `${placement}`.
        """
        given : """
            A component which is much wider than the longest line, with three lines of different
            lengths in three colours. Every line consists only of the letter "H". The letter "H"
            is symmetric: it has as many empty pixel columns to the left of its first stroke as
            to the right of its last stroke. So the middle of the painted pixels of a line is
            also the middle of the line:
        """
            var box =
                    UI.box().withStyle( it -> it
                        .size(400, 200)
                        .padding(10)
                        .text( t -> t
                            .placement(placement)
                            .wrapLines(false)
                            .font( f -> f.family(Font.DIALOG).size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),   "HH\n"       ),
                                StyledString.of( f -> f.color(GREEN), "HHHHHHHH\n" ),
                                StyledString.of( f -> f.color(BLUE),  "HHHHH"      )
                            )
                        )
                    )
                    .get(JBox)

        when : 'We render the component and find the horizontal middle of the pixels of every line:'
            var image = Utility.renderSingleComponent(box)
            var lines = [RED, GREEN, BLUE].collect { boundsOfPixelsPaintedIn(image, it) }
            var leftEdges = lines.collect { it.x }
            var middles   = lines.collect { it.x + it.width / 2.0 }

        then : 'The middle of every line is at 200 pixels, give or take one and a half pixels of rounding:'
            middles.every { Math.abs(it - 200) <= 1.5 }
        and : 'The lines start far apart, because they are of different lengths:'
            leftEdges.max() - leftEdges.min() > 20

        where :
            placement << [UI.Placement.TOP, UI.Placement.CENTER, UI.Placement.BOTTOM]
    }

    def 'A top `placement` puts the text at the top of the content area, a bottom `placement` at the bottom, and `CENTER` leaves the same height free above and below the text.'()
    {
        reportInfo """
            The component in this scenario is 220 pixels tall and has a padding of 10 pixels,
            so its content area is 200 pixels tall. The text consists of two lines, so it is
            two line heights tall, and the content area has 200 pixels minus two line heights
            to spare. We call those spare pixels the free height.

            - With `TOP`, the first line starts at the top of the content area,
              and the whole free height lies below the text.
            - With `BOTTOM`, the last line ends at the bottom of the content area,
              and the whole free height lies above the text. Compared to `TOP`, the text
              is therefore moved down by the whole free height.
            - With `CENTER`, half of the free height lies above the text and the other half
              below it. Compared to `TOP`, the text is therefore moved down by half of the free height.

            The image does not show where a line starts, only where its painted pixels start,
            and the painted pixels of an "H" start a few pixels below the top of its line.
            But they start the same number of pixels below the top of the line in all three
            components. So when we subtract the top of the painted pixels in one component from
            the top of the painted pixels in another, those few pixels cancel out, and the
            result is the distance by which the line was moved.
        """
        given : 'We read the height of one line from the `FontMetrics` of the font we paint with:'
            var lineHeight = new JBox().getFontMetrics(new Font(Font.DIALOG, Font.PLAIN, 20)).getHeight()
        and : 'Three components which differ only in the vertical part of their placement, each with a red and a blue line:'
            var components = [UI.Placement.TOP, UI.Placement.CENTER, UI.Placement.BOTTOM].collect { placement ->
                    UI.box().withStyle( it -> it
                        .size(300, 220)
                        .padding(10)
                        .text( t -> t
                            .placement(placement)
                            .wrapLines(false)
                            .font( f -> f.family(Font.DIALOG).size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),  "HHHH\n" ),
                                StyledString.of( f -> f.color(BLUE), "HHHH"   )
                            )
                        )
                    )
                    .get(JBox)
                }

        when : 'We render the three components and find the top of the painted pixels of the red line in each of them:'
            var topOfRedPixels = components.collect { boundsOfPixelsPaintedIn(Utility.renderSingleComponent(it), RED).y }
            var topOfRedPixelsWithTop    = topOfRedPixels[0]
            var topOfRedPixelsWithCenter = topOfRedPixels[1]
            var topOfRedPixelsWithBottom = topOfRedPixels[2]
            var freeHeight = 200 - 2 * lineHeight

        then : """
            With `BOTTOM`, the red line is moved down by the free height compared to `TOP`.
            We allow 2 pixels of difference, because `FontMetrics` rounds the parts of a line
            height to whole pixels, while SwingTree lays out lines with fractional heights:
        """
            Math.abs((topOfRedPixelsWithBottom - topOfRedPixelsWithTop) - freeHeight) <= 2
        and : """
            With `CENTER`, the red line is moved down by half of the distance it is moved with `BOTTOM`.
            We allow 1 pixel of difference, because a fractional position is rounded to a pixel row:
        """
            Math.abs((topOfRedPixelsWithCenter - topOfRedPixelsWithTop) - (topOfRedPixelsWithBottom - topOfRedPixelsWithTop) / 2.0) <= 1
    }

    def 'An empty line between two lines of text is exactly as tall as the size of the font.'()
    {
        reportInfo """
            The text `"HHHH\\n\\nHHHH"` has an empty line between its two lines of "H".
            The height of a line which contains text comes from the font: how far its glyphs
            may reach above the baseline, which is the line the letters stand on, plus how far
            they may reach below it, plus the gap the font asks for between two lines.
            A line without text has no glyphs, so SwingTree gives it a height equal to the
            size of the font, which is 20 pixels in this scenario.

            We compare two texts: `"HHHH\\n\\nHHHH"` with the empty line and `"HHHH\\nHHHH"`
            without it. In both texts we measure the distance from the top of the painted
            pixels of the first line to the top of the painted pixels of the last line.
            The empty line makes this distance 20 pixels larger, give or take one pixel,
            because the positions of the lines are fractional and the pixels are not.
        """
        given : 'Two components which differ only in the empty line between the red and the blue line of their text:'
            var withEmptyLine =
                    UI.box().withStyle( it -> it
                        .size(300, 200)
                        .padding(10)
                        .text( t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family(Font.DIALOG).size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),  "HHHH\n\n" ),
                                StyledString.of( f -> f.color(BLUE), "HHHH"     )
                            )
                        )
                    )
                    .get(JBox)
            var withoutEmptyLine =
                    UI.box().withStyle( it -> it
                        .size(300, 200)
                        .padding(10)
                        .text( t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .font( f -> f.family(Font.DIALOG).size(20) )
                            .content(
                                StyledString.of( f -> f.color(RED),  "HHHH\n" ),
                                StyledString.of( f -> f.color(BLUE), "HHHH"   )
                            )
                        )
                    )
                    .get(JBox)

        when : 'We render both components and measure the distance from the top of the red pixels to the top of the blue pixels:'
            var imageWithEmptyLine    = Utility.renderSingleComponent(withEmptyLine)
            var imageWithoutEmptyLine = Utility.renderSingleComponent(withoutEmptyLine)
            var distanceWithEmptyLine    = boundsOfPixelsPaintedIn(imageWithEmptyLine, BLUE).y    - boundsOfPixelsPaintedIn(imageWithEmptyLine, RED).y
            var distanceWithoutEmptyLine = boundsOfPixelsPaintedIn(imageWithoutEmptyLine, BLUE).y - boundsOfPixelsPaintedIn(imageWithoutEmptyLine, RED).y

        then : 'The empty line adds 20 pixels to the distance, give or take one pixel of rounding:'
            Math.abs((distanceWithEmptyLine - distanceWithoutEmptyLine) - 20) <= 1
    }

    def 'A line break at the very end of a text does not add an empty line below the text.'()
    {
        reportInfo """
            The texts `"HHHH\\n"` and `"HHHH"` are painted at exactly the same place.
            SwingTree does not turn a line break at the very end of a text into an
            empty last line.

            We check this with `BOTTOM_LEFT`, which puts the last line of the text at the
            bottom of the content area. If the line break at the end added an empty line,
            that empty line would sit at the bottom of the content area, and it would push
            the line of "H" up by the height of an empty line, which is the font size of 20 pixels.
        """
        given : 'Two components with bottom-left placed text, which differ only in the line break at the end of their text:'
            var endingWithLineBreak =
                    UI.box().withStyle( it -> it
                        .size(300, 200)
                        .padding(10)
                        .text( t -> t
                            .placement(UI.Placement.BOTTOM_LEFT)
                            .font( f -> f.family(Font.DIALOG).size(20) )
                            .content( StyledString.of( f -> f.color(RED), "HHHH\n" ) )
                        )
                    )
                    .get(JBox)
            var endingWithoutLineBreak =
                    UI.box().withStyle( it -> it
                        .size(300, 200)
                        .padding(10)
                        .text( t -> t
                            .placement(UI.Placement.BOTTOM_LEFT)
                            .font( f -> f.family(Font.DIALOG).size(20) )
                            .content( StyledString.of( f -> f.color(RED), "HHHH" ) )
                        )
                    )
                    .get(JBox)

        when : 'We render both components and find the rectangle around the red pixels in each:'
            var redPixelsEndingWithLineBreak    = boundsOfPixelsPaintedIn(Utility.renderSingleComponent(endingWithLineBreak), RED)
            var redPixelsEndingWithoutLineBreak = boundsOfPixelsPaintedIn(Utility.renderSingleComponent(endingWithoutLineBreak), RED)

        then : 'Both rectangles are identical, so the line of "H" was painted at the same place:'
            redPixelsEndingWithLineBreak == redPixelsEndingWithoutLineBreak
    }

    def 'With `placement(TOP_LEFT)`, an obstacle splits a line into two parts, and each part starts at the left edge of the free space it is in.'()
    {
        reportInfo """
            With `obstacles(Shape...)` you can tell SwingTree about areas of the component
            on which no text may be painted, for example because a child component sits there.
            In this scenario a 100 pixel wide rectangle covers the middle of the first line,
            from 150 to 250 pixels. That leaves two free spaces in the first line:
            one from the left edge of the content area at 10 pixels to the obstacle at 150 pixels,
            and one from the obstacle at 250 pixels to the right edge of the content area at 390 pixels.

            SwingTree fills the left free space with as many words as fit into it and continues
            the text in the right free space. Both parts of the first line stand on the same
            baseline, which is the line the letters stand on, so the painted pixels of both parts
            start and end at the same height. Nothing is painted on the obstacle.

            With `TOP_LEFT`, each part is put at the left edge of its own free space:
            the left part starts at 10 pixels and the right part starts at 250 pixels,
            directly after the obstacle. The painted pixels start up to 4 pixels further right,
            because the letter "H" does not start with a stroke: in this font it has a few empty
            pixel columns to the left of its first stroke.
        """
        given : 'A rectangle which covers the middle of the first line:'
            int obstacleLeft = 150, obstacleRight = 250
            var obstacle = new Rectangle(obstacleLeft, 0, obstacleRight - obstacleLeft, 40)
        and : 'A component with a red text which is long enough to need more than one line, even without the obstacle:'
            var box =
                    UI.box().withStyle( it -> it
                        .size(400, 200)
                        .padding(10)
                        .text( t -> t
                            .placement(UI.Placement.TOP_LEFT)
                            .wrapLines(true)
                            .obstacles(obstacle)
                            .font( f -> f.family(Font.DIALOG).size(20) )
                            .content( StyledString.of( f -> f.color(RED), "HH HH HH HH HH HH HH HH HH HH HH HH HH HH" ) )
                        )
                    )
                    .get(JBox)

        when : """
            We render the component and look at the top 35 pixels of the image, which contain the
            first line but not the second one. In that strip, we find the rectangle around the red
            pixels left of the obstacle and the rectangle around the red pixels right of the obstacle:
        """
            var image = Utility.renderSingleComponent(box)
            var firstLine = new Rectangle(0, 0, 400, 35)
            var leftPart  = boundsOfPixelsPaintedIn(image, RED, firstLine.intersection(new Rectangle(0, 0, obstacleLeft, 200)))
            var rightPart = boundsOfPixelsPaintedIn(image, RED, firstLine.intersection(new Rectangle(obstacleRight, 0, 400, 200)))

        then : 'No red pixel lies on the obstacle:'
            boundsOfPixelsPaintedIn(image, RED, obstacle) == null
        and : 'Both parts of the first line are painted, and their painted pixels start and end at the same height:'
            leftPart != null && rightPart != null
            leftPart.y == rightPart.y
            leftPart.y + leftPart.height == rightPart.y + rightPart.height
        and : 'The left part starts at the left edge of the content area at 10 pixels, plus the empty pixels before an "H":'
            leftPart.x >= 10
            leftPart.x <= 10 + 4
        and : 'The right part starts directly after the obstacle at 250 pixels, plus the empty pixels before an "H":'
            rightPart.x >= obstacleRight
            rightPart.x <= obstacleRight + 4
    }

    def 'With `placement(TOP_RIGHT)`, an obstacle splits a line into two parts, and the last letter of each part ends at the right edge of the free space it is in.'()
    {
        reportInfo """
            With `obstacles(Shape...)` you can tell SwingTree about areas of the component
            on which no text may be painted, for example because a child component sits there.
            In this scenario a 100 pixel wide rectangle covers the middle of the first line,
            from 150 to 250 pixels. That leaves two free spaces in the first line:
            one from the left edge of the content area at 10 pixels to the obstacle at 150 pixels,
            and one from the obstacle at 250 pixels to the right edge of the content area at 390 pixels.

            SwingTree fills the left free space with as many words as fit into it and continues
            the text in the right free space. Both parts of the first line stand on the same
            baseline, which is the line the letters stand on, so the painted pixels of both parts
            start and end at the same height. Nothing is painted on the obstacle.

            With `TOP_RIGHT`, each part is put at the right edge of its own free space.
            A part which ends because the next word did not fit still contains the space
            character which followed its last word. That space character paints no pixels,
            so SwingTree ignores its width when it lines the part up with the right edge.
            This matters because the space character may stick out past the right edge
            of the free space: if SwingTree counted it, the part would be moved left by
            the width of a space, and its first letter could be painted onto the obstacle.
            So the last "H" of the left part ends at the obstacle at 150 pixels, and the
            last "H" of the right part ends at the right edge of the content area at 390 pixels.
            The painted pixels end up to 4 pixels further left than that, because the letter "H"
            does not end with a stroke: in this font it has a few empty pixel columns to the right
            of its last stroke.
        """
        given : 'A rectangle which covers the middle of the first line:'
            int obstacleLeft = 150, obstacleRight = 250
            var obstacle = new Rectangle(obstacleLeft, 0, obstacleRight - obstacleLeft, 40)
        and : 'A component with a red text which is long enough to need more than one line, even without the obstacle:'
            var box =
                    UI.box().withStyle( it -> it
                        .size(400, 200)
                        .padding(10)
                        .text( t -> t
                            .placement(UI.Placement.TOP_RIGHT)
                            .wrapLines(true)
                            .obstacles(obstacle)
                            .font( f -> f.family(Font.DIALOG).size(20) )
                            .content( StyledString.of( f -> f.color(RED), "HH HH HH HH HH HH HH HH HH HH HH HH HH HH" ) )
                        )
                    )
                    .get(JBox)

        when : """
            We render the component and look at the top 35 pixels of the image, which contain the
            first line but not the second one. In that strip, we find the rectangle around the red
            pixels left of the obstacle and the rectangle around the red pixels right of the obstacle:
        """
            var image = Utility.renderSingleComponent(box)
            var firstLine = new Rectangle(0, 0, 400, 35)
            var leftPart  = boundsOfPixelsPaintedIn(image, RED, firstLine.intersection(new Rectangle(0, 0, obstacleLeft, 200)))
            var rightPart = boundsOfPixelsPaintedIn(image, RED, firstLine.intersection(new Rectangle(obstacleRight, 0, 400, 200)))

        then : 'No red pixel lies on the obstacle:'
            boundsOfPixelsPaintedIn(image, RED, obstacle) == null
        and : 'Both parts of the first line are painted, and their painted pixels start and end at the same height:'
            leftPart != null && rightPart != null
            leftPart.y == rightPart.y
            leftPart.y + leftPart.height == rightPart.y + rightPart.height
        and : 'The left part ends at the obstacle at 150 pixels, minus the empty pixels after an "H":'
            leftPart.x + leftPart.width <= obstacleLeft
            leftPart.x + leftPart.width >= obstacleLeft - 4
        and : 'The right part ends at the right edge of the content area at 390 pixels, minus the empty pixels after an "H":'
            rightPart.x + rightPart.width <= 390
            rightPart.x + rightPart.width >= 390 - 4
    }

    static Rectangle boundsOfPixelsPaintedIn( BufferedImage image, Color color ) {
        return boundsOfPixelsPaintedIn(image, color, new Rectangle(0, 0, image.width, image.height))
    }

    static Rectangle boundsOfPixelsPaintedIn( BufferedImage image, Color color, Rectangle region ) {
        var area = region.intersection(new Rectangle(0, 0, image.width, image.height))
        Rectangle bounds = null
        for ( int y = area.y; y < area.y + area.height; y++ )
            for ( int x = area.x; x < area.x + area.width; x++ ) {
                var pixel = new Color(image.getRGB(x, y), true)
                var isPaintedInColor = pixel.alpha >= 160 &&
                                       Math.abs(pixel.red   - color.red  ) <= 40 &&
                                       Math.abs(pixel.green - color.green) <= 40 &&
                                       Math.abs(pixel.blue  - color.blue ) <= 40
                if ( isPaintedInColor ) {
                    if ( bounds == null ) bounds = new Rectangle(x, y, 1, 1)
                    else bounds.add(new Rectangle(x, y, 1, 1))
                }
            }
        return bounds
    }
}
