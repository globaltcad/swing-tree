package swingtree.style;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/**
 *  The process-wide weak interner backing {@link Pooled#intern()} — conceptually
 *  {@link String#intern()} for SwingTree's large immutable configuration objects.
 *  It collapses every {@link Pooled} that is {@code equal} to one already seen onto a
 *  single <em>canonical</em> instance, so equal config values can be compared by
 *  reference and, crucially, share a single entry in the various {@link WeakHashMap}-based
 *  rendering caches (style layers, noise, shadows, text images, layouts, component areas).
 *
 *  <h2>Why both the keys and the values are weak</h2>
 *  The backing map holds its keys weakly (it is a {@link WeakHashMap}) <em>and</em> wraps
 *  each canonical value in a {@link WeakReference}. A canonical {@link Pooled} therefore
 *  stays interned only while something <em>outside</em> the pool still references it
 *  strongly; once the last such reference is dropped it is reclaimed and falls out of the
 *  pool on its own. This is the linchpin of SwingTree's self-clearing cache design: a
 *  rendering cache keyed by these canonical instances keeps an entry alive for exactly as
 *  long as a live component or style configuration still holds the corresponding key, and
 *  not a moment longer. The pool itself pins nothing.
 *
 *  <h2>Threading</h2>
 *  Interning happens on the Event Dispatch Thread, like all of SwingTree's painting and
 *  layout work — it is, after all, built on single-threaded Swing. The pool is therefore a
 *  plain, unsynchronized {@link WeakHashMap}, consistent with every cache that funnels its
 *  keys through it.
 */
final class ObjectPool {
    private static final ObjectPool INSTANCE = new ObjectPool();

    static ObjectPool get() {
        return INSTANCE;
    }

    private final Map<Pooled<?>, WeakReference<Pooled<?>>> pool = new WeakHashMap<>();

    private ObjectPool(){}

    public <T> Pooled<T> intern(Pooled<T> value ) {
        WeakReference<Pooled<T>> ref = (WeakReference) pool.get(value);

        if (ref != null) {
            Pooled<T> canonical = ref.get();
            if (canonical != null) {
                return canonical;
            }
        }
        pool.put(value, new WeakReference<>(value));
        return value;
    }
}
