package swingtree;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sprouts.Association;

import javax.swing.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

final class BuiltCells<C extends JComponent, E> {

    private static final Logger log = LoggerFactory.getLogger(BuiltCells.class);

    private final Class<C> _componentType;
    private final Class<E> _elementType;
    private final Association<Class<?>, CellBuilder.CellView<C>> _rendererLookup;

    BuiltCells(
            Class<C> componentType,
            Class<E> elementType
    ) {
        this(
            componentType,
            elementType,
            new LinkedHashMap<>()
        );
    }

    BuiltCells(
        Class<C> componentType,
        Class<E> elementType,
        Map<Class<?>, CellBuilder.CellView<C>> rendererLookup
    ) {
        this(
            componentType,
            elementType,
            ((Association) Association.betweenLinked(Class.class, CellBuilder.CellView.class)).putAll(rendererLookup)
        );
    }

    BuiltCells(
        Class<C> componentType,
        Class<E> elementType,
        Association<Class<?>, CellBuilder.CellView<C>> rendererLookup
    ) {
        _componentType = componentType;
        _elementType = elementType;
        _rendererLookup = rendererLookup;
    }

    public Class<C> componentType() {
        return _componentType;
    }

    public Class<E> elementType() {
        return _elementType;
    }

    public Association<Class<?>, CellBuilder.CellView<C>> rendererLookup() {
        return _rendererLookup;
    }

    BuiltCells<C,E> computeIfAbsent(Class<?> type, Supplier<CellBuilder.CellView<C>> cellViewSupplier) {
        if ( _rendererLookup.containsKey(type) ) {
            return this;
        }
        return new BuiltCells<>(
                _componentType,
                _elementType,
                _rendererLookup.put(type, cellViewSupplier.get())
            );
    }

    void checkTypeValidity( @Nullable Object encounteredValue ) {
        if ( encounteredValue != null ) {
            if ( !_elementType.isAssignableFrom(encounteredValue.getClass()) )
                log.debug(
                    "Encountered an unusual cell entry in component '{}'. " +
                    "Expected type '{}', but got '{}'.",
                    _componentType.getSimpleName(),
                    _elementType.getSimpleName(),
                    encounteredValue.getClass().getSimpleName()
                );
        }
    }

}
