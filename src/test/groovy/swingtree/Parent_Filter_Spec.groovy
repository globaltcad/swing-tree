package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Timeout
import spock.lang.Title
import swingtree.threading.EventProcessor
import utility.Utility

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
}
