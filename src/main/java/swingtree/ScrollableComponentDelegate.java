package swingtree;

import swingtree.api.ScrollIncrementSupplier;
import swingtree.layout.Bounds;
import swingtree.layout.Size;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import java.util.Objects;

public class ScrollableComponentDelegate
{
    static ScrollableComponentDelegate of(
        JScrollPane scrollPane, JComponent view, Size preferredSize,
        int unitIncrement, int blockIncrement, boolean fitWidth, boolean fitHeight
    ) {
        return new ScrollableComponentDelegate(
                scrollPane, view, preferredSize, (a,b,c)->unitIncrement, (a,b,c)->blockIncrement, fitWidth, fitHeight
        );
    }

    private final JScrollPane             _scrollPane;
    private final JComponent              _view;
    private final Size                    _preferredSize;
    private final ScrollIncrementSupplier _unitIncrement;
    private final ScrollIncrementSupplier _blockIncrement;
    private final boolean                 _fitWidth;
    private final boolean                 _fitHeight;


    private ScrollableComponentDelegate(
        JScrollPane                 scrollPane,
        JComponent                  view,
        Size                         preferredSize,
        ScrollIncrementSupplier unitIncrement,
        ScrollIncrementSupplier blockIncrement,
        boolean                      fitWidth,
        boolean                      fitHeight
    ) {
        _scrollPane     = scrollPane;
        _view           = view;
        _preferredSize  = preferredSize;
        _unitIncrement  = unitIncrement;
        _blockIncrement = blockIncrement;
        _fitWidth       = fitWidth;
        _fitHeight      = fitHeight;
    }

    public ScrollableComponentDelegate preferredSize( int width, int height ) {
        return new ScrollableComponentDelegate(
                _scrollPane, _view, Size.of(width, height), _unitIncrement, _blockIncrement, _fitWidth, _fitHeight
            );
    }

    public ScrollableComponentDelegate preferredSize( Size preferredSize ) {
        return new ScrollableComponentDelegate(
                _scrollPane, _view, preferredSize, _unitIncrement, _blockIncrement, _fitWidth, _fitHeight
            );
    }

    public ScrollableComponentDelegate unitIncrement( int unitIncrement ) {
        return new ScrollableComponentDelegate(
                _scrollPane, _view, _preferredSize, (a,b,c)->unitIncrement, _blockIncrement, _fitWidth, _fitHeight
            );
    }

    public ScrollableComponentDelegate unitIncrement( ScrollIncrementSupplier unitIncrement ) {
        return new ScrollableComponentDelegate(
                _scrollPane, _view, _preferredSize, unitIncrement, _blockIncrement, _fitWidth, _fitHeight
            );
    }

    public ScrollableComponentDelegate blockIncrement( int blockIncrement ) {
        return new ScrollableComponentDelegate(
                _scrollPane, _view, _preferredSize, _unitIncrement, (a,b,c)->blockIncrement, _fitWidth, _fitHeight
            );
    }

    public ScrollableComponentDelegate blockIncrement( ScrollIncrementSupplier blockIncrement ) {
        return new ScrollableComponentDelegate(
                _scrollPane, _view, _preferredSize, _unitIncrement, blockIncrement, _fitWidth, _fitHeight
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
     * @return A new scroll config with the desired width fitting mode, which,
     *          if true, makes the viewport force the Scrollables width to match its own.
     */
    public ScrollableComponentDelegate fitWidth( boolean fitWidth ) {
        return new ScrollableComponentDelegate(
                _scrollPane, _view, _preferredSize, _unitIncrement, _blockIncrement, fitWidth, _fitHeight
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
     * @return A new scroll config with the desired width fitting mode, which,
     *          if true, makes a viewport force the Scrollables height to match its own.
     */
    public ScrollableComponentDelegate fitHeight( boolean fitHeight ) {
        return new ScrollableComponentDelegate(
                _scrollPane, _view, _preferredSize, _unitIncrement, _blockIncrement, _fitWidth, fitHeight
            );
    }

    public JScrollPane scrollPane() {
        return _scrollPane;
    }

    public JComponent view() {
        return _view;
    }

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
                "preferredSize=" + _preferredSize + ", " +
                "unitIncrement=" + _unitIncrement + ", " +
                "blockIncrement=" + _blockIncrement + ", " +
                "fitWidth=" + _fitWidth + ", " +
                "fitHeight=" + _fitHeight +
            "]";
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null || getClass() != obj.getClass() ) return false;
        ScrollableComponentDelegate that = (ScrollableComponentDelegate) obj;
        return _preferredSize.equals(that._preferredSize) &&
               _unitIncrement == that._unitIncrement &&
               _blockIncrement == that._blockIncrement &&
               _fitWidth == that._fitWidth &&
               _fitHeight == that._fitHeight;
    }

    @Override
    public int hashCode() {
        return Objects.hash(_preferredSize, _unitIncrement, _blockIncrement, _fitWidth, _fitHeight);
    }

}
