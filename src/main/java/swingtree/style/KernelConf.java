package swingtree.style;

import swingtree.layout.Size;

import java.awt.image.Kernel;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

final class KernelConf
{
    private static final KernelConf _NONE = new KernelConf(0, 0, new float[0]);

    public static KernelConf none() {
        return _NONE;
    }

    private static KernelConf of( int width, int height, float[] data ) {
        if (
            _NONE._width == width &&
            _NONE._height == height &&
            Arrays.equals(_NONE._data, data)
        ) {
            return _NONE;
        }
        int size = width * height;
        if ( data.length != size ) {
            return none();
        }
        return new KernelConf(width, height, data);
    }

    public static KernelConf of(
            Size size,
            double... data
    ) {
        Optional<Float> width = size.width();
        Optional<Float> height = size.height();
        if ( !width.isPresent() || !height.isPresent() ) {
            return none();
        }
        float[] saveCopy = new float[data.length];
        for ( int i = 0; i < data.length; i++ ) {
            saveCopy[i] = (float)data[i];
        }
        return of(
            width.get().intValue(),
            height.get().intValue(),
            saveCopy
        );
    }

    private final int     _width;
    private final int     _height;
    private final float[] _data;


    private KernelConf( int width, int height, float[] data ) {
        _width  = width;
        _height = height;
        _data   = Objects.requireNonNull(data);
    }

    public int width() {
        return _width;
    }

    public int height() {
        return _height;
    }

    public float[] data() {
        return _data;
    }

    KernelConf simplified() {
        if ( _data.length == 0 ) {
            return none();
        }
        // No width or height, so we can't simplify
        if ( _width == 0 || _height == 0 ) {
            return this;
        }
        // If the kernel is 1x1, we can simplify
        if ( _width == 1 && _height == 1 && _data[0] == 1.0f ) {
            return none();
        }
        return this;
    }

    Kernel toAwtKernel() {
        return new Kernel(_width, _height, _data);
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) {
            return true;
        }
        if ( obj == null || getClass() != obj.getClass() ) {
            return false;
        }
        KernelConf that = (KernelConf) obj;
        return _width == that._width &&
               _height == that._height &&
               Arrays.equals(_data, that._data);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(_width, _height);
        result = 31 * result + Arrays.hashCode(_data);
        return result;
    }

    @Override
    public String toString() {
        if ( this.equals(none()) )
            return this.getClass().getSimpleName() + "[NONE]";

        return this.getClass().getSimpleName() + "[" +
                "width="  + _width                 + ", " +
                "height=" + _height                + ", " +
                "data="   + Arrays.toString(_data) +
            ']';
    }
}
