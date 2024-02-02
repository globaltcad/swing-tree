package swingtree.style;

import swingtree.UI;
import swingtree.layout.Bounds;

import javax.swing.JComponent;
import javax.swing.border.Border;
import java.awt.Graphics;
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
final class ComponentConf
{
    public static ComponentConf none() {
        return new ComponentConf(
                    StyleConf.none(),
                    Bounds.none(),
                    Outline.none(),
                    new ComponentAreas()
                );
    }

    private final StyleConf _styleConf;
    private final Bounds  _currentBounds;
    private final Outline _baseOutline;

    private final ComponentAreas _areas;

    private boolean _wasAlreadyHashed = false;
    private int     _hashCode         = 0; // cached hash code


    private ComponentConf(
        StyleConf styleConf,
        Bounds         currentBounds,
        Outline        baseOutline,
        ComponentAreas componentAreas
    ) {
        _styleConf     = Objects.requireNonNull(styleConf);
        _currentBounds = Objects.requireNonNull(currentBounds);
        _baseOutline   = Objects.requireNonNull(baseOutline);
        _areas         = Objects.requireNonNull(componentAreas);
    }

    StyleConf style() { return _styleConf; }

    Bounds currentBounds() { return _currentBounds; }

    Outline baseOutline() { return _baseOutline; }

    Optional<Shape> componentArea() {
        Shape contentClip = null;
        if ( _areas.bodyArea().exists() || _styleConf.margin().isPositive() )
            contentClip = get(UI.ComponentArea.BODY);

        return Optional.ofNullable(contentClip);
    }

    ComponentAreas areas() { return _areas; }

    public Area get( UI.ComponentArea areaType ) {
        switch ( areaType ) {
            case ALL:
                return null; // No clipping
            case BODY:
                return _areas.bodyArea().getFor(this.toRenderConf(), _areas); // all - exterior == interior + border
            case INTERIOR:
                return _areas.interiorArea().getFor(this.toRenderConf(), _areas); // all - exterior - border == content - border
            case BORDER:
                return _areas.borderArea().getFor(this.toRenderConf(), _areas); // all - exterior - interior
            case EXTERIOR:
                return _areas.exteriorArea().getFor(this.toRenderConf(), _areas); // all - border - interior
            default:
                return null;
        }
    }


    void paintClippedTo(UI.ComponentArea area, Graphics g, Runnable painter ) {
        toRenderConf().paintClippedTo(area, g, painter);
    }

    RenderConf toRenderConf() {
        return RenderConf.of(UI.Layer.BORDER, this);
    }

    ComponentConf with( StyleConf styleConf, JComponent component )
    {
        Outline outline = Outline.none();
        Border border = component.getBorder();
        if ( border instanceof StyleAndAnimationBorder ) {
            Insets base = ((StyleAndAnimationBorder<?>) border).getBaseInsets(true);
            outline = Outline.of(base.top, base.left, base.bottom, base.right);
        }

        boolean sameStyle   = _styleConf.equals(styleConf);
        boolean sameBounds  = _currentBounds.equals(component.getX(), component.getY(), component.getWidth(), component.getHeight());
        boolean sameOutline = _baseOutline.equals(outline);
        if ( sameStyle && sameBounds && sameOutline )
            return this;

        ComponentConf newConf = new ComponentConf(
                                    styleConf,
                                    Bounds.of(component.getX(), component.getY(), component.getWidth(), component.getHeight()),
                                    outline,
                                    _areas
                                );

        return new ComponentConf(
                        newConf._styleConf,
                        newConf._currentBounds,
                        newConf._baseOutline,
                        _areas.validate(this.toRenderConf(), newConf.toRenderConf())
                );
    }

    ComponentConf withStyle( StyleConf styleConf) {
        if ( _styleConf.equals(styleConf) )
            return this;

        return new ComponentConf(
                styleConf,
                    _currentBounds,
                    _baseOutline,
                    _areas
                );
    }

    /**
     *  Returns a new {@link ComponentConf} instance which only contains style information relevant
     *  to the provided {@link UI.Layer}. Style information on other layers is discarded.
     * @param layer The layer to retain.
     * @return A new {@link ComponentConf} instance which only contains style information relevant to the provided {@link UI.Layer}.
     */
    RenderConf onlyRetainingLayer( UI.Layer layer ) {
        return RenderConf.of(layer,this);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"[" +
                    "style="         + _styleConf + ", "+
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
        return Objects.equals(_styleConf, other._styleConf)
            && Objects.equals(_currentBounds, other._currentBounds)
            && Objects.equals(_baseOutline, other._baseOutline);
    }

    @Override
    public int hashCode() {
        if ( _wasAlreadyHashed )
            return _hashCode;

        _hashCode = Objects.hash(_styleConf, _currentBounds, _baseOutline);
        _wasAlreadyHashed = true;
        return _hashCode;
    }
}
