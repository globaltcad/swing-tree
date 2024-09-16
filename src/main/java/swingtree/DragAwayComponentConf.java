package swingtree;

import javax.swing.*;

public final class DragAwayComponentConf<C extends JComponent, E>
{
    public static <C extends JComponent, E> DragAwayComponentConf<C, E> of(
        C component,
        E event
    ) {
        return new DragAwayComponentConf<>(component, event, false, 1);
    }

    private final C       _component;
    private final E       _event;
    private final boolean _enabled;
    private final double  _opacity;

    private DragAwayComponentConf(
        C       component,
        E       event,
        boolean enabled,
        double  opacity
    ) {
        _component = component;
        _event = event;
        _enabled = enabled;
        _opacity = opacity;
    }

    public C component() {
        return _component;
    }

    public E event() {
        return _event;
    }

    public boolean enabled() {
        return _enabled;
    }

    public double opacity() {
        return _opacity;
    }

    public DragAwayComponentConf<C, E> enabled( boolean enabled ) {
        return new DragAwayComponentConf<>(_component, _event, enabled, _opacity);
    }

    public DragAwayComponentConf<C, E> opacity( double opacity ) {
        return new DragAwayComponentConf<>(_component, _event, _enabled, opacity);
    }
}
