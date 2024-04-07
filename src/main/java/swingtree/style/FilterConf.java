package swingtree.style;

import swingtree.UI;
import swingtree.layout.Size;

import java.util.Objects;

/**
 *  The filter configuration object
 *  defines if and how a filter should be applied
 *  on the rendering of the parent of a particular component.
 *  This may include a blur, a convolution kernel, a scale and translation
 *  applied to the parent layer. <br>
 *  Note that this configuration may only have an effect if the component
 *  it is applied to is non-opaque, which is to say that it does
 *  not have another other styles which would
 *  obstruct the rendering of the parent layer. <br>
 *  <br>
 *  This class exposes the following properties
 *  with their respective purpose and default values:
 *  <ul>
 *      <li><b>Kernel</b>
 *          <p>
 *              The convolution kernel to apply to the parent layer
 *              using the {@link java.awt.image.ConvolveOp} class.
 *              Default value is {@link KernelConf#none()}
 *          </p>
 *      </li>
 *      <li><b>Area</b>
 *      <p>
 *          The area of the current component to which
 *          the filter should be applied to.
 *          Default value is {@link UI.ComponentArea#BODY},
 *          which includes the interior and border area of the component.
 *      </p>
 *      </li><li><b>Offset</b>
 *      <p>
 *          The translation to apply to the filtered parent layer.
 *          Default value is {@code (0.0, 0.0)}, which means no translation.
 *      </p>
 *      </li><li><b>Scale</b>
 *      <p>
 *          The scale to apply to the filtered parent layer.
 *          Default value is {@code (1.0, 1.0)}, which means no scaling
 *          is applied to the parent layer.
 *      </p>
 *      </li><li><b>Blur</b>
 *      <p>
 *          The blur radius, which is used to apply a gaussian blur
 *          to the parent layer using the {@link java.awt.image.ConvolveOp} class.
 *          Default value is {@code 0.0}, which means no blur is applied.
 *      </p>
 *      </li>
 *  </ul>
 */
public final class FilterConf
{
    private static final FilterConf _NONE = new FilterConf(
                                                KernelConf.none(),
                                                UI.ComponentArea.BODY,
                                                Offset.none(),
                                                Scale.none(),
                                                0f
                                            );

    static FilterConf none() {
        return _NONE;
    }

    private static FilterConf of(
        KernelConf       kernel,
        UI.ComponentArea area,
        Offset           offset,
        Scale            scale,
        float            blur
    ) {
        blur = Math.max(0, blur);
        if (
            _NONE._kernel.equals(kernel) &&
            _NONE._area.equals(area)     &&
            _NONE._offset.equals(offset) &&
            _NONE._scale.equals(scale)   &&
            _NONE._blur == blur
        ) {
            return _NONE;
        }
        return new FilterConf(kernel, area, offset, scale, blur);
    }

    private final KernelConf       _kernel;
    private final UI.ComponentArea _area;
    private final Offset           _offset;
    private final Scale            _scale;
    private final float            _blur;

    FilterConf(
        KernelConf       kernel,
        UI.ComponentArea area,
        Offset           offset,
        Scale            scale,
        float            blur
    ) {
        _kernel = Objects.requireNonNull(kernel);
        _area   = Objects.requireNonNull(area);
        _offset = Objects.requireNonNull(offset);
        _scale  = Objects.requireNonNull(scale);
        _blur   = blur;
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

    Scale scale() {
        return _scale;
    }

    float blur() {
        return _blur;
    }

    /**
     *  Use this to configure a custom convolution kernel
     *  based on a row major matrix represented by a width, height
     *  and an array of values whose length is the product
     *  of the supplied width and height.<br>
     *  Note that this operation will be applied after
     *  the parent layer was translated and scaled
     *  and blurred.
     *
     * @param size The {@link Size} object representing the width and height of the matrix
     *             to be used as the convolution kernel.
     * @param matrix A var args array of double values
     *               used to form a row major matrix.
     * @return An updated filter configuration
     *          containing the desired convolution kernel.
     */
    public FilterConf kernel( Size size, double... matrix ) {
        return of(KernelConf.of(size, matrix), _area, _offset, _scale, _blur);
    }

    /**
     *  Define the component area which should display the filtered parent.
     *  Everything outside that area will not be affected by the filter.
     *  Take a look at the {@link UI.ComponentArea} enum documentation
     *  for more information on the available areas.
     *
     * @param area A constant representing the area of the component
     *             to which the filter should be clipped to.
     * @return An updated filter configuration
     *         with the desired clipping area.
     */
    public FilterConf area(UI.ComponentArea area) {
        return of(_kernel, area, _offset, _scale, _blur);
    }

    /**
     *  Uses the given x and y offset values to translate the
     *  parent layer before applying other filtering operations.
     *
     * @param x The amount to translate the parent layer along the x-axis before filtering.
     * @param y The amount to translate the parent layer along the y-axis before filtering.
     * @return An updated filter configuration having the desired translation.
     */
    public FilterConf offset( double x, double y ) {
        return of(_kernel, _area, Offset.of(x, y), _scale, _blur);
    }

    /**
     *  Scales the parent layer by the given x and y factors
     *  before applying other filtering operations.
     *
     * @param x The factor to scale the parent layer along the x-axis before filtering.
     * @param y The factor to scale the parent layer along the y-axis before filtering.
     * @return An updated filter configuration having the desired scaling.
     */
    public FilterConf scale( double x, double y ) {
        return of(_kernel, _area, _offset, Scale.of(x, y), _blur);
    }

    /**
     *  Supply values greater than 0 to this to apply a gaussian blur
     *  to the parent layer.
     *  Note that this operation will be applied after
     *  the parent layer was translated and scaled.
     *
     * @param radius The radius of the gaussian blur filter
     *               to apply to the parent layer.
     * @return An updated filter configuration having the desired blur radius.
     */
    public FilterConf blur( double radius ) {
        return of(_kernel, _area, _offset, _scale, (float)radius);
    }

    FilterConf _scale( double factor ) {
        if ( factor == 1 ) {
            return this;
        }
        if ( this.equals(FilterConf.none()) ) {
            return this;
        }
        return of(_kernel, _area, _offset.scale(factor), _scale, (float) (_blur * factor));
    }

    FilterConf simplified() {
        KernelConf kernel = _kernel.simplified();
        if (
            KernelConf.none().equals(kernel) &&
            _scale.equals(Scale.none()) &&
            _offset.equals(Offset.none()) &&
            _blur <= 0
        ) {
            return _NONE;
        }
        return of(kernel, _area, _offset, _scale, _blur);
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
            _offset.equals(other._offset) &&
            _scale.equals(other._scale)   &&
            _blur == other._blur;
    }

    @Override
    public int hashCode() {
        return Objects.hash(_kernel, _area, _offset, _scale, _blur);
    }

    @Override
    public String toString() {
        if ( this.equals(FilterConf.none()) )
            return this.getClass().getSimpleName() + "[NONE]";
        return this.getClass().getSimpleName() + "["
                    + "kernel=" + _kernel + ", "
                    + "area="   + _area   + ", "
                    + "center=" + _offset + ", "
                    + "scale="  + _scale  + ", "
                    + "blur="   + _blur
                + "]";
    }
}
