package swingtree.style;

import swingtree.UI;
import swingtree.layout.Size;

import java.util.Objects;
import java.util.Optional;

/**
 *  An immutable config container for border styles that is part of
 *  a {@link StyleConf} configuration object.
 *  The state of this object is updated through with-methods that return
 *  a new instance of this class with the updated state.
 */
final class BoxModelConf
{
    private static final BoxModelConf _NONE = new BoxModelConf(
                                                Arc.none(),
                                                Arc.none(),
                                                Arc.none(),
                                                Arc.none(),
                                                Outline.none(),
                                                Outline.none(),
                                                Outline.none(),
                                                Outline.none(),
                                                Size.unknown()
                                            );

    public static BoxModelConf none() { return _NONE; }

    static BoxModelConf of(
        Arc     topLeftArc,
        Arc     topRightArc,
        Arc     bottomLeftArc,
        Arc     bottomRightArc,
        Outline borderWidths,
        Outline margin,
        Outline padding,
        Outline baseOutline,
        Size    size
    ) {
        if ( topLeftArc      .equals( Arc.none() ) &&
             topRightArc     .equals( Arc.none() ) &&
             bottomLeftArc   .equals( Arc.none() ) &&
             bottomRightArc  .equals( Arc.none() ) &&
             borderWidths    .equals( Outline.none() ) &&
             margin          .equals( Outline.none() ) &&
             padding         .equals( Outline.none() ) &&
             baseOutline     .equals( Outline.none() ) &&
             size            .equals( Size.unknown() )
        )
            return _NONE;
        else {
            return new BoxModelConf(
                                        topLeftArc,    topRightArc,
                                        bottomLeftArc, bottomRightArc,
                                        borderWidths,  margin,
                                        padding,       baseOutline,
                                        size
                                    );
        }
    }

    static BoxModelConf of( BorderConf borderConf, Outline baseOutline, Size size )   {
        return BoxModelConf.of(
            borderConf.topLeftArc().orElse(Arc.none()),
            borderConf.topRightArc().orElse(Arc.none()),
            borderConf.bottomLeftArc().orElse(Arc.none()),
            borderConf.bottomRightArc().orElse(Arc.none()),
            borderConf.widths(),
            borderConf.margin(),
            borderConf.padding(),
            baseOutline,
            size
        );
    }


    private final Arc   _topLeftArc;
    private final Arc   _topRightArc;
    private final Arc   _bottomLeftArc;
    private final Arc   _bottomRightArc;

    private final Outline _borderWidths;
    private final Outline _margin;
    private final Outline _padding;
    private final Outline _baseOutline;

    private final Size    _size;


    private BoxModelConf(
        Arc     topLeftArc,
        Arc     topRightArc,
        Arc     bottomLeftArc,
        Arc     bottomRightArc,
        Outline borderWidths,
        Outline margin,
        Outline padding,
        Outline baseOutline,
        Size    size
    ) {
        _topLeftArc      = topLeftArc;
        _topRightArc     = topRightArc;
        _bottomLeftArc   = bottomLeftArc;
        _bottomRightArc  = bottomRightArc;
        _borderWidths    = Objects.requireNonNull(borderWidths);
        _margin          = Objects.requireNonNull(margin);
        _padding         = Objects.requireNonNull(padding);
        _baseOutline     = Objects.requireNonNull(baseOutline);
        _size            = Objects.requireNonNull(size);
    }

    ComponentAreas areas() {
        return ComponentAreas.of(this);
    }

    public Optional<Arc> topLeftArc() { return _topLeftArc == Arc.none() ? Optional.empty() : Optional.of(_topLeftArc); }

    public Optional<Arc> topRightArc() { return _topRightArc == Arc.none() ? Optional.empty() : Optional.of(_topRightArc); }

    public Optional<Arc> bottomLeftArc() { return _bottomLeftArc == Arc.none() ? Optional.empty() : Optional.of(_bottomLeftArc); }

    public Optional<Arc> bottomRightArc() { return _bottomRightArc == Arc.none() ? Optional.empty() : Optional.of(_bottomRightArc); }

    public boolean hasAnyNonZeroArcs() {
        return _topLeftArc     != Arc.none() && _topLeftArc.width()     > 0 && _topLeftArc.height()     > 0 ||
               _topRightArc    != Arc.none() && _topRightArc.width()    > 0 && _topRightArc.height()    > 0 ||
               _bottomLeftArc  != Arc.none() && _bottomLeftArc.width()  > 0 && _bottomLeftArc.height()  > 0 ||
               _bottomRightArc != Arc.none() && _bottomRightArc.width() > 0 && _bottomRightArc.height() > 0;
    }

    public float topLeftRadius() { return _topLeftArc != Arc.none() ? (_topLeftArc.width() + _topLeftArc.height()) / 2 : 0; }

    public float topRightRadius() { return _topRightArc != Arc.none() ? (_topRightArc.width() + _topRightArc.height()) / 2 : 0; }

    public float bottomLeftRadius() { return _bottomLeftArc != Arc.none() ? (_bottomLeftArc.width() + _bottomLeftArc.height()) / 2 : 0; }

    public float bottomRightRadius() { return _bottomRightArc != Arc.none() ? (_bottomRightArc.width() + _bottomRightArc.height()) / 2 : 0; }

    public Outline widths() { return _borderWidths; }

    public Outline margin() { return _margin; }

    public Outline padding() { return _padding; }

    public Outline baseOutline() { return _baseOutline; }
    
    public Size size() { return _size; }

    BoxModelConf withArcWidthAt(UI.Corner corner, double borderArcWidth ) {
        if ( corner == UI.Corner.EVERY )
            return this.withArcWidth(borderArcWidth);
        float arcHeight;
        switch ( corner ) {
            case TOP_LEFT:
                arcHeight = !_topLeftArc.equals(Arc.none()) ? _topLeftArc.height() : 0;
                return BoxModelConf.of(Arc.of(borderArcWidth, arcHeight), _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths, _margin, _padding, _baseOutline, _size);
            case TOP_RIGHT:
                arcHeight = !_topRightArc.equals(Arc.none()) ? _topRightArc.height() : 0;
                return BoxModelConf.of(_topLeftArc, Arc.of(borderArcWidth, arcHeight), _bottomLeftArc, _bottomRightArc, _borderWidths, _margin, _padding, _baseOutline, _size);
            case BOTTOM_LEFT:
                arcHeight = !_bottomLeftArc.equals(Arc.none()) ? _bottomLeftArc.height() : 0;
                return BoxModelConf.of(_topLeftArc, _topRightArc, Arc.of(borderArcWidth, arcHeight), _bottomRightArc, _borderWidths, _margin, _padding, _baseOutline, _size);
            case BOTTOM_RIGHT:
                arcHeight = !_bottomRightArc.equals(Arc.none()) ? _bottomRightArc.height() : 0;
                return BoxModelConf.of(_topLeftArc, _topRightArc, _bottomLeftArc, Arc.of(borderArcWidth, arcHeight), _borderWidths, _margin, _padding, _baseOutline, _size);
            default:
                throw new IllegalArgumentException("Unknown corner: " + corner);
        }
    }

    BoxModelConf withArcWidth(double borderArcWidth ) {
        return this.withArcWidthAt(UI.Corner.TOP_LEFT,     borderArcWidth)
                   .withArcWidthAt(UI.Corner.TOP_RIGHT,    borderArcWidth)
                   .withArcWidthAt(UI.Corner.BOTTOM_LEFT,  borderArcWidth)
                   .withArcWidthAt(UI.Corner.BOTTOM_RIGHT, borderArcWidth);
    }

    BoxModelConf withArcHeightAt(UI.Corner corner, double borderArcHeight ) {
        if ( corner == UI.Corner.EVERY )
            return this.withArcHeight(borderArcHeight);
        float arcWidth;
        switch ( corner ) {
            case TOP_LEFT:
                arcWidth = _topLeftArc != Arc.none() ? _topLeftArc.width() : 0;
                return BoxModelConf.of(Arc.of(arcWidth, borderArcHeight), _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths, _margin, _padding, _baseOutline, _size);
            case TOP_RIGHT:
                arcWidth = _topRightArc != Arc.none() ? _topRightArc.width() : 0;
                return BoxModelConf.of(_topLeftArc, Arc.of(arcWidth, borderArcHeight), _bottomLeftArc, _bottomRightArc, _borderWidths, _margin, _padding, _baseOutline, _size);
            case BOTTOM_LEFT:
                arcWidth = _bottomLeftArc != Arc.none() ? _bottomLeftArc.width() : 0;
                return BoxModelConf.of(_topLeftArc, _topRightArc, Arc.of(arcWidth, borderArcHeight), _bottomRightArc, _borderWidths, _margin, _padding, _baseOutline, _size);
            case BOTTOM_RIGHT:
                arcWidth = _bottomRightArc != Arc.none() ? _bottomRightArc.width() : 0;
                return BoxModelConf.of(_topLeftArc, _topRightArc, _bottomLeftArc, Arc.of(arcWidth, borderArcHeight), _borderWidths, _margin, _padding, _baseOutline, _size);
            default:
                throw new IllegalArgumentException("Unknown corner: " + corner);
        }
    }

    BoxModelConf withArcHeight(double borderArcHeight ) {
        return this.withArcHeightAt(UI.Corner.TOP_LEFT,     borderArcHeight)
                   .withArcHeightAt(UI.Corner.TOP_RIGHT,    borderArcHeight)
                   .withArcHeightAt(UI.Corner.BOTTOM_LEFT,  borderArcHeight)
                   .withArcHeightAt(UI.Corner.BOTTOM_RIGHT, borderArcHeight);
    }

    BoxModelConf withWidthAt(UI.Edge edge, float borderWidth ) {
        if ( edge == UI.Edge.EVERY )
            return this.withWidth(borderWidth);
        switch (edge) {
            case TOP:    return BoxModelConf.of(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths.withTop(borderWidth), _margin, _padding, _baseOutline, _size);
            case RIGHT:  return BoxModelConf.of(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths.withRight(borderWidth), _margin, _padding, _baseOutline, _size);
            case BOTTOM: return BoxModelConf.of(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths.withBottom(borderWidth), _margin, _padding, _baseOutline, _size);
            case LEFT:   return BoxModelConf.of(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths.withLeft(borderWidth), _margin, _padding, _baseOutline, _size);
            default:
                throw new IllegalArgumentException("Unknown side: " + edge);
        }
    }

    BoxModelConf withWidth(double borderWidth ) {
        return this.withWidthAt(UI.Edge.TOP,    (float) borderWidth)
                   .withWidthAt(UI.Edge.RIGHT,  (float) borderWidth)
                   .withWidthAt(UI.Edge.BOTTOM, (float) borderWidth)
                   .withWidthAt(UI.Edge.LEFT,   (float) borderWidth);
    }

    boolean allCornersShareTheSameArc() {
        return Objects.equals(_topLeftArc, _topRightArc) &&
               Objects.equals(_topLeftArc, _bottomLeftArc) &&
               Objects.equals(_topLeftArc, _bottomRightArc);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + _topLeftArc.hashCode();
        hash = 97 * hash + _topRightArc.hashCode();
        hash = 97 * hash + _bottomLeftArc.hashCode();
        hash = 97 * hash + _bottomRightArc.hashCode();
        hash = 97 * hash + _borderWidths.hashCode();
        hash = 97 * hash + _margin.hashCode();
        hash = 97 * hash + _padding.hashCode();
        hash = 97 * hash + _baseOutline.hashCode();
        hash = 97 * hash + _size.hashCode();
        return hash;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        BoxModelConf rhs = (BoxModelConf) obj;
        return
            Objects.equals(_topLeftArc,     rhs._topLeftArc)     &&
            Objects.equals(_topRightArc,    rhs._topRightArc)    &&
            Objects.equals(_bottomLeftArc,  rhs._bottomLeftArc)  &&
            Objects.equals(_bottomRightArc, rhs._bottomRightArc) &&
            Objects.equals(_borderWidths,   rhs._borderWidths)   &&
            Objects.equals(_margin,         rhs._margin)         &&
            Objects.equals(_padding,        rhs._padding)        &&
            Objects.equals(_baseOutline,    rhs._baseOutline)    &&
            Objects.equals(_size,           rhs._size);
    }

    @Override
    public String toString()
    {
        if ( this == _NONE )
            return this.getClass().getSimpleName() + "[NONE]";

        return this.getClass().getSimpleName() + "[" +
                    "topLeftArc="     + _topLeftArc     + ", " +
                    "topRightArc="    + _topRightArc    + ", " +
                    "bottomLeftArc="  + _bottomLeftArc  + ", " +
                    "bottomRightArc=" + _bottomRightArc + ", " +
                    "borderWidths="   + _borderWidths   + ", " +
                    "margin="         + _margin         + ", " +
                    "padding="        + _padding        + ", " +
                    "baseOutline="    + _baseOutline    + ", " +
                    "size="           + _size           +
                "]";
    }
}
