package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.layout.ResponsiveGridFlowLayout
import swingtree.threading.EventProcessor

import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.border.EmptyBorder
import java.awt.ComponentOrientation
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Rectangle

@Title("Flow Layout Invariance")
@Narrative('''

    The `ResponsiveGridFlowLayout` is modelled after the plain old `FlowLayout`
    of the JDK, and it extends it with responsive cell spans. A hard requirement
    follows from that: **a responsive grid flow layout which was not given any
    responsive behaviour must behave exactly like a `FlowLayout`**. Anybody
    swapping the one for the other should not be able to tell the difference,
    which is what makes `withFlowLayout()` a safe replacement for
    `withLayout(new FlowLayout(..))`.

    This specification pins that equivalence down across alignments, gap sizes,
    container widths, component orientations, container insets, hidden children
    and baseline alignment. Every feature builds the same UI twice, once with
    each layout manager, and then compares the results.

    There is exactly one deliberate exception, and the last two features below
    describe it: `preferredLayoutSize`. A `FlowLayout` always reports the size of
    a single row, even when its children demonstrably wrap into several. The
    responsive grid flow layout reports the height its rows really occupy,
    because a wrapping layout that misreports its height cannot be nested and
    cannot be scrolled correctly.

''')
@Subject([ResponsiveGridFlowLayout])
class Flow_Layout_Invariance_Spec extends Specification
{
    def setup() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
        // The responsive layout scales its gaps for high DPI screens, the AWT one
        // does not. A scale of 1 is what makes the two directly comparable.
        SwingTree.get().setUiScaleFactor(1f)
    }

    def cleanup() {
        SwingTree.clear()
    }

    // ── Little helpers shared by the features below ──────────────────────────

    private static ComponentOrientation _orientation( boolean leftToRight ) {
        return leftToRight ? ComponentOrientation.LEFT_TO_RIGHT : ComponentOrientation.RIGHT_TO_LEFT
    }

    /** The five boxes used by most features, of increasing width. */
    private static List<List<Integer>> FIVE_BOXES() {
        return [[10, 20], [20, 20], [30, 20], [40, 20], [50, 20]]
    }

    private static JPanel _panelOf( def layout, List<List<Integer>> boxes, boolean ltr, boolean withInsets ) {
        return UI.panel().withLayout(layout)
                .peek({ it.setComponentOrientation(_orientation(ltr)) })
                .applyIf(withInsets, { it.peek({ p -> p.setBorder(new EmptyBorder(4, 6, 8, 10)) }) })
                .apply({ ui -> boxes.each { b -> ui.add(UI.box().withPrefSize(b[0], b[1])) } })
                .get(JPanel)
    }

    private static List<Rectangle> _laidOutBoundsOf( JPanel panel, int width, int height ) {
        panel.setSize(width, height)
        panel.doLayout()
        return (0..<panel.getComponentCount()).collect { panel.getComponent(it).getBounds() }
    }

    private static int _contentBottomOf( JPanel panel ) {
        int bottom = 0
        for ( int i = 0; i < panel.getComponentCount(); i++ )
            bottom = Math.max(bottom, panel.getComponent(i).getY() + panel.getComponent(i).getHeight())
        return bottom
    }

    // ── The features ─────────────────────────────────────────────────────────

    def 'A responsive flow layout places its components exactly where a `FlowLayout` places them.'(
        UI.HorizontalAlignment alignment, int horizontalGap, int verticalGap, int width
    ) {
        reportInfo """
            The core of the equivalence: for the very same children and the very
            same container width, every single component has to end up at exactly
            the same position and size under both layout managers.

            The widths in this table are chosen to span the interesting range. The
            five boxes need 180 pixels to fit into a single row when the gaps are
            5, so the narrow widths in this table genuinely force the layout to
            wrap into several rows.
        """
        given : 'Two panels with identical children, one per layout manager.'
            var ours = _panelOf(
                            new ResponsiveGridFlowLayout(alignment, horizontalGap, verticalGap),
                            FIVE_BOXES(), true, false
                        )
            var awts = _panelOf(
                            new FlowLayout(alignment.forFlowLayout().orElse(FlowLayout.CENTER), horizontalGap, verticalGap),
                            FIVE_BOXES(), true, false
                        )
        when : 'Both are given the same size and told to lay out their children,'
            var ourBounds = _laidOutBoundsOf(ours, width, 200)
            var awtBounds = _laidOutBoundsOf(awts, width, 200)
        then : 'every child ends up in exactly the same place.'
            ourBounds == awtBounds

        where :
            alignment                       | horizontalGap | verticalGap | width
            UI.HorizontalAlignment.LEFT     | 0             | 0           | 0
            UI.HorizontalAlignment.LEFT     | 0             | 0           | 60
            UI.HorizontalAlignment.LEFT     | 0             | 0           | 300
            UI.HorizontalAlignment.LEFT     | 5             | 5           | 30
            UI.HorizontalAlignment.LEFT     | 5             | 5           | 100
            UI.HorizontalAlignment.LEFT     | 5             | 5           | 300
            UI.HorizontalAlignment.LEFT     | 7             | 3           | 175
            UI.HorizontalAlignment.CENTER   | 0             | 0           | 60
            UI.HorizontalAlignment.CENTER   | 5             | 5           | 100
            UI.HorizontalAlignment.CENTER   | 5             | 5           | 300
            UI.HorizontalAlignment.CENTER   | 7             | 3           | 175
            UI.HorizontalAlignment.RIGHT    | 0             | 0           | 60
            UI.HorizontalAlignment.RIGHT    | 5             | 5           | 100
            UI.HorizontalAlignment.RIGHT    | 5             | 5           | 300
            UI.HorizontalAlignment.RIGHT    | 7             | 3           | 175
            UI.HorizontalAlignment.LEADING  | 0             | 0           | 60
            UI.HorizontalAlignment.LEADING  | 5             | 5           | 100
            UI.HorizontalAlignment.LEADING  | 5             | 5           | 300
            UI.HorizontalAlignment.TRAILING | 0             | 0           | 60
            UI.HorizontalAlignment.TRAILING | 5             | 5           | 100
            UI.HorizontalAlignment.TRAILING | 5             | 5           | 300
    }

    def 'A right-to-left container is laid out exactly like a right-to-left `FlowLayout` container.'(
        UI.HorizontalAlignment alignment, boolean leftToRight, int width
    ) {
        reportInfo """
            Both layout managers mirror their rows when the container has a
            right-to-left component orientation, and the `LEADING` and `TRAILING`
            alignments swap sides with it. This has to agree as well, otherwise
            the two would drift apart for anybody building a UI for a
            right-to-left locale.
        """
        given : 'Two panels with the same component orientation, one per layout manager.'
            var ours = _panelOf(new ResponsiveGridFlowLayout(alignment, 5, 5), FIVE_BOXES(), leftToRight, false)
            var awts = _panelOf(
                            new FlowLayout(alignment.forFlowLayout().orElse(FlowLayout.CENTER), 5, 5),
                            FIVE_BOXES(), leftToRight, false
                        )
        expect : 'The two panels really do carry the orientation we asked for.'
            ours.getComponentOrientation().isLeftToRight() == leftToRight
            awts.getComponentOrientation().isLeftToRight() == leftToRight

        when : 'Both are laid out at the same width,'
            var ourBounds = _laidOutBoundsOf(ours, width, 200)
            var awtBounds = _laidOutBoundsOf(awts, width, 200)
        then : 'the mirrored positions agree exactly.'
            ourBounds == awtBounds

        where :
            alignment                       | leftToRight | width
            UI.HorizontalAlignment.LEFT     | false       | 100
            UI.HorizontalAlignment.LEFT     | false       | 300
            UI.HorizontalAlignment.CENTER   | false       | 100
            UI.HorizontalAlignment.CENTER   | false       | 300
            UI.HorizontalAlignment.RIGHT    | false       | 100
            UI.HorizontalAlignment.RIGHT    | false       | 300
            UI.HorizontalAlignment.LEADING  | false       | 100
            UI.HorizontalAlignment.LEADING  | false       | 300
            UI.HorizontalAlignment.TRAILING | false       | 100
            UI.HorizontalAlignment.TRAILING | false       | 300
            UI.HorizontalAlignment.LEADING  | true        | 100
            UI.HorizontalAlignment.TRAILING | true        | 100
    }

    def 'The insets of the container are honoured exactly like a `FlowLayout` honours them.'(
        UI.HorizontalAlignment alignment, int width
    ) {
        reportInfo """
            A border on the container shrinks the space available for the rows and
            shifts them right and down. Both layout managers read those insets
            from the container itself, so the outcome has to be identical.
        """
        given : 'Two panels carrying the same asymmetric border, one per layout manager.'
            var ours = _panelOf(new ResponsiveGridFlowLayout(alignment, 5, 5), FIVE_BOXES(), true, true)
            var awts = _panelOf(
                            new FlowLayout(alignment.forFlowLayout().orElse(FlowLayout.CENTER), 5, 5),
                            FIVE_BOXES(), true, true
                        )
        when : 'Both are laid out at the same width,'
            var ourBounds = _laidOutBoundsOf(ours, width, 200)
            var awtBounds = _laidOutBoundsOf(awts, width, 200)
        then : 'the inset rows agree exactly.'
            ourBounds == awtBounds

        where :
            alignment                     | width
            UI.HorizontalAlignment.LEFT   | 60
            UI.HorizontalAlignment.LEFT   | 175
            UI.HorizontalAlignment.LEFT   | 300
            UI.HorizontalAlignment.CENTER | 60
            UI.HorizontalAlignment.CENTER | 175
            UI.HorizontalAlignment.CENTER | 300
            UI.HorizontalAlignment.RIGHT  | 60
            UI.HorizontalAlignment.RIGHT  | 175
            UI.HorizontalAlignment.RIGHT  | 300
    }

    def 'Children of differing heights are aligned in their row exactly like a `FlowLayout` aligns them.'(
        UI.HorizontalAlignment alignment, int width
    ) {
        reportInfo """
            A row is as tall as its tallest child, and the shorter children are
            centered within it. Feeding the layout children of four different
            heights makes any disagreement about the row height, or about the
            vertical centering inside it, immediately visible.
        """
        given : 'Two panels holding four equally wide but differently tall children.'
            var boxes = [[40, 10], [40, 30], [40, 20], [40, 50]]
            var ours = _panelOf(new ResponsiveGridFlowLayout(alignment, 5, 5), boxes, true, false)
            var awts = _panelOf(
                            new FlowLayout(alignment.forFlowLayout().orElse(FlowLayout.CENTER), 5, 5),
                            boxes, true, false
                        )
        when : 'Both are laid out at the same width,'
            var ourBounds = _laidOutBoundsOf(ours, width, 300)
            var awtBounds = _laidOutBoundsOf(awts, width, 300)
        then : 'the row heights and the vertical placement inside them agree exactly.'
            ourBounds == awtBounds

        where :
            alignment                     | width
            UI.HorizontalAlignment.LEFT   | 60
            UI.HorizontalAlignment.LEFT   | 110
            UI.HorizontalAlignment.LEFT   | 300
            UI.HorizontalAlignment.CENTER | 60
            UI.HorizontalAlignment.CENTER | 110
            UI.HorizontalAlignment.CENTER | 300
            UI.HorizontalAlignment.RIGHT  | 110
    }

    def 'Baseline aligned rows come out exactly like the baseline aligned rows of a `FlowLayout`.'(
        UI.HorizontalAlignment alignment, int width
    ) {
        reportInfo """
            Both layout managers can align the children of a row on their text
            baseline instead of centering them. A baseline aligned row is as tall
            as the deepest ascent plus the deepest descent found in it, and those
            two may well come from two different children — which makes such a row
            taller than any single child in it.

            The two labels below are picked to make exactly that happen: a large
            one whose baseline sits low in its bounds, and a small one padded from
            below so that its baseline sits high in its bounds.
        """
        given : 'A layout manager of each kind, both aligning on the baseline.'
            var ourLayout = new ResponsiveGridFlowLayout(alignment, 5, 5)
            ourLayout.setAlignOnBaseline(true)
            var awtLayout = new FlowLayout(alignment.forFlowLayout().orElse(FlowLayout.CENTER), 5, 5)
            awtLayout.setAlignOnBaseline(true)
        and : 'Two panels holding labels with deliberately mismatched baselines.'
            var mismatchedLabels = { ->
                var big = new JLabel("Big")
                big.setFont(new Font("SansSerif", Font.PLAIN, 40))
                var low = new JLabel("low")
                low.setFont(new Font("SansSerif", Font.PLAIN, 10))
                low.setBorder(new EmptyBorder(0, 0, 40, 0))
                return [big, low]
            }
            var ours = UI.panel().withLayout(ourLayout)
                            .apply({ ui -> mismatchedLabels().each { l -> ui.add(UI.of(l)) } })
                            .get(JPanel)
            var awts = UI.panel().withLayout(awtLayout)
                            .apply({ ui -> mismatchedLabels().each { l -> ui.add(UI.of(l)) } })
                            .get(JPanel)
        when : 'Both are laid out at the same width,'
            var ourBounds = _laidOutBoundsOf(ours, width, 300)
            var awtBounds = _laidOutBoundsOf(awts, width, 300)
        then : 'the baseline shifted positions agree exactly.'
            ourBounds == awtBounds

        where :
            alignment                     | width
            UI.HorizontalAlignment.LEFT   | 30
            UI.HorizontalAlignment.LEFT   | 100
            UI.HorizontalAlignment.LEFT   | 300
            UI.HorizontalAlignment.CENTER | 100
            UI.HorizontalAlignment.CENTER | 300
            UI.HorizontalAlignment.RIGHT  | 100
            UI.HorizontalAlignment.RIGHT  | 300
    }

    def 'The preferred size of a baseline aligned row is the one a `FlowLayout` reports.'(
        UI.HorizontalAlignment alignment, int width
    ) {
        reportInfo """
            Aligning on the baseline does not only move the children around, it
            also makes the row taller than its tallest child whenever the deepest
            ascent and the deepest descent come from two different children. Both
            layout managers have to fold that into the height they report.

            The widths in this table all leave the two labels in a single row, so
            there is no wrapping involved and the two layout managers have to
            agree exactly — this is the guard which keeps the responsive layout
            from quietly reporting the height of its tallest child instead.
        """
        given : 'A layout manager of each kind, both aligning on the baseline.'
            var ourLayout = new ResponsiveGridFlowLayout(alignment, 5, 5)
            ourLayout.setAlignOnBaseline(true)
            var awtLayout = new FlowLayout(alignment.forFlowLayout().orElse(FlowLayout.CENTER), 5, 5)
            awtLayout.setAlignOnBaseline(true)
        and : 'Two panels holding labels whose baselines sit at very different depths.'
            var mismatchedLabels = { ->
                var big = new JLabel("Big")
                big.setFont(new Font("SansSerif", Font.PLAIN, 40))
                var low = new JLabel("low")
                low.setFont(new Font("SansSerif", Font.PLAIN, 10))
                low.setBorder(new EmptyBorder(0, 0, 40, 0))
                return [big, low]
            }
            var ours = UI.panel().withLayout(ourLayout)
                            .apply({ ui -> mismatchedLabels().each { l -> ui.add(UI.of(l)) } })
                            .get(JPanel)
            var awts = UI.panel().withLayout(awtLayout)
                            .apply({ ui -> mismatchedLabels().each { l -> ui.add(UI.of(l)) } })
                            .get(JPanel)

        when : 'Both are laid out at a width which keeps the two labels in one row,'
            _laidOutBoundsOf(ours, width, 300)
            _laidOutBoundsOf(awts, width, 300)
        then : 'they agree on the height that aligning the baselines demands.'
            ours.getLayout().preferredLayoutSize(ours) == awts.getLayout().preferredLayoutSize(awts)
        and : """
            And that height really is more than the tallest of the two labels
            needs on its own, which is what makes this a meaningful check.
        """
            var tallest = Math.max((int) ours.getComponent(0).getPreferredSize().height,
                                   (int) ours.getComponent(1).getPreferredSize().height)
            ours.getLayout().preferredLayoutSize(ours).height > tallest + 10

        where : 'A width of 0 is the state before the first layout pass.'
            alignment                     | width
            UI.HorizontalAlignment.LEFT   | 0
            UI.HorizontalAlignment.LEFT   | 300
            UI.HorizontalAlignment.CENTER | 0
            UI.HorizontalAlignment.CENTER | 300
            UI.HorizontalAlignment.RIGHT  | 300
    }

    def 'Hidden children are skipped exactly like a `FlowLayout` skips them.'(
        UI.HorizontalAlignment alignment, int width
    ) {
        reportInfo """
            An invisible child takes part in neither the rows nor the reported
            sizes. The hidden child in this feature is deliberately far wider and
            taller than its visible siblings, so that a layout manager which
            failed to skip it would produce very obviously different numbers.
        """
        given : 'A panel builder which places a large hidden child between two small visible ones.'
            var withHidden = { layout ->
                return UI.panel().withLayout(layout)
                        .add(UI.box().withPrefSize(30, 20))
                        .add(UI.box().withPrefSize(200, 90).peek({ it.setVisible(false) }))
                        .add(UI.box().withPrefSize(30, 20))
                        .get(JPanel)
            }
            var ours = withHidden(new ResponsiveGridFlowLayout(alignment, 5, 5))
            var awts = withHidden(new FlowLayout(alignment.forFlowLayout().orElse(FlowLayout.CENTER), 5, 5))
        when : 'Both are laid out at the same width,'
            var ourBounds = _laidOutBoundsOf(ours, width, 200)
            var awtBounds = _laidOutBoundsOf(awts, width, 200)
        then : 'the visible children agree exactly.'
            ourBounds == awtBounds
        and : 'and so does the minimum size, which the hidden child must not inflate.'
            ours.getLayout().minimumLayoutSize(ours) == awts.getLayout().minimumLayoutSize(awts)

        where :
            alignment                     | width
            UI.HorizontalAlignment.LEFT   | 50
            UI.HorizontalAlignment.LEFT   | 300
            UI.HorizontalAlignment.CENTER | 50
            UI.HorizontalAlignment.CENTER | 300
            UI.HorizontalAlignment.RIGHT  | 300
    }

    def 'The minimum layout size is exactly the minimum layout size of a `FlowLayout`.'(
        UI.HorizontalAlignment alignment, int horizontalGap, int verticalGap, boolean withInsets
    ) {
        reportInfo """
            `minimumLayoutSize` answers with a single row of minimum sized
            children under both layout managers, and it does so regardless of how
            wide the container currently is. This is the sharpest part of the
            equivalence: unlike the preferred size, there is no wrapping involved
            and therefore no reason for the two to ever differ.
        """
        given : 'Two panels, one per layout manager.'
            var ours = _panelOf(new ResponsiveGridFlowLayout(alignment, horizontalGap, verticalGap), FIVE_BOXES(), true, withInsets)
            var awts = _panelOf(
                            new FlowLayout(alignment.forFlowLayout().orElse(FlowLayout.CENTER), horizontalGap, verticalGap),
                            FIVE_BOXES(), true, withInsets
                        )
        expect : 'Before any layout pass the two agree.'
            ours.getLayout().minimumLayoutSize(ours) == awts.getLayout().minimumLayoutSize(awts)

        when : 'We squeeze both containers into a width which forces several rows,'
            _laidOutBoundsOf(ours, 60, 200)
            _laidOutBoundsOf(awts, 60, 200)
        then : 'the minimum size is unchanged and still agrees.'
            ours.getLayout().minimumLayoutSize(ours) == awts.getLayout().minimumLayoutSize(awts)

        where :
            alignment                     | horizontalGap | verticalGap | withInsets
            UI.HorizontalAlignment.LEFT   | 0             | 0           | false
            UI.HorizontalAlignment.LEFT   | 5             | 5           | false
            UI.HorizontalAlignment.LEFT   | 5             | 5           | true
            UI.HorizontalAlignment.CENTER | 7             | 3           | false
            UI.HorizontalAlignment.CENTER | 7             | 3           | true
            UI.HorizontalAlignment.RIGHT  | 5             | 5           | true
    }

    def 'The preferred layout size is the one of a `FlowLayout` as long as the content does not wrap.'(
        UI.HorizontalAlignment alignment, int horizontalGap, int verticalGap, int width
    ) {
        reportInfo """
            As long as all children fit into a single row there is nothing to
            disagree about, and the preferred size of the two layout managers is
            identical.

            The width of `0` in this table is the state a container is in before
            it has ever been laid out. Both layout managers then assume a single
            row, which is what lets a window pack itself around a flow layout.
        """
        given : 'Two panels, one per layout manager.'
            var ours = _panelOf(new ResponsiveGridFlowLayout(alignment, horizontalGap, verticalGap), FIVE_BOXES(), true, false)
            var awts = _panelOf(
                            new FlowLayout(alignment.forFlowLayout().orElse(FlowLayout.CENTER), horizontalGap, verticalGap),
                            FIVE_BOXES(), true, false
                        )
        when : 'Both are laid out at a width which does not force any wrapping,'
            _laidOutBoundsOf(ours, width, 200)
            _laidOutBoundsOf(awts, width, 200)
        then : 'their preferred sizes are exactly the same.'
            ours.getLayout().preferredLayoutSize(ours) == awts.getLayout().preferredLayoutSize(awts)

        where : 'The five boxes need 180 pixels for one row at a gap of 5, and 170 at a gap of 0.'
            alignment                     | horizontalGap | verticalGap | width
            UI.HorizontalAlignment.LEFT   | 0             | 0           | 0
            UI.HorizontalAlignment.LEFT   | 0             | 0           | 300
            UI.HorizontalAlignment.LEFT   | 5             | 5           | 0
            UI.HorizontalAlignment.LEFT   | 5             | 5           | 300
            UI.HorizontalAlignment.CENTER | 5             | 5           | 0
            UI.HorizontalAlignment.CENTER | 5             | 5           | 300
            UI.HorizontalAlignment.RIGHT  | 7             | 3           | 0
            UI.HorizontalAlignment.RIGHT  | 7             | 3           | 300
    }

    def 'When the content wraps, the preferred height covers the extra rows, unlike a `FlowLayout`.'(
        int width, int expectedRows
    ) {
        reportInfo """
            This is the one deliberate departure from `FlowLayout`, and it is the
            reason the responsive grid flow layout can be nested and scrolled.

            A `FlowLayout` reports the height of a single row no matter how narrow
            its container is, so a parent which trusts that number hands out too
            little height and the wrapped rows are clipped away. The responsive
            grid flow layout instead reports the height its rows really occupy at
            the width it currently has.

            Note that the preferred *width* does not differ: both report the width
            of the ideal single row. Only the height reacts to wrapping.
        """
        given : 'Two panels, one per layout manager.'
            var ours = _panelOf(new ResponsiveGridFlowLayout(UI.HorizontalAlignment.LEFT, 5, 5), FIVE_BOXES(), true, false)
            var awts = _panelOf(new FlowLayout(FlowLayout.LEFT, 5, 5), FIVE_BOXES(), true, false)

        when : 'Both are laid out at a width which forces the children to wrap,'
            var ourBounds = _laidOutBoundsOf(ours, width, 400)
            var awtBounds = _laidOutBoundsOf(awts, width, 400)
        then : 'they still place every child in exactly the same spot...'
            ourBounds == awtBounds
        and : '...and both really did wrap into the number of rows we expect.'
            ourBounds.collect({ it.y }).toSet().size() == expectedRows

        and : 'Both agree on the preferred width, which is the ideal single row.'
            ours.getLayout().preferredLayoutSize(ours).width == awts.getLayout().preferredLayoutSize(awts).width

        and : """
            But only our layout reports a height which actually covers those rows.
            The `FlowLayout` still answers with the height of one single row.
        """
            ours.getLayout().preferredLayoutSize(ours).height == _contentBottomOf(ours) + 5
            awts.getLayout().preferredLayoutSize(awts).height == 20 + 5 + 5
            ours.getLayout().preferredLayoutSize(ours).height > awts.getLayout().preferredLayoutSize(awts).height

        where : '''
            Mind that a child is taken into the current row when `x + width` still
            fits, which is checked *before* the horizontal gap is added. The five
            boxes therefore still squeeze into a single row at a width of 175.
        '''
            width || expectedRows
            30    || 5
            60    || 4
            80    || 3
            100   || 2
    }
}
