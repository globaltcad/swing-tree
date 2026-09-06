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
    stops you from handing one to a method that wanted the other.
    SwingTree replaces all of them with a small set of enums, and this
    specification is about those enums themselves rather than about any
    one component.

    There are only a handful of them and they turn up everywhere:
    `UI.Axis` says which way a thing runs, `UI.Placement` names one point
    of a rectangle, `UI.Side` and `UI.Corner` name an edge and a corner,
    `UI.Span` names the direction a gradient travels, and `UI.ComponentArea`
    together with `UI.ComponentBoundary` names the regions and the dividing
    lines of the box model.

    Each of them carries a few small methods, so that your own code can ask
    the enum a question instead of writing a switch over its constants.
    The scenarios below show the situations those methods were made for.

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

    def 'A slider reads `UI.Axis.LINE` as horizontal, and a box layout reads it as a reading direction.'()
    {
        reportInfo """
            `UI.Axis` has four constants, and at first glance two of them look
            redundant. `HORIZONTAL` and `VERTICAL` say which way a thing runs.
            `LINE` says "the way a line of text runs" and `PAGE` says "the way
            lines follow each other down a page", both of which are decided by
            the container's reading direction.

            Here is the thing to remember: only a `BoxLayout` can act on that
            difference, because only a `BoxLayout` can place its children right
            to left. Every other component just needs an axis, so it calls
            `UI.Axis.resolve()` and reads `LINE` as `HORIZONTAL` and `PAGE` as
            `VERTICAL`.

            That is why a slider declared with `LINE` is simply a horizontal
            slider, while a box layout declared with `LINE` installs
            `BoxLayout.LINE_AXIS` and keeps the distinction alive.

            If this were not so, `UI.slider(UI.Axis.LINE)` would have to either
            throw or guess, and a view model that models its axis as `LINE`
            could not feed a slider and a box layout from the same property.
        """
        given : 'A slider and a panel, both declared along the very same axis constant.'
            var slider = UI.slider(UI.Axis.LINE).get(JSlider)
            var panel  = UI.panel().withBoxLayout(UI.Axis.LINE).get(javax.swing.JPanel)

        expect : 'The slider is horizontal, because a slider has no reading direction to honour.'
            slider.orientation == SwingConstants.HORIZONTAL

        and : 'The box layout however keeps the reading direction, by using the line axis.'
            panel.layout instanceof BoxLayout
            ((BoxLayout) panel.layout).axis == BoxLayout.LINE_AXIS

        and : 'Asking the enum directly gives the same answer the slider arrived at.'
            UI.Axis.LINE.resolve() == UI.Axis.HORIZONTAL
            UI.Axis.PAGE.resolve() == UI.Axis.VERTICAL
            UI.Axis.LINE.isHorizontal()
            !UI.Axis.PAGE.isHorizontal()
    }

    def 'Use `UI.Axis.perpendicular()` to describe the thing that lies across an axis.'()
    {
        reportInfo """
            A split pane laid out along the horizontal axis puts its two
            components side by side, which means the divider between them is a
            vertical bar. The axis of the split and the axis of the divider are
            always at right angles, and `perpendicular()` is how you say that
            without naming both constants.

            This matters when the axis is not a literal in your code but a
            value you were handed. Imagine a view model holding the split
            direction: to draw a separator that lies across the split, you need
            the other axis, and writing `axis == HORIZONTAL ? VERTICAL :
            HORIZONTAL` at every such place is exactly the kind of switch this
            method removes.
        """
        given : 'The axis a split pane is laid out along, as it would arrive from a view model.'
            var splitAxis = UI.Axis.HORIZONTAL

        and : 'A split pane along that axis, and a separator lying across it.'
            var splitPane = UI.splitPane(splitAxis).get(JSplitPane)
            var separator = UI.separator(splitAxis.perpendicular()).get(javax.swing.JSeparator)

        expect : 'The split places its two components left and right of each other.'
            splitPane.orientation == JSplitPane.HORIZONTAL_SPLIT

        and : 'The separator, lying across that split, runs vertically.'
            separator.orientation == SwingConstants.VERTICAL

        and : 'Going across twice brings you back to where you started.'
            splitAxis.perpendicular().perpendicular() == splitAxis
    }

    def 'A `UI.Side` knows the axis its tab strip runs along, and which side faces it.'()
    {
        reportInfo """
            `UI.Side` names one of the four edges of a component, and it is how
            you tell a tabbed pane where to keep its tabs.

            An edge is a line, so it has an axis of its own, and it is easy to
            get that backwards. The **top** edge is a **horizontal** line, so a
            tab strip docked to the top is laid out horizontally. The **left**
            edge is a **vertical** line, so tabs docked to the left stack
            downwards. `UI.Side.axis()` answers that question, and
            `UI.Side.opposite()` gives you the edge facing it across the
            component, which is what you need when the user asks to move a
            panel to the other side.
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

    def 'A `UI.Placement` is a point, so the side constants centre the other axis.'()
    {
        reportInfo """
            `UI.Placement` names a single point of a rectangle: one of the four
            corners, the middle of one of the four sides, or the centre.
            A label takes one to decide where its text and icon sit.

            The trap worth knowing is that a `Placement` always answers for
            **both** axes, because a point has both coordinates. So
            `UI.Placement.TOP` is not "the top and leave the rest alone" — it is
            the middle of the top edge, and a label aligned with it is centred
            horizontally as well. If you want the top left, say `TOP_LEFT`;
            if you want to move one axis and leave the other untouched, use
            `withHorizontalAlignment(..)` or `withVerticalAlignment(..)` instead.

            `vertical()` and `horizontal()` are how you read those two
            coordinates back out of a point.
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

    def 'Pass a `UI.ComponentOrientation` to `UI.Placement.of(..)` to resolve a leading alignment.'()
    {
        reportInfo """
            `UI.HorizontalAlignment` has two constants that are not sides at
            all: `LEADING` and `TRAILING`. They mean "wherever a line of text
            begins" and "wherever it ends", which is the left and the right in
            English, and the right and the left in Arabic or Hebrew.

            A `UI.Placement` cannot hold either of them, because a placement is
            an already chosen point and those two are still a question. So the
            question is answered exactly once, when you build the placement:
            `UI.Placement.of(vertical, horizontal, orientation)` reads the
            orientation and hands back a real point.

            Note that the two argument `of(..)` overload reads left to right.
            That is the same choice `java.awt.ComponentOrientation.UNKNOWN`
            makes, and it is why a component whose orientation was never set
            behaves like an English one.
        """
        given : 'A heading that should sit at the top, where the text of the label begins.'
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

        and : 'An unknown reading direction is treated as left to right, matching AWT.'
            UI.Placement.of(vertical, atTextStart, UI.ComponentOrientation.UNKNOWN) == UI.Placement.TOP_LEFT
            UI.Placement.of(vertical, atTextStart) == UI.Placement.TOP_LEFT
            UI.ComponentOrientation.UNKNOWN.isLeftToRightOrUnknown()

        and : 'A resolved placement never reports leading or trailing back to you, only real sides.'
            english.horizontal() == UI.HorizontalAlignment.LEFT
            arabic.horizontal()  == UI.HorizontalAlignment.RIGHT
    }

    def 'Use `UI.Placement.opposite()` to put one thing on the far side of another.'()
    {
        reportInfo """
            When you have placed something at a point of a component and want a
            second thing to sit across from it, you want the point you reach by
            going through the middle and out the other side. That is what
            `opposite()` returns.

            The centre and the undefined placement return themselves, because
            neither of them names a side to be on the far side of.
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

    def 'Round a corner and the one across from it using `UI.Corner.opposite()`.'()
    {
        reportInfo """
            `UI.Corner` names one corner of a component, and the style API takes
            one in `borderRadiusAt(..)` to round just that corner.

            A very common shape rounds two corners that lie across from each
            other, which gives a panel a leaning, ticket-like silhouette.
            `opposite()` lets you name only the first corner and derive the
            second, so a single value can drive the whole shape.

            The scenario proves this the honest way: it paints one box whose
            second corner comes from `opposite()`, and a second box that spells
            both corners out by hand, and then compares the two pictures pixel
            for pixel. If `opposite()` returned the wrong corner, the two boxes
            would lean in different directions and the comparison would fail.
        """
        given : 'A corner to round, as a theme or a view model might supply it.'
            var corner = UI.Corner.TOP_LEFT

        and : 'A box which rounds that corner and derives the second one from it.'
            var derived =
                    UI.box().withStyle( it -> it
                        .size(120, 80)
                        .backgroundColor(new Color(70, 130, 180))
                        .borderRadiusAt(corner, 30)
                        .borderRadiusAt(corner.opposite(), 30)
                    )
                    .get(JBox)

        and : 'A second box which spells both corners out literally.'
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

        then : 'Not a single colour channel of a single pixel differs between them.'
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

    def 'Turning a gradient around with `UI.Span.reversed()` is the same as swapping its colours.'()
    {
        reportInfo """
            A `UI.Span` says where a gradient starts and where it ends, either
            side to side or corner to corner. `reversed()` gives you the span
            that runs the other way.

            There are two ways to make a gradient point the other direction:
            keep the colours and reverse the span, or keep the span and swap the
            colours. They must produce the very same picture, and this scenario
            paints both and compares them.

            That equality is worth pinning down, because `reversed()` is a
            sixteen line switch over eight constants, and a single transposed
            pair in it would be invisible in a code review and obvious on
            screen.
        """
        given : 'Two colours and the span a gradient was declared with.'
            var first  = new Color(220, 60, 60)
            var second = new Color(60, 90, 200)
            var span   = UI.Span.TOP_LEFT_TO_BOTTOM_RIGHT

        and : 'A box which reverses the span and keeps the colours in their order.'
            var reversedSpan =
                    UI.box().withStyle( it -> it
                        .size(140, 90)
                        .gradient( grad -> grad.colors(first, second).span(span.reversed()) )
                    )
                    .get(JBox)

        and : 'A box which keeps the span and swaps the two colours instead.'
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

    def 'A `UI.Span` tells you the two points it runs between, and whether it is diagonal.'()
    {
        reportInfo """
            Every span is defined by the point it starts at and the point it
            ends at, and `from()` and `to()` hand those out as placements.
            This is how the style engine decides where to anchor the first and
            the last colour of a gradient.

            Because the endpoints are placements, the rest follows from them
            without a second list of constants to keep in step: a span is
            diagonal exactly when it starts at a corner, and a span that is not
            diagonal runs along an axis you can name. A diagonal one runs along
            no single axis at all, which is why `axis()` hands back an empty
            `Optional` rather than guessing.
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

    def 'Ask `UI.Cursor.resizeAt(..)` for the cursor that belongs on a resize handle.'()
    {
        reportInfo """
            A window or a panel that the user can resize usually has invisible
            handles along its edges and in its corners, and each handle should
            show the arrow that points the way that handle moves.

            Since the handle already knows which `UI.Side` or `UI.Corner` it
            sits on, it should not have to name the cursor a second time.
            `UI.Cursor.resizeAt(..)` performs that lookup, so a loop over the
            four sides can build all four handles.

            The scenario also pins each constant to the AWT cursor it stands
            for. That mapping is worth a test of its own: these constants used
            to be named after compass directions, and a rename that quietly
            attached `RESIZE_BOTTOM_LEFT` to the north east arrow would show up
            nowhere except under the user's mouse.
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

    def 'Use `UI.FontStyle.withBold(..)` so that turning on bold does not throw away the italics.'()
    {
        reportInfo """
            `UI.FontStyle` has four constants, because bold and italic can each
            be on or off. A toolbar with a **B** button and an *I* button has to
            move between those four, and each button must change only its own
            half of the answer.

            Written by hand that is a switch with four cases per button, and the
            mistake it invites is losing the other half: pressing **B** while
            the text is italic should give you bold italic, not plain bold.
            `withBold(..)` and `withItalic(..)` do that arithmetic, and
            `isBold()` and `isItalic()` read the two halves back out so the two
            buttons can show whether they are pressed.
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

    def 'Let `UI.Active.decide(..)` answer a three way policy for you.'()
    {
        reportInfo """
            `UI.Active` is the enum behind a scroll bar policy: show it never,
            show it always, or show it only when it is needed. It fits any
            setting of that shape, such as whether to show a warning strip.

            The point of `decide(..)` is that only one of the three constants
            actually looks at the situation. `NEVER` and `ALWAYS` are decisions
            the user already made and your runtime condition must not override
            them, which is precisely the mistake a hand written
            `policy == ALWAYS || hasWarnings` makes.
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

    def 'Ask a `UI.DragAction` whether it permits another one using `includes(..)`.'()
    {
        reportInfo """
            `UI.DragAction` mirrors the transfer actions of drag and drop:
            copying, moving, linking, both copying and moving, or nothing.

            `COPY_OR_MOVE` is the one that makes this more than an equality
            check, because it permits two things at once. A drop target that
            accepts `COPY_OR_MOVE` accepts a plain copy, but a target that only
            accepts `COPY` must refuse a request that insists on being allowed
            to move as well. `includes(..)` asks that question the right way
            round: does this action permit everything the other one permits?
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

    def 'The box model areas and boundaries describe one another.'()
    {
        reportInfo """
            A styled component is built from nested regions: the margin around
            it, then the border, then the padding, then the content.
            `UI.ComponentArea` names those regions and `UI.ComponentBoundary`
            names the infinitely thin lines between them. Clipping takes an
            area, while a gradient can be anchored to a boundary.

            Two of the five areas overlap the others: `ALL` is the whole
            component, and `BODY` is the border plus the interior, which is the
            component with its margin removed. `contains(..)` spells that
            algebra out, so that code deciding whether one clip already covers
            another does not have to rediscover it.

            The two enums are linked, but not mirror images. `outerBoundary()`
            gives the line an area begins at, and `wrappedArea()` goes back the
            other way. The round trip only holds for the three areas that own a
            boundary outright: `ALL` and `EXTERIOR` both begin at the outer edge
            of the component, so that one line cannot name them both.
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

    def 'Build a `UI.Editability` from the boolean your view model already has.'()
    {
        reportInfo """
            Whether a table may be edited is modelled by `UI.Editability` rather
            than a boolean, so that a call reads as `withEditability(READ_ONLY)`
            instead of `setEditable(false)`.

            View models rarely store it that way though. Far more often there is
            already a flag, perhaps derived from a permission or from whether a
            record is locked. `UI.Editability.of(..)` is the bridge, so that the
            flag can be turned into the constant at the one place it enters the
            user interface.
        """
        given : 'A flag from a view model saying whether the current user may edit the records.'
            var userMayEdit = false

        and : 'A table whose editability is derived from exactly that flag.'
            var data = TableData.of(UI.CellOrder.ROW_MAJOR, "Name", "Age")
                                .addRow("Alice", 30)
                                .withEditability(UI.Editability.of(userMayEdit))
            var table = UI.table(Var.of(data)).get(JTable)

        expect : 'The table refuses to let the user type into its cells.'
            !table.isCellEditable(0, 0)

        when : 'The user is given permission and the table is rebuilt from the new flag.'
            userMayEdit = true
            var editableData = data.withEditability(UI.Editability.of(userMayEdit))
            var editableTable = UI.table(Var.of(editableData)).get(JTable)

        then : 'The cells accept editing.'
            editableTable.isCellEditable(0, 0)

        and : 'The two constants say the same thing the flag did.'
            UI.Editability.of(true)  == UI.Editability.EDITABLE
            UI.Editability.of(false) == UI.Editability.READ_ONLY
    }
}
