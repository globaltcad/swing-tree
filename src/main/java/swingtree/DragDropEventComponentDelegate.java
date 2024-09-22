package swingtree;

import swingtree.components.JGlassPane;

import javax.swing.*;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.Objects;

public final class DragDropEventComponentDelegate<C extends JComponent> extends ComponentMouseEventDelegate<C>
{
    public static <C extends JComponent> DragDropEventComponentDelegate<C> of(
        C          component,
        MouseEvent globalEvent,
        JComponent hoveringComponent
    ) {
        Component glassPane = (JGlassPane) globalEvent.getSource();
        MouseEvent localEvent = SwingUtilities.convertMouseEvent( glassPane, globalEvent, component );
        return new DragDropEventComponentDelegate<>( component, localEvent, globalEvent, hoveringComponent );
    }

    private final JComponent _droppingComponent;
    private final MouseEvent _globalEvent;

    private boolean _allowOtherEventConsumers;


    private DragDropEventComponentDelegate(
        C component,
        MouseEvent localEvent,
        MouseEvent globalEvent,
        JComponent droppingComponent
    ) {
        super(component, localEvent);
        _droppingComponent        = Objects.requireNonNull(droppingComponent);
        _globalEvent              = Objects.requireNonNull(globalEvent);
        _allowOtherEventConsumers = false;
    }

    public JComponent droppingComponent() {
        return _droppingComponent;
    }

    public MouseEvent globalEvent() {
        return _globalEvent;
    }

    public void allowOtherEventConsumers(boolean allow) {
        _allowOtherEventConsumers = allow;
    }

    public boolean isConsuming() {
        return !_allowOtherEventConsumers;
    }

}
