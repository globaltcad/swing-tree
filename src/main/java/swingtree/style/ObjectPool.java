package swingtree.style;

import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

final class ObjectPool {
    private static @Nullable ObjectPool INSTANCE = null;

    static ObjectPool get() {
        if ( INSTANCE == null )
            INSTANCE = new ObjectPool();
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
