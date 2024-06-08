package swingtree;

import javax.swing.JComponent;
import java.awt.Component;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

class BasicCellDelegate<C extends JComponent, V> implements CellDelegate<C, V>
{
    private final C owner;
    private final V value;
    private final boolean isSelected;
    private final boolean hasFocus;
    private final int row;
    private final int column;
    private final Component[] componentRef;
    private final List<String> toolTips;
    private final Object[] defaultValueRef;
    private final Function<BasicCellDelegate<C, V>, Component> defaultRenderer;

    public BasicCellDelegate(
        C owner,
        V value,
        boolean isSelected,
        boolean hasFocus,
        int row,
        int column,
        Component[] componentRef,
        List<String> toolTips,
        Object[] defaultValueRef,
        Function<BasicCellDelegate<C, V>, Component> defaultRenderer
    ) {
        this.owner = owner;
        this.value = value;
        this.isSelected = isSelected;
        this.hasFocus = hasFocus;
        this.row = row;
        this.column = column;
        this.componentRef = componentRef;
        this.toolTips = toolTips;
        this.defaultValueRef = defaultValueRef;
        this.defaultRenderer = defaultRenderer;
    }

    @Override
    public C getComponent() {
        return owner;
    }

    @Override
    public Optional<V> value() {
        return Optional.ofNullable(value);
    }

    @Override
    public boolean isSelected() {
        return isSelected;
    }

    @Override
    public boolean hasFocus() {
        return hasFocus;
    }

    @Override
    public int getRow() {
        return row;
    }

    @Override
    public int getColumn() {
        return column;
    }

    @Override
    public Component getRenderer() {
        return defaultRenderer.apply(this);
    }

    @Override
    public void setRenderer(Component component) {
        componentRef[0] = component;
    }

    @Override
    public void setToolTip(String toolTip) {
        toolTips.add(toolTip);
    }

    @Override
    public void setDefaultRenderValue(Object newValue) {
        defaultValueRef[0] = newValue;
    }
}
