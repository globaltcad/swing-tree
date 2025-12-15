package swingtree.style;

import com.google.errorprone.annotations.Immutable;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 *  A wrapper designed for larger immutable value objects (typically config objects
 *  used as cache keys), which can be put to and retrieved from an internal
 *  object poll through its {@link #intern()} method. This is conceptually
 *  identical to {@link String#intern()}...<br>
 *  In practice this is a wrapper over a value object, which extends the value semantics
 *  of the thing it wraps through composition but note that at the same time it has
 *  its own reference identity to be used in a pool of weakly referenced objects.<br>
 *  <b>So although this type is treated as a value semantically
 *  (it overrides {@link #hashCode()} and {@link #equals(Object)})
 *  it still needs to be referenced in order to effectively act as a shared pointer
 *  and must not be converted into a full-blown value object!</b>
 *
 * @param <V> The type of the value object to store in an object pool
 *            to achieve one instance for all such values equal to each other...
 */
@Immutable
@SuppressWarnings("Immutable")
final class Pooled<V> {
    private final AtomicReference<@Nullable Integer> _hashCache = new AtomicReference<>();
    private final V value;

    public Pooled( V value ) {
        this.value = Objects.requireNonNull(value);
    }

    public V get() {
        return this.value;
    }

    /**
     * When the intern method is invoked, if the pool already contains a
     * {@code Pooled} equal to this {@code Pooled} object as determined by
     * the {@link #equals(Object)} method, then the {@code Pooled} instance
     * from the pool is returned. Otherwise, this {@code Pooled} object is
     * added to the pool and a reference to this {@code Pooled} object is returned.
     * <p>
     * It follows that for any two pooled objects {@code s} and {@code t},
     * {@code s.intern() == t.intern()} is {@code true}
     * if and only if {@code s.equals(t)} is {@code true}.
     *
     * @return  a {@code Pooled} that has the same contents as this {@code Pooled}, but is
     *          guaranteed to be from a pool of unique {@code Pooled} instances.
     */
    public Pooled<V> intern() {
        return ObjectPool.get().intern(this);
    }

    public Pooled<V> map( Function<V, V> updater ) {
        return new Pooled<>(updater.apply(value));
    }

    @Override
    public boolean equals( Object o ) {
        if (o == null || getClass() != o.getClass()) return false;
        Pooled<?> other = (Pooled<?>) o;
        Integer otherHash = other._hashCache.get();
        Integer thisHash = this._hashCache.get();
        if ( thisHash != null && otherHash != null ) {
            if ( !Objects.equals(thisHash, otherHash) )
                return false;
        }
        return Objects.equals(this.value, other.value);
    }

    @Override
    public int hashCode() {
        Integer hash = _hashCache.get();
        if ( hash != null )
            return hash;
        hash = Objects.hashCode(value);
        _hashCache.set(hash);
        return hash;
    }

    @Override
    public String toString() {
        return "PooledObject[" + "value=" + value + ']';
    }
}
