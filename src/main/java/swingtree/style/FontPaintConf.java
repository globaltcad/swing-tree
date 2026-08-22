package swingtree.style;

import com.google.errorprone.annotations.Immutable;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import swingtree.SwingTree;
import swingtree.api.Configurator;

import javax.swing.JComponent;
import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.Objects;

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
@Immutable
@SuppressWarnings("Immutable")
final class FontPaintConf
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(FontPaintConf.class);
    private static final FontPaintConf _NONE = new FontPaintConf(null, null, null, null);

    public static FontPaintConf none() { return _NONE; }

    public static FontPaintConf of(
            @Nullable Color             color,
            @Nullable Paint             paint,
            @Nullable Pooled<NoiseConf> noise,
            @Nullable GradientConf      gradient
    ) {
        color    = StyleUtil.isUndefinedColor(color)    ? null : color;
        noise    = noise == null || NoiseConf.none().equals(noise.get()) ? null : noise;
        gradient = GradientConf.none().equals(gradient) ? null : gradient;
        if ( color == null && paint == null && noise == null && gradient == null )
            return _NONE;
        if ( noise != null )
            noise = noise.intern();

        return new FontPaintConf(color, paint, noise, gradient);
    }

    private final @Nullable Color _color;
    private final @Nullable Paint _paint;
    private final @Nullable Pooled<NoiseConf> _noise;
    private final @Nullable GradientConf _gradient;


    FontPaintConf(
        @Nullable Color color,
        @Nullable Paint paint,
        @Nullable Pooled<NoiseConf> noise,
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

    FontPaintConf noise( Configurator<NoiseConf> noiseConfigurator ) {
        Objects.requireNonNull(noiseConfigurator);
        Pooled<NoiseConf> noise = _noise == null ? new Pooled<>(NoiseConf.none()) : _noise;
        try {
            noise = new Pooled<>(noiseConfigurator.configure(noise.get()));
            return of(null, null, noise, null);
        } catch ( Exception e ) {
            log.error(SwingTree.get().logMarker(), "Failed to apply noise configuration.", e);
        }
        return this;
    }

    FontPaintConf gradient( Configurator<GradientConf> gradientConfigurator ) {
        Objects.requireNonNull(gradientConfigurator);
        GradientConf gradient = _gradient == null ? GradientConf.none() : _gradient;
        try {
            gradient = gradientConfigurator.configure(gradient);
            return of(null, null, null, gradient);
        } catch ( Exception e ) {
            log.error(SwingTree.get().logMarker(), "Failed to apply gradient configuration.", e);
        }
        return this;
    }

    @Nullable Paint getFor( BoxModelConf boxModelConf ) {
        if (_color != null)
            return _color;
        if (_paint != null)
            return _paint;
        if (_noise != null)
            return StyleRenderer.createNoisePaint(boxModelConf, _noise);
        if (_gradient != null)
            return StyleRenderer.createGradientPaint(boxModelConf, _gradient);
        return null;
    }

    @Nullable Paint getFor(JComponent component) {
        return new FontPaint(this, new WeakReference<>(component));
    }

    /** The one plain, geometry-independent {@link Color} this conf consists of, or {@code null}
     *  if it is empty or needs the paint machinery (custom paint, noise, gradient). A solid
     *  color is special because it is the only font paint that can be rendered through the
     *  component <em>foreground</em> channel instead of a {@code TextAttribute.FOREGROUND}
     *  font attribute — see the color-channel documentation on {@link FontConf}. */
    @Nullable Color solidColor() {
        if ( _noise == null && _gradient == null ) {
            if ( _paint == null )
                return _color;
            if ( _color == null && _paint instanceof Color )
                return (Color) _paint; // a Color supplied through the generic paint API is still just a solid color
        }
        return null;
    }

    public boolean representsColor( @Nullable Color color ) {
        return Objects.equals(color, _color) &&
                _paint    == null &&
                _gradient == null &&
                _noise    == null;
    }

    FontPaintConf _scale( double scale ) {
        if ( _noise != null ) {
            return of(null, null, _noise.map( it -> it._scale(scale) ), null);
        }
        if ( _gradient != null ) {
            return of(null, null, null, _gradient._scale(scale));
        }
        return this;
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
