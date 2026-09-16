package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.Var
import swingtree.api.Layout
import swingtree.layout.UniformGridLayout
import swingtree.layout.UniformGridLayout.CollapseEmpty
import swingtree.layout.UniformGridLayout.Mode
import swingtree.layout.UniformGridLayout.OverflowGrowth
import swingtree.threading.EventProcessor
import utility.SwingTreeTestConfigurator

import javax.swing.JPanel
import java.awt.GridLayout
import java.awt.Rectangle

@Title("Grid Layouts")
@Narrative('''

    A grid layout divides a component into cells of equal size and puts its
    children into those cells, row by row, starting with the top row. In a
    SwingTree UI declaration, you get one in three ways:

      - `withGridLayout(..)` on the builder of the component,
      - `Layout.grid(..)` in a style, through `it.layout(Layout.grid(..))`,
      - `Layout.grid(..)` in a `Var<Layout>`, which you bind with `withLayout(..)`,
        so that the grid can change while the application runs.

    All three install a `UniformGridLayout`. You declare a number of rows and
    columns, and by default, a new row starts after every declared number of
    columns, and the rows and columns which no child occupies are left out, so
    that the children share the whole component. A grid of 2 rows and 3 columns
    therefore lays out 6 children in 2 rows of 3 columns, and 2 children side by
    side in a single row.

    Both `withGridLayout(..)` and `Layout.grid(..)` also accept a
    `UniformGridLayout.Mode` and a `UniformGridLayout.CollapseEmpty` setting in
    front of the numbers of rows and columns, which choose how the grid is built
    and which empty rows and columns are left out. The "Uniform Grid Layout"
    specification describes every setting in detail.

    A third setting, `UniformGridLayout.OverflowGrowth`, chooses how the grid
    grows when a component holds more children than the declared number of rows
    times the declared number of columns. It can add rows, add columns, or add
    both so that the grid keeps the ratio of rows to columns it was declared with.

''')
@Subject([UIForAnySwing, Layout, UniformGridLayout])
class Grid_Layout_Spec extends Specification
{
    def setupSpec() {
        SwingTree.initializeUsing(SwingTreeTestConfigurator.get())
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
        SwingTree.get().setUiScaleFactor(1f)
    }

    def cleanupSpec() {
        SwingTree.clear()
    }

    def 'Use `withGridLayout(rows, cols)` to arrange the children of a component in cells of equal size.'()
    {
        reportInfo """
            `withGridLayout(rows, cols)` attaches a `UniformGridLayout` without gaps to
            the component. The children fill its cells row by row, and every child is
            resized to exactly the size of its cell.

            The panel in this feature has a grid of 2 rows and 3 columns, and 4 buttons.
            At 300 by 100 pixels, every cell is 100 pixels wide and 50 pixels high: the
            buttons 1, 2 and 3 fill the top row, and the button 4 starts the bottom row.
        """
        given : 'A panel with a grid of 2 rows and 3 columns, holding 4 buttons.'
            var panel =
                    UI.panel().withGridLayout(2, 3)
                    .add(UI.button("1")).add(UI.button("2")).add(UI.button("3"))
                    .add(UI.button("4"))
                    .get(JPanel)
        expect : 'The panel has a uniform grid layout with the declared numbers of rows and columns, no gaps and the default settings.'
            panel.getLayout() instanceof UniformGridLayout
            panel.getLayout().getRows() == 2
            panel.getLayout().getColumns() == 3
            panel.getLayout().getHgap() == 0
            panel.getLayout().getVgap() == 0
            panel.getLayout().getMode() == Mode.WRAP_AFTER_COLUMNS
            panel.getLayout().getCollapseEmpty() == CollapseEmpty.ROWS_AND_COLUMNS

        when : 'The panel is laid out at 300 by 100 pixels,'
            panel.setSize(300, 100)
            panel.doLayout()
        then : 'every button has its cell of 100 by 50 pixels.'
            (0..<4).collect { panel.getComponent(it).getBounds() } == [
                new Rectangle(  0,  0, 100, 50),
                new Rectangle(100,  0, 100, 50),
                new Rectangle(200,  0, 100, 50),
                new Rectangle(  0, 50, 100, 50)
            ]
    }

    def 'A grid from `withGridLayout(rows, cols)` leaves out the rows and columns its children do not fill.'(
        int children, int cellWidth, int cellHeight
    ) {
        reportInfo """
            By default, a grid only has the rows and columns its children occupy, so the
            children never leave empty space next to them or below them. The table adds a
            growing number of children to a panel of 300 by 100 pixels with a grid of 2
            rows and 3 columns, and shows the size every child gets.

            A single child takes the whole panel, 2 children get half of its width each,
            and 3 children a third. From 4 children on, the grid has 2 rows, and a seventh
            child adds a third row, because the grid never has more than the 3 declared
            columns.
        """
        given : 'A panel with a grid of 2 rows and 3 columns, holding the number of children from the table.'
            var panel =
                    UI.panel().withGridLayout(2, 3)
                    .apply({ ui -> (0..<children).each { ui.add(UI.box()) } })
                    .get(JPanel)
        when : 'The panel is laid out at 300 by 100 pixels,'
            panel.setSize(300, 100)
            panel.doLayout()
        then : 'every child has the size from the table.'
            (0..<children).every { panel.getComponent(it).getWidth() == cellWidth && panel.getComponent(it).getHeight() == cellHeight }

        where :
            children || cellWidth | cellHeight
            1        || 300       | 100
            2        || 150       | 100
            3        || 100       | 100
            4        || 100       | 50
            6        || 100       | 50
            7        || 100       | 33
    }

    def 'Use `withGridLayout()` to place all children side by side in a single row.'()
    {
        reportInfo """
            Without any arguments, `withGridLayout()` attaches a grid of a single row and as
            many columns as there are children, which is the same as `withGridLayout(1, 0)`.
            So every child gets an equal share of the width and the full height, like the
            buttons of a button bar.
        """
        given : 'A panel with the default grid layout, holding 4 buttons.'
            var panel =
                    UI.panel().withGridLayout()
                    .add(UI.button("Back")).add(UI.button("Next")).add(UI.button("Skip")).add(UI.button("Finish"))
                    .get(JPanel)
        expect : 'The panel has a uniform grid layout of 1 row and as many columns as needed.'
            panel.getLayout() instanceof UniformGridLayout
            panel.getLayout().getRows() == 1
            panel.getLayout().getColumns() == 0

        when : 'The panel is laid out at 200 by 30 pixels,'
            panel.setSize(200, 30)
            panel.doLayout()
        then : 'the 4 buttons share the row equally.'
            (0..<4).collect { panel.getComponent(it).getBounds() } == [
                new Rectangle(  0, 0, 50, 30),
                new Rectangle( 50, 0, 50, 30),
                new Rectangle(100, 0, 50, 30),
                new Rectangle(150, 0, 50, 30)
            ]
    }

    def 'The gaps of `withGridLayout(rows, cols, horizontalGap, verticalGap)` are scaled by the UI scale factor.'(
        float scaleFactor, int horizontalGap, int verticalGap
    ) {
        reportInfo """
            You declare the gaps between the cells for a UI scale factor of 1, and the grid
            scales them by the current UI scale factor every time it lays out the component,
            rounded to the nearest whole pixel. That is what `withFlowLayout(..)` does with
            its gaps too, so both keep their proportions on high resolution screens.

            The panels in this table are built after the scale factor is set, like the UI
            of an application which starts on a high resolution screen. A horizontal gap of
            7 pixels becomes 9 pixels at a scale factor of 1.25 and 14 pixels at 2, and a
            vertical gap of 3 pixels becomes 4 and 6 pixels. The layout manager keeps
            reporting the gaps as you declared them.
        """
        given : 'The UI scale factor from the table, as a high resolution screen would set it.'
            SwingTree.get().setUiScaleFactor(scaleFactor)
        and : 'A panel with a grid of 2 rows and 3 columns, a horizontal gap of 7 and a vertical gap of 3, holding 6 children.'
            var panel =
                    UI.panel().withGridLayout(2, 3, 7, 3)
                    .apply({ ui -> (0..<6).each { ui.add(UI.box()) } })
                    .get(JPanel)
        expect : 'The layout manager reports the gaps as they were declared.'
            panel.getLayout().getHgap() == 7
            panel.getLayout().getVgap() == 3
        and : 'The gaps we expect are the declared gaps scaled by the UI scale factor.'
            horizontalGap == UI.scale(7)
            verticalGap   == UI.scale(3)

        when : 'The panel is laid out exactly large enough for cells of 40 by 20 pixels with the scaled gaps between them,'
            panel.setSize(3 * 40 + 2 * horizontalGap, 2 * 20 + verticalGap)
            panel.doLayout()
        then : 'every child sits in its 40 by 20 pixel cell, with the scaled gaps between the cells.'
            (0..<6).collect { panel.getComponent(it).getBounds() } ==
                (0..<6).collect { i -> new Rectangle((i % 3) * (40 + horizontalGap), i.intdiv(3) * (20 + verticalGap), 40, 20) }

        cleanup :
            SwingTree.get().setUiScaleFactor(1f)

        where :
            scaleFactor || horizontalGap | verticalGap
            1f          || 7             | 3
            1.25f       || 9             | 4
            1.5f        || 11            | 5
            1.75f       || 12            | 5
            2f          || 14            | 6
    }

    def 'Pass a `Mode` and a `CollapseEmpty` setting to `withGridLayout(..)` to choose how the grid treats its rows and columns.'(
        Mode mode, CollapseEmpty collapseEmpty, List<Rectangle> bounds
    ) {
        reportInfo """
            `withGridLayout(mode, collapseEmpty, rows, cols, horizontalGap, verticalGap)`
            attaches a `UniformGridLayout` with the settings you choose. The table lays out
            the same 3 children in a panel of 220 by 45 pixels with a grid of 2 rows and 5
            columns and gaps of 5 pixels, which is exactly large enough for 5 cells of 40
            pixels in width and 2 cells of 20 pixels in height.

            With `ROWS_AND_COLUMNS`, the default, the children share a single row of 3
            columns. `COLUMNS` keeps the empty second row, `ROWS` keeps the empty fourth and
            fifth column, and `NONE` keeps both, so the children take the first 3 cells of
            all 10. In the mode `SPREAD_OVER_ROWS`, the 3 children are spread over the 2 rows
            in 2 columns, like a `GridLayout` spreads them, which leaves nothing empty.
        """
        given : 'A panel with a grid of 2 rows and 5 columns, gaps of 5 pixels and the settings from the table, holding 3 children.'
            var panel =
                    UI.panel().withGridLayout(mode, collapseEmpty, 2, 5, 5, 5)
                    .add(UI.box()).add(UI.box()).add(UI.box())
                    .get(JPanel)
        expect : 'The panel has a uniform grid layout with those settings.'
            panel.getLayout() instanceof UniformGridLayout
            panel.getLayout().getMode() == mode
            panel.getLayout().getCollapseEmpty() == collapseEmpty
            [panel.getLayout().getRows(), panel.getLayout().getColumns(), panel.getLayout().getHgap(), panel.getLayout().getVgap()] == [2, 5, 5, 5]

        when : 'The panel is laid out at 220 by 45 pixels,'
            panel.setSize(220, 45)
            panel.doLayout()
        then : 'the children have the bounds from the table.'
            (0..<3).collect { panel.getComponent(it).getBounds() } == bounds

        where :
            mode                    | collapseEmpty                  | bounds
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.ROWS_AND_COLUMNS | [new Rectangle(0, 0, 70, 45),  new Rectangle(75, 0, 70, 45),  new Rectangle(150, 0, 70, 45)]
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.COLUMNS          | [new Rectangle(0, 0, 70, 20),  new Rectangle(75, 0, 70, 20),  new Rectangle(150, 0, 70, 20)]
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.ROWS             | [new Rectangle(0, 0, 40, 45),  new Rectangle(45, 0, 40, 45),  new Rectangle( 90, 0, 40, 45)]
            Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.NONE             | [new Rectangle(0, 0, 40, 20),  new Rectangle(45, 0, 40, 20),  new Rectangle( 90, 0, 40, 20)]
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.NONE             | [new Rectangle(0, 0, 107, 20), new Rectangle(112, 0, 107, 20), new Rectangle(  0, 25, 107, 20)]
            Mode.SPREAD_OVER_ROWS   | CollapseEmpty.ROWS_AND_COLUMNS | [new Rectangle(0, 0, 107, 20), new Rectangle(112, 0, 107, 20), new Rectangle(  0, 25, 107, 20)]
    }

    def 'Use `withGridLayout(Mode.SPREAD_OVER_ROWS, CollapseEmpty.NONE, OverflowGrowth.ADD_COLUMNS, rows, cols)` to lay out children exactly like a `GridLayout`.'(
        int rows, int cols, int children
    ) {
        reportInfo """
            The `GridLayout` of the JDK ignores its number of columns whenever its number of
            rows is not zero, spreads the children over its rows, and keeps empty rows. When it
            runs out of cells it makes room in one direction only: it keeps its rows and widens
            itself. A grid which spreads its children over its rows, collapses nothing and grows
            by columns does exactly the same, in all three respects. So if a part of your UI
            relies on how a `GridLayout` arranges its children, these are the three settings
            which keep that arrangement while using `withGridLayout(..)`.

            The table compares both layout managers for grids with more children than cells,
            fewer children than cells and rows or columns left open, in a panel of 230 by 70
            pixels, which does not divide evenly into cells for most of them.
        """
        given : 'A panel with a grid which spreads its children over its rows, collapses nothing and grows by columns.'
            var ours =
                    UI.panel().withGridLayout(Mode.SPREAD_OVER_ROWS, CollapseEmpty.NONE, OverflowGrowth.ADD_COLUMNS, rows, cols)
                    .apply({ ui -> (0..<children).each { ui.add(UI.box()) } })
                    .get(JPanel)
        and : 'A panel with a `GridLayout` of the same numbers of rows and columns, holding as many children.'
            var awts =
                    UI.panel().withLayout(new GridLayout(rows, cols))
                    .apply({ ui -> (0..<children).each { ui.add(UI.box()) } })
                    .get(JPanel)
        when : 'Both panels are laid out at 230 by 70 pixels,'
            ours.setSize(230, 70)
            ours.doLayout()
            awts.setSize(230, 70)
            awts.doLayout()
        then : 'every child of the uniform grid has exactly the bounds of the child with the same index in the `GridLayout`.'
            (0..<children).collect { ours.getComponent(it).getBounds() } == (0..<children).collect { awts.getComponent(it).getBounds() }

        where :
            rows | cols | children
            2    | 5    | 8
            2    | 5    | 3
            2    | 5    | 13
            3    | 0    | 2
            0    | 3    | 7
    }

    def '`withGridLayout(..)` does not accept a missing setting.'()
    {
        reportInfo """
            A grid needs both of its settings to decide how it lays out its children, so
            `withGridLayout(..)` rejects a `null` mode or a `null` setting for its empty rows
            and columns right away, with an `IllegalArgumentException`, instead of failing
            later when the component is laid out.
        """
        when : 'We pass no mode,'
            UI.panel().withGridLayout(null, CollapseEmpty.NONE, 2, 5)
        then : 'the builder rejects it.'
            thrown(IllegalArgumentException)

        when : 'We pass no setting for the empty rows and columns,'
            UI.panel().withGridLayout(Mode.WRAP_AFTER_COLUMNS, null, 2, 5, 5, 5)
        then : 'the builder rejects that as well.'
            thrown(IllegalArgumentException)
    }

    def 'Use `Layout.grid(..)` in a style to give a component a uniform grid.'(
        Layout grid, int rows, int cols, int gap, Mode mode, CollapseEmpty collapseEmpty, int cellWidth, int cellHeight
    ) {
        reportInfo """
            In the style API, `it.layout(Layout.grid(..))` installs a `UniformGridLayout`.
            `Layout.grid(..)` takes the same arguments as `withGridLayout(..)`: the numbers
            of rows and columns, optionally the gaps, and optionally a `Mode` and a
            `CollapseEmpty` setting in front of them. You can also change the settings of a
            grid you already have, with `withMode(..)` and `withCollapseEmpty(..)`, which
            return a new grid and leave the original one untouched.

            Every panel in this table holds 3 children and is laid out at 220 by 45 pixels,
            and the last two columns show the size every child gets.
        """
        given : 'A panel whose style declares the grid from the table, holding 3 children.'
            var panel =
                    UI.panel()
                    .withStyle( it -> it.layout(grid) )
                    .add(UI.box()).add(UI.box()).add(UI.box())
                    .get(JPanel)
        expect : 'The panel has a uniform grid layout with the properties from the table.'
            panel.getLayout() instanceof UniformGridLayout
            panel.getLayout().getRows() == rows
            panel.getLayout().getColumns() == cols
            panel.getLayout().getHgap() == gap
            panel.getLayout().getVgap() == gap
            panel.getLayout().getMode() == mode
            panel.getLayout().getCollapseEmpty() == collapseEmpty

        when : 'The panel is laid out at 220 by 45 pixels,'
            panel.setSize(220, 45)
            panel.doLayout()
        then : 'every child has the size from the table.'
            (0..<3).every { panel.getComponent(it).getWidth() == cellWidth && panel.getComponent(it).getHeight() == cellHeight }

        where :
            grid                                                                          || rows | cols | gap | mode                    | collapseEmpty                  | cellWidth | cellHeight
            Layout.grid(2, 5)                                                             || 2    | 5    | 0   | Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.ROWS_AND_COLUMNS | 73        | 45
            Layout.grid(2, 5, 5, 5)                                                       || 2    | 5    | 5   | Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.ROWS_AND_COLUMNS | 70        | 45
            Layout.grid(Mode.WRAP_AFTER_COLUMNS, CollapseEmpty.NONE, 2, 5)                || 2    | 5    | 0   | Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.NONE             | 44        | 22
            Layout.grid(Mode.SPREAD_OVER_ROWS, CollapseEmpty.NONE, 2, 5, 5, 5)            || 2    | 5    | 5   | Mode.SPREAD_OVER_ROWS   | CollapseEmpty.NONE             | 107       | 20
            Layout.grid(2, 5, 5, 5).withCollapseEmpty(CollapseEmpty.COLUMNS)              || 2    | 5    | 5   | Mode.WRAP_AFTER_COLUMNS | CollapseEmpty.COLUMNS          | 70        | 20
            Layout.grid(2, 5).withMode(Mode.SPREAD_OVER_ROWS)                             || 2    | 5    | 0   | Mode.SPREAD_OVER_ROWS   | CollapseEmpty.ROWS_AND_COLUMNS | 110       | 22
    }

    def 'A `Layout.grid(..)` style updates the grid of `withGridLayout(..)` in place, settings included.'()
    {
        reportInfo """
            `withGridLayout(rows, cols)` and a style declaring `Layout.grid(..)` both install a
            `UniformGridLayout`. When a style declares a grid for a component which already
            has one, SwingTree reconfigures the layout manager the component already has
            instead of creating a new one. So you can declare a grid in the builder and let
            a style adjust it later, and anything which holds on to the layout manager keeps
            seeing the one the component really uses.
        """
        given : 'A variable for the layout manager which the builder method installs.'
            var installedByBuilder = null
        and : 'A panel which gets a default grid of 2 rows and 5 columns from the builder, and a different grid with different settings from its style.'
            var panel =
                    UI.panel()
                    .withGridLayout(2, 5)
                    .peek({ installedByBuilder = it.getLayout() })
                    .withStyle( it -> it
                        .layout(Layout.grid(Mode.SPREAD_OVER_ROWS, CollapseEmpty.NONE, 3, 3, 4, 2))
                    )
                    .get(JPanel)

        expect : 'The builder method installed a uniform grid layout.'
            installedByBuilder instanceof UniformGridLayout
        and : 'It is still the layout manager of the panel, now with the numbers, gaps and settings of the style.'
            panel.getLayout().is(installedByBuilder)
            [panel.getLayout().getRows(), panel.getLayout().getColumns()] == [3, 3]
            [panel.getLayout().getHgap(), panel.getLayout().getVgap()] == [4, 2]
            panel.getLayout().getMode() == Mode.SPREAD_OVER_ROWS
            panel.getLayout().getCollapseEmpty() == CollapseEmpty.NONE
    }

    def 'A `Var<Layout>` holding `Layout.grid(..)` changes the settings of a grid while the application runs.'()
    {
        reportInfo """
            When you bind a component to a `Var<Layout>` with `withLayout(..)`, every new
            `Layout.grid(..)` value you set reconfigures the `UniformGridLayout` of the
            component in place, including its mode and which empty rows and columns it leaves
            out. A button which switches a panel between two looks only has to set a new value.

            The panel in this feature holds 3 children in a grid of 2 rows and 5 columns with
            gaps of 5 pixels, laid out at 220 by 45 pixels. With the default settings, the
            children share a single row of 3 columns, 70 by 45 pixels each. When nothing is
            collapsed, they take the first 3 of 10 cells of 40 by 20 pixels, and when they are
            spread over the rows, they get 2 rows of 2 columns, 107 by 20 pixels each.
        """
        given : 'A property holding a default grid of 2 rows and 5 columns with gaps of 5 pixels.'
            var layout = Var.of(Layout.class, Layout.grid(2, 5, 5, 5))
        and : 'A panel bound to the property, holding 3 children.'
            var panel =
                    UI.panel()
                    .withLayout(layout)
                    .add(UI.box()).add(UI.box()).add(UI.box())
                    .get(JPanel)
        and : 'We remember the layout manager the panel starts with.'
            var gridLayout = panel.getLayout()

        when : 'The panel is laid out at 220 by 45 pixels,'
            panel.setSize(220, 45)
            panel.doLayout()
        then : 'the children share a single row of 3 columns.'
            gridLayout instanceof UniformGridLayout
            (0..<3).collect { panel.getComponent(it).getBounds() } == [new Rectangle(0, 0, 70, 45), new Rectangle(75, 0, 70, 45), new Rectangle(150, 0, 70, 45)]

        when : 'We set the same grid, but without collapsing anything, and lay out the panel again,'
            UI.runNow({ layout.set(Layout.grid(2, 5, 5, 5).withCollapseEmpty(CollapseEmpty.NONE)) })
            panel.doLayout()
        then : 'the very same layout manager keeps the empty cells, and the children take the first 3 of them.'
            panel.getLayout().is(gridLayout)
            panel.getLayout().getCollapseEmpty() == CollapseEmpty.NONE
            (0..<3).collect { panel.getComponent(it).getBounds() } == [new Rectangle(0, 0, 40, 20), new Rectangle(45, 0, 40, 20), new Rectangle(90, 0, 40, 20)]

        when : 'We set a grid which spreads its children over its rows, and lay out the panel again,'
            UI.runNow({ layout.set(Layout.grid(Mode.SPREAD_OVER_ROWS, CollapseEmpty.NONE, 2, 5, 5, 5)) })
            panel.doLayout()
        then : 'the very same layout manager spreads the 3 children over 2 rows of 2 columns.'
            panel.getLayout().is(gridLayout)
            panel.getLayout().getMode() == Mode.SPREAD_OVER_ROWS
            (0..<3).collect { panel.getComponent(it).getBounds() } == [new Rectangle(0, 0, 107, 20), new Rectangle(112, 0, 107, 20), new Rectangle(0, 25, 107, 20)]
    }

    def 'A `Var<Layout>` can switch a grid from a fixed number of rows to a fixed number of columns in place.'()
    {
        reportInfo """
            A row count of zero means "as many rows as the children need", and a column count
            of zero means the same for columns, but a grid may not leave both of them open. A
            `UniformGridLayout` rejects that with an `IllegalArgumentException`, whether the two
            zeros are given to its constructor or set one after the other through `setRows`
            and `setColumns`.

            When a property switches a panel from `Layout.grid(2, 0)` to `Layout.grid(0, 3)`,
            both grids are perfectly valid, but updating the installed layout manager in place
            could pass through a moment in which the rows are already zero and the columns are
            still zero. SwingTree therefore sets the count which stays greater than zero first,
            so the grid never leaves both counts open, not even between two setter calls.
        """
        given : 'A property holding a grid of 2 rows and as many columns as needed.'
            var layout = Var.of(Layout.class, Layout.grid(2, 0))
        and : 'A panel bound to the property.'
            var panel = UI.panel().withLayout(layout).get(JPanel)
        and : 'We remember the layout manager the panel starts with.'
            var gridLayout = panel.getLayout()

        when : 'We switch to a grid of 3 columns and as many rows as needed,'
            UI.runNow({ layout.set(Layout.grid(0, 3)) })
        then : 'the very same layout manager now has 0 rows and 3 columns.'
            panel.getLayout().is(gridLayout)
            panel.getLayout().getRows() == 0
            panel.getLayout().getColumns() == 3

        when : 'We switch back to 2 rows and as many columns as needed,'
            UI.runNow({ layout.set(Layout.grid(2, 0)) })
        then : 'that works just as well.'
            panel.getLayout().is(gridLayout)
            panel.getLayout().getRows() == 2
            panel.getLayout().getColumns() == 0
    }

    def 'Pass an `OverflowGrowth` setting to `withGridLayout(..)` to choose how a grid grows when it holds more children than cells.'(
        int children, OverflowGrowth overflowGrowth, int rows, int cols
    ) {
        reportInfo """
            A grid of 2 rows and 5 columns has 10 cells, so from the eleventh child on it has to
            grow. `withGridLayout(mode, collapseEmpty, overflowGrowth, rows, cols)` chooses how:

              - `ADD_ROWS`, the default, keeps the 5 declared columns and adds rows below them,
              - `ADD_COLUMNS` keeps the 2 declared rows and adds columns to their right,
              - `ADD_ROWS_AND_COLUMNS` adds a row or a column at a time, each time the one which
                keeps the ratio of rows to columns closest to the declared 2 : 5.

            The table lays out a growing number of children in a panel of 600 by 300 pixels and
            reports the grid every setting builds, as rows by columns. Only `ADD_ROWS` keeps the
            child at index `i` in row `i / 5`; the other two settings rearrange the children the
            moment the grid outgrows its declared shape.
        """
        given : 'A panel with a grid of 2 rows and 5 columns and the growth setting from the table, holding more children than its 10 cells.'
            var panel =
                    UI.panel().withGridLayout(Mode.WRAP_AFTER_COLUMNS, CollapseEmpty.ROWS_AND_COLUMNS, overflowGrowth, 2, 5)
                    .apply({ ui -> (0..<children).each { ui.add(UI.box()) } })
                    .get(JPanel)
        expect : 'The panel has a uniform grid layout with that growth setting.'
            panel.getLayout() instanceof UniformGridLayout
            panel.getLayout().getOverflowGrowth() == overflowGrowth
        and : 'The panel really does hold more children than the 10 cells of the declared grid.'
            children > 2 * 5

        when : 'The panel is laid out at 600 by 300 pixels,'
            panel.setSize(600, 300)
            panel.doLayout()
        then : 'the children are arranged in the grid from the table.'
            gridShapeOf(panel) == [rows, cols]
        and : 'that grid really does have a cell for every child.'
            rows * cols >= children

        where :
            children | overflowGrowth                        || rows | cols
            11       | OverflowGrowth.ADD_ROWS               || 3    | 5
            11       | OverflowGrowth.ADD_COLUMNS            || 2    | 6
            11       | OverflowGrowth.ADD_ROWS_AND_COLUMNS   || 2    | 6
            13       | OverflowGrowth.ADD_ROWS               || 3    | 5
            13       | OverflowGrowth.ADD_COLUMNS            || 2    | 7
            13       | OverflowGrowth.ADD_ROWS_AND_COLUMNS   || 3    | 6
            20       | OverflowGrowth.ADD_ROWS               || 4    | 5
            20       | OverflowGrowth.ADD_COLUMNS            || 2    | 10
            20       | OverflowGrowth.ADD_ROWS_AND_COLUMNS   || 3    | 7
            30       | OverflowGrowth.ADD_ROWS               || 6    | 5
            30       | OverflowGrowth.ADD_COLUMNS            || 2    | 15
            30       | OverflowGrowth.ADD_ROWS_AND_COLUMNS   || 4    | 9
    }

    def '`OverflowGrowth.ADD_ROWS_AND_COLUMNS` keeps the declared ratio exactly when a grid scales up evenly.'(
        int rows, int cols, int factor
    ) {
        reportInfo """
            `ADD_ROWS_AND_COLUMNS` grows a grid towards the ratio of rows to columns it was declared
            with. Whenever a component holds exactly `factor * factor` times as many children as the
            declared grid has cells, that ratio can be met exactly, and the grid becomes exactly
            `factor` times as high and `factor` times as wide, without a single cell left over.

            So a grid declared with 1 row and 4 columns, holding 400 children, which is 100 times its
            4 cells, is laid out in exactly 10 rows of 40 columns. Because the component keeps its
            size while the grid grows, holding the ratio of rows to columns also holds the shape of
            every cell: a cell of a grid of `r` rows and `c` columns is `width / c` by `height / r`
            large, so all 400 cells have the same shape the 4 declared cells had.
        """
        given : 'The number of children which fills the declared grid scaled up by the factor from the table.'
            var children = rows * cols * factor * factor
        and : 'A panel which grows such a grid by rows and columns at once, holding that many children.'
            var panel =
                    UI.panel().withGridLayout(Mode.WRAP_AFTER_COLUMNS, CollapseEmpty.ROWS_AND_COLUMNS, OverflowGrowth.ADD_ROWS_AND_COLUMNS, rows, cols)
                    .apply({ ui -> (0..<children).each { ui.add(UI.box()) } })
                    .get(JPanel)

        when : 'The panel is laid out at 800 by 400 pixels,'
            panel.setSize(800, 400)
            panel.doLayout()
        then : 'the grid is exactly the declared one scaled up by the factor,'
            gridShapeOf(panel) == [rows * factor, cols * factor]
        and : 'it holds exactly as many cells as there are children, so none is left empty.'
            rows * factor * cols * factor == children

        where :
            rows | cols | factor
            1    | 4    | 2
            1    | 4    | 5
            1    | 4    | 10
            2    | 5    | 2
            2    | 5    | 4
            3    | 7    | 2
    }

    def 'The `OverflowGrowth` setting changes nothing about a grid which cannot overflow.'(
        Mode mode, int rows, int cols, int expectedRows, int expectedCols
    ) {
        reportInfo """
            A grid can only hold more children than cells when it has a cell count to exceed, and
            it only has one when both its declared numbers are greater than zero. A number of zero
            means "as many as the children need", so that axis grows as far as the children need in
            either mode and there is never an overflow to handle. The growth setting then has
            nothing to decide, whichever of the three you pick.

            This is the one case in which the setting is idle. Both modes cap a grid as soon as you
            declare both numbers, so both modes hand the growth setting something to decide, which
            the feature after this one shows.

            Every panel in this table holds 13 children and is laid out three times, once per
            `OverflowGrowth` setting, and every one of the three lays the children out in the very
            same grid.
        """
        given : 'The three growth settings, and a panel per setting with the grid from the table, each holding 13 children.'
            var panels = OverflowGrowth.values().collect { growth ->
                    UI.panel().withGridLayout(mode, CollapseEmpty.ROWS_AND_COLUMNS, growth, rows, cols)
                    .apply({ ui -> (0..<13).each { ui.add(UI.box()) } })
                    .get(JPanel)
                }
        expect : 'There is one panel per growth setting.'
            panels.size() == 3

        when : 'All of them are laid out at 600 by 300 pixels,'
            panels.each { it.setSize(600, 300); it.doLayout() }
        then : 'every one of them arranges its children in the grid from the table.'
            panels.collect { gridShapeOf(it) } == [[expectedRows, expectedCols]] * 3

        where :
            mode                    | rows | cols || expectedRows | expectedCols
            Mode.WRAP_AFTER_COLUMNS | 0    | 4    || 4            | 4
            Mode.WRAP_AFTER_COLUMNS | 2    | 0    || 2            | 7
            Mode.SPREAD_OVER_ROWS   | 0    | 4    || 4            | 4
            Mode.SPREAD_OVER_ROWS   | 2    | 0    || 2            | 7
    }

    def 'A grid grows the way its `OverflowGrowth` setting says in both modes.'(
        Mode mode, int children, OverflowGrowth overflowGrowth, int rows, int cols
    ) {
        reportInfo """
            The mode decides the shape of the grid while it still has a cell for every child:
            `WRAP_AFTER_COLUMNS` keeps the declared columns and fills the rows one after another,
            while `SPREAD_OVER_ROWS` keeps the declared rows and takes only as many columns as the
            children need. Once there are more children than the declared numbers multiply to, that
            question is settled and a second one takes over: on which axis does the grid make room?
            That is what `OverflowGrowth` answers, and it answers it in both modes.

            Every panel in this table declares 2 rows and 4 columns, which is 8 cells, and is laid
            out at 600 by 300 pixels. With 8 children both modes agree on 2 rows of 4 columns. With
            the ninth child the grid has to grow, and each setting grows it the way it promises,
            whichever mode built the grid underneath.
        """
        given : 'A panel with a grid of 2 rows and 4 columns and the mode and growth setting from the table.'
            var panel =
                    UI.panel().withGridLayout(mode, CollapseEmpty.NONE, overflowGrowth, 2, 4)
                    .apply({ ui -> (0..<children).each { ui.add(UI.box()) } })
                    .get(JPanel)

        when : 'The panel is laid out at 600 by 300 pixels,'
            panel.setSize(600, 300)
            panel.doLayout()
        then : 'the children are arranged in the grid from the table.'
            gridShapeOf(panel) == [rows, cols]

        where :
            mode                    | children | overflowGrowth                      || rows | cols
            Mode.WRAP_AFTER_COLUMNS | 8        | OverflowGrowth.ADD_ROWS             || 2    | 4
            Mode.WRAP_AFTER_COLUMNS | 9        | OverflowGrowth.ADD_ROWS             || 3    | 4
            Mode.WRAP_AFTER_COLUMNS | 9        | OverflowGrowth.ADD_COLUMNS          || 2    | 5
            Mode.WRAP_AFTER_COLUMNS | 9        | OverflowGrowth.ADD_ROWS_AND_COLUMNS || 2    | 5
            Mode.WRAP_AFTER_COLUMNS | 11       | OverflowGrowth.ADD_ROWS_AND_COLUMNS || 3    | 5
            Mode.SPREAD_OVER_ROWS   | 8        | OverflowGrowth.ADD_ROWS             || 2    | 4
            Mode.SPREAD_OVER_ROWS   | 9        | OverflowGrowth.ADD_ROWS             || 3    | 4
            Mode.SPREAD_OVER_ROWS   | 9        | OverflowGrowth.ADD_COLUMNS          || 2    | 5
            Mode.SPREAD_OVER_ROWS   | 9        | OverflowGrowth.ADD_ROWS_AND_COLUMNS || 2    | 5
            Mode.SPREAD_OVER_ROWS   | 11       | OverflowGrowth.ADD_ROWS_AND_COLUMNS || 3    | 5
    }

    def '`withGridLayout(..)` does not accept a missing growth setting.'()
    {
        reportInfo """
            Just like the mode and the setting for the empty rows and columns, a grid needs to know
            how it grows before it lays out a single child, so `withGridLayout(..)` rejects a `null`
            growth setting right away with an `IllegalArgumentException`.
        """
        when : 'We pass no growth setting,'
            UI.panel().withGridLayout(Mode.WRAP_AFTER_COLUMNS, CollapseEmpty.NONE, null, 2, 5)
        then : 'the builder rejects it.'
            thrown(IllegalArgumentException)

        when : 'We pass no growth setting to the overload which also takes the gaps,'
            UI.panel().withGridLayout(Mode.WRAP_AFTER_COLUMNS, CollapseEmpty.NONE, null, 2, 5, 5, 5)
        then : 'the builder rejects that as well.'
            thrown(IllegalArgumentException)
    }

    def 'Use `Layout.grid(..).withOverflowGrowth(..)` to change how a grid grows while the application runs.'()
    {
        reportInfo """
            `Layout.grid(..)` takes an `OverflowGrowth` setting alongside the mode and the empty rows
            and columns, and `withOverflowGrowth(..)` returns a new grid with that setting and every
            other property copied unchanged, leaving the original grid untouched.

            When such a grid is set on a `Var<Layout>` which a component is bound to, the
            `UniformGridLayout` the component already has is reconfigured in place, so a panel can
            switch between growing by rows and growing by rows and columns without ever exchanging
            its layout manager.

            The panel in this feature holds 13 children in a grid of 2 rows and 5 columns, which has
            only 10 cells.
        """
        given : 'A property holding a grid of 2 rows and 5 columns, which grows by rows.'
            var layout = Var.of(Layout.class, Layout.grid(2, 5))
        and : 'A panel bound to the property, holding 13 children.'
            var panel =
                    UI.panel()
                    .withLayout(layout)
                    .apply({ ui -> (0..<13).each { ui.add(UI.box()) } })
                    .get(JPanel)
        and : 'We remember the layout manager the panel starts with.'
            var gridLayout = panel.getLayout()

        expect : 'A grid grows by rows unless you say otherwise, and `withOverflowGrowth(..)` leaves the grid it is called on untouched.'
            Layout.grid(2, 5).withOverflowGrowth(OverflowGrowth.ADD_COLUMNS) != Layout.grid(2, 5)
            Layout.grid(2, 5).withOverflowGrowth(OverflowGrowth.ADD_ROWS) == Layout.grid(2, 5)

        when : 'The panel is laid out at 600 by 300 pixels,'
            panel.setSize(600, 300)
            panel.doLayout()
        then : 'the 13 children fill 3 rows of the 5 declared columns.'
            gridLayout instanceof UniformGridLayout
            gridLayout.getOverflowGrowth() == OverflowGrowth.ADD_ROWS
            gridShapeOf(panel) == [3, 5]

        when : 'We set the same grid, but growing by rows and columns at once, and lay out the panel again,'
            UI.runNow({ layout.set(Layout.grid(2, 5).withOverflowGrowth(OverflowGrowth.ADD_ROWS_AND_COLUMNS)) })
            panel.doLayout()
        then : 'the very same layout manager now arranges the children in 3 rows of 6 columns.'
            panel.getLayout().is(gridLayout)
            panel.getLayout().getOverflowGrowth() == OverflowGrowth.ADD_ROWS_AND_COLUMNS
            gridShapeOf(panel) == [3, 6]

        when : 'We set a grid which grows by columns instead, and lay out the panel again,'
            UI.runNow({ layout.set(Layout.grid(Mode.WRAP_AFTER_COLUMNS, CollapseEmpty.ROWS_AND_COLUMNS, OverflowGrowth.ADD_COLUMNS, 2, 5)) })
            panel.doLayout()
        then : 'the very same layout manager keeps the 2 declared rows and widens the grid to 7 columns.'
            panel.getLayout().is(gridLayout)
            panel.getLayout().getOverflowGrowth() == OverflowGrowth.ADD_COLUMNS
            gridShapeOf(panel) == [2, 7]
    }

    def 'The `UniformGridLayout` constructor which takes every setting builds the grid all three settings describe.'()
    {
        reportInfo """
            Outside of a SwingTree UI declaration, the constructor
            `UniformGridLayout(mode, collapseEmpty, overflowGrowth, rows, cols, hgap, vgap)` spells
            out every setting of a grid in one call, so you never have to build a layout manager and
            then reach for its setters. The other constructors leave the three settings at their
            defaults: `WRAP_AFTER_COLUMNS`, `ROWS_AND_COLUMNS` and `ADD_ROWS`.
        """
        given : 'A grid layout with every setting spelled out.'
            var layout = new UniformGridLayout(Mode.WRAP_AFTER_COLUMNS, CollapseEmpty.NONE, OverflowGrowth.ADD_ROWS_AND_COLUMNS, 2, 5, 5, 5)
        expect : 'It reports every one of them.'
            layout.getMode() == Mode.WRAP_AFTER_COLUMNS
            layout.getCollapseEmpty() == CollapseEmpty.NONE
            layout.getOverflowGrowth() == OverflowGrowth.ADD_ROWS_AND_COLUMNS
            [layout.getRows(), layout.getColumns(), layout.getHgap(), layout.getVgap()] == [2, 5, 5, 5]
        and : 'A grid layout built without them uses the default settings.'
            new UniformGridLayout(2, 5).getMode() == Mode.WRAP_AFTER_COLUMNS
            new UniformGridLayout(2, 5).getCollapseEmpty() == CollapseEmpty.ROWS_AND_COLUMNS
            new UniformGridLayout(2, 5).getOverflowGrowth() == OverflowGrowth.ADD_ROWS

        when : 'We install it on a panel with 13 children and lay the panel out at 600 by 300 pixels,'
            var panel =
                    UI.panel().withLayout(layout)
                    .apply({ ui -> (0..<13).each { ui.add(UI.box()) } })
                    .get(JPanel)
            panel.setSize(600, 300)
            panel.doLayout()
        then : 'the children are arranged in the 3 rows of 6 columns the growth setting asks for.'
            gridShapeOf(panel) == [3, 6]

        when : 'We ask the layout manager to describe itself,'
            var description = layout.toString()
        then : 'it names all three settings.'
            description.contains("mode=WRAP_AFTER_COLUMNS")
            description.contains("collapseEmpty=NONE")
            description.contains("overflowGrowth=ADD_ROWS_AND_COLUMNS")
    }

    def 'A grid layout rejects a missing growth setting instead of quietly keeping the old one.'()
    {
        reportInfo """
            `setOverflowGrowth(null)` throws a `NullPointerException`, and the constructor which takes
            every setting does the same, so a grid never ends up without an answer to how it grows.
        """
        when : 'We take the growth setting away from a grid,'
            new UniformGridLayout(2, 5).setOverflowGrowth(null)
        then : 'it is rejected,'
            thrown(NullPointerException)

        when : 'and we build a grid without one,'
            new UniformGridLayout(Mode.WRAP_AFTER_COLUMNS, CollapseEmpty.NONE, null, 2, 5, 0, 0)
        then : 'that is rejected as well.'
            thrown(NullPointerException)
    }

    /**
     *  Reports the grid a laid out panel arranged its children in, as a list of the number of rows
     *  and the number of columns. Every cell of a row shares its y coordinate and every cell of a
     *  column shares its x coordinate, so counting the distinct coordinates counts the rows and
     *  columns which hold at least one child.
     */
    private static List<Integer> gridShapeOf( JPanel panel ) {
        var bounds = (0..<panel.getComponentCount()).collect { panel.getComponent(it).getBounds() }
        return [ bounds.collect({ it.@y }).unique().size(), bounds.collect({ it.@x }).unique().size() ]
    }
}
