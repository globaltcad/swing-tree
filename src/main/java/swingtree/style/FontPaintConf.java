package swingtree.style;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import javax.swing.JComponent;
import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.function.Function;

/**
 *  An internal class that holds immutable configuration data
 *  needed for defining the {@link java.awt.Paint} for
 *  the font of a UI component.
 *  <p>
 *  Instances of this hold either a {@link Color}, a {@link Paint}, a {@link NoiseConf},
 *  or a {@link GradientConf} object, but not more than one,
 *  so this means that only one of these objects can be non-null
 *  at a time the other three must be null.
 */
final class FontPaintConf
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(FontPaintConf.class);
    private static final FontPaintConf _NONE = new FontPaintConf(null, null, null, null);

    public static FontPaintConf none() { return _NONE; }

    public static FontPaintConf of(
            @Nullable Color        color,
            @Nullable Paint        paint,
            @Nullable NoiseConf    noise,
            @Nullable GradientConf gradient
    ) {
        color    = StyleUtil.isUndefinedColor(color)    ? null : color;
        noise    = NoiseConf.none().equals(noise)       ? null : noise;
        gradient = GradientConf.none().equals(gradient) ? null : gradient;
        if ( color == null && paint == null && noise == null && gradient == null )
            return _NONE;

        return new FontPaintConf(color, paint, noise, gradient);
    }

    private final @Nullable Color _color;
    private final @Nullable Paint _paint;
    private final @Nullable NoiseConf _noise;
    private final @Nullable GradientConf _gradient;


    FontPaintConf(
        @Nullable Color color,
        @Nullable Paint paint,
        @Nullable NoiseConf noise,
        @Nullable GradientConf gradient
    ) {
        if ( color != null ) {
            paint    = null;
            noise    = null;
            gradient = null;
        }
        if ( paint != null ) {
            color    = null;
            noise    = null;
            gradient = null;
        }
        if ( noise != null ) {
            color    = null;
            paint    = null;
            gradient = null;
        }
        if ( gradient != null ) {
            color = null;
            paint = null;
            noise = null;
        }
        _color    = color;
        _paint    = paint;
        _noise    = noise;
        _gradient = gradient;
    }

    FontPaintConf color( Color color ) {
        return of(color, null, null, null);
    }

    FontPaintConf paint( Paint paint ) {
        return of(null, paint, null, null);
    }

    FontPaintConf noise( Function<NoiseConf, NoiseConf> noiseConfigurator ) {
        Objects.requireNonNull(noiseConfigurator);
        NoiseConf noise = _noise == null ? NoiseConf.none() : _noise;
        try {
            noise = noiseConfigurator.apply(noise);
            return of(null, null, noise, null);
        } catch ( Exception e ) {
            log.error("Failed to apply noise configuration.", e);
        }
        return this;
    }

    FontPaintConf gradient( Function<GradientConf, GradientConf> gradientConfigurator ) {
        Objects.requireNonNull(gradientConfigurator);
        GradientConf gradient = _gradient == null ? GradientConf.none() : _gradient;
        try {
            gradient = gradientConfigurator.apply(gradient);
            return of(null, null, null, gradient);
        } catch ( Exception e ) {
            log.error("Failed to apply gradient configuration.", e);
        }
        return this;
    }

    @Nullable Paint getFor( BoxModelConf boxModelConf ) {
        if (_color != null)
            return _color;
        if (_paint != null)
            return _paint;
        if (_noise != null)
            return StyleRenderer._createNoisePaint(boxModelConf, _noise);
        if (_gradient != null)
            return StyleRenderer._createGradientPaint(boxModelConf, _gradient);
        return null;
    }

    @Nullable Paint getFor(JComponent component) {
        return new FontPaint(this, new WeakReference<>(component));
    }

    public boolean representsColor( @Nullable Color color ) {
        return Objects.equals(color, _color) &&
                _paint    == null &&
                _gradient == null &&
                _noise    == null;
    }

    @Override
    public boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( o == null || getClass() != o.getClass() ) return false;
        FontPaintConf that = (FontPaintConf) o;
        return Objects.equals(_color, that._color) &&
               Objects.equals(_paint, that._paint) &&
               Objects.equals(_noise, that._noise) &&
               Objects.equals(_gradient, that._gradient);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_color, _paint, _noise, _gradient);
    }

    @Override
    public String toString() {
        if ( this.equals(_NONE) )
            return this.getClass().getSimpleName() + "[NONE]";
        String paint = "?";
        if (_color != null)
            paint = StyleUtil.toString(_color);
        if (_paint != null)
            paint = StyleUtil.toString(_paint);
        if (_noise != null)
            paint = _noise.toString();
        if (_gradient != null)
            paint = _gradient.toString();
        return this.getClass().getSimpleName() + "[" + paint + "]";
    }

}
