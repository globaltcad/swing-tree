package swingtree.style;

import java.util.Objects;

/**
 *  A wrapper for a value that is produced from a {@link ComponentConf} through
 *  a {@link AreasCache.CacheLogic#produce(ComponentConf, AreasCache)} implementation and validated against
 *  a previous {@link ComponentConf} through a
 *  {@link AreasCache.CacheLogic#leadsToSameValue(ComponentConf, ComponentConf, AreasCache)}
 *  implementation.
 *  <p>
 *  This design is possible due to the fact that the {@link ComponentConf} is deeply immutable
 *  and can be used as a key data structure for caching.
 *
 * @param <T> The type of the cached value.
 */
final class Cached<T> implements AreasCache.CacheLogic<T>
{
    private final AreasCache.CacheLogic<T> _cacheLogic;
    private T _value;


    public Cached( AreasCache.CacheLogic<T> cacheLogic ) {
        _cacheLogic = Objects.requireNonNull(cacheLogic);
    }

    public final Cached<T> validate( ComponentConf oldState, ComponentConf newState, AreasCache context ) {
        if ( _value != null && !_cacheLogic.leadsToSameValue(oldState, newState, context) ) {
            return new Cached<>(_cacheLogic);
        }
        return this;
    }

    public final T getFor( ComponentConf currentState, AreasCache context ) {
        if ( _value == null )
            _value = _cacheLogic.produce(currentState, context);
        return _value;
    }

    public final boolean exists() {
        return _value != null;
    }

    @Override
    public T produce(ComponentConf currentState, AreasCache context) {
        return _cacheLogic.produce(currentState, context);
    }

    @Override
    public boolean leadsToSameValue(ComponentConf oldState, ComponentConf newState, AreasCache context) {
        return _cacheLogic.leadsToSameValue(oldState, newState, context);
    }
}
