package swingtree.style;

import java.util.Objects;
import java.util.Optional;

public final class DimensionalityStyle
{
    private static final DimensionalityStyle _NONE = new DimensionalityStyle(
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null
                                                    );

    public static DimensionalityStyle none() { return _NONE; }

    private final Integer _minWidth;
    private final Integer _minHeight;

    private final Integer _maxWidth;
    private final Integer _maxHeight;

    private final Integer _preferredWidth;
    private final Integer _preferredHeight;


    public DimensionalityStyle(
        Integer minWidth,
        Integer minHeight,
        Integer maxWidth,
        Integer maxHeight,
        Integer preferredWidth,
        Integer preferredHeight
    ) {
        _minWidth        = minWidth;
        _minHeight       = minHeight;
        _maxWidth        = maxWidth;
        _maxHeight       = maxHeight;
        _preferredWidth  = preferredWidth;
        _preferredHeight = preferredHeight;
    }

    DimensionalityStyle _withMinWidth( Integer minWidth ) {
        return new DimensionalityStyle(minWidth, _minHeight, _maxWidth, _maxHeight, _preferredWidth, _preferredHeight);
    }

    DimensionalityStyle _withMinHeight( Integer minHeight ) {
        return new DimensionalityStyle(_minWidth, minHeight, _maxWidth, _maxHeight, _preferredWidth, _preferredHeight);
    }

    DimensionalityStyle _withMaxWidth( Integer maxWidth ) {
        return new DimensionalityStyle(_minWidth, _minHeight, maxWidth, _maxHeight, _preferredWidth, _preferredHeight);
    }

    DimensionalityStyle _withMaxHeight( Integer maxHeight ) {
        return new DimensionalityStyle(_minWidth, _minHeight, _maxWidth, maxHeight, _preferredWidth, _preferredHeight);
    }

    DimensionalityStyle _withPreferredWidth( Integer preferredWidth ) {
        return new DimensionalityStyle(_minWidth, _minHeight, _maxWidth, _maxHeight, preferredWidth, _preferredHeight);
    }

    DimensionalityStyle _withPreferredHeight( Integer preferredHeight ) {
        return new DimensionalityStyle(_minWidth, _minHeight, _maxWidth, _maxHeight, _preferredWidth, preferredHeight);
    }

    public Optional<Integer> minWidth() { return Optional.ofNullable(_minWidth); }

    public Optional<Integer> minHeight() { return Optional.ofNullable(_minHeight); }

    public Optional<Integer> maxWidth() { return Optional.ofNullable(_maxWidth); }

    public Optional<Integer> maxHeight() { return Optional.ofNullable(_maxHeight); }

    public Optional<Integer> preferredWidth() { return Optional.ofNullable(_preferredWidth); }

    public Optional<Integer> preferredHeight() { return Optional.ofNullable(_preferredHeight); }

    @Override
    public String toString() {
        return String.format(
            "DimensionalityStyle[" +
                    "minWidth=%s, " +
                    "minHeight=%s, " +
                    "maxWidth=%s, " +
                    "maxHeight=%s, " +
                    "preferredWidth=%s, " +
                    "preferredHeight=%s" +
                "]",
            _minWidth,
            _minHeight,
            _maxWidth,
            _maxHeight,
            _preferredWidth,
            _preferredHeight
        );
    }

    @Override
    public final boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( !(o instanceof DimensionalityStyle) ) return false;
        DimensionalityStyle that = (DimensionalityStyle) o;
        return Objects.equals(_minWidth, that._minWidth) &&
               Objects.equals(_minHeight, that._minHeight) &&
               Objects.equals(_maxWidth, that._maxWidth) &&
               Objects.equals(_maxHeight, that._maxHeight) &&
               Objects.equals(_preferredWidth, that._preferredWidth) &&
               Objects.equals(_preferredHeight, that._preferredHeight);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(_minWidth, _minHeight, _maxWidth, _maxHeight, _preferredWidth, _preferredHeight);
    }

}
