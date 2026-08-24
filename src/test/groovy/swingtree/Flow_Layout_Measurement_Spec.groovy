package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.layout.ResponsiveGridFlowLayout
import swingtree.threading.EventProcessor

import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JPanel
import java.awt.Dimension

@Title("Flow Layout Measurement")
@Narrative('''

    A wrapping layout is asked how big it would like to be far more often than it is
    actually laid out. A scroll pane alone asks several times per pass while it works
    out whether it needs scroll bars, and a responsive grid nested inside another one
    is asked once more for every measurement of its parent. Answering is expensive:
    it walks every child, runs the cell configurator for each of them and descends
    into any nested grid. So the same question being asked five times is five times
    the work.

    The `ResponsiveGridFlowLayout` therefore remembers the answer it last gave. The
    rule for handing it out again is the one Swing itself applies to the size cached
    by `Container.getPreferredSize()`: **a remembered measurement is only ever reused
    while the container is `valid`**, and it is thrown away as soon as Swing reports
    a change through `LayoutManager2.invalidateLayout`.

    Every scenario below is about that answer never going stale, and about it really
    being reused when it may be. Note that a container has to be *realised* -- placed
    in a window which was packed -- before any of this is observable at all. A
    detached panel is never `valid`, so it is measured afresh every single time.

''')
@Subject([ResponsiveGridFlowLayout])
class Flow_Layout_Measurement_Spec extends Specification
{
    def setup() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
        SwingTree.get().setUiScaleFactor(1f)
    }

    def cleanup() {
        SwingTree.clear()
    }

    def 'A remembered measurement is only ever handed back for the width it was taken at.'()
    {
        reportInfo """
            The whole purpose of `preferredLayoutSizeAtWidth` is that the answer depends
            on the width it is given: a grid which wraps needs more height when it is
            narrow. An answer therefore belongs to exactly one width and to no other.

            This scenario walks a grid through two different widths and then back to the
            first one, which is what a scroll pane does when it discovers halfway through
            a layout pass that it needs a vertical scroll bar after all. Every one of the
            three questions has to be answered on its own terms -- a grid which mixed them
            up would size its content for a width it does not have.
        """
        given : 'Three boxes, each of which claims half a row from the `MEDIUM` category upwards, and a whole row below it.'
            var grid =
                    UI.panel().withFlowLayout(UI.HorizontalAlignment.LEFT, 5, 5)
                    .add(UI.AUTO_SPAN({ it.verySmall(12).small(12).medium(6).large(6).veryLarge(6).oversize(6) }),
                        UI.box().withPrefSize(40, 20)
                    )
                    .add(UI.AUTO_SPAN({ it.verySmall(12).small(12).medium(6).large(6).veryLarge(6).oversize(6) }),
                        UI.box().withPrefSize(40, 20)
                    )
                    .add(UI.AUTO_SPAN({ it.verySmall(12).small(12).medium(6).large(6).veryLarge(6).oversize(6) }),
                        UI.box().withPrefSize(40, 20)
                    )
                    .get(JPanel)
        and : 'We declare that this grid considers itself full at a width of 60, which is what its size categories are measured against.'
            grid.setPreferredSize(new Dimension(60, 0))
        and : 'We put it into a window and pack it, because only a realised container is ever `valid`.'
            var frame = new JFrame("A remembered measurement belongs to one width")
            frame.getContentPane().add(grid)
            UI.runNow({ frame.pack() })

        expect : 'The grid really is valid, which is the state in which a measurement may be reused at all.'
            grid.isValid()

        when : 'We ask how tall it would be at a width of 40, two thirds of its reference width and so the `LARGE` category,'
            var wide = UI.runAndGet({ grid.getLayout().preferredLayoutSizeAtWidth(grid, 40) })
        then : 'the boxes pair up and we get two rows: two boxes of 20 and three gaps of 5.'
            wide.height == 55

        when : 'We ask the very same grid about a width of 20 instead, a third of its reference width and thus `SMALL`,'
            var narrow = UI.runAndGet({ grid.getLayout().preferredLayoutSizeAtWidth(grid, 20) })
        then : 'every box claims a whole row, so three boxes of 20 and four gaps of 5 are needed.'
            narrow.height == 80

        when : 'We come back to the width we started with,'
            var wideAgain = UI.runAndGet({ grid.getLayout().preferredLayoutSizeAtWidth(grid, 40) })
        then : 'we are told what we were told the first time, and not what belongs to the narrow width.'
            wideAgain == wide

        cleanup : 'We close the window again.'
            UI.runNow({ frame.dispose() })
    }

    def 'A grid measures its children again after one of them changed.'()
    {
        reportInfo """
            A component which changes size announces it by calling `revalidate()`, and
            Swing turns that into an `invalidate()` travelling up the parent chain. When
            it passes the grid, the grid is told through `invalidateLayout` that what it
            knows about itself is now out of date.

            This is the ordinary, everyday case: a label gets a longer text, a panel gets
            another child, a component is given a new preferred size. The grid has to
            forget what it measured, or the window would reserve yesterday's room for
            today's content and clip whatever no longer fits.
        """
        given : 'Three boxes in a grid, side by side.'
            var grid =
                    UI.panel().withFlowLayout(UI.HorizontalAlignment.LEFT, 5, 5)
                    .add(UI.box().withPrefSize(40, 20))
                    .add(UI.box().withPrefSize(40, 20))
                    .add(UI.box().withPrefSize(40, 20))
                    .get(JPanel)
        and : 'A window holding it, packed, so that the grid is valid and its measurements may be reused.'
            var frame = new JFrame("A changed child forces a new measurement")
            frame.getContentPane().add(grid)
            UI.runNow({ frame.pack() })

        when : 'We ask how big the grid would like to be at a width of 200, which is room enough for all three boxes,'
            var before = UI.runAndGet({ grid.getLayout().preferredLayoutSizeAtWidth(grid, 200) })
        then : 'it asks for a single row: a box of 20 and two gaps of 5.'
            before.height == 30

        when : 'The first box now becomes four and a half times as tall and says so,'
            UI.runNow({
                ((JComponent) grid.getComponent(0)).setPreferredSize(new Dimension(40, 90))
                ((JComponent) grid.getComponent(0)).revalidate()
            })
        and : 'we ask the grid the very same question once more,'
            var after = UI.runAndGet({ grid.getLayout().preferredLayoutSizeAtWidth(grid, 200) })
        then : 'the taller row is accounted for: a box of 90 and two gaps of 5.'
            after.height == 100

        cleanup : 'We close the window again.'
            UI.runNow({ frame.dispose() })
    }

    def 'A grid measures its children again while it is invalid, even though nothing told it a second time.'()
    {
        reportInfo """
            Swing propagates `invalidate()` upwards only until it reaches a component
            which is already invalid, because there is nothing left to tell such a
            component. A grid which is invalidated once and then has three of its children
            change therefore hears about it exactly once, not four times.

            That is the reason a remembered measurement may only be handed out while the
            container is *valid*, and it is not a subtle corner case: being invalid is the
            state a container spends every single layout pass in. An invalid grid has to
            measure afresh every time, precisely because it is no longer being told when
            something moves underneath it. The delegation box which a configured scroll
            pane places around its content follows the same reasoning.
        """
        given : 'Three boxes in a grid, side by side.'
            var grid =
                    UI.panel().withFlowLayout(UI.HorizontalAlignment.LEFT, 5, 5)
                    .add(UI.box().withPrefSize(40, 20))
                    .add(UI.box().withPrefSize(40, 20))
                    .add(UI.box().withPrefSize(40, 20))
                    .get(JPanel)
        and : 'A window holding it, packed, which leaves the grid valid to begin with.'
            var frame = new JFrame("An invalid grid may not reuse a measurement")
            frame.getContentPane().add(grid)
            UI.runNow({ frame.pack() })

        when : 'We invalidate the grid, putting it in the state it is in throughout every layout pass,'
            UI.runNow({ grid.invalidate() })
        then : 'it is indeed no longer valid.'
            !grid.isValid()

        when : 'We measure it in that state, just like a layout pass would,'
            var before = UI.runAndGet({ grid.getLayout().preferredLayoutSizeAtWidth(grid, 200) })
        then : 'we are told about the single row the three boxes occupy.'
            before.height == 30

        when : 'The first box now grows and announces it to its ancestors,'
            UI.runNow({
                ((JComponent) grid.getComponent(0)).setPreferredSize(new Dimension(40, 90))
                ((JComponent) grid.getComponent(0)).revalidate()
            })
        then : """
            the grid is none the wiser, because it was already invalid and Swing does not
            invalidate what is invalid already. Whatever it remembers, it cannot have been
            told to forget it.
        """
            !grid.isValid()

        when : 'We measure it once more,'
            var after = UI.runAndGet({ grid.getLayout().preferredLayoutSizeAtWidth(grid, 200) })
        then : 'it answers with the size it needs now rather than the one it needed a moment ago.'
            after.height == 100

        cleanup : 'We close the window again.'
            UI.runNow({ frame.dispose() })
    }

    def 'Changing a gap on the layout manager is reflected in the very next measurement.'(
        String property, int expectedWidth, int expectedHeight, Closure<?> change
    ) {
        reportInfo """
            The gaps, the alignment and the baseline flag belong to the layout manager
            rather than to the container. Changing one of them does not touch the
            container at all, so Swing has nothing to report and `invalidateLayout` is
            never called. A layout which remembers measurements has to notice such a
            change entirely by itself.

            This scenario deliberately changes a gap **without** revalidating anything, and
            then asks the same question as before. A layout which trusted Swing to tell it
            everything would answer with the measurement it took before the change, and the
            gaps would appear to have no effect until something else happened to invalidate
            the container.

            The two gap sizes are the ones shown here because their effect on the measured
            size can be pointed at directly with boxes of a fixed size. `setAlignment` and
            `setAlignOnBaseline` forget the remembered measurement through the very same
            path, but neither makes for a good scenario: the first has no effect on the
            measured size at all, since it only decides where within a row the children end
            up, and the second needs children carrying a real text baseline, whose measured
            size depends on the fonts of whichever machine runs the test.
        """
        given : 'Three boxes in a grid, side by side, with gaps of 5.'
            var grid =
                    UI.panel().withFlowLayout(UI.HorizontalAlignment.LEFT, 5, 5)
                    .add(UI.box().withPrefSize(40, 20))
                    .add(UI.box().withPrefSize(40, 20))
                    .add(UI.box().withPrefSize(40, 20))
                    .get(JPanel)
        and : 'A window holding it, packed, so the grid is valid and may reuse what it measures.'
            var frame = new JFrame("A changed layout manager forces a new measurement")
            frame.getContentPane().add(grid)
            UI.runNow({ frame.pack() })

        when : 'We measure it at a width of 200, an answer the grid is now free to remember,'
            var before = UI.runAndGet({ grid.getLayout().preferredLayoutSizeAtWidth(grid, 200) })
        then : 'we get a single row of three boxes: 140 wide including all four gaps, and 30 tall.'
            before == new Dimension(140, 30)

        when : "We change $property on the layout manager itself, and revalidate nothing at all,"
            UI.runNow({ change((ResponsiveGridFlowLayout) grid.getLayout()) })
        then : 'the container knows nothing of it and stays perfectly valid.'
            grid.isValid()

        when : 'We nevertheless ask the very same question again,'
            var after = UI.runAndGet({ grid.getLayout().preferredLayoutSizeAtWidth(grid, 200) })
        then : 'the new gap is honoured instead of the measurement taken before the change.'
            after == new Dimension(expectedWidth, expectedHeight)

        cleanup : 'We close the window again.'
            UI.runNow({ frame.dispose() })

        where :
            property             | expectedWidth | expectedHeight | change
            'the vertical gap'   | 140           | 100            | { ResponsiveGridFlowLayout it -> it.setVerticalGapSize(40) }
            'the horizontal gap' | 280           | 55             | { ResponsiveGridFlowLayout it -> it.setHorizontalGapSize(40) }
    }

    def 'A layout manager shared by two containers answers for each of them separately.'()
    {
        reportInfo """
            Nothing stops a single layout manager instance from being installed on more
            than one container -- `java.awt.FlowLayout` is routinely shared that way, and
            the responsive grid keeps no per-container state that would forbid it.

            A layout which remembers its last measurement therefore has to remember *which*
            container that measurement was of. Handing the size of one container to a
            different one would be the worst kind of bug: it only shows up when a layout
            manager happens to be shared, and it makes two unrelated parts of a window
            silently agree on the wrong size.
        """
        given : 'One single layout manager instance.'
            var shared = new ResponsiveGridFlowLayout(UI.HorizontalAlignment.LEFT, 5, 5)
        and : 'A small container which uses it, holding one small box.'
            var small = UI.panel().withLayout(shared).add(UI.box().withPrefSize(40, 20)).get(JPanel)
        and : 'A large container which uses the very same instance, holding one much bigger box.'
            var large = UI.panel().withLayout(shared).add(UI.box().withPrefSize(90, 60)).get(JPanel)
        and : 'Both of them realised in windows of their own, so both are valid.'
            var smallFrame = new JFrame("The small one")
            var largeFrame = new JFrame("The large one")
            smallFrame.getContentPane().add(small)
            largeFrame.getContentPane().add(large)
            UI.runNow({ smallFrame.pack(); largeFrame.pack() })

        expect : 'They really do share the one layout manager.'
            small.getLayout().is(shared)
            large.getLayout().is(shared)

        when : 'We measure the small one,'
            var firstSmall = UI.runAndGet({ shared.preferredLayoutSizeAtWidth(small, 300) })
        then : 'we are told about its one small box and the gaps around it.'
            firstSmall == new Dimension(50, 30)

        when : 'We then measure the large one through the same instance,'
            var firstLarge = UI.runAndGet({ shared.preferredLayoutSizeAtWidth(large, 300) })
        then : 'we are told about its one large box, and not about the container measured just before.'
            firstLarge == new Dimension(100, 70)

        when : 'And when we come back to the small one,'
            var secondSmall = UI.runAndGet({ shared.preferredLayoutSizeAtWidth(small, 300) })
        then : 'it is still measured as itself.'
            secondSmall == firstSmall

        cleanup : 'We close both windows again.'
            UI.runNow({ smallFrame.dispose(); largeFrame.dispose() })
    }

    def 'A grid which has not changed does not walk its children again.'()
    {
        reportInfo """
            This is the reason the answer is remembered in the first place, so it is worth
            pinning down directly rather than trusting it to stay true.

            Walking the children is what makes measuring expensive: every child is asked
            for its preferred size, which for a text component means shaping its text, and
            for a nested grid means measuring that entire grid in turn. A grid asked the
            same question five times over must pay for that walk once. The moment it is
            invalidated it has to pay again, because then it can no longer know that the
            answer still holds.
        """
        given : 'Three boxes which keep count of how often they are asked for their preferred size.'
            var boxes = [new CountingBox(40, 20), new CountingBox(40, 20), new CountingBox(40, 20)]
        and : 'A grid holding them.'
            var grid =
                    UI.panel().withFlowLayout(UI.HorizontalAlignment.LEFT, 5, 5)
                    .add(UI.of(boxes[0]))
                    .add(UI.of(boxes[1]))
                    .add(UI.of(boxes[2]))
                    .get(JPanel)
        and : 'A window holding the grid, packed, so the grid is valid.'
            var frame = new JFrame("An unchanged grid walks its children once")
            frame.getContentPane().add(grid)
            UI.runNow({ frame.pack() })

        when : 'We measure the grid once and note how much walking that took,'
            UI.runNow({ grid.getLayout().preferredLayoutSizeAtWidth(grid, 200) })
            var afterOneMeasurement = boxes[0].measurements + boxes[1].measurements + boxes[2].measurements
        then : 'the children were in fact walked.'
            afterOneMeasurement > 0

        when : 'We ask the very same question four more times,'
            4.times { UI.runNow({ grid.getLayout().preferredLayoutSizeAtWidth(grid, 200) }) }
        then : 'not a single child was asked again -- all four answers came from the first walk.'
            boxes[0].measurements + boxes[1].measurements + boxes[2].measurements == afterOneMeasurement

        when : 'We invalidate the grid, which is Swing telling it that it knows nothing anymore,'
            UI.runNow({ grid.invalidate() })
        and : 'we ask once more,'
            UI.runNow({ grid.getLayout().preferredLayoutSizeAtWidth(grid, 200) })
        then : 'it walks its children all over again.'
            boxes[0].measurements + boxes[1].measurements + boxes[2].measurements > afterOneMeasurement

        cleanup : 'We close the window again.'
            UI.runNow({ frame.dispose() })
    }

    /** A box of a fixed size which counts how often it is asked how big it would like to be. */
    static class CountingBox extends JPanel
    {
        int measurements = 0

        CountingBox( int width, int height ) {
            setPreferredSize(new Dimension(width, height))
        }

        @Override Dimension getPreferredSize() {
            measurements++
            return super.getPreferredSize()
        }
    }
}
