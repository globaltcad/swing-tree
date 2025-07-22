package swingtree;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sprouts.Association;
import sprouts.Pair;

import javax.swing.*;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
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

    public BuiltCells<C,E> addRenderLookups(Association<Class<?>, CellBuilder.CellView<C>> additionalLookups) {
        Association<Class<?>, CellBuilder.CellView<C>> rendererLookup = _rendererLookup;
        for ( Pair<Class<?>, CellBuilder.CellView<C>> entry : additionalLookups.entrySet() ) {
            if ( !rendererLookup.containsKey(entry.first()) )
                rendererLookup = rendererLookup.put(entry.first(), entry.second());
            else {
                CellBuilder.CellView<C> existing = rendererLookup.get(entry.first()).get();
                existing._configurators.addAll(0, entry.second()._configurators);
            }
        }
        Optional<CellBuilder.CellView<C>> forObject = rendererLookup.get(Object.class);
        if ( forObject.isPresent() ) {
            rendererLookup = rendererLookup.remove(Object.class);
            rendererLookup = rendererLookup.put(Object.class, forObject.get());
            // The 'Object' type always needs to be last when rendering!
        }
        return new BuiltCells<>(_componentType, _elementType, rendererLookup);
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
