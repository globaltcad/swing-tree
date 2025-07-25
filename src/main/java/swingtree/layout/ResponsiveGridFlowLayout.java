
package swingtree.layout;

import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import swingtree.UI;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A flow layout arranges components in a directional flow, much
 * like lines of text in a paragraph.
 */
public final class ResponsiveGridFlowLayout implements LayoutManager2 {

    private static final int NUMBER_OF_COLUMNS = 12;
    private static final Logger log = LoggerFactory.getLogger(ResponsiveGridFlowLayout.class);

    private UI.HorizontalAlignment _alignmentCode;
    private int                    _horizontalGapSize;
    private int                    _verticalGapSize;
    private boolean                _alignOnBaseline;

    /**
     * Constructs a new {@code FlowLayout} with a centered alignment and a
     * default 5-unit horizontal and vertical gap.
     */
    public ResponsiveGridFlowLayout() {
        this(UI.HorizontalAlignment.CENTER, 5, 5);
    }

    /**
     * Constructs a new {@code FlowLayout} with the specified
     * alignment and a default 5-unit horizontal and vertical gap.
     * The value of the alignment argument must be one of
     * {@code FlowLayout.LEFT}, {@code FlowLayout.RIGHT},
     * {@code FlowLayout.CENTER}, {@code FlowLayout.LEADING},
     * or {@code FlowLayout.TRAILING}.
     *
     * @param align the alignment value
     */
    public ResponsiveGridFlowLayout(UI.HorizontalAlignment align) {
        this(align, 5, 5);
    }

    /**
     * Creates a new flow layout manager with the indicated alignment
     * and the indicated horizontal and vertical gaps.
     * <p>
     * The value of the alignment argument must be one of
     * {@link UI.HorizontalAlignment#LEFT}, {@link UI.HorizontalAlignment#RIGHT},
     * {@link UI.HorizontalAlignment#CENTER}, {@link UI.HorizontalAlignment#LEADING},
     * or {@link UI.HorizontalAlignment#TRAILING}.
     *
     * @param align the alignment value
     * @param horizontalGapSize  the horizontal gap between components
     *              and between the components and the
     *              borders of the {@code Container}
     * @param verticalGapSize  the vertical gap between components
     *              and between the components and the
     *              borders of the {@code Container}
     */
    public ResponsiveGridFlowLayout(
        UI.HorizontalAlignment align,
        int horizontalGapSize,
        int verticalGapSize
    ) {
        _alignmentCode     = align;
        _horizontalGapSize = horizontalGapSize;
        _verticalGapSize   = verticalGapSize;
    }

    /**
     * Gets the alignment for this layout.
     * Possible values are {@link UI.HorizontalAlignment#LEFT},
     * {@link UI.HorizontalAlignment#RIGHT}, {@link UI.HorizontalAlignment#CENTER},
     * {@link UI.HorizontalAlignment#LEADING},
     * or {@link UI.HorizontalAlignment#TRAILING}.
     *
     * @return the alignment value for this layout
     * @see #setAlignment
     */
    public UI.HorizontalAlignment getAlignment() {
        return _alignmentCode;
    }

    /**
     * Sets the alignment for this layout.
     * Possible values are
     * <ul>
     * <li>{@link UI.HorizontalAlignment#LEFT}
     * <li>{@link UI.HorizontalAlignment#RIGHT}
     * <li>{@link UI.HorizontalAlignment#CENTER}
     * <li>{@link UI.HorizontalAlignment#LEADING}
     * <li>{@link UI.HorizontalAlignment#TRAILING}
     * </ul>
     *
     * @param align one of the alignment values shown above
     * @see #getAlignment()
     */
    public void setAlignment(UI.HorizontalAlignment align) {
        _alignmentCode = align;
    }

    /**
     * Gets the horizontal gap between components
     * and between the components and the borders
     * of the {@code Container}
     *
     * @return the horizontal gap between components
     * and between the components and the borders
     * of the {@code Container}
     * @see ResponsiveGridFlowLayout#setHorizontalGapSize(int)
     */
    public int horizontalGapSize() {
        return UI.scale(_horizontalGapSize);
    }

    /**
     * Sets the horizontal gap between components and
     * between the components and the borders of the
     * {@code Container}.
     *
     * @param size the horizontal gap between components
     *             and between the components and the borders
     *             of the {@code Container}
     * @see ResponsiveGridFlowLayout#horizontalGapSize()
     */
    public void setHorizontalGapSize(int size) {
        _horizontalGapSize = size;
    }

    /**
     * Gets the vertical gap between components and
     * between the components and the borders of the
     * {@code Container}.
     *
     * @return the vertical gap between components
     * and between the components and the borders
     * of the {@code Container}
     * @see ResponsiveGridFlowLayout#setVerticalGapSize(int)
     */
    public int verticalGapSize() {
        return UI.scale(_verticalGapSize);
    }

    /**
     * Sets the vertical gap between components and between
     * the components and the borders of the {@code Container}.
     *
     * @param size the vertical gap between components
     *             and between the components and the borders
     *             of the {@code Container}
     * @see ResponsiveGridFlowLayout#verticalGapSize()
     */
    public void setVerticalGapSize(int size) {
        _verticalGapSize = size;
    }

    /**
     * Sets whether or not components should be vertically aligned along their
     * baseline.  Components that do not have a baseline will be centered.
     * The default is false.
     *
     * @param alignOnBaseline whether or not components should be
     *                        vertically aligned on their baseline
     */
    public void setAlignOnBaseline(boolean alignOnBaseline) {
        this._alignOnBaseline = alignOnBaseline;
    }

    /**
     * Returns true if components are to be vertically aligned along
     * their baseline.  The default is false.
     *
     * @return true if components are to be vertically aligned along
     * their baseline
     */
    public boolean getAlignOnBaseline() {
        return _alignOnBaseline;
    }

    /**
     * Adds the specified component to the layout.
     * Not used by this class.
     *
     * @param name the name of the component
     * @param comp the component to be added
     */
    @Override
    public void addLayoutComponent( String name, Component comp ) {
    }

    /**
     * Removes the specified component from the layout.
     * Not used by this class.
     *
     * @param comp the component to remove
     * @see java.awt.Container#removeAll
     */
    @Override
    public void removeLayoutComponent( Component comp ) {
    }

    /**
     * Returns the preferred dimensions for this layout given the
     * <i>visible</i> components in the specified target container.
     *
     * @param target the container that needs to be laid out
     * @return the preferred dimensions to lay out the
     * subcomponents of the specified container
     * @see java.awt.Container
     * @see #minimumLayoutSize
     * @see java.awt.Container#getPreferredSize
     */
    @Override
    public Dimension preferredLayoutSize( Container target ) {
        synchronized (target.getTreeLock()) {
            Dimension dim = new Dimension(0, 0);
            int nmembers = target.getComponentCount();
            boolean firstVisibleComponent = true;
            boolean useBaseline = getAlignOnBaseline();
            int maxAscent = 0;
            int maxDescent = 0;
            int hgap = UI.scale(_horizontalGapSize);
            int vgap = UI.scale(_verticalGapSize);

            Bounds bounds = Bounds.of(Position.of(0,0), recursionSafePreferredSizeOf(target));
            for (int i = 0; i < nmembers; i++) {
                Component m = target.getComponent(i);
                if (m.isVisible()) {
                    Bounds childBounds = Bounds.of(Position.of(m.getLocation()), Size.of(m.getSize()).plus(hgap, vgap));
                    bounds = bounds.merge(childBounds);
                    Dimension d = m.getSize();
                    dim.height = Math.max(dim.height, d.height);
                    if (firstVisibleComponent) {
                        firstVisibleComponent = false;
                    } else {
                        dim.width += hgap;
                    }
                    dim.width += d.width;
                    if (useBaseline) {
                        int baseline = m.getBaseline(d.width, d.height);
                        if (baseline >= 0) {
                            maxAscent = Math.max(maxAscent, baseline);
                            maxDescent = Math.max(maxDescent, d.height - baseline);
                        }
                    }
                }
            }
            if (useBaseline) {
                dim.height = Math.max(maxAscent + maxDescent, dim.height);
            }
            Insets insets = target.getInsets();
            dim.width  = Math.max( dim.width  + hgap * 2, bounds.size().width().orElse(0f).intValue() );
            dim.height = Math.max( dim.height + vgap * 2, bounds.size().height().orElse(0f).intValue() );
            dim.width  += insets.left + insets.right;
            dim.height += insets.top  + insets.bottom;
            return dim;
        }
    }

    private static Size recursionSafePreferredSizeOf( Container target ) {
        if ( target.isPreferredSizeSet() )
            return Size.of(target.getPreferredSize());
        else if ( target.isMaximumSizeSet() )
            return Size.of(target.getMaximumSize()); // Good enough fall back 1.
        else if  ( target.isMinimumSizeSet() )
            return Size.of(target.getMinimumSize()); // Good enough fall back 2.
        else
            return Size.of(target.getSize()); // Good enough fallback 3.
    }

    /**
     * Returns the minimum dimensions needed to layout the <i>visible</i>
     * components contained in the specified target container.
     *
     * @param target the container that needs to be laid out
     * @return the minimum dimensions to lay out the
     * subcomponents of the specified container
     * @see #preferredLayoutSize
     * @see java.awt.Container
     * @see java.awt.Container#doLayout
     */
    @Override
    public Dimension minimumLayoutSize( Container target ) {
        synchronized (target.getTreeLock()) {
            boolean useBaseline = getAlignOnBaseline();
            Dimension dim = new Dimension(0, 0);
            int nmembers = target.getComponentCount();
            int maxAscent = 0;
            int maxDescent = 0;
            boolean firstVisibleComponent = true;
            int hgap = UI.scale(_horizontalGapSize);
            int vgap = UI.scale(_verticalGapSize);

            for (int i = 0; i < nmembers; i++) {
                Component m = target.getComponent(i);
                if (m.isVisible()) {
                    Dimension d = m.getMinimumSize();
                    dim.height = Math.max(dim.height, d.height);
                    if (firstVisibleComponent) {
                        firstVisibleComponent = false;
                    } else {
                        dim.width += hgap;
                    }
                    dim.width += d.width;
                    if (useBaseline) {
                        int baseline = m.getBaseline(d.width, d.height);
                        if (baseline >= 0) {
                            maxAscent = Math.max(maxAscent, baseline);
                            maxDescent = Math.max(maxDescent,
                                    dim.height - baseline);
                        }
                    }
                }
            }

            if (useBaseline) {
                dim.height = Math.max(maxAscent + maxDescent, dim.height);
            }

            Insets insets = target.getInsets();
            dim.width += insets.left + insets.right + hgap * 2;
            dim.height += insets.top + insets.bottom + vgap * 2;
            return dim;
        }
    }

    /**
     * Centers the elements in the specified row, if there is any slack.
     *
     * @param target      the component which needs to be moved
     * @param cells       an array of cells, one for each component of the target
     * @param x           the x coordinate
     * @param y           the y coordinate
     * @param width       the width dimensions
     * @param height      the height dimensions
     * @param rowStart    the beginning of the row
     * @param rowEnd      the ending of the row
     * @param useBaseline Whether or not to align on baseline.
     * @param ascent      Ascent for the components. This is only valid if
     *                    useBaseline is true.
     * @param descent     Ascent for the components. This is only valid if
     *                    useBaseline is true.
     * @return actual row height
     */
    private int moveComponents(
            Container target, Cell[] cells,
            int x, int y, int width, int height,
            int rowStart, int rowEnd, boolean ltr,
            boolean useBaseline, @Nullable int[] ascent,
            @Nullable int[] descent
    ) {
        int hgap = UI.scale(_horizontalGapSize);
        switch (_alignmentCode) {
            case LEFT:
                x += ltr ? 0 : width;
                break;
            case CENTER:
                x += width / 2;
                break;
            case RIGHT:
                x += ltr ? width : 0;
                break;
            case LEADING:
                break;
            case TRAILING:
                x += width;
                break;
            case UNDEFINED:
                break; // Should happen?
        }
        int maxAscent = 0;
        int nonbaselineHeight = 0;
        int baselineOffset = 0;
        if (useBaseline) {
            Objects.requireNonNull(ascent);
            Objects.requireNonNull(descent);
            int maxDescent = 0;
            for (int i = rowStart; i < rowEnd; i++) {
                Component m = target.getComponent(i);
                if (m.isVisible()) {
                    if (ascent[i] >= 0) {
                        maxAscent = Math.max(maxAscent, ascent[i]);
                        maxDescent = Math.max(maxDescent, descent[i]);
                    } else {
                        nonbaselineHeight = Math.max(m.getHeight(),
                                nonbaselineHeight);
                    }
                }
            }
            height = Math.max(maxAscent + maxDescent, nonbaselineHeight);
            baselineOffset = (height - maxAscent - maxDescent) / 2;
        }
        for (int i = rowStart; i < rowEnd; i++) {
            Component m = target.getComponent(i);
            if (m.isVisible()) {
                Optional<FlowCellConf> optionalFlowCellConf = cells[i].flowCell();
                boolean fillHeight = optionalFlowCellConf.map(FlowCellConf::fill).orElse(false);
                UI.VerticalAlignment verticalAlignment = optionalFlowCellConf.map(FlowCellConf::verticalAlignment).orElse(UI.VerticalAlignment.CENTER);
                int cy;
                if (ascent != null && useBaseline && ascent[i] >= 0) {
                    cy = y + baselineOffset + maxAscent - ascent[i];
                } else {
                    if (fillHeight) {
                        cy = y;
                    } else {
                        if ( verticalAlignment == UI.VerticalAlignment.TOP )
                            cy = y;
                        else if ( verticalAlignment == UI.VerticalAlignment.BOTTOM )
                            cy = y + height - m.getHeight();
                        else // centered:
                            cy = y + (height - m.getHeight()) / 2;
                    }
                }
                if (ltr) {
                    m.setLocation(x, cy);
                } else {
                    m.setLocation(target.getWidth() - x - m.getWidth(), cy);
                }
                x += m.getWidth() + hgap;

                if ( fillHeight ) {
                    m.setSize(m.getWidth(), height);
                }
            }
        }
        return height;
    }

    /**
     * Lays out the container. This method lets each
     * <i>visible</i> component take
     * its preferred size by reshaping the components in the
     * target container in order to satisfy the alignment of
     * this layout manager.
     *
     * @param target the specified component being laid out
     * @see Container
     * @see java.awt.Container#doLayout
     */
    @Override
    public void layoutContainer(Container target) {
        synchronized (target.getTreeLock()) {
            final int hgap = UI.scale(_horizontalGapSize);
            final int vgap = UI.scale(_verticalGapSize);
            final Insets insets = target.getInsets();
            final int maxwidth = target.getWidth() - (insets.left + insets.right + hgap * 2);
            final int generalMaxWidth = target.getPreferredSize().width - (insets.left + insets.right + hgap * 2);
            final int nmembers = target.getComponentCount();
            int x = 0, y = insets.top + vgap;
            int rowh = 0, start = 0;

            Cell[] cells = _createCells(target, nmembers, maxwidth, generalMaxWidth);

            boolean ltr = target.getComponentOrientation().isLeftToRight();
            boolean useBaseline = getAlignOnBaseline();
            int[] ascent = null;
            int[] descent = null;

            if (useBaseline) {
                ascent = new int[nmembers];
                descent = new int[nmembers];
            }

            for (int i = 0; i < nmembers; i++) {
                Component m = cells[i].component();
                if (m.isVisible()) {
                    Dimension d = m.getPreferredSize();
                    try {
                        d = _dimensionsFromCellConf(cells[i], maxwidth).orElse(d);
                    } catch (Exception e) {
                        log.error("Error applying cell configuration", e);
                    }
                    m.setSize(d.width, d.height);

                    if (useBaseline ) {
                        Objects.requireNonNull(ascent);
                        Objects.requireNonNull(descent);
                        int baseline = m.getBaseline(d.width, d.height);
                        if (baseline >= 0) {
                            ascent[i] = baseline;
                            descent[i] = d.height - baseline;
                        } else {
                            ascent[i] = -1;
                        }
                    }
                    if ((x == 0) || ((x + d.width) <= maxwidth)) {
                        if (x > 0) {
                            x += hgap;
                        }
                        x += d.width;
                        rowh = Math.max(rowh, d.height);
                    } else {
                        rowh = moveComponents(
                                target, cells,
                                insets.left + hgap, y,
                                maxwidth - x, rowh, start, i, ltr,
                                useBaseline, ascent, descent
                        );
                        x = d.width;
                        y += vgap + rowh;
                        rowh = d.height;
                        start = i;
                    }
                }
            }
            moveComponents(
                    target, cells,
                    insets.left + hgap, y, maxwidth - x, rowh,
                    start, nmembers, ltr, useBaseline, ascent, descent
            );
        }
    }

    private Cell[] _createCells(
            Container target,
            int nmembers,
            int maxwidth,
            int generalMaxWidth
    ) {
        Cell[] cells = new Cell[nmembers];
        AtomicInteger componentsInRow = new AtomicInteger(0);
        double currentRowSize = 0;
        for (int i = 0; i < nmembers; i++) {
            Component m = target.getComponent(i);
            Optional<Cell> optionalCell = Optional.empty();
            double rowSizeIncrease = 0;
            if (m instanceof JComponent) {
                JComponent jc = (JComponent) m;
                AddConstraint addConstraint = (AddConstraint) jc.getClientProperty(AddConstraint.class);
                if (addConstraint instanceof FlowCell) {
                    FlowCell cell = (FlowCell) addConstraint;
                    optionalCell = cellFromCellConf(target, cell, jc, componentsInRow, maxwidth, generalMaxWidth);
                    rowSizeIncrease += optionalCell.flatMap(Cell::autoSpan)
                                                    .map(FlowCellSpanPolicy::cellsToFill)
                                                    .orElse(0);
                }
            }
            if ( !optionalCell.isPresent() ) {
                double prefComponentWidth = m.getPreferredSize().getWidth();
                if ( maxwidth > 0 && prefComponentWidth > 0 ) {
                    rowSizeIncrease += NUMBER_OF_COLUMNS * prefComponentWidth / maxwidth;
                }
            }

            cells[i] = optionalCell.orElse(new Cell(m, componentsInRow, null, null));

            double newRowSize = currentRowSize + rowSizeIncrease;
            if ( newRowSize < NUMBER_OF_COLUMNS ) {
                // Still room in the row for new components...
                componentsInRow.set(componentsInRow.get() + 1);
                currentRowSize += rowSizeIncrease;
            } else if ( Math.round(newRowSize) == NUMBER_OF_COLUMNS ) {
                // We have a new row with no leftovers.
                componentsInRow.set(componentsInRow.get() + 1);
                componentsInRow = new AtomicInteger(0);
                currentRowSize = 0;
            } else if ( newRowSize > NUMBER_OF_COLUMNS ) {
                // The row does not fit the new component. We need to start a new row.
                componentsInRow = new AtomicInteger(1);
                cells[i].setNumberOfComponents(componentsInRow);
                currentRowSize = rowSizeIncrease; // We have a new row with the current component.
            }
        }
        return cells;
    }

    /**
     * Returns a string representation of this {@code FlowLayout}
     * object and its values.
     *
     * @return a string representation of this layout
     */
    @Override
    public String toString() {
        String str = "";
        int hgap = UI.scale(_horizontalGapSize);
        int vgap = UI.scale(_verticalGapSize);
        switch (_alignmentCode) {
            case LEFT:
                str = ",align=left";
                break;
            case CENTER:
                str = ",align=center";
                break;
            case RIGHT:
                str = ",align=right";
                break;
            case LEADING:
                str = ",align=leading";
                break;
            case TRAILING:
                str = ",align=trailing";
                break;
            case UNDEFINED:
                str = ",align=?";
                break;
        }
        return getClass().getName() + "[horizontalGap=" + hgap + ",verticalGap=" + vgap + str + "]";
    }

    @Override
    public void addLayoutComponent(Component comp, Object constraints) {
        if (constraints instanceof AddConstraint) {
            if (comp instanceof JComponent) {
                JComponent jc = (JComponent) comp;
                jc.putClientProperty(AddConstraint.class, constraints);
            }
        }
    }

    @Override
    public Dimension maximumLayoutSize(Container target) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    @Override
    public float getLayoutAlignmentX(Container target) {
        return 0;
    }

    @Override
    public float getLayoutAlignmentY(Container target) {
        return 0;
    }

    @Override
    public void invalidateLayout(Container target) {

    }

    public Optional<Cell> cellFromCellConf(
            Component parent,
            FlowCell flowCell,
            Component child,
            AtomicInteger componentCounter,
            int maxWidth,
            int generalMaxWidth
    ) {
        if ( maxWidth <= 0 ) {
            return Optional.empty();
        }
        // How much preferred width the parent actually fills:
        ParentSizeClass currentParentSizeCategory = ParentSizeClass.of(maxWidth, generalMaxWidth);

        boolean shouldFillHeight = false;
        LayoutManager childLayout = ( child instanceof JComponent ) ? ((JComponent) child).getLayout() : null;
        if ( childLayout instanceof MigLayout ) {
            Object layoutConstraints = ((MigLayout) childLayout).getLayoutConstraints();
            // If the child has the "fill" or "filly" constraint, we should fill the height.
            if ( layoutConstraints instanceof String ) {
                String constraints = (String) layoutConstraints;
                shouldFillHeight = constraints.contains("fill") || constraints.contains("filly");
            } else if ( layoutConstraints instanceof LC ) {
                LC lc = (LC) layoutConstraints;
                shouldFillHeight = lc.isFillY();
            }
        }
        Size parentSize = Size.of(parent.getWidth(), parent.getHeight());
        FlowCellConf cellConf = flowCell.fetchConfig(NUMBER_OF_COLUMNS, parentSize, currentParentSizeCategory, shouldFillHeight);
        Optional<FlowCellSpanPolicy> autoSpan = _findNextBestAutoSpan(cellConf, currentParentSizeCategory);
        return autoSpan.map(autoCellSpanPolicy -> new Cell(child, componentCounter, autoCellSpanPolicy, cellConf));
    }

    private Optional<Dimension> _dimensionsFromCellConf( Cell cell, int maxWidth ) {

        if ( maxWidth <= 0 ) {
            return Optional.empty();
        }

        Optional<FlowCellSpanPolicy> autoSpan = cell.autoSpan();
        if (!autoSpan.isPresent()) {
            return Optional.empty();
        }

        int cellsToFill = autoSpan.get().cellsToFill();
        int unusableSpace = ((cell.numberOfComponentsInRow()-1) * UI.scale(_horizontalGapSize));
        int width = ((maxWidth - unusableSpace) * cellsToFill) / NUMBER_OF_COLUMNS;
        Dimension newSize = new Dimension(width, cell.component().getPreferredSize().height);
        return Optional.of(newSize);
    }

    private static Optional<FlowCellSpanPolicy> _findNextBestAutoSpan( FlowCellConf cell, ParentSizeClass targetSize ) {
        Optional<FlowCellSpanPolicy> autoSpan = _find(targetSize.ordinal(), cell);
        if ( autoSpan.isPresent() )
            return autoSpan;

        // We did not find the exact match. Let's try to find the closest match.

        int numberOfSizeClasses = ParentSizeClass.values().length;
        int targetOrdinal = targetSize.ordinal();
        /*
            We want to find the enum value which is closed to the target ordinal.
         */
        int sign = ( targetSize.ordinal() > numberOfSizeClasses / 2 ? 1 : -1 );
        for ( int offset = 1; offset < numberOfSizeClasses; offset++ ) {
            sign = -sign;
            autoSpan = _find(targetOrdinal + offset * sign, cell);
            if ( autoSpan.isPresent() )
                return autoSpan;

            sign = -sign;
            autoSpan = _find(targetOrdinal + offset * sign, cell);
            if ( autoSpan.isPresent() )
                return autoSpan;
        }
        return Optional.empty();
    }

    private static Optional<FlowCellSpanPolicy> _find( int ordinal, FlowCellConf cell ) {
        if ( ordinal < 0 || ordinal >= ParentSizeClass.values().length ) {
            return Optional.empty();
        }
        ParentSizeClass targetSize = ParentSizeClass.values()[ordinal];
        for ( FlowCellSpanPolicy autoSpan : cell.autoSpans() ) {
            if ( autoSpan.parentSize() == targetSize ) {
                return Optional.of(autoSpan);
            }
        }
        return Optional.empty();
    }

    private static final class Cell {

        private final Component component;
        private final @Nullable FlowCellSpanPolicy autoSpan;
        private final @Nullable FlowCellConf cellConf;

        private AtomicInteger numberOfComponents;


        Cell(
                Component component,
                AtomicInteger componentCounter,
                @Nullable FlowCellSpanPolicy autoSpan,
                @Nullable FlowCellConf cellConf
        ) {
            this.component          = component;
            this.numberOfComponents = componentCounter;
            this.autoSpan           = autoSpan;
            this.cellConf           = cellConf;
        }

        public Component component() {
            return component;
        }

        public Optional<FlowCellSpanPolicy> autoSpan() {
            return Optional.ofNullable(autoSpan);
        }

        public Optional<FlowCellConf> flowCell() {
            return Optional.ofNullable(cellConf);
        }

        public int numberOfComponentsInRow() {
            return numberOfComponents.get();
        }

        public void setNumberOfComponents(AtomicInteger numberOfComponents) {
            this.numberOfComponents = numberOfComponents;
        }
    }

}
