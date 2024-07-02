package swingtree;

import swingtree.api.Configurator;

import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.util.Objects;
import java.util.function.Function;

/**
 *  A declarative builder node for the {@link UI.TableHeader} component.
 *
 * @param <H> The type of the table header.
 */
public final class UIForTableHeader<H extends UI.TableHeader> extends UIForAnySwing<UIForTableHeader<H>, H>
{
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UIForTableHeader.class);
    private final BuilderState<H> _state;

    /**
     * Extensions of the {@link  UIForAnySwing} always wrap
     * a single component for which they are responsible.
     *
     * @param state The {@link BuilderState} modelling how the component is built.
     */
    UIForTableHeader( BuilderState<H> state ) {
        Objects.requireNonNull(state);
        _state = state;
    }

    @Override
    protected BuilderState<H> _state() {
        return _state;
    }
    
    @Override
    protected UIForTableHeader<H> _newBuilderWithState(BuilderState<H> newState ) {
        return new UIForTableHeader<>(newState);
    }

    /**
     * Sets the tool tips for the table header.
     *
     * @param toolTips The tool tips to set.
     * @return This builder node.
     */
    UIForTableHeader<H> withToolTips( String... toolTips ) {
        Objects.requireNonNull(toolTips);
        for ( String toolTip : toolTips )
            Objects.requireNonNull(toolTip);
        return _with( thisComponent -> {
                    thisComponent.setToolTips(toolTips);
                })
                ._this();
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
        return _with( thisComponent -> {
                    thisComponent.setToolTipsSupplier(toolTipAt);
                })
                ._this();
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
        return _with( thisComponent -> {
                    thisComponent.setDefaultRenderer(renderer);
                })
                ._this();
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
     *         .withRenderer( it -> it
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
    UIForTableHeader<H> withRenderer(
        Configurator<RenderBuilder<H, Object>> renderBuilder
    ) {
        NullUtil.nullArgCheck(renderBuilder, "renderBuilder", RenderBuilder.class);
        RenderBuilder<H, Object> builder = _renderTable();
        try {
            builder = renderBuilder.configure(builder);
        } catch (Exception e) {
            log.error("Error while building table renderer.", e);
            return this;
        }
        Objects.requireNonNull(builder);
        return withDefaultRenderer(builder.getForTable());
    }

    private static <T extends JTableHeader> RenderBuilder<T, Object> _renderTable() {
        return (RenderBuilder) RenderBuilder.forTable(Object.class);
    }

}
