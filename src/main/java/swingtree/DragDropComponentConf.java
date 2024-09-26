package swingtree;

import sprouts.Action;

import javax.swing.JComponent;
import java.awt.dnd.*;
import java.util.Objects;

public final class DragDropComponentConf<C extends JComponent>
{
    private static final Action NO_ACTION = e -> {};

    public static <C extends JComponent> DragDropComponentConf<C> of(
        C component
    ) {
        return new DragDropComponentConf<>(
                component,
                NO_ACTION, NO_ACTION, NO_ACTION, NO_ACTION, NO_ACTION
        );
    }

    private final C                           _component;
    private final Action<ComponentDelegate<C,DropTargetDragEvent>> _onDragEnter;
    private final Action<ComponentDelegate<C,DropTargetDragEvent>> _onDragOver;
    private final Action<ComponentDelegate<C,DropTargetDragEvent>> _onDropActionChanged;
    private final Action<ComponentDelegate<C,DropTargetEvent>>     _onDragExit;
    private final Action<ComponentDelegate<C,DropTargetDropEvent>> _onDragDropEnd;

    private DragDropComponentConf(
        C component,
        Action<ComponentDelegate<C,DropTargetDragEvent>> onDragEnter,
        Action<ComponentDelegate<C,DropTargetDragEvent>> onDragOver,
        Action<ComponentDelegate<C,DropTargetDragEvent>> onDropActionChanged,
        Action<ComponentDelegate<C,DropTargetEvent>>     onDragExit,
        Action<ComponentDelegate<C,DropTargetDropEvent>> onDragDropEnd
    ) {
        _component           = Objects.requireNonNull(component);
        _onDragEnter         = Objects.requireNonNull(onDragEnter);
        _onDragOver          = Objects.requireNonNull(onDragOver);
        _onDropActionChanged = Objects.requireNonNull(onDropActionChanged);
        _onDragExit          = Objects.requireNonNull(onDragExit);
        _onDragDropEnd       = Objects.requireNonNull(onDragDropEnd);
    }

    public C component() {
        return _component;
    }

    public DragDropComponentConf<C> onDragEnter(Action<ComponentDelegate<C,DropTargetDragEvent>> action) {
        return new DragDropComponentConf<>(
            _component,
            action,
            _onDragOver,
            _onDropActionChanged,
            _onDragExit,
            _onDragDropEnd
        );
    }

    public DragDropComponentConf<C> onDragOver(Action<ComponentDelegate<C,DropTargetDragEvent>> action) {
        return new DragDropComponentConf<>(
            _component,
            _onDragEnter,
            action,
            _onDropActionChanged,
            _onDragExit,
            _onDragDropEnd
        );
    }

    public DragDropComponentConf<C> onDropActionChanged(Action<ComponentDelegate<C,DropTargetDragEvent>> action) {
        return new DragDropComponentConf<>(
            _component,
            _onDragEnter,
            _onDragOver,
            action,
            _onDragExit,
            _onDragDropEnd
        );
    }

    public DragDropComponentConf<C> onDragExit(Action<ComponentDelegate<C,DropTargetEvent>> action) {
        return new DragDropComponentConf<>(
            _component,
            _onDragEnter,
            _onDragOver,
            _onDropActionChanged,
            action,
            _onDragDropEnd
        );
    }

    public DragDropComponentConf<C> onDragDropEnd(Action<ComponentDelegate<C,DropTargetDropEvent>> action) {
        return new DragDropComponentConf<>(
            _component,
            _onDragEnter,
            _onDragOver,
            _onDropActionChanged,
            _onDragExit,
            action
        );
    }


    DropTarget toNewDropTarget() {
        return new DropTarget(
            _component,
            new DropTargetListener() {
                @Override
                public void dragEnter(DropTargetDragEvent event) {
                    _onDragEnter.accept(new ComponentDelegate<>(_component, event));
                }

                @Override
                public void dragOver(DropTargetDragEvent event) {
                    _onDragOver.accept(new ComponentDelegate<>(_component, event));
                }

                @Override
                public void dropActionChanged(DropTargetDragEvent event) {
                    _onDropActionChanged.accept(new ComponentDelegate<>(_component, event));
                }

                @Override
                public void dragExit(DropTargetEvent event) {
                    _onDragExit.accept(new ComponentDelegate<>(_component, event));
                }

                @Override
                public void drop(DropTargetDropEvent event) {
                    _onDragDropEnd.accept(new ComponentDelegate<>(_component, event));
                }
            }
        );
    }
}
