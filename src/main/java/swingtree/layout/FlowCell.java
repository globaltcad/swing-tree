package swingtree.layout;

import com.google.errorprone.annotations.Immutable;

@Immutable
public final class FlowCell implements AddConstraint
{
    @SuppressWarnings("Immutable")
    private final AutoCellSpanPolicy[] _autoSpans;


    public FlowCell( AutoCellSpanPolicy[] autoSpans ) {
        _autoSpans = autoSpans.clone();
    }

    AutoCellSpanPolicy[] autoSpans() {
        return _autoSpans.clone();
    }

    @Override
    public Object toConstraintForLayoutManager() {
        return this;
    }
}
