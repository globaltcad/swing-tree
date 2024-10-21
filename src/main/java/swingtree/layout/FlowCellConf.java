package swingtree.layout;

import com.google.errorprone.annotations.Immutable;
import swingtree.UI;

@Immutable
public final class FlowCellConf
{
    private final UI.ParentSize _parentSizeCategory;
    @SuppressWarnings("Immutable")
    private final FlowCellSpanPolicy[] _autoSpans;


    FlowCellConf(UI.ParentSize parentSizeCategory, FlowCellSpanPolicy[] autoSpans ) {
        _parentSizeCategory = parentSizeCategory;
        _autoSpans = autoSpans.clone();
    }

    public UI.ParentSize parentSizeCategory() {
        return _parentSizeCategory;
    }

    FlowCellSpanPolicy[] autoSpans() {
        return _autoSpans.clone();
    }

    public FlowCellConf with(UI.ParentSize size, int cellsToFill ) {
        FlowCellSpanPolicy[] autoSpans = new FlowCellSpanPolicy[_autoSpans.length+1];
        System.arraycopy(_autoSpans, 0, autoSpans, 0, _autoSpans.length);
        autoSpans[_autoSpans.length] = FlowCellSpanPolicy.of(size, cellsToFill);
        return new FlowCellConf(_parentSizeCategory, autoSpans);
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
}
