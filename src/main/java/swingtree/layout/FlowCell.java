package swingtree.layout;

import com.google.errorprone.annotations.Immutable;

@Immutable
public final class FlowCell implements AddConstraint
{
    @SuppressWarnings("Immutable")
    private final AutoCellSpanPolicy[] _autoSpans;
    private final int _numberOfColumns;

    public FlowCell(int numberOfColumns, AutoCellSpanPolicy[] autoSpans) {
        _autoSpans = autoSpans.clone();
        _numberOfColumns = numberOfColumns;
    }

    AutoCellSpanPolicy[] autoSpans() {
        return _autoSpans.clone();
    }

    int numberOfColumns() {
        return _numberOfColumns;
    }

    @Override
    public Object toConstraintForLayoutManager() {
        return this;
    }
}
