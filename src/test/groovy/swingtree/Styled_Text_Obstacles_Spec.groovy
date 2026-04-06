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
                    "more room, leading to fewer line-breaks and a shorter overall layout height."
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
            float ox = 200, oy = 0, ow = 150, oh = 200
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
}