package swingtree.layout;

import com.google.errorprone.annotations.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import swingtree.SwingTree;
import swingtree.UI;
import swingtree.UIForAnySwing;
import swingtree.api.Configurator;

import java.util.Arrays;
import java.util.Objects;

/**
 *  An immutable configuration object used to define how a {@link FlowCell} should
 *  place its associated component within a {@link ResponsiveGridFlowLayout}.<br>
 *  Instances of this are passed to the {@link swingtree.api.Configurator} of a {@link FlowCell}
 *  so that you can dynamically assign the number of cells a component should span,
 *  based on the size category of the parent container.<br>
 *  <br>
 *  Use {@link UI#AUTO_SPAN(Configurator)} to create a {@link FlowCell} from a {@link Configurator}
 *  and pass it to the {@link swingtree.UIForAnySwing#add(AddConstraint, UIForAnySwing[])}
 *  of a SwingTree UI declaration.<br>
 *  Here an example demonstrating how this might be used:
 *  <pre>{@code
 *    UI.panel().withPrefSize(400, 300)
 *    .withFlowLayout()
 *    .add(UI.AUTO_SPAN( it->it.small(12).medium(6).large(8) ),
 *         html("A red cell").withStyle(it->it
 *             .backgroundColor(UI.Color.RED)
 *         )
 *    )
 *    .add(UI.AUTO_SPAN( it->it.small(12).medium(6).large(4) ),
 *         html("a green cell").withStyle(it->it
 *             .backgroundColor(Color.GREEN)
 *         )
 *    )
 *  }</pre>
 *  In the above example, the {@code it} parameter of the {@code Configurator} is an instance of this class.
 *  The {@link Configurator} is called every time the {@link ResponsiveGridFlowLayout} updates the layout
 *  of the parent container, so that the number of cells a component spans can be adjusted dynamically.<br>
 *  The {@link UIForAnySwing#withFlowLayout()} creates a {@link ResponsiveGridFlowLayout} and attaches it to the panel.<br>
 *  <p><b>
 *      Note that the {@link FlowCell} configuration may only take effect if the parent
 *      container has a {@link ResponsiveGridFlowLayout} as a {@link java.awt.LayoutManager} installed.
 *  </b>
 *  <br><br>
 *  <p>
 *  Besides configuring the number of cells to span, you can also
 *  define how the cell should be filled vertically by the component
 *  and how the component should be aligned vertically within the cell.<br>
 *  Use the {@link #fill(boolean)} method to set the fill flag to {@code true}
 *  if you want the component to fill the cell vertically.<br>
 *  Use the {@link #align(UI.VerticalAlignment)} method to set the vertical alignment
 *  of the component within the cell to either {@link UI.VerticalAlignment#TOP},
 *  {@link UI.VerticalAlignment#CENTER}, or {@link UI.VerticalAlignment#BOTTOM}.
 */
@Immutable
public final class FlowCellConf
{
    private static final Logger log = LoggerFactory.getLogger(FlowCellConf.class);
    private final int                    _maxCellsToFill;
    private final Size                   _parentSize;
    private final ParentSizeClass        _parentSizeCategory;
    @SuppressWarnings("Immutable")
    private final FlowCellSpanPolicy[]   _autoSpans;
    private final boolean                _fill;
    private final UI.VerticalAlignment   _verticalAlignment;


    FlowCellConf(
            int                  maxCellsToFill,
            Size                 parentSize,
            ParentSizeClass      parentSizeCategory,
            FlowCellSpanPolicy[] autoSpans,
            boolean              fill,
            UI.VerticalAlignment alignVertically
    ) {
        _maxCellsToFill     = maxCellsToFill;
        _parentSize         = Objects.requireNonNull(parentSize);
        _parentSizeCategory = Objects.requireNonNull(parentSizeCategory);
        _autoSpans          = Objects.requireNonNull(autoSpans.clone());
        _fill               = fill;
        _verticalAlignment = alignVertically;
    }

    FlowCellSpanPolicy[] autoSpans() {
        return _autoSpans.clone();
    }

    /**
     *  Returns the maximum number of cells that a component can span
     *  in a grid layout managed by a {@link ResponsiveGridFlowLayout}.<br>
     *  The default value is 12, which is the maximum number of cells in a row
     *  of the {@link ResponsiveGridFlowLayout}.
     *  You may use this value to respond to it dynamically in the {@link Configurator}.
     *
     *  @return The maximum number of cells that a component can span in a grid layout.
     */
    public int maxCellsToFill() {
        return _maxCellsToFill;
    }

    /**
     *  Returns the {@link Size} object that represents the parent container's size.
     *  The cell exposes this information so that you can use for a more
     *  informed decision on how many cells a component should span.
     *
     *  @return The width and height of the parent container of the component
     *          associated with this cell span configuration.
     */
    public Size parentSize() {
        return _parentSize;
    }

    /**
     *  Returns the {@link ParentSizeClass} category that represents the parent container's width
     *  relative to its preferred width.<br>
     *  The cell exposes this information so that you can use for a more
     *  informed decision on how many cells a component should span.<br>
     *  <p>
     *  The {@link ParentSizeClass} category is calculated by dividing the parent container's width
     *  by the preferred width of the parent container. <br>
     *  So a parent container is considered large if its size is close to the preferred size,
     *  and {@link ParentSizeClass#OVERSIZE} if it is significantly larger.
     *
     *  @return The category of the parent container's size.
     */
    public ParentSizeClass parentSizeCategory() {
        return _parentSizeCategory;
    }

    /**
     *  Returns a new and updated {@link FlowCellConf} instance with an additional
     *  {@link FlowCellSpanPolicy} that specifies the number of cells to fill
     *  for a given {@link ParentSizeClass} category.<br>
     *
     *  @param size The {@link ParentSizeClass} category to set.
     *  @param cellsToFill The number of cells to fill.
     *  @return A new {@link FlowCellConf} instance with the given {@link ParentSizeClass} and number of cells to fill.
     */
    public FlowCellConf with( ParentSizeClass size, int cellsToFill ) {
        Objects.requireNonNull(size);
        if ( cellsToFill < 0 ) {
            log.warn(SwingTree.get().logMarker(),
                    "Encountered negative number '{}' for cells to fill as part of the '{}' layout.",
                    cellsToFill, ResponsiveGridFlowLayout.class.getSimpleName(),
                    new Throwable("Stack trace for debugging purposes.")
                );
            cellsToFill = 0;
        } else if ( cellsToFill > _maxCellsToFill ) {
            log.warn(SwingTree.get().logMarker(),
                    "Encountered number '{}' for cells to fill that is greater than the maximum " +
                    "number of cells to fill '{}' as part of the '{}' layout.",
                    cellsToFill, _maxCellsToFill, ResponsiveGridFlowLayout.class.getSimpleName(),
                    new Throwable("Stack trace for debugging purposes.")
                );
            cellsToFill = _maxCellsToFill;
        }
        FlowCellSpanPolicy[] autoSpans = new FlowCellSpanPolicy[_autoSpans.length+1];
        System.arraycopy(_autoSpans, 0, autoSpans, 0, _autoSpans.length);
        autoSpans[_autoSpans.length] = FlowCellSpanPolicy.of(size, cellsToFill);
        return new FlowCellConf(_maxCellsToFill, _parentSize, _parentSizeCategory, autoSpans, _fill, _verticalAlignment);
    }

    /**
     *  Returns a new and updated {@link FlowCellConf} instance with an additional
     *  {@link FlowCellSpanPolicy} that specifies the number of cells to fill
     *  when the parent container is categorized as {@link ParentSizeClass#VERY_SMALL}.<br>
     *  A parent container is considered "very small" if its width
     *  is between 0/5 and 1/5 of its preferred width.<br>
     *  <p>
     *  The supplied number of cells to fill should be in the inclusive range of 0 to {@link #maxCellsToFill()}.<br>
     *  Values outside this range will be clamped to the nearest valid value and a warning will be logged.
     *
     *  @param span The number of cells to span as part of a grid
     *                     with a total of {@link #maxCellsToFill()} number of cells horizontally.
     *  @return A new {@link FlowCellConf} instance with the given number of cells to fill
     *          for the {@link ParentSizeClass#VERY_SMALL} category.
     */
    public FlowCellConf verySmall( int span ) {
        return with(ParentSizeClass.VERY_SMALL, span);
    }

    /**
     *  Returns a new and updated {@link FlowCellConf} instance with an additional
     *  {@link FlowCellSpanPolicy} that specifies the number of cells to fill
     *  when the parent container is categorized as {@link ParentSizeClass#SMALL}.<br>
     *  A parent container is considered "small" if its width
     *  is between 1/5 and 2/5 of its preferred width.<br>
     *  <p>
     *  The supplied number of cells to fill should be in the inclusive range of 0 to {@link #maxCellsToFill()}.<br>
     *  Values outside this range will be clamped to the nearest valid value and a warning will be logged.
     *
     *  @param span The number of cells to span as part of a grid
     *                     with a total of {@link #maxCellsToFill()} number of cells horizontally.
     *  @return A new {@link FlowCellConf} instance with the given number of cells to fill
     *          for the {@link ParentSizeClass#SMALL} category.
     */
    public FlowCellConf small( int span ) {
        return with(ParentSizeClass.SMALL, span);
    }

    /**
     *  Returns a new and updated {@link FlowCellConf} instance with an additional
     *  {@link FlowCellSpanPolicy} that specifies the number of cells to fill
     *  when the parent container is categorized as {@link ParentSizeClass#MEDIUM}.<br>
     *  A parent container is considered "medium" if its width
     *  is between 2/5 and 3/5 of its preferred width.<br>
     *  <p>
     *  The supplied number of cells to fill should be in the inclusive range of 0 to {@link #maxCellsToFill()}.<br>
     *  Values outside this range will be clamped to the nearest valid value and a warning will be logged.
     *
     *  @param span The number of cells to span as part of a grid
     *                     with a total of {@link #maxCellsToFill()} number of cells horizontally.
     *  @return A new {@link FlowCellConf} instance with the given number of cells to fill
     *          for the {@link ParentSizeClass#MEDIUM} category.
     */
    public FlowCellConf medium( int span ) {
        return with(ParentSizeClass.MEDIUM, span);
    }

    /**
     *  Returns a new and updated {@link FlowCellConf} instance with an additional
     *  {@link FlowCellSpanPolicy} that specifies the number of cells to fill
     *  when the parent container is categorized as {@link ParentSizeClass#LARGE}.<br>
     *  A parent container is considered "large" if its width
     *  is between 3/5 and 4/5 of its preferred width.<br>
     *  <p>
     *  The supplied number of cells to fill should be in the inclusive range of 0 to {@link #maxCellsToFill()}.<br>
     *  Values outside this range will be clamped to the nearest valid value and a warning will be logged.
     *
     *  @param span The number of cells to span as part of a grid
     *                     with a total of {@link #maxCellsToFill()} number of cells horizontally.
     *  @return A new {@link FlowCellConf} instance with the given number of cells to fill
     *          for the {@link ParentSizeClass#LARGE} category.
     */
    public FlowCellConf large( int span ) {
        return with(ParentSizeClass.LARGE, span);
    }

    /**
     *  Returns a new and updated {@link FlowCellConf} instance with an additional
     *  {@link FlowCellSpanPolicy} that specifies the number of cells to fill
     *  when the parent container is categorized as {@link ParentSizeClass#VERY_LARGE}.<br>
     *  A parent container is considered "very large" if its width
     *  is between 4/5 and 5/5 of its preferred width.<br>
     *  <p>
     *  The supplied number of cells to fill should be in the inclusive range of 0 to {@link #maxCellsToFill()}.<br>
     *  Values outside this range will be clamped to the nearest valid value and a warning will be logged.
     *
     *  @param span The number of cells to span as part of a grid
     *                     with a total of {@link #maxCellsToFill()} number of cells horizontally.
     *  @return A new {@link FlowCellConf} instance with the given number of cells to fill
     *          for the {@link ParentSizeClass#VERY_LARGE} category.
     */
    public FlowCellConf veryLarge( int span ) {
        return with(ParentSizeClass.VERY_LARGE, span);
    }

    /**
     *  Returns a new and updated {@link FlowCellConf} instance with an additional
     *  {@link FlowCellSpanPolicy} that specifies the number of cells to fill
     *  when the parent container is categorized as {@link ParentSizeClass#OVERSIZE}.<br>
     *  A parent container is considered "oversize" if its width is greater than its preferred width.<br>
     *  <p>
     *  The supplied number of cells to fill should be in the inclusive range of 0 to {@link #maxCellsToFill()}.<br>
     *  Values outside this range will be clamped to the nearest valid value and a warning will be logged.
     *
     *  @param span The number of cells to span as part of a grid
     *                     with a total of {@link #maxCellsToFill()} number of cells horizontally.
     *  @return A new {@link FlowCellConf} instance with the given number of cells to fill
     *          for the {@link ParentSizeClass#OVERSIZE} category.
     */
    public FlowCellConf oversize( int span ) {
        return with(ParentSizeClass.OVERSIZE, span);
    }

    boolean fill() {
        return _fill;
    }

    /**
     *  This flag defines how the grid cell in the flow layout should be filled
     *  by the component to which this cell configuration belongs.<br>
     *  The default behavior is to not fill the cell, but rather to align the component
     *  vertically in the center of the cell and use the component's preferred height.<br>
     *  If this flag is set to {@code true}, the component will fill the cell vertically
     *  and use the full height of the cell, which is also the full height of the row.<br>
     *  Note that this will ignore the component's preferred height!
     *
     * @param fill Whether the cell should be filled by the component.
     * @return A new {@link FlowCellConf} instance with the given fill flag.
     */
    public FlowCellConf fill(boolean fill) {
        return new FlowCellConf(_maxCellsToFill, _parentSize, _parentSizeCategory, _autoSpans, fill, _verticalAlignment);
    }

    UI.VerticalAlignment verticalAlignment() {
        return _verticalAlignment;
    }

    /**
     *  The {@link UI.VerticalAlignment} of a flow cell tells the
     *  {@link ResponsiveGridFlowLayout} how to place the component
     *  vertically within the cell.<br>
     *  So if you want the component to be aligned at the top,
     *  use {@link UI.VerticalAlignment#TOP}, if you want it to be
     *  centered, use {@link UI.VerticalAlignment#CENTER}, and if you
     *  want it to be aligned at the bottom, use {@link UI.VerticalAlignment#BOTTOM}.
     *
     * @param alignment The vertical alignment of the component within the cell.
     * @return A new {@link FlowCellConf} instance with the given vertical alignment policy.
     */
    public FlowCellConf align(UI.VerticalAlignment alignment) {
        return new FlowCellConf(_maxCellsToFill, _parentSize, _parentSizeCategory, _autoSpans, _fill, alignment);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" +
                    "maxCellsToFill="     + _maxCellsToFill + ", " +
                    "parentSize="         + _parentSize + ", " +
                    "parentSizeCategory=" + _parentSizeCategory + ", " +
                    "autoSpans="          + _autoSpans.length + ", " +
                    "fill="               + _fill + ", " +
                    "alignVertically="    + _verticalAlignment +
                "]";
    }

    @Override
    public boolean equals(Object obj) {
        if ( obj == this ) {
            return true;
        }
        if ( obj == null || obj.getClass() != this.getClass() ) {
            return false;
        }
        FlowCellConf that = (FlowCellConf)obj;
        return this._maxCellsToFill == that._maxCellsToFill
            && this._parentSize.equals(that._parentSize)
            && this._parentSizeCategory == that._parentSizeCategory
            && this._autoSpans.length == that._autoSpans.length
            && Arrays.deepEquals(this._autoSpans, that._autoSpans)
            && this._fill == that._fill
            && this._verticalAlignment == that._verticalAlignment;
    }

    @Override
    public int hashCode() {
        return Objects.hash(_maxCellsToFill, _parentSize, _parentSizeCategory, Arrays.hashCode(_autoSpans), _fill, _verticalAlignment);
    }
}
