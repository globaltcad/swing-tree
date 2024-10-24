package swingtree.layout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import swingtree.UI;
import swingtree.api.Configurator;

import java.util.Objects;

/**
 *  This class is an {@link AddConstraint} designed as a
 *  component constraint for the {@link ResponsiveGridFlowLayout} layout manager,
 *  allowing you to dynamically adjust the number of grid layout cells
 *  a particular component can span based on the current parent container's size.<br>
 *  <p>
 *  Instances of this are wrapper for a {@link Configurator} lambda that
 *  processes a {@link FlowCellConf} instance containing an
 *  array of {@link FlowCellSpanPolicy} instances which
 *  define at which parent size category how many cells the component should span.<br>
 *  <p>
 *  This is intended to be created using the
 *  {@link UI#AUTO_SPAN(Configurator)} factory method
 *  as part of a fluent layout configuration oon your UI declarations.<br>
 *  Here is an example of how a usage of this class might look like:
 *  <pre>{@code
 *    UI.panel().withPrefSize(400, 300)
 *    .withFlowLayout()
 *    .add(UI.AUTO_SPAN( it->it.small(12).medium(6).large(8) ),
 *         html("A red cell").withStyle(it->it
 *             .backgroundColor(UI.Color.RED)
 *         )
 *    )
 *    .add(UI.AUTO_SPAN( it->it.small(12).medium(6).large(4) ),
 *         html("a green cell").withStyle(it->it
 *             .backgroundColor(Color.GREEN)
 *         )
 *    )
 *  }</pre>
 *  In the above example, the {@code it} parameter of the {@code Configurator}
 *  is an instance of the {@link FlowCellConf} class.
 *  The {@link Configurator} is called every time the {@link ResponsiveGridFlowLayout} updates the layout
 *  of the parent container, so that the number of cells a component spans can be adjusted dynamically.<br>
 *  The {@link swingtree.UIForAnySwing#withFlowLayout()} creates a {@link ResponsiveGridFlowLayout}
 *  and attaches it to the panel.<br>
 *  <p><b>
 *      Note that the {@link FlowCell} configuration may only take effect if the parent
 *      container has a {@link ResponsiveGridFlowLayout} as a {@link java.awt.LayoutManager} installed.
 *  </b>
 */
public final class FlowCell implements AddConstraint
{
    private static final Logger log = LoggerFactory.getLogger(FlowCell.class);
    private final Configurator<FlowCellConf> _configurator;

    public FlowCell(Configurator<FlowCellConf> configurator) {
        Objects.requireNonNull(configurator);
        _configurator = configurator;
    }

    FlowCellConf fetchConfig(
        int maxCells, Size parentSize, UI.ParentSize parentSizeCategory
    ) {
        FlowCellConf conf = new FlowCellConf(maxCells, parentSize, parentSizeCategory, new FlowCellSpanPolicy[0]);
        try {
            return _configurator.configure(conf);
        } catch (Exception e) {
            log.error(
                "Error configuring '"+ FlowCellConf.class.getSimpleName()+"' instance " +
                "for '"+ ResponsiveGridFlowLayout.class.getSimpleName() + "' layout.",
                e
            );
        }
        return conf;
    }

    @Override
    public Object toConstraintForLayoutManager() {
        return this;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" + _configurator + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        FlowCell that = (FlowCell) obj;
        return Objects.equals(_configurator, that._configurator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_configurator);
    }
}
