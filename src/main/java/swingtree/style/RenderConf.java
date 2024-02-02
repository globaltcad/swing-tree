package swingtree.style;

import swingtree.UI;
import swingtree.layout.Size;

import java.awt.Graphics;
import java.awt.Shape;
import java.awt.geom.Area;
import java.util.Objects;

/**
 *  An immutable snapshot of essential component state needed for rendering
 *  the style of a component.
 *  This is immutable to use it as a basis for caching.
 *  When the snapshot changes compared to the previous one, the image buffer based
 *  render cache is being invalidated and the component is rendered again
 *  (potentially with a new cached image buffer).
 */
final class RenderConf
{
    private static final RenderConf _NONE = new RenderConf(
                                                    BorderConf.none(),
                                                    BaseConf.none(),
                                                    StyleLayer.empty(),
                                                    Size.unknown(),
                                                    Outline.none(),
                                                    new ComponentAreas()
                                                );

    private final BorderConf     _border;
    private final BaseConf       _base;
    private final StyleLayer _layer;
    private final Size _size;
    private final Outline        _baseOutline;
    private final ComponentAreas _areas;

    private boolean _wasAlreadyHashed = false;
    private int     _hashCode         = 0; // cached hash code



    private RenderConf(
        BorderConf     border,
        BaseConf       base,
        StyleLayer     layers,
        Size           currentBounds,
        Outline        baseOutline,
        ComponentAreas areas
    ) {
        _border        = Objects.requireNonNull(border);
        _base          = Objects.requireNonNull(base);
        _layer = Objects.requireNonNull(layers);
        _size = Objects.requireNonNull(currentBounds);
        _baseOutline   = Objects.requireNonNull(baseOutline);
        _areas         = Objects.requireNonNull(areas);
    }

    static RenderConf of(
        BorderConf     border,
        BaseConf       base,
        StyleLayer     layers,
        Size           currentBounds,
        Outline        baseOutline,
        ComponentAreas areas
    ) {
        if (
            border        == _NONE._border &&
            base          == _NONE._base &&
            layers        == _NONE._layer &&
            currentBounds == _NONE._size &&
            baseOutline   == _NONE._baseOutline &&
            areas         == _NONE._areas
        )
            return _NONE;
        else
            return new RenderConf(border, base, layers, currentBounds, baseOutline, areas);
    }

    static RenderConf of(UI.Layer layer, ComponentConf fullConf) {
        return of(
            ( layer == UI.Layer.BORDER     ? fullConf.style().border() : fullConf.style().border().withColor(null) ),
            ( layer == UI.Layer.BACKGROUND ? fullConf.style().base()   : BaseConf.none() ),
            fullConf.style().layer(layer),
            fullConf.currentBounds().size(),
            fullConf.baseOutline(),
            fullConf.areas()
        );
    }

    BorderConf border() { return _border; }

    BaseConf base() { return _base; }

    StyleLayer layer() { return _layer; }

    Size size() { return _size; }

    Outline baseOutline() { return _baseOutline; }

    ComponentAreas areas() { return _areas; }


    void paintClippedTo(UI.ComponentArea area, Graphics g, Runnable painter ) {
        Shape oldClip = g.getClip();

        Shape newClip = get(area);
        if ( newClip != null && newClip != oldClip ) {
            newClip = StyleUtility.intersect(newClip, oldClip);
            g.setClip(newClip);
        }

        painter.run();

        g.setClip(oldClip);
    }

    public Area get(UI.ComponentArea areaType ) {
        switch ( areaType ) {
            case ALL:
                return null; // No clipping
            case BODY:
                return _areas.bodyArea().getFor(this, _areas); // all - exterior == interior + border
            case INTERIOR:
                return _areas.interiorArea().getFor(this, _areas); // all - exterior - border == content - border
            case BORDER:
                return _areas.borderArea().getFor(this, _areas); // all - exterior - interior
            case EXTERIOR:
                return _areas.exteriorArea().getFor(this, _areas); // all - border - interior
            default:
                return null;
        }
    }

    @Override
    public int hashCode() {
        if ( _wasAlreadyHashed )
            return _hashCode;

        _hashCode = Objects.hash(_border, _base, _layer, _size, _baseOutline);
        _wasAlreadyHashed = true;
        return _hashCode;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o == this ) return true;
        if ( o == null ) return false;
        if ( o.getClass() != this.getClass() ) return false;
        RenderConf other = (RenderConf) o;
        return Objects.equals(_border,        other._border) &&
               Objects.equals(_base,          other._base  ) &&
               Objects.equals(_layer,         other._layer ) &&
               Objects.equals(_size,          other._size  ) &&
               Objects.equals(_baseOutline,   other._baseOutline);
    }


}
