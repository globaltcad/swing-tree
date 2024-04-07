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
                                                0f
                                            );

    public static FilterConf none() {
        return _NONE;
    }

    private static FilterConf of(
        KernelConf       kernel,
        UI.ComponentArea area,
        Offset           center,
        Offset           scale,
        float            blur
    ) {
        if (
            _NONE._kernel.equals(kernel) &&
            _NONE._area.equals(area)     &&
            _NONE._center.equals(center) &&
            _NONE._scale.equals(scale)   &&
            _NONE._blur == blur
        ) {
            return _NONE;
        }
        return new FilterConf(kernel, area, center, scale, blur);
    }

    private final KernelConf       _kernel;
    private final UI.ComponentArea _area;
    private final Offset           _center;
    private final Offset           _scale;
    private final float            _blur;

    FilterConf(
        KernelConf       kernel,
        UI.ComponentArea area,
        Offset           center,
        Offset           scale,
        float            blur
    ) {
        _kernel = Objects.requireNonNull(kernel);
        _area   = Objects.requireNonNull(area);
        _center = Objects.requireNonNull(center);
        _scale  = Objects.requireNonNull(scale);
        _blur   = blur;
    }

    KernelConf kernel() {
        return _kernel;
    }

    UI.ComponentArea area() {
        return _area;
    }

    Offset center() {
        return _center;
    }

    Offset scale() {
        return _scale;
    }

    float blur() {
        return _blur;
    }

    public FilterConf kernel(Size size, double... matrix) {
        return of(KernelConf.of(size, matrix), _area, _center, _scale, _blur);
    }

    public FilterConf area(UI.ComponentArea area) {
        return of(_kernel, area, _center, _scale, _blur);
    }

    public FilterConf center(double x, double y) {
        return of(_kernel, _area, Offset.of(x, y), _scale, _blur);
    }

    public FilterConf scale(double x, double y) {
        return of(_kernel, _area, _center, Offset.of(x, y), _blur);
    }

    public FilterConf blur( double radius ) {
        return of(_kernel, _area, _center, _scale, (float)radius);
    }

    FilterConf _scale( double factor ) {
        if ( factor == 1 ) {
            return this;
        }
        if ( this.equals(FilterConf.none()) ) {
            return this;
        }
        return of(_kernel, _area, _center.scale(factor), _scale, (float) (_blur * factor));
    }

    FilterConf simplified() {
        KernelConf kernel = _kernel.simplified();
        if (
            KernelConf.none().equals(kernel) &&
            _scale.equals(Offset.none()) &&
            _center.equals(Offset.none()) &&
            _blur == 0
        ) {
            return _NONE;
        }
        return of(kernel, _area, _center, _scale, _blur);
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
            _area.equals(other._area)     &&
            _center.equals(other._center) &&
            _scale.equals(other._scale)   &&
            _blur == other._blur;
    }

    @Override
    public int hashCode() {
        return Objects.hash(_kernel, _area, _center, _scale, _blur);
    }

    @Override
    public String toString() {
        if ( this.equals(FilterConf.none()) )
            return this.getClass().getSimpleName() + "[NONE]";
        return this.getClass().getSimpleName() + "["
                    + "kernel=" + _kernel + ", "
                    + "area="   + _area   + ", "
                    + "center=" + _center + ", "
                    + "scale="  + _scale  + ", "
                    + "blur="   + _blur
                + "]";
    }
}
