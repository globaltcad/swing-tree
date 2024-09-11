package swingtree.style;

import com.google.errorprone.annotations.Immutable;
import swingtree.layout.Size;

import java.util.Objects;
import java.util.Optional;

/**
 *  A style that defines the dimensionality of a component
 *  in the form of a minimum, maximum, preferred and regular size.
 *  The layout manager of a component will use this information
 *  to determine the actual size of the component in the layout.
 **/
@Immutable
final class DimensionalityConf
{
    private static final DimensionalityConf _NONE = new DimensionalityConf(
                                                            Size.unknown(),
                                                            Size.unknown(),
                                                            Size.unknown(),
                                                            Size.unknown()
                                                        );

    static DimensionalityConf none() { return _NONE; }

    static DimensionalityConf of(
        Size minSize,
        Size maxSize,
        Size preferredSize,
        Size size
    ) {
        if (
            minSize       .equals( _NONE._minSize       ) &&
            maxSize       .equals( _NONE._maxSize       ) &&
            preferredSize .equals( _NONE._preferredSize ) &&
            size          .equals( _NONE._size          )
        )
            return _NONE;
        else
            return new DimensionalityConf(minSize, maxSize, preferredSize, size);
    }


    private final Size _minSize;
    private final Size _maxSize;
    private final Size _preferredSize;
    private final Size _size;


    private DimensionalityConf(
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

    DimensionalityConf _withMinWidth( int minWidth ) {
        return DimensionalityConf.of(_minSize.withWidth(minWidth), _maxSize, _preferredSize, _size);
    }

    DimensionalityConf _withMinHeight( int minHeight ) {
        return DimensionalityConf.of(_minSize.withHeight(minHeight), _maxSize, _preferredSize, _size);
    }

    DimensionalityConf _withMinSize( Size minSize ) {
        return DimensionalityConf.of(minSize, _maxSize, _preferredSize, _size);
    }

    DimensionalityConf _withMaxWidth( int maxWidth ) {
        return DimensionalityConf.of(_minSize, _maxSize.withWidth(maxWidth), _preferredSize, _size);
    }

    DimensionalityConf _withMaxHeight( int maxHeight ) {
        return DimensionalityConf.of(_minSize, _maxSize.withHeight(maxHeight), _preferredSize, _size);
    }

    DimensionalityConf _withMaxSize( Size maxSize ) {
        return DimensionalityConf.of(_minSize, maxSize, _preferredSize, _size);
    }

    DimensionalityConf _withPreferredWidth( int preferredWidth ) {
        return DimensionalityConf.of(_minSize, _maxSize, _preferredSize.withWidth(preferredWidth), _size);
    }

    DimensionalityConf _withPreferredHeight( int preferredHeight ) {
        return DimensionalityConf.of(_minSize, _maxSize, _preferredSize.withHeight(preferredHeight), _size);
    }

    DimensionalityConf _withPreferredSize( Size preferredSize ) {
        return DimensionalityConf.of(_minSize, _maxSize, preferredSize, _size);
    }

    DimensionalityConf _withWidth( int width ) {
        return DimensionalityConf.of(_minSize, _maxSize, _preferredSize, _size.withWidth(width));
    }

    DimensionalityConf _withHeight( int height ) {
        return DimensionalityConf.of(_minSize, _maxSize, _preferredSize, _size.withHeight(height));
    }

    DimensionalityConf _withSize(Size size ) {
        return DimensionalityConf.of(_minSize, _maxSize, _preferredSize, size);
    }

    public Optional<Integer> minWidth() { return _minSize.width().map(Number::intValue); }

    public Optional<Integer> minHeight() { return _minSize.height().map(Number::intValue); }

    public Optional<Integer> maxWidth() { return _maxSize.width().map(Number::intValue); }

    public Optional<Integer> maxHeight() { return _maxSize.height().map(Number::intValue); }

    public Optional<Integer> preferredWidth() { return _preferredSize.width().map(Number::intValue); }

    public Optional<Integer> preferredHeight() { return _preferredSize.height().map(Number::intValue); }

    public Optional<Integer> width() { return _size.width().map(Number::intValue); }

    public Optional<Integer> height() { return _size.height().map(Number::intValue); }

    DimensionalityConf _scale( double scale ) {
        return DimensionalityConf.of(
                    _minSize.scale(scale),
                    _maxSize.scale(scale),
                    _preferredSize.scale(scale),
                    _size.scale(scale)
                );
    }

    DimensionalityConf simplified() {
        if ( this.equals(_NONE) )
            return _NONE;
        else
            return DimensionalityConf.of(
                    _minSize.round(),
                    _maxSize.round(),
                    _preferredSize.round(),
                    _size.round()
                );
    }

    @Override
    public String toString() {
        if ( this.equals(_NONE) )
            return this.getClass().getSimpleName() + "[NONE]";

        return
            this.getClass().getSimpleName() + "[" +
                    "minWidth="        + _minSize.width().map(this::_toString).orElse("?") + ", " +
                    "minHeight="       + _minSize.height().map(this::_toString).orElse("?") + ", " +
                    "maxWidth="        + _maxSize.height().map(this::_toString).orElse("?") + ", " +
                    "maxHeight="       + _maxSize.width().map(this::_toString).orElse("?") + ", " +
                    "preferredWidth="  + _preferredSize.width().map(this::_toString).orElse("?") + ", " +
                    "preferredHeight=" + _preferredSize.height().map(this::_toString).orElse("?") + ", " +
                    "width="           + _size.width().map(this::_toString).orElse("?") + ", " +
                    "height="          + _size.height().map(this::_toString).orElse("?") +
                "]";
    }

    private String _toString( Float value ) {
        return String.valueOf(value).replace(".0", "");
    }

    @Override
    public final boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( !(o instanceof DimensionalityConf) ) return false;
        DimensionalityConf that = (DimensionalityConf) o;
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
