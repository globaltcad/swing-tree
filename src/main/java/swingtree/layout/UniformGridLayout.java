package swingtree.layout;

import swingtree.UI;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.Objects;

/**
 *  A layout manager which divides a container into a grid of cells of equal size
 *  and puts every component of the container into a cell of its own.
 *  The components fill the cells row by row, starting with the top row,
 *  and every component is resized to exactly the size of its cell,
 *  whatever size it would prefer.
 *  <p>
 *  In a SwingTree UI declaration, you install it through
 *  {@link swingtree.UIForAnySwing#withGridLayout(int, int, int, int)}:
 *  <pre>{@code
 *  UI.panel().withGridLayout(2, 3, 5, 5)
 *  .add(UI.button("1")).add(UI.button("2")).add(UI.button("3"))
 *  .add(UI.button("4")).add(UI.button("5"))
 *  }</pre>
 *  This panel has a grid of 2 rows and 3 columns, with a gap of 5 pixels between
 *  neighbouring cells. The buttons 1, 2 and 3 fill the top row from left to right,
 *  the buttons 4 and 5 take the first two cells of the bottom row, and the last
 *  cell of the bottom row stays empty. If the panel only held the buttons 1 and 2,
 *  they would sit side by side in a single row of 2 columns, as high as the whole
 *  panel, because by default the rows and columns which no component occupies are
 *  left out of the grid.
 *  <p>
 *  In the style API and in a reactive {@code Var<Layout>}, {@link swingtree.api.Layout#grid(int, int, int, int)}
 *  installs the same layout manager, and both {@code withGridLayout(..)} and {@code Layout.grid(..)}
 *  accept a {@link Mode}, a {@link CollapseEmpty} and an {@link OverflowGrowth} setting in front of
 *  the numbers of rows and columns. Outside of a SwingTree UI declaration, you install it like any
 *  other layout manager: {@code panel.setLayout(new UniformGridLayout(2, 3, 5, 5))}, or with every
 *  setting spelled out through
 *  {@link #UniformGridLayout(Mode, CollapseEmpty, OverflowGrowth, int, int, int, int)}.
 *
 *  <h2>How the grid is built</h2>
 *
 *  You declare a number of rows and a number of columns, and three settings decide what
 *  the layout makes of them:
 *  <ul>
 *      <li>
 *          The {@link Mode} decides the shape of the grid while it still has a cell for every
 *          component. In the default mode, {@link Mode#WRAP_AFTER_COLUMNS}, a new row starts after
 *          every declared number of columns: 7 components in a grid of 2 rows and 3 columns are laid
 *          out in 3 rows. In the mode {@link Mode#SPREAD_OVER_ROWS}, the components are spread over
 *          the declared rows in as many columns as they need, which is what a
 *          {@link java.awt.GridLayout} does.
 *      </li>
 *      <li>
 *          The {@link OverflowGrowth} setting takes over once there are more components than the
 *          declared number of rows times the declared number of columns, and decides on which axis
 *          the grid makes room. By default, {@link OverflowGrowth#ADD_ROWS} adds rows at the bottom
 *          and keeps the declared number of columns. Both modes cap a grid as soon as both declared
 *          numbers are greater than 0, so this setting matters in both of them; only a number of 0
 *          leaves an axis which grows as far as the components need and therefore never overflows.
 *      </li>
 *      <li>
 *          The {@link CollapseEmpty} setting decides which of the rows and columns that no component
 *          occupies are left out of the grid, so that the components share their space. By default,
 *          {@link CollapseEmpty#ROWS_AND_COLUMNS} leaves out both.
 *      </li>
 *  </ul>
 *  A number of rows of 0 means "as many rows as the components need", and a number of columns
 *  of 0 means "as many columns as the components need". So 10 components in a grid of 0 rows and
 *  4 columns are laid out in 3 rows, and 10 components in a grid of 2 rows and 0 columns are laid
 *  out in 5 columns. The two numbers cannot both be 0, and negative numbers are not supported.
 *  <p>
 *  This is how a grid which is declared with 2 rows and 5 columns lays out 3, 8 and 11 components,
 *  with the default {@link OverflowGrowth#ADD_ROWS}:
 *  <table class="striped">
 *    <caption>Rows × columns laid out for a grid declared with 2 rows and 5 columns</caption>
 *    <tr><th>Mode</th><th>CollapseEmpty</th><th>3 components</th><th>8 components</th><th>11 components</th></tr>
 *    <tr><td>{@code WRAP_AFTER_COLUMNS}</td><td>{@code ROWS_AND_COLUMNS}</td><td>1 × 3</td><td>2 × 5</td><td>3 × 5</td></tr>
 *    <tr><td>{@code WRAP_AFTER_COLUMNS}</td><td>{@code COLUMNS}</td>         <td>2 × 3</td><td>2 × 5</td><td>3 × 5</td></tr>
 *    <tr><td>{@code WRAP_AFTER_COLUMNS}</td><td>{@code ROWS}</td>            <td>1 × 5</td><td>2 × 5</td><td>3 × 5</td></tr>
 *    <tr><td>{@code WRAP_AFTER_COLUMNS}</td><td>{@code NONE}</td>            <td>2 × 5</td><td>2 × 5</td><td>3 × 5</td></tr>
 *    <tr><td>{@code SPREAD_OVER_ROWS}</td>  <td>any of them</td>             <td>2 × 2</td><td>2 × 4</td><td>3 × 5</td></tr>
 *  </table>
 *  Only the last column of that table holds more components than the declared 10 cells, so only
 *  there does the {@link OverflowGrowth} setting make a difference, and there it makes the same
 *  difference in both modes: once a grid overflows, the mode has nothing left to say and the growth
 *  policy alone decides the shape. This is how the same grid of 2 rows and 5 columns grows for 11,
 *  13, 20 and 30 components, in either mode and whichever rows and columns it collapses:
 *  <table class="striped">
 *    <caption>Rows × columns laid out for an overflowing grid declared with 2 rows and 5 columns</caption>
 *    <tr><th>OverflowGrowth</th><th>11 components</th><th>13 components</th><th>20 components</th><th>30 components</th></tr>
 *    <tr><td>{@code ADD_ROWS}</td>            <td>3 × 5</td><td>3 × 5</td><td>4 × 5</td> <td>6 × 5</td></tr>
 *    <tr><td>{@code ADD_COLUMNS}</td>         <td>2 × 6</td><td>2 × 7</td><td>2 × 10</td><td>2 × 15</td></tr>
 *    <tr><td>{@code ADD_ROWS_AND_COLUMNS}</td><td>2 × 6</td><td>3 × 6</td><td>3 × 7</td> <td>4 × 9</td></tr>
 *  </table>
 *  <ul>
 *      <li>
 *          The default settings leave no row and no column empty. 3 components share the whole
 *          container in a single row, and as soon as there are enough components to fill a row, the
 *          grid has exactly the declared number of columns. While there are fewer components than
 *          declared columns, every added component makes the cells narrower. Because empty rows are
 *          left out, the declared number of rows only makes a difference while the declared number
 *          of columns is 0.
 *      </li>
 *      <li>
 *          With {@link CollapseEmpty#NONE}, the cells of a grid of 2 rows and 5 columns have the same
 *          size whether the grid holds 1 component or 10, and two such grids of the same size have
 *          cells which line up. With {@link CollapseEmpty#ROWS}, the cells keep their width while
 *          components are added, but the grid only has as many rows as the components fill.
 *      </li>
 *      <li>
 *          In the mode {@link Mode#SPREAD_OVER_ROWS}, an added component can change the width of every
 *          cell. This mode never leaves a column empty, so collapsing empty columns moves no component,
 *          but it can leave rows empty: 2 components in a grid of 3 rows get 1 column and only fill
 *          2 of the 3 rows, and {@link CollapseEmpty#ROWS} leaves out the third one. At a UI scale
 *          factor of 1, a {@code UniformGridLayout} in this mode, with {@link CollapseEmpty#NONE} and
 *          {@link OverflowGrowth#ADD_COLUMNS}, lays out and measures a container exactly like a
 *          {@link java.awt.GridLayout} with the same numbers of rows and columns and the same gaps.
 *      </li>
 *  </ul>
 *  You choose the settings with {@link #setMode(Mode)}, {@link #setCollapseEmpty(CollapseEmpty)}
 *  and {@link #setOverflowGrowth(OverflowGrowth)}.
 *
 *  <h2>The size of the cells</h2>
 *
 *  All cells have the same size. To find their width, the layout manager takes the width
 *  of the container, subtracts the left and right insets and the horizontal gaps between
 *  the columns, divides the rest by the number of columns and rounds the result down.
 *  The few pixels which that rounding leaves over are split in two: half of them, rounded
 *  down, come before the first column, and the rest come after the last column, which
 *  centres the grid inside the insets.
 *  <p>
 *  For example, a container 105 pixels wide without insets, with 3 columns and a horizontal
 *  gap of 5 pixels, has cells which are (105 − 2 × 5) / 3 = 31.67 pixels wide, rounded down
 *  to 31. The cells and the gaps take up 3 × 31 + 2 × 5 = 103 pixels, and of the 2 pixels
 *  left over, 1 comes before the first column, so the first column starts at x = 1.
 *  The height of the cells is found in the same way, from the height of the container,
 *  the top and bottom insets, the vertical gaps and the number of rows.
 *  <p>
 *  A few more details follow from how the cells are filled:
 *  <ul>
 *      <li>
 *          A component which is not visible still has its cell, so hiding a component
 *          leaves an empty cell behind, moves none of the other components, and does not
 *          make a row or column empty enough to be left out.
 *      </li>
 *      <li>
 *          In a container with a right-to-left {@link java.awt.ComponentOrientation}, every
 *          row is filled from right to left, so the first component of a row is in its
 *          rightmost cell. The rows are still filled from the top.
 *      </li>
 *      <li>
 *          The preferred, minimum and maximum sizes of the components play no part in laying
 *          them out. The preferred and minimum sizes only matter for
 *          {@link #preferredLayoutSize(Container)} and {@link #minimumLayoutSize(Container)}.
 *      </li>
 *      <li>
 *          When a container is too small for its insets and gaps, its cells end up with
 *          a width or a height of 0 or less.
 *      </li>
 *  </ul>
 *
 *  <h2>Gaps and the UI scale factor</h2>
 *
 *  The horizontal gap is the space between two neighbouring columns, and the vertical
 *  gap is the space between two neighbouring rows. There is no gap between the outer
 *  cells and the insets of the container, so if you want space around the grid, give
 *  the container a border, for example through a padding in its SwingTree style.
 *  <p>
 *  You declare the gaps in pixels at a UI scale factor of 1. Every time the layout manager
 *  lays out or measures a container, it scales the gaps with {@link UI#scale(int)}, which
 *  rounds to the nearest whole pixel. So a gap of 5 is 10 pixels wide at a UI scale factor
 *  of 2 and 6 pixels wide at a UI scale factor of 1.25, and a change of the scale factor
 *  takes effect the next time the container is laid out. {@link #getHgap()} and
 *  {@link #getVgap()} return the gaps as you declared them.
 */
public final class UniformGridLayout implements LayoutManager {

    /**
     *  Decides how the grid of a {@link UniformGridLayout} is built from the declared number of
     *  rows, the declared number of columns and the number of components in the container.
     *  In both modes, the grid is filled row by row, and a number of rows or columns of 0 means
     *  "as many as the components need". The mode only shapes a grid which still has a cell for
     *  every component; once there are more components than the declared numbers multiply to, the
     *  {@link OverflowGrowth} setting decides the shape instead. The {@link CollapseEmpty} setting
     *  then decides which of the rows and columns that no component occupies are left out.
     *  <p>
     *  This is how each mode builds a grid declared with 2 rows and 5 columns, as rows × columns,
     *  before any empty rows or columns are left out:
     *  <ul>
     *      <li>3 components: {@link #WRAP_AFTER_COLUMNS} 2 × 5, {@link #SPREAD_OVER_ROWS} 2 × 2</li>
     *      <li>8 components: {@link #WRAP_AFTER_COLUMNS} 2 × 5, {@link #SPREAD_OVER_ROWS} 2 × 4</li>
     *      <li>11 components: both modes 3 × 5, because 11 components overflow the declared 10
     *          cells and the default {@link OverflowGrowth#ADD_ROWS} takes the decision</li>
     *  </ul>
     *
     * @see UniformGridLayout#setMode(Mode)
     */
    public enum Mode {
        /**
         *  A new row starts after every declared number of columns, so the grid has the declared
         *  number of columns and at least the declared number of rows. When there are more components
         *  than cells, the {@link OverflowGrowth} setting decides whether the grid grows by rows,
         *  by columns or by both. This is the default mode.
         *  <p>
         *  A grid of 2 rows and 5 columns builds 2 rows of 5 columns for 8 components, and, with the
         *  default {@link OverflowGrowth#ADD_ROWS}, 3 rows of 5 columns for 11 components. While the
         *  grid keeps its declared number of columns, the component at index {@code i} is always in
         *  row {@code i / columns} and column {@code i % columns}, so adding a component never moves
         *  one of the components before it into another row or column.
         *  <p>
         *  If the declared number of columns is 0, the grid gets as many columns as it takes to fit
         *  the components into the declared number of rows, and can never overflow.
         */
        WRAP_AFTER_COLUMNS,
        /**
         *  The components are spread over the declared number of rows in as many columns as they
         *  need, so the declared number of columns does not shape the grid. It still counts the
         *  cells: once there are more components than the declared rows times the declared columns,
         *  the {@link OverflowGrowth} setting decides whether the grid makes room by rows, by
         *  columns or by both, exactly as it does in {@link #WRAP_AFTER_COLUMNS}.
         *  <p>
         *  A grid of 2 rows and 5 columns builds 2 columns for 3 components and 4 columns for 8
         *  components. Its 11th component overflows the declared 10 cells, and from there the growth
         *  policy answers: 6 columns under {@link OverflowGrowth#ADD_COLUMNS}, a third row under the
         *  default {@link OverflowGrowth#ADD_ROWS}. A grid of 0 rows builds its declared number of
         *  columns, and as many rows as its components need.
         *  <p>
         *  {@link OverflowGrowth#ADD_COLUMNS} is the policy which keeps widening the grid the way the
         *  {@link java.awt.GridLayout} of the JDK does, so a {@code UniformGridLayout} in this mode,
         *  with {@link CollapseEmpty#NONE} and that growth policy, can replace a {@code GridLayout}
         *  without changing where any component ends up.
         */
        SPREAD_OVER_ROWS
    }

    /**
     *  Decides which of the rows and columns that no component occupies a {@link UniformGridLayout}
     *  leaves out of its grid. The components share the space which the left out rows and columns
     *  would have taken up, so they get larger cells.
     *  <p>
     *  The grid is filled row by row, so a column is empty when there are fewer components than
     *  columns, and a row is empty when the components do not reach it. A component which is not
     *  visible still occupies its cell. Here is how a grid declared with 2 rows and 5 columns, in the
     *  mode {@link Mode#WRAP_AFTER_COLUMNS}, lays out 3 components, as rows × columns:
     *  {@link #NONE} 2 × 5, {@link #COLUMNS} 2 × 3, {@link #ROWS} 1 × 5 and {@link #ROWS_AND_COLUMNS} 1 × 3.
     *  <p>
     *  In a container without components, a number of rows or columns which is collapsed is 1, and
     *  that single row or column is 0 pixels large, because there is no component to size it after.
     *
     * @see UniformGridLayout#setCollapseEmpty(CollapseEmpty)
     */
    public enum CollapseEmpty {
        /**
         *  Rows and columns are kept, even when no component occupies them. In the mode
         *  {@link Mode#WRAP_AFTER_COLUMNS}, the cells of a grid therefore keep their size while
         *  components are added, until the grid is full.
         */
        NONE,
        /**
         *  The columns which no component occupies are left out, so the grid never has more columns
         *  than components. A grid of 2 rows and 5 columns lays out 3 components in 2 rows of 3 columns,
         *  and keeps its empty second row.
         */
        COLUMNS,
        /**
         *  The rows which no component occupies are left out, so the grid only has as many rows as the
         *  components fill. A grid of 2 rows and 5 columns lays out 3 components in 1 row of 5 columns,
         *  and keeps its 2 empty columns. In the mode {@link Mode#WRAP_AFTER_COLUMNS}, the declared number
         *  of rows then only makes a difference while the declared number of columns is 0.
         */
        ROWS,
        /**
         *  The rows and the columns which no component occupies are left out, so no row and no column
         *  of the grid is ever empty. A grid of 2 rows and 5 columns lays out 3 components in 1 row of
         *  3 columns, and 8 components in 2 rows of 5 columns. This is the default.
         */
        ROWS_AND_COLUMNS
    }

    /**
     *  Decides how the grid of a {@link UniformGridLayout} grows when a container holds more
     *  components than the declared number of rows times the declared number of columns, so that
     *  every component still gets a cell of its own.
     *  <p>
     *  A grid can overflow whenever its declared number of rows and its declared number of columns
     *  are both greater than 0, in either {@link Mode}: both modes then have a fixed number of cells
     *  to run out of. A number of rows or columns of 0 means "as many as the components need", which
     *  leaves that axis free to grow on its own, so such a grid never overflows and this setting
     *  never changes it.
     *  <p>
     *  The {@link Mode} shapes the grid up to that point and this setting shapes it from there on, so
     *  two grids which differ only in their mode are laid out alike once both have overflowed. This is
     *  how a grid declared with 2 rows and 5 columns, which has 10 cells, grows for 13 components, as
     *  rows × columns: {@link #ADD_ROWS} 3 × 5, {@link #ADD_COLUMNS} 2 × 7 and
     *  {@link #ADD_ROWS_AND_COLUMNS} 3 × 6. The grid grows before the {@link CollapseEmpty} setting
     *  leaves out the rows and columns which no component occupies.
     *
     * @see UniformGridLayout#setOverflowGrowth(OverflowGrowth)
     */
    public enum OverflowGrowth {
        /**
         *  Rows are added at the bottom until every component has a cell, and the grid keeps its
         *  declared number of columns. A grid of 2 rows and 5 columns lays out 13 components in
         *  3 rows of 5 columns, and 30 components in 6 rows of 5 columns. This is the default.
         *  <p>
         *  In the mode {@link Mode#WRAP_AFTER_COLUMNS}, this is the only setting under which the
         *  component at index {@code i} stays in row {@code i / columns} and column {@code i % columns}
         *  however many components there are, so it is the only one under which adding a component
         *  never moves one of the components before it into another row or column.
         */
        ADD_ROWS,
        /**
         *  Columns are added at the right until every component has a cell, and the grid keeps its
         *  declared number of rows. A grid of 2 rows and 5 columns lays out 13 components in 2 rows
         *  of 7 columns, and 30 components in 2 rows of 15 columns.
         *  <p>
         *  This is the one policy which keeps making room the way a {@link java.awt.GridLayout} makes
         *  room, so it is the growth policy the mode {@link Mode#SPREAD_OVER_ROWS} needs, alongside
         *  {@link CollapseEmpty#NONE}, to lay out components exactly like one. The mode still decides
         *  the grid below the declared number of cells: with {@link Mode#WRAP_AFTER_COLUMNS} a grid of
         *  2 rows and 5 columns lays out 3 components in 5 columns, and with
         *  {@link Mode#SPREAD_OVER_ROWS} it lays them out in 2.
         */
        ADD_COLUMNS,
        /**
         *  Rows and columns are added one at a time, each time the one which keeps the ratio of rows
         *  to columns closest to the declared ratio, until every component has a cell. The grid
         *  therefore keeps its shape as closely as whole rows and columns allow, and so do its cells:
         *  the container does not change size while the grid grows, so a cell of a grid of {@code r}
         *  rows and {@code c} columns is {@code width / c} by {@code height / r} large, and holding
         *  {@code r / c} at the declared ratio holds the shape of every cell as well.
         *  <p>
         *  A grid of 2 rows and 5 columns lays out 13 components in 3 rows of 6 columns, and 30
         *  components in 4 rows of 9 columns. When the declared numbers scale up evenly, the ratio is
         *  kept exactly and no cell is left over: a grid of 1 row and 4 columns lays out 400
         *  components, 100 times its 4 cells, in exactly 10 rows of 40 columns.
         *  <p>
         *  Growing both numbers can leave more empty cells behind than {@link #ADD_ROWS} or
         *  {@link #ADD_COLUMNS} do, because the last row of the grid is the one which absorbs the
         *  difference: 13 components in 3 rows of 6 columns leave 5 cells of the last row empty.
         */
        ADD_ROWS_AND_COLUMNS
    }

    int hgap;
    int vgap;
    int rows;
    int cols;

    Mode mode = Mode.WRAP_AFTER_COLUMNS;

    CollapseEmpty collapseEmpty = CollapseEmpty.ROWS_AND_COLUMNS;

    OverflowGrowth overflowGrowth = OverflowGrowth.ADD_ROWS;

    /**
     *  Creates a grid layout with a single row, a column for every component and no gaps,
     *  which places the components of the container side by side in cells of equal width.
     *  This is the same as {@code new UniformGridLayout(1, 0, 0, 0)}.
     */
    public UniformGridLayout() {
        this(1, 0, 0, 0);
    }

    /**
     *  Creates a grid layout with the given number of rows and columns and no gaps between the
     *  cells, in the mode {@link Mode#WRAP_AFTER_COLUMNS}, which collapses
     *  {@link CollapseEmpty#ROWS_AND_COLUMNS}. A number of 0 means "as many as the components need",
     *  so {@code new UniformGridLayout(0, 3)} arranges the components in 3 columns and as many rows
     *  as it takes to hold all of them, and arranges 2 components in a single row of 2 columns.
     *
     * @param rows The number of rows, or 0 for as many rows as the components need.
     * @param cols The number of columns, or 0 for as many columns as the components need.
     * @throws IllegalArgumentException If both {@code rows} and {@code cols} are 0.
     */
    public UniformGridLayout(int rows, int cols) {
        this(rows, cols, 0, 0);
    }

    /**
     *  Creates a grid layout with the given number of rows and columns and the given gaps between
     *  neighbouring cells, in the mode {@link Mode#WRAP_AFTER_COLUMNS}, which collapses
     *  {@link CollapseEmpty#ROWS_AND_COLUMNS}. A number of 0 means "as many as the components need",
     *  so {@code new UniformGridLayout(2, 0, 5, 5)} spreads the components over 2 rows, in as many
     *  columns as it takes to hold all of them, with 5 pixels between neighbouring cells, and leaves
     *  out the second row for a single component. The gaps are scaled with {@link UI#scale(int)}
     *  every time the container is laid out or measured.
     *
     * @param rows The number of rows, or 0 for as many rows as the components need.
     * @param cols The number of columns, or 0 for as many columns as the components need.
     * @param hgap The space between two neighbouring columns, in pixels at a UI scale factor of 1.
     * @param vgap The space between two neighbouring rows, in pixels at a UI scale factor of 1.
     * @throws IllegalArgumentException If both {@code rows} and {@code cols} are 0.
     */
    public UniformGridLayout(int rows, int cols, int hgap, int vgap) {
        if ((rows == 0) && (cols == 0)) {
            throw new IllegalArgumentException("rows and cols cannot both be zero");
        }
        this.rows = rows;
        this.cols = cols;
        this.hgap = hgap;
        this.vgap = vgap;
    }

    /**
     *  Creates a grid layout with every one of its settings spelled out: how the grid is built from
     *  the given numbers of rows and columns, how it grows when it holds more components than cells,
     *  which of its empty rows and columns it leaves out, and the gaps between neighbouring cells.
     *  The other constructors leave the three settings at {@link Mode#WRAP_AFTER_COLUMNS},
     *  {@link CollapseEmpty#ROWS_AND_COLUMNS} and {@link OverflowGrowth#ADD_ROWS}.
     *  <p>
     *  For example,
     *  {@code new UniformGridLayout(Mode.WRAP_AFTER_COLUMNS, CollapseEmpty.NONE, OverflowGrowth.ADD_COLUMNS, 2, 5, 5, 5)}
     *  keeps all 10 cells of a grid of 2 rows and 5 columns for 3 components, and widens the grid to
     *  2 rows of 7 columns for 13 components, with 5 pixels between neighbouring cells at a UI scale
     *  factor of 1.
     *
     * @param mode How the grid is built from the numbers of rows and columns.
     * @param collapseEmpty Which of the rows and columns that no component occupies are left out.
     * @param overflowGrowth How the grid grows when it holds more components than cells.
     * @param rows The number of rows, or 0 for as many rows as the components need.
     * @param cols The number of columns, or 0 for as many columns as the components need.
     * @param hgap The space between two neighbouring columns, in pixels at a UI scale factor of 1.
     * @param vgap The space between two neighbouring rows, in pixels at a UI scale factor of 1.
     * @throws IllegalArgumentException If both {@code rows} and {@code cols} are 0.
     * @throws NullPointerException If {@code mode}, {@code collapseEmpty} or {@code overflowGrowth} is {@code null}.
     */
    public UniformGridLayout(
        Mode           mode,
        CollapseEmpty  collapseEmpty,
        OverflowGrowth overflowGrowth,
        int rows,
        int cols,
        int hgap,
        int vgap
    ) {
        this(rows, cols, hgap, vgap);
        this.mode           = Objects.requireNonNull(mode);
        this.collapseEmpty  = Objects.requireNonNull(collapseEmpty);
        this.overflowGrowth = Objects.requireNonNull(overflowGrowth);
    }

    /**
     *  Returns the number of rows you gave this layout, where 0 means "as many rows as the
     *  components need". The container can be laid out in a different number of rows: in the mode
     *  {@link Mode#WRAP_AFTER_COLUMNS}, rows are added when there are more components than cells,
     *  and when empty rows are collapsed, the rows which no component reaches are left out.
     *
     * @return The declared number of rows, which may be 0.
     */
    public int getRows() {
        return rows;
    }

    /**
     *  Sets the number of rows of the grid, where 0 means "as many rows as the components need".
     *  In the mode {@link Mode#WRAP_AFTER_COLUMNS} with empty rows collapsed, which are the default
     *  settings, this number only makes a difference while the number of columns is 0.
     *  <p>
     *  The number of rows and the number of columns cannot both be 0, and this method checks
     *  that against the number of columns this layout has at the moment it is called. So to
     *  switch a layout of 0 rows and 3 columns to 2 rows and 0 columns, call {@code setRows(2)}
     *  first and {@code setColumns(0)} second, and to switch it back, call {@code setColumns(3)}
     *  first and {@code setRows(0)} second.
     *  <p>
     *  This method does not lay out the container again. Call
     *  {@link java.awt.Component#revalidate()} on the container to apply the new number.
     *
     * @param rows The number of rows, or 0 for as many rows as the components need.
     * @throws IllegalArgumentException If {@code rows} is 0 while the number of columns is 0 as well.
     */
    public void setRows(int rows) {
        if ((rows == 0) && (this.cols == 0)) {
            throw new IllegalArgumentException("rows and cols cannot both be zero");
        }
        this.rows = rows;
    }

    /**
     *  Returns the number of columns you gave this layout, where 0 means "as many columns as the
     *  components need". The container can be laid out in a different number of columns: in the mode
     *  {@link Mode#SPREAD_OVER_ROWS}, the number of columns is worked out from the number of rows
     *  whenever that is greater than 0, and when empty columns are collapsed, the grid never has more
     *  columns than components.
     *
     * @return The declared number of columns, which may be 0.
     */
    public int getColumns() {
        return cols;
    }

    /**
     *  Sets the number of columns of the grid, where 0 means "as many columns as the components need".
     *  In the mode {@link Mode#SPREAD_OVER_ROWS}, this number is ignored while the number of rows is
     *  greater than 0, and when empty columns are collapsed, the grid never has more columns than
     *  components.
     *  <p>
     *  The number of rows and the number of columns cannot both be 0, and this method checks
     *  that against the number of rows this layout has at the moment it is called. So to switch
     *  a layout of 2 rows and 0 columns to 0 rows and 3 columns, call {@code setColumns(3)} first
     *  and {@code setRows(0)} second, and to switch it back, call {@code setRows(2)} first and
     *  {@code setColumns(0)} second.
     *  <p>
     *  This method does not lay out the container again. Call
     *  {@link java.awt.Component#revalidate()} on the container to apply the new number.
     *
     * @param cols The number of columns, or 0 for as many columns as the components need.
     * @throws IllegalArgumentException If {@code cols} is 0 while the number of rows is 0 as well.
     */
    public void setColumns(int cols) {
        if ((cols == 0) && (this.rows == 0)) {
            throw new IllegalArgumentException("rows and cols cannot both be zero");
        }
        this.cols = cols;
    }

    /**
     *  Returns the mode which decides how the grid is built from the declared numbers of rows and
     *  columns. See {@link Mode} for what each mode does.
     *
     * @return The mode of this layout, which is {@link Mode#WRAP_AFTER_COLUMNS} unless you changed it.
     */
    public Mode getMode() {
        return mode;
    }

    /**
     *  Sets the mode which decides how the grid is built from the declared numbers of rows and columns,
     *  for as long as it still has a cell for every component:
     *  <ul>
     *      <li>{@link Mode#WRAP_AFTER_COLUMNS}, the default, starts a new row after every declared
     *          number of columns.</li>
     *      <li>{@link Mode#SPREAD_OVER_ROWS} spreads the components over the declared rows in as many
     *          columns as they need, like a {@link java.awt.GridLayout}.</li>
     *  </ul>
     *  A grid of 2 rows and 5 columns builds 2 rows of 5 columns for 8 components in the first mode,
     *  and 2 rows of 4 columns in the second. Once a grid holds more components than its declared
     *  numbers multiply to, the mode no longer decides its shape and
     *  {@link #setOverflowGrowth(OverflowGrowth)} does. The layout manager reads the mode every time it lays out
     *  or measures the container, but this method does not lay out the container again. Call
     *  {@link java.awt.Component#revalidate()} on the container to apply the new mode.
     *
     * @param mode The mode of this layout.
     * @throws NullPointerException If {@code mode} is {@code null}, in which case the mode stays as it was.
     */
    public void setMode(Mode mode) {
        this.mode = Objects.requireNonNull(mode);
    }

    /**
     *  Returns the setting which decides which of the rows and columns that no component occupies
     *  are left out of the grid. See {@link CollapseEmpty} for what each setting does.
     *
     * @return The setting of this layout, which is {@link CollapseEmpty#ROWS_AND_COLUMNS} unless you changed it.
     */
    public CollapseEmpty getCollapseEmpty() {
        return collapseEmpty;
    }

    /**
     *  Sets which of the rows and columns that no component occupies are left out of the grid:
     *  {@link CollapseEmpty#NONE}, {@link CollapseEmpty#COLUMNS}, {@link CollapseEmpty#ROWS}, or
     *  {@link CollapseEmpty#ROWS_AND_COLUMNS}, which is the default. In the mode
     *  {@link Mode#WRAP_AFTER_COLUMNS}, a grid of 2 rows and 5 columns lays out 3 components in
     *  2 × 5, 2 × 3, 1 × 5 and 1 × 3 rows × columns with these settings.
     *  <p>
     *  The layout manager reads the setting every time it lays out or measures the container, but
     *  this method does not lay out the container again. Call {@link java.awt.Component#revalidate()}
     *  on the container to apply the new setting.
     *
     * @param collapseEmpty Which empty rows and columns this layout leaves out.
     * @throws NullPointerException If {@code collapseEmpty} is {@code null}, in which case the setting stays as it was.
     */
    public void setCollapseEmpty(CollapseEmpty collapseEmpty) {
        this.collapseEmpty = Objects.requireNonNull(collapseEmpty);
    }

    /**
     *  Returns the setting which decides how the grid grows when the container holds more components
     *  than cells. See {@link OverflowGrowth} for what each setting does, and for the grids which
     *  can overflow at all.
     *
     * @return The setting of this layout, which is {@link OverflowGrowth#ADD_ROWS} unless you changed it.
     */
    public OverflowGrowth getOverflowGrowth() {
        return overflowGrowth;
    }

    /**
     *  Sets how the grid grows when the container holds more components than the declared number of
     *  rows times the declared number of columns: {@link OverflowGrowth#ADD_ROWS}, which is the default,
     *  {@link OverflowGrowth#ADD_COLUMNS}, or {@link OverflowGrowth#ADD_ROWS_AND_COLUMNS}, which keeps
     *  the ratio of rows to columns as close to the declared ratio as whole rows and columns allow.
     *  A grid of 2 rows and 5 columns lays out 13 components in 3 × 5, 2 × 7 and 3 × 6 rows × columns
     *  with these settings.
     *  <p>
     *  This setting makes a difference in both modes, but only while the declared numbers of rows and
     *  columns are both greater than 0, because a grid with a number of 0 grows that axis as far as the
     *  components need and can therefore never hold more components than it has cells.
     *  <p>
     *  The layout manager reads the setting every time it lays out or measures the container, but
     *  this method does not lay out the container again. Call {@link java.awt.Component#revalidate()}
     *  on the container to apply the new setting.
     *
     * @param overflowGrowth How this layout grows a grid which holds more components than cells.
     * @throws NullPointerException If {@code overflowGrowth} is {@code null}, in which case the setting stays as it was.
     */
    public void setOverflowGrowth(OverflowGrowth overflowGrowth) {
        this.overflowGrowth = Objects.requireNonNull(overflowGrowth);
    }

    private boolean collapsesEmptyColumns() {
        return collapseEmpty == CollapseEmpty.COLUMNS || collapseEmpty == CollapseEmpty.ROWS_AND_COLUMNS;
    }

    private boolean collapsesEmptyRows() {
        return collapseEmpty == CollapseEmpty.ROWS || collapseEmpty == CollapseEmpty.ROWS_AND_COLUMNS;
    }

    private static final int ROW_COUNT    = 0;
    private static final int COLUMN_COUNT = 1;

    private int[] gridFor(int ncomponents) {
        int nrows = rows;
        int ncols = cols;
        if (rows > 0 && cols > 0) {
            if ((long) rows * cols < ncomponents) {
                switch (overflowGrowth) {
                    case ADD_COLUMNS:
                        ncols = (ncomponents + rows - 1) / rows;
                        break;
                    case ADD_ROWS_AND_COLUMNS:
                        while ((long) nrows * ncols < ncomponents) {
                            long withOneMoreRow    = Math.abs((long) (nrows + 1) * cols - (long) rows * ncols) * (ncols + 1);
                            long withOneMoreColumn = Math.abs((long) nrows * cols - (long) rows * (ncols + 1)) * ncols;
                            if (withOneMoreRow <= withOneMoreColumn) {
                                nrows++;
                            } else {
                                ncols++;
                            }
                        }
                        break;
                    default:
                        nrows = (ncomponents + cols - 1) / cols;
                }
            } else if (mode == Mode.SPREAD_OVER_ROWS) {
                ncols = (ncomponents + rows - 1) / rows;
            }
        } else if (rows > 0) {
            ncols = (ncomponents + rows - 1) / rows;
        } else {
            nrows = ncols > 0 ? (ncomponents + ncols - 1) / ncols : 0;
        }
        if (collapsesEmptyColumns()) {
            ncols = Math.max(Math.min(ncols, ncomponents), 1);
        }
        if (collapsesEmptyRows()) {
            nrows = ncols > 0 ? Math.max((ncomponents + ncols - 1) / ncols, 1) : 1;
        }
        int[] grid = new int[2];
        grid[ROW_COUNT]    = nrows;
        grid[COLUMN_COUNT] = ncols;
        return grid;
    }

    /**
     *  Returns the space between two neighbouring columns as you declared it, in pixels at
     *  a UI scale factor of 1. In a laid out container, the space is this number scaled with
     *  {@link UI#scale(int)}.
     *
     * @return The declared horizontal gap.
     */
    public int getHgap() {
        return hgap;
    }

    /**
     *  Sets the space between two neighbouring columns, in pixels at a UI scale factor of 1.
     *  The layout manager scales it with {@link UI#scale(int)} every time it lays out or
     *  measures the container, and puts no gap between the outer columns and the insets of the
     *  container. A negative gap is not rejected, and makes neighbouring cells overlap.
     *  <p>
     *  This method does not lay out the container again. Call
     *  {@link java.awt.Component#revalidate()} on the container to apply the new gap.
     *
     * @param hgap The horizontal gap, in pixels at a UI scale factor of 1.
     */
    public void setHgap(int hgap) {
        this.hgap = hgap;
    }

    /**
     *  Returns the space between two neighbouring rows as you declared it, in pixels at
     *  a UI scale factor of 1. In a laid out container, the space is this number scaled with
     *  {@link UI#scale(int)}.
     *
     * @return The declared vertical gap.
     */
    public int getVgap() {
        return vgap;
    }

    /**
     *  Sets the space between two neighbouring rows, in pixels at a UI scale factor of 1.
     *  The layout manager scales it with {@link UI#scale(int)} every time it lays out or
     *  measures the container, and puts no gap between the outer rows and the insets of the
     *  container. A negative gap is not rejected, and makes neighbouring cells overlap.
     *  <p>
     *  This method does not lay out the container again. Call
     *  {@link java.awt.Component#revalidate()} on the container to apply the new gap.
     *
     * @param vgap The vertical gap, in pixels at a UI scale factor of 1.
     */
    public void setVgap(int vgap) {
        this.vgap = vgap;
    }

    /**
     *  Does nothing. This layout manager keeps no information about individual components:
     *  every time it lays out or measures a container, it asks the container for its components
     *  and places them in the order of their index in the container.
     *
     * @param name The name the component was added with, which this layout manager ignores.
     * @param comp The component which was added to the container.
     */
    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    /**
     *  Does nothing. This layout manager keeps no information about individual components,
     *  so a removed component simply no longer takes a cell the next time the container is laid out.
     *
     * @param comp The component which was removed from the container.
     */
    @Override
    public void removeLayoutComponent(Component comp) {
    }

    /**
     *  Computes the size a container needs to give every component a cell as wide as the widest
     *  preferred width, and as tall as the tallest preferred height, among its components.
     *  <p>
     *  The widest preferred width and the tallest preferred height may belong to two different
     *  components, and components which are not visible count as well. The preferred width of the
     *  container is the cell width times the number of columns, plus the horizontal gaps between
     *  the columns, plus the left and right insets. The preferred height is computed in the same
     *  way from the cell height, the number of rows, the vertical gaps and the top and bottom insets.
     *  The gaps are scaled with {@link UI#scale(int)}.
     *  <p>
     *  For example, 3 components which each prefer 30 × 20 pixels, in a grid of 2 rows and 5 columns
     *  with gaps of 5 pixels, at a UI scale factor of 1 and in a container without insets, prefer
     *  <ul>
     *      <li>3 × 30 + 2 × 5 = 100 by 20 pixels with the default settings, which lay them out in 1 row of 3 columns,</li>
     *      <li>5 × 30 + 4 × 5 = 170 by 2 × 20 + 1 × 5 = 45 pixels with {@link CollapseEmpty#NONE},</li>
     *      <li>100 by 45 pixels with {@link CollapseEmpty#COLUMNS}, and 170 by 20 pixels with {@link CollapseEmpty#ROWS},</li>
     *      <li>2 × 30 + 1 × 5 = 65 by 45 pixels in the mode {@link Mode#SPREAD_OVER_ROWS} with {@link CollapseEmpty#NONE}.</li>
     *  </ul>
     *  The numbers of rows and columns are worked out exactly like {@link #layoutContainer(Container)}
     *  works them out, so a container laid out at its preferred size gives every component a cell of
     *  exactly that width and height.
     *  <p>
     *  In a container without components, a number of rows or columns which is worked out from the
     *  components is 0, and the gaps between 0 cells add up to minus one gap: an empty container with
     *  a grid of 2 rows and 0 columns, a horizontal gap of 5 pixels and {@link CollapseEmpty#NONE}
     *  prefers a width of −5 pixels, exactly like it would with a {@link java.awt.GridLayout}.
     *  A number of rows or columns which is collapsed is 1 instead, so with the default settings, an
     *  empty container prefers exactly the size of its insets.
     *
     * @param parent The container to compute the preferred size for.
     * @return The preferred size of {@code parent}, including its insets.
     */
    @Override
    public Dimension preferredLayoutSize(Container parent) {
      synchronized (parent.getTreeLock()) {
        Insets insets = parent.getInsets();
        int ncomponents = parent.getComponentCount();
        int[] grid = gridFor(ncomponents);
        int nrows = grid[ROW_COUNT];
        int ncols = grid[COLUMN_COUNT];
        int horizontalGap = UI.scale(hgap);
        int verticalGap = UI.scale(vgap);
        int w = 0;
        int h = 0;
        for (int i = 0 ; i < ncomponents ; i++) {
            Component comp = parent.getComponent(i);
            Dimension d = comp.getPreferredSize();
            if (w < d.width) {
                w = d.width;
            }
            if (h < d.height) {
                h = d.height;
            }
        }
        return new Dimension(insets.left + insets.right + ncols*w + (ncols-1)*horizontalGap,
                             insets.top + insets.bottom + nrows*h + (nrows-1)*verticalGap);
      }
    }

    /**
     *  Computes the size a container needs to give every component a cell as wide as the widest
     *  minimum width, and as tall as the tallest minimum height, among its components.
     *  <p>
     *  The widest minimum width and the tallest minimum height may belong to two different
     *  components, and components which are not visible count as well. The minimum width of the
     *  container is the cell width times the number of columns, plus the horizontal gaps between
     *  the columns, plus the left and right insets. The minimum height is computed in the same
     *  way from the cell height, the number of rows, the vertical gaps and the top and bottom insets.
     *  The gaps are scaled with {@link UI#scale(int)}, and the numbers of rows and columns are worked
     *  out exactly like {@link #layoutContainer(Container)} works them out.
     *  <p>
     *  {@link #layoutContainer(Container)} does not enforce this size. A container which is smaller
     *  is laid out all the same, and then at least one of its components gets a cell smaller than
     *  its minimum size.
     *
     * @param parent The container to compute the minimum size for.
     * @return The minimum size of {@code parent}, including its insets.
     */
    @Override
    public Dimension minimumLayoutSize(Container parent) {
      synchronized (parent.getTreeLock()) {
        Insets insets = parent.getInsets();
        int ncomponents = parent.getComponentCount();
        int[] grid = gridFor(ncomponents);
        int nrows = grid[ROW_COUNT];
        int ncols = grid[COLUMN_COUNT];
        int horizontalGap = UI.scale(hgap);
        int verticalGap = UI.scale(vgap);
        int w = 0;
        int h = 0;
        for (int i = 0 ; i < ncomponents ; i++) {
            Component comp = parent.getComponent(i);
            Dimension d = comp.getMinimumSize();
            if (w < d.width) {
                w = d.width;
            }
            if (h < d.height) {
                h = d.height;
            }
        }
        return new Dimension(insets.left + insets.right + ncols*w + (ncols-1)*horizontalGap,
                             insets.top + insets.bottom + nrows*h + (nrows-1)*verticalGap);
      }
    }

    /**
     *  Moves and resizes every component of the container into its cell of the grid.
     *  <p>
     *  The grid has the declared numbers of rows and columns, with these exceptions:
     *  <ul>
     *      <li>A number of rows or columns of 0 is worked out from the number of components.</li>
     *      <li>In the mode {@link Mode#SPREAD_OVER_ROWS}, while the number of rows is greater than 0,
     *          the number of columns is worked out from the number of rows and the number of components.</li>
     *      <li>When there are more components than the declared number of rows times the declared
     *          number of columns, the grid grows on the axis the {@link OverflowGrowth} setting names,
     *          in either mode.</li>
     *      <li>The rows and columns which no component occupies are then left out, as far as the
     *          {@link CollapseEmpty} setting says so.</li>
     *  </ul>
     *  All cells are equally large. Their width is the width inside the insets, minus the horizontal
     *  gaps scaled with {@link UI#scale(int)}, divided by the number of columns and rounded down, and
     *  their height is found in the same way from the rows. The pixels which the rounding leaves over
     *  are split: half of them, rounded down, come before the first column and above the first row.
     *  <p>
     *  Every component, visible or not, is given exactly the bounds of its cell. The cells are filled
     *  row by row from the top, and within a row from left to right, or from right to left in a
     *  container with a right-to-left {@link java.awt.ComponentOrientation}. A container without
     *  components is left untouched.
     *
     * @param parent The container whose components are laid out.
     */
    @Override
    public void layoutContainer(Container parent) {
      synchronized (parent.getTreeLock()) {
        Insets insets = parent.getInsets();
        int ncomponents = parent.getComponentCount();
        boolean ltr = parent.getComponentOrientation().isLeftToRight();

        if (ncomponents == 0) {
            return;
        }
        int[] grid = gridFor(ncomponents);
        int nrows = grid[ROW_COUNT];
        int ncols = grid[COLUMN_COUNT];
        int horizontalGap = UI.scale(hgap);
        int verticalGap = UI.scale(vgap);
        // 4370316. To position components in the center we should:
        // 1. get an amount of extra space within Container
        // 2. incorporate half of that value to the left/top position
        // Note that we use trancating division for widthOnComponent
        // The reminder goes to extraWidthAvailable
        int totalGapsWidth = (ncols - 1) * horizontalGap;
        int widthWOInsets = parent.getWidth() - (insets.left + insets.right);
        int widthOnComponent = (widthWOInsets - totalGapsWidth) / ncols;
        int extraWidthAvailable = (widthWOInsets - (widthOnComponent * ncols + totalGapsWidth)) / 2;

        int totalGapsHeight = (nrows - 1) * verticalGap;
        int heightWOInsets = parent.getHeight() - (insets.top + insets.bottom);
        int heightOnComponent = (heightWOInsets - totalGapsHeight) / nrows;
        int extraHeightAvailable = (heightWOInsets - (heightOnComponent * nrows + totalGapsHeight)) / 2;
        if (ltr) {
            for (int c = 0, x = insets.left + extraWidthAvailable; c < ncols ; c++, x += widthOnComponent + horizontalGap) {
                for (int r = 0, y = insets.top + extraHeightAvailable; r < nrows ; r++, y += heightOnComponent + verticalGap) {
                    int i = r * ncols + c;
                    if (i < ncomponents) {
                        parent.getComponent(i).setBounds(x, y, widthOnComponent, heightOnComponent);
                    }
                }
            }
        } else {
            for (int c = 0, x = (parent.getWidth() - insets.right - widthOnComponent) - extraWidthAvailable; c < ncols ; c++, x -= widthOnComponent + horizontalGap) {
                for (int r = 0, y = insets.top + extraHeightAvailable; r < nrows ; r++, y += heightOnComponent + verticalGap) {
                    int i = r * ncols + c;
                    if (i < ncomponents) {
                        parent.getComponent(i).setBounds(x, y, widthOnComponent, heightOnComponent);
                    }
                }
            }
        }
      }
    }

    /**
     *  Returns the declared settings of this layout manager in a line of text, for example
     *  {@code swingtree.layout.UniformGridLayout[hgap=5,vgap=5,rows=2,cols=5,mode=WRAP_AFTER_COLUMNS,collapseEmpty=ROWS_AND_COLUMNS,overflowGrowth=ADD_ROWS]}.
     *  The gaps in it are the declared gaps, not the scaled ones.
     *
     * @return A text with the class name, the gaps, the numbers of rows and columns, the mode,
     *         which empty rows and columns are left out, and how the grid grows when it overflows.
     */
    @Override
    public String toString() {
        return getClass().getName() + "[hgap=" + hgap + ",vgap=" + vgap +
                                       ",rows=" + rows + ",cols=" + cols +
                                       ",mode=" + mode + ",collapseEmpty=" + collapseEmpty +
                                       ",overflowGrowth=" + overflowGrowth + "]";
    }
}
