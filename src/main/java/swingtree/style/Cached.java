package swingtree.style;

abstract class Cached<T>
{
    private T _value;


    public Cached() {}

    public final boolean update( StyleRenderState oldState, StyleRenderState newState ) {
        if ( leadsToSameValue(oldState, newState) ) {
            _value = null;
            return true;
        }
        return false;
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
