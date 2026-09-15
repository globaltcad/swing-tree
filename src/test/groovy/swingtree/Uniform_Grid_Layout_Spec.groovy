package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.layout.UniformGridLayout
import swingtree.layout.UniformGridLayout.CollapseEmpty
import swingtree.layout.UniformGridLayout.Mode
import swingtree.threading.EventProcessor
import utility.SwingTreeTestConfigurator

import javax.swing.BorderFactory
import javax.swing.JPanel
import java.awt.ComponentOrientation
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.Rectangle

@Title("Uniform Grid Layout")
@Narrative('''

    A `UniformGridLayout` divides a container into cells of equal size, places
    one component into each cell and fills the cells row by row, starting with
    the top row. You tell it how many rows and how many columns the grid has,
    and two settings decide what it makes of those two numbers.

    The `UniformGridLayout.Mode` decides how the grid is built:

      - `WRAP_AFTER_COLUMNS`, the default, starts a new row after every column
        count you declare. When there are more components than cells, rows are
        added, so a grid of 2 rows and 5 columns lays out 11 components in 3 rows
        of 5 columns.
      - `SPREAD_OVER_ROWS` spreads the components over the rows you declare and
        ignores the column count, exactly like the `GridLayout` of the JDK does.
        It lays out 8 components in a grid of 2 rows and 5 columns in 4 columns,
        and 11 components in 6 columns.

    The `UniformGridLayout.CollapseEmpty` setting decides whether the rows and
    columns which no component occupies are left out of the grid, so that the
    components share their space. For 3 components in a grid of 2 rows and 5
    columns, `NONE` keeps both and lays them out in 2 rows of 5 columns, `COLUMNS`
    leaves out the 2 empty columns, `ROWS` leaves out the empty row, and
    `ROWS_AND_COLUMNS`, the default, leaves out both, so the 3 components fill a
    single row of 3 columns.

    A row or column count of zero means "as many as the components need". The
    "Grid Layout Invariance" specification shows that a `UniformGridLayout` with
    `SPREAD_OVER_ROWS` and `CollapseEmpty.NONE` cannot be told apart from a
    `GridLayout`.

    The gaps between the cells are scaled by the UI scale factor of SwingTree,
    so that they keep their proportions on high resolution screens.

''')
@Subject([UniformGridLayout])
class Uniform_Grid_Layout_Spec extends Specification
{
    def setupSpec() {
        SwingTree.initializeUsing(SwingTreeTestConfigurator.get())
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
        SwingTree.get().setUiScaleFactor(1f)
    }

    def cleanupSpec() {
        SwingTree.clear()
    }

    def 'By default, a `UniformGridLayout` starts a new row after the columns you declare, and leaves out every row and column that would stay empty.'(
        int rows, int cols, int components, int columnsLaidOut, int rowsLaidOut
    ) {
        reportInfo """
            A new `UniformGridLayout` is in the mode `WRAP_AFTER_COLUMNS`, and collapses
            `ROWS_AND_COLUMNS`. As soon as there are enough components to fill its first
            row, the grid has exactly the number of columns you declare: 8 components in
            a grid of 2 rows and 5 columns are laid out in 5 columns. A `GridLayout` of
            the JDK would use only 4 columns for them.

            With fewer components than cells, the rows and columns which would stay
            empty are left out, and the components share the whole container instead of
            leaving empty space next to or below them. So 3 components in a grid of 2
            rows and 5 columns fill a single row of 3 columns. When there are more
            components than cells, rows are added.

            Because empty rows are left out, the number of rows you declare only makes a
            difference when the number of columns is zero. Then the components are
            spread over the declared rows, and the rows they do not fill are left out:
            2 components in a grid of 3 rows and zero columns fill 2 rows of 1 column.

            Every container in this table is exactly large enough for cells of 40 by
            20 pixels with gaps of 5 pixels, in the number of columns and rows the grid
            is expected to have. If the grid had any other number of columns or rows,
            its cells would not be 40 by 20 pixels large.
        """
        given : 'A container exactly large enough for the expected grid of 40 by 20 pixel cells.'
            var width  = columnsLaidOut * 40 + (columnsLaidOut - 1) * 5
            var height = rowsLaidOut * 20 + (rowsLaidOut - 1) * 5
        and : 'A panel with a uniform grid layout which is created without any further settings.'
            var layout = new UniformGridLayout(rows, cols, 5, 5)
            var panel =
                    UI.panel()
                    .withLayout(layout)
                    .apply({ ui -> (0..<components).each { ui.add(UI.box()) } })
                    .get(JPanel)
        expect : 'The layout wraps after its columns and collapses empty rows and columns.'
            layout.getMode() == Mode.WRAP_AFTER_COLUMNS
            layout.getCollapseEmpty() == CollapseEmpty.ROWS_AND_COLUMNS

        when : 'The panel is laid out at that size,'
            panel.setSize(width, height)
            panel.doLayout()
            var bounds = (0..<components).collect { panel.getComponent(it).getBounds() }
        then : 'the component at index `i` sits in row `i / columnsLaidOut` and column `i % columnsLaidOut`, in a cell of 40 by 20 pixels.'
            bounds == (0..<components).collect { i -> new Rectangle((i % columnsLaidOut) * 45, i.intdiv(columnsLaidOut) * 25, 40, 20) }

        where :
            rows | cols | components || columnsLaidOut | rowsLaidOut
            2    | 5    | 8          || 5              | 2
            2    | 5    | 11         || 5              | 3
            3    | 3    | 4          || 3              | 2
            2    | 5    | 5          || 5              | 1
            2    | 5    | 3          || 3              | 1
            1    | 4    | 2          || 2              | 1
            4    | 2    | 1          || 1              | 1
            0    | 6    | 2          || 2              | 1
            3    | 0    | 2          || 1              | 2
            4    | 0    | 6          || 2              | 3
    }

    def 'Use `CollapseEmpty` to decide which empty rows and columns are left out of a grid of 2 rows and 5 columns.'(
        CollapseEmpty collapseEmpty, int components, int columnsLaidOut, int rowsLaidOut
    ) {
        reportInfo """
            This table puts the four `CollapseEmpty` settings side by side, for the same
            declared grid of 2 rows and 5 columns in the mode `WRAP_AFTER_COLUMNS`, and a
            growing number of components.

            The grid is filled row by row. So a column is empty when there are fewer
            components than columns, and a row is empty when the components do not reach
            it. `NONE` keeps both, `COLUMNS` leaves out the empty columns, `ROWS` leaves
            out the empty rows, and `ROWS_AND_COLUMNS` leaves out both. From 8 components
            on, no row and no column is empty, so all four settings lay out the same grid,
            and with 11 components, all four add a row.

            Every container in this table is exactly large enough for cells of 40 by
            20 pixels with gaps of 5 pixels, in the number of columns and rows the grid
            is expected to have.
        """
        given : 'A container exactly large enough for the expected grid of 40 by 20 pixel cells.'
            var width  = columnsLaidOut * 40 + (columnsLaidOut - 1) * 5
            var height = rowsLaidOut * 20 + (rowsLaidOut - 1) * 5
        and : 'A panel with a uniform grid layout of 2 rows and 5 columns, which collapses what the table says.'
            var layout = new UniformGridLayout(2, 5, 5, 5)
            layout.setCollapseEmpty(collapseEmpty)
            var panel =
                    UI.panel()
                    .withLayout(layout)
                    .apply({ ui -> (0..<components).each { ui.add(UI.box()) } })
                    .get(JPanel)

        when : 'The panel is laid out at that size,'
            panel.setSize(width, height)
            panel.doLayout()
            var bounds = (0..<components).collect { panel.getComponent(it).getBounds() }
        then : 'the component at index `i` sits in row `i / columnsLaidOut` and column `i % columnsLaidOut`, in a cell of 40 by 20 pixels.'
            bounds == (0..<components).collect { i -> new Rectangle((i % columnsLaidOut) * 45, i.intdiv(columnsLaidOut) * 25, 40, 20) }

        where :
            collapseEmpty                  | components || columnsLaidOut | rowsLaidOut
            CollapseEmpty.NONE             | 1          || 5              | 2
            CollapseEmpty.NONE             | 3          || 5              | 2
            CollapseEmpty.NONE             | 8          || 5              | 2
            CollapseEmpty.NONE             | 11         || 5              | 3
            CollapseEmpty.COLUMNS          | 1          || 1              | 2
            CollapseEmpty.COLUMNS          | 3          || 3              | 2
            CollapseEmpty.COLUMNS          | 8          || 5              | 2
            CollapseEmpty.COLUMNS          | 11         || 5              | 3
            CollapseEmpty.ROWS             | 1          || 5              | 1
            CollapseEmpty.ROWS             | 3          || 5              | 1
            CollapseEmpty.ROWS             | 8          || 5              | 2
            CollapseEmpty.ROWS             | 11         || 5              | 3
            CollapseEmpty.ROWS_AND_COLUMNS | 1          || 1              | 1
            CollapseEmpty.ROWS_AND_COLUMNS | 3          || 3              | 1
            CollapseEmpty.ROWS_AND_COLUMNS | 8          || 5              | 2
            CollapseEmpty.ROWS_AND_COLUMNS | 11         || 5              | 3
    }

    def 'In the mode `SPREAD_OVER_ROWS`, a `UniformGridLayout` spreads its components over the rows you declare, like a `GridLayout`.'(
        CollapseEmpty collapseEmpty, int rows, int cols, int components, int columnsLaidOut, int rowsLaidOut
    ) {
        reportInfo """
            In the mode `SPREAD_OVER_ROWS`, the column count is ignored whenever the row
            count is not zero, and the grid gets as many columns as it takes to fit all
            components into the declared rows. That is how the `GridLayout` of the JDK
            builds its grid: 3 components in a grid of 2 rows and 5 columns get 2 columns,
            8 components get 4 and 11 components get 6.

            Spreading the components like that never leaves a column empty, so collapsing
            empty columns changes nothing in this mode. It can leave rows empty, though:
            2 components in a grid of 3 rows get a single column, and only reach 2 of the
            3 rows. `CollapseEmpty.NONE` keeps the empty row, like a `GridLayout` does, and
            `ROWS` or `ROWS_AND_COLUMNS` leave it out.

            Every container in this table is exactly large enough for cells of 40 by
            20 pixels with gaps of 5 pixels, in the number of columns and rows the grid
            is expected to have.
        """
        given : 'A container exactly large enough for the expected grid of 40 by 20 pixel cells.'
            var width  = columnsLaidOut * 40 + (columnsLaidOut - 1) * 5
            var height = rowsLaidOut * 20 + (rowsLaidOut - 1) * 5
        and : 'A panel with a uniform grid layout which spreads its components over its rows, and collapses what the table says.'
            var layout = new UniformGridLayout(rows, cols, 5, 5)
            layout.setMode(Mode.SPREAD_OVER_ROWS)
            layout.setCollapseEmpty(collapseEmpty)
            var panel =
                    UI.panel()
                    .withLayout(layout)
                    .apply({ ui -> (0..<components).each { ui.add(UI.box()) } })
                    .get(JPanel)

        when : 'The panel is laid out at that size,'
            panel.setSize(width, height)
            panel.doLayout()
            var bounds = (0..<components).collect { panel.getComponent(it).getBounds() }
        then : 'the component at index `i` sits in row `i / columnsLaidOut` and column `i % columnsLaidOut`, in a cell of 40 by 20 pixels.'
            bounds == (0..<components).collect { i -> new Rectangle((i % columnsLaidOut) * 45, i.intdiv(columnsLaidOut) * 25, 40, 20) }

        where :
            collapseEmpty                  | rows | cols | components || columnsLaidOut | rowsLaidOut
            CollapseEmpty.NONE             | 2    | 5    | 3          || 2              | 2
            CollapseEmpty.NONE             | 2    | 5    | 8          || 4              | 2
            CollapseEmpty.NONE             | 2    | 5    | 11         || 6              | 2
            CollapseEmpty.NONE             | 3    | 5    | 2          || 1              | 3
            CollapseEmpty.NONE             | 4    | 0    | 6          || 2              | 4
            CollapseEmpty.COLUMNS          | 2    | 5    | 8          || 4              | 2
            CollapseEmpty.COLUMNS          | 3    | 5    | 2          || 1              | 3
            CollapseEmpty.ROWS             | 2    | 5    | 11         || 6              | 2
            CollapseEmpty.ROWS             | 3    | 5    | 2          || 1              | 2
            CollapseEmpty.ROWS             | 4    | 0    | 6          || 2              | 3
            CollapseEmpty.ROWS_AND_COLUMNS | 2    | 5    | 3          || 2              | 2
            CollapseEmpty.ROWS_AND_COLUMNS | 3    | 5    | 2          || 1              | 2
    }

    def 'When there are more components than cells, a `UniformGridLayout` which wraps after its columns adds rows and keeps the column count.'(
        CollapseEmpty collapseEmpty, int rows, int cols, int components, int rowsLaidOut
    ) {
        reportInfo """
            A grid which is too small for its components has to grow, and in the mode
            `WRAP_AFTER_COLUMNS` it grows by adding rows. The grid is filled row by row,
            so adding rows is the choice which keeps every component in its cell: the
            component at index `i` sits in row `i / cols` and column `i % cols`, no
            matter how many components follow it. Adding columns instead would move every
            component after the first row into a different cell, which is what the mode
            `SPREAD_OVER_ROWS` does, for the sake of behaving like a `GridLayout`.

            A grid which is too small has no empty rows or columns, so the table gives
            the same result for every `CollapseEmpty` setting.

            Every container in this table is exactly large enough for cells of 40 by
            20 pixels with gaps of 5 pixels, in the number of rows the grid grows to.
        """
        given : 'A container exactly large enough for the grown grid of 40 by 20 pixel cells.'
            var width  = cols * 40 + (cols - 1) * 5
            var height = rowsLaidOut * 20 + (rowsLaidOut - 1) * 5
        and : 'A panel with more children than its declared grid has cells, which collapses what the table says.'
            var layout = new UniformGridLayout(rows, cols, 5, 5)
            layout.setCollapseEmpty(collapseEmpty)
            var panel =
                    UI.panel()
                    .withLayout(layout)
                    .apply({ ui -> (0..<components).each { ui.add(UI.box()) } })
                    .get(JPanel)
        expect : 'The declared grid really is too small.'
            rows * cols < components

        when : 'The panel is laid out at that size,'
            panel.setSize(width, height)
            panel.doLayout()
            var bounds = (0..<components).collect { panel.getComponent(it).getBounds() }
        then : 'the component at index `i` sits in row `i / cols` and column `i % cols` of the grown grid.'
            bounds == (0..<components).collect { i -> new Rectangle((i % cols) * 45, i.intdiv(cols) * 25, 40, 20) }

        where :
            collapseEmpty                  | rows | cols | components | rowsLaidOut
            CollapseEmpty.NONE             | 2    | 5    | 11         | 3
            CollapseEmpty.NONE             | 1    | 3    | 7          | 3
            CollapseEmpty.NONE             | 2    | 2    | 5          | 3
            CollapseEmpty.NONE             | 3    | 4    | 13         | 4
            CollapseEmpty.COLUMNS          | 2    | 5    | 11         | 3
            CollapseEmpty.COLUMNS          | 1    | 3    | 7          | 3
            CollapseEmpty.COLUMNS          | 2    | 2    | 5          | 3
            CollapseEmpty.COLUMNS          | 3    | 4    | 13         | 4
            CollapseEmpty.ROWS             | 2    | 5    | 11         | 3
            CollapseEmpty.ROWS             | 1    | 3    | 7          | 3
            CollapseEmpty.ROWS             | 2    | 2    | 5          | 3
            CollapseEmpty.ROWS             | 3    | 4    | 13         | 4
            CollapseEmpty.ROWS_AND_COLUMNS | 2    | 5    | 11         | 3
            CollapseEmpty.ROWS_AND_COLUMNS | 1    | 3    | 7          | 3
            CollapseEmpty.ROWS_AND_COLUMNS | 2    | 2    | 5          | 3
            CollapseEmpty.ROWS_AND_COLUMNS | 3    | 4    | 13         | 4
    }

    def 'While you add components to a `UniformGridLayout`, its settings decide how the size of its cells changes.'(
        Mode mode, CollapseEmpty collapseEmpty, List<Integer> cellWidths, List<Integer> cellHeights
    ) {
        reportInfo """
            A grid of 2 rows and 5 columns in a container 220 by 45 pixels large has room
            for 5 cells of 40 pixels with 4 gaps of 5 pixels in width, and 2 cells of
            20 pixels with a gap of 5 pixels in height. This feature adds ten components to
            such a grid, one at a time, and writes down the size of the first cell after
            each one.

            When empty columns are collapsed, the first four components share the whole
            width: one component is 220 pixels wide, two are 107 pixels wide, three are 70
            and four are 51. When they are kept, the cells are 40 pixels wide from the
            start. When empty rows are collapsed, the first five components share the whole
            height of 45 pixels, and when they are kept, the cells are 20 pixels high from
            the start.

            In the mode `SPREAD_OVER_ROWS`, the grid gets another column for every second
            component, because all components have to fit into its 2 rows. That is how the
            cells of a `GridLayout` of the JDK change as well. Only the very first component
            leaves a row empty, which is why it takes the whole height when empty rows
            are collapsed.
        """
        given : 'An empty panel 220 by 45 pixels large, with a uniform grid of 2 rows and 5 columns with the settings from the table.'
            var layout = new UniformGridLayout(2, 5, 5, 5)
            layout.setMode(mode)
            layout.setCollapseEmpty(collapseEmpty)
            var panel = UI.panel().withLayout(layout).get(JPanel)
            panel.setSize(220, 45)
        when : 'We add ten components to it, one at a time, and write down the size of the first cell after each one,'
            var widths = []
            var heights = []
            10.times {
                UI.of(panel).add(UI.box())
                panel.doLayout()
                widths << panel.getComponent(0).getWidth()
                heights << panel.getComponent(0).getHeight()
            }
        then : 'the widths and heights are the ones from the table.'
            widths == cellWidths
            heights == cellHeights

        where :
            mode                    | collapseEmpty                  | cellWidths                                    | cellHeights
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.NONE             | [40, 40, 40, 40, 40, 40, 40, 40, 40, 40]      | [20, 20, 20, 20, 20, 20, 20, 20, 20, 20]
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.COLUMNS          | [220, 107, 70, 51, 40, 40, 40, 40, 40, 40]    | [20, 20, 20, 20, 20, 20, 20, 20, 20, 20]
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.ROWS             | [40, 40, 40, 40, 40, 40, 40, 40, 40, 40]      | [45, 45, 45, 45, 45, 20, 20, 20, 20, 20]
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.ROWS_AND_COLUMNS | [220, 107, 70, 51, 40, 40, 40, 40, 40, 40]    | [45, 45, 45, 45, 45, 20, 20, 20, 20, 20]
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.NONE             | [220, 220, 107, 107, 70, 70, 51, 51, 40, 40]  | [20, 20, 20, 20, 20, 20, 20, 20, 20, 20]
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.COLUMNS          | [220, 220, 107, 107, 70, 70, 51, 51, 40, 40]  | [20, 20, 20, 20, 20, 20, 20, 20, 20, 20]
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.ROWS             | [220, 220, 107, 107, 70, 70, 51, 51, 40, 40]  | [45, 20, 20, 20, 20, 20, 20, 20, 20, 20]
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.ROWS_AND_COLUMNS | [220, 220, 107, 107, 70, 70, 51, 51, 40, 40]  | [45, 20, 20, 20, 20, 20, 20, 20, 20, 20]
    }

    def 'A row or column count of zero means "as many as the components need".'(
        Mode mode, CollapseEmpty collapseEmpty, int rows, int cols, int components
    ) {
        reportInfo """
            A zero is how you leave one of the two counts open: a grid with zero rows
            and 3 columns gets as many rows as its components need, and a grid with 2
            rows and zero columns gets as many columns as they need. With one count left
            open, there is nothing to ignore, so the components in this table are laid
            out exactly like in a `GridLayout` with the same counts, with the default
            settings as well as with the settings of a `GridLayout`.

            The table leaves out the grids in which collapsing makes a difference. When
            empty columns are collapsed, 2 components in a grid of zero rows and 3 columns
            get 2 columns, where a `GridLayout` gives them 3. When empty rows are collapsed,
            2 components in a grid of 3 rows and zero columns get 2 rows, where a
            `GridLayout` gives them 3.
        """
        given : 'A uniform grid layout with the settings from the table.'
            var layout = new UniformGridLayout(rows, cols, 5, 5)
            layout.setMode(mode)
            layout.setCollapseEmpty(collapseEmpty)
        and : 'Two panels with the same children, one per layout manager.'
            var ours =
                    UI.panel()
                    .withLayout(layout)
                    .apply({ ui -> (0..<components).each { ui.add(UI.box()) } })
                    .get(JPanel)
            var awts =
                    UI.panel()
                    .withLayout(new GridLayout(rows, cols, 5, 5))
                    .apply({ ui -> (0..<components).each { ui.add(UI.box()) } })
                    .get(JPanel)
        when : 'Both are laid out at the same size,'
            ours.setSize(200, 120)
            ours.doLayout()
            awts.setSize(200, 120)
            awts.doLayout()
            var ourBounds = (0..<components).collect { ours.getComponent(it).getBounds() }
            var awtBounds = (0..<components).collect { awts.getComponent(it).getBounds() }
        then : 'every component was given a cell,'
            ourBounds.every { it.width > 0 && it.height > 0 }
        and : 'and it is the same cell in both grids.'
            ourBounds == awtBounds

        where :
            mode                    | collapseEmpty                  | rows | cols | components
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.ROWS_AND_COLUMNS | 0    | 3    | 7
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.ROWS_AND_COLUMNS | 2    | 0    | 7
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.ROWS_AND_COLUMNS | 1    | 0    | 5
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.ROWS_AND_COLUMNS | 0    | 1    | 4
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.NONE             | 0    | 3    | 7
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.NONE             | 2    | 0    | 7
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.NONE             | 1    | 0    | 5
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.NONE             | 0    | 1    | 4
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.NONE             | 0    | 3    | 7
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.NONE             | 2    | 0    | 7
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.NONE             | 1    | 0    | 5
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.NONE             | 0    | 1    | 4
    }

    def 'The preferred and minimum size of a `UniformGridLayout` fit the grid it lays out.'(
        Mode mode, CollapseEmpty collapseEmpty, int rows, int cols, int components, Dimension preferredSize, Dimension minimumSize
    ) {
        reportInfo """
            A `UniformGridLayout` works out the number of rows and columns in the same
            way when it lays out its components and when it is asked for its preferred
            or minimum size. Every child in this table prefers 30 by 20 pixels and
            needs at least 10 by 5 pixels, and the gaps are 5 pixels.

            So with the default settings, a grid of 2 rows and 5 columns with 8
            components prefers 5 times 30 plus 4 gaps in width, and 2 times 20 plus 1
            gap in height. With 3 components, it only has 1 row of 3 columns, and prefers
            3 times 30 plus 2 gaps in width and 20 pixels in height. When empty rows and
            columns are kept, the same 3 components prefer the size of all 2 rows and
            5 columns, and in the mode `SPREAD_OVER_ROWS`, they prefer the size of 2 rows
            and 2 columns.

            That is also why a grid laid out at its own preferred size gives every
            child exactly its preferred size.
        """
        given : 'A uniform grid layout with the settings from the table.'
            var layout = new UniformGridLayout(rows, cols, 5, 5)
            layout.setMode(mode)
            layout.setCollapseEmpty(collapseEmpty)
        and : 'A panel whose children prefer 30 by 20 pixels and need at least 10 by 5 pixels.'
            var panel =
                    UI.panel()
                    .withLayout(layout)
                    .apply({ ui ->
                        (0..<components).each {
                            ui.add(UI.box().withPrefSize(30, 20).withMinSize(10, 5))
                        }
                    })
                    .get(JPanel)
        expect : 'The layout manager reports the sizes of the grid it lays out.'
            layout.preferredLayoutSize(panel) == preferredSize
            layout.minimumLayoutSize(panel) == minimumSize

        when : 'The panel is laid out at exactly its preferred size,'
            panel.setSize(preferredSize)
            panel.doLayout()
            var sizes = (0..<components).collect { panel.getComponent(it).getSize() }
        then : 'every child is given exactly the size it prefers.'
            sizes.every { it == new Dimension(30, 20) }

        where :
            mode                    | collapseEmpty                  | rows | cols | components | preferredSize          | minimumSize
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.ROWS_AND_COLUMNS | 2    | 5    | 8          | new Dimension(170, 45) | new Dimension(70, 15)
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.ROWS_AND_COLUMNS | 2    | 5    | 3          | new Dimension(100, 20) | new Dimension(40, 5)
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.ROWS_AND_COLUMNS | 2    | 5    | 11         | new Dimension(170, 70) | new Dimension(70, 25)
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.ROWS_AND_COLUMNS | 3    | 3    | 4          | new Dimension(100, 45) | new Dimension(40, 15)
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.ROWS_AND_COLUMNS | 0    | 4    | 6          | new Dimension(135, 45) | new Dimension(55, 15)
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.NONE             | 2    | 5    | 3          | new Dimension(170, 45) | new Dimension(70, 15)
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.COLUMNS          | 2    | 5    | 3          | new Dimension(100, 45) | new Dimension(40, 15)
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.ROWS             | 2    | 5    | 3          | new Dimension(170, 20) | new Dimension(70, 5)
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.NONE             | 2    | 5    | 3          | new Dimension(65, 45)  | new Dimension(25, 15)
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.NONE             | 2    | 5    | 11         | new Dimension(205, 45) | new Dimension(85, 15)
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.NONE             | 3    | 0    | 2          | new Dimension(30, 70)  | new Dimension(10, 25)
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.ROWS             | 3    | 0    | 2          | new Dimension(30, 45)  | new Dimension(10, 15)
    }

    def 'Use `setMode(Mode)` to change how an installed `UniformGridLayout` builds its grid.'()
    {
        reportInfo """
            The mode is read every time the container is laid out, so a new mode takes
            effect on the next layout pass, without installing a new layout manager.

            This feature switches a grid of 2 rows and 5 columns with 3 components between
            both modes, in a container 220 by 45 pixels large, while empty rows and
            columns are collapsed. When it wraps after its columns, the 3 components fill a
            single row of 3 columns, each 70 pixels wide. When it spreads them over its
            2 rows, they need 2 columns, and there is no empty row or column to leave out,
            so they get exactly the cells a `GridLayout` gives them: 107 pixels wide, with
            the 1 pixel left over by 2 cells and a gap on the right.
        """
        given : 'A uniform grid of 2 rows and 5 columns holding 3 components, and a JDK grid holding the same.'
            var layout = new UniformGridLayout(2, 5, 5, 5)
            var ours =
                    UI.panel()
                    .withLayout(layout)
                    .apply({ ui -> (0..<3).each { ui.add(UI.box()) } })
                    .get(JPanel)
            var awts =
                    UI.panel()
                    .withLayout(new GridLayout(2, 5, 5, 5))
                    .apply({ ui -> (0..<3).each { ui.add(UI.box()) } })
                    .get(JPanel)
            ours.setSize(220, 45)
            awts.setSize(220, 45)
        when : 'We lay out the uniform grid in its default mode,'
            ours.doLayout()
        then : 'the mode is `WRAP_AFTER_COLUMNS`, and the 3 components share the container in a single row of 3 columns.'
            layout.getMode() == Mode.WRAP_AFTER_COLUMNS
            (0..<3).collect { ours.getComponent(it).getBounds() } == [new Rectangle(0, 0, 70, 45), new Rectangle(75, 0, 70, 45), new Rectangle(150, 0, 70, 45)]

        when : 'We switch the uniform grid to the mode `SPREAD_OVER_ROWS` and lay out both panels,'
            layout.setMode(Mode.SPREAD_OVER_ROWS)
            ours.doLayout()
            awts.doLayout()
        then : 'the mode is `SPREAD_OVER_ROWS`, and the 3 components are laid out in 2 rows of 2 columns, 107 pixels wide,'
            layout.getMode() == Mode.SPREAD_OVER_ROWS
            (0..<3).collect { ours.getComponent(it).getBounds() } == [new Rectangle(0, 0, 107, 20), new Rectangle(112, 0, 107, 20), new Rectangle(0, 25, 107, 20)]
        and : 'exactly like in the JDK grid.'
            (0..<3).collect { ours.getComponent(it).getBounds() } == (0..<3).collect { awts.getComponent(it).getBounds() }

        when : 'We switch back to the mode `WRAP_AFTER_COLUMNS` and lay out the uniform grid again,'
            layout.setMode(Mode.WRAP_AFTER_COLUMNS)
            ours.doLayout()
        then : 'the single row of 3 columns is back.'
            (0..<3).collect { ours.getComponent(it).getBounds() } == [new Rectangle(0, 0, 70, 45), new Rectangle(75, 0, 70, 45), new Rectangle(150, 0, 70, 45)]

        when : 'We try to set no mode at all,'
            layout.setMode(null)
        then : 'the layout rejects it,'
            thrown(NullPointerException)
        and : 'and keeps the mode it had.'
            layout.getMode() == Mode.WRAP_AFTER_COLUMNS
    }

    def 'Use `setCollapseEmpty(CollapseEmpty)` to change which empty rows and columns an installed `UniformGridLayout` leaves out.'()
    {
        reportInfo """
            The setting is read every time the container is laid out, so a new setting
            takes effect on the next layout pass, without installing a new layout manager.

            This feature switches a grid of 2 rows and 5 columns with 3 components through
            all four settings, in a container 220 by 45 pixels large. The container is
            exactly wide enough for 5 cells of 40 pixels with 4 gaps of 5 pixels, or for
            3 cells of 70 pixels with 2 gaps, and exactly high enough for 2 cells of
            20 pixels with a gap, or for a single cell of 45 pixels.
        """
        given : 'A panel 220 by 45 pixels large, with a uniform grid of 2 rows and 5 columns holding 3 components.'
            var layout = new UniformGridLayout(2, 5, 5, 5)
            var panel =
                    UI.panel()
                    .withLayout(layout)
                    .apply({ ui -> (0..<3).each { ui.add(UI.box()) } })
                    .get(JPanel)
            panel.setSize(220, 45)
        when : 'We lay out the panel with the default setting,'
            panel.doLayout()
        then : 'empty rows and columns are collapsed, so the 3 components fill a single row of 3 columns.'
            layout.getCollapseEmpty() == CollapseEmpty.ROWS_AND_COLUMNS
            (0..<3).collect { panel.getComponent(it).getBounds() } == [new Rectangle(0, 0, 70, 45), new Rectangle(75, 0, 70, 45), new Rectangle(150, 0, 70, 45)]

        when : 'We only collapse empty columns, and lay out the panel again,'
            layout.setCollapseEmpty(CollapseEmpty.COLUMNS)
            panel.doLayout()
        then : 'the 3 columns are back in the top one of 2 rows.'
            layout.getCollapseEmpty() == CollapseEmpty.COLUMNS
            (0..<3).collect { panel.getComponent(it).getBounds() } == [new Rectangle(0, 0, 70, 20), new Rectangle(75, 0, 70, 20), new Rectangle(150, 0, 70, 20)]

        when : 'We only collapse empty rows, and lay out the panel again,'
            layout.setCollapseEmpty(CollapseEmpty.ROWS)
            panel.doLayout()
        then : 'the components take the first 3 of 5 columns, in a single row.'
            layout.getCollapseEmpty() == CollapseEmpty.ROWS
            (0..<3).collect { panel.getComponent(it).getBounds() } == [new Rectangle(0, 0, 40, 45), new Rectangle(45, 0, 40, 45), new Rectangle(90, 0, 40, 45)]

        when : 'We collapse nothing, and lay out the panel again,'
            layout.setCollapseEmpty(CollapseEmpty.NONE)
            panel.doLayout()
        then : 'the components take the first 3 cells of a grid of 2 rows and 5 columns.'
            layout.getCollapseEmpty() == CollapseEmpty.NONE
            (0..<3).collect { panel.getComponent(it).getBounds() } == [new Rectangle(0, 0, 40, 20), new Rectangle(45, 0, 40, 20), new Rectangle(90, 0, 40, 20)]

        when : 'We try to set no setting at all,'
            layout.setCollapseEmpty(null)
        then : 'the layout rejects it,'
            thrown(NullPointerException)
        and : 'and keeps the setting it had.'
            layout.getCollapseEmpty() == CollapseEmpty.NONE
    }

    def 'The gaps of a `UniformGridLayout` are scaled by the UI scale factor, with every setting.'(
        Mode mode, CollapseEmpty collapseEmpty, float scaleFactor, int horizontalGap, int verticalGap
    ) {
        reportInfo """
            You declare the gaps of a grid for a UI scale factor of 1, and the layout
            multiplies them by the current scale factor every time it lays out its
            container or computes its size, rounded to the nearest whole pixel. So the
            horizontal gap of 5 pixels in this table becomes 6 pixels at a scale factor
            of 1.25, 8 pixels at 1.5, 9 pixels at 1.75 and 10 pixels at 2, and the
            vertical gap of 3 pixels becomes 4, 5, 5 and 6 pixels.

            The layout reads the scale factor when it lays out or measures its container,
            not when it is created. This table creates every layout at a scale factor
            of 1, and changes the scale factor afterwards. The getters keep reporting the
            gaps you declared, so that they can be compared with the values you set.

            The scaling does not depend on the settings, because they decide how many
            rows and columns the grid has, not how far apart they are. The 6 components in
            this table fill a grid of 2 rows and 3 columns with every setting.
        """
        given : 'A uniform grid layout of 2 rows and 3 columns with the settings from the table, a horizontal gap of 5 and a vertical gap of 3, created at a scale factor of 1.'
            var layout = new UniformGridLayout(2, 3, 5, 3)
            layout.setMode(mode)
            layout.setCollapseEmpty(collapseEmpty)
        and : 'A panel with 6 children that prefer 30 by 20 pixels and need at least 10 by 5 pixels.'
            var panel =
                    UI.panel()
                    .withLayout(layout)
                    .apply({ ui -> (0..<6).each { ui.add(UI.box().withPrefSize(30, 20).withMinSize(10, 5)) } })
                    .get(JPanel)

        when : 'The UI scale factor changes to the one from the table,'
            SwingTree.get().setUiScaleFactor(scaleFactor)
        and : 'the panel is laid out exactly large enough for cells of 40 by 20 pixels with the scaled gaps between them,'
            panel.setSize(3 * 40 + 2 * horizontalGap, 2 * 20 + verticalGap)
            panel.doLayout()
            var bounds = (0..<6).collect { panel.getComponent(it).getBounds() }
        then : 'the gaps we expect are the declared gaps scaled by the UI scale factor,'
            horizontalGap == UI.scale(5)
            verticalGap   == UI.scale(3)
        and : 'every child sits in its 40 by 20 pixel cell, with the scaled gaps between the cells.'
            bounds == (0..<6).collect { i -> new Rectangle((i % 3) * (40 + horizontalGap), i.intdiv(3) * (20 + verticalGap), 40, 20) }

        when : 'We ask the layout for the preferred and the minimum size of the panel, and the children for theirs,'
            var preferredSize = layout.preferredLayoutSize(panel)
            var minimumSize   = layout.minimumLayoutSize(panel)
            int childPreferredWidth  = panel.getComponent(0).getPreferredSize().width
            int childPreferredHeight = panel.getComponent(0).getPreferredSize().height
            int childMinimumWidth    = panel.getComponent(0).getMinimumSize().width
            int childMinimumHeight   = panel.getComponent(0).getMinimumSize().height
        then : 'both sizes are made of 3 columns and 2 rows of the size of a child, with the scaled gaps between them,'
            childPreferredWidth > 0 && childMinimumWidth > 0
            preferredSize == new Dimension(3 * childPreferredWidth + 2 * horizontalGap, 2 * childPreferredHeight + verticalGap)
            minimumSize   == new Dimension(3 * childMinimumWidth   + 2 * horizontalGap, 2 * childMinimumHeight   + verticalGap)
        and : 'while the layout still reports the gaps as they were declared.'
            layout.getHgap() == 5
            layout.getVgap() == 3

        cleanup :
            SwingTree.get().setUiScaleFactor(1f)

        where :
            mode                    | collapseEmpty                  | scaleFactor || horizontalGap | verticalGap
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.ROWS_AND_COLUMNS | 1f          || 5             | 3
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.ROWS_AND_COLUMNS | 1.25f       || 6             | 4
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.ROWS_AND_COLUMNS | 1.5f        || 8             | 5
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.ROWS_AND_COLUMNS | 1.75f       || 9             | 5
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.ROWS_AND_COLUMNS | 2f          || 10            | 6
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.ROWS             | 1f          || 5             | 3
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.ROWS             | 1.25f       || 6             | 4
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.ROWS             | 1.5f        || 8             | 5
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.ROWS             | 1.75f       || 9             | 5
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.ROWS             | 2f          || 10            | 6
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.COLUMNS          | 1f          || 5             | 3
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.COLUMNS          | 1.25f       || 6             | 4
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.COLUMNS          | 1.5f        || 8             | 5
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.COLUMNS          | 1.75f       || 9             | 5
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.COLUMNS          | 2f          || 10            | 6
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.NONE             | 1f          || 5             | 3
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.NONE             | 1.25f       || 6             | 4
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.NONE             | 1.5f        || 8             | 5
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.NONE             | 1.75f       || 9             | 5
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.NONE             | 2f          || 10            | 6
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.ROWS_AND_COLUMNS | 1f          || 5             | 3
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.ROWS_AND_COLUMNS | 1.25f       || 6             | 4
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.ROWS_AND_COLUMNS | 1.5f        || 8             | 5
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.ROWS_AND_COLUMNS | 1.75f       || 9             | 5
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.ROWS_AND_COLUMNS | 2f          || 10            | 6
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.ROWS             | 1f          || 5             | 3
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.ROWS             | 1.25f       || 6             | 4
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.ROWS             | 1.5f        || 8             | 5
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.ROWS             | 1.75f       || 9             | 5
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.ROWS             | 2f          || 10            | 6
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.COLUMNS          | 1f          || 5             | 3
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.COLUMNS          | 1.25f       || 6             | 4
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.COLUMNS          | 1.5f        || 8             | 5
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.COLUMNS          | 1.75f       || 9             | 5
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.COLUMNS          | 2f          || 10            | 6
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.NONE             | 1f          || 5             | 3
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.NONE             | 1.25f       || 6             | 4
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.NONE             | 1.5f        || 8             | 5
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.NONE             | 1.75f       || 9             | 5
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.NONE             | 2f          || 10            | 6
    }

    def 'A right-to-left `UniformGridLayout` fills every row from its right end.'(
        Mode mode, CollapseEmpty collapseEmpty, List<Rectangle> bounds
    ) {
        reportInfo """
            In a container with a right-to-left component orientation, the grid is
            mirrored: the first component of a row sits in its rightmost cell, and the
            rows are still filled from the top.

            The table lays out 3 components in a grid of 2 rows and 5 columns, in a
            container 220 by 45 pixels large. When empty columns are kept, the rightmost
            cell belongs to the last of the 5 declared columns, so the components fill the
            three cells on the right side of the top row, and the two empty columns stay on
            the left. When empty columns are collapsed, the grid only has 3 columns, which
            span the whole width. When empty rows are collapsed, the single row spans the
            whole height. In the mode `SPREAD_OVER_ROWS`, the grid has 2 columns, and the
            third component starts the second row on the right, with the 1 pixel left over
            by the 2 cells of 107 pixels on the left.
        """
        given : 'A right-to-left panel with a uniform grid of 2 rows and 5 columns with the settings from the table, holding 3 components.'
            var layout = new UniformGridLayout(2, 5, 5, 5)
            layout.setMode(mode)
            layout.setCollapseEmpty(collapseEmpty)
            var panel =
                    UI.panel()
                    .withLayout(layout)
                    .peek({ it.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT) })
                    .apply({ ui -> (0..<3).each { ui.add(UI.box()) } })
                    .get(JPanel)
        when : 'The panel is laid out at 220 by 45 pixels,'
            panel.setSize(220, 45)
            panel.doLayout()
        then : 'the components fill the rows from their right end.'
            (0..<3).collect { panel.getComponent(it).getBounds() } == bounds

        where :
            mode                    | collapseEmpty                  | bounds
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.ROWS_AND_COLUMNS | [new Rectangle(150, 0, 70, 45),  new Rectangle( 75, 0, 70, 45),  new Rectangle(  0,  0, 70, 45)]
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.COLUMNS          | [new Rectangle(150, 0, 70, 20),  new Rectangle( 75, 0, 70, 20),  new Rectangle(  0,  0, 70, 20)]
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.ROWS             | [new Rectangle(180, 0, 40, 45),  new Rectangle(135, 0, 40, 45),  new Rectangle( 90,  0, 40, 45)]
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.NONE             | [new Rectangle(180, 0, 40, 20),  new Rectangle(135, 0, 40, 20),  new Rectangle( 90,  0, 40, 20)]
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.NONE             | [new Rectangle(113, 0, 107, 20), new Rectangle(  1, 0, 107, 20), new Rectangle(113, 25, 107, 20)]
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.ROWS_AND_COLUMNS | [new Rectangle(113, 0, 107, 20), new Rectangle(  1, 0, 107, 20), new Rectangle(113, 25, 107, 20)]
    }

    def 'An empty `UniformGridLayout` with the default settings asks for no space but the insets of its container.'(
        int rows, int cols
    ) {
        reportInfo """
            A container without children has nothing to lay out, but Swing still asks its
            layout manager for its preferred and minimum size, for example when the window
            it is part of is packed.

            When empty rows and columns are collapsed, which is the default, an empty grid
            collapses to a single row and a single column. That cell is 0 pixels large,
            because there is no component to size it after, so the container only asks for
            the space of its insets, whatever numbers of rows and columns and gaps you
            declared. The panels in this table have a border which is 1 pixel thick at the
            top, 2 pixels on the left, 3 pixels at the bottom and 4 pixels on the right.
        """
        given : 'An empty panel with a border, and a uniform grid layout with gaps of 5 and 3 pixels and otherwise default settings.'
            var layout = new UniformGridLayout(rows, cols, 5, 3)
            var panel =
                    UI.panel()
                    .withLayout(layout)
                    .peek({ it.setBorder(BorderFactory.createEmptyBorder(1, 2, 3, 4)) })
                    .get(JPanel)
        expect : 'The panel really is empty.'
            panel.getComponentCount() == 0

        when : 'We ask the layout for the preferred and the minimum size of the panel,'
            var preferredSize = layout.preferredLayoutSize(panel)
            var minimumSize   = layout.minimumLayoutSize(panel)
        then : 'both are exactly the size of the insets: 2 plus 4 pixels wide and 1 plus 3 pixels high.'
            preferredSize == new Dimension(6, 4)
            minimumSize   == new Dimension(6, 4)

        where :
            rows | cols
            2    | 5
            0    | 5
            2    | 0
            1    | 0
    }

    def 'An empty `UniformGridLayout` can be laid out and measured with every setting.'(
        Mode mode, CollapseEmpty collapseEmpty, int rows, int cols
    ) {
        reportInfo """
            An empty grid has no components to count rows and columns for, so every
            number of rows or columns which would be worked out from the components is 0.
            The layout manager has to lay out and measure such a grid all the same, with
            every combination of settings, without dividing by that 0.
        """
        given : 'A panel without children, with a uniform grid layout with the settings from the table.'
            var layout = new UniformGridLayout(rows, cols, 5, 3)
            layout.setMode(mode)
            layout.setCollapseEmpty(collapseEmpty)
            var panel = UI.panel().withLayout(layout).get(JPanel)
        expect : 'The panel really is empty.'
            panel.getComponentCount() == 0

        when : 'The panel is laid out and measured,'
            panel.setSize(100, 50)
            panel.doLayout()
            layout.preferredLayoutSize(panel)
            layout.minimumLayoutSize(panel)
        then : 'the layout manager does all of that without failing.'
            noExceptionThrown()

        where :
            mode                    | collapseEmpty                  | rows | cols
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.ROWS_AND_COLUMNS | 0    | 5
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.ROWS_AND_COLUMNS | 2    | 0
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.ROWS             | 0    | 5
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.ROWS             | 2    | 0
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.COLUMNS          | 0    | 5
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.COLUMNS          | 2    | 0
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.NONE             | 0    | 5
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.NONE             | 2    | 0
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.ROWS_AND_COLUMNS | 0    | 5
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.ROWS_AND_COLUMNS | 2    | 0
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.ROWS             | 0    | 5
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.ROWS             | 2    | 0
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.COLUMNS          | 0    | 5
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.COLUMNS          | 2    | 0
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.NONE             | 0    | 5
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.NONE             | 2    | 0
    }
}
