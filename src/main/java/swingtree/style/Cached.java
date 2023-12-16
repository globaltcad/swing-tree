package swingtree.style;

/**
 *  A wrapper for a value that is produced from a {@link ComponentConf} through
 *  a {@link #produce(ComponentConf)} implementation and validated against
 *  a previous {@link ComponentConf} through a {@link #leadsToSameValue(ComponentConf, ComponentConf)}
 *  implementation.
 *  <p>
 *  This design is possible due to the fact that the {@link ComponentConf} is deeply immutable
 *  and can be used as a key data structure for caching.
 *
 * @param <T> The type of the cached value.
 */
abstract class Cached<T>
{
    private T _value;


    public Cached() {}

    public final void validate(ComponentConf oldState, ComponentConf newState ) {
        if ( _value != null && !leadsToSameValue(oldState, newState) ) {
            _value = null;
        }
    }

    public final T getFor(ComponentConf currentState ) {
        if ( _value == null )
            _value = produce(currentState);
        return _value;
    }

    public final boolean exists() {
        return _value != null;
    }

    protected abstract T produce( ComponentConf currentState );

    public abstract boolean leadsToSameValue(ComponentConf oldState, ComponentConf newState );
}
