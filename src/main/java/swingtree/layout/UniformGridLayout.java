package swingtree.layout;

import swingtree.UI;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.Objects;

/**
 * The {@code UniformGridLayout} class is a layout manager that
 * lays out a container's components in a rectangular grid.
 * The container is divided into equal-sized rectangles,
 * and one component is placed in each rectangle.
 * For example, the following is an applet that lays out six buttons
 * into three rows and two columns:
 *
 * <hr><blockquote>
 * <pre>
 * import java.awt.*;
 * import java.applet.Applet;
 * public class ButtonGrid extends Applet {
 *     public void init() {
 *         setLayout(new UniformGridLayout(3,2));
 *         add(new Button("1"));
 *         add(new Button("2"));
 *         add(new Button("3"));
 *         add(new Button("4"));
 *         add(new Button("5"));
 *         add(new Button("6"));
 *     }
 * }
 * </pre></blockquote><hr>
 * <p>
 * If the container's {@code ComponentOrientation} property is horizontal
 * and left-to-right, the above example produces the buttons 1 then 2 in
 * the first row, 3 then 4 in the second and 5 then 6 in the third.
 * If the container's {@code ComponentOrientation} property is horizontal
 * and right-to-left, every row is mirrored: 2 then 1, 4 then 3, 6 then 5.
 * <p>
 * The {@link Mode} of the layout decides how the grid is built from the
 * number of rows and the number of columns. In the default mode,
 * {@link Mode#WRAP_AFTER_COLUMNS}, a new row starts after every specified
 * number of columns, so if three rows and two columns have been specified
 * and nine components are added to the layout, they will be displayed as
 * five rows of two columns. In the mode {@link Mode#SPREAD_OVER_ROWS}, the
 * components are spread over the specified number of rows, and the number of
 * columns is ignored whenever the number of rows is non-zero, which is the
 * behaviour of the {@link java.awt.GridLayout} of the JDK. A number of rows
 * or columns set to zero means "as many as the components need".
 * <p>
 * The {@link CollapseEmpty} setting of the layout then decides whether the rows
 * and the columns which no component occupies are left out of the grid. By
 * default, both are left out, so a single component takes up the whole
 * container, whatever number of rows and columns have been specified.
 * <p>
 * The horizontal and vertical gaps are scaled by the UI scale factor of
 * SwingTree (see {@link UI#scale(int)}) every time the container is laid
 * out or measured.
 */
public final class UniformGridLayout implements LayoutManager {

    /**
     * Decides how the grid of a {@link UniformGridLayout} is built from the
     * declared number of rows, the declared number of columns and the number
     * of components in the container.
     * <p>
     * In both modes, the grid is filled row by row, and a count of zero means
     * "as many as the components need". Here is how each mode lays out a grid
     * declared with 2 rows and 5 columns, written as rows x columns, before
     * {@link CollapseEmpty} leaves out any empty rows or columns:
     * <ul>
     *     <li>3 components: {@link #WRAP_AFTER_COLUMNS} 2 x 5, {@link #SPREAD_OVER_ROWS} 2 x 2</li>
     *     <li>8 components: {@link #WRAP_AFTER_COLUMNS} 2 x 5, {@link #SPREAD_OVER_ROWS} 2 x 4</li>
     *     <li>11 components: {@link #WRAP_AFTER_COLUMNS} 3 x 5, {@link #SPREAD_OVER_ROWS} 2 x 6</li>
     * </ul>
     *
     * @see UniformGridLayout#setMode(Mode)
     */
    public enum Mode {
        /**
         * A new row starts after every declared number of columns, so the grid has
         * the declared number of columns, and at least the declared number of rows.
         * When there are more components than cells, rows are added.
         * <p>
         * A grid of 2 rows and 5 columns lays out 8 components in 2 rows of 5 columns,
         * and 11 components in 3 rows of 5 columns. The component at index {@code i}
         * is always in row {@code i / columns} and column {@code i % columns}.
         * <p>
         * If the declared number of columns is zero, the grid gets as many columns as
         * it takes to fit the components into the declared number of rows.
         * <p>
         * This is the default mode.
         */
        WRAP_AFTER_COLUMNS,
        /**
         * The components are spread over the declared number of rows, and the declared
         * number of columns is ignored whenever the number of rows is not zero. The grid
         * gets as many columns as it takes to fit all components into those rows.
         * <p>
         * A grid of 2 rows and 5 columns lays out 3 components in 2 columns,
         * 8 components in 4 columns and 11 components in 6 columns.
         * <p>
         * This is how the {@link java.awt.GridLayout} of the JDK lays out its
         * components, so a {@code UniformGridLayout} in this mode, with
         * {@link CollapseEmpty#NONE}, can replace a {@code GridLayout} without
         * changing where any component ends up.
         */
        SPREAD_OVER_ROWS
    }

    /**
     * Decides whether a {@link UniformGridLayout} leaves the rows and the columns
     * which no component occupies out of its grid, so that the components share
     * the space those rows and columns would take up.
     * <p>
     * The grid is filled row by row, so a column stays empty when there are fewer
     * components than columns, and a row stays empty when the components fill fewer
     * rows than the grid has. Here is how a grid declared with 2 rows and 5 columns,
     * in the mode {@link Mode#WRAP_AFTER_COLUMNS}, lays out 3 components, written as
     * rows x columns: {@link #NONE} 2 x 5, {@link #COLUMNS} 2 x 3, {@link #ROWS} 1 x 5,
     * {@link #ROWS_AND_COLUMNS} 1 x 3.
     *
     * @see UniformGridLayout#setCollapseEmpty(CollapseEmpty)
     */
    public enum CollapseEmpty {
        /**
         * Rows and columns are kept even when no component occupies them, so the cells
         * of a grid keep their size while components are added, until the grid is full.
         */
        NONE,
        /**
         * The columns which no component occupies are left out, so the grid never has
         * more columns than components. A grid of 2 rows and 5 columns lays out
         * 3 components in 2 rows of 3 columns.
         */
        COLUMNS,
        /**
         * The rows which no component occupies are left out, so the grid only has as
         * many rows as the components fill. A grid of 2 rows and 5 columns lays out
         * 3 components in 1 row of 5 columns.
         */
        ROWS,
        /**
         * The rows and the columns which no component occupies are left out, so no row
         * and no column of the grid is ever empty. A grid of 2 rows and 5 columns lays out
         * 3 components in 1 row of 3 columns, and 8 components in 2 rows of 5 columns.
         * <p>
         * This is the default.
         */
        ROWS_AND_COLUMNS
    }

    /**
     * This is the horizontal gap (in pixels) which specifies the space
     * between columns.  They can be changed at any time.
     * This should be a non-negative integer.
     *
     * @see #getHgap()
     * @see #setHgap(int)
     */
    int hgap;
    /**
     * This is the vertical gap (in pixels) which specifies the space
     * between rows.  They can be changed at any time.
     * This should be a non negative integer.
     *
     * @see #getVgap()
     * @see #setVgap(int)
     */
    int vgap;
    /**
     * This is the number of rows specified for the grid.  The number
     * of rows can be changed at any time.
     * This should be a non negative integer, where '0' means
     * 'any number' meaning that the number of Rows in that
     * dimension depends on the other dimension.
     *
     * @see #getRows()
     * @see #setRows(int)
     */
    int rows;
    /**
     * This is the number of columns specified for the grid.  The number
     * of columns can be changed at any time.
     * This should be a non negative integer, where '0' means
     * 'any number' meaning that the number of Columns in that
     * dimension depends on the other dimension.
     *
     * @see #getColumns()
     * @see #setColumns(int)
     */
    int cols;

    Mode mode = Mode.WRAP_AFTER_COLUMNS;

    CollapseEmpty collapseEmpty = CollapseEmpty.ROWS_AND_COLUMNS;

    /**
     * Creates a grid layout with a default of one column per component,
     * in a single row.
     */
    public UniformGridLayout() {
        this(1, 0, 0, 0);
    }

    /**
     * Creates a grid layout with the specified number of rows and
     * columns. All components in the layout are given equal size.
     * <p>
     * One, but not both, of {@code rows} and {@code cols} can
     * be zero, which means that any number of objects can be placed in a
     * row or in a column.
     * @param     rows   the rows, with the value zero meaning
     *                   any number of rows.
     * @param     cols   the columns, with the value zero meaning
     *                   any number of columns.
     */
    public UniformGridLayout(int rows, int cols) {
        this(rows, cols, 0, 0);
    }

    /**
     * Creates a grid layout with the specified number of rows and
     * columns. All components in the layout are given equal size.
     * <p>
     * In addition, the horizontal and vertical gaps are set to the
     * specified values. Horizontal gaps are placed between each
     * of the columns. Vertical gaps are placed between each of
     * the rows.
     * <p>
     * One, but not both, of {@code rows} and {@code cols} can
     * be zero, which means that any number of objects can be placed in a
     * row or in a column.
     * <p>
     * All {@code UniformGridLayout} constructors defer to this one.
     * @param     rows   the rows, with the value zero meaning
     *                   any number of rows
     * @param     cols   the columns, with the value zero meaning
     *                   any number of columns
     * @param     hgap   the horizontal gap
     * @param     vgap   the vertical gap
     * @throws   IllegalArgumentException  if the value of both
     *                  {@code rows} and {@code cols} is
     *                  set to zero
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
     * Gets the number of rows in this layout.
     * @return    the number of rows in this layout
     */
    public int getRows() {
        return rows;
    }

    /**
     * Sets the number of rows in this layout to the specified value.
     * @param        rows   the number of rows in this layout
     * @throws    IllegalArgumentException  if the value of both
     *               {@code rows} and {@code cols} is set to zero
     */
    public void setRows(int rows) {
        if ((rows == 0) && (this.cols == 0)) {
            throw new IllegalArgumentException("rows and cols cannot both be zero");
        }
        this.rows = rows;
    }

    /**
     * Gets the number of columns in this layout.
     * @return     the number of columns in this layout
     */
    public int getColumns() {
        return cols;
    }

    /**
     * Sets the number of columns in this layout to the specified value.
     * The number of columns is ignored while the number of rows is
     * non-zero and the mode is {@link Mode#SPREAD_OVER_ROWS}.
     * In that case, the number of columns displayed in the layout is
     * determined by the total number of components and the number of rows.
     * @param        cols   the number of columns in this layout
     * @throws    IllegalArgumentException  if the value of both
     *               {@code rows} and {@code cols} is set to zero
     */
    public void setColumns(int cols) {
        if ((cols == 0) && (this.rows == 0)) {
            throw new IllegalArgumentException("rows and cols cannot both be zero");
        }
        this.cols = cols;
    }

    /**
     * Gets the mode of this layout, which decides how the grid is built
     * from the number of rows and the number of columns.
     * @return the mode of this layout, {@link Mode#WRAP_AFTER_COLUMNS} unless it was changed
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * Sets the mode of this layout, which decides how the grid is built
     * from the number of rows and the number of columns.
     * See {@link Mode} for what each mode does.
     * The new mode takes effect the next time the container is laid out.
     * @param mode the mode of this layout
     * @throws NullPointerException if {@code mode} is {@code null}
     */
    public void setMode(Mode mode) {
        this.mode = Objects.requireNonNull(mode);
    }

    /**
     * Gets which of the rows and columns that no component occupies
     * are left out of the grid.
     * @return the setting of this layout, {@link CollapseEmpty#ROWS_AND_COLUMNS} unless it was changed
     */
    public CollapseEmpty getCollapseEmpty() {
        return collapseEmpty;
    }

    /**
     * Sets which of the rows and columns that no component occupies
     * are left out of the grid.
     * See {@link CollapseEmpty} for what each setting does.
     * The new setting takes effect the next time the container is laid out.
     * @param collapseEmpty the setting of this layout
     * @throws NullPointerException if {@code collapseEmpty} is {@code null}
     */
    public void setCollapseEmpty(CollapseEmpty collapseEmpty) {
        this.collapseEmpty = Objects.requireNonNull(collapseEmpty);
    }

    private boolean collapsesEmptyColumns() {
        return collapseEmpty == CollapseEmpty.COLUMNS || collapseEmpty == CollapseEmpty.ROWS_AND_COLUMNS;
    }

    private boolean collapsesEmptyRows() {
        return collapseEmpty == CollapseEmpty.ROWS || collapseEmpty == CollapseEmpty.ROWS_AND_COLUMNS;
    }

    private int columnsFor(int ncomponents) {
        int ncols = cols;
        if (rows > 0 && (mode == Mode.SPREAD_OVER_ROWS || cols <= 0)) {
            ncols = (ncomponents + rows - 1) / rows;
        }
        if (collapsesEmptyColumns()) {
            return Math.max(Math.min(ncols, ncomponents), 1);
        }
        return ncols;
    }

    private int rowsFor(int ncomponents) {
        int ncols = columnsFor(ncomponents);
        if (collapsesEmptyRows()) {
            return ncols > 0 ? Math.max((ncomponents + ncols - 1) / ncols, 1) : 1;
        }
        if (rows > 0 && (ncols <= 0 || (long) rows * ncols >= ncomponents)) {
            return rows;
        }
        return (ncomponents + ncols - 1) / ncols;
    }

    /**
     * Gets the horizontal gap between components, as it was set,
     * before it is scaled by the UI scale factor.
     * @return       the horizontal gap between components
     */
    public int getHgap() {
        return hgap;
    }

    /**
     * Sets the horizontal gap between components to the specified value,
     * which is scaled by the UI scale factor when the container is laid out.
     * @param        hgap   the horizontal gap between components
     */
    public void setHgap(int hgap) {
        this.hgap = hgap;
    }

    /**
     * Gets the vertical gap between components, as it was set,
     * before it is scaled by the UI scale factor.
     * @return       the vertical gap between components
     */
    public int getVgap() {
        return vgap;
    }

    /**
     * Sets the vertical gap between components to the specified value,
     * which is scaled by the UI scale factor when the container is laid out.
     * @param         vgap  the vertical gap between components
     */
    public void setVgap(int vgap) {
        this.vgap = vgap;
    }

    /**
     * Adds the specified component with the specified name to the layout.
     * @param name the name of the component
     * @param comp the component to be added
     */
    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    /**
     * Removes the specified component from the layout.
     * @param comp the component to be removed
     */
    @Override
    public void removeLayoutComponent(Component comp) {
    }

    /**
     * Determines the preferred size of the container argument using
     * this grid layout.
     * <p>
     * The preferred width of a grid layout is the largest preferred
     * width of all of the components in the container times the number of
     * columns, plus the horizontal padding times the number of columns
     * minus one, plus the left and right insets of the target container.
     * <p>
     * The preferred height of a grid layout is the largest preferred
     * height of all of the components in the container times the number of
     * rows, plus the vertical padding times the number of rows minus one,
     * plus the top and bottom insets of the target container.
     *
     * @param     parent   the container in which to do the layout
     * @return    the preferred dimensions to lay out the
     *                      subcomponents of the specified container
     * @see       #minimumLayoutSize
     * @see       java.awt.Container#getPreferredSize()
     */
    @Override
    public Dimension preferredLayoutSize(Container parent) {
      synchronized (parent.getTreeLock()) {
        Insets insets = parent.getInsets();
        int ncomponents = parent.getComponentCount();
        int nrows = rowsFor(ncomponents);
        int ncols = columnsFor(ncomponents);
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
     * Determines the minimum size of the container argument using this
     * grid layout.
     * <p>
     * The minimum width of a grid layout is the largest minimum width
     * of all of the components in the container times the number of columns,
     * plus the horizontal padding times the number of columns minus one,
     * plus the left and right insets of the target container.
     * <p>
     * The minimum height of a grid layout is the largest minimum height
     * of all of the components in the container times the number of rows,
     * plus the vertical padding times the number of rows minus one, plus
     * the top and bottom insets of the target container.
     *
     * @param       parent   the container in which to do the layout
     * @return      the minimum dimensions needed to lay out the
     *                      subcomponents of the specified container
     * @see         #preferredLayoutSize
     * @see         java.awt.Container#doLayout
     */
    @Override
    public Dimension minimumLayoutSize(Container parent) {
      synchronized (parent.getTreeLock()) {
        Insets insets = parent.getInsets();
        int ncomponents = parent.getComponentCount();
        int nrows = rowsFor(ncomponents);
        int ncols = columnsFor(ncomponents);
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
     * Lays out the specified container using this layout.
     * <p>
     * This method reshapes the components in the specified target
     * container in order to satisfy the constraints of the
     * {@code UniformGridLayout} object.
     * <p>
     * The grid layout manager determines the size of individual
     * components by dividing the free space in the container into
     * equal-sized portions according to the number of rows and columns
     * in the layout. The container's free space equals the container's
     * size minus any insets and any specified horizontal or vertical
     * gap. All components in a grid layout are given the same size.
     *
     * @param      parent   the container in which to do the layout
     * @see        java.awt.Container
     * @see        java.awt.Container#doLayout
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
        int nrows = rowsFor(ncomponents);
        int ncols = columnsFor(ncomponents);
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
     * Returns the string representation of this grid layout's values.
     * @return     a string representation of this grid layout
     */
    @Override
    public String toString() {
        return getClass().getName() + "[hgap=" + hgap + ",vgap=" + vgap +
                                       ",rows=" + rows + ",cols=" + cols +
                                       ",mode=" + mode + ",collapseEmpty=" + collapseEmpty + "]";
    }
}
