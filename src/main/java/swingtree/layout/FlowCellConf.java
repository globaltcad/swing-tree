package swingtree.layout;

import com.google.errorprone.annotations.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 */
@Immutable
public final class FlowCellConf
{
    private static final Logger log = LoggerFactory.getLogger(FlowCellConf.class);
    private final int                  _maxCellsToFill;
    private final Size                 _parentSize;
    private final UI.ParentSize        _parentSizeCategory;
    @SuppressWarnings("Immutable")
    private final FlowCellSpanPolicy[] _autoSpans;


    FlowCellConf(
        int                  maxCellsToFill,
        Size                 parentSize,
        UI.ParentSize        parentSizeCategory,
        FlowCellSpanPolicy[] autoSpans
    ) {
        _maxCellsToFill     = maxCellsToFill;
        _parentSize         = Objects.requireNonNull(parentSize);
        _parentSizeCategory = Objects.requireNonNull(parentSizeCategory);
        _autoSpans          = Objects.requireNonNull(autoSpans.clone());
    }

    FlowCellSpanPolicy[] autoSpans() {
        return _autoSpans.clone();
    }

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
     *  Returns the {@link UI.ParentSize} category that represents the parent container's width
     *  relative to its preferred width.<br>
     *  The cell exposes this information so that you can use for a more
     *  informed decision on how many cells a component should span.<br>
     *  <p>
     *  The {@link UI.ParentSize} category is calculated by dividing the parent container's width
     *  by the preferred width of the parent container. <br>
     *  So a parent container is considered large if its size is close to the preferred size,
     *  and {@link UI.ParentSize#OVERSIZE} if it is significantly larger.
     *
     *  @return The category of the parent container's size.
     */
    public UI.ParentSize parentSizeCategory() {
        return _parentSizeCategory;
    }

    /**
     *  Returns a new and updated {@link FlowCellConf} instance with an additional
     *  {@link FlowCellSpanPolicy} that specifies the number of cells to fill
     *  for a given {@link UI.ParentSize} category.<br>
     *
     *  @param size The {@link UI.ParentSize} category to set.
     *  @param cellsToFill The number of cells to fill.
     *  @return A new {@link FlowCellConf} instance with the given {@link UI.ParentSize} and number of cells to fill.
     */
    public FlowCellConf with( UI.ParentSize size, int cellsToFill ) {
        Objects.requireNonNull(size);
        if ( cellsToFill < 0 ) {
            log.warn(
                    "Encountered negative number '"+cellsToFill+"' for cells " +
                    "to fill as part of the '"+ResponsiveGridFlowLayout.class.getSimpleName()+"' layout.",
                    new Throwable()
                );
            cellsToFill = 0;
        } else if ( cellsToFill > _maxCellsToFill ) {
            log.warn(
                    "Encountered number '"+cellsToFill+"' for cells to fill that is greater " +
                    "than the maximum number of cells to fill '"+_maxCellsToFill+"' " +
                    "as part of the '"+ResponsiveGridFlowLayout.class.getSimpleName()+"' layout.",
                    new Throwable()
                );
            cellsToFill = _maxCellsToFill;
        }
        FlowCellSpanPolicy[] autoSpans = new FlowCellSpanPolicy[_autoSpans.length+1];
        System.arraycopy(_autoSpans, 0, autoSpans, 0, _autoSpans.length);
        autoSpans[_autoSpans.length] = FlowCellSpanPolicy.of(size, cellsToFill);
        return new FlowCellConf(_maxCellsToFill, _parentSize, _parentSizeCategory, autoSpans);
    }

    /**
     *  Returns a new and updated {@link FlowCellConf} instance with an additional
     *  {@link FlowCellSpanPolicy} that specifies the number of cells to fill
     *  when the parent container is categorized as {@link UI.ParentSize#VERY_SMALL}.<br>
     *  A parent container is considered "very small" if its width
     *  is between 0/5 and 1/5 of its preferred width.<br>
     *  <p>
     *  The supplied number of cells to fill should be in the inclusive range of 0 to {@link #maxCellsToFill()}.<br>
     *  Values outside this range will be clamped to the nearest valid value and a warning will be logged.
     *
     *  @param span The number of cells to span as part of a grid
     *                     with a total of {@link #maxCellsToFill()} number of cells horizontally.
     *  @return A new {@link FlowCellConf} instance with the given number of cells to fill
     *          for the {@link UI.ParentSize#VERY_SMALL} category.
     */
    public FlowCellConf verySmall( int span ) {
        return with(UI.ParentSize.VERY_SMALL, span);
    }

    /**
     *  Returns a new and updated {@link FlowCellConf} instance with an additional
     *  {@link FlowCellSpanPolicy} that specifies the number of cells to fill
     *  when the parent container is categorized as {@link UI.ParentSize#SMALL}.<br>
     *  A parent container is considered "small" if its width
     *  is between 1/5 and 2/5 of its preferred width.<br>
     *  <p>
     *  The supplied number of cells to fill should be in the inclusive range of 0 to {@link #maxCellsToFill()}.<br>
     *  Values outside this range will be clamped to the nearest valid value and a warning will be logged.
     *
     *  @param span The number of cells to span as part of a grid
     *                     with a total of {@link #maxCellsToFill()} number of cells horizontally.
     *  @return A new {@link FlowCellConf} instance with the given number of cells to fill
     *          for the {@link UI.ParentSize#SMALL} category.
     */
    public FlowCellConf small( int span ) {
        return with(UI.ParentSize.SMALL, span);
    }

    /**
     *  Returns a new and updated {@link FlowCellConf} instance with an additional
     *  {@link FlowCellSpanPolicy} that specifies the number of cells to fill
     *  when the parent container is categorized as {@link UI.ParentSize#MEDIUM}.<br>
     *  A parent container is considered "medium" if its width
     *  is between 2/5 and 3/5 of its preferred width.<br>
     *  <p>
     *  The supplied number of cells to fill should be in the inclusive range of 0 to {@link #maxCellsToFill()}.<br>
     *  Values outside this range will be clamped to the nearest valid value and a warning will be logged.
     *
     *  @param span The number of cells to span as part of a grid
     *                     with a total of {@link #maxCellsToFill()} number of cells horizontally.
     *  @return A new {@link FlowCellConf} instance with the given number of cells to fill
     *          for the {@link UI.ParentSize#MEDIUM} category.
     */
    public FlowCellConf medium( int span ) {
        return with(UI.ParentSize.MEDIUM, span);
    }

    /**
     *  Returns a new and updated {@link FlowCellConf} instance with an additional
     *  {@link FlowCellSpanPolicy} that specifies the number of cells to fill
     *  when the parent container is categorized as {@link UI.ParentSize#LARGE}.<br>
     *  A parent container is considered "large" if its width
     *  is between 3/5 and 4/5 of its preferred width.<br>
     *  <p>
     *  The supplied number of cells to fill should be in the inclusive range of 0 to {@link #maxCellsToFill()}.<br>
     *  Values outside this range will be clamped to the nearest valid value and a warning will be logged.
     *
     *  @param span The number of cells to span as part of a grid
     *                     with a total of {@link #maxCellsToFill()} number of cells horizontally.
     *  @return A new {@link FlowCellConf} instance with the given number of cells to fill
     *          for the {@link UI.ParentSize#LARGE} category.
     */
    public FlowCellConf large( int span ) {
        return with(UI.ParentSize.LARGE, span);
    }

    /**
     *  Returns a new and updated {@link FlowCellConf} instance with an additional
     *  {@link FlowCellSpanPolicy} that specifies the number of cells to fill
     *  when the parent container is categorized as {@link UI.ParentSize#VERY_LARGE}.<br>
     *  A parent container is considered "very large" if its width
     *  is between 4/5 and 5/5 of its preferred width.<br>
     *  <p>
     *  The supplied number of cells to fill should be in the inclusive range of 0 to {@link #maxCellsToFill()}.<br>
     *  Values outside this range will be clamped to the nearest valid value and a warning will be logged.
     *
     *  @param span The number of cells to span as part of a grid
     *                     with a total of {@link #maxCellsToFill()} number of cells horizontally.
     *  @return A new {@link FlowCellConf} instance with the given number of cells to fill
     *          for the {@link UI.ParentSize#VERY_LARGE} category.
     */
    public FlowCellConf veryLarge( int span ) {
        return with(UI.ParentSize.VERY_LARGE, span);
    }

    /**
     *  Returns a new and updated {@link FlowCellConf} instance with an additional
     *  {@link FlowCellSpanPolicy} that specifies the number of cells to fill
     *  when the parent container is categorized as {@link UI.ParentSize#OVERSIZE}.<br>
     *  A parent container is considered "oversize" if its width is greater than its preferred width.<br>
     *  <p>
     *  The supplied number of cells to fill should be in the inclusive range of 0 to {@link #maxCellsToFill()}.<br>
     *  Values outside this range will be clamped to the nearest valid value and a warning will be logged.
     *
     *  @param span The number of cells to span as part of a grid
     *                     with a total of {@link #maxCellsToFill()} number of cells horizontally.
     *  @return A new {@link FlowCellConf} instance with the given number of cells to fill
     *          for the {@link UI.ParentSize#OVERSIZE} category.
     */
    public FlowCellConf oversize( int span ) {
        return with(UI.ParentSize.OVERSIZE, span);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" +
                    "maxCellsToFill="     + _maxCellsToFill + ", " +
                    "parentSize="         + _parentSize + ", " +
                    "parentSizeCategory=" + _parentSizeCategory + ", " +
                    "autoSpans="          + _autoSpans.length +
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
            && Arrays.deepEquals(this._autoSpans, that._autoSpans);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_maxCellsToFill, _parentSize, _parentSizeCategory, Arrays.hashCode(_autoSpans));
    }
}
