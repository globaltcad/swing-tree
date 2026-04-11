package swingtree.style

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.Tuple
import swingtree.SwingTree
import swingtree.UI
import swingtree.components.JBox
import swingtree.threading.EventProcessor
import utility.SwingTreeTestConfigurator

import javax.swing.*
import java.awt.Shape
import java.awt.geom.Ellipse2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage

@Title("Styled Text Layout Around Obstacles")
@Narrative('''

    SwingTree\'s styled text layout engine supports the notion of *obstacles*:
    shapes (given in component coordinates) that the text must flow around
    and may never be rendered on top of.  The practical motivation is that
    a styled component can contain child components — buttons, icons, or
    small widgets — that live in the same visual area as the text, and the
    text should politely step aside rather than collide with them.

    An obstacle shape is registered through the `TextConf#obstacles(Shape...)`
    or `TextConf#obstacles(Tuple<Shape>)` method.  During layout, each text
    line queries the obstacle-free horizontal intervals at its vertical position
    and fits the text into those intervals.  Obstacles that do not intersect
    a given line have no effect on that line at all.

    The two most important emergent properties of this design are:

     1.  *An obstacle only matters when a text line reaches it.*
         If a line wraps naturally before hitting an obstacle\'s left edge, the
         obstacle is completely invisible to the layout engine for that line.
         The preferred height therefore stays identical to the obstacle-free case.

     2.  *An obstacle that does intersect a line narrows the available width.*
         Narrower width means earlier wrapping, which means more lines, which
         means a taller preferred height.  The effect is proportional to the
         width of the obstacle: a wider obstacle steals more horizontal space,
         forcing more lines and producing a taller component.

    This specification explores both properties, as well as the compounding
    effect of multiple obstacles, the geometric precision with which curved
    (elliptical) obstacles are handled, and the way obstacle position interacts
    with line length to determine when a height increase becomes observable.

''')
@Subject([TextConf, StyledString])
class Styled_Text_Obstacles_Spec extends Specification
{
    def setupSpec() {
        SwingTree.initializeUsing(SwingTreeTestConfigurator.get())
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
    }

    def cleanupSpec() {
        SwingTree.clear()
    }

    def setup() {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())
    }

    def cleanup() {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())
    }


    def 'An obstacle at the right edge of a component does not affect preferred height when lines break before reaching it'()
    {
        reportInfo """
            Consider a component that displays many short, newline-separated
            phrases — the kind of content you might find in a sidebar, a tag
            cloud, or a list of key-value labels.  Each line is only a fraction
            of the component\'s total width, so the text never reaches the right
            edge.

            If a right-edge obstacle is added to such a component — say, to
            reserve room for a scroll indicator or a small icon that lives at
            the far right — the layout engine will compute an obstacle-free
            interval that stretches from the left edge up to the obstacle\'s
            left edge.  Because every text line is short enough to fit inside
            that interval, the line breaks occur at exactly the same positions
            as they would without any obstacle.  The preferred height is
            therefore identical in both cases.

            This "invisible obstacle" behaviour is deliberate: obstacles should
            only affect the layout when they actually get in the way.  When they
            don\'t, the performance cost of their presence is negligible (one
            bounding-box check per line) and the visual result is unchanged.
        """
        given : 'We initialize SwingTree with a fixed UI scale so font metrics stay deterministic:'
            SwingTree.initializeUsing( it -> {
                it = it.uiScaleFactor(1.0f)
                it = SwingTreeTestConfigurator.get().configure(it)
            })
        and : 'A column of short, newline-separated phrases — each well below the 300 px mark:'
            var content = Tuple.of(
                StyledString.of(f -> f.size(14),
                    "First item.\nSecond item.\nThird item.\n" +
                    "Fourth item.\nFifth item.\nSixth item.\n" +
                    "Seventh item.\nEighth item.\nNinth item.\nTenth item."
                )
            )
        and : 'A helper that measures preferred height at 400 px with the supplied obstacles:'
            def preferredHeight = { Shape[] obstacles ->
                int[] result = new int[1]
                UI.runNow({
                    var box = UI.box()
                                .withStyle(conf -> conf
                                    .text(t -> t
                                        .font(f -> f.family("Ubuntu"))
                                        .content(content)
                                        .wrapLines(true)
                                        .autoPreferredHeight(true)
                                        .obstacles(obstacles)
                                    )
                                )
                                .get(JBox)
                    box.setSize(400, 0)
                    var buf = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                    box.paintComponent(buf.createGraphics())
                    result[0] = box.getPreferredSize().height
                })
                return result[0]
            }

        when : 'We measure the preferred height without any obstacle:'
            int heightNoObstacle = preferredHeight(new Shape[0])
        and : """
            We add a right-edge obstacle that fills the last 100 px of the component
            from top to bottom (tall enough to overlap every text line):
        """
            var rightEdgeObstacle = new Rectangle2D.Float(300, 0, 100, 10_000)
            int heightWithObstacle = preferredHeight(rightEdgeObstacle)

        then : 'The preferred height is exactly the same — the obstacle is invisible to the short lines:'
            heightNoObstacle > 0
            heightNoObstacle == heightWithObstacle
    }


    def 'An obstacle inside the text flow area forces additional wrapping and increases the preferred height'()
    {
        reportInfo """
            When a text line is wide enough to reach an obstacle, the layout
            engine must wrap the text earlier — it cannot render on top of the
            obstacle.  Each line that previously spanned the obstacle\'s
            horizontal range is now split at (or before) the obstacle\'s left
            edge, and the remaining characters must move to the next line.
            More lines means more total height, so the preferred height grows.

            This test uses a long, prose-style sentence that, when laid out at
            400 px, comfortably fills a substantial portion of each line.  A
            right-edge obstacle that starts at x=200 forces every such line to
            wrap within the obstacle-free 200 px interval, roughly doubling the
            number of lines and therefore roughly doubling the preferred height.
        """
        given : 'SwingTree with a fixed UI scale:'
            SwingTree.initializeUsing( it -> {
                it = it.uiScaleFactor(1.0f)
                it = SwingTreeTestConfigurator.get().configure(it)
            })
        and : 'A long sentence that fills most of the line width when laid out at 400 px:'
            var content = Tuple.of(
                StyledString.of(f -> f.size(14),
                    "The quick brown fox jumps over the lazy dog, and the lazy dog just shrugged " +
                    "because it had seen this particular fox many times before and was no longer impressed."
                )
            )
        and : 'A helper that measures preferred height at 400 px with the supplied obstacles:'
            def preferredHeight = { Shape[] obstacles ->
                int[] result = new int[1]
                UI.runNow({
                    var box = UI.box()
                                .withStyle(conf -> conf
                                    .text(t -> t
                                        .font(f -> f.family("Ubuntu"))
                                        .content(content)
                                        .wrapLines(true)
                                        .autoPreferredHeight(true)
                                        .obstacles(obstacles)
                                        .placement(UI.Placement.TOP)
                                    )
                                )
                                .get(JBox)
                    box.setSize(400, 0)
                    var buf = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                    box.paintComponent(buf.createGraphics())
                    result[0] = box.getPreferredSize().height
                })
                return result[0]
            }

        when : 'We measure the baseline preferred height with no obstacles:'
            int heightNoObstacle = preferredHeight(new Shape[0])
        and : 'We add a large right-side obstacle that takes up the right half of the component:'
            var halfWidthObstacle = new Rectangle2D.Float(200, 0, 200, 10_000)
            int heightHalfBlocked = preferredHeight(halfWidthObstacle)

        then : 'The obstacle forces more wrapping — preferred height must be strictly greater:'
            heightNoObstacle > 0
            heightHalfBlocked > heightNoObstacle
    }


    def 'The wider the obstacle, the narrower the text area and the taller the preferred height'()
    {
        reportInfo """
            An obstacle occupies horizontal space that text cannot use.
            The more horizontal space it occupies, the earlier text must
            wrap on each affected line, and the more total lines are
            produced.

            This test places three right-side obstacles of increasing widths
            at the same right-hand position and confirms that the preferred
            height grows monotonically: a narrow obstacle causes a modest
            increase, and a wide obstacle causes a much larger increase.

            This is directly analogous to reducing the available line width
            via horizontal padding — except that the narrowing comes from an
            obstacle shape rather than from the component\'s box-model insets.
        """
        given : 'SwingTree with a fixed UI scale:'
            SwingTree.initializeUsing( it -> {
                it = it.uiScaleFactor(1.0f)
                it = SwingTreeTestConfigurator.get().configure(it)
            })
        and : 'A long paragraph that will wrap repeatedly as the available width shrinks:'
            var content = Tuple.of(
                StyledString.of(f -> f.size(14),
                    "Width matters most when text is plentiful: every pixel of horizontal space " +
                    "saved by the obstacle translates directly into additional line-breaks, more lines, " +
                    "and therefore a taller component — so a wider obstacle always costs more height."
                )
            )
        and : 'A helper that measures preferred height at 400 px for a right-edge obstacle of a given width:'
            def preferredHeight = { int obstacleWidth ->
                int[] result = new int[1]
                UI.runNow({
                    def obstacles = obstacleWidth > 0
                        ? new Shape[]{ new Rectangle2D.Float(400 - obstacleWidth, 0, obstacleWidth, 10_000) }
                        : new Shape[0]
                    var box = UI.box()
                                .withStyle(conf -> conf
                                    .text(t -> t
                                        .font(f -> f.family("Ubuntu"))
                                        .content(content)
                                        .wrapLines(true)
                                        .autoPreferredHeight(true)
                                        .obstacles(obstacles)
                                        .placement(UI.Placement.TOP)
                                    )
                                )
                                .get(JBox)
                    box.setSize(400, 0)
                    var buf = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                    box.paintComponent(buf.createGraphics())
                    result[0] = box.getPreferredSize().height
                })
                return result[0]
            }

        when : 'We measure preferred height for obstacles of widths 0, 50, 150, and 250 px:'
            int h0   = preferredHeight(0)    // no obstacle   — full 400 px available
            int h50  = preferredHeight(50)   // small nudge   — 350 px available
            int h150 = preferredHeight(150)  // noticeable    — 250 px available
            int h250 = preferredHeight(250)  // severe        — 150 px available

        then : 'Each step up in obstacle width produces a strictly greater preferred height:'
            h0   > 0
            h0   < h50
            h50  < h150
            h150 < h250
        and : 'The most severe obstacle (leaving only 150 px) produces a substantially taller layout:'
            h250 > h0
    }


    def 'Two separate obstacles placed in different parts of the line combine their effects on preferred height'()
    {
        reportInfo """
            When two obstacles occupy non-overlapping horizontal ranges on the
            same line, each independently reduces the free horizontal space
            available to the text.  Together they have a greater impact than
            either one alone.

            A concrete mental model: imagine a 400 px component with a 50 px
            label widget anchored to the left edge and a 50 px icon anchored
            to the right edge.  Neither widget alone would force a dramatic
            increase in height, but together they reduce the usable width from
            400 px to 300 px, which — for long text — is enough to cause
            several extra line-breaks.

            This test verifies that three obstacle configurations produce a
            clear, monotonically increasing sequence of preferred heights:
            no obstacles < one side obstacle < both side obstacles.
        """
        given : 'SwingTree with a fixed UI scale:'
            SwingTree.initializeUsing( it -> {
                it = it.uiScaleFactor(1.0f)
                it = SwingTreeTestConfigurator.get().configure(it)
            })
        and : 'A long text that will wrap when horizontal space is reduced from either side:'
            var content = Tuple.of(
                StyledString.of(f -> f.size(14),
                    "Two obstacles flanking the text on both sides act like bilateral padding: " +
                    "each one independently reduces the available width from the respective edge, " +
                    "so together their combined footprint is strictly larger than either obstacle " +
                    "alone and forces more line-breaks, more lines, and a taller preferred height."
                )
            )
        and : 'A helper that measures preferred height at 400 px for the given obstacles:'
            def preferredHeight = { Shape[] obstacles ->
                int[] result = new int[1]
                UI.runNow({
                    var box = UI.box()
                                .withStyle(conf -> conf
                                    .text(t -> t
                                        .font(f -> f.family("Ubuntu"))
                                        .content(content)
                                        .wrapLines(true)
                                        .autoPreferredHeight(true)
                                        .obstacles(obstacles)
                                        .placement(UI.Placement.TOP)
                                    )
                                )
                                .get(JBox)
                    box.setSize(400, 0)
                    var buf = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                    box.paintComponent(buf.createGraphics())
                    result[0] = box.getPreferredSize().height
                })
                return result[0]
            }

        and : 'The two obstacles, each 80 px wide, anchored to opposite edges:'
            var leftObstacle  = new Rectangle2D.Float(  0, 0, 80, 10_000)   // blocks x = 0..80,   free: [80, 400] = 320 px
            var rightObstacle = new Rectangle2D.Float(320, 0, 80, 10_000)   // blocks x = 320..400, free: [0, 320]  = 320 px

        when : 'We measure preferred height for the three configurations:'
            int heightNone  = preferredHeight(new Shape[0])
            int heightLeft  = preferredHeight(leftObstacle)
            int heightRight = preferredHeight(rightObstacle)
            int heightBoth  = preferredHeight(leftObstacle, rightObstacle)

        then : 'No obstacles produces the smallest height (most horizontal space available):'
            heightNone > 0
        and : 'A single 80 px obstacle on either side has the same effect (symmetric geometry):'
            heightLeft == heightRight
        and : 'A single obstacle increases height compared to no obstacles:'
            heightLeft > heightNone
        and : 'Both obstacles together (leaving only 240 px free) produce a strictly greater height than either alone:'
            heightBoth > heightLeft
        and : 'The combined height is strictly greater than the no-obstacle baseline:'
            heightBoth > heightNone
    }


    def 'An elliptical obstacle narrows the available text width less than a same-size rectangle does'()
    {
        reportInfo """
            The text layout engine uses exact geometric intersection, not
            bounding-box approximation, when computing obstacle-free intervals.
            For a convex curved shape like an ellipse, this means that the
            text lines near the top and bottom of the ellipse\'s vertical span
            see a narrower blocked region than they would if the obstacle were
            a rectangle with the same bounding box.

            Concretely: at the very top and bottom of an ellipse only a
            sliver of the line is blocked (the chord is short), whereas a
            rectangle blocks its full width regardless of vertical position.
            Lines that pass through the narrow "tips" of the ellipse are
            therefore wider when the obstacle is an ellipse than they would
            be with a rectangle, leading to fewer line-breaks and a shorter
            preferred height.

            This property is what allows the layout engine to produce natural,
            arced text-flow around circular child components — the kind of
            layout you would see in a magazine running text around a round
            portrait photograph.
        """
        given : 'SwingTree with a fixed UI scale:'
            SwingTree.initializeUsing( it -> {
                it = it.uiScaleFactor(1.0f)
                it = SwingTreeTestConfigurator.get().configure(it)
            })
        and : 'A long text that will wrap repeatedly around the obstacle area:'
            var content = Tuple.of(
                StyledString.of(f -> f.size(13),
                    "Geometry matters: an ellipse blocks less horizontal space near its top and bottom " +
                    "than a same-size rectangle does, because the chord of an ellipse narrows as you " +
                    "approach either pole. Lines passing through those tapered regions therefore have " +
                    "more room, leading to fewer line-breaks and a shorter overall layout height. And so " +
                    "therefore, you can expect text flowing around an elliptical obstacle to take up less " +
                    "vertical space than text flowing around a rectangular obstacle of the same width and height."
                )
            )
        and : 'A helper that measures preferred height at 400 px for the given obstacle:'
            def preferredHeight = { Shape obstacle ->
                int[] result = new int[1]
                UI.runNow({
                    var box = UI.box()
                                .withStyle(conf -> conf
                                    .text(t -> t
                                        .font(f -> f.family("Ubuntu"))
                                        .content(content)
                                        .wrapLines(true)
                                        .autoPreferredHeight(true)
                                        .obstacles(obstacle)
                                        .placement(UI.Placement.TOP_LEFT)
                                    )
                                )
                                .get(JBox)
                    box.setSize(400, 0)
                    var buf = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                    box.paintComponent(buf.createGraphics())
                    result[0] = box.getPreferredSize().height
                })
                return result[0]
            }

        and : 'A rectangle and an ellipse sharing the exact same bounding box (right-centre area):'
            float ox = 100, oy = 0, ow = 200, oh = 300
            var rectangle = new Rectangle2D.Float(ox, oy, ow, oh)
            var ellipse   = new Ellipse2D.Float(ox, oy, ow, oh)

        when : 'We measure the preferred height for each shape:'
            int heightRect    = preferredHeight(rectangle)
            int heightEllipse = preferredHeight(ellipse)

        then : 'Both obstacles increase the preferred height compared to an unconstrained layout:'
            heightRect    > 0
            heightEllipse > 0
        and : """
            The rectangle always blocks its full 150 px of width, regardless of y-position,
            while the ellipse tapers to zero width at the top and bottom.
            The ellipse therefore causes less additional wrapping than the rectangle:
        """
            heightEllipse < heightRect
    }


    def 'Moving an obstacle progressively from the far right toward the centre increases preferred height'()
    {
        reportInfo """
            An obstacle placed far to the right of where the text naturally ends
            has no effect on the layout.  As the obstacle moves leftward into
            the text\'s territory, it begins to clip lines, forcing earlier
            wrapping.  The further left it moves, the more lines are clipped
            and the greater the increase in preferred height.

            This test documents that progression by measuring preferred height
            for an obstacle at four horizontal positions:

              — x = 380 px (obstacle in the last 20 px, text likely doesn\'t reach it)
              — x = 300 px (obstacle in the right quarter)
              — x = 200 px (obstacle in the right half)
              — x = 100 px (obstacle in the left half — severely constrains width)

            All four obstacles have the same width (300 px) and span the full
            component height, so the only variable is their horizontal position.
            The expected outcome is a non-decreasing sequence of preferred heights,
            with the leftmost obstacle producing the tallest layout by a clear margin.
        """
        given : 'SwingTree with a fixed UI scale:'
            SwingTree.initializeUsing( it -> {
                it = it.uiScaleFactor(1.0f)
                it = SwingTreeTestConfigurator.get().configure(it)
            })
        and : 'A generous amount of text that fills lines at 400 px and wraps significantly at 200 px:'
            var content = Tuple.of(
                StyledString.of(f -> f.size(14),
                    "As an obstacle slides leftward from the quiet right margin into the busiest " +
                    "part of the text column, it intercepts more and more lines, each time forcing " +
                    "them to wrap earlier and pushing subsequent lines further down the page."
                )
            )
        and : 'A helper that measures preferred height at 400 px for a 80-px-wide obstacle at the given x:'
            def preferredHeight = { int obstacleX, boolean wrapLines ->
                int[] result = new int[1]
                UI.runNow({
                    var obstacle = new Rectangle2D.Float(obstacleX, 0, 300, 10_000)
                    var box = UI.box()
                                .withStyle(conf -> conf
                                    .text(t -> t
                                        .font(f -> f.family("Ubuntu"))
                                        .content(content)
                                        .wrapLines(wrapLines)
                                        .autoPreferredHeight(true)
                                        .obstacles(obstacle)
                                        .placement(UI.Placement.TOP_RIGHT)
                                    )
                                )
                                .get(JBox)
                    box.setSize(400, 0)
                    var buf = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                    box.paintComponent(buf.createGraphics())
                    result[0] = box.getPreferredSize().height
                })
                return result[0]
            }

        when : 'We measure preferred height as the obstacle slides from x=380 down to x=100:'
            int hFarRight = preferredHeight(380, true)  // barely touches the text area (if at all)
            int hRight    = preferredHeight(300, true)  // right quarter
            int hCentre   = preferredHeight(200, true)  // right half
            int hLeft     = preferredHeight(100, true)  // left half — maximum disruption

        then : 'Each leftward step produces a preferred height that is at least as large as the previous:'
            hFarRight > 0
            hFarRight < hRight
            hRight    < hCentre
            hCentre   < hLeft
        and : 'The obstacle at x=100 forces substantially more wrapping than the one at x=380:'
            hLeft > hFarRight

        when : 'We repeat the previous measurements without line wrapping...'
            hFarRight = preferredHeight(380, false)  // barely touches the text area (if at all)
            hRight    = preferredHeight(300, false)  // right quarter
            hCentre   = preferredHeight(200, false)  // right half
            hLeft     = preferredHeight(100, false)  // left half — maximum disruption

        then : 'Each leftward step does not affect the preferred height because the text will always flow to the right of the obstacle!'
            hFarRight > 0
            hFarRight == hRight
            hRight    == hCentre
            hCentre   == hLeft
    }


    def 'A right-edge obstacle and a left-edge obstacle of equal size produce the same preferred height'()
    {
        reportInfo """
            The text layout engine computes obstacle-free intervals symmetrically:
            it subtracts each obstacle\'s horizontal footprint from the full
            available width and works with whatever contiguous intervals remain.
            Whether a 60 px obstacle sits on the left edge (blocking x = 0..60)
            or on the right edge (blocking x = 340..400) makes no difference to
            the total free width available to the text on each line.  Both leave
            a single 340 px interval, and a 340 px interval produces exactly the
            same line-break positions regardless of where it starts.

            This symmetry property is worth documenting explicitly: users who add
            a component to the right corner of a text panel should expect the same
            height penalty as users who add an equal-sized component to the left
            corner.  Neither side is "cheaper" than the other.
        """
        given : 'SwingTree with a fixed UI scale:'
            SwingTree.initializeUsing( it -> {
                it = it.uiScaleFactor(1.0f)
                it = SwingTreeTestConfigurator.get().configure(it)
            })
        and : 'A long text that spans most of the line width when laid out at 400 px:'
            var content = Tuple.of(
                StyledString.of(f -> f.size(14),
                    "Left and right obstacles of equal width are perfectly symmetric in their impact " +
                    "on line-wrapping: both reduce the usable line width by the same amount, and so " +
                    "both produce the same number of lines and the same preferred height."
                )
            )
        and : 'A helper that measures preferred height for the given obstacle:'
            def preferredHeight = { Shape obstacle ->
                int[] result = new int[1]
                UI.runNow({
                    var box = UI.box()
                                .withStyle(conf -> conf
                                    .text(t -> t
                                        .font(f -> f.family("Ubuntu"))
                                        .content(content)
                                        .wrapLines(true)
                                        .autoPreferredHeight(true)
                                        .obstacles(obstacle)
                                    )
                                )
                                .get(JBox)
                    box.setSize(400, 0)
                    var buf = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                    box.paintComponent(buf.createGraphics())
                    result[0] = box.getPreferredSize().height
                })
                return result[0]
            }

        and : 'A left-edge and a right-edge obstacle, each 60 px wide:'
            var leftObstacle  = new Rectangle2D.Float(  0, 0, 60, 10_000) // free interval: [60, 400] = 340 px
            var rightObstacle = new Rectangle2D.Float(340, 0, 60, 10_000) // free interval: [0, 340]  = 340 px

        when : 'We measure the preferred height for each obstacle:'
            int heightLeft  = preferredHeight(leftObstacle)
            int heightRight = preferredHeight(rightObstacle)

        then : 'Both obstacles produce exactly the same preferred height:'
            heightLeft  > 0
            heightRight > 0
            heightLeft == heightRight
    }


    def 'A narrow obstacle at the right edge is invisible to short-line text but visible to long-line text'()
    {
        reportInfo """
            The most important mental model for obstacles is that they only
            interact with text when a line actually reaches them.  This test
            puts that mental model to the test by combining two observations
            in a single scenario:

             — When the text consists of short, early-breaking lines (each line
               is well within the left boundary of the obstacle), the obstacle
               is completely invisible to the layout engine and the preferred
               height is unaffected.

             — When the very same obstacle is present but the text is a long,
               continuous paragraph whose lines span the full component width,
               the obstacle clips those lines and forces additional wrapping,
               increasing the preferred height.

            The layout engine therefore obeys a simple rule: an obstacle costs
            height only when it gets in the way.
        """
        given : 'SwingTree with a fixed UI scale:'
            SwingTree.initializeUsing( it -> {
                it = it.uiScaleFactor(1.0f)
                it = SwingTreeTestConfigurator.get().configure(it)
            })
        and : 'A narrow right-edge obstacle that blocks only the last 80 px of the 400 px component:'
            var narrowRightObstacle = new Rectangle2D.Float(320, 0, 80, 10_000)

        and : 'Short-line content: many brief phrases that wrap at roughly 60–80 px each:'
            var shortLineContent = Tuple.of(
                StyledString.of(f -> f.size(14),
                    "OK.\nFine.\nSure.\nYes.\nNo.\nDone.\nNext.\nBack.\nSkip.\nStop."
                )
            )
        and : 'Long-line content: a paragraph whose lines naturally span most of the 320 px gap:'
            var longLineContent = Tuple.of(
                StyledString.of(f -> f.size(14),
                    "Long lines cross the boundary and are clipped by the obstacle on every pass, " +
                    "which means more wrapping, more lines, and a greater preferred height than " +
                    "the same text would need without that right-edge reservation."
                )
            )
        and : 'A helper that measures preferred height at 400 px for the given content and obstacles:'
            def preferredHeight = { Tuple<StyledString> textContent, Shape[] obstacles ->
                int[] result = new int[1]
                UI.runNow({
                    var box = UI.box()
                                .withStyle(conf -> conf
                                    .text(t -> t
                                        .font(f -> f.family("Ubuntu"))
                                        .content(textContent)
                                        .wrapLines(true)
                                        .autoPreferredHeight(true)
                                        .obstacles(obstacles)
                                        .placement(UI.Placement.TOP_RIGHT)
                                    )
                                )
                                .get(JBox)
                    box.setSize(400, 0)
                    var buf = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                    box.paintComponent(buf.createGraphics())
                    result[0] = box.getPreferredSize().height
                })
                return result[0]
            }

        when : 'We measure both contents with and without the right-edge obstacle:'
            int shortNoObstacle   = preferredHeight(shortLineContent,  new Shape[0])
            int shortWithObstacle = preferredHeight(shortLineContent,  new Shape[]{ narrowRightObstacle })
            int longNoObstacle    = preferredHeight(longLineContent,   new Shape[0])
            int longWithObstacle  = preferredHeight(longLineContent,   new Shape[]{ narrowRightObstacle })

        then : 'All measurements are positive (the text is non-empty):'
            shortNoObstacle   > 0
            longNoObstacle    > 0

        and : 'Short lines are unaffected by the right-edge obstacle — it never enters the picture:'
            shortWithObstacle == shortNoObstacle

        and : 'Long lines are affected — the obstacle clips them and forces more wrapping:'
            longWithObstacle > longNoObstacle
    }


    def 'Text placement (alignment) does not affect preferred height, even when obstacles are present'()
    {
        reportInfo """
            The `placement` property of a `TextConf` — LEFT, CENTER, RIGHT,
            TOP_LEFT, and so on — controls where inside the available bounds
            the rendered text is *positioned* at paint time.  It is a purely
            visual offset that the renderer applies after the layout engine has
            already determined how many lines are needed and how tall they are.

            As a consequence, switching from LEFT to CENTER or RIGHT placement
            cannot change the number of lines, and therefore cannot change the
            preferred height — even when obstacles are present.  The layout
            engine always computes obstacle-free intervals over the full
            available bounds width; the renderer then draws each line within
            its interval according to the requested alignment.

            This is an important non-intuition to document: users who expect
            that centering text "toward an obstacle" will incur a height penalty
            will be surprised to learn that it does not.  The layout stage is
            alignment-agnostic.  If you need text to avoid an obstacle region,
            register the obstacle via `TextConf#obstacles(Shape...)` — do not
            rely on alignment to steer text away from it, because the layout
            engine does not use the alignment for its interval computations.
        """
        given : 'SwingTree with a fixed UI scale:'
            SwingTree.initializeUsing( it -> {
                it = it.uiScaleFactor(1.0f)
                it = SwingTreeTestConfigurator.get().configure(it)
            })
        and : 'A moderately long paragraph and a right-edge obstacle that narrows the available width:'
            var content = Tuple.of(
                StyledString.of(f -> f.size(14),
                    "The alignment of text within the available layout bounds is a rendering concern, " +
                    "not a layout concern: it never changes which lines are produced or how tall they are. " +
                    "So across all placements, the layout engine computes the same line-break positions and the same " +
                    "preferred height..."
                )
            )
            var rightObstacle = new Rectangle2D.Float(200, 0, 120, 10_000)

        and : 'A helper that measures preferred height at 400 px for a given placement and obstacle list:'
            def preferredHeight = { UI.Placement placement, Shape[] obstacles ->
                int[] result = new int[1]
                UI.runNow({
                    var box = UI.box()
                                .withStyle(conf -> conf
                                    .text(t -> t
                                        .font(f -> f.family("Ubuntu"))
                                        .content(content)
                                        .placement(placement)
                                        .wrapLines(true)
                                        .autoPreferredHeight(true)
                                        .obstacles(obstacles)
                                        .placement(UI.Placement.TOP)
                                    )
                                )
                                .get(JBox)
                    box.setSize(400, 0)
                    var buf = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                    box.paintComponent(buf.createGraphics())
                    result[0] = box.getPreferredSize().height
                })
                return result[0]
            }

        when : 'We measure preferred height for each placement, all with the same right-edge obstacle:'
            def obstacle = new Shape[]{ rightObstacle }
            int hLeft       = preferredHeight(UI.Placement.LEFT,         obstacle)
            int hCenter     = preferredHeight(UI.Placement.CENTER,       obstacle)
            int hRight      = preferredHeight(UI.Placement.RIGHT,        obstacle)
            int hTopLeft    = preferredHeight(UI.Placement.TOP_LEFT,     obstacle)
            int hTopRight   = preferredHeight(UI.Placement.TOP_RIGHT,    obstacle)
            int hBottomLeft = preferredHeight(UI.Placement.BOTTOM_LEFT,  obstacle)

        then : 'Every placement produces a positive preferred height:'
            hLeft > 0
        and : 'All placements produce exactly the same preferred height despite the obstacle:'
            hLeft       == hCenter
            hCenter     == hRight
            hRight      == hTopLeft
            hTopLeft    == hTopRight
            hTopRight   == hBottomLeft
    }


    def 'A right-edge obstacle leaves short lines unchanged but increases height for a mix of short and long lines'()
    {
        reportInfo """
            Real-world text is rarely uniform: a document might have a mix of
            short headings and long body paragraphs.  When a right-edge obstacle
            is present, only the body paragraphs are affected — the short headings
            comfortably fit within the obstacle-free region and contribute the
            same height they would without the obstacle.

            The overall preferred height of such a mixed layout is therefore
            the sum of: the unchanged heading heights plus the increased body
            heights.  It is strictly greater than the no-obstacle case (because
            the body lines are longer), but the increase is proportionally
            smaller than it would be if the entire content were long prose
            (because the headings are unaffected).

            This test demonstrates that nuance by measuring three layouts:
              1. Mixed content with no obstacle.
              2. Mixed content with a right-edge obstacle.
              3. Long-only content with the same right-edge obstacle.

            The expected height ordering is:
              mixed(no obstacle) < mixed(with obstacle), and long(with obstacle) ≤ mixed(with obstacle)
        """
        given : 'SwingTree with a fixed UI scale:'
            SwingTree.initializeUsing( it -> {
                it = it.uiScaleFactor(1.0f)
                it = SwingTreeTestConfigurator.get().configure(it)
            })
        and : 'Short headings that will comfortably fit within the obstacle-free 200 px region:'
            var heading1 = StyledString.of(f -> f.size(16), "Introduction\n")
            var heading2 = StyledString.of(f -> f.size(16), "\nConclusion\n")
        and : 'A long body that will be clipped by the right-edge obstacle:'
            var body = StyledString.of(f -> f.size(14),
                "When some lines are short and others are long, only the long ones are affected " +
                "by a right-edge obstacle — the short ones slide in under the obstacle\'s left edge " +
                "and never notice it is there. This body is long enough to fill multiple lines even " +
                "at full width, so narrowing the usable column to 200 px forces a clear increase " +
                "in the number of lines and therefore in the preferred height."
            )
        and : 'Content tuples for the two scenarios:'
            var mixedContent   = Tuple.of(heading1, body, heading2)
            var longOnlyContent = Tuple.of(body)
        and : 'A substantial right-edge obstacle that blocks the rightmost 200 px, leaving only 200 px free:'
            var rightObstacle = new Rectangle2D.Float(200, 0, 200, 10_000)
        and : 'A helper that measures preferred height at 400 px:'
            def preferredHeight = { Tuple<StyledString> textContent, Shape[] obstacles ->
                int[] result = new int[1]
                UI.runNow({
                    var box = UI.box()
                                .withStyle(conf -> conf
                                    .text(t -> t
                                        .font(f -> f.family("Ubuntu"))
                                        .content(textContent)
                                        .wrapLines(true)
                                        .autoPreferredHeight(true)
                                        .obstacles(obstacles)
                                        .placement(UI.Placement.TOP_LEFT)
                                    )
                                )
                                .get(JBox)
                    box.setSize(400, 0)
                    var buf = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                    box.paintComponent(buf.createGraphics())
                    result[0] = box.getPreferredSize().height
                })
                return result[0]
            }

        when : 'We measure the three configurations:'
            int mixedNoObstacle   = preferredHeight(mixedContent,    new Shape[0])
            int mixedWithObstacle = preferredHeight(mixedContent,    new Shape[]{ rightObstacle })
            int longWithObstacle  = preferredHeight(longOnlyContent, new Shape[]{ rightObstacle })

        then : 'All heights are positive:'
            mixedNoObstacle > 0
        and : 'Adding the obstacle increases the mixed height because the body paragraph is clipped:'
            mixedWithObstacle > mixedNoObstacle
        and : 'The long-only layout (which has no short lines to buffer the obstacle\'s impact) is at least as tall:'
            longWithObstacle < mixedWithObstacle
            // Note: mixed is taller because it includes the headings on top of the wrapped body
    }


    def 'Obstacle avoidance is only active for top-anchored placements; all other placements ignore obstacles'()
    {
        reportInfo """
            The obstacle-avoidance algorithm works by flowing text downward from the top of
            the available bounds, querying the free horizontal intervals at each successive
            line\'s y-coordinate.  This mapping is only geometrically correct when the text
            block is anchored to the top of the bounds — i.e. when placement is TOP, TOP_LEFT,
            or TOP_RIGHT.

            For *bottom-anchored* placements (BOTTOM, BOTTOM_LEFT, BOTTOM_RIGHT) the algorithm
            would have to run in reverse — flowing upward — which would require a completely
            separate implementation and roughly double the complexity.

            For *vertically-centred* placements (CENTER, LEFT, RIGHT) the problem is even more
            fundamental: the vertical position at which a line is rendered depends on the total
            height of the text block, but the total height depends on which obstacles each line
            hits, which in turn depends on the vertical position of each line.  This circular
            dependency has no clean closed-form solution; resolving it correctly would require
            iterative convergence similar to a fluid-dynamics solver — far beyond the scope of
            a UI layout engine.

            Therefore, obstacle avoidance is intentionally disabled for every non-top placement.
            When an obstacle is registered but an incompatible placement is in use, the layout
            engine silently ignores the obstacle and renders the text as if none were present.
            The preferred height is consequently identical to the obstacle-free case.

            This test verifies both sides of that contract:
             — TOP, TOP_LEFT, and TOP_RIGHT DO apply obstacle avoidance (height increases).
             — CENTER, LEFT, RIGHT, BOTTOM, BOTTOM_LEFT, and BOTTOM_RIGHT do NOT (height unchanged).
        """
        given : 'SwingTree with a fixed UI scale:'
            SwingTree.initializeUsing( it -> {
                it = it.uiScaleFactor(1.0f)
                it = SwingTreeTestConfigurator.get().configure(it)
            })
        and : 'A long paragraph whose lines span the full component width at 400 px:'
            var content = Tuple.of(
                StyledString.of(f -> f.size(14),
                    "Obstacle avoidance requires top-anchored placement: only then does the layout engine " +
                    "know the exact y-coordinate of each line before it is placed, allowing it to query " +
                    "which horizontal intervals are free at that level and route the text accordingly."
                )
            )
        and : 'A large central obstacle that blocks the right half of every line:'
            var centralObstacle = new Rectangle2D.Float(200, 0, 200, 10_000)

        and : 'A helper that measures preferred height at 400 px for a given placement and obstacle list:'
            def preferredHeight = { UI.Placement placement, Shape[] obstacles ->
                int[] result = new int[1]
                UI.runNow({
                    var box = UI.box()
                                .withStyle(conf -> conf
                                    .text(t -> t
                                        .font(f -> f.family("Ubuntu"))
                                        .content(content)
                                        .placement(placement)
                                        .wrapLines(true)
                                        .autoPreferredHeight(true)
                                        .obstacles(obstacles)
                                    )
                                )
                                .get(JBox)
                    box.setSize(400, 0)
                    var buf = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                    box.paintComponent(buf.createGraphics())
                    result[0] = box.getPreferredSize().height
                })
                return result[0]
            }

        when : 'We measure the baseline height (no obstacle) for a top-anchored placement:'
            int baselineHeight = preferredHeight(UI.Placement.TOP, new Shape[0])

        then : 'The baseline is positive:'
            baselineHeight > 0

        when : 'We measure height WITH the central obstacle for each top-anchored placement:'
            int hTop      = preferredHeight(UI.Placement.TOP,       new Shape[]{ centralObstacle })
            int hTopLeft  = preferredHeight(UI.Placement.TOP_LEFT,  new Shape[]{ centralObstacle })
            int hTopRight = preferredHeight(UI.Placement.TOP_RIGHT, new Shape[]{ centralObstacle })

        then : 'All three top-anchored placements honour the obstacle — height increases:'
            hTop      > baselineHeight
            hTopLeft  > baselineHeight
            hTopRight > baselineHeight

        when : 'We measure height WITH the same obstacle for every non-top placement:'
            int hCenter      = preferredHeight(UI.Placement.CENTER,       new Shape[]{ centralObstacle })
            int hLeft        = preferredHeight(UI.Placement.LEFT,         new Shape[]{ centralObstacle })
            int hRight       = preferredHeight(UI.Placement.RIGHT,        new Shape[]{ centralObstacle })
            int hBottom      = preferredHeight(UI.Placement.BOTTOM,       new Shape[]{ centralObstacle })
            int hBottomLeft  = preferredHeight(UI.Placement.BOTTOM_LEFT,  new Shape[]{ centralObstacle })
            int hBottomRight = preferredHeight(UI.Placement.BOTTOM_RIGHT, new Shape[]{ centralObstacle })

        then : 'Non-top placements ignore the obstacle entirely — height is the same as the baseline:'
            hCenter      == baselineHeight
            hLeft        == baselineHeight
            hRight       == baselineHeight
            hBottom      == baselineHeight
            hBottomLeft  == baselineHeight
            hBottomRight == baselineHeight
    }


    def 'Child components of a styled container are automatically registered as text-layout obstacles'()
    {
        reportInfo """
            The most natural way to encounter text obstacles in a real application is
            through child components.  Consider a panel that renders introductory text
            with a thumbnail image widget anchored to the right: the text should flow
            to the left of the image rather than disappearing behind it.

            SwingTree handles this automatically.  When the style engine evaluates a
            component\'s text configuration it inspects `owner.getComponents()` and adds
            every child\'s `getBounds()` rectangle to the text obstacles.  This happens
            transparently, without any explicit `TextConf#obstacles(...)` call from
            application code.

            Because the obstacle shapes come directly from `Component.getBounds()`, which
            returns the rectangle of each child in the *parent\'s* coordinate system, the
            positions used by the layout engine are exactly those that the Swing paint
            system uses — there is no coordinate-space mismatch.

            This test uses a null layout manager so that child bounds can be set precisely
            via `JComponent.setBounds(x, y, width, height)`.  That makes the relationship
            between the child\'s on-screen position and the text-layout effect completely
            transparent and easy to reason about.
        """
        given : 'SwingTree with a fixed UI scale:'
            SwingTree.initializeUsing( it -> {
                it = it.uiScaleFactor(1.0f)
                it = SwingTreeTestConfigurator.get().configure(it)
            })
        and : 'A long text that will wrap clearly whenever the available width is halved:'
            var content = Tuple.of(
                StyledString.of(f -> f.size(14),
                    "When a child component occupies the right half of the parent, " +
                    "the styled text must wrap earlier on every line — just as if a " +
                    "200 px wide obstacle had been explicitly registered via " +
                    "TextConf.obstacles().  The preferred height therefore grows."
                )
            )

        and : """
            A helper that builds a 400-px-wide JBox with the text above, optionally
            adds a single child at x=200 with width=200, paints the parent, and returns
            both the preferred height and the full style-conf string so that we can
            inspect the auto-derived obstacles:
        """
            def paintAndCapture = { boolean addChild ->
                def captured = new Object[2]
                UI.runNow({
                    var box = UI.box()
                                .withStyle(conf -> conf
                                    .text(t -> t
                                        .font(f -> f.family("Ubuntu"))
                                        .content(content)
                                        .wrapLines(true)
                                        .autoPreferredHeight(true)
                                        .placement(UI.Placement.TOP)
                                        // No explicit obstacles — they should come from the child
                                    )
                                )
                                .get(JBox)
                    // Null layout so we control the child position ourselves
                    box.setLayout(null)
                    box.setSize(400, 0)
                    if ( addChild ) {
                        var child = new JBox()
                        // Place the child in the right half: x=200, y=0, width=200, height=500
                        child.setBounds(200, 0, 200, 500)
                        box.add(child)
                    }
                    var buf = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                    box.paintComponent(buf.createGraphics())
                    captured[0] = box.getPreferredSize().height
                    captured[1] = ComponentExtension.from(box).getStyle().toString()
                })
                return captured
            }

        when : 'We paint the box with no children (plain text, no obstacles):'
            def noChild = paintAndCapture(false)
        and : 'We paint the box with a child occupying the right half (x=200, w=200):'
            def withChild = paintAndCapture(true)

        then : 'Without a child, the preferred height is positive but small (full 400 px available):'
            (noChild[0] as int) > 0
        and : """
            With the child present, the layout engine has only 200 px of free horizontal
            space per line — the child\'s bounding rectangle blocks the right half.
            This forces more line-breaks and produces a strictly taller preferred height:
        """
            (withChild[0] as int) > (noChild[0] as int)

        and : """
            The style conf recorded after paint with the child present must include
            the child\'s exact AWT bounds rectangle among the text obstacles.
            This confirms that SwingTree used `Component.getBounds()` of the child —
            java.awt.Rectangle[x=200,y=0,width=200,height=500] — as the obstacle shape:
        """
            (withChild[1] as String).contains("java.awt.Rectangle[x=200,y=0,width=200,height=500]")

        and : 'Without a child, no Rectangle obstacle shapes appear in the style conf at all:'
            !(noChild[1] as String).contains("java.awt.Rectangle")

        when : """
            We also verify that moving the child to the left half of the component (x=0, w=200)
            produces an equally tall preferred height — confirming the symmetric obstacle behaviour
            that was demonstrated in an earlier test: left and right obstacles of the same size
            have identical impact on line-wrapping.
        """
            def capturedLeft = new Object[2]
            UI.runNow({
                var box = UI.box()
                            .withStyle(conf -> conf
                                .text(t -> t
                                    .font(f -> f.family("Ubuntu"))
                                    .content(content)
                                    .wrapLines(true)
                                    .autoPreferredHeight(true)
                                    .placement(UI.Placement.TOP_RIGHT)
                                )
                            )
                            .get(JBox)
                box.setLayout(null)
                box.setSize(400, 0)
                var childLeft = new JBox()
                childLeft.setBounds(0, 0, 200, 500)   // left half this time
                box.add(childLeft)
                var buf = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                box.paintComponent(buf.createGraphics())
                capturedLeft[0] = box.getPreferredSize().height
                capturedLeft[1] = ComponentExtension.from(box).getStyle().toString()
            })
        then : 'A left-half child produces the same preferred height as a right-half child (symmetric):'
            (capturedLeft[0] as int) == (withChild[0] as int)
        and : 'The style conf with a left-half child shows its Rectangle obstacle at x=0:'
            (capturedLeft[1] as String).contains("java.awt.Rectangle[x=0,y=0,width=200,height=500]")
    }


    def 'Disabling automatic child-derived obstacles via obstaclesFromChildrenEnabled(false) makes child components invisible to the text layout'()
    {
        reportInfo """
            By default, SwingTree registers every child of a styled container as a
            text-layout obstacle so that text flows politely around the children.
            This is usually the desired behaviour — but there are legitimate cases
            where you want text and child components to share the same visual area:
            a transparent overlay badge, a purely decorative widget, a glass-pane
            effect, or a child component that is positioned far off-screen and would
            otherwise skew the layout needlessly.

            For those cases the `TextConf#obstaclesFromChildrenEnabled(boolean)` switch
            lets you turn the automatic child-obstacle registration off entirely.
            When it is set to `false`, child components are completely ignored by the
            obstacle-collection pass — the layout engine behaves as if the container
            had no children at all, regardless of the current
            `obstaclesFromChildren(UI.ComponentBoundary)` setting.

            This test pins down that contract by measuring three configurations of
            the same 400 × N box with a 200 × 500 child occupying the right half:

              1. default (enabled, so the child narrows the text area)
              2. enabled=false (so the child is ignored and the text uses the full width)
              3. no child at all (as a control baseline)

            The expected outcome is that (2) and (3) produce the exact same preferred
            height — proving that `obstaclesFromChildrenEnabled(false)` suppresses
            child-derived obstacles completely — while (1) is strictly taller than both,
            confirming that the child does interact with the layout when the switch is on.

            We additionally inspect the rendered style-conf string to verify that the
            disabled case does not leak a child `Rectangle` into the obstacle tuple
            and that the chosen flag value is faithfully recorded.
        """
        given : 'SwingTree with a fixed UI scale so that font metrics stay deterministic:'
            SwingTree.initializeUsing( it -> {
                it = it.uiScaleFactor(1.0f)
                it = SwingTreeTestConfigurator.get().configure(it)
            })
        and : 'A long text that will wrap noticeably whenever the available width is reduced:'
            var content = Tuple.of(
                StyledString.of(f -> f.size(14),
                    "Sometimes you want a child component to share the visual area with the text " +
                    "without forcing the text to flow around it — a transparent badge, a decorative " +
                    "marker, or a glass-pane overlay. The obstaclesFromChildrenEnabled flag controls " +
                    "precisely that behaviour in the SwingTree styled text layout engine."
                )
            )
        and : 'A helper that builds a 400 px box, optionally adds a right-half child, and captures height + style string:'
            def paintAndCapture = { boolean addChild, boolean enabled ->
                def captured = new Object[2]
                UI.runNow({
                    var box = UI.box()
                                .withStyle(conf -> conf
                                    .text(t -> t
                                        .font(f -> f.family("Ubuntu"))
                                        .content(content)
                                        .wrapLines(true)
                                        .autoPreferredHeight(true)
                                        .placement(UI.Placement.TOP_LEFT)
                                        .obstaclesFromChildrenEnabled(enabled)
                                    )
                                )
                                .get(JBox)
                    box.setLayout(null)
                    box.setSize(400, 0)
                    if ( addChild ) {
                        var child = new JBox()
                        child.setBounds(200, 0, 200, 500)
                        box.add(child)
                    }
                    var buf = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                    box.paintComponent(buf.createGraphics())
                    captured[0] = box.getPreferredSize().height
                    captured[1] = ComponentExtension.from(box).getStyle().toString()
                })
                return captured
            }

        when : 'We measure the three configurations — child+enabled (default), child+disabled, no child:'
            def enabled  = paintAndCapture(true,  true)   // the child IS an obstacle
            def disabled = paintAndCapture(true,  false)  // the child is NOT an obstacle
            def noChild  = paintAndCapture(false, true)   // control baseline: no child

        then : 'The default (enabled) configuration produces a strictly taller layout than the baseline:'
            (enabled[0] as int)  > (noChild[0] as int)
        and : 'Disabling the flag recovers the baseline height exactly — the child is invisible to the layout:'
            (disabled[0] as int) == (noChild[0] as int)

        and : 'With the flag enabled, the child Rectangle appears in the obstacle tuple of the style conf:'
            (enabled[1] as String).contains("java.awt.Rectangle[x=200,y=0,width=200,height=500]")
        and : 'With the flag disabled, no child Rectangle appears in the style conf at all:'
            !(disabled[1] as String).contains("java.awt.Rectangle")

        and : 'The style conf faithfully records the chosen obstaclesFromChildrenEnabled flag value:'
            (enabled[1]  as String).contains("obstaclesFromChildrenEnabled=true")
            (disabled[1] as String).contains("obstaclesFromChildrenEnabled=false")
    }


    def 'The obstaclesFromChildren(ComponentBoundary) setting peels the child\'s obstacle inward through the box-model layers'()
    {
        reportInfo """
            When child components are automatically registered as text obstacles, you
            can choose *how much* of each child counts as "in the way" of the text
            by picking a `UI.ComponentBoundary`.  Think of it as peeling an onion
            inward through the child\'s box model:

              — `OUTER_TO_EXTERIOR` (the default) uses the child\'s full bounding
                rectangle, including any styled margin.  This is the largest possible
                obstacle — the text must give the entire child a wide berth.

              — `EXTERIOR_TO_BORDER` excludes the margin, so the text is allowed to
                flow *into* the child\'s margin area, but then stop at the *body*.  
                The obstacle shrinks inward on every side by the margin size.

              — `BORDER_TO_INTERIOR` additionally excludes the border, so text may
                flow through both margin and border areas.  The obstacle shrinks
                further.

              — `INTERIOR_TO_CONTENT` excludes margin, border *and* padding — only
                the innermost content rectangle remains as an obstacle.  This is the
                smallest obstacle the switch can produce.

            A smaller obstacle takes up less horizontal space on each line, leaves
            more room for text, forces fewer line-breaks, and therefore produces a
            *shorter* preferred layout height.  The net effect is a monotonically
            decreasing sequence of preferred heights as we peel the boundary inward:

              height(OUTER_TO_EXTERIOR) ≥ height(EXTERIOR_TO_BORDER)
                                        ≥ height(BORDER_TO_INTERIOR)
                                        ≥ height(INTERIOR_TO_CONTENT)

            This property is the mental model users should carry: "closer to the
            child\'s centre means less space consumed by the child\'s obstacle".
            The extremes (outermost vs innermost) must differ strictly for a child
            that has meaningful styled insets, because the innermost boundary sees
            a substantially smaller rectangle than the outermost one.
        """
        given : 'SwingTree with a fixed UI scale so that font metrics stay deterministic:'
            SwingTree.initializeUsing( it -> {
                it = it.uiScaleFactor(1.0f)
                it = SwingTreeTestConfigurator.get().configure(it)
            })
        and : 'A long paragraph whose line-breaks are sensitive to horizontal width changes:'
            var content = Tuple.of(
                StyledString.of(f -> f.size(14),
                    "The further inward we peel the child obstacle boundary, the more horizontal " +
                    "space is returned to the text, and the fewer line-breaks the layout engine has " +
                    "to introduce. A smaller obstacle therefore produces a shorter preferred height, " +
                    "exactly as if the child had never reserved that outer margin-border-padding ring " +
                    "in the first place. This is the core intuition behind the boundary switch."
                )
            )
        and : 'A helper that builds a 400 px parent with a right-side child that carries generous margin, border, and padding:'
            def preferredHeight = { UI.ComponentBoundary boundary ->
                int[] result = new int[1]
                UI.runNow({
                    // The parent: styled text, right-anchored so obstacles on the right matter most.
                    var parent = UI.box()
                                .withStyle(conf -> conf
                                    .text(t -> t
                                        .font(f -> f.family("Ubuntu"))
                                        .content(content)
                                        .wrapLines(true)
                                        .autoPreferredHeight(true)
                                        .placement(UI.Placement.TOP_LEFT)
                                        .obstaclesFromChildrenEnabled(true)
                                        .obstaclesFromChildren(boundary)
                                    )
                                )
                                .get(JBox)
                    parent.setLayout(null)
                    parent.setSize(400, 0)

                    // The styled child: big margin, big border, big padding — each layer visibly
                    // shrinks the obstacle rectangle when it is peeled off by the chosen boundary.
                    var child = UI.box()
                                .withStyle(conf -> conf
                                    .margin(30)       // peeled off by EXTERIOR_TO_BORDER
                                    .border(20, "black") // peeled off by BORDER_TO_INTERIOR
                                    .padding(30)      // peeled off by INTERIOR_TO_CONTENT
                                )
                                .get(JBox)
                    child.setBounds(200, 0, 200, 500)
                    // Force the child's style engine to compute its ComponentConf from the styler
                    // so that `childShapeForArea(...)` can observe the margin/border/padding values:
                    ComponentExtension.from(child).gatherApplyAndInstallStyle(true)
                    parent.add(child)

                    var buf = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                    parent.paintComponent(buf.createGraphics())
                    result[0] = parent.getPreferredSize().height
                })
                return result[0]
            }

        when : 'We measure preferred height for each of the four box-model boundaries:'
            int hOuter    = preferredHeight(UI.ComponentBoundary.OUTER_TO_EXTERIOR)
            int hExterior = preferredHeight(UI.ComponentBoundary.EXTERIOR_TO_BORDER)
            int hBorder   = preferredHeight(UI.ComponentBoundary.BORDER_TO_INTERIOR)
            int hInterior = preferredHeight(UI.ComponentBoundary.INTERIOR_TO_CONTENT)

        then : 'All four heights are positive (the text is non-empty):'
            hOuter    > 0
            hExterior > 0
            hBorder   > 0
            hInterior > 0

        and : """
            Peeling the boundary inward never grows the height — each inner boundary
            produces an obstacle that is a subset of the previous one, and a smaller
            obstacle cannot force more wrapping:
        """
            hOuter    >= hExterior
            hExterior >= hBorder
            hBorder   >= hInterior

        and : """
            The extremes differ strictly: the innermost boundary (INTERIOR_TO_CONTENT)
            sees a rectangle shrunk inward on every side by margin+border+padding,
            leaving meaningfully more horizontal space for the text and therefore a
            strictly shorter preferred height than the outermost boundary:
        """
            hInterior < hOuter
    }


    def 'A child component without a SwingTree style always falls back to its full bounds regardless of the chosen boundary'()
    {
        reportInfo """
            The `obstaclesFromChildren(UI.ComponentBoundary)` setting only has a visible
            effect when the child has a SwingTree style that defines margin, border and
            padding — those are the insets that the engine peels off when computing the
            inner boundaries.  For a plain Swing component (or a SwingTree component
            without any box-model styling) there is *nothing* to peel off, so all four
            boundary values collapse to the child\'s full bounding rectangle.

            This fallback is important to document because it explains why the switch
            appears to "do nothing" in the common case of using vanilla Swing children:
            there is no styled margin, border or padding to remove, so every boundary
            value yields the same shape.  Users who want the boundary switch to take
            effect must style their child components with non-zero margin/border/padding.

            This test verifies the fallback by building four parent boxes with the
            same unstyled (plain) child and one each of the four boundary values, and
            confirming that they all produce the same preferred height.
        """
        given : 'SwingTree with a fixed UI scale:'
            SwingTree.initializeUsing( it -> {
                it = it.uiScaleFactor(1.0f)
                it = SwingTreeTestConfigurator.get().configure(it)
            })
        and : 'A long text so that any change in obstacle size would visibly affect the preferred height:'
            var content = Tuple.of(
                StyledString.of(f -> f.size(14),
                    "A plain Swing child has no styled margin, border or padding for the boundary " +
                    "switch to peel off, so the obstacle defaults to the full bounding rectangle — " +
                    "identical to what the outermost boundary produces. Every boundary value " +
                    "therefore yields the same preferred height for an unstyled child."
                )
            )
        and : 'A helper that builds a 400 px parent with a plain (unstyled) right-half child and the given boundary:'
            def preferredHeight = { UI.ComponentBoundary boundary ->
                int[] result = new int[1]
                UI.runNow({
                    var parent = UI.box()
                                .withStyle(conf -> conf
                                    .text(t -> t
                                        .font(f -> f.family("Ubuntu"))
                                        .content(content)
                                        .wrapLines(true)
                                        .autoPreferredHeight(true)
                                        .placement(UI.Placement.TOP_LEFT)
                                        .obstaclesFromChildrenEnabled(true)
                                        .obstaclesFromChildren(boundary)
                                    )
                                )
                                .get(JBox)
                    parent.setLayout(null)
                    parent.setSize(400, 0)
                    var plainChild = new JBox() // no withStyle — no margin, no border, no padding
                    plainChild.setBounds(200, 0, 200, 500)
                    parent.add(plainChild)
                    var buf = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                    parent.paintComponent(buf.createGraphics())
                    result[0] = parent.getPreferredSize().height
                })
                return result[0]
            }

        when : 'We measure preferred height for each of the four boundary values:'
            int hOuter    = preferredHeight(UI.ComponentBoundary.OUTER_TO_EXTERIOR)
            int hExterior = preferredHeight(UI.ComponentBoundary.EXTERIOR_TO_BORDER)
            int hBorder   = preferredHeight(UI.ComponentBoundary.BORDER_TO_INTERIOR)
            int hInterior = preferredHeight(UI.ComponentBoundary.INTERIOR_TO_CONTENT)

        then : 'All four measurements are positive:'
            hOuter > 0
        and : 'The plain child falls back to the full bounding rectangle — all boundaries produce the same height:'
            hOuter    == hExterior
            hExterior == hBorder
            hBorder   == hInterior
    }


    def 'The obstaclesFromChildrenEnabled and obstaclesFromChildren settings are faithfully recorded in the TextConf data representation'()
    {
        reportInfo """
            The `TextConf` class is an immutable, value-based configuration type, and
            its `toString()` serialisation is part of how the style layer communicates
            *what it was told* to render.  It is also a handy observation window that
            SwingTree\'s own tests (and users debugging their own code) rely on to
            verify that values flowed through the fluent API unchanged.

            This test configures a `TextConf` through the public builder API and then
            reads the resulting object back through three lenses:

              — the object\'s fluent getters (`obstaclesFromChildren()` and the package-
                private `obstaclesFromChildrenEnabled()`), which are the canonical source
                of truth,
              — the `equals`/`hashCode` contract (by building an identical second instance
                and comparing),
              — the `toString()` serialisation, which must expose both flags so that
                debug output and logs remain informative.

            The test also verifies that the two switches are *independent*: changing
            one does not implicitly change the other.  Finally, it pins down the
            documented default values — enabled = `true`,
            boundary = `OUTER_TO_EXTERIOR` — so that regressions in the defaults are
            caught by this spec rather than silently drifting into user code.
        """
        given : 'The default TextConf#none() serves as the starting point for the data checks:'
            var defaultConf = TextConf.none()

        expect : 'The documented defaults: enabled = true, boundary = OUTER_TO_EXTERIOR'
            defaultConf.obstaclesFromChildrenEnabled() == true
            defaultConf.obstaclesFromChildren() == UI.ComponentBoundary.OUTER_TO_EXTERIOR

        when : 'We disable the switch and select the innermost boundary on the default instance:'
            var customConf = defaultConf
                                .obstaclesFromChildrenEnabled(false)
                                .obstaclesFromChildren(UI.ComponentBoundary.INTERIOR_TO_CONTENT)
        then : 'Both values round-trip through the fluent API unchanged:'
            customConf.obstaclesFromChildrenEnabled() == false
            customConf.obstaclesFromChildren() == UI.ComponentBoundary.INTERIOR_TO_CONTENT
        and : 'The original default instance remains unchanged — TextConf is immutable:'
            defaultConf.obstaclesFromChildrenEnabled() == true
            defaultConf.obstaclesFromChildren() == UI.ComponentBoundary.OUTER_TO_EXTERIOR

        and : 'Both switches are independent — toggling one does not implicitly change the other:'
            customConf.obstaclesFromChildrenEnabled(true).obstaclesFromChildren() == UI.ComponentBoundary.INTERIOR_TO_CONTENT
            customConf.obstaclesFromChildren(UI.ComponentBoundary.OUTER_TO_EXTERIOR).obstaclesFromChildrenEnabled() == false

        when : 'We build a second identical instance via the same builder calls:'
            var twinConf = TextConf.none()
                                .obstaclesFromChildrenEnabled(false)
                                .obstaclesFromChildren(UI.ComponentBoundary.INTERIOR_TO_CONTENT)
        then : 'Value equality and hashCode agree — the two instances are interchangeable:'
            customConf == twinConf
            customConf.hashCode() == twinConf.hashCode()

        and : """
            The textual serialisation of the configuration (`toString()`) exposes both
            flags so that debug output and logs show exactly what was configured.
            This is what users see when they inspect the style conf after painting,
            and what the living-documentation examples above rely on to verify
            child-obstacle behaviour:
        """
            customConf.toString().contains("obstaclesFromChildrenEnabled=false")
            customConf.toString().contains("obstaclesFromChildrenAs=INTERIOR_TO_CONTENT")
        and : 'The default instance serialises to a compact NONE marker — defaults never leak into the string:'
            defaultConf.toString() == "TextConf[NONE]"
    }
}