package swingtree;

import swingtree.api.ScrollIncrementSupplier;
import swingtree.layout.Bounds;
import swingtree.layout.Size;

import java.util.Objects;

public class ScrollableComponentDelegate
{
    private static final ScrollableComponentDelegate _NONE = new ScrollableComponentDelegate(
        Size.unknown(), ScrollIncrementSupplier.none(), ScrollIncrementSupplier.none(),
        false, false
    );

    static ScrollableComponentDelegate none() {
        return _NONE;
    }

    private final Size                    _preferredSize;
    private final ScrollIncrementSupplier _unitIncrement;
    private final ScrollIncrementSupplier _blockIncrement;
    private final boolean                 _fitWidth;
    private final boolean                 _fitHeight;

    private ScrollableComponentDelegate(
        Size                         preferredSize,
        ScrollIncrementSupplier unitIncrement,
        ScrollIncrementSupplier blockIncrement,
        boolean                      fitWidth,
        boolean                      fitHeight
    ) {
        _preferredSize  = preferredSize;
        _unitIncrement  = unitIncrement;
        _blockIncrement = blockIncrement;
        _fitWidth       = fitWidth;
        _fitHeight      = fitHeight;
    }

    public ScrollableComponentDelegate preferredSize( Size preferredSize ) {
        return new ScrollableComponentDelegate(
            preferredSize, _unitIncrement, _blockIncrement, _fitWidth, _fitHeight
        );
    }

    public ScrollableComponentDelegate unitIncrement( ScrollIncrementSupplier unitIncrement ) {
        return new ScrollableComponentDelegate(
            _preferredSize, unitIncrement, _blockIncrement, _fitWidth, _fitHeight
        );
    }

    public ScrollableComponentDelegate blockIncrement( ScrollIncrementSupplier blockIncrement ) {
        return new ScrollableComponentDelegate(
            _preferredSize, _unitIncrement, blockIncrement, _fitWidth, _fitHeight
        );
    }

    public ScrollableComponentDelegate fitWidth( boolean fitWidth ) {
        return new ScrollableComponentDelegate(
            _preferredSize, _unitIncrement, _blockIncrement, fitWidth, _fitHeight
        );
    }

    public ScrollableComponentDelegate fitHeight( boolean fitHeight ) {
        return new ScrollableComponentDelegate(
            _preferredSize, _unitIncrement, _blockIncrement, _fitWidth, fitHeight
        );
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
