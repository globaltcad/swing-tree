package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.layout.ResponsiveGridFlowLayout
import swingtree.threading.EventProcessor

import javax.swing.JPanel
import java.awt.Component
import java.awt.Dimension

@Title("Nesting Responsive Layouts")
@Narrative('''

    The `ResponsiveGridFlowLayout` wraps its children into rows, which means that
    the height it needs depends on the width it is given. A layout with that
    property is called *width-for-height*, and it only composes if two conditions
    hold:

    - it must be able to answer "how tall are you at width X" **before** it has
      ever been laid out at width X, and
    - a parent must actually ask that question, instead of asking for a preferred
      height that was computed for some other width.

    This specification pins down both requirements. They matter most when one
    responsive grid is placed inside another one, which is the natural way to
    express a page whose sidebar is itself a grid of items. Without them, such a
    nested grid reports the row count of the *previous* layout pass, and the rows
    that no longer fit are silently clipped.

''')
@Subject([ResponsiveGridFlowLayout])
class Nested_Responsive_Layout_Spec extends Specification
{
    def setup() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
        SwingTree.get().setUiScaleFactor(1f)
    }

    def cleanup() {
        SwingTree.clear()
    }

    /**
     *  Returns the lowest edge occupied by any child of the given container,
     *  which is how much vertical room the container would need to show all
     *  of its content.
     */
    private static int _contentBottomOf( java.awt.Container container ) {
        int bottom = 0
        for ( Component child : container.getComponents() )
            bottom = Math.max(bottom, child.getY() + child.getHeight())
        return bottom
    }

    /**
     *  Lays out a container and then its children, top down, which is the order
     *  in which `Container.validate()` would do it. Laying out only the outer
     *  container would leave the nested one with the child positions of the
     *  previous pass, and we would be asserting against stale numbers.
     */
    private static void _layoutTree( java.awt.Container container ) {
        container.doLayout()
        for ( Component child : container.getComponents() )
            if ( child instanceof java.awt.Container )
                _layoutTree((java.awt.Container) child)
    }

    private static JPanel _gridOfThreeBoxes( int span ) {
        return UI.panel().withFlowLayout(UI.HorizontalAlignment.LEFT, 5, 5)
                .add(UI.AUTO_SPAN({ it.verySmall(12).small(12).medium(span).large(span).veryLarge(span).oversize(span) }),
                    UI.box().withPrefSize(40, 20)
                )
                .add(UI.AUTO_SPAN({ it.verySmall(12).small(12).medium(span).large(span).veryLarge(span).oversize(span) }),
                    UI.box().withPrefSize(40, 20)
                )
                .add(UI.AUTO_SPAN({ it.verySmall(12).small(12).medium(span).large(span).veryLarge(span).oversize(span) }),
                    UI.box().withPrefSize(40, 20)
                )
                .get(JPanel)
    }

    def 'A responsive grid reports the height it needs for the width it currently has.'()
    {
        reportInfo """
            A responsive grid wraps, so its height is a function of its width.
            When it is narrow enough to force its three children onto three
            separate rows, then it needs room for three rows -- and it has to
            say so *before* it is laid out, because a parent container reads the
            preferred size in order to decide how much room to hand out.

            This is the foundation of everything else in this specification:
            a layout which answers this question using the row count of the
            previous pass cannot be nested, and it also cannot be placed in a
            scroll pane and be expected to scroll to the right height.
        """
        given : 'A grid of three boxes, each 40 by 20 pixels, with a gap of 5.'
            var panel = _gridOfThreeBoxes(12)
        expect : """
            Laid out in a single row, the grid is 140 wide
            (3*40 for the boxes, 2*5 for the gaps between them and 2*5 for the
            outer gaps) and 30 tall (20 for a box plus the two outer gaps).
            This "ideal" single row width is also the reference against which
            the size categories of the children are measured.
        """
            panel.getLayout().preferredLayoutSize(panel) == new Dimension(140, 30)

        when : """
            We now give the grid a third of the width it would like to have,
            which puts it into the `SMALL` size category, where every child was
            declared to span all 12 columns. Three children, each claiming a
            full row, need three rows.
        """
            panel.setSize(46, 0)
        then : """
            The grid asks for the height of three rows: three boxes of 20 and
            four gaps of 5, so 80 in total. Note that we did *not* lay the panel
            out before asking -- that is the whole point.
        """
            panel.getLayout().preferredLayoutSize(panel).height == 80

        when : 'We lay the panel out and read the actual extent of its children,'
            panel.doLayout()
        then : 'it agrees with the height that was promised beforehand.'
            _contentBottomOf(panel) + 5 == 80
    }

    def 'A responsive grid nested inside another one is tall enough for its own rows.'()
    {
        reportInfo """
            Here we place a responsive grid inside another responsive grid, which
            is how one would express a page whose sidebar is itself a grid of
            items. The outer grid gives the nested grid a fraction of its width,
            and the nested grid then has to wrap its own children into however
            many rows that width implies.

            The outer grid must therefore ask the nested grid how tall it is *at
            the width it is about to receive*. Asking for a plain preferred
            height instead yields the height of the previous layout pass, and any
            row that does not fit into that stale height is clipped away.
        """
        given : 'A nested grid whose three boxes each span half a row,'
            var nested = _gridOfThreeBoxes(6)
        and : 'placed as the only child of an outer grid, where it spans half of the outer row.'
            var outer = UI.panel().withFlowLayout(UI.HorizontalAlignment.LEFT, 0, 0)
                            .withPrefSize(400, 200)
                            .add(UI.AUTO_SPAN({ it.verySmall(12).small(12).medium(6).large(6).veryLarge(6).oversize(6) }),
                                UI.of(nested)
                            )
                            .get(JPanel)

        when : """
            The outer grid is given its full preferred width of 400, so the
            nested grid receives half of it, 200 pixels. At 200 pixels the nested
            grid is wider than the 140 it would ideally like, so its children
            keep their declared span of 6 columns and pack two per row -- three
            children need two rows.
        """
            outer.setSize(400, 200)
            _layoutTree(outer)
        then : 'The nested grid was given a width of half the outer row.'
            nested.getWidth() == 200
        and : """
            And it was given enough height for its two rows: two boxes of 20
            and three gaps of 5, so 55 in total.
        """
            nested.getHeight() >= _contentBottomOf(nested)
            nested.getHeight() == 55
    }

    def 'Rows are not clipped when a nested grid is resized straight from narrow to wide.'()
    {
        reportInfo """
            This is the regression which motivated this specification. A stale
            preferred height is only visible when the number of rows *changes*,
            so we drive a nested grid through a resize in one single step,
            without any intermediate layout passes to smooth it over.

            Whichever way the resize goes, the nested grid must end up tall
            enough for the rows it actually has. A layout pass has to be
            self-sufficient: there is nothing which would schedule a second,
            corrective pass afterwards.
        """
        given : 'A nested grid inside an outer grid, both responsive.'
            var nested = _gridOfThreeBoxes(6)
            var outer = UI.panel().withFlowLayout(UI.HorizontalAlignment.LEFT, 0, 0)
                            .withPrefSize(400, 200)
                            .add(UI.AUTO_SPAN({ it.verySmall(12).small(12).medium(12).large(12).veryLarge(6).oversize(6) }),
                                UI.of(nested)
                            )
                            .get(JPanel)

        when : 'We first settle the layout at a narrow width, where the nested grid stacks its children.'
            outer.setSize(100, 400)
            _layoutTree(outer)
        then : 'Every child of the nested grid is inside its bounds.'
            nested.getHeight() >= _contentBottomOf(nested)

        when : 'And now we jump straight to the full width in a single resize.'
            outer.setSize(400, 400)
            _layoutTree(outer)
        then : 'The nested grid still contains all of its children, none of them clipped away.'
            nested.getHeight() >= _contentBottomOf(nested)

        when : 'The same jump in the opposite direction, from wide back to narrow,'
            outer.setSize(100, 400)
            _layoutTree(outer)
        then : 'is equally free of clipping.'
            nested.getHeight() >= _contentBottomOf(nested)
    }

    def 'A nested grid does not impose its ideal single row width on the size categories of its parent.'()
    {
        reportInfo """
            The size categories of a responsive grid are measured against the
            width it considers itself "full" at. By default that is the width of
            all of its children in a single row, which is also the preferred
            width it reports to its parent.

            For a nested grid those two roles pull in opposite directions. A
            sidebar which holds a dozen items has an ideal single row width of
            well over a thousand pixels, and reporting *that* to the page it
            lives on would push the page into its narrowest size category and
            make the sidebar the widest thing on the screen.

            Setting an explicit preferred size on a nested grid resolves this:
            it declares the width at which the nested grid considers itself full,
            and is what the parent sees. Its height, however, still follows the
            rows -- an explicit preferred size is a lower bound, not a fixed
            height, because the rows have to fit no matter what.
        """
        given : 'A nested grid of three boxes whose ideal single row width is 140,'
            var nested = _gridOfThreeBoxes(6)
        expect : 'which is indeed what it reports when left to its own devices.'
            nested.getPreferredSize().width == 140

        when : """
            We now declare that this grid considers itself full at a width of 60,
            which is well below the 140 it would take to show all three boxes
            side by side.
        """
            nested.setPreferredSize(new Dimension(60, 0))
        then : 'That is the width the parent gets to see, so the ideal single row width no longer leaks upwards.'
            nested.getPreferredSize().width == 60

        when : """
            At a width of 40 the nested grid is at two thirds of its declared
            reference width of 60, which is the `LARGE` category, where its
            children span 6 columns and pack two per row.
        """
            nested.setSize(40, 0)
            var preferred = nested.getLayout().preferredLayoutSizeAtWidth(nested, 40)
        then : """
            The height is that of two rows (two boxes of 20 and three gaps of 5),
            so the explicitly set height of 0 did not freeze it.
        """
            preferred.height == 55

        when : 'We narrow it to a third of its reference width, the `SMALL` category, where every child claims a full row,'
            var narrow = nested.getLayout().preferredLayoutSizeAtWidth(nested, 20)
        then : 'the grid asks for three rows instead: three boxes of 20 and four gaps of 5.'
            narrow.height == 80
    }
}
