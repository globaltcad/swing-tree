package swingtree.api;

import swingtree.UI;
import swingtree.layout.Bounds;

import javax.swing.JScrollBar;
import java.awt.Rectangle;

/**
 *  A supplier of scrollable increments for a {@link javax.swing.JScrollPane}.
 *  which can be passed to the scroll pane configurator API
 *  at {@link UI#scrollPane(Configurator)} using
 *  {@link swingtree.ScrollableComponentDelegate#blockIncrement(ScrollIncrementSupplier)}
 *  or {@link swingtree.ScrollableComponentDelegate#unitIncrement(ScrollIncrementSupplier)}.
 */
@FunctionalInterface
public interface ScrollIncrementSupplier
{
    static ScrollIncrementSupplier none() {
        return Constants.SCROLLABLE_INCREMENT_SUPPLIER_NONE;
    }

    /**
     *  Returns the scroll increment for the given view rectangle, orientation and direction.
     *  This scroll increment may either be a unit increment or a block increment,
     *  see {@link javax.swing.Scrollable#getScrollableUnitIncrement(Rectangle, int, int)}
     *  and {@link javax.swing.Scrollable#getScrollableBlockIncrement(Rectangle, int, int)}
     *  for more information about the parameters. <br>
     *  <b>
     *      This method is not designed to be used anywhere else than in the
     *      {@link swingtree.ScrollableComponentDelegate#blockIncrement(ScrollIncrementSupplier)}
     *      or {@link swingtree.ScrollableComponentDelegate#unitIncrement(ScrollIncrementSupplier)}.
     *  </b>
     *  <br>
     * Components that display logical rows or columns should compute
     * the scroll increment that will completely expose one new row
     * or column, depending on the value of orientation.  Ideally,
     * components should handle a partially exposed row or column by
     * returning the distance required to completely expose the item.
     * <p>
     * Scrolling containers, like JScrollPane, will use this method
     * each time the user requests a unit scroll.
     *
     * @param visibleRectangle The view area visible within the viewport
     * @param orientation Either UI.VERTICAL or UI.HORIZONTAL.
     * @param direction Less than zero to scroll up/left, greater than zero for down/right.
     * @return The "unit" or "block" increment for scrolling in the specified direction.
     *         This value should always be positive.
     * @see JScrollBar#setUnitIncrement
     * @see JScrollBar#setBlockIncrement
     */
    int get(
        Bounds   visibleRectangle,
        UI.Align orientation,
        int      direction
    );
}
