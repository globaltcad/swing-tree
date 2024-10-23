package swingtree.layout;

import com.google.errorprone.annotations.Immutable;
import swingtree.UI;
import swingtree.api.Configurator;

/**
 * Represents a policy for how many cells a component should span in a {@link ResponsiveGridFlowLayout}
 * in cae of the parent component's width being classified with the same {@link swingtree.UI.ParentSize}
 * category as is specified in the policy (see {@link #parentSize()}).<br>
 * Instances of this are aggregated in a {@link FlowCellConf} instance
 * managed by a {@link FlowCell}, which is an {@link AddConstraint}
 * implementation created using the {@link UI#AUTO_SPAN(Configurator)}
 * and then passed to one of the {@link swingtree.UIForAnySwing} {@code add} methods.<br><br>
 * <p>
 * Note that this class is not intended to be instantiated directly, but rather
 * implicitly created through the {@link FlowCellConf} class and its fluent API.
 */
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
