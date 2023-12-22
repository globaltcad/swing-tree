package swingtree.style;

import swingtree.UI;
import swingtree.layout.Bounds;

import javax.swing.JComponent;
import javax.swing.border.Border;
import java.awt.Insets;
import java.awt.Shape;
import java.awt.geom.Area;
import java.util.Objects;
import java.util.Optional;

/**
 *  An immutable snapshot of essential component state needed for rendering
 *  the style of a component.
 *  This is immutable to use it as a basis for caching.
 *  When the snapshot changes compared to the previous one, the image buffer based
 *  render cache is being invalidated and the component is rendered again
 *  (potentially with a new cached image buffer).
 */
class ComponentConf
{
    public static ComponentConf none() {
        return new ComponentConf(
                    Style.none(),
                    Bounds.none(),
                    Outline.none(),
                    new AreasCache()
                );
    }

    private final Style   _style;
    private final Bounds  _currentBounds;
    private final Outline _baseOutline;

    private final AreasCache _areasCache;

    private boolean _wasAlreadyHashed = false;
    private int     _hashCode         = 0; // cached hash code


    private ComponentConf(
        Style style,
        Bounds currentBounds,
        Outline baseOutline,
        AreasCache areasCache
    ) {
        _style         = Objects.requireNonNull(style);
        _currentBounds = Objects.requireNonNull(currentBounds);
        _baseOutline   = Objects.requireNonNull(baseOutline);
        _areasCache    = Objects.requireNonNull(areasCache);
    }

    Style style() { return _style; }

    Bounds currentBounds() { return _currentBounds; }

    Outline baseOutline() { return _baseOutline; }

    Optional<Shape> componentArea() {
        Shape contentClip = null;
        if ( _areasCache.mainArea().exists() || _style.margin().isPositive() )
            contentClip = getMainComponentArea();

        return Optional.ofNullable(contentClip);
    }

    Area getMainComponentArea()
    {
        return _areasCache.mainArea().getFor(this, _areasCache);
    }

    Area getExteriorComponentArea()
    {
        return _areasCache.exteriorArea().getFor(this, _areasCache);
    }

    Area getInteriorComponentArea()
    {
        return _areasCache.interiorArea().getFor(this, _areasCache);
    }

    Area getComponentBorderArea()
    {
        return _areasCache.borderArea().getFor(this, _areasCache);
    }


    ComponentConf with( Style style, JComponent component )
    {
        Outline outline = Outline.none();
        Border border = component.getBorder();
        if ( border instanceof StyleAndAnimationBorder ) {
            Insets base = ((StyleAndAnimationBorder<?>) border).getBaseInsets(true);
            outline = Outline.of(base.top, base.left, base.bottom, base.right);
        }

        boolean sameStyle   = _style.equals(style);
        boolean sameBounds  = _currentBounds.equals(component.getX(), component.getY(), component.getWidth(), component.getHeight());
        boolean sameOutline = _baseOutline.equals(outline);
        if ( sameStyle && sameBounds && sameOutline )
            return this;

        ComponentConf newConf = new ComponentConf(
                                    style,
                                    Bounds.of(component.getX(), component.getY(), component.getWidth(), component.getHeight()),
                                    outline,
                                    _areasCache
                                );

        return new ComponentConf(
                        newConf._style,
                        newConf._currentBounds,
                        newConf._baseOutline,
                        _areasCache.validate(this, newConf)
                );
    }

    /**
     *  Returns a new {@link ComponentConf} instance which only contains style information relevant
     *  to the provided {@link UI.Layer}. Style information on other layers is discarded.
     * @param layer The layer to retain.
     * @return A new {@link ComponentConf} instance which only contains style information relevant to the provided {@link UI.Layer}.
     */
    ComponentConf onlyRetainingLayer( UI.Layer layer ) {
        return new ComponentConf(
                    _style.onlyRetainingLayer(layer),
                    _currentBounds,
                    _baseOutline,
                    _areasCache
                );
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"[" +
                    "style="         + _style         + ", "+
                    "bounds="        + _currentBounds + ", "+
                    "baseOutline="   + _baseOutline   + ", "+
                "]";
    }

    @Override
    public boolean equals( Object o ) {
        if ( o == this ) return true;
        if ( o == null ) return false;
        if ( o.getClass() != this.getClass() ) return false;
        ComponentConf other = (ComponentConf) o;
        return Objects.equals(_style, other._style)
            && Objects.equals(_currentBounds, other._currentBounds)
            && Objects.equals(_baseOutline, other._baseOutline);
    }

    @Override
    public int hashCode() {
        if ( _wasAlreadyHashed )
            return _hashCode;

        _hashCode = Objects.hash(_style, _currentBounds, _baseOutline);
        _wasAlreadyHashed = true;
        return _hashCode;
    }
}
