package swingtree;

import swingtree.components.JGlassPane;

import javax.swing.*;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.Objects;

public final class DragOverEventComponentDelegate<C extends JComponent> extends ComponentMouseEventDelegate<C>
{
    public static <C extends JComponent> DragOverEventComponentDelegate<C> of(
        C          component,
        MouseEvent globalEvent,
        JComponent hoveringComponent
    ) {
        Component glassPane = (JGlassPane) globalEvent.getSource();
        MouseEvent localEvent = SwingUtilities.convertMouseEvent( glassPane, globalEvent, component );
        return new DragOverEventComponentDelegate<>( component, localEvent, globalEvent, hoveringComponent );
    }

    private final JComponent _hoveringComponent;
    private final MouseEvent _globalEvent;


    private DragOverEventComponentDelegate(
        C component,
        MouseEvent localEvent,
        MouseEvent globalEvent,
        JComponent hoveringComponent
    ) {
        super(component, localEvent);
        _hoveringComponent = Objects.requireNonNull(hoveringComponent);
        _globalEvent       = Objects.requireNonNull(globalEvent);
    }

    public JComponent hoveringComponent() {
        return _hoveringComponent;
    }

    public MouseEvent globalEvent() {
        return _globalEvent;
    }

}
