package swingtree

import groovy.transform.CompileDynamic
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.Var
import swingtree.api.model.TableData
import swingtree.components.JBox
import swingtree.threading.EventProcessor
import utility.SwingTreeTestConfigurator
import utility.Utility

import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JSlider
import javax.swing.JSplitPane
import javax.swing.JTabbedPane
import javax.swing.JTable
import javax.swing.SwingConstants
import java.awt.Color

@Title("The Vocabulary Enums")
@Narrative('''

    Raw Swing describes geometry with loose integer constants:
    `JSlider.HORIZONTAL` is `0`, `SwingConstants.TOP` is `1`, and nothing
    stops you from handing one to a method which wanted the other.
    SwingTree replaces all of them with a small set of enums, and this
    specification is about those enums themselves rather than about any
    one component.

    There are only a handful of them and they turn up everywhere:
    `UI.Axis` says which way a thing runs, `UI.Placement` names one point
    of a rectangle, `UI.Side` and `UI.Corner` name an edge and a corner,
    `UI.Span` names the direction a gradient travels, and `UI.ComponentArea`
    together with `UI.ComponentBoundary` names the regions of the box model
    and the lines dividing them.

    Each of them carries a few small methods, so that an application can ask
    the enum a question instead of writing a switch over its constants.
    Every scenario in this specification takes one of those methods and
    checks the promise it makes.

''')
@Subject([UI])
@CompileDynamic
class UI_Enum_Spec extends Specification
{
    def setupSpec() {
        SwingTree.initializeUsing(SwingTreeTestConfigurator.get())
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
        // In this specification we are using the strict event processor
        // which will throw exceptions if we try to perform UI operations in the test thread.
    }

    def cleanupSpec() {
        SwingTree.clear()
    }

    def 'A slider built with `UI.Axis.LINE` is horizontal, while a box layout built with it keeps the reading direction.'()
    {
        reportInfo """
            `UI.Axis` has four constants. `HORIZONTAL` and `VERTICAL` say which way a
            thing runs outright. `LINE` says "the way a line of text runs" and `PAGE`
            says "the way lines follow each other down a page", and both of those are
            decided by the reading direction of the container they are used in.

            SwingTree promises two things about that. A component which has no reading
            direction to honour reads `LINE` as `HORIZONTAL` and `PAGE` as `VERTICAL`,
            which is the answer `UI.Axis.resolve()` gives. A `BoxLayout` keeps all four
            constants apart, because it is the one place SwingTree hands a `UI.Axis` to
            which can place its children from right to left.

            This scenario builds a `JSlider` through `UI.slider(UI.Axis.LINE)` and a
            panel through `UI.panel().withBoxLayout(UI.Axis.LINE)`, both from that very
            same constant. The slider has to report `SwingConstants.HORIZONTAL`, and the
            panel has to carry a `BoxLayout` whose axis is `BoxLayout.LINE_AXIS` rather
            than `BoxLayout.X_AXIS`.

            **Why this is fragile:** `UI.Axis.LINE` reaches a slider through
            `UI.Axis.forSlider()` and a box layout through `UI.Axis.forBoxLayout()`, and
            those two have to disagree on purpose. Making them agree breaks something
            either way. If the slider stopped resolving `LINE`, then
            `UI.slider(UI.Axis.LINE)` would have to throw or guess, and a view model
            holding its axis as `LINE` could no longer feed a slider and a box layout
            from one property. If the box layout started resolving it, then a panel in an
            Arabic or Hebrew user interface would lay its children out from left to
            right, and the window would read backwards.
        """
        given : 'A slider and a panel, both declared with the very same axis constant, `UI.Axis.LINE`.'
            var slider = UI.slider(UI.Axis.LINE).get(JSlider)
            var panel  = UI.panel().withBoxLayout(UI.Axis.LINE).get(javax.swing.JPanel)

        expect : 'The slider is horizontal, because a slider has no reading direction to honour.'
            slider.orientation == SwingConstants.HORIZONTAL

        and : 'The panel carries a box layout on the line axis, which is where the reading direction survives.'
            panel.layout instanceof BoxLayout
            ((BoxLayout) panel.layout).axis == BoxLayout.LINE_AXIS

        and : 'Asking the enum itself gives the answer the slider arrived at: the line axis is the horizontal one.'
            UI.Axis.LINE.resolve() == UI.Axis.HORIZONTAL
            UI.Axis.PAGE.resolve() == UI.Axis.VERTICAL
            UI.Axis.LINE.isHorizontal()
            !UI.Axis.PAGE.isHorizontal()
    }

    def '`UI.Axis.perpendicular()` names the axis at a right angle, which is the axis of a divider across a split.'()
    {
        reportInfo """
            A `JSplitPane` laid out along `UI.Axis.HORIZONTAL` puts its two components
            side by side, which makes the divider between them a vertical bar. The axis a
            split runs along and the axis of the bar across it are always at right
            angles, and `UI.Axis.perpendicular()` is how a caller says "the other one"
            without naming both constants.

            This matters where the axis is not a literal in the source but a value which
            was handed in. A view model holding the split direction is the usual case: to
            draw a separator lying across the split, the other axis is needed, and
            writing `axis == HORIZONTAL ? VERTICAL : HORIZONTAL` at every such place is
            the switch this method removes.

            This scenario holds `UI.Axis.HORIZONTAL` in a variable, as it would arrive
            from a view model, builds a split pane along it and a separator along
            `perpendicular()` of it. The split pane has to report
            `JSplitPane.HORIZONTAL_SPLIT` and the separator `SwingConstants.VERTICAL`.
            Going across twice has to return the axis it started from.

            **Why this is fragile:** `perpendicular()` is the single expression
            `isHorizontal() ? VERTICAL : HORIZONTAL`, and writing it the other way round
            compiles and type checks, because both answers are the same type. Every
            separator, divider and label meant to lie across an axis would then be drawn
            along it: a separator meant to cut a horizontal split in two would run
            parallel to the seam and disappear into it.
        """
        given : 'The axis a split pane is laid out along, as it would arrive from a view model.'
            var splitAxis = UI.Axis.HORIZONTAL

        and : 'A split pane along that axis, and a separator along the axis across it.'
            var splitPane = UI.splitPane(splitAxis).get(JSplitPane)
            var separator = UI.separator(splitAxis.perpendicular()).get(javax.swing.JSeparator)

        expect : 'The split places its two components left and right of each other.'
            splitPane.orientation == JSplitPane.HORIZONTAL_SPLIT

        and : 'The separator, lying across that split, runs vertically.'
            separator.orientation == SwingConstants.VERTICAL

        and : 'Going across twice returns the horizontal axis it started from.'
            splitAxis.perpendicular().perpendicular() == splitAxis
    }

    def 'A `UI.Side` names the axis its edge runs along and the edge facing it across the component.'()
    {
        reportInfo """
            `UI.Side` names one of the four edges of a component, and it is how a
            `JTabbedPane` is told where to keep its tabs.

            An edge is a line, so it runs along an axis of its own, and that is easy to
            get backwards. The top edge is a horizontal line, so a tab strip docked to
            the top is laid out in a row. The left edge is a vertical line, so tabs
            docked to the left stack downwards. `UI.Side.axis()` answers that question,
            and `UI.Side.opposite()` returns the edge facing this one across the
            component, which is what a "move it to the other side" command needs.

            This scenario docks the tabs of one `JTabbedPane` to `UI.Side.LEFT` and the
            tabs of a second one to `UI.Side.LEFT.opposite()`. The first has to report
            `JTabbedPane.LEFT` and the second `JTabbedPane.RIGHT`. The left edge has to
            report a vertical axis, the top and bottom edges a horizontal one, and facing
            twice has to return every side to itself.

            **Why this is fragile:** `axis()` answers `UI.Axis.HORIZONTAL` for the top
            and the bottom, which reads as the wrong way round at a glance, because the
            top of a component is above its middle rather than beside it. Swapping the
            two answers compiles, and anything asking a docked strip for its axis would
            then lay that strip out at a right angle to the edge it is docked against:
            tabs along the top would be stacked into a column one tab wide, running off
            the bottom of the window.
        """
        given : 'A tabbed pane with its tabs docked to the left edge.'
            var docked = UI.Side.LEFT
            var tabs = UI.tabbedPane(docked).get(JTabbedPane)

        and : 'A second one docked to the edge facing it, as a "move to the other side" action would build.'
            var movedTabs = UI.tabbedPane(docked.opposite()).get(JTabbedPane)

        expect : 'The first pane keeps its tabs on the left, and the second on the right.'
            tabs.tabPlacement == JTabbedPane.LEFT
            movedTabs.tabPlacement == JTabbedPane.RIGHT

        and : 'The left edge is a vertical line, so the tabs docked there stack downwards.'
            docked.axis() == UI.Axis.VERTICAL

        and : 'The top and bottom edges are horizontal lines, so tabs docked there sit in a row.'
            UI.Side.TOP.axis() == UI.Axis.HORIZONTAL
            UI.Side.BOTTOM.axis() == UI.Axis.HORIZONTAL

        and : 'Every side faces the one across from it, and facing twice returns to the start.'
            UI.Side.TOP.opposite() == UI.Side.BOTTOM
            UI.Side.RIGHT.opposite() == UI.Side.LEFT
            UI.Side.values().every { it.opposite().opposite() == it }
    }

    def 'A `UI.Placement` names both coordinates of a point, so `UI.Placement.TOP` centres a label horizontally too.'()
    {
        reportInfo """
            `UI.Placement` names a single point of a rectangular component: one of the
            four corners, the middle of one of the four sides, or the centre. A `JLabel`
            built through `UI.label(text, placement)` takes one to decide where its text
            and icon sit.

            The trap worth knowing is that a placement always answers for **both** axes,
            because a point has two coordinates. `UI.Placement.TOP` does not mean "at the
            top and leave the rest alone": it is the middle of the top edge, so a label
            aligned with it is centred horizontally as well. Naming the corner
            `UI.Placement.TOP_LEFT` is how both axes are pinned, and
            `withHorizontalAlignment(..)` or `withVerticalAlignment(..)` are how one axis
            is moved and the other left where it is.

            This scenario builds one label with `UI.Placement.TOP` and one with
            `UI.Placement.TOP_LEFT`. Both have to report `SwingConstants.TOP` vertically.
            The first has to report `SwingConstants.CENTER` horizontally and the second
            `SwingConstants.LEFT`. Reading the two halves back out through `vertical()`
            and `horizontal()` has to say the same thing, and every one of the ten
            constants has to be rebuildable from the two halves it reports, through
            `UI.Placement.of(vertical, horizontal)`.

            **Why this is fragile:** `horizontal()` and `vertical()` are two switches over
            ten constants, and `UIForLabel.withAlignment(..)` applies both of their
            answers to the component, skipping the ones which report
            `UI.HorizontalAlignment.UNDEFINED`. A constant answering `UNDEFINED` where
            `CENTER` was meant would therefore leave the horizontal alignment of the label
            at whatever it happened to be. `UI.Placement.TOP` would silently mean "at the
            top, and keep the old side", so the same title would sit top left in one label
            and top centre in the next.
        """
        given : 'Two labels, one placed at the top and one at the top left corner.'
            var top     = UI.label("Title", UI.Placement.TOP).get(JLabel)
            var topLeft = UI.label("Title", UI.Placement.TOP_LEFT).get(JLabel)

        expect : 'Both labels put their content at the top, as their names promise.'
            top.verticalAlignment     == SwingConstants.TOP
            topLeft.verticalAlignment == SwingConstants.TOP

        and : 'But `TOP` also centres the content horizontally, because it names the middle of that edge.'
            top.horizontalAlignment == SwingConstants.CENTER

        and : 'Only the corner constant pins the horizontal axis to the left.'
            topLeft.horizontalAlignment == SwingConstants.LEFT

        and : 'Reading the two coordinates back out of the point says the same thing.'
            UI.Placement.TOP.vertical()   == UI.VerticalAlignment.TOP
            UI.Placement.TOP.horizontal() == UI.HorizontalAlignment.CENTER

        and : 'Every point can be rebuilt from the two coordinates it reports.'
            UI.Placement.values().every {
                UI.Placement.of(it.vertical(), it.horizontal()) == it
            }
    }

    def '`UI.Placement.of(..)` turns a leading alignment into a left or a right using the reading direction given to it.'()
    {
        reportInfo """
            `UI.HorizontalAlignment` has two constants which are not sides at all:
            `LEADING` and `TRAILING`. They mean "wherever a line of text begins" and
            "wherever it ends", which is the left and the right in English, and the right
            and the left in Arabic or Hebrew.

            A `UI.Placement` cannot hold either of them, because a placement is a point
            which has already been chosen and those two are still a question. So the
            question is answered exactly once, when the point is built:
            `UI.Placement.of(vertical, horizontal, orientation)` reads the orientation and
            hands back a real point.

            This scenario resolves `UI.VerticalAlignment.TOP` together with
            `UI.HorizontalAlignment.LEADING` three times. Against
            `UI.ComponentOrientation.LEFT_TO_RIGHT` the answer has to be
            `UI.Placement.TOP_LEFT`. Against `UI.ComponentOrientation.RIGHT_TO_LEFT` it
            has to be `UI.Placement.TOP_RIGHT`. Against
            `UI.ComponentOrientation.UNKNOWN` it has to be `UI.Placement.TOP_LEFT` once
            more, which is the choice `java.awt.ComponentOrientation.UNKNOWN` makes too,
            and it is why a component whose orientation was never set behaves like an
            English one. The two argument overload has to agree with the unknown one, and
            neither resolved point may report `LEADING` or `TRAILING` back from
            `horizontal()`.

            **Why this is fragile:** the reading direction is consulted in this one place,
            so getting it backwards is a single swapped pair. Nothing in an English user
            interface would look wrong, because `LEADING` and `LEFT` land on the same side
            there. In an Arabic or Hebrew user interface every heading, icon and caption
            would sit on the side opposite the one its text starts at, and only a reader
            of that language would see it. SwingTree reaches this method itself: styled
            text with no placement of its own is placed by
            `UI.Placement.of(verticalAlignment, horizontalAlignment)`.
        """
        given : 'A heading which should sit at the top, where the text of the label begins.'
            var vertical = UI.VerticalAlignment.TOP
            var atTextStart = UI.HorizontalAlignment.LEADING

        when : 'The component reads from left to right, as an English one does...'
            var english = UI.Placement.of(vertical, atTextStart, UI.ComponentOrientation.LEFT_TO_RIGHT)

        then : '...the heading belongs in the top left corner.'
            english == UI.Placement.TOP_LEFT

        when : 'The very same declaration is resolved for a right to left reading direction...'
            var arabic = UI.Placement.of(vertical, atTextStart, UI.ComponentOrientation.RIGHT_TO_LEFT)

        then : '...the heading belongs in the top right corner instead.'
            arabic == UI.Placement.TOP_RIGHT

        and : 'An unknown reading direction is read as left to right, which is what AWT does.'
            UI.Placement.of(vertical, atTextStart, UI.ComponentOrientation.UNKNOWN) == UI.Placement.TOP_LEFT
            UI.Placement.of(vertical, atTextStart) == UI.Placement.TOP_LEFT
            UI.ComponentOrientation.UNKNOWN.isLeftToRightOrUnknown()

        and : 'A resolved placement never reports leading or trailing back to you, only real sides.'
            english.horizontal() == UI.HorizontalAlignment.LEFT
            arabic.horizontal()  == UI.HorizontalAlignment.RIGHT
    }

    def '`UI.Placement.opposite()` returns the point reached through the middle of the component and out the other side.'()
    {
        reportInfo """
            When something has been placed at a point of a component and a second thing
            should sit across from it, the point wanted is the one reached by going
            through the middle and out the other side. That is what
            `UI.Placement.opposite()` returns.

            This scenario starts from `UI.Placement.TOP_LEFT`, as the placement of an
            image would arrive, and requires its opposite to be
            `UI.Placement.BOTTOM_RIGHT`. The middle of the top edge has to face the middle
            of the bottom edge, and crossing the component twice has to return every one
            of the ten constants to itself. `UI.Placement.CENTER` and
            `UI.Placement.UNDEFINED` have to return themselves, because neither of them
            names a side to be on the far side of. Only the four corner constants may
            report `isCorner()`.

            **Why this is fragile:** `opposite()` is a switch over ten constants which
            reflects a point through the centre, and every answer it can give is another
            valid point, so a transposed pair throws nothing and puts a caption in the
            wrong corner. `isCorner()` carries further than that:
            `UI.Span.isDiagonal()` is defined as `from().isCorner()`, and SwingTree
            chooses between its diagonal and its straight gradient painter from that
            answer. A corner which stopped reporting itself as a corner would therefore
            have a corner to corner gradient painted by the painter meant for side to
            side ones.
        """
        given : 'A corner an image was placed at.'
            var imageCorner = UI.Placement.TOP_LEFT

        expect : 'The caption belongs at the corner reached through the middle of the component.'
            imageCorner.opposite() == UI.Placement.BOTTOM_RIGHT

        and : 'The same holds for the middle of an edge.'
            UI.Placement.TOP.opposite() == UI.Placement.BOTTOM
            UI.Placement.LEFT.opposite() == UI.Placement.RIGHT

        and : 'Crossing the component twice brings you back to where you started.'
            UI.Placement.values().every { it.opposite().opposite() == it }

        and : 'The centre and the undefined point have no far side, so they answer with themselves.'
            UI.Placement.CENTER.opposite() == UI.Placement.CENTER
            UI.Placement.UNDEFINED.opposite() == UI.Placement.UNDEFINED

        and : 'Only the four corners report themselves as corners.'
            UI.Placement.values().findAll { it.isCorner() } as Set == [
                UI.Placement.TOP_LEFT,    UI.Placement.TOP_RIGHT,
                UI.Placement.BOTTOM_LEFT, UI.Placement.BOTTOM_RIGHT
            ] as Set
    }

    def '`UI.Corner.opposite()` names the corner across the diagonal, so one value can round both of them.'()
    {
        reportInfo """
            `UI.Corner` names one corner of a component, and the style API takes one in
            `borderRadiusAt(..)` to round that corner by itself.

            A very common shape rounds two corners lying across the diagonal from each
            other, which gives a panel a leaning, ticket-like silhouette.
            `UI.Corner.opposite()` lets a caller name the first corner and derive the
            second, so a single value can drive the whole shape.

            This scenario paints two boxes of 120 by 80 pixels, both filled with the
            colour (70, 130, 180) and both rounded by 30 pixels at two corners. The first
            box rounds `UI.Corner.TOP_LEFT` and `UI.Corner.TOP_LEFT.opposite()`. The
            second spells out `UI.Corner.TOP_LEFT` and `UI.Corner.BOTTOM_RIGHT`. The two
            renderings may not differ in a single colour channel of a single pixel.

            **Why this is fragile:** `opposite()` is a switch over five constants, and a
            corner rounded in the wrong place is still a rounded corner, so a wrong answer
            throws nothing and passes any test which only counts rounded corners. Were
            `TOP_RIGHT` returned for `TOP_LEFT`, the first box would lean the other way
            and the comparison here would fail. `UI.Corner.EVERY` names all four corners
            at once, so no single corner lies across from it, and it answers with itself.
        """
        given : 'A corner to round, as a theme or a view model might supply it.'
            var corner = UI.Corner.TOP_LEFT

        and : 'A box of 120 by 80 pixels which rounds that corner and the one derived from it, both by 30 pixels.'
            var derived =
                    UI.box().withStyle( it -> it
                        .size(120, 80)
                        .backgroundColor(new Color(70, 130, 180))
                        .borderRadiusAt(corner, 30)
                        .borderRadiusAt(corner.opposite(), 30)
                    )
                    .get(JBox)

        and : 'A second box of the same size and colour which spells both corners out literally.'
            var literal =
                    UI.box().withStyle( it -> it
                        .size(120, 80)
                        .backgroundColor(new Color(70, 130, 180))
                        .borderRadiusAt(UI.Corner.TOP_LEFT, 30)
                        .borderRadiusAt(UI.Corner.BOTTOM_RIGHT, 30)
                    )
                    .get(JBox)

        when : 'Both boxes are painted into images of their own.'
            var derivedImage = Utility.renderSingleComponent(derived)
            var literalImage = Utility.renderSingleComponent(literal)

        then : 'Not a single colour channel of a single pixel differs between the two pictures.'
            Utility.worstChannelDelta(derivedImage, literalImage) == 0

        and : 'The corner reached diagonally is the one the drawing used.'
            corner.opposite() == UI.Corner.BOTTOM_RIGHT
            UI.Corner.TOP_RIGHT.opposite() == UI.Corner.BOTTOM_LEFT

        and : '`EVERY` names no single corner, so it has no diagonal partner and answers with itself.'
            UI.Corner.EVERY.opposite() == UI.Corner.EVERY

        and : 'A corner hands itself to the placement based parts of the API as the matching point.'
            UI.Corner.TOP_LEFT.toPlacement() == UI.Placement.TOP_LEFT
            UI.Corner.EVERY.toPlacement() == UI.Placement.UNDEFINED
            UI.Side.LEFT.toPlacement() == UI.Placement.LEFT
    }

    def 'Reversing a `UI.Span` paints the picture that handing the two gradient colours over in the other order paints.'()
    {
        reportInfo """
            A `UI.Span` says where a gradient starts and where it ends, either from the
            middle of one side to the middle of the facing side, or from one corner to the
            facing corner. `UI.Span.reversed()` returns the span running the other way.

            There are two ways to make a gradient point the other direction: keep the
            colours and reverse the span, or keep the span and hand the colours over in
            the other order. Both have to produce the very same picture.

            This scenario paints two boxes of 140 by 90 pixels. The first is given the
            colours (220, 60, 60) and (60, 90, 200), in that order, across
            `UI.Span.TOP_LEFT_TO_BOTTOM_RIGHT.reversed()`. The second is given the two
            colours in the other order across `UI.Span.TOP_LEFT_TO_BOTTOM_RIGHT` itself.
            The two renderings may not differ in a single colour channel of a single
            pixel, and reversing any of the eight spans twice has to return it.

            **Why this is fragile:** `reversed()` is a switch pairing up eight constants,
            and every answer it can give is another valid `UI.Span`, so a transposed pair
            compiles and paints. A reviewer reading
            `case TOP_RIGHT_TO_BOTTOM_LEFT: return BOTTOM_LEFT_TO_TOP_RIGHT;` has to hold
            four corner names in mind to judge it. On screen the mistake is plain: the
            gradient runs along the wrong diagonal.
        """
        given : 'Two colours and the span a gradient was declared with.'
            var first  = new Color(220, 60, 60)
            var second = new Color(60, 90, 200)
            var span   = UI.Span.TOP_LEFT_TO_BOTTOM_RIGHT

        and : 'A box of 140 by 90 pixels which reverses the span and keeps the colours in their order.'
            var reversedSpan =
                    UI.box().withStyle( it -> it
                        .size(140, 90)
                        .gradient( grad -> grad.colors(first, second).span(span.reversed()) )
                    )
                    .get(JBox)

        and : 'A box of the same size which keeps the span and swaps the two colours instead.'
            var swappedColours =
                    UI.box().withStyle( it -> it
                        .size(140, 90)
                        .gradient( grad -> grad.colors(second, first).span(span) )
                    )
                    .get(JBox)

        when : 'Both boxes are painted.'
            var reversedImage = Utility.renderSingleComponent(reversedSpan)
            var swappedImage  = Utility.renderSingleComponent(swappedColours)

        then : 'The two pictures are identical, down to the last colour channel.'
            Utility.worstChannelDelta(reversedImage, swappedImage) == 0

        and : 'Reversing a span twice gives the original span back.'
            UI.Span.values().every { it.reversed().reversed() == it }
    }

    def 'A `UI.Span` reports the two points it runs between, and it ends at the point facing the one it starts at.'()
    {
        reportInfo """
            Every span is defined by the point it starts at and the point it ends at, and
            `UI.Span.from()` and `UI.Span.to()` hand those out as `UI.Placement` values.
            The first colour given to `colors(..)` sits at `from()`, and the last one at
            `to()`.

            Because the endpoints are points of the component, everything else follows
            from them, with no second list of constants to keep in step. A span is
            diagonal exactly when it starts at a corner, and a span which is not diagonal
            runs along an axis which can be named. A diagonal span runs along no single
            axis, which is why `UI.Span.axis()` hands back an empty `Optional` rather than
            picking one of the two.

            This scenario requires `UI.Span.LEFT_TO_RIGHT` to run from `UI.Placement.LEFT`
            to `UI.Placement.RIGHT`, `UI.Span.TOP_LEFT_TO_BOTTOM_RIGHT` to run from
            `UI.Placement.TOP_LEFT` to `UI.Placement.BOTTOM_RIGHT`, and each of the eight
            spans to end at the `opposite()` of the point it starts at. Exactly the four
            corner to corner spans have to report `isDiagonal()`, the two side to side
            spans across the component have to name `UI.Axis.HORIZONTAL`, the one down
            the component `UI.Axis.VERTICAL`, and a diagonal span has to name none.

            **Why this is fragile:** the two endpoints are written out by hand next to
            each constant, and nothing in the compiler checks that
            `BOTTOM_LEFT_TO_TOP_RIGHT` was given `UI.Placement.BOTTOM_LEFT` rather than
            `UI.Placement.BOTTOM_RIGHT`. The requirement that a span end at the opposite
            of its start is what catches such a slip, because a mismatched pair breaks it.
            The consequence of one reaches the screen: `isDiagonal()` is defined as
            `from().isCorner()`, and SwingTree chooses between its diagonal and its
            straight gradient painter from that answer, so a straight span given a corner
            as its start would be painted corner to corner.
        """
        expect : 'A side to side span starts and ends at the middle of two facing edges.'
            UI.Span.LEFT_TO_RIGHT.from() == UI.Placement.LEFT
            UI.Span.LEFT_TO_RIGHT.to()   == UI.Placement.RIGHT

        and : 'A corner to corner span starts and ends at two facing corners.'
            UI.Span.TOP_LEFT_TO_BOTTOM_RIGHT.from() == UI.Placement.TOP_LEFT
            UI.Span.TOP_LEFT_TO_BOTTOM_RIGHT.to()   == UI.Placement.BOTTOM_RIGHT

        and : 'A span always ends at the point facing the one it starts at.'
            UI.Span.values().every { it.to() == it.from().opposite() }

        and : 'Exactly the four corner to corner spans call themselves diagonal.'
            UI.Span.values().findAll { it.isDiagonal() } as Set == [
                UI.Span.TOP_LEFT_TO_BOTTOM_RIGHT, UI.Span.BOTTOM_LEFT_TO_TOP_RIGHT,
                UI.Span.TOP_RIGHT_TO_BOTTOM_LEFT, UI.Span.BOTTOM_RIGHT_TO_TOP_LEFT
            ] as Set

        and : 'A straight span names the axis it travels along.'
            UI.Span.LEFT_TO_RIGHT.axis().get() == UI.Axis.HORIZONTAL
            UI.Span.RIGHT_TO_LEFT.axis().get() == UI.Axis.HORIZONTAL
            UI.Span.TOP_TO_BOTTOM.axis().get() == UI.Axis.VERTICAL

        and : 'A diagonal span travels along no single axis, so it names none.'
            !UI.Span.TOP_LEFT_TO_BOTTOM_RIGHT.axis().isPresent()
    }

    def '`UI.Cursor.resizeAt(..)` returns the arrow belonging on a resize handle sitting at a given side or corner.'()
    {
        reportInfo """
            A window or a panel which the user can resize usually carries invisible
            handles along its edges and in its corners, and each handle should show the
            arrow pointing the way that handle moves.

            The handle already knows which `UI.Side` or `UI.Corner` it sits on, so it
            should not have to name the cursor a second time.
            `UI.Cursor.resizeAt(..)` performs that lookup, which lets a loop over the four
            sides build all four handles.

            This scenario puts `UI.Cursor.resizeAt(UI.Side.BOTTOM)` on one button and
            `UI.Cursor.resizeAt(UI.Corner.BOTTOM_LEFT)` on a second one, and requires the
            first to carry `java.awt.Cursor.S_RESIZE_CURSOR` and the second
            `java.awt.Cursor.SW_RESIZE_CURSOR`. It then pins each of the eight resize
            constants to the AWT cursor it stands for, and requires
            `UI.Cursor.resizeAt(UI.Corner.EVERY)` to be `UI.Cursor.DEFAULT`, because
            `EVERY` names all four corners at once and so has no one arrow. Finally,
            asking for a cursor has to hand out the instance AWT already keeps rather than
            building a new one.

            **Why this is fragile:** SwingTree names these constants after the side or the
            corner a drag would move, while AWT names its own after compass points, so
            `UI.Cursor.RESIZE_BOTTOM_LEFT` has to be built from
            `java.awt.Cursor.SW_RESIZE_CURSOR`. South is down and west is left, and
            getting one of those eight pairings wrong compiles, throws nothing, and shows
            up nowhere except under the mouse of the user, whose corner handle then offers
            to resize along the wrong diagonal.
        """
        given : 'A handle sitting on the bottom edge of a resizable panel.'
            var handle = UI.button().withCursor(UI.Cursor.resizeAt(UI.Side.BOTTOM)).get(javax.swing.JButton)

        and : 'A handle sitting in the corner next to it.'
            var cornerHandle = UI.button().withCursor(UI.Cursor.resizeAt(UI.Corner.BOTTOM_LEFT)).get(javax.swing.JButton)

        expect : 'The edge handle shows the arrow that resizes downwards.'
            handle.cursor.type == java.awt.Cursor.S_RESIZE_CURSOR

        and : 'The corner handle shows the diagonal arrow of that corner.'
            cornerHandle.cursor.type == java.awt.Cursor.SW_RESIZE_CURSOR

        and : 'Every side maps to the arrow that points away from the middle of the component.'
            UI.Cursor.resizeAt(UI.Side.TOP).toAWTCursor().type    == java.awt.Cursor.N_RESIZE_CURSOR
            UI.Cursor.resizeAt(UI.Side.LEFT).toAWTCursor().type   == java.awt.Cursor.W_RESIZE_CURSOR
            UI.Cursor.resizeAt(UI.Side.RIGHT).toAWTCursor().type  == java.awt.Cursor.E_RESIZE_CURSOR

        and : 'And every corner maps to its own diagonal arrow.'
            UI.Cursor.resizeAt(UI.Corner.TOP_LEFT).toAWTCursor().type     == java.awt.Cursor.NW_RESIZE_CURSOR
            UI.Cursor.resizeAt(UI.Corner.TOP_RIGHT).toAWTCursor().type    == java.awt.Cursor.NE_RESIZE_CURSOR
            UI.Cursor.resizeAt(UI.Corner.BOTTOM_RIGHT).toAWTCursor().type == java.awt.Cursor.SE_RESIZE_CURSOR

        and : '`EVERY` names no single corner, so there is no one arrow for it and you get the plain pointer.'
            UI.Cursor.resizeAt(UI.Corner.EVERY) == UI.Cursor.DEFAULT

        and : 'Assigning a cursor hands out the instance AWT already keeps, rather than building a new one.'
            UI.Cursor.HAND.toAWTCursor().is(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR))
    }

    def '`UI.FontStyle.withBold(..)` turns boldness on without throwing away the slant of the font.'()
    {
        reportInfo """
            `UI.FontStyle` has four constants, because boldness and slant are two
            independent switches. A toolbar with a **B** button and an *I* button has to
            move between those four, and each button has to change only its own half of
            the answer.

            Written by hand that is a four case switch per button, and the mistake it
            invites is losing the other half: pressing **B** while the text is italic
            should give bold italic, not plain bold. `withBold(..)` and `withItalic(..)`
            do that arithmetic, and `isBold()` and `isItalic()` read the two halves back
            out, which is how the two buttons show whether they are pressed.

            This scenario starts from `UI.FontStyle.ITALIC` and wears it on a label
            through `UI.Font.of("Dialog", style, 12)`. Turning boldness on has to give
            `UI.FontStyle.BOLD_ITALIC`, and a label wearing it has to report a font which
            is both bold and italic. Turning the slant back off has to give
            `UI.FontStyle.BOLD` rather than `UI.FontStyle.PLAIN`. Setting either half to
            the value it already holds has to return the constant unchanged.

            **Why this is fragile:** all four constants are valid answers, so a wrong one
            neither throws nor fails to render. Returning `BOLD` where `BOLD_ITALIC` was
            meant drops the slant, so a user who italicises a word and then emphasises it
            watches the slant vanish under their hands, with no error shown anywhere.
        """
        given : 'Text which is currently italic, and a label wearing a font of that style.'
            var style = UI.FontStyle.ITALIC
            var label = UI.label("Slanted").withFont(UI.Font.of("Dialog", style, 12)).get(JLabel)

        expect : 'The label is slanted but not thickened, and the two buttons would show that.'
            label.font.italic
            !label.font.bold

        and : 'The style itself reports the same two halves.'
            style.isItalic()
            !style.isBold()

        when : 'The user presses the bold button, which turns boldness on and leaves the slant alone.'
            style = style.withBold(true)
            var bolder = UI.label("Slanted").withFont(UI.Font.of("Dialog", style, 12)).get(JLabel)

        then : 'The style is now both, rather than having traded one for the other.'
            style == UI.FontStyle.BOLD_ITALIC

        and : 'And the label wearing it is thickened as well as slanted.'
            bolder.font.bold
            bolder.font.italic

        when : 'The user presses the italic button, which turns the slant back off.'
            style = style.withItalic(false)
            var upright = UI.label("Upright").withFont(UI.Font.of("Dialog", style, 12)).get(JLabel)

        then : 'Only the boldness survives, which is what the two button presses asked for.'
            style == UI.FontStyle.BOLD
            !style.isItalic()
            style.isBold()

        and : 'The label is thickened but no longer slanted.'
            upright.font.bold
            !upright.font.italic

        and : 'Setting a half to the value it already has changes nothing.'
            UI.FontStyle.values().every { it.withBold(it.isBold()) == it }
            UI.FontStyle.values().every { it.withItalic(it.isItalic()) == it }
    }

    def '`UI.Active.decide(..)` lets only `AS_NEEDED` consult the situation, so a condition cannot overrule the user.'()
    {
        reportInfo """
            `UI.Active` is the enum behind a scroll bar policy: show it never, show it
            always, or show it only when it is needed. Nothing about it is specific to
            scroll bars, so it fits any setting with those same three answers, such as
            whether to show a strip of warnings.

            `UI.Active.decide(..)` takes one boolean saying whether the situation calls
            for the thing, and only one of the three constants looks at it. `NEVER` and
            `ALWAYS` are answers the user of the program already gave, and a condition
            evaluated at runtime must not overrule them.

            This scenario asks all three constants twice, once with a situation which
            calls for the thing (there are warnings to show) and once with one which does
            not (all is well). `AS_NEEDED` has to follow the situation both times, `NEVER`
            has to answer false both times, and `ALWAYS` has to answer true both times.

            **Why this is fragile:** the tempting hand written form is
            `policy == ALWAYS || thereAreWarnings`. It agrees with `decide(..)` on
            `ALWAYS` and on `AS_NEEDED`, and it is wrong on exactly one of the six
            answers here: a user who asked never to see the warning strip is shown it the
            moment a warning turns up, which is the one case they asked against.
        """
        given : 'A situation which does call for the thing: there are warnings to show.'
            var thereAreWarnings = true

        and : 'And one which does not.'
            var allIsWell = false

        expect : 'The automatic policy follows the situation, showing the strip only when there is something to say.'
            UI.Active.AS_NEEDED.decide(thereAreWarnings)
            !UI.Active.AS_NEEDED.decide(allIsWell)

        and : 'A user who asked to never see the strip does not get it, warnings or not.'
            !UI.Active.NEVER.decide(thereAreWarnings)
            !UI.Active.NEVER.decide(allIsWell)

        and : 'And a user who asked to always see it keeps it even when there is nothing to report.'
            UI.Active.ALWAYS.decide(thereAreWarnings)
            UI.Active.ALWAYS.decide(allIsWell)
    }

    def '`UI.DragAction.includes(..)` answers whether one action permits every transfer another one permits.'()
    {
        reportInfo """
            `UI.DragAction` mirrors the transfer actions of drag and drop: copying,
            moving, linking, both copying and moving, or nothing at all.

            `COPY_OR_MOVE` is the constant which makes this more than an equality check,
            because it permits two transfers at once. A drop target which accepts
            `COPY_OR_MOVE` accepts a source which only wants to copy, while a target which
            only accepts `COPY` has to refuse a source insisting on being allowed to move
            as well. `includes(..)` asks that question the right way round: does this
            action permit everything the other one permits?

            This scenario asks `UI.DragAction.COPY_OR_MOVE` about `COPY`, `MOVE` and
            `LINK`, requiring it to permit the first two and refuse the third. It asks the
            question the other way round as well, requiring `COPY` not to permit
            `COPY_OR_MOVE`, and requires copying and moving not to permit each other.
            Finally every one of the five constants has to permit itself, and to permit
            `NONE`, which permits nothing.

            **Why this is fragile:** the natural first attempt is `this == action`, and it
            gives the right answer for every pair except the ones involving
            `COPY_OR_MOVE`, which is the only reason the method exists. What the user sees
            is a drop target refusing a drag it was configured to accept: they drag
            something onto a panel and the panel declines it, with nothing on screen to
            say why.
        """
        given : 'A drop target which lets the user either copy or move onto it.'
            var target = UI.DragAction.COPY_OR_MOVE

        expect : 'It accepts a source that only wants to copy, and one that only wants to move.'
            target.includes(UI.DragAction.COPY)
            target.includes(UI.DragAction.MOVE)

        and : 'It does not accept a source that wants to create a link, which is a different transfer.'
            !target.includes(UI.DragAction.LINK)

        and : 'A target that only copies cannot promise the freedom that copy-or-move promises.'
            !UI.DragAction.COPY.includes(target)

        and : 'Neither copying nor moving covers the other.'
            !UI.DragAction.COPY.includes(UI.DragAction.MOVE)
            !UI.DragAction.MOVE.includes(UI.DragAction.COPY)

        and : 'Every action permits everything it permits, and everything the empty action permits.'
            UI.DragAction.values().every { it.includes(it) }
            UI.DragAction.values().every { it.includes(UI.DragAction.NONE) }

        and : 'Each constant reports the transfers it stands for.'
            UI.DragAction.COPY_OR_MOVE.isCopy()
            UI.DragAction.COPY_OR_MOVE.isMove()
            !UI.DragAction.COPY_OR_MOVE.isLink()
            UI.DragAction.LINK.isLink()
    }

    def 'The regions of the box model report which others they cover and which boundary line each of them begins at.'()
    {
        reportInfo """
            A styled component is built from nested regions: the margin around it, then
            the border, then the padding, then the content. `UI.ComponentArea` names those
            regions and `UI.ComponentBoundary` names the infinitely thin lines between
            them. Clipping an image takes an area, while a gradient can be anchored to a
            boundary.

            Two of the five areas are unions of the others: `ALL` is the whole component,
            and `BODY` is the border together with the interior, which is the component
            with its margin removed. `UI.ComponentArea.contains(..)` spells that algebra
            out, so that code deciding whether one clip already covers another does not
            have to rediscover it.

            The two enums are linked, but they are not mirror images.
            `UI.ComponentArea.outerBoundary()` returns the line an area begins at, and
            `UI.ComponentBoundary.wrappedArea()` goes back the other way. The round trip
            holds only for the three areas which own a boundary outright: `ALL` and
            `EXTERIOR` both begin at the outer edge of the component, so that one line
            cannot name them both, and it names `ALL`.

            This scenario requires `ALL` to cover all five areas, `BODY` to cover `BORDER`,
            `INTERIOR` and itself but neither `EXTERIOR` nor `ALL`, and `EXTERIOR`,
            `BORDER` and `INTERIOR` to cover nothing but themselves. It then pins the
            boundary each of the five areas begins at, walks the three round trips, and
            requires `INTERIOR_TO_CONTENT` and `CENTER_TO_CONTENT` to wrap no area at all,
            because both of them lie inside the interior.

            **Why this is fragile:** the two enums are written out separately, each with
            its own switch, so nothing but a scenario keeps them in step. `contains(..)`
            is what an application asks before skipping a clip it believes is already
            covered, so an area claiming to cover more than it does lets a painter skip a
            clip it needed, and the paint spills out over the margin of the component.
            The round trip is asymmetric for the same reason: `OUTER_TO_EXTERIOR` is
            where both `ALL` and `EXTERIOR` begin, and it has to answer `ALL`. Answering
            `EXTERIOR` would hand back the ring the margin leaves, a thin frame, where
            the whole component was meant, so an application clipping to the area a
            boundary wraps would paint inside that frame and leave the middle of the
            component blank.
        """
        expect : 'The whole component covers every region, itself included.'
            UI.ComponentArea.values().every { UI.ComponentArea.ALL.contains(it) }

        and : 'The body is the border plus the interior, so it covers both and itself.'
            UI.ComponentArea.BODY.contains(UI.ComponentArea.BORDER)
            UI.ComponentArea.BODY.contains(UI.ComponentArea.INTERIOR)
            UI.ComponentArea.BODY.contains(UI.ComponentArea.BODY)

        and : 'But the body stops where the margin begins, so it does not cover the exterior or the whole.'
            !UI.ComponentArea.BODY.contains(UI.ComponentArea.EXTERIOR)
            !UI.ComponentArea.BODY.contains(UI.ComponentArea.ALL)

        and : 'The three regions that do not overlap cover nothing but themselves.'
            [UI.ComponentArea.EXTERIOR, UI.ComponentArea.BORDER, UI.ComponentArea.INTERIOR].every { area ->
                UI.ComponentArea.values().every { other -> area.contains(other) == (area == other) }
            }

        and : 'Each area begins at a boundary, and areas sharing an edge share that boundary.'
            UI.ComponentArea.ALL.outerBoundary()      == UI.ComponentBoundary.OUTER_TO_EXTERIOR
            UI.ComponentArea.EXTERIOR.outerBoundary() == UI.ComponentBoundary.OUTER_TO_EXTERIOR
            UI.ComponentArea.BODY.outerBoundary()     == UI.ComponentBoundary.EXTERIOR_TO_BORDER
            UI.ComponentArea.BORDER.outerBoundary()   == UI.ComponentBoundary.EXTERIOR_TO_BORDER
            UI.ComponentArea.INTERIOR.outerBoundary() == UI.ComponentBoundary.BORDER_TO_INTERIOR

        and : 'Going back from a boundary names the area it wraps, for the three that own one.'
            UI.ComponentBoundary.OUTER_TO_EXTERIOR.wrappedArea().get()  == UI.ComponentArea.ALL
            UI.ComponentBoundary.EXTERIOR_TO_BORDER.wrappedArea().get() == UI.ComponentArea.BODY
            UI.ComponentBoundary.BORDER_TO_INTERIOR.wrappedArea().get() == UI.ComponentArea.INTERIOR

        and : 'Those three round trip, because each of them begins at the boundary that names it.'
            [UI.ComponentArea.ALL, UI.ComponentArea.BODY, UI.ComponentArea.INTERIOR].every {
                it.outerBoundary().wrappedArea().get() == it
            }

        and : 'The last two boundaries lie inside the interior, so they wrap no named area at all.'
            !UI.ComponentBoundary.INTERIOR_TO_CONTENT.wrappedArea().isPresent()
            !UI.ComponentBoundary.CENTER_TO_CONTENT.wrappedArea().isPresent()
    }

    def '`UI.Editability.of(..)` turns the boolean a view model already holds into the constant a table takes.'()
    {
        reportInfo """
            Whether a table may be edited is modelled by `UI.Editability` rather than a
            boolean, so that a call reads as `withEditability(UI.Editability.READ_ONLY)`
            instead of `setEditable(false)`.

            View models rarely store it that way though. Far more often there is already a
            flag, perhaps derived from a permission or from whether a record is locked.
            `UI.Editability.of(..)` is the bridge, so that the flag becomes the constant at
            the one place it enters the user interface.

            This scenario builds a `TableData` of one row, "Alice" and 30, under the column
            names "Name" and "Age", and gives it the editability derived from a flag which
            is false. The `JTable` bound to that data has to refuse to let the user type
            into the cell at row 0 and column 0. The scenario then sets the flag to true,
            rebuilds the table data from it, and requires the new table to accept editing
            of that same cell.

            **Why this is fragile:** `of(..)` is one conditional, and inverting it compiles
            and type checks, because both answers are `UI.Editability` constants. A table
            which came out read only where it should be editable shows the user no error
            and no explanation: they click a cell, nothing happens, and there is nothing on
            screen to say why.
        """
        given : 'A flag from a view model saying whether the current user may edit the records.'
            var userMayEdit = false

        and : 'A table whose editability is derived from exactly that flag.'
            var data = TableData.of(UI.CellOrder.ROW_MAJOR, "Name", "Age")
                                .addRow("Alice", 30)
                                .withEditability(UI.Editability.of(userMayEdit))
            var table = UI.table(Var.of(data)).get(JTable)

        expect : 'The table refuses to let the user type into the cell at row 0 and column 0.'
            !table.isCellEditable(0, 0)

        when : 'The user is given permission and the table is rebuilt from the new flag.'
            userMayEdit = true
            var editableData = data.withEditability(UI.Editability.of(userMayEdit))
            var editableTable = UI.table(Var.of(editableData)).get(JTable)

        then : 'That same cell now accepts editing.'
            editableTable.isCellEditable(0, 0)

        and : 'The two constants say the same thing the flag did.'
            UI.Editability.of(true)  == UI.Editability.EDITABLE
            UI.Editability.of(false) == UI.Editability.READ_ONLY
    }
}
