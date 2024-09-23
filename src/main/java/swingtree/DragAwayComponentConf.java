package swingtree;

import org.jspecify.annotations.Nullable;
import sprouts.Action;
import swingtree.api.IconDeclaration;

import javax.swing.*;
import java.awt.Image;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.util.Objects;
import java.util.Optional;

public final class DragAwayComponentConf<C extends JComponent, E>
{
    private static final Action NO_ACTION = e -> {};

    public static <C extends JComponent, E> DragAwayComponentConf<C, E> of(
        C component,
        E event
    ) {
        return new DragAwayComponentConf<>(
                component, event, false, 1, UI.Cursor.DEFAULT, null,
                NO_ACTION, NO_ACTION, NO_ACTION, NO_ACTION, NO_ACTION, NO_ACTION
        );
    }

    private final C               _component;
    private final E               _event;
    private final boolean         _enabled;
    private final double          _opacity;
    private final UI.Cursor       _cursor;
    private final @Nullable Image _customDragImage;
    private final Action<DragSourceDragEvent> _onDragEnter;
    private final Action<DragSourceDragEvent> _onDragMove;
    private final Action<DragSourceDragEvent> _onDragOver;
    private final Action<DragSourceDragEvent> _onDropActionChanged;
    private final Action<DragSourceEvent>     _onDragExit;
    private final Action<DragSourceDropEvent> _onDragDropEnd;


    private DragAwayComponentConf(
        C               component,
        E               event,
        boolean         enabled,
        double          opacity,
        UI.Cursor       cursor,
        @Nullable Image customDragImage,
        Action<DragSourceDragEvent> onDragEnter,
        Action<DragSourceDragEvent> onDragMove,
        Action<DragSourceDragEvent> onDragOver,
        Action<DragSourceDragEvent> onDropActionChanged,
        Action<DragSourceEvent>     onDragExit,
        Action<DragSourceDropEvent> onDragDropEnd
    ) {
        _component           = Objects.requireNonNull(component);
        _event               = Objects.requireNonNull(event);
        _enabled             = enabled;
        _opacity             = opacity;
        _cursor              = Objects.requireNonNull(cursor);
        _customDragImage     = customDragImage;
        _onDragEnter         = Objects.requireNonNull(onDragEnter);
        _onDragMove          = Objects.requireNonNull(onDragMove);
        _onDragOver          = Objects.requireNonNull(onDragOver);
        _onDropActionChanged = Objects.requireNonNull(onDropActionChanged);
        _onDragExit          = Objects.requireNonNull(onDragExit);
        _onDragDropEnd       = Objects.requireNonNull(onDragDropEnd);
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

    public UI.Cursor cursor() {
        return _cursor;
    }

    public Optional<Image> customDragImage() {
        return Optional.ofNullable(_customDragImage);
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
                _component, _event, enabled, _opacity, _cursor, _customDragImage,
                _onDragEnter, _onDragMove, _onDragOver, _onDropActionChanged, _onDragExit, _onDragDropEnd
            );
    }

    public DragAwayComponentConf<C, E> opacity( double opacity ) {
        return new DragAwayComponentConf<>(
                _component, _event, _enabled, opacity, _cursor, _customDragImage,
                _onDragEnter, _onDragMove, _onDragOver, _onDropActionChanged, _onDragExit, _onDragDropEnd
        );
    }

    public DragAwayComponentConf<C, E> cursor( UI.Cursor cursor ) {
        return new DragAwayComponentConf<>(
                _component, _event, _enabled, _opacity, cursor, _customDragImage,
                _onDragEnter, _onDragMove, _onDragOver, _onDropActionChanged, _onDragExit, _onDragDropEnd
        );
    }

    public DragAwayComponentConf<C, E> customDragImage( Image customDragImage ) {
        Objects.requireNonNull(customDragImage);
        return new DragAwayComponentConf<>(
                _component, _event, _enabled, _opacity, _cursor, customDragImage,
                _onDragEnter, _onDragMove, _onDragOver, _onDropActionChanged, _onDragExit, _onDragDropEnd
        );
    }

    public DragAwayComponentConf<C, E> customDragImage( IconDeclaration icon ) {
        Objects.requireNonNull(icon);
        return icon.find().map(ImageIcon::getImage).map(this::customDragImage).orElse(this);
    }

    public DragAwayComponentConf<C, E> customDragImage( String path ) {
        return customDragImage(IconDeclaration.of(path));
    }

    public DragAwayComponentConf<C, E> onDragEnter( Action<DragSourceDragEvent> onDragEnter ) {
        return new DragAwayComponentConf<>(
                _component, _event, _enabled, _opacity, _cursor, _customDragImage,
                onDragEnter, _onDragMove, _onDragOver, _onDropActionChanged, _onDragExit, _onDragDropEnd
        );
    }

    public DragAwayComponentConf<C, E> onDragMove( Action<DragSourceDragEvent> onDragMove ) {
        return new DragAwayComponentConf<>(
                _component, _event, _enabled, _opacity, _cursor, _customDragImage,
                _onDragEnter, onDragMove, _onDragOver, _onDropActionChanged, _onDragExit, _onDragDropEnd
        );
    }

    public DragAwayComponentConf<C, E> onDragOver( Action<DragSourceDragEvent> onDragOver ) {
        return new DragAwayComponentConf<>(
                _component, _event, _enabled, _opacity, _cursor, _customDragImage,
                _onDragEnter, _onDragMove, onDragOver, _onDropActionChanged, _onDragExit, _onDragDropEnd
        );
    }

    public DragAwayComponentConf<C, E> onDropActionChanged( Action<DragSourceDragEvent> onDropActionChanged ) {
        return new DragAwayComponentConf<>(
                _component, _event, _enabled, _opacity, _cursor, _customDragImage,
                _onDragEnter, _onDragMove, _onDragOver, onDropActionChanged, _onDragExit, _onDragDropEnd
        );
    }

    public DragAwayComponentConf<C, E> onDragExit( Action<DragSourceEvent> onDragExit ) {
        return new DragAwayComponentConf<>(
                _component, _event, _enabled, _opacity, _cursor, _customDragImage,
                _onDragEnter, _onDragMove, _onDragOver, _onDropActionChanged, onDragExit, _onDragDropEnd
        );
    }

    public DragAwayComponentConf<C, E> onDragDropEnd( Action<DragSourceDropEvent> onDragDropEnd ) {
        return new DragAwayComponentConf<>(
                _component, _event, _enabled, _opacity, _cursor, _customDragImage,
                _onDragEnter, _onDragMove, _onDragOver, _onDropActionChanged, _onDragExit, onDragDropEnd
        );
    }

}
