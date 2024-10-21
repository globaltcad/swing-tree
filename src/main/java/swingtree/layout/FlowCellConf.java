package swingtree.layout;

import com.google.errorprone.annotations.Immutable;
import swingtree.UI;

@Immutable
public final class FlowCellConf implements AddConstraint
{
    @SuppressWarnings("Immutable")
    private final AutoCellSpanPolicy[] _autoSpans;


    public FlowCellConf(AutoCellSpanPolicy[] autoSpans ) {
        _autoSpans = autoSpans.clone();
    }

    AutoCellSpanPolicy[] autoSpans() {
        return _autoSpans.clone();
    }

    public FlowCellConf with(UI.ParentSize size, int cellsToFill ) {
        AutoCellSpanPolicy[] autoSpans = new AutoCellSpanPolicy[_autoSpans.length+1];
        System.arraycopy(_autoSpans, 0, autoSpans, 0, _autoSpans.length);
        autoSpans[_autoSpans.length] = AutoCellSpanPolicy.of(size, cellsToFill);
        return new FlowCellConf(autoSpans);
    }

    public FlowCellConf verySmall(int cellsToFill ) {
        return with(UI.ParentSize.VERY_SMALL, cellsToFill);
    }

    public FlowCellConf small( int span ) {
        return with(UI.ParentSize.SMALL, span);
    }

    public FlowCellConf medium( int span ) {
        return with(UI.ParentSize.MEDIUM, span);
    }

    public FlowCellConf large( int span ) {
        return with(UI.ParentSize.LARGE, span);
    }

    public FlowCellConf veryLarge( int span ) {
        return with(UI.ParentSize.VERY_LARGE, span);
    }

    public FlowCellConf oversize( int span ) {
        return with(UI.ParentSize.OVERSIZE, span);
    }

    @Override
    public Object toConstraintForLayoutManager() {
        return this;
    }
}
