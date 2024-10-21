package swingtree.layout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import swingtree.UI;
import swingtree.api.Configurator;

import java.util.Objects;

public final class FlowCell implements AddConstraint
{
    private static final Logger log = LoggerFactory.getLogger(FlowCell.class);
    private final Configurator<FlowCellConf> _configurator;

    public FlowCell(Configurator<FlowCellConf> configurator) {
        Objects.requireNonNull(configurator);
        _configurator = configurator;
    }

    FlowCellConf fetchConfig(
        UI.ParentSize parentSizeCategory
    ) {
        FlowCellConf conf = new FlowCellConf(parentSizeCategory, new FlowCellSpanPolicy[0]);
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
}
