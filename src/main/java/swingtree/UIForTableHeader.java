package swingtree;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
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

    /**
     * Sets the default renderer for the table header,
     * which is used when no <code>headerRenderer</code>
     * is defined by a <code>TableColumn</code>.
     *
     * @param renderer The renderer to set.
     * @return This builder node.
     */
    UIForTableHeader<H> withDefaultRenderer( TableCellRenderer renderer ) {
        Objects.requireNonNull(renderer);
        getComponent().setDefaultRenderer(renderer);
        return _this();
    }

    /**
     * Sets the default renderer for the table header,
     * which is used when no <code>headerRenderer</code>
     * is defined by a <code>TableColumn</code>. <br>
     * This method does not receive a renderer directly,
     * instead it expects a builder for the renderer
     * whose build method will be called for you. <br>
     * As part of a UI declaration, this might look like this:
     * <pre>{@code
     *     UI.table(
     *         ...
     *     )
     *     .withHeader(
     *         UI.tableHeader()
     *         .withMaxWidth(100)
     *         .withDefaultRenderer(
     *             UI.renderTable()
     *             .when(Integer.class)
     *             .asText( cell ->
     *                 cell.valueAsString()
     *                     .orElse("")+"!"
     *             )
     *             .when(...)
     *             ...
     *         )
     *     )
     * }</pre>
     *
     * @param renderBuilder The builder for the renderer to set.
     * @return This builder node.
     */
    UIForTableHeader<H> withDefaultRenderer( Render.Builder<JTable, ?> renderBuilder ) {
        Objects.requireNonNull(renderBuilder);
        getComponent().setDefaultRenderer(renderBuilder.getForTable());
        return _this();
    }
}
