package swingtree;

import javax.swing.*;

public final class DragOverComponentConf<C extends JComponent, E>
{
    public static <C extends JComponent, E> DragOverComponentConf<C, E> of( C component, E event, JComponent hoveringComponent ) {
        return new DragOverComponentConf<>( component, event, hoveringComponent, true );
    }

    private final C          _component;
    private final E          _event;
    private final JComponent _hoveringComponent;
    private final boolean    _allowDrop;

    private DragOverComponentConf( C component, E event, JComponent hoveringComponent, boolean allowDrop ) {
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

    public DragOverComponentConf<C, E> allowDrop( boolean allowDrop ) {
        return new DragOverComponentConf<>( _component, _event, _hoveringComponent, allowDrop );
    }

}
