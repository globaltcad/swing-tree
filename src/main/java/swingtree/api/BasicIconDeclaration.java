package swingtree.api;

import com.google.errorprone.annotations.Immutable;
import org.jspecify.annotations.Nullable;
import swingtree.layout.Size;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 *  A basic implementation of the {@link IconDeclaration} interface
 *  with object pooling built in. So repeated calls to the {@link #of(Size, SourceFormat, String)}
 *  factory method with the same values, will always return the same instance.
 *  This is because the {@link IconDeclaration} is used as key in weak hash map based caches.
 */
@Immutable
@SuppressWarnings("Immutable")
final class BasicIconDeclaration implements IconDeclaration
{
    /**
     * Implementations of {@link IconDeclaration} are used as weak cache keys
     * in the style engine as well as the image cache... So we pool instances
     * of this in order to ensure that cache entries are not cleared too early.<br>
     * This optimizes both cache hits as well as memory consumption!
     */
    private static final Map<BasicIconDeclaration, WeakReference<BasicIconDeclaration>> POOL = new WeakHashMap<>();

    private static BasicIconDeclaration intern( BasicIconDeclaration value ) {
        WeakReference<BasicIconDeclaration> ref = POOL.get(value);
        if (ref != null) {
            BasicIconDeclaration canonical = ref.get();
            if (canonical != null) {
                return canonical;
            }
        }
        POOL.put(value, new WeakReference<>(value));
        return value;
    }
    
    private final @Nullable Size size;
    private final SourceFormat sourceFormat;
    private final String source;
    private final AtomicReference<@Nullable Integer> _hashCache = new AtomicReference<>();
    

    public static BasicIconDeclaration of( @Nullable Size size, SourceFormat sourceFormat, String source) {
        return intern(new BasicIconDeclaration( size, sourceFormat, source ));
    }

    private BasicIconDeclaration( @Nullable Size size, SourceFormat sourceFormat, String source) {
        this.size = size;
        this.sourceFormat = sourceFormat;
        this.source = source;
    }

    @Override
    public Optional<Size> size() {
        return Optional.ofNullable(size);
    }

    @Override
    public String source() {
        return source;
    }

    @Override
    public SourceFormat sourceFormat() {
        return sourceFormat;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"["+
                    "size=" + ( size().map(it->it.equals(Size.unknown()) ? "?" : String.valueOf(it) ).orElse("?") ) + ", " +
                    "sourceFormat=" + sourceFormat + ", " +
                    "source='" + source + "'" +
                "]";
    }

    @Override public int hashCode() {
        return Objects.requireNonNull(
                _hashCache.updateAndGet(h -> h != null ? h : Objects.hash(source, sourceFormat, size))
            );
    }

    @Override public boolean equals( Object other ) {
        if ( other == this ) return true;
        if ( other == null ) return false;
        if ( other.getClass() != this.getClass() ) return false;
        IconDeclaration that = (IconDeclaration) other;
        return Objects.equals(this.source(), that.source())
                && Objects.equals(this.sourceFormat(), that.sourceFormat())
                && Objects.equals(this.size(), that.size());
    }
}
