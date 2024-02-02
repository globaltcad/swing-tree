package swingtree.style;

import java.util.Objects;

/**
 *  A wrapper for a value that is produced from a {@link StructureConf} through
 *  a {@link CacheProducerAndValidator#produce(StructureConf, ComponentAreas)} implementation and validated against
 *  a previous {@link StructureConf} through a
 *  {@link CacheProducerAndValidator#leadsToSameValue(StructureConf, StructureConf, ComponentAreas)}
 *  implementation.
 *  <p>
 *  This design is possible due to the fact that the {@link StructureConf} is deeply immutable
 *  and can be used as a key data structure for caching.
 *
 * @param <T> The type of the cached value.
 */
final class Cached<T> implements CacheProducerAndValidator<T>
{
    private final CacheProducerAndValidator<T> _producerAndValidator;
    private T _value;


    Cached( CacheProducerAndValidator<T> producerAndValidator) {
        _producerAndValidator = Objects.requireNonNull(producerAndValidator);
    }

    final Cached<T> validate( StructureConf oldState, StructureConf newState, ComponentAreas context ) {
        if ( _value != null && !_producerAndValidator.leadsToSameValue(oldState, newState, context) ) {
            return new Cached<>(_producerAndValidator);
        }
        return this;
    }

    final T getFor( StructureConf currentState, ComponentAreas context ) {
        if ( _value == null )
            _value = _producerAndValidator.produce(currentState, context);
        return _value;
    }

    final boolean exists() {
        return _value != null;
    }

    @Override
    public T produce(StructureConf currentState, ComponentAreas context) {
        return _producerAndValidator.produce(currentState, context);
    }

    @Override
    public boolean leadsToSameValue(StructureConf oldState, StructureConf newState, ComponentAreas context) {
        return _producerAndValidator.leadsToSameValue(oldState, newState, context);
    }

}
