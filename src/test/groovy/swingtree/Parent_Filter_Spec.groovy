package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Timeout
import spock.lang.Title
import swingtree.threading.EventProcessor
import utility.Utility

import swingtree.layout.Size
import swingtree.style.FilterConf

import javax.swing.JPanel
import java.awt.image.BufferedImage
import java.util.concurrent.TimeUnit

@Title("Filtering the Parent Behind a Component")
@Narrative('''

    A component can be told to show its own parent through itself, put through a filter on the
    way there. That is what `parentFilter(..)` does, and it is how a pane of frosted glass is
    made: the pane itself is barely more than a tint, and everything which makes it read as
    glass comes from the window behind it arriving blurred.

    ```
    UI.panel().withStyle( it -> it.parentFilter( f -> f.blur(6) ) )
    ```

    A blur is only one of the things the parent can be put through on the way. `f.scale(..)`
    magnifies it, `f.offset(..)` slides it, `f.kernel(..)` runs an arbitrary convolution over
    it, and `f.area(..)` says which part of the component the result is allowed to show in.

    What all of them have in common, and what the scenarios below are about, is that the
    picture a pane shows is a picture **of its parent**. Its siblings are not part of what it
    is looking at. So a second pane which happens to filter the same parent may not change
    what the first one shows - not by a single pixel, and no matter how many of them there
    are. A pane which grows blurrier the more neighbours it has is showing something which is
    not there.

    Two notes on how these scenarios are built, both of which matter for reading them.

    The parent is painted in fine-grained noise rather than in a colour or a gradient. Blurring
    something smooth gives back very nearly the same smooth thing, so a smooth parent would
    hide exactly the defect being looked for. Noise is the opposite: it is all detail, and
    losing it twice over is plain to see.

    The panes are compared against a *reference rendering of the very same layout*, in which
    only the pane under test filters. Switching a filter off does not move anything, so the
    pane occupies the same rectangle in both renderings and the two can be compared pixel for
    pixel.

    The last scenario is about a different way the same picture can go wrong. Swing repaints as
    little as it can get away with: move the pointer onto a button and the only thing redrawn
    is that button's rectangle, expressed as a clip on the graphics everything is painted
    through. A filter still has to read the whole of the parent underneath its pane, because a
    blur gathers colour from further away than the pixel it is computing. So what a pane shows
    inside the repainted rectangle has to be the same whether that rectangle was the whole
    window or a corner of one button.

''')
@Subject([UI])
@Timeout(value = 90, unit = TimeUnit.SECONDS)
class Parent_Filter_Spec extends Specification
{
    def setup() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
        SwingTree.get().setUiScaleFactor(1f)
    }

    def cleanup() {
        SwingTree.clear()
    }

    def 'Use `parentFilter(..)` on two panes over one parent, and neither changes what the other shows.'()
    {
        reportInfo """
            Two panes sit side by side over a noisy parent, and both are asked to blur it.
            Each of them is then compared against a rendering in which it is the *only* pane
            filtering - which is to say, against what it would look like if its neighbour were
            not there at all.

            Both panes have to pass. A pane is looking at the parent, and the parent does not
            change when a sibling is told to blur it, so there is no reading of `parentFilter`
            under which the answer depends on the neighbour.

            Testing both of them rather than one is what makes this scenario complete. Swing
            paints a container's children in reverse order of how they were added, so exactly
            one of these two panes is painted after the other has had its turn - and a defect
            in which a pane damages what it filtered would only ever show up in that one.
            Which of the two that is, is not something the scenario needs to know.
        """
        given : """
            A parent painted in fine-grained noise, holding two equally sized panes side by
            side. A pane has no background of its own, so everything it shows arrives through
            the filter - and whether it filters at all is the one thing which varies here.
            Everything else stays put, so switching a filter off does not move a pane.
        """
            var twoPanesWhere = { boolean leftFilters, boolean rightFilters ->
                var leftPane = leftFilters
                        ? UI.panel().withStyle( it -> it
                                .backgroundColor(UI.Color.TRANSPARENT)
                                .parentFilter( f -> f.blur(6) ) )
                        : UI.panel().withStyle( it -> it
                                .backgroundColor(UI.Color.TRANSPARENT) )
                var rightPane = rightFilters
                        ? UI.panel().withStyle( it -> it
                                .backgroundColor(UI.Color.TRANSPARENT)
                                .parentFilter( f -> f.blur(6) ) )
                        : UI.panel().withStyle( it -> it
                                .backgroundColor(UI.Color.TRANSPARENT) )
                var parent =
                        UI.panel("fill, ins 0, gap 0")
                        .withStyle( it -> it
                            .backgroundColor(UI.Color.BLACK)
                            .noise( n -> n
                                .function(UI.NoiseType.STOCHASTIC)
                                .scale(0.3)
                                .colors(UI.Color.BLACK, UI.Color.WHITE) ) )
                        .add("grow", leftPane)
                        .add("grow", rightPane)
                        .get(JPanel)
                parent.setSize(240, 160)
                parent.doLayout()
                return Utility.renderSingleComponent(parent)
            }
        and : """
            A way of counting how many pixels of one half of a rendering differ from the same
            half of another. A channel has to be off by more than 8 out of 255 to count, which
            is far below what the defect this is looking for is worth: a pane which blurred an
            already blurred parent loses detail the second time round, and here that costs it
            up to 28 out of 255. It is also far above anything rounding can produce, since the
            two renderings being compared come out of the same code on the same machine.
        """
            var pixelsDifferingBetween = { BufferedImage a, BufferedImage b, int fromX, int toX ->
                int count = 0
                for ( int y = 0; y < a.height; y++ )
                    for ( int x = fromX; x < toX; x++ )
                        for ( int channelShift : [0, 8, 16, 24] ) // blue, green, red and alpha
                            if ( Math.abs(
                                    ((a.getRGB(x, y) >> channelShift) & 0xff) -
                                    ((b.getRGB(x, y) >> channelShift) & 0xff)
                                 ) > 8 ) {
                                count++
                                break
                            }
                return count
            }

        when : 'Both panes are asked to blur the parent behind them:'
            var bothFiltering = twoPanesWhere(true, true)

        and : 'And each of them is rendered again as the only pane filtering, to be compared against:'
            var onlyTheLeftFiltering  = twoPanesWhere(true, false)
            var onlyTheRightFiltering = twoPanesWhere(false, true)

        and : 'The three renderings are all of the same 240 by 160 parent, so the panes line up:'
            var leftHalf  = [0,   120]
            var rightHalf = [120, 240]

        then : """
            The filter is doing something in the first place. A pane which quietly stopped
            filtering would pass every other assertion here, so this one is what rules that
            out: the blurred left pane is compared against a rendering of the same layout in
            which nothing filters at all, and the two are nothing like each other.
        """
            pixelsDifferingBetween(onlyTheLeftFiltering, twoPanesWhere(false, false), leftHalf[0], leftHalf[1]) > 15000

        and : 'The left pane shows exactly what it shows when the right one does not filter.'
            pixelsDifferingBetween(bothFiltering, onlyTheLeftFiltering, leftHalf[0], leftHalf[1]) == 0

        and : 'And the right pane shows exactly what it shows when the left one does not filter.'
            pixelsDifferingBetween(bothFiltering, onlyTheRightFiltering, rightHalf[0], rightHalf[1]) == 0
    }

    def 'A pane using `parentFilter(..)` shows the same thing however many siblings also filter. (#description)'(
        String description, boolean secondFilters, boolean thirdFilters, boolean fourthFilters
    ) {
        reportInfo """
            The previous scenario asks whether one neighbour can reach a pane. This one asks
            whether the reach adds up.

            Four panes sit in a row over the same noisy parent. The first of them always blurs
            it, and the other three are switched between filtering and not, one row of the
            table at a time. Whatever the other three are doing, the first pane is looking at
            the same parent through the same blur, so it has to come out identical every time.

            A defect in which filtering damages what was filtered would not merely show up
            here, it would *accumulate*: each pane which filtered before this one would have
            left the parent that bit more blurred, and the pane would end up showing the parent
            blurred as many times over as it has neighbours.
        """
        given : """
            The same fine-grained noise as before, this time under four equally sized panes.
            The first pane is the one under test and always filters; the other three filter or
            not according to the row of the table below.
        """
            var fourPanesWhere = { boolean second, boolean third, boolean fourth ->
                var paneFiltering = { ->
                        UI.panel().withStyle( it -> it
                            .backgroundColor(UI.Color.TRANSPARENT)
                            .parentFilter( f -> f.blur(6) ) )
                    }
                var paneNotFiltering = { ->
                        UI.panel().withStyle( it -> it
                            .backgroundColor(UI.Color.TRANSPARENT) )
                    }
                var parent =
                        UI.panel("fill, ins 0, gap 0")
                        .withStyle( it -> it
                            .backgroundColor(UI.Color.BLACK)
                            .noise( n -> n
                                .function(UI.NoiseType.STOCHASTIC)
                                .scale(0.3)
                                .colors(UI.Color.BLACK, UI.Color.WHITE) ) )
                        .add("grow", paneFiltering())
                        .add("grow", second ? paneFiltering() : paneNotFiltering())
                        .add("grow", third  ? paneFiltering() : paneNotFiltering())
                        .add("grow", fourth ? paneFiltering() : paneNotFiltering())
                        .get(JPanel)
                parent.setSize(240, 160)
                parent.doLayout()
                return Utility.renderSingleComponent(parent)
            }
        and : 'The same pixel count as before, over the first of the four panes.'
            var pixelsDifferingInTheFirstPane = { BufferedImage a, BufferedImage b ->
                int count = 0
                for ( int y = 0; y < a.height; y++ )
                    for ( int x = 0; x < 60; x++ ) // the first of four panes across 240 pixels
                        for ( int channelShift : [0, 8, 16, 24] ) // blue, green, red and alpha
                            if ( Math.abs(
                                    ((a.getRGB(x, y) >> channelShift) & 0xff) -
                                    ((b.getRGB(x, y) >> channelShift) & 0xff)
                                 ) > 8 ) {
                                count++
                                break
                            }
                return count
            }
        and : 'The pane under test, rendered with all three of its neighbours leaving the parent alone.'
            var undisturbed = fourPanesWhere(false, false, false)

        when : 'The three neighbours are set to filter as the row of the table says:'
            var candidate = fourPanesWhere(secondFilters, thirdFilters, fourthFilters)

        then : 'The pane under test is unchanged.'
            pixelsDifferingInTheFirstPane(undisturbed, candidate) == 0

        where : 'The three neighbours take every combination of filtering and not.'
            description                  | secondFilters | thirdFilters | fourthFilters
            'one neighbour filters'      | true          | false        | false
            'a further neighbour does'   | true          | true         | false
            'all three neighbours do'    | true          | true         | true
            'only the furthest one does' | false         | false        | true
    }

    def 'A pane using `parentFilter(..)` shows the same thing when only part of the window is repainted. (#description)'(
        String description, int clipX, int clipY, int clipWidth, int clipHeight
    ) {
        reportInfo """
            Swing hands a component a graphics whose clip says which part of it is worth
            redrawing, and redraws nothing else. Moving the pointer onto one button in a window
            full of them sets that clip to the button, and every ancestor it is painted through
            gets the same narrow clip.

            The clip says what may be **drawn**. It does not say what may be **read**. A pane
            blurring its parent needs the parent for a good distance around every pixel it
            produces, including the parent from outside the rectangle being redrawn - so a
            filter which only ever looks at the freshly repainted strip is looking at a parent
            with a hole around it, and paints the hole.

            The rows below repaint through three different rectangles: one lying wholly inside
            the pane, one straddling its edge, and - as a control which must pass either way -
            one covering the whole component, which is simply the full repaint again.
        """
        given : """
            A parent painted in fine-grained noise, with a single pane over it blurring what is
            behind it. The parent is inset by 40 pixels all round, so the pane sits well away
            from the parent's own edges and nothing about this scenario depends on what a filter
            does when it runs out of parent to read.
        """
            var parent =
                    UI.panel("fill, ins 40")
                    .withStyle( it -> it
                        .backgroundColor(UI.Color.BLACK)
                        .noise( n -> n
                            .function(UI.NoiseType.STOCHASTIC)
                            .scale(0.3)
                            .colors(UI.Color.BLACK, UI.Color.WHITE) ) )
                    .add("grow", UI.panel().withStyle( it -> it
                            .backgroundColor(UI.Color.TRANSPARENT)
                            .parentFilter( f -> f.blur(6) ) ))
                    .get(JPanel)
            parent.setSize(240, 160)
            parent.doLayout()
        and : """
            A way of painting that parent through a clip of our choosing, which is the one thing
            `Utility.renderSingleComponent` cannot be asked for: it always paints the component
            whole, and a partial repaint is exactly what is under test here.
        """
            var paintedThroughClip = { int x, int y, int width, int height ->
                return UI.runAndGet(() -> {
                    var image = Utility.createDeterministicImage(240, 160)
                    var graphics = Utility.createDeterministicGraphics(image)
                    graphics.setClip(x, y, width, height)
                    Utility.paintWithoutWindow(parent, graphics)
                    graphics.dispose()
                    return image
                })
            }
        and : 'The same pixel count as before, over a rectangle of our choosing.'
            var pixelsDifferingInside = { BufferedImage a, BufferedImage b,
                                          int x, int y, int width, int height ->
                int count = 0
                for ( int row = y; row < y + height; row++ )
                    for ( int column = x; column < x + width; column++ )
                        for ( int channelShift : [0, 8, 16, 24] ) // blue, green, red and alpha
                            if ( Math.abs(
                                    ((a.getRGB(column, row) >> channelShift) & 0xff) -
                                    ((b.getRGB(column, row) >> channelShift) & 0xff)
                                 ) > 8 ) {
                                count++
                                break
                            }
                return count
            }
        and : 'The whole component painted in one go, which is what a partial repaint has to agree with.'
            var fullRepaint = paintedThroughClip(0, 0, 240, 160)

        when : 'Only the rectangle of this row is repainted:'
            var partialRepaint = paintedThroughClip(clipX, clipY, clipWidth, clipHeight)

        then : 'Inside that rectangle, the two are the same picture.'
            pixelsDifferingInside(fullRepaint, partialRepaint,
                                  clipX, clipY, clipWidth, clipHeight) == 0

        where : 'The repainted rectangle sits inside the pane, across its edge, and around everything.'
            description                      | clipX | clipY | clipWidth | clipHeight
            'a rectangle inside the pane'    | 100   | 70    | 40        | 30
            'a rectangle across its edge'    | 30    | 60    | 60        | 40
            'the whole component (control)'  | 0     | 0     | 240       | 160
    }

    def 'Repainting part of a window over what is already on it leaves a `parentFilter(..)` pane alone. (#description)'(
        String description, int clipX, int clipY, int clipWidth, int clipHeight
    ) {
        reportInfo """
            This is the previous scenario as a user meets it. A window is already on screen, and
            something small happens on it - the pointer arrives on a button, a caret blinks, a
            progress bar ticks. Swing does not redraw the window; it redraws that rectangle,
            over what is already there.

            Nothing about the window has changed here, so redrawing any rectangle of it has to
            leave it exactly as it was. That makes every pixel of the window an assertion,
            rather than only the ones inside the rectangle, and so it catches two things at
            once: a pane which paints the wrong thing inside the rectangle, and a pane which
            paints anything at all outside it.

            When this fails, what the user sees is a rectangular stain around whatever they
            touched, which stays until something forces the whole window to be redrawn.
        """
        given : """
            A parent painted in fine-grained noise, with two panes over it blurring what is
            behind them - two, because a single pane cannot show whether the repaint of one
            damages another.
        """
            var parent =
                    UI.panel("fill, ins 20, gap 20")
                    .withStyle( it -> it
                        .backgroundColor(UI.Color.BLACK)
                        .noise( n -> n
                            .function(UI.NoiseType.STOCHASTIC)
                            .scale(0.3)
                            .colors(UI.Color.BLACK, UI.Color.WHITE) ) )
                    .add("grow", UI.panel().withStyle( it -> it
                            .backgroundColor(UI.Color.TRANSPARENT)
                            .parentFilter( f -> f.blur(6) ) ))
                    .add("grow", UI.panel().withStyle( it -> it
                            .backgroundColor(UI.Color.TRANSPARENT)
                            .parentFilter( f -> f.blur(6) ) ))
                    .get(JPanel)
            parent.setSize(240, 160)
            parent.doLayout()
        and : 'The window as it already sits on screen, which is simply a full repaint of it.'
            var onScreen = UI.runAndGet(() -> {
                var image = Utility.createDeterministicImage(240, 160)
                var graphics = Utility.createDeterministicGraphics(image)
                Utility.paintWithoutWindow(parent, graphics)
                graphics.dispose()
                return image
            })

        when : """
            Swing redraws one rectangle of it, the way it does when something small happens:
            over what is already there, through a clip which says how far the redraw reaches.
        """
            var afterRedraw = UI.runAndGet(() -> {
                var image = Utility.createDeterministicImage(240, 160)
                var graphics = Utility.createDeterministicGraphics(image)
                graphics.drawImage(onScreen, 0, 0, null)
                graphics.setClip(clipX, clipY, clipWidth, clipHeight)
                Utility.paintWithoutWindow(parent, graphics)
                graphics.dispose()
                return image
            })

        and : 'Every pixel of the window is compared, not only the ones which were redrawn.'
            int pixelsChanged = 0
            for ( int y = 0; y < 160; y++ )
                for ( int x = 0; x < 240; x++ )
                    for ( int channelShift : [0, 8, 16, 24] ) // blue, green, red and alpha
                        if ( Math.abs(
                                ((onScreen.getRGB(x, y) >> channelShift) & 0xff) -
                                ((afterRedraw.getRGB(x, y) >> channelShift) & 0xff)
                             ) > 8 ) {
                            pixelsChanged++
                            break
                        }

        then : 'Nothing changed, because nothing had changed.'
            pixelsChanged == 0

        where : 'The redrawn rectangle lands on one pane, on both of them, and between them.'
            description                  | clipX | clipY | clipWidth | clipHeight
            'a spot on the left pane'    | 60    | 70    | 20        | 20
            'a spot on the right pane'   | 160   | 70    | 20        | 20
            'a strip across both'        | 20    | 75    | 200       | 10
            'the gap between them'       | 110   | 20    | 20        | 120
    }

    def 'A pane keeps what it shows however `parentFilter(..)` is configured, whoever else filters. (#description)'(
        String description, Closure<FilterConf> filter
    ) {
        reportInfo """
            The scenarios above put one filter through its paces. This one puts every kind of
            filter through the first of them.

            A blur is only the most familiar way to configure `parentFilter(..)`. The parent can
            also be slid with `offset(..)`, magnified with `scale(..)`, run through a convolution
            matrix of one's own with `kernel(..)`, and confined to one area of the component with
            `area(..)` - and any of those may be combined with a blur. Each is a different route
            through the filtering code, so the guarantee the previous scenarios establish for a
            blur has to be established for each of them separately: what a pane shows is a
            picture of its parent, and a sibling looking at the same parent cannot change it.

            The blur radii in the table are chosen to land on either side of the point at which
            a wide blur stops being convolved pixel by pixel. Beyond a certain radius the parent
            is shrunk before it is blurred and stretched back out afterwards, because a blur
            that wide throws away the detail a smaller raster could not have held anyway. That
            is an entirely different route through the same method, and a radius from each side
            of the threshold is what keeps both of them covered.
        """
        given : """
            Two panes side by side over a noisy parent, as in the first scenario, except that
            what the panes are asked to do is now whatever the row of the table says.
        """
            var twoPanesWhere = { boolean leftFilters, boolean rightFilters ->
                var pane = { boolean filters ->
                        filters
                            ? UI.panel().withStyle( it -> it
                                    .backgroundColor(UI.Color.TRANSPARENT)
                                    .parentFilter( f -> filter(f) ) )
                            : UI.panel().withStyle( it -> it
                                    .backgroundColor(UI.Color.TRANSPARENT) )
                    }
                var parent =
                        UI.panel("fill, ins 0, gap 0")
                        .withStyle( it -> it
                            .backgroundColor(UI.Color.BLACK)
                            .noise( n -> n
                                .function(UI.NoiseType.STOCHASTIC)
                                .scale(0.3)
                                .colors(UI.Color.BLACK, UI.Color.WHITE) ) )
                        .add("grow", pane(leftFilters))
                        .add("grow", pane(rightFilters))
                        .get(JPanel)
                parent.setSize(240, 160)
                parent.doLayout()
                return Utility.renderSingleComponent(parent)
            }
        and : 'The same pixel count as in the first scenario, over a half of the rendering.'
            var pixelsDifferingBetween = { BufferedImage a, BufferedImage b, int fromX, int toX ->
                int count = 0
                for ( int y = 0; y < a.height; y++ )
                    for ( int x = fromX; x < toX; x++ )
                        for ( int channelShift : [0, 8, 16, 24] ) // blue, green, red and alpha
                            if ( Math.abs(
                                    ((a.getRGB(x, y) >> channelShift) & 0xff) -
                                    ((b.getRGB(x, y) >> channelShift) & 0xff)
                                 ) > 8 ) {
                                count++
                                break
                            }
                return count
            }

        when : 'Both panes filter the parent as this row says:'
            var bothFiltering = twoPanesWhere(true, true)

        and : 'And each is rendered again as the only pane filtering, to be compared against:'
            var onlyTheLeftFiltering  = twoPanesWhere(true, false)
            var onlyTheRightFiltering = twoPanesWhere(false, true)

        then : """
            This configuration does something in the first place. Without this, a filter which
            silently did nothing at all would satisfy every other assertion here.
        """
            pixelsDifferingBetween(onlyTheLeftFiltering, twoPanesWhere(false, false), 0, 120) > 1000

        and : 'The left pane shows exactly what it shows when the right one does not filter.'
            pixelsDifferingBetween(bothFiltering, onlyTheLeftFiltering, 0, 120) == 0

        and : 'And the right pane shows exactly what it shows when the left one does not filter.'
            pixelsDifferingBetween(bothFiltering, onlyTheRightFiltering, 120, 240) == 0

        where : 'The filter is configured every way the style API allows.'
            description                        | filter
            'a narrow blur'                    | { f -> f.blur(4) }
            'a blur wide enough to be shrunk'  | { f -> f.blur(24) }
            'a blur wider than the shrinking'  | { f -> f.blur(64) }
            'a convolution kernel of our own'  | { f -> f.kernel(Size.of(3, 3), 0d,1d,0d, 1d,2d,1d, 0d,1d,0d) }
            'a kernel and a wide blur'         | { f -> f.kernel(Size.of(3, 3), 1d,1d,1d, 1d,1d,1d, 1d,1d,1d).blur(24) }
            'a wide blur and an offset'        | { f -> f.blur(24).offset(12, -8) }
            'a wide blur and a scale'          | { f -> f.blur(24).scale(1.4, 1.4) }
            'a wide blur inside the interior'  | { f -> f.blur(24).area(UI.ComponentArea.INTERIOR) }
    }

    def 'A pane shows the same thing under a partial repaint however `parentFilter(..)` is configured. (#description)'(
        String description, Closure<FilterConf> filter
    ) {
        reportInfo """
            This is the partial repaint scenario above, asked of every kind of filter rather
            than of a blur alone.

            It is the sharper of the two questions for a wide blur, because of *how* a wide blur
            is computed. The parent is shrunk before it is convolved, and the region which is
            shrunk is the part of the parent the pane needs - which is smaller when Swing asks
            for a smaller rectangle to be redrawn. So the pixels a filter samples the parent at
            are decided by something which changes from one repaint to the next, and a filter
            which let that reach its output would give a pane which shifts, very slightly, every
            time the pointer passes over it.
        """
        given : """
            A parent painted in fine-grained noise, inset by 40 pixels all round, with a single
            pane over it filtering as this row says.
        """
            var parent =
                    UI.panel("fill, ins 40")
                    .withStyle( it -> it
                        .backgroundColor(UI.Color.BLACK)
                        .noise( n -> n
                            .function(UI.NoiseType.STOCHASTIC)
                            .scale(0.3)
                            .colors(UI.Color.BLACK, UI.Color.WHITE) ) )
                    .add("grow", UI.panel().withStyle( it -> it
                            .backgroundColor(UI.Color.TRANSPARENT)
                            .parentFilter( f -> filter(f) ) ))
                    .get(JPanel)
            parent.setSize(240, 160)
            parent.doLayout()
        and : 'The same way of painting it through a clip of our choosing as in the scenario above.'
            var paintedThroughClip = { int x, int y, int width, int height ->
                return UI.runAndGet(() -> {
                    var image = Utility.createDeterministicImage(240, 160)
                    var graphics = Utility.createDeterministicGraphics(image)
                    graphics.setClip(x, y, width, height)
                    Utility.paintWithoutWindow(parent, graphics)
                    graphics.dispose()
                    return image
                })
            }
        and : 'The same pixel count, over a rectangle of our choosing.'
            var pixelsDifferingInside = { BufferedImage a, BufferedImage b,
                                          int x, int y, int width, int height ->
                int count = 0
                for ( int row = y; row < y + height; row++ )
                    for ( int column = x; column < x + width; column++ )
                        for ( int channelShift : [0, 8, 16, 24] ) // blue, green, red and alpha
                            if ( Math.abs(
                                    ((a.getRGB(column, row) >> channelShift) & 0xff) -
                                    ((b.getRGB(column, row) >> channelShift) & 0xff)
                                 ) > 8 ) {
                                count++
                                break
                            }
                return count
            }
        and : 'The whole component painted in one go, which is what a partial repaint has to agree with.'
            var fullRepaint = paintedThroughClip(0, 0, 240, 160)

        when : 'Only a rectangle well inside the pane is repainted:'
            var insideThePane = paintedThroughClip(100, 70, 40, 30)

        and : 'And then one straddling the edge of the pane:'
            var acrossTheEdge = paintedThroughClip(30, 60, 60, 40)

        then : 'Inside either rectangle, what the pane shows is what the full repaint showed.'
            pixelsDifferingInside(fullRepaint, insideThePane, 100, 70, 40, 30) == 0
            pixelsDifferingInside(fullRepaint, acrossTheEdge, 30, 60, 60, 40) == 0

        where : 'The filter is configured every way the style API allows.'
            description                        | filter
            'a narrow blur'                    | { f -> f.blur(4) }
            'a blur wide enough to be shrunk'  | { f -> f.blur(24) }
            'a blur wider than the shrinking'  | { f -> f.blur(64) }
            'a convolution kernel of our own'  | { f -> f.kernel(Size.of(3, 3), 0d,1d,0d, 1d,2d,1d, 0d,1d,0d) }
            'a kernel and a wide blur'         | { f -> f.kernel(Size.of(3, 3), 1d,1d,1d, 1d,1d,1d, 1d,1d,1d).blur(24) }
            'a wide blur and an offset'        | { f -> f.blur(24).offset(12, -8) }
            'a wide blur and a scale'          | { f -> f.blur(24).scale(1.4, 1.4) }
            'a wide blur inside the interior'  | { f -> f.blur(24).area(UI.ComponentArea.INTERIOR) }
    }

    def 'A wide blur still reads as a blur however far the shrinking goes. (#description)'(
        String description, int blurRadius
    ) {
        reportInfo """
            The scenarios above are about *repeatability*: a pane shows a stable picture of its
            parent. This one is about *effect*. A blur which is computed on a shrunk raster and
            stretched back out still has to read as a blur - the wider the radius, the more of
            the fine detail it has to remove. The rows land on the points where the shrinking
            grows: crossing into it (8), a step deep (16), at the cap (64) and beyond it (100).
            A blur which silently stopped blurring at any of them would pass every repeatability
            scenario above while showing a sharp parent.
        """
        given : """
            A single pane spanning an entire noisy parent, filtering with this row's radius. The
            pane covers all of the parent, so the region measured below stays well inside even
            the widest blur's convolved band.
        """
            var parentWith = { boolean filters, int radius ->
                var parent =
                        UI.panel("fill, ins 0, gap 0")
                        .withStyle( it -> it
                            .backgroundColor(UI.Color.BLACK)
                            .noise( n -> n
                                .function(UI.NoiseType.STOCHASTIC)
                                .scale(0.3)
                                .colors(UI.Color.BLACK, UI.Color.WHITE) ) )
                        .add("grow", UI.panel().withStyle( it -> {
                                if ( filters )
                                    return it
                                        .backgroundColor(UI.Color.TRANSPARENT)
                                        .parentFilter( f -> f.blur(radius) )
                                return it.backgroundColor(UI.Color.TRANSPARENT)
                            } ))
                        .get(JPanel)
                parent.setSize(480, 320)
                parent.doLayout()
                return Utility.renderSingleComponent(parent)
            }
        and : 'A way of measuring fine detail: the mean difference between horizontally adjacent pixels.'
            var fineDetailOf = { BufferedImage img, int x, int y, int w, int h ->
                double total = 0
                long   n     = 0
                for ( int row = y; row < y + h; row++ )
                    for ( int col = x; col < x + w - 1; col++ ) {
                        total += Math.abs(
                                    (img.getRGB(col, row) & 0xff) -
                                    (img.getRGB(col + 1, row) & 0xff) )
                        n++
                    }
                return n == 0 ? 0 : total / n
            }
        when : 'The same parent, once behind a sharp pane and once behind this blur:'
            var raw     = parentWith(false, 0)
            var blurred = parentWith(true, blurRadius)
        then : """
            The blurred pane has kept only a small fraction of the parent's fine detail. This
            is the point of a low pass, and it is what the shrinking has to keep putting out
            no matter how far the radius has gone.
        """
            fineDetailOf(blurred, 150, 100, 180, 120) * 2 < fineDetailOf(raw, 150, 100, 180, 120)

        where : 'A radius from each of the shrinking tiers.'
            description                       | blurRadius
            'crossing into the shrinking'     | 8
            'a step deep'                     | 16
            'beyond where the cap is reached' | 100
    }

    def 'A wider blur is not sharper than a narrower one while both are meaningful. (#description)'(
        String description, int narrow, int wide
    ) {
        reportInfo """
            The shrinking computes a wide blur by convolving a smaller kernel on a shrunk raster
            and stretching the result back out. That is an approximation, and the approximation
            is only acceptable if it does not *sharpen* the parent: a pane told to blur more has
            to look more blurred, or the control reads the wrong way. Between radii where the
            blur is still visually meaningful this has to hold strictly. A defect which made the
            shrinking re-introduce detail would pass the repeatability scenarios above.
        """
        given : 'A pane over a full noisy parent, rendered at each of the two radii.'
            var parentWith = { int radius ->
                var parent =
                        UI.panel("fill, ins 0, gap 0")
                        .withStyle( it -> it
                            .backgroundColor(UI.Color.BLACK)
                            .noise( n -> n
                                .function(UI.NoiseType.STOCHASTIC)
                                .scale(0.3)
                                .colors(UI.Color.BLACK, UI.Color.WHITE) ) )
                        .add("grow", UI.panel().withStyle( it -> it
                                .backgroundColor(UI.Color.TRANSPARENT)
                                .parentFilter( f -> f.blur(radius) ) ))
                        .get(JPanel)
                parent.setSize(480, 320)
                parent.doLayout()
                return Utility.renderSingleComponent(parent)
            }
        and : 'The same fine-detail measure as above.'
            var fineDetailOf = { BufferedImage img, int x, int y, int w, int h ->
                double total = 0
                long   n     = 0
                for ( int row = y; row < y + h; row++ )
                    for ( int col = x; col < x + w - 1; col++ ) {
                        total += Math.abs(
                                    (img.getRGB(col, row) & 0xff) -
                                    (img.getRGB(col + 1, row) & 0xff) )
                        n++
                    }
                return n == 0 ? 0 : total / n
            }
        when : 'Both radii are rendered from the very same parent:'
            var narrower = parentWith(narrow)
            var wider    = parentWith(wide)
        then : 'The wider radius has not retained more fine detail than the narrower one.'
            fineDetailOf(wider, 150, 100, 180, 120) <= fineDetailOf(narrower, 150, 100, 180, 120) * 1.5 + 0.2

        where : 'A pair straddling each of the meaningful shrink transitions.'
            description                            | narrow | wide
            'crossing into the shrinking'          | 8      | 16
            'crossing a second shrink'             | 16     | 32
    }

    def 'A wide blur works on a pane flush against the far edge of its parent.'() {
        reportInfo """
            A region is blured read well beyond the pixels it is drawn into. For a pane that
            lies flush against the far edge of its parent that padding reaches past the parent,
            where there are no pixels to read - the shrinking grows the window onto a grid,
            and the grown cells past the far edge simply have nothing to contribute. This has
            to be handled without an exception and without the pane changing what it shows
            when only part of it is repainted.
        """
        given : """
            A noisy parent with a single wide-blurred pane pinned into its bottom-right corner,
            so the blur's read-ahead padding is cut short on two sides at once.
        """
            var parent =
                    UI.panel("fill, ins 0, gap 0")
                    .withStyle( it -> it
                        .backgroundColor(UI.Color.BLACK)
                        .noise( n -> n
                            .function(UI.NoiseType.STOCHASTIC)
                            .scale(0.3)
                            .colors(UI.Color.BLACK, UI.Color.WHITE) ) )
                    .add("pos 360 200",
                        UI.panel()
                        .withPrefSize(120, 120)
                        .withStyle( it -> it
                            .backgroundColor(UI.Color.TRANSPARENT)
                            .parentFilter( f -> f.blur(24) ) ) )
                    .get(JPanel)
            parent.setSize(480, 320)
            parent.doLayout()
        and : 'The same way of painting it through a clip of our choosing as above.'
            var paintedThroughClip = { int x, int y, int width, int height ->
                return UI.runAndGet(() -> {
                    var image = Utility.createDeterministicImage(480, 320)
                    var graphics = Utility.createDeterministicGraphics(image)
                    graphics.setClip(x, y, width, height)
                    Utility.paintWithoutWindow(parent, graphics)
                    graphics.dispose()
                    return image
                })
            }
        and : 'The same pixel count, over a rectangle of our choosing.'
            var pixelsDifferingInside = { BufferedImage a, BufferedImage b,
                                          int x, int y, int width, int height ->
                int count = 0
                for ( int row = y; row < y + height; row++ )
                    for ( int column = x; column < x + width; column++ )
                        for ( int channelShift : [0, 8, 16, 24] ) // blue, green, red and alpha
                            if ( Math.abs(
                                    ((a.getRGB(column, row) >> channelShift) & 0xff) -
                                    ((b.getRGB(column, row) >> channelShift) & 0xff)
                                 ) > 8 ) {
                                count++
                                break
                            }
                return count
            }
        when : 'The whole component is painted in one go:'
            var fullRepaint = paintedThroughClip(0, 0, 480, 320)
        and : 'And then only a rectangle partway down and in across the corner pane:'
            var patched = paintedThroughClip(380, 220, 60, 50)
        then : 'Painting at all did not throw, and the pane shows the same thing under a partial repaint.'
            pixelsDifferingInside(fullRepaint, patched, 380, 220, 60, 50) == 0
    }
}
