package swingtree.style;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

final class LazyRef<T>
{
    private final CacheProducerAndValidator<T> _producerAndValidator;
    private @Nullable T _value;


    LazyRef(CacheProducerAndValidator<T> producerAndValidator) {
        _producerAndValidator = Objects.requireNonNull(producerAndValidator);
    }

    final T getFor(BoxModelConf currentState, ComponentAreas context ) {
        if ( _value == null )
            _value = _producerAndValidator.produce(currentState, context);
        return _value;
    }

    final boolean exists() {
        return _value != null;
    }
}
