package swingtree.style;

abstract class Cached<T>
{
    private T _value;


    public Cached() {}

    public void update( StyleRenderState oldState, StyleRenderState newState ) {
        if ( isValid(oldState, newState) )
            _value = null;
    }

    public T getFor(StyleRenderState currentState ) {
        if ( _value == null )
            _value = produce(currentState);
        return _value;
    }

    public boolean exists() {
        return _value != null;
    }

    protected abstract boolean isValid( StyleRenderState oldState, StyleRenderState newState );

    protected abstract T produce( StyleRenderState currentState );
}
