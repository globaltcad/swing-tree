package swingtree.style;

import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *  An immutable value container that stores {@link NamedConf} instances
 *  representing a mapping of unique string names to styles of type {@link S}.
 *  The {@link NamedConf} instances are stored in an array and can be accessed
 *  by their unique name.
 *  Yes, this class could have been a linked hashmap or treemap
 *  however, we do not expect the existence of more than a handful
 *  of named styles in a {@link StyleConf} instance which is why we chose
 *  to use an array instead as it is more memory as well as CPU efficient
 *  to just iterate over a few array elements than to use a hashmap or treemap.
 *
 * @param <S> The type of the style.
 */
final class NamedConfigs<S> implements Simplifiable<NamedConfigs<S>>
{
    private static final NamedConfigs<?> EMPTY = new NamedConfigs<>();

    static <S> NamedConfigs<S> of(NamedConf<S> defaultStyle ) {
        return new NamedConfigs<>( defaultStyle );
    }

    static <S> NamedConfigs<S> empty() { return (NamedConfigs<S>) EMPTY; }

    private final NamedConf<S>[] _styles;


    @SafeVarargs
    private NamedConfigs(NamedConf<S>... styles ) {
        _styles = Objects.requireNonNull(styles);
        // No nll entries:
        for ( NamedConf<S> style : styles )
            Objects.requireNonNull(style);

        // No duplicate names:
        Set<String> names = new HashSet<>(styles.length * 2);
        for ( NamedConf<S> style : styles )
            if ( !names.add(style.name()) )
                throw new IllegalArgumentException("Duplicate style name: " + style.name());
    }

    public int size() { return _styles.length; }

    public List<NamedConf<S>> namedStyles() { return Collections.unmodifiableList(Arrays.asList(_styles)); }

    public Stream<S> stylesStream() {
        return namedStyles()
                .stream()
                .map(NamedConf::style);
    }

    public NamedConfigs<S> withNamedStyle(String name, S style ) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(style);

        int foundIndex = _findNamedStyle(name);

        if ( foundIndex == -1 ) {
            NamedConf<S>[] styles = Arrays.copyOf(_styles, _styles.length + 1);
            styles[styles.length - 1] = NamedConf.of(name, style);
            return new NamedConfigs<>(styles);
        }

        NamedConf<S>[] styles = Arrays.copyOf(_styles, _styles.length);
        styles[foundIndex] = NamedConf.of(name, style);
        return new NamedConfigs<>(styles);
    }

    public NamedConfigs<S> mapStyles(Function<S,S> f ) {
        Objects.requireNonNull(f);
        return mapNamedStyles( ns -> NamedConf.of(ns.name(), f.apply(ns.style())) );
    }

    public NamedConfigs<S> mapNamedStyles(Function<NamedConf<S>, NamedConf<S>> f ) {
        Objects.requireNonNull(f);

        NamedConf<S>[] newStyles = null;
        for ( int i = 0; i < _styles.length; i++ ) {
            NamedConf<S> mapped = f.apply(_styles[i]);
            if ( newStyles == null && mapped != _styles[i] ) {
                newStyles = Arrays.copyOf(_styles, _styles.length);
                // We avoid heap allocation if possible!
            }
            if ( newStyles != null )
                newStyles[i] = mapped;
        }
        if ( newStyles == null )
            return this;

        return new NamedConfigs<>(newStyles);
    }

    private int _findNamedStyle( String name ) {
        for ( int i = 0; i < _styles.length; i++ ) {
            if ( _styles[i].name().equals(name) )
                return i;
        }
        return -1;
    }

    public @Nullable S get(String name ) {
        Objects.requireNonNull(name);

        int foundIndex = _findNamedStyle(name);

        if ( foundIndex == -1 )
            return null;

        return _styles[foundIndex].style();
    }

    public Optional<S> find( String name ) {
        Objects.requireNonNull(name);
        return Optional.ofNullable(get(name));
    }

    public List<S> sortedByNames() {
        return Collections.unmodifiableList(
                    namedStyles()
                    .stream()
                    .sorted(Comparator.comparing(NamedConf::name))
                    .map(NamedConf::style)
                    .collect(Collectors.toList())
                );
    }

    /**
     * @param namedStyleTester The predicate to test the named styles against.
     * @return True if at least one of the named styles in this instance passes the test.
     */
    public boolean any( Predicate<NamedConf<S>> namedStyleTester ) {
        return Arrays.stream(_styles).anyMatch(namedStyleTester);
    }

    public String toString( String defaultName, String styleType ) {
        if ( styleType.isEmpty() )
            styleType = this.getClass().getSimpleName();
        else
            styleType += "=";
        if ( this.size() == 1 )
            return String.valueOf(this.get(defaultName));
        else
            return this.namedStyles()
                    .stream()
                    .map(e -> e.name() + "=" + e.style())
                    .collect(Collectors.joining(", ", styleType+"[", "]"));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName()).append("[");
        for ( int i = 0; i < _styles.length; i++ ) {
            sb.append(_styles[i].name()).append("=").append(_styles[i].style());
            if ( i < _styles.length - 1 )
                sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
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
        NamedConfigs<?> rhs = (NamedConfigs<?>) obj;
        return Arrays.equals(_styles, rhs._styles);
    }

    @Override
    public NamedConfigs<S> simplified() {
        return mapNamedStyles(NamedConf::simplified);
    }
}
