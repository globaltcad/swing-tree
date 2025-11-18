package swingtree;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import swingtree.api.Configurator;
import swingtree.api.ScrollIncrementSupplier;
import swingtree.layout.Bounds;
import swingtree.layout.Size;

import javax.swing.*;
import java.awt.Component;
import java.util.Objects;

/**
 * This class is an immutable builder which defines the {@link javax.swing.Scrollable} behavior of a component
 * within a {@link JScrollPane}.
 * So it is in essence a config object that provides information to a scrolling container
 * like {@link JScrollPane}.
 * <p>
 * Instances of this class are exposed to the {@link swingtree.api.Configurator} lambda
 * of the {@link UI#scrollPane(Configurator)} factory method, where you can configure the scrollable behavior
 * according to your needs. <br>
 * This includes setting the preferred size, unit increment, block increment, and whether the component should
 * fit the width or height of the viewport. <br>
 * Here an example demonstrating how the API of this class
 * is typically used:
 * <pre>{@code
 * UI.panel()
 * .withBorderTitled("Scrollable Panel")
 * .add(
 *     UI.scrollPane(conf -> conf
 *         .prefSize(400, 300)
 *         .unitIncrement(20)
 *         .blockIncrement(50)
 *         .fitWidth(true)
 *         .fitHeight(false)
 *     )
 * )
 * }</pre>
 * <p>
 * Note that the provided {@link Configurator} will be called
 * for every call to a method of the underlying {@link javax.swing.Scrollable}
 * component implementation, so the settings you provide can
 * also change dynamically based on the context captured by the lambda.<br>
 * <p>
 * Also note that this configuration object also exposes some context
 * information you may find useful when defining its properties like {@link #fitWidth(boolean)},
 * {@link #fitHeight(boolean)}, {@link #unitIncrement(int)}, and so on...<br>
 * Like for example the current {@link #view()}, which implements the {@link javax.swing.Scrollable}
 * and wraps your {@link #content} component. You can also access the {@link #viewport()} as
 * well as the overarching {@link #scrollPane()} overall!<br>
 * Again, the configurator you pass to {@link UI#scrollPane(Configurator)} will be
 * called eagerly, so everything you define in there will be completely dynamic,
 * which means your scroll behaviour can dynamically react to the involved components.
 */
public final class ScrollableComponentDelegate
{
    private static final Logger log = LoggerFactory.getLogger(ScrollableComponentDelegate.class);

    static ScrollableComponentDelegate of(
        JScrollPane scrollPane,
        JComponent content,
        Size preferredSize
    ) {
        Component view     = scrollPane.getViewport().getView();
        JViewport viewport = scrollPane.getViewport();

        boolean fitWidth ;
        boolean fitHeight;
        ScrollIncrementSupplier unitIncrementSupplier;
        ScrollIncrementSupplier blockIncrementSupplier;
        if ( content instanceof Scrollable) {
            Scrollable scrollable = (Scrollable)content;
            fitWidth = scrollable.getScrollableTracksViewportWidth();
            fitHeight = scrollable.getScrollableTracksViewportHeight();
            unitIncrementSupplier = (a,b,c) -> {
                int orientation = b == UI.Align.HORIZONTAL ? SwingConstants.HORIZONTAL : SwingConstants.VERTICAL;
                return scrollable.getScrollableUnitIncrement(a.toRectangle(),orientation,c);
            };
            blockIncrementSupplier = (a,b,c) -> {
                int orientation = b == UI.Align.HORIZONTAL ? SwingConstants.HORIZONTAL : SwingConstants.VERTICAL;
                return scrollable.getScrollableBlockIncrement(a.toRectangle(),orientation,c);
            };
        } else {
            int averageBlockIncrement  = 10;
            int averageUnitIncrement   = 10;
            try {
                int verticalBlockIncrement   = scrollPane.getVerticalScrollBar().getBlockIncrement();
                int horizontalBlockIncrement = scrollPane.getHorizontalScrollBar().getBlockIncrement();
                averageBlockIncrement = (verticalBlockIncrement + horizontalBlockIncrement) / 2;
            } catch ( Exception e ) {
                log.error(SwingTree.get().logMarker(), "Error while calculating average block increment for scrollable component.", e);
            }
            try {
                int verticalUnitIncrement   = scrollPane.getVerticalScrollBar().getUnitIncrement();
                int horizontalUnitIncrement = scrollPane.getHorizontalScrollBar().getUnitIncrement();
                averageUnitIncrement = (verticalUnitIncrement + horizontalUnitIncrement) / 2;
            } catch ( Exception e ) {
                log.error(SwingTree.get().logMarker(), "Error while calculating average unit increment for scrollable component.", e);
            }
            fitWidth  = viewport.getWidth()  >  view.getPreferredSize().width;
            fitHeight = viewport.getHeight() > view.getPreferredSize().height;
            int unitIncrement  = averageUnitIncrement;
            int blockIncrement = averageBlockIncrement;
            unitIncrementSupplier = (a,b,c) -> {return unitIncrement;};
            blockIncrementSupplier = (a,b,c) -> {return blockIncrement;};
        }
        return new ScrollableComponentDelegate(
                    scrollPane, content, view, preferredSize,
                    unitIncrementSupplier, blockIncrementSupplier,
                    fitWidth, fitHeight
                );
    }

    private final JScrollPane             _scrollPane;
    private final JComponent              _content;
    private final Component               _view;
    private final Size                    _preferredSize;
    private final ScrollIncrementSupplier _unitIncrement;
    private final ScrollIncrementSupplier _blockIncrement;
    private final boolean                 _fitWidth;
    private final boolean                 _fitHeight;


    private ScrollableComponentDelegate(
        JScrollPane             scrollPane,
        JComponent              content,
        Component               view,
        Size                    preferredSize,
        ScrollIncrementSupplier unitIncrement,
        ScrollIncrementSupplier blockIncrement,
        boolean                 fitWidth,
        boolean                 fitHeight
    ) {
        _scrollPane     = scrollPane;
        _content        = content;
        _view           = view;
        _preferredSize  = preferredSize;
        _unitIncrement  = unitIncrement;
        _blockIncrement = blockIncrement;
        _fitWidth       = fitWidth;
        _fitHeight      = fitHeight;
    }

    /**
     * Creates an updated scrollable config with the
     * preferred size of the viewport for a view component.
     * For example, the preferred size of a <code>JList</code> component
     * is the size required to accommodate all the cells in its list.
     * However, the value of <code>preferredScrollableViewportSize</code>
     * is the size required for <code>JList.getVisibleRowCount</code> rows.
     * A component without any properties that would affect the viewport
     * size should just return <code>getPreferredSize</code> here.
     *
     * @param width  The preferred width of a <code>JViewport</code> whose view a
     *               <code>Scrollable</code> configured by this config object.
     * @param height The preferred height of a <code>JViewport</code> whose view a
     *               <code>Scrollable</code> configured by this config object.
     * @return A new instance of {@link ScrollableComponentDelegate} with the updated preferred size.
     * @see JViewport#getPreferredSize
     */
    public ScrollableComponentDelegate prefSize( int width, int height ) {
        return new ScrollableComponentDelegate(
                _scrollPane, _content, _view, Size.of(width, height), _unitIncrement, _blockIncrement, _fitWidth, _fitHeight
            );
    }

    /**
     * Creates an updated scrollable config with the
     * preferred size of the viewport for a view component.
     * For example, the preferred size of a <code>JList</code> component
     * is the size required to accommodate all the cells in its list.
     * However, the value of <code>preferredScrollableViewportSize</code>
     * is the size required for <code>JList.getVisibleRowCount</code> rows.
     * A component without any properties that would affect the viewport
     * size should just return <code>getPreferredSize</code> here.
     *
     * @param preferredSize The preferred size of the component.
     * @return A new instance of {@link ScrollableComponentDelegate} with the updated preferred size.
     * @see JViewport#getPreferredSize
     * @throws NullPointerException If the preferred size is null, use {@link Size#unknown()}
     *                              to indicate that the preferred size is unknown.
     */
    public ScrollableComponentDelegate prefSize( Size preferredSize ) {
        Objects.requireNonNull(preferredSize);
        if ( preferredSize.equals(Size.unknown()) )
            return this;
        return new ScrollableComponentDelegate(
                _scrollPane, _content, _view, preferredSize, _unitIncrement, _blockIncrement, _fitWidth, _fitHeight
            );
    }

    /**
     * Creates an updated scrollable config with the specified unit increment.
     * The unit increment is the amount to scroll when the user requests a unit scroll.
     * For example, this could be the amount to scroll when the user presses the arrow keys.
     * Components that display logical rows or columns should compute
     * the scroll increment that will completely expose one new row
     * or column, depending on the value of orientation.  Ideally,
     * components should handle a partially exposed row or column by
     * returning the distance required to completely expose the item.
     * <p>
     * Scrolling containers, like JScrollPane, will use this increment value
     * each time the user requests a unit scroll.
     *
     * @param unitIncrement The unit increment value.
     * @return A new instance of {@link ScrollableComponentDelegate} with the updated unit increment.
     * @see JScrollBar#setUnitIncrement
     */
    public ScrollableComponentDelegate unitIncrement( int unitIncrement ) {
        return new ScrollableComponentDelegate(
                _scrollPane, _content, _view, _preferredSize, (a, b, c)->unitIncrement, _blockIncrement, _fitWidth, _fitHeight
            );
    }

    /**
     * Creates an updated scrollable config with the specified unit increment supplier,
     * (see {@link ScrollIncrementSupplier}) which takes the visible rectangle,
     * orientation and direction as arguments and returns the unit increment for the given context. <br>
     * The unit increment is the amount to scroll when the user requests a unit scroll.
     * For example, this could be the amount to scroll when the user presses the arrow keys.
     * Components that display logical rows or columns should compute
     * the scroll increment that will completely expose one new row
     * or column, depending on the value of orientation.  Ideally,
     * components should handle a partially exposed row or column by
     * returning the distance required to completely expose the item.
     * <p>
     * Scrolling containers, like JScrollPane, will use this increment value
     * each time the user requests a unit scroll.
     *
     * @param unitIncrement A {@link ScrollIncrementSupplier} that returns the unit increment for the given context.
     * @return A new instance of {@link ScrollableComponentDelegate} with the updated unit increment supplier.
     * @see JScrollBar#setUnitIncrement
     */
    public ScrollableComponentDelegate unitIncrement( ScrollIncrementSupplier unitIncrement ) {
        return new ScrollableComponentDelegate(
                _scrollPane, _content, _view, _preferredSize, unitIncrement, _blockIncrement, _fitWidth, _fitHeight
            );
    }

    /**
     * Creates an updated scrollable config with the specified block increment.
     * The block increment is the amount to scroll when the user requests a block scroll.
     * For example, this could be the amount to scroll when the user presses the page up or page down keys.
     * Components that display logical rows or columns should compute
     * the scroll increment that will completely expose one block
     * of rows or columns, depending on the value of orientation.
     * <p>
     * Scrolling containers, like JScrollPane, will use this increment value
     * each time the user requests a block scroll.
     *
     * @param blockIncrement The block increment value.
     * @return A new instance of {@link ScrollableComponentDelegate} with the updated block increment.
     * @see JScrollBar#setBlockIncrement
     */
    public ScrollableComponentDelegate blockIncrement( int blockIncrement ) {
        return new ScrollableComponentDelegate(
                _scrollPane, _content, _view, _preferredSize, _unitIncrement, (a, b, c)->blockIncrement, _fitWidth, _fitHeight
            );
    }

    /**
     * Creates an updated scrollable config with the specified block increment supplier,
     * (see {@link ScrollIncrementSupplier}) which takes the visible rectangle,
     * orientation and direction as arguments and returns the block increment for the given context. <br>
     * The block increment is the amount to scroll when the user requests a block scroll.
     * For example, this could be the amount to scroll when the user presses the page up or page down keys.
     * Components that display logical rows or columns should compute
     * the scroll increment that will completely expose one block
     * of rows or columns, depending on the value of orientation.
     * <p>
     * Scrolling containers, like JScrollPane, will use this increment value
     * each time the user requests a block scroll.
     *
     * @param blockIncrement A {@link ScrollIncrementSupplier} that returns the block increment for the given context.
     * @return A new instance of {@link ScrollableComponentDelegate} with the updated block increment supplier.
     * @see JScrollBar#setBlockIncrement
     */
    public ScrollableComponentDelegate blockIncrement( ScrollIncrementSupplier blockIncrement ) {
        return new ScrollableComponentDelegate(
                _scrollPane, _content, _view, _preferredSize, _unitIncrement, blockIncrement, _fitWidth, _fitHeight
            );
    }

    /**
     * Set this to true if a viewport should always force the width of this
     * <code>Scrollable</code> to match the width of the viewport.
     * For example a normal
     * text view that supported line wrapping would return true here, since it
     * would be undesirable for wrapped lines to disappear beyond the right
     * edge of the viewport.  Note that returning true for a Scrollable
     * whose ancestor is a JScrollPane effectively disables horizontal
     * scrolling.
     * <p>
     * Scrolling containers, like JViewport, will use this method each
     * time they are validated.
     *
     * @param fitWidth If true, the viewport will force the Scrollables width to match its own.
     * @return A new scroll config with the desired width fitting mode, which,
     *          if true, makes the viewport force the Scrollables width to match its own.
     */
    public ScrollableComponentDelegate fitWidth( boolean fitWidth ) {
        return new ScrollableComponentDelegate(
                _scrollPane, _content, _view, _preferredSize, _unitIncrement, _blockIncrement, fitWidth, _fitHeight
        );
    }

    /**
     * Set this to true if a viewport should always force the height of this
     * Scrollable to match the height of the viewport.  For example a
     * columnar text view that flowed text in left to right columns
     * could effectively disable vertical scrolling by returning
     * true here.
     * <p>
     * Scrolling containers, like JViewport, will use this method each
     * time they are validated.
     *
     * @param fitHeight If true, the viewport will force the Scrollables height to match its own.
     * @return A new scroll config with the desired width fitting mode, which,
     *          if true, makes a viewport force the Scrollables height to match its own.
     */
    public ScrollableComponentDelegate fitHeight( boolean fitHeight ) {
        return new ScrollableComponentDelegate(
                _scrollPane, _content, _view, _preferredSize, _unitIncrement, _blockIncrement, _fitWidth, fitHeight
        );
    }

    /**
     * Returns the scroll pane that contains the scrollable component
     * this configuration is for.
     *
     * @return The scroll pane that contains the scrollable component.
     */
    public JScrollPane scrollPane() {
        return _scrollPane;
    }

    /**
     * Returns the viewport of the scroll pane that contains the {@link javax.swing.Scrollable} component
     * this configuration is for.
     *
     * @return The viewport of the scroll pane that contains the scrollable component.
     */
    public JViewport viewport() {
        return _scrollPane.getViewport();
    }

    /**
     * Returns the user provided content component that is contained in the scroll pane
     * and which is wrapped by a view component implementing the {@link javax.swing.Scrollable}
     * interface configured by this {@link ScrollableComponentDelegate}.
     * <b>
     *     The content component is effectively the component supplied to
     *     the {@link UIForAnyScrollPane#add(Component[])} method.
     *     (Please note that a scroll pane can only ever hold a single content component)
     * </b>
     *
     * @return The content component that is contained within the scroll pane
     *         and wrapped by the {@link #view()} component.
     */
    public JComponent content() {
        return _content;
    }

    /**
     * Returns the view component implementing the {@link javax.swing.Scrollable} interface and which
     * is placed directly in the scroll panes {@link #viewport()} through {@link JViewport#setView(Component)}.<br>
     * This is the main UI component that is configured by this {@link ScrollableComponentDelegate}.
     *
     * @return The view component that is contained within the scroll pane.
     */
    public Component view() {
        return _view;
    }

    // Not part of the public API below:

    Size preferredSize() {
        return _preferredSize;
    }

    int unitIncrement(
        Bounds   viewRectangle,
        UI.Align orientation,
        int      direction
    ) {
        return _unitIncrement.get(viewRectangle, orientation, direction);
    }

    int blockIncrement(
        Bounds   viewRectangle,
        UI.Align orientation,
        int      direction
    ) {
        return _blockIncrement.get(viewRectangle, orientation, direction);
    }

    boolean fitWidth() {
        return _fitWidth;
    }

    boolean fitHeight() {
        return _fitHeight;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" +
                "preferredSize="  + _preferredSize  + ", " +
                "unitIncrement="  + _unitIncrement  + ", " +
                "blockIncrement=" + _blockIncrement + ", " +
                "fitWidth="       + _fitWidth       + ", " +
                "fitHeight="      + _fitHeight      +
            "]";
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( !(obj instanceof ScrollableComponentDelegate) )
            return false;
        ScrollableComponentDelegate that = (ScrollableComponentDelegate) obj;
        return _preferredSize.equals(that._preferredSize) &&
               _unitIncrement.equals(that._unitIncrement) &&
               _blockIncrement.equals(that._blockIncrement) &&
               _fitWidth       == that._fitWidth &&
               _fitHeight      == that._fitHeight;
    }

    @Override
    public int hashCode() {
        return Objects.hash(_preferredSize, _unitIncrement, _blockIncrement, _fitWidth, _fitHeight);
    }

}
