package swingtree;

import java.util.Objects;
import java.util.function.Function;

/**
 *  A declarative builder node for the {@link UI.TableHeader} component.
 *
 * @param <H> The type of the table header.
 */
public class UIForTableHeader<H extends UI.TableHeader> extends UIForAnySwing<UIForTableHeader<H>, H>
{
    /**
     * Extensions of the {@link  UIForAnySwing} always wrap
     * a single component for which they are responsible.
     *
     * @param tableHeader The JComponent type which will be wrapped by this builder node.
     */
    public UIForTableHeader( H tableHeader ) {
        super(tableHeader);
    }

    /**
     * Sets the tool tips for the table header.
     *
     * @param toolTips The tool tips to set.
     * @return This builder node.
     */
    UIForTableHeader<H> withToolTips( String... toolTips ) {
        Objects.requireNonNull(toolTips);
        getComponent().setToolTips(toolTips);
        return _this();
    }

    /**
     *  Determines the tool tips for each column header based
     *  on a function mapping the column index to the tool tip.
     *  The function is called each time the tool tip is needed.
     * @param toolTipAt The function mapping the column index to the tool tip.
     * @return This builder node.
     */
    UIForTableHeader<H> withToolTipAt( Function<Integer, String> toolTipAt ) {
        Objects.requireNonNull(toolTipAt);
        getComponent().setToolTipsSupplier(toolTipAt);
        return _this();
    }

}
