package swingtree.layout;

import com.google.errorprone.annotations.Immutable;
import swingtree.UI;

@Immutable
public final class AutoCellSpanPolicy {

    public static AutoCellSpanPolicy of(
        UI.ParentSize parentSize,
        int cellsToFill
    ) {
        return new AutoCellSpanPolicy(parentSize, cellsToFill);
    }


    private final UI.ParentSize _parentSize;
    private final int _cellsToFill;


    public AutoCellSpanPolicy(
        UI.ParentSize parentSize,
        int cellsToFill
    ) {
        _parentSize = parentSize;
        _cellsToFill = cellsToFill;
    }

    public UI.ParentSize parentSize() {
        return _parentSize;
    }

    public int cellsToFill() {
        return _cellsToFill;
    }
}
