package swingtree.style;

/**
 *  A wrapper for a value that is produced from a {@link StyleRenderState} through
 *  a {@link #produce(StyleRenderState)} implementation and validated against
 *  a previous {@link StyleRenderState} through a {@link #leadsToSameValue(StyleRenderState, StyleRenderState)}
 *  implementation.
 *  <p>
 *  This design is possible due to the fact that the {@link StyleRenderState} is deeply immutable
 *  and can be used as a key data structure for caching.
 *
 * @param <T> The type of the cached value.
 */
abstract class Cached<T>
{
    private T _value;


    public Cached() {}

    public final void validate( StyleRenderState oldState, StyleRenderState newState ) {
        //if ( _value != null && !leadsToSameValue(oldState, newState) ) {
            _value = null;
        //}
    }

    public final T getFor(StyleRenderState currentState ) {
        if ( _value == null )
            _value = produce(currentState);
        return _value;
    }

    public final boolean exists() {
        return _value != null;
    }

    protected abstract T produce( StyleRenderState currentState );

    public abstract boolean leadsToSameValue(StyleRenderState oldState, StyleRenderState newState );
}
