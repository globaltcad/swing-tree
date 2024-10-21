package swingtree.layout;

import com.google.errorprone.annotations.Immutable;
import swingtree.UI;

@Immutable
public final class FlowCellSpanPolicy {

    public static FlowCellSpanPolicy of(
        UI.ParentSize parentSize,
        int cellsToFill
    ) {
        return new FlowCellSpanPolicy(parentSize, cellsToFill);
    }


    private final UI.ParentSize _parentSize;
    private final int _cellsToFill;


    public FlowCellSpanPolicy(
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
