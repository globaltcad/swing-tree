package swingtree;

import javax.swing.*;

public final class DragDropEventComponentDelegate<C extends JComponent, E> {

    public static <C extends JComponent, E> DragDropEventComponentDelegate<C, E> of(C component, E event, JComponent hoveringComponent ) {
        return new DragDropEventComponentDelegate<>( component, event, hoveringComponent, true );
    }

    private final C          _component;
    private final E          _event;
    private final JComponent _droppingComponent;
    private final boolean    _isConsumed;

    private DragDropEventComponentDelegate(C component, E event, JComponent hoveringComponent, boolean isConsumed ) {
        _component         = component;
        _event             = event;
        _droppingComponent = hoveringComponent;
        _isConsumed        = isConsumed;
    }

    public C component() {
        return _component;
    }

    public E event() {
        return _event;
    }

    public JComponent droppingComponent() {
        return _droppingComponent;
    }

    public boolean isConsumed() {
        return _isConsumed;
    }

    public DragDropEventComponentDelegate<C, E> consumeEvent() {
        return new DragDropEventComponentDelegate<>( _component, _event, _droppingComponent, true );
    }

}
