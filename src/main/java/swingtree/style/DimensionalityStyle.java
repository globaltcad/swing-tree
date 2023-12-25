package swingtree.style;

import swingtree.layout.Size;

import java.util.Objects;
import java.util.Optional;

/**
 *  A style that defines the dimensionality of a component
 *  in the form of a minimum, maximum, preferred and regular size.
 *  The layout manager of a component will use this information
 *  to determine the actual size of the component in the layout.
 **/
final class DimensionalityStyle
{
    private static final DimensionalityStyle _NONE = new DimensionalityStyle(
                                                        Size.unknown(),
                                                        Size.unknown(),
                                                        Size.unknown(),
                                                        Size.unknown()
                                                    );

    public static DimensionalityStyle none() { return _NONE; }


    private final Size _minSize;
    private final Size _maxSize;
    private final Size _preferredSize;
    private final Size _size;


    private DimensionalityStyle(
        Size minSize,
        Size maxSize,
        Size preferredSize,
        Size size
    ) {
        _minSize       = Objects.requireNonNull(minSize);
        _maxSize       = Objects.requireNonNull(maxSize);
        _preferredSize = Objects.requireNonNull(preferredSize);
        _size          = Objects.requireNonNull(size);
    }

    DimensionalityStyle _withMinWidth( int minWidth ) {
        return new DimensionalityStyle(_minSize.withWidth(minWidth), _maxSize, _preferredSize, _size);
    }

    DimensionalityStyle _withMinHeight( int minHeight ) {
        return new DimensionalityStyle(_minSize.withHeight(minHeight), _maxSize, _preferredSize, _size);
    }

    DimensionalityStyle _withMinSize( Size minSize ) {
        return new DimensionalityStyle(minSize, _maxSize, _preferredSize, _size);
    }

    DimensionalityStyle _withMaxWidth( int maxWidth ) {
        return new DimensionalityStyle(_minSize, _maxSize.withWidth(maxWidth), _preferredSize, _size);
    }

    DimensionalityStyle _withMaxHeight( int maxHeight ) {
        return new DimensionalityStyle(_minSize, _maxSize.withHeight(maxHeight), _preferredSize, _size);
    }

    DimensionalityStyle _withMaxSize( Size maxSize ) {
        return new DimensionalityStyle(_minSize, maxSize, _preferredSize, _size);
    }

    DimensionalityStyle _withPreferredWidth( int preferredWidth ) {
        return new DimensionalityStyle(_minSize, _maxSize, _preferredSize.withWidth(preferredWidth), _size);
    }

    DimensionalityStyle _withPreferredHeight( int preferredHeight ) {
        return new DimensionalityStyle(_minSize, _maxSize, _preferredSize.withHeight(preferredHeight), _size);
    }

    DimensionalityStyle _withPreferredSize( Size preferredSize ) {
        return new DimensionalityStyle(_minSize, _maxSize, preferredSize, _size);
    }

    DimensionalityStyle _withWidth( int width ) {
        return new DimensionalityStyle(_minSize, _maxSize, _preferredSize, _size.withWidth(width));
    }

    DimensionalityStyle _withHeight( int height ) {
        return new DimensionalityStyle(_minSize, _maxSize, _preferredSize, _size.withHeight(height));
    }

    DimensionalityStyle _withSize( Size size ) {
        return new DimensionalityStyle(_minSize, _maxSize, _preferredSize, size);
    }

    public Optional<Integer> minWidth() { return _minSize.width(); }

    public Optional<Integer> minHeight() { return _minSize.height(); }

    public Optional<Integer> maxWidth() { return _maxSize.width(); }

    public Optional<Integer> maxHeight() { return _maxSize.height(); }

    public Optional<Integer> preferredWidth() { return _preferredSize.width(); }

    public Optional<Integer> preferredHeight() { return _preferredSize.height(); }

    public Optional<Integer> width() { return _size.width(); }

    public Optional<Integer> height() { return _size.height(); }

    DimensionalityStyle _scale( double scale ) {
        return new DimensionalityStyle(
                    _minSize.scale(scale),
                    _maxSize.scale(scale),
                    _preferredSize.scale(scale),
                    _size.scale(scale)
                );
    }

    DimensionalityStyle simplified() {
        if ( this == _NONE )
            return _NONE;

        Size simplifiedMinSize       = _minSize.width().orElse(0) == 0 && _minSize.height().orElse(0) == 0 ? _NONE._minSize : _minSize;
        Size simplifiedMaxSize       = _maxSize.width().orElse(0) == 0 && _maxSize.height().orElse(0) == 0 ? _NONE._maxSize : _maxSize;
        Size simplifiedPreferredSize = _preferredSize.width().orElse(0) == 0 && _preferredSize.height().orElse(0) == 0 ? _NONE._preferredSize : _preferredSize;
        Size simplifiedSize          = _size.width().orElse(0) == 0 && _size.height().orElse(0) == 0 ? _NONE._size : _size;

        if (
            simplifiedMinSize       == _NONE._minSize &&
            simplifiedMaxSize       == _NONE._maxSize &&
            simplifiedPreferredSize == _NONE._preferredSize &&
            simplifiedSize          == _NONE._size
        )
            return _NONE;

        return new DimensionalityStyle(
                    simplifiedMinSize,
                    simplifiedMaxSize,
                    simplifiedPreferredSize,
                    simplifiedSize
                );
    }

    @Override
    public String toString() {
        if ( this == _NONE )
            return this.getClass().getSimpleName() + "[NONE]";

        return
            this.getClass().getSimpleName() + "[" +
                    "minWidth="        + _minSize.width().map(Objects::toString).orElse("?") + ", " +
                    "minHeight="       + _minSize.height().map(Objects::toString).orElse("?") + ", " +
                    "maxWidth="        + _maxSize.height().map(Objects::toString).orElse("?") + ", " +
                    "maxHeight="       + _maxSize.width().map(Objects::toString).orElse("?") + ", " +
                    "preferredWidth="  + _preferredSize.width().map(Objects::toString).orElse("?") + ", " +
                    "preferredHeight=" + _preferredSize.height().map(Objects::toString).orElse("?") + ", " +
                    "width="           + _size.width().map(Objects::toString).orElse("?") + ", " +
                    "height="          + _size.height().map(Objects::toString).orElse("?") +
                "]";
    }

    @Override
    public final boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( !(o instanceof DimensionalityStyle) ) return false;
        DimensionalityStyle that = (DimensionalityStyle) o;
        return Objects.equals(_minSize,        that._minSize)       &&
               Objects.equals(_maxSize,        that._maxSize)       &&
               Objects.equals(_preferredSize,  that._preferredSize) &&
               Objects.equals(_size,           that._size);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(_minSize, _maxSize, _preferredSize, _size);
    }

}
