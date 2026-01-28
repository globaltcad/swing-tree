package swingtree.style;

import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

final class LazyRef<T>
{
    private final Object _source;
    private final Function<Object, T> _producer;
    private @Nullable T _value;

    @SuppressWarnings("unchecked")
    <S> LazyRef(S source, Function<S, T> producer) {
        Objects.requireNonNull(producer);
        _source = Objects.requireNonNull(source);
        _producer = (Function<Object, T>) producer;
    }

    final T get() {
        if ( _value == null )
            _value = _producer.apply(_source);
        return _value;
    }

    final boolean exists() {
        return _value != null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        LazyRef<?> lazyRef = (LazyRef<?>) o;
        return Objects.equals(_source, lazyRef._source) &&
                Objects.equals(_producer, lazyRef._producer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_source, _producer);
    }
}
