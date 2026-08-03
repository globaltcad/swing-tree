package swingtree.style;

import com.google.errorprone.annotations.Immutable;

import java.util.Objects;
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

    static <V> Class<Pooled<V>> classTyped( Class<V> clazz ) {
        Objects.requireNonNull(clazz);
        @SuppressWarnings("unchecked")
        Class<Pooled<V>> pooledClass = (Class<Pooled<V>>) (Class<?>) Pooled.class;
        return pooledClass;
    }

    /*
     *  A lazily computed, cached hash of the wrapped value, with 0 meaning "not computed
     *  yet". A plain non-volatile int rather than an AtomicReference: one of these is
     *  allocated for every render configuration on the paint path, so the extra object,
     *  the Integer boxing and the compare-and-set loop were all pure overhead - a race can
     *  only ever make a second thread recompute the identical value, because the wrapped
     *  value is immutable. A value whose real hash is 0 simply keeps recomputing it, which
     *  costs nothing but the computation it would have done anyway.
     */
    private int _hashCode = 0;
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
        /*
            Two values with different hashes cannot be equal, so an already computed pair of
            them settles the question without the deep comparison below. This is only ever a
            shortcut: a hash still being 0 (not computed, or genuinely 0) merely skips it.
        */
        final int thisHash  = this._hashCode;
        final int otherHash = other._hashCode;
        if ( thisHash != 0 && otherHash != 0 && thisHash != otherHash )
            return false;
        return Objects.equals(this.value, other.value);
    }

    @Override
    public int hashCode() {
        int hash = _hashCode;
        if ( hash == 0 )
            _hashCode = hash = Objects.hashCode(value);
        return hash;
    }

    @Override
    public String toString() {
        return "PooledObject[" + "value=" + value + ']';
    }
}
