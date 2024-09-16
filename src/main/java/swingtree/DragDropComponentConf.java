package swingtree;

import javax.swing.*;

public final class DragDropComponentConf<C extends JComponent, E> {

    public static <C extends JComponent, E> DragDropComponentConf<C, E> of(C component, E event, JComponent hoveringComponent ) {
        return new DragDropComponentConf<>( component, event, hoveringComponent, false );
    }

    private final C          _component;
    private final E          _event;
    private final JComponent _hoveringComponent;
    private final boolean    _allowDrop;

    private DragDropComponentConf( C component, E event, JComponent hoveringComponent, boolean allowDrop ) {
        _component         = component;
        _event             = event;
        _hoveringComponent = hoveringComponent;
        _allowDrop         = allowDrop;
    }

    public C component() {
        return _component;
    }

    public E event() {
        return _event;
    }

    public JComponent hoveringComponent() {
        return _hoveringComponent;
    }

    public boolean allowDrop() {
        return _allowDrop;
    }

    public DragDropComponentConf<C, E> allowDrop( boolean allowDrop ) {
        return new DragDropComponentConf<>( _component, _event, _hoveringComponent, allowDrop );
    }

}
