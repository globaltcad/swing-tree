package swingtree;

import sprouts.Action;

import javax.swing.*;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;

public final class DragAwayComponentConf<C extends JComponent, E>
{
    private static final Action NO_ACTION = e -> {};

    public static <C extends JComponent, E> DragAwayComponentConf<C, E> of(
        C component,
        E event
    ) {
        return new DragAwayComponentConf<>(
                component, event, false, 1,
                NO_ACTION, NO_ACTION, NO_ACTION, NO_ACTION, NO_ACTION, NO_ACTION
        );
    }

    private final C       _component;
    private final E       _event;
    private final boolean _enabled;
    private final double  _opacity;
    private final Action<DragSourceDragEvent> _onDragEnter;
    private final Action<DragSourceDragEvent> _onDragMove;
    private final Action<DragSourceDragEvent> _onDragOver;
    private final Action<DragSourceDragEvent> _onDropActionChanged;
    private final Action<DragSourceEvent>     _onDragExit;
    private final Action<DragSourceDropEvent> _onDragDropEnd;


    private DragAwayComponentConf(
        C       component,
        E       event,
        boolean enabled,
        double  opacity,
        Action<DragSourceDragEvent> onDragEnter,
        Action<DragSourceDragEvent> onDragMove,
        Action<DragSourceDragEvent> onDragOver,
        Action<DragSourceDragEvent> onDropActionChanged,
        Action<DragSourceEvent>     onDragExit,
        Action<DragSourceDropEvent> onDragDropEnd
    ) {
        _component           = component;
        _event               = event;
        _enabled             = enabled;
        _opacity             = opacity;
        _onDragEnter         = onDragEnter;
        _onDragMove          = onDragMove;
        _onDragOver          = onDragOver;
        _onDropActionChanged = onDropActionChanged;
        _onDragExit          = onDragExit;
        _onDragDropEnd       = onDragDropEnd;
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

    public Action<DragSourceDragEvent> onDragEnter() {
        return _onDragEnter;
    }

    public Action<DragSourceDragEvent> onDragMove() {
        return _onDragMove;
    }

    public Action<DragSourceDragEvent> onDragOver() {
        return _onDragOver;
    }

    public Action<DragSourceDragEvent> onDropActionChanged() {
        return _onDropActionChanged;
    }

    public Action<DragSourceEvent> onDragExit() {
        return _onDragExit;
    }

    public Action<DragSourceDropEvent> onDragDropEnd() {
        return _onDragDropEnd;
    }

    public DragAwayComponentConf<C, E> enabled( boolean enabled ) {
        return new DragAwayComponentConf<>(
                _component, _event, enabled, _opacity,
                _onDragEnter, _onDragMove, _onDragOver, _onDropActionChanged, _onDragExit, _onDragDropEnd
            );
    }

    public DragAwayComponentConf<C, E> opacity( double opacity ) {
        return new DragAwayComponentConf<>(
                _component, _event, _enabled, opacity,
                _onDragEnter, _onDragMove, _onDragOver, _onDropActionChanged, _onDragExit, _onDragDropEnd
        );
    }

    public DragAwayComponentConf<C, E> onDragEnter( Action<DragSourceDragEvent> onDragEnter ) {
        return new DragAwayComponentConf<>(
                _component, _event, _enabled, _opacity,
                onDragEnter, _onDragMove, _onDragOver, _onDropActionChanged, _onDragExit, _onDragDropEnd
        );
    }

    public DragAwayComponentConf<C, E> onDragMove( Action<DragSourceDragEvent> onDragMove ) {
        return new DragAwayComponentConf<>(
                _component, _event, _enabled, _opacity,
                _onDragEnter, onDragMove, _onDragOver, _onDropActionChanged, _onDragExit, _onDragDropEnd
        );
    }

    public DragAwayComponentConf<C, E> onDragOver( Action<DragSourceDragEvent> onDragOver ) {
        return new DragAwayComponentConf<>(
                _component, _event, _enabled, _opacity,
                _onDragEnter, _onDragMove, onDragOver, _onDropActionChanged, _onDragExit, _onDragDropEnd
        );
    }

    public DragAwayComponentConf<C, E> onDropActionChanged( Action<DragSourceDragEvent> onDropActionChanged ) {
        return new DragAwayComponentConf<>(
                _component, _event, _enabled, _opacity,
                _onDragEnter, _onDragMove, _onDragOver, onDropActionChanged, _onDragExit, _onDragDropEnd
        );
    }

    public DragAwayComponentConf<C, E> onDragExit( Action<DragSourceEvent> onDragExit ) {
        return new DragAwayComponentConf<>(
                _component, _event, _enabled, _opacity,
                _onDragEnter, _onDragMove, _onDragOver, _onDropActionChanged, onDragExit, _onDragDropEnd
        );
    }

    public DragAwayComponentConf<C, E> onDragDropEnd( Action<DragSourceDropEvent> onDragDropEnd ) {
        return new DragAwayComponentConf<>(
                _component, _event, _enabled, _opacity,
                _onDragEnter, _onDragMove, _onDragOver, _onDropActionChanged, _onDragExit, onDragDropEnd
        );
    }

}
