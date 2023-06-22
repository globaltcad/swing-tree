package swingtree.style;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public final class ForegroundStyle
{
    private static final ForegroundStyle _NONE = new ForegroundStyle(null, Collections.singletonMap(StyleUtility.DEFAULT_KEY, Painter.none()));

    public static ForegroundStyle none() { return _NONE; }

    private final Color _color;
    private final Map<String, Painter> _painters = new TreeMap<>();


    private ForegroundStyle(
        Color color,
        Map<String, Painter> painters
    ) {
        _color   = color;
        _painters.putAll(painters);
    }

    public Optional<Color> color() { return Optional.ofNullable(_color); }

    /**
     * @return An unmodifiable list of painters sorted by their names in ascending alphabetical order.
     */
    public List<Painter> painters() {
        return Collections.unmodifiableList(
                        _painters
                             .entrySet()
                             .stream()
                             .sorted(Map.Entry.comparingByKey())
                             .map(Map.Entry::getValue)
                             .collect(Collectors.toList())
                    );
    }

    public boolean hasPainters() { return !_painters.isEmpty(); }

    ForegroundStyle color( Color color ) { return new ForegroundStyle(color, _painters); }

    ForegroundStyle painter( Map<String, Painter> painters ) { return new ForegroundStyle(_color, painters); }

    ForegroundStyle painter( String painterName, Painter painter ) {
        Objects.requireNonNull(painterName);
        Objects.requireNonNull(painter);
        // We clone the painter map:
        Map<String, Painter> newPainters = new HashMap<>(_painters);
        newPainters.put(painterName, painter); // Overwrites any existing painter with the same name.
        return painter(newPainters);
    }

    @Override
    public int hashCode() { return Objects.hash(_color, StyleUtility.mapHash(_painters)); }

    @Override
    public boolean equals(Object obj) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        ForegroundStyle rhs = (ForegroundStyle) obj;
        return Objects.equals(_color, rhs._color) &&
               StyleUtility.mapEquals(_painters, rhs._painters);
    }

    @Override
    public String toString() {

        String painterString;
        if ( _painters.size() == 1 )
            painterString = "painter=" + StyleUtility.toString(_painters.values().iterator().next());
        else
            painterString = _painters.values()
                    .stream()
                    .map(StyleUtility::toString)
                    .collect(Collectors.joining(", ", "painters=[", "]"));

        return "ForegroundStyle[" +
                    "color="   + StyleUtility.toString(_color) + ", " +
                    painterString +
                "]";
    }
}
