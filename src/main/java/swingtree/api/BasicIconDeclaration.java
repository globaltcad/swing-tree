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
    private final SourceFormat sourceFormat;
    private final String source;


    public BasicIconDeclaration( @Nullable Size size, SourceFormat sourceFormat, String source) {
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
        return Objects.hash(source, sourceFormat, size);
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
