package swingtree.style;

import swingtree.UI;
import swingtree.layout.Size;

import java.util.Objects;

public final class FilterConf
{
    private static final FilterConf _NONE = new FilterConf(
                                                KernelConf.none(),
                                                UI.ComponentArea.BODY,
                                                Offset.none(),
                                                Offset.none(),
                                                Offset.none()
                                            );

    public static FilterConf none() {
        return _NONE;
    }

    private static FilterConf of(
        KernelConf       kernel,
        UI.ComponentArea area,
        Offset           offset,
        Offset           center,
        Offset           scale
    ) {
        if (
            _NONE._kernel.equals(kernel) &&
            _NONE._area.equals(area) &&
            _NONE._offset.equals(offset) &&
            _NONE._center.equals(center) &&
            _NONE._scale.equals(scale)
        ) {
            return _NONE;
        }
        return new FilterConf(kernel, area, offset, center, scale);
    }

    private final KernelConf       _kernel;
    private final UI.ComponentArea _area;
    private final Offset           _offset;
    private final Offset           _center;
    private final Offset           _scale;

    FilterConf(
        KernelConf       kernel,
        UI.ComponentArea area,
        Offset           offset,
        Offset           center,
        Offset           scale
    ) {
        _kernel = Objects.requireNonNull(kernel);
        _area   = Objects.requireNonNull(area);
        _offset = Objects.requireNonNull(offset);
        _center = Objects.requireNonNull(center);
        _scale  = Objects.requireNonNull(scale);
    }

    KernelConf kernel() {
        return _kernel;
    }

    UI.ComponentArea area() {
        return _area;
    }

    Offset offset() {
        return _offset;
    }

    Offset center() {
        return _center;
    }

    Offset scale() {
        return _scale;
    }

    public FilterConf kernel(Size size, double... matrix) {
        return of(KernelConf.of(size, matrix), _area, _offset, _center, _scale);
    }

    public FilterConf area(UI.ComponentArea area) {
        return of(_kernel, area, _offset, _center, _scale);
    }

    public FilterConf offset(double x, double y) {
        return of(_kernel, _area, Offset.of(x, y), _center, _scale);
    }

    public FilterConf center(double x, double y) {
        return of(_kernel, _area, _offset, Offset.of(x, y), _scale);
    }

    public FilterConf scale(double x, double y) {
        return of(_kernel, _area, _offset, _center, Offset.of(x, y));
    }

    FilterConf simplified() {
        KernelConf kernel = _kernel.simplified();
        if ( KernelConf.none().equals(kernel) ) {
            return _NONE;
        }
        return of(kernel, _area, _offset, _center, _scale);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        FilterConf other = (FilterConf) obj;
        return
            _kernel.equals(other._kernel) &&
            _area.equals(other._area) &&
            _offset.equals(other._offset) &&
            _center.equals(other._center) &&
            _scale.equals(other._scale);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_kernel, _area, _offset, _center, _scale);
    }

    @Override
    public String toString() {
        if ( this.equals(FilterConf.none()) )
            return this.getClass().getSimpleName() + "[NONE]";
        return this.getClass().getSimpleName() + "["
                    + "kernel=" + _kernel + ", "
                    + "area="   + _area   + ", "
                    + "offset=" + _offset + ", "
                    + "center=" + _center + ", "
                    + "scale="  + _scale
                + "]";
    }
}
