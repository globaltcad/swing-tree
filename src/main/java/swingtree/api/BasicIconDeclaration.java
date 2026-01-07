package swingtree.api;

import com.google.errorprone.annotations.Immutable;
import org.jspecify.annotations.Nullable;
import swingtree.layout.Size;

import java.util.Objects;
import java.util.Optional;

/**
 *  A basic implementation of the {@link IconDeclaration} interface.
 */
@Immutable
final class BasicIconDeclaration implements IconDeclaration
{
    private final @Nullable Size size;
    private final String path;


    public BasicIconDeclaration( @Nullable Size size, String path ) {
        this.size = size;
        this.path = path;
    }

    @Override
    public Optional<Size> size() {
        return Optional.ofNullable(size);
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"["+
                    "size=" + ( size().map(it->it.equals(Size.unknown()) ? "?" : String.valueOf(it) ).orElse("?") ) + ", " +
                    "path='" + path() + "'" +
                "]";
    }

    @Override public int hashCode() {
        return Objects.hash(path(), size());
    }

    @Override public boolean equals( Object other ) {
        if ( other == this ) return true;
        if ( other == null ) return false;
        if ( other.getClass() != this.getClass() ) return false;
        IconDeclaration that = (IconDeclaration) other;
        return Objects.equals(this.path(), that.path())
                && Objects.equals(this.size(), that.size());
    }
}
