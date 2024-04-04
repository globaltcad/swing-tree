package swingtree.style;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.lang.ref.WeakReference;
import java.util.Objects;

final class FontPaint implements Paint
{
    private final FontPaintConf _fontPaintConf;
    private final WeakReference<JComponent> _componentRef;
    private BoxModelConf _boxModelConf = BoxModelConf.none();
    private @Nullable Paint _delegatedPaint = null;

    FontPaint(FontPaintConf fontPaintConf, WeakReference<JComponent> componentRef) {
        _fontPaintConf = Objects.requireNonNull(fontPaintConf);
        _componentRef  = Objects.requireNonNull(componentRef);
    }

    private void _init() {
        JComponent component = _componentRef.get();
        if ( component != null ) {
            BoxModelConf latestBoxModelConf = ComponentExtension.from(component).getBoxModelConf();
            if ( !latestBoxModelConf.equals(_boxModelConf) ) {
                _boxModelConf = latestBoxModelConf;
                _delegatedPaint = _fontPaintConf.getFor(latestBoxModelConf);
            }
        }
    }

    public Paint getDelegatedPaint() {
        _init();
        return Objects.requireNonNull(_delegatedPaint);
    }

    @Override
    public PaintContext createContext(
            ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds,
            AffineTransform xform, RenderingHints hints
    ) {
        _init();
        return Objects.requireNonNull(_delegatedPaint).createContext(cm, deviceBounds, userBounds, xform, hints);
    }

    @Override
    public int getTransparency() {
        _init();
        return Objects.requireNonNull(_delegatedPaint).getTransparency();
    }

    @Override
    public int hashCode() {
        JComponent component = _componentRef.get();
        if ( component != null )
            return Objects.hash(_fontPaintConf, component);
        else
            return Objects.hash(_fontPaintConf, _boxModelConf);
    }

    @Override
    public boolean equals(Object obj) {
        if ( obj == this ) return true;
        if ( obj == null || obj.getClass() != this.getClass() ) return false;
        FontPaint that = (FontPaint) obj;
        JComponent thisComponent = _componentRef.get();
        JComponent thatComponent = _componentRef.get();
        if ( thisComponent != thatComponent )
            return false;
        else if ( thisComponent != null )
            return this._fontPaintConf.equals(that._fontPaintConf);
        else
            return this._fontPaintConf.equals(that._fontPaintConf) &&
                   this._boxModelConf.equals(that._boxModelConf);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" + _fontPaintConf + "]";
    }
}
