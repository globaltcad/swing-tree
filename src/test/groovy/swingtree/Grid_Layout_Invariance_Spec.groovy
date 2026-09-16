package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.layout.UniformGridLayout
import swingtree.threading.EventProcessor
import utility.SwingTreeTestConfigurator

import javax.swing.BorderFactory
import javax.swing.JPanel
import java.awt.ComponentOrientation
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.Insets

@Title("Grid Layout Invariance")
@Narrative('''

    The `UniformGridLayout` is SwingTree's own version of the `GridLayout` of
    the JDK. Both divide a container into cells of equal size and place one
    component into each cell, filling the grid row by row.

    They differ in how the grid is built. A `GridLayout` ignores its column
    count whenever its row count is not zero, and spreads the components over
    its rows instead. A `UniformGridLayout` starts a new row after the column
    count you give it, and leaves out the rows and columns which no component
    occupies. You switch it to the behaviour of a `GridLayout` with three
    settings: the mode `UniformGridLayout.Mode.SPREAD_OVER_ROWS`, the setting
    `UniformGridLayout.CollapseEmpty.NONE`, and the growth policy
    `UniformGridLayout.OverflowGrowth.ADD_COLUMNS`. A hard requirement follows
    from that: **a `UniformGridLayout` which spreads its components over its
    rows, collapses nothing and grows by columns must behave exactly like a
    `GridLayout`**. Anybody replacing the one with the other should not be able
    to tell the difference.

    This specification pins that equivalence down across row counts, column
    counts, component counts, gap sizes, container sizes, component orientations,
    container insets, hidden children, preferred and minimum sizes, and the
    argument checks of the constructors and setters. Every feature builds the
    same UI twice, once with each layout manager, and then compares the results.

    A `UniformGridLayout` scales its gaps by the UI scale factor of SwingTree, and
    a `GridLayout` does not, so this specification runs at a scale factor of 1.

    The growth policy has to be named because a `GridLayout` answers the question
    it asks in exactly one way. Once a grid holds more components than its declared
    row count times its declared column count, `ADD_COLUMNS` is the policy which
    keeps widening the grid the way a `GridLayout` does, while `ADD_ROWS` and
    `ADD_ROWS_AND_COLUMNS` deliberately make room on the other axis instead. The
    last feature of this specification pins that difference down, so that the one
    policy which does match cannot start matching by accident.

''')
@Subject([UniformGridLayout])
class Grid_Layout_Invariance_Spec extends Specification
{
    def setupSpec() {
        SwingTree.initializeUsing(SwingTreeTestConfigurator.get())
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
        SwingTree.get().setUiScaleFactor(1f)
    }

    def cleanupSpec() {
        SwingTree.clear()
    }

    def 'A `UniformGridLayout` places every component exactly where a `GridLayout` places it.'(
        int rows, int cols, int components, int horizontalGap, int verticalGap, int width, int height
    ) {
        reportInfo """
            The core of the equivalence: for the very same children in a container of
            the very same size, every component has to end up at exactly the same
            position and with exactly the same size under both layout managers.

            The table covers a single row, a single column, grids where either the
            row count or the column count is zero, grids with more components than
            cells, grids with fewer components than cells, and container sizes which
            do not divide evenly into cells. When a container cannot be divided
            evenly, both layout managers centre the grid and leave the few pixels
            that are left over as a margin around it.
        """
        given : 'A uniform grid layout which spreads its components over its rows, collapses nothing and grows by columns, like a `GridLayout` does.'
            var ourLayout = new UniformGridLayout(rows, cols, horizontalGap, verticalGap)
            ourLayout.setMode(UniformGridLayout.Mode.SPREAD_OVER_ROWS)
            ourLayout.setCollapseEmpty(UniformGridLayout.CollapseEmpty.NONE)
            ourLayout.setOverflowGrowth(UniformGridLayout.OverflowGrowth.ADD_COLUMNS)
        and : 'Two panels with the same number of children, one per layout manager.'
            var ours =
                    UI.panel()
                    .withLayout(ourLayout)
                    .apply({ ui -> (0..<components).each { ui.add(UI.box().withPrefSize(30, 20)) } })
                    .get(JPanel)
            var awts =
                    UI.panel()
                    .withLayout(new GridLayout(rows, cols, horizontalGap, verticalGap))
                    .apply({ ui -> (0..<components).each { ui.add(UI.box().withPrefSize(30, 20)) } })
                    .get(JPanel)
        when : 'Both are given the same size and told to lay out their children,'
            ours.setSize(width, height)
            ours.doLayout()
            awts.setSize(width, height)
            awts.doLayout()
            var ourBounds = (0..<components).collect { ours.getComponent(it).getBounds() }
            var awtBounds = (0..<components).collect { awts.getComponent(it).getBounds() }
        then : 'every child really was given a place in the grid,'
            ourBounds.every { it.width > 0 && it.height > 0 }
        and : 'and it is exactly the same place under both layout managers.'
            ourBounds == awtBounds

        where :
            rows | cols | components | horizontalGap | verticalGap | width | height
            1    | 0    | 5          | 0             | 0           | 200   | 50
            0    | 1    | 5          | 0             | 0           | 50    | 200
            0    | 3    | 7          | 5             | 5           | 200   | 120
            2    | 0    | 7          | 5             | 5           | 200   | 120
            3    | 2    | 6          | 0             | 0           | 120   | 90
            3    | 2    | 9          | 5             | 5           | 120   | 90
            2    | 5    | 8          | 5             | 5           | 250   | 60
            2    | 5    | 3          | 5             | 5           | 250   | 60
            3    | 3    | 4          | 0             | 0           | 90    | 90
            2    | 2    | 1          | 4             | 4           | 100   | 100
            4    | 1    | 2          | 7             | 3           | 61    | 103
            2    | 3    | 6          | 7             | 3           | 103   | 97
            1    | 1    | 1          | 0             | 0           | 17    | 13
            5    | 5    | 25         | 1             | 2           | 104   | 111
    }

    def 'A right-to-left container is laid out exactly like a right-to-left `GridLayout` container.'(
        ComponentOrientation orientation, int rows, int cols, int components
    ) {
        reportInfo """
            Both layout managers mirror the columns of the grid when the container has
            a right-to-left component orientation: the first component of every row is
            placed into the rightmost cell. The width of 107 pixels does not divide
            evenly into cells, so the left over pixels have to end up on the same side
            under both layout managers as well.
        """
        given : 'A uniform grid layout which spreads its components over its rows, collapses nothing and grows by columns, like a `GridLayout` does.'
            var ourLayout = new UniformGridLayout(rows, cols, 5, 5)
            ourLayout.setMode(UniformGridLayout.Mode.SPREAD_OVER_ROWS)
            ourLayout.setCollapseEmpty(UniformGridLayout.CollapseEmpty.NONE)
            ourLayout.setOverflowGrowth(UniformGridLayout.OverflowGrowth.ADD_COLUMNS)
        and : 'Two panels with the same component orientation, one per layout manager.'
            var ours =
                    UI.panel()
                    .withLayout(ourLayout)
                    .peek({ it.setComponentOrientation(orientation) })
                    .apply({ ui -> (0..<components).each { ui.add(UI.box().withPrefSize(30, 20)) } })
                    .get(JPanel)
            var awts =
                    UI.panel()
                    .withLayout(new GridLayout(rows, cols, 5, 5))
                    .peek({ it.setComponentOrientation(orientation) })
                    .apply({ ui -> (0..<components).each { ui.add(UI.box().withPrefSize(30, 20)) } })
                    .get(JPanel)
        expect : 'The two panels really do carry the orientation we asked for.'
            ours.getComponentOrientation() == orientation
            awts.getComponentOrientation() == orientation

        when : 'Both are laid out at the same size,'
            ours.setSize(107, 80)
            ours.doLayout()
            awts.setSize(107, 80)
            awts.doLayout()
            var ourBounds = (0..<components).collect { ours.getComponent(it).getBounds() }
            var awtBounds = (0..<components).collect { awts.getComponent(it).getBounds() }
        then : 'the mirrored positions agree exactly.'
            ourBounds == awtBounds

        where :
            orientation                              | rows | cols | components
            ComponentOrientation.RIGHT_TO_LEFT       | 2    | 3    | 5
            ComponentOrientation.RIGHT_TO_LEFT       | 0    | 4    | 6
            ComponentOrientation.RIGHT_TO_LEFT       | 2    | 5    | 8
            ComponentOrientation.RIGHT_TO_LEFT       | 3    | 0    | 7
            ComponentOrientation.LEFT_TO_RIGHT       | 2    | 3    | 5
    }

    def 'The insets of the container are honoured exactly like a `GridLayout` honours them.'(
        ComponentOrientation orientation, int rows, int cols, int components
    ) {
        reportInfo """
            A border on the container shrinks the space which is divided into cells and
            shifts the grid right and down. Both layout managers read those insets from
            the container itself, so the outcome has to be identical. The border used
            here is different on every side, so that a mix up of two sides shows.

            A right-to-left container places its first column against its right inset
            instead of its left inset, so the table runs every grid in both orientations.
            The container is 150 by 91 pixels large, which does not divide evenly into
            cells for most of the grids in the table, so the pixels left over by rounding
            the cell size down have to end up on the same side under both layout managers.
        """
        given : 'A uniform grid layout which spreads its components over its rows, collapses nothing and grows by columns, like a `GridLayout` does.'
            var ourLayout = new UniformGridLayout(rows, cols, 3, 4)
            ourLayout.setMode(UniformGridLayout.Mode.SPREAD_OVER_ROWS)
            ourLayout.setCollapseEmpty(UniformGridLayout.CollapseEmpty.NONE)
            ourLayout.setOverflowGrowth(UniformGridLayout.OverflowGrowth.ADD_COLUMNS)
        and : 'Two panels carrying the same asymmetric border, one per layout manager.'
            var ours =
                    UI.panel()
                    .withLayout(ourLayout)
                    .peek({ it.setBorder(BorderFactory.createEmptyBorder(4, 6, 8, 10)) })
                    .peek({ it.setComponentOrientation(orientation) })
                    .apply({ ui -> (0..<components).each { ui.add(UI.box().withPrefSize(30, 20)) } })
                    .get(JPanel)
            var awts =
                    UI.panel()
                    .withLayout(new GridLayout(rows, cols, 3, 4))
                    .peek({ it.setBorder(BorderFactory.createEmptyBorder(4, 6, 8, 10)) })
                    .peek({ it.setComponentOrientation(orientation) })
                    .apply({ ui -> (0..<components).each { ui.add(UI.box().withPrefSize(30, 20)) } })
                    .get(JPanel)
        expect : 'The insets of both panels are the ones of the border, and both have the orientation from the table.'
            ours.getInsets() == new Insets(4, 6, 8, 10)
            awts.getInsets() == new Insets(4, 6, 8, 10)
            ours.getComponentOrientation() == orientation
            awts.getComponentOrientation() == orientation

        when : 'Both are laid out at the same size,'
            ours.setSize(150, 91)
            ours.doLayout()
            awts.setSize(150, 91)
            awts.doLayout()
            var ourBounds = (0..<components).collect { ours.getComponent(it).getBounds() }
            var awtBounds = (0..<components).collect { awts.getComponent(it).getBounds() }
        then : 'the grid sits inside the insets in exactly the same way.'
            ourBounds == awtBounds

        where :
            orientation                        | rows | cols | components
            ComponentOrientation.LEFT_TO_RIGHT | 2    | 3    | 6
            ComponentOrientation.LEFT_TO_RIGHT | 0    | 2    | 5
            ComponentOrientation.LEFT_TO_RIGHT | 3    | 0    | 4
            ComponentOrientation.LEFT_TO_RIGHT | 2    | 4    | 7
            ComponentOrientation.RIGHT_TO_LEFT | 2    | 3    | 6
            ComponentOrientation.RIGHT_TO_LEFT | 0    | 2    | 5
            ComponentOrientation.RIGHT_TO_LEFT | 3    | 0    | 4
            ComponentOrientation.RIGHT_TO_LEFT | 2    | 4    | 7
    }

    def 'The preferred and minimum sizes of a `UniformGridLayout` are those of a `GridLayout`.'(
        int rows, int cols, List<List<Integer>> preferredSizes, List<List<Integer>> minimumSizes
    ) {
        reportInfo """
            Both layout managers size every cell after the largest child: the preferred
            width of a cell is the largest preferred width of any child, and its
            preferred height is the largest preferred height of any child, which may
            well belong to a different child. The same goes for the minimum size.
            The size of the whole grid is then that cell size times the number of
            columns and rows, plus the gaps between them, plus the insets of the
            container.

            The children in this table deliberately have a different widest and
            tallest member, so that a layout manager which picks both values from
            the same child would show.
        """
        given : 'A uniform grid layout which spreads its components over its rows, collapses nothing and grows by columns, like a `GridLayout` does.'
            var ourLayout = new UniformGridLayout(rows, cols, 5, 3)
            ourLayout.setMode(UniformGridLayout.Mode.SPREAD_OVER_ROWS)
            ourLayout.setCollapseEmpty(UniformGridLayout.CollapseEmpty.NONE)
            ourLayout.setOverflowGrowth(UniformGridLayout.OverflowGrowth.ADD_COLUMNS)
        and : 'Two panels with the same children and the same border, one per layout manager.'
            var ours =
                    UI.panel()
                    .withLayout(ourLayout)
                    .peek({ it.setBorder(BorderFactory.createEmptyBorder(1, 2, 3, 4)) })
                    .apply({ ui ->
                        preferredSizes.eachWithIndex { size, i ->
                            ui.add(
                                UI.box()
                                .withPrefSize(size[0], size[1])
                                .withMinSize(minimumSizes[i][0], minimumSizes[i][1])
                            )
                        }
                    })
                    .get(JPanel)
            var awts =
                    UI.panel()
                    .withLayout(new GridLayout(rows, cols, 5, 3))
                    .peek({ it.setBorder(BorderFactory.createEmptyBorder(1, 2, 3, 4)) })
                    .apply({ ui ->
                        preferredSizes.eachWithIndex { size, i ->
                            ui.add(
                                UI.box()
                                .withPrefSize(size[0], size[1])
                                .withMinSize(minimumSizes[i][0], minimumSizes[i][1])
                            )
                        }
                    })
                    .get(JPanel)
        when : 'We ask both layout managers for the sizes they need,'
            var ourPreferredSize = ours.getLayout().preferredLayoutSize(ours)
            var awtPreferredSize = awts.getLayout().preferredLayoutSize(awts)
            var ourMinimumSize   = ours.getLayout().minimumLayoutSize(ours)
            var awtMinimumSize   = awts.getLayout().minimumLayoutSize(awts)
        then : 'the preferred sizes are identical,'
            ourPreferredSize == awtPreferredSize
        and : 'and so are the minimum sizes.'
            ourMinimumSize == awtMinimumSize

        where :
            rows | cols | preferredSizes                          | minimumSizes
            1    | 0    | [[10, 20], [30, 5], [25, 25]]           | [[5, 10], [15, 2], [12, 12]]
            0    | 2    | [[10, 20], [30, 5], [25, 25]]           | [[5, 10], [15, 2], [12, 12]]
            2    | 5    | [[40, 10], [10, 40], [20, 20]]          | [[20, 5], [5, 20], [10, 10]]
            2    | 5    | (1..8).collect { [it * 3, 30 - it] }    | (1..8).collect { [it, 10 - it] }
            3    | 3    | [[12, 7], [7, 12], [9, 9], [1, 1]]      | [[6, 3], [3, 6], [4, 4], [0, 0]]
            3    | 2    | (1..9).collect { [20, it * 2] }         | (1..9).collect { [10, it] }
    }

    def 'An empty `UniformGridLayout` reports the very same, even negative, size as an empty `GridLayout`.'(
        int rows, int cols, Dimension expectedSize
    ) {
        reportInfo """
            A grid without any children has no cells to size, but it still has gaps.
            When the row count is set, the column count is worked out from the number
            of components, which gives zero columns for zero components. The `GridLayout`
            of the JDK then subtracts one gap for the "minus one" gaps between zero
            columns, and reports a negative width. The `UniformGridLayout` reports exactly
            the same, down to the negative number.
        """
        given : 'A uniform grid layout which spreads its components over its rows, collapses nothing and grows by columns, like a `GridLayout` does.'
            var ourLayout = new UniformGridLayout(rows, cols, 5, 3)
            ourLayout.setMode(UniformGridLayout.Mode.SPREAD_OVER_ROWS)
            ourLayout.setCollapseEmpty(UniformGridLayout.CollapseEmpty.NONE)
            ourLayout.setOverflowGrowth(UniformGridLayout.OverflowGrowth.ADD_COLUMNS)
        and : 'Two empty panels with gaps of 5 horizontally and 3 vertically, one per layout manager.'
            var ours = UI.panel().withLayout(ourLayout).get(JPanel)
            var awts = UI.panel().withLayout(new GridLayout(rows, cols, 5, 3)).get(JPanel)
        expect : 'Both panels really are empty.'
            ours.getComponentCount() == 0
            awts.getComponentCount() == 0
        and : 'The preferred size of the JDK grid is the one in the table,'
            awts.getLayout().preferredLayoutSize(awts) == expectedSize
        and : 'and the uniform grid reports the same preferred and minimum size.'
            ours.getLayout().preferredLayoutSize(ours) == expectedSize
            ours.getLayout().minimumLayoutSize(ours) == awts.getLayout().minimumLayoutSize(awts)

        where :
            rows | cols | expectedSize
            1    | 0    | new Dimension(-5, 0)
            2    | 0    | new Dimension(-5, 3)
            2    | 5    | new Dimension(-5, 3)
            0    | 4    | new Dimension(15, -3)
    }

    def 'A container that is too small for its gaps is laid out exactly like one using a `GridLayout`.'(
        int width, int height
    ) {
        reportInfo """
            When a container is smaller than the gaps between its cells, there is
            no room left for the cells themselves, and both layout managers hand out
            cells with a negative width or height. Nothing about that is useful, but
            it still has to be the same, because a container passes through such sizes
            while a window is being shrunk.
        """
        given : 'A uniform grid layout which spreads its components over its rows, collapses nothing and grows by columns, like a `GridLayout` does.'
            var ourLayout = new UniformGridLayout(2, 3, 5, 5)
            ourLayout.setMode(UniformGridLayout.Mode.SPREAD_OVER_ROWS)
            ourLayout.setCollapseEmpty(UniformGridLayout.CollapseEmpty.NONE)
            ourLayout.setOverflowGrowth(UniformGridLayout.OverflowGrowth.ADD_COLUMNS)
        and : 'Two panels holding a 2 by 3 grid with gaps of 5, one per layout manager.'
            var ours =
                    UI.panel()
                    .withLayout(ourLayout)
                    .apply({ ui -> (0..<6).each { ui.add(UI.box().withPrefSize(30, 20)) } })
                    .get(JPanel)
            var awts =
                    UI.panel()
                    .withLayout(new GridLayout(2, 3, 5, 5))
                    .apply({ ui -> (0..<6).each { ui.add(UI.box().withPrefSize(30, 20)) } })
                    .get(JPanel)
        when : 'Both are squeezed to the same tiny size and laid out,'
            ours.setSize(width, height)
            ours.doLayout()
            awts.setSize(width, height)
            awts.doLayout()
            var ourBounds = (0..<6).collect { ours.getComponent(it).getBounds() }
            var awtBounds = (0..<6).collect { awts.getComponent(it).getBounds() }
        then : 'the JDK grid really does hand out cells without any room,'
            awtBounds.every { it.width <= 0 || it.height <= 0 }
        and : 'and the uniform grid hands out exactly the same ones.'
            ourBounds == awtBounds

        where :
            width | height
            0     | 0
            3     | 2
            9     | 40
            100   | 4
    }

    def 'A hidden child still occupies its cell, exactly like it does in a `GridLayout`.'()
    {
        reportInfo """
            Unlike a flow layout, a grid layout does not skip components which are not
            visible. A hidden child keeps its cell, so the children after it stay where
            they are when it is hidden or shown again. The `UniformGridLayout` has to
            keep that behaviour, because a grid in which components move around when
            one of them is hidden would be a different layout altogether.
        """
        given : 'A uniform grid layout which spreads its components over its rows, collapses nothing and grows by columns, like a `GridLayout` does.'
            var ourLayout = new UniformGridLayout(2, 3, 5, 5)
            ourLayout.setMode(UniformGridLayout.Mode.SPREAD_OVER_ROWS)
            ourLayout.setCollapseEmpty(UniformGridLayout.CollapseEmpty.NONE)
            ourLayout.setOverflowGrowth(UniformGridLayout.OverflowGrowth.ADD_COLUMNS)
        and : 'Two panels with five children, the second of which is hidden, one per layout manager.'
            var ours =
                    UI.panel()
                    .withLayout(ourLayout)
                    .add(UI.box().withPrefSize(30, 20))
                    .add(UI.box().withPrefSize(30, 20).isVisibleIf(false))
                    .add(UI.box().withPrefSize(30, 20))
                    .add(UI.box().withPrefSize(30, 20))
                    .add(UI.box().withPrefSize(30, 20))
                    .get(JPanel)
            var awts =
                    UI.panel()
                    .withLayout(new GridLayout(2, 3, 5, 5))
                    .add(UI.box().withPrefSize(30, 20))
                    .add(UI.box().withPrefSize(30, 20).isVisibleIf(false))
                    .add(UI.box().withPrefSize(30, 20))
                    .add(UI.box().withPrefSize(30, 20))
                    .add(UI.box().withPrefSize(30, 20))
                    .get(JPanel)
        when : 'Both are laid out at the same size,'
            ours.setSize(160, 70)
            ours.doLayout()
            awts.setSize(160, 70)
            awts.doLayout()
            var ourBounds = (0..<5).collect { ours.getComponent(it).getBounds() }
            var awtBounds = (0..<5).collect { awts.getComponent(it).getBounds() }
        then : 'the second child is really hidden, but still has a cell of its own,'
            !ours.getComponent(1).isVisible()
            ourBounds[1].width > 0
            ourBounds[1] != ourBounds[2]
        and : 'and every child sits exactly where it sits in the JDK grid.'
            ourBounds == awtBounds
    }

    def 'Changing a `UniformGridLayout` through its setters has the same effect as changing a `GridLayout`.'(
        int rows, int cols, int horizontalGap, int verticalGap
    ) {
        reportInfo """
            Both layout managers can be reconfigured after they were installed, through
            `setRows`, `setColumns`, `setHgap` and `setVgap`. The next layout pass then
            uses the new values. Both layout managers start from their default here:
            a single row with a cell for every component and no gaps.
        """
        given : 'A uniform grid layout which spreads its components over its rows, collapses nothing and grows by columns, and a JDK grid layout, both otherwise in their default configuration.'
            var ourLayout = new UniformGridLayout()
            ourLayout.setMode(UniformGridLayout.Mode.SPREAD_OVER_ROWS)
            ourLayout.setCollapseEmpty(UniformGridLayout.CollapseEmpty.NONE)
            ourLayout.setOverflowGrowth(UniformGridLayout.OverflowGrowth.ADD_COLUMNS)
            var awtLayout = new GridLayout()
        and : 'Two panels with seven children, one per layout manager.'
            var ours =
                    UI.panel()
                    .withLayout(ourLayout)
                    .apply({ ui -> (0..<7).each { ui.add(UI.box().withPrefSize(30, 20)) } })
                    .get(JPanel)
            var awts =
                    UI.panel()
                    .withLayout(awtLayout)
                    .apply({ ui -> (0..<7).each { ui.add(UI.box().withPrefSize(30, 20)) } })
                    .get(JPanel)
        expect : 'Both defaults are a single row without gaps.'
            [ourLayout.rows, ourLayout.columns, ourLayout.hgap, ourLayout.vgap] == [1, 0, 0, 0]
            [awtLayout.rows, awtLayout.columns, awtLayout.hgap, awtLayout.vgap] == [1, 0, 0, 0]

        when : 'We reconfigure both layout managers in the same way and lay out both panels,'
            ourLayout.setColumns(cols)
            ourLayout.setRows(rows)
            ourLayout.setHgap(horizontalGap)
            ourLayout.setVgap(verticalGap)
            awtLayout.setColumns(cols)
            awtLayout.setRows(rows)
            awtLayout.setHgap(horizontalGap)
            awtLayout.setVgap(verticalGap)
            ours.setSize(180, 120)
            ours.doLayout()
            awts.setSize(180, 120)
            awts.doLayout()
            var ourBounds = (0..<7).collect { ours.getComponent(it).getBounds() }
            var awtBounds = (0..<7).collect { awts.getComponent(it).getBounds() }
        then : 'both report the values we have set,'
            [ourLayout.rows, ourLayout.columns, ourLayout.hgap, ourLayout.vgap] == [rows, cols, horizontalGap, verticalGap]
            [awtLayout.rows, awtLayout.columns, awtLayout.hgap, awtLayout.vgap] == [rows, cols, horizontalGap, verticalGap]
        and : 'and the children are laid out identically.'
            ourBounds == awtBounds

        where :
            rows | cols | horizontalGap | verticalGap
            1    | 0    | 0             | 0
            0    | 3    | 4             | 2
            3    | 0    | 2             | 4
            2    | 5    | 6             | 1
            3    | 3    | 0             | 9
    }

    def 'A `UniformGridLayout` rejects a grid without rows and columns exactly like a `GridLayout` does.'()
    {
        reportInfo """
            A row count of zero means "as many rows as the components need", and so does
            a column count of zero. At least one of the two has to be given, otherwise
            there is no way to tell how the components should be arranged. Both layout
            managers throw an `IllegalArgumentException` with the same message, whether
            the two zeros are passed to the constructor or set later through a setter.
        """
        when : 'We create a uniform grid layout without rows and without columns,'
            new UniformGridLayout(0, 0)
        then : 'it is rejected,'
            var ourConstructorFailure = thrown(IllegalArgumentException)
        when : 'and we create a JDK grid layout without rows and without columns,'
            new GridLayout(0, 0)
        then : 'it is rejected as well, with the same message.'
            var awtConstructorFailure = thrown(IllegalArgumentException)
            ourConstructorFailure.message == awtConstructorFailure.message

        when : 'We set the rows of a uniform grid without columns to zero,'
            new UniformGridLayout(2, 0).setRows(0)
        then : 'it is rejected,'
            var ourRowsFailure = thrown(IllegalArgumentException)
        when : 'and we do the same to a JDK grid layout,'
            new GridLayout(2, 0).setRows(0)
        then : 'it is rejected as well, with the same message.'
            var awtRowsFailure = thrown(IllegalArgumentException)
            ourRowsFailure.message == awtRowsFailure.message

        when : 'We set the columns of a uniform grid without rows to zero,'
            new UniformGridLayout(0, 2).setColumns(0)
        then : 'it is rejected,'
            var ourColumnsFailure = thrown(IllegalArgumentException)
        when : 'and we do the same to a JDK grid layout,'
            new GridLayout(0, 2).setColumns(0)
        then : 'it is rejected as well, with the same message.'
            var awtColumnsFailure = thrown(IllegalArgumentException)
            ourColumnsFailure.message == awtColumnsFailure.message
    }

    def 'Only `OverflowGrowth.ADD_COLUMNS` keeps an overflowing grid equivalent to a `GridLayout`.'(
        UniformGridLayout.OverflowGrowth overflowGrowth, int rows, int cols, int components,
        int rowsLaidOut, int columnsLaidOut, boolean matchesGridLayout
    ) {
        reportInfo """
            Once a grid holds more components than its declared row count times its declared
            column count, it has to make room, and a `GridLayout` only ever makes room in one
            direction: it keeps its rows and widens itself. `ADD_COLUMNS` is the growth policy
            which says exactly that, so it is the third setting the equivalence needs.

            The other two policies are asked to make room on the other axis, and they do, which
            means they part company with a `GridLayout` at the very component that overflows the
            declared grid. That is not a defect of the equivalence but the point of the policy:
            a grid of 3 rows and 2 columns holding 20 components stays 3 rows high and grows to
            7 columns under `ADD_COLUMNS`, becomes 10 rows of 2 columns under `ADD_ROWS`, and
            5 rows of 4 columns under `ADD_ROWS_AND_COLUMNS`.

            The table therefore runs all three policies over grids which hold far more components
            than the declared counts multiply to, and checks both directions: that `ADD_COLUMNS`
            matches a `GridLayout` down to the last pixel, and that the other two really do differ.
        """
        given : 'A uniform grid layout which spreads its components over its rows, collapses nothing, and grows in the way the table asks for.'
            var ourLayout = new UniformGridLayout(
                                    UniformGridLayout.Mode.SPREAD_OVER_ROWS,
                                    UniformGridLayout.CollapseEmpty.NONE,
                                    overflowGrowth,
                                    rows, cols, 5, 5
                                )
        and : 'Two panels with the same number of children, one per layout manager.'
            var ours =
                    UI.panel()
                    .withLayout(ourLayout)
                    .apply({ ui -> (0..<components).each { ui.add(UI.box().withPrefSize(30, 20)) } })
                    .get(JPanel)
            var awts =
                    UI.panel()
                    .withLayout(new GridLayout(rows, cols, 5, 5))
                    .apply({ ui -> (0..<components).each { ui.add(UI.box().withPrefSize(30, 20)) } })
                    .get(JPanel)
        expect : 'The uniform grid really does carry the growth setting from the table.'
            ourLayout.getOverflowGrowth() == overflowGrowth
        and : 'The panels really do hold more components than the declared numbers of rows and columns multiply to.'
            components > rows * cols

        when : 'Both are laid out at the same size,'
            ours.setSize(230, 70)
            ours.doLayout()
            awts.setSize(230, 70)
            awts.doLayout()
            var ourBounds = (0..<components).collect { ours.getComponent(it).getBounds() }
            var awtBounds = (0..<components).collect { awts.getComponent(it).getBounds() }
        then : 'the grown grid is the one the table expects.'
            [ourBounds.collect({ it.@y }).unique().size(), ourBounds.collect({ it.@x }).unique().size()] == [rowsLaidOut, columnsLaidOut]

        and : 'only the policy which makes room the way a `GridLayout` makes room places every child where the JDK grid places it.'
            (ourBounds == awtBounds) == matchesGridLayout
        and : 'and only that policy asks for the same preferred and minimum size.'
            (ours.getLayout().preferredLayoutSize(ours) == awts.getLayout().preferredLayoutSize(awts)) == matchesGridLayout
            (ours.getLayout().minimumLayoutSize(ours) == awts.getLayout().minimumLayoutSize(awts)) == matchesGridLayout

        where :
            overflowGrowth                                        | rows | cols | components || rowsLaidOut | columnsLaidOut | matchesGridLayout
            UniformGridLayout.OverflowGrowth.ADD_COLUMNS          | 2    | 5    | 13         || 2           | 7              | true
            UniformGridLayout.OverflowGrowth.ADD_ROWS             | 2    | 5    | 13         || 3           | 5              | false
            UniformGridLayout.OverflowGrowth.ADD_ROWS_AND_COLUMNS | 2    | 5    | 13         || 3           | 6              | false
            UniformGridLayout.OverflowGrowth.ADD_COLUMNS          | 3    | 2    | 20         || 3           | 7              | true
            UniformGridLayout.OverflowGrowth.ADD_ROWS             | 3    | 2    | 20         || 10          | 2              | false
            UniformGridLayout.OverflowGrowth.ADD_ROWS_AND_COLUMNS | 3    | 2    | 20         || 5           | 4              | false
    }
}
