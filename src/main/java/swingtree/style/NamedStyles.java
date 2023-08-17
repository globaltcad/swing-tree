package swingtree.style;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *  An immutable value container that stores {@link NamedStyle} instances
 *  representing a mapping of unique string names to styles of type {@link S}.
 *  The {@link NamedStyle} instances are stored in an array and can be accessed
 *  by their unique name.
 *  Yes, this class could have been a linked hashmap or treemap
 *  however, we do not expect the existence of more than a handful
 *  of named styles in a {@link Style} instance which is why we chose
 *  to use an array instead as it is more memory as well as CPU efficient
 *  to just iterate over a few array elements than to use a hashmap or treemap.
 *
 * @param <S> The type of the style.
 */
class NamedStyles<S>
{
    private static final NamedStyles<?> EMPTY = new NamedStyles<>();

    static <S> NamedStyles<S> of( NamedStyle<S> defaultStyle ) { return new NamedStyles<>( defaultStyle ); }

    static <S> NamedStyles<S> empty() { return (NamedStyles<S>) EMPTY; }

    private final NamedStyle<S>[] _styles;

    @SafeVarargs
    private NamedStyles( NamedStyle<S>... styles ) {
        _styles = Objects.requireNonNull(styles);
        // No nll entries:
        for ( NamedStyle<S> style : styles )
            Objects.requireNonNull(style);

        // No duplicate names:
        Set<String> names = new HashSet<>(styles.length * 2);
        for ( NamedStyle<S> style : styles )
            if ( !names.add(style.name()) )
                throw new IllegalArgumentException("Duplicate style name: " + style.name());
    }

    public int size() { return _styles.length; }

    public List<NamedStyle<S>> namedStyles() { return Collections.unmodifiableList(Arrays.asList(_styles)); }

    public Stream<S> stylesStream() {
        return namedStyles()
                .stream()
                .map(NamedStyle::style);
    }

    public NamedStyles<S> withNamedStyle( String name, S style ) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(style);

        int foundIndex = _findNamedStyle(name);

        if ( foundIndex == -1 ) {
            NamedStyle<S>[] styles = Arrays.copyOf(_styles, _styles.length + 1);
            styles[styles.length - 1] = NamedStyle.of(name, style);
            return new NamedStyles<>(styles);
        }

        NamedStyle<S>[] styles = Arrays.copyOf(_styles, _styles.length);
        styles[foundIndex] = NamedStyle.of(name, style);
        return new NamedStyles<>(styles);
    }

    public NamedStyles<S> mapStyles( Function<S,S> f ) {
        Objects.requireNonNull(f);

        NamedStyle<S>[] styles = Arrays.copyOf(_styles, _styles.length);
        for ( int i = 0; i < styles.length; i++ )
            styles[i] = NamedStyle.of(styles[i].name(), f.apply(styles[i].style()));

        return new NamedStyles<>(styles);
    }

    private int _findNamedStyle( String name ) {
        for ( int i = 0; i < _styles.length; i++ ) {
            if ( _styles[i].name().equals(name) )
                return i;
        }
        return -1;
    }

    public S get( String name ) {
        Objects.requireNonNull(name);

        int foundIndex = _findNamedStyle(name);

        if ( foundIndex == -1 )
            return null;

        return _styles[foundIndex].style();
    }

    public Optional<S> style( String name ) {
        Objects.requireNonNull(name);
        return Optional.ofNullable(get(name));
    }

    public List<S> sortedByNamesAndFilteredBy( Predicate<S> filter ) {
        return Collections.unmodifiableList(
                    namedStyles()
                    .stream()
                    .sorted(Comparator.comparing(NamedStyle::name))
                    .map(NamedStyle::style)
                    .filter( filter )
                    .collect(Collectors.toList())
                );
    }

    public String toString( String defaultName, String styleType ) {
        if ( this.size() == 1 )
            return this.get(defaultName).toString();
        else
            return this.namedStyles()
                    .stream()
                    .map(e -> e.name() + ": " + e.style())
                    .collect(Collectors.joining(", ", styleType+"=[", "]"));
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(_styles);
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        NamedStyles<?> rhs = (NamedStyles<?>) obj;
        return Arrays.equals(_styles, rhs._styles);
    }
}
