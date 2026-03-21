package swingtree.style;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

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
