package swingtree;

import javax.swing.*;

public final class DragDropComponentConf<C extends JComponent, E> {

    public static <C extends JComponent, E> DragDropComponentConf<C, E> of(C component, E event, JComponent hoveringComponent ) {
        return new DragDropComponentConf<>( component, event, hoveringComponent, false );
    }

    private final C          _component;
    private final E          _event;
    private final JComponent _droppingComponent;
    private final boolean    _isConsumed;

    private DragDropComponentConf( C component, E event, JComponent hoveringComponent, boolean isConsumed ) {
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

    public DragDropComponentConf<C, E> consumeEvent() {
        return new DragDropComponentConf<>( _component, _event, _droppingComponent, true );
    }

}
