package swingtree.style;

import com.google.errorprone.annotations.Immutable;
import swingtree.UI;

import java.awt.Color;
import java.util.Objects;
import java.util.Optional;

/**
 *  An immutable config container for border styles that is part of
 *  a {@link StyleConf} configuration object.
 *  The state of this object is updated through with-methods that return
 *  a new instance of this class with the updated state.
 */
@Immutable
final class BorderConf
{
    private static final BorderConf _NONE = new BorderConf(
                                                Arc.none(),
                                                Arc.none(),
                                                Arc.none(),
                                                Arc.none(),
                                                Outline.none(),
                                                Outline.none(),
                                                Outline.none(),
                                                BorderColorsConf.none()
                                            );

    public static BorderConf none() { return _NONE; }

    static BorderConf of(
        Arc              topLeftArc,
        Arc              topRightArc,
        Arc              bottomLeftArc,
        Arc              bottomRightArc,
        Outline          borderWidths,
        Outline          margin,
        Outline          padding,
        BorderColorsConf borderColor
    ) {
        if ( topLeftArc    .equals( Arc.none()     ) &&
             topRightArc   .equals( Arc.none()     ) &&
             bottomLeftArc .equals( Arc.none()     ) &&
             bottomRightArc.equals( Arc.none()     ) &&
             borderWidths  .equals( Outline.none() ) &&
             margin        .equals( Outline.none() ) &&
             padding       .equals( Outline.none() ) &&
             borderColor   .equals( BorderColorsConf.none() )
        )
            return _NONE;
        else
            return new BorderConf(topLeftArc, topRightArc, bottomLeftArc, bottomRightArc, borderWidths, margin, padding, borderColor);
    }


    private final Arc   _topLeftArc;
    private final Arc   _topRightArc;
    private final Arc   _bottomLeftArc;
    private final Arc   _bottomRightArc;

    private final Outline _borderWidths;
    private final Outline _margin;
    private final Outline _padding;

    private final BorderColorsConf _borderColors;


    private BorderConf(
        Arc              topLeftArc,
        Arc              topRightArc,
        Arc              bottomLeftArc,
        Arc              bottomRightArc,
        Outline          borderWidths,
        Outline          margin,
        Outline          padding,
        BorderColorsConf borderColors
    ) {
        _topLeftArc      = topLeftArc;
        _topRightArc     = topRightArc;
        _bottomLeftArc   = bottomLeftArc;
        _bottomRightArc  = bottomRightArc;
        _borderWidths    = Objects.requireNonNull(borderWidths);
        _margin          = Objects.requireNonNull(margin);
        _padding         = Objects.requireNonNull(padding);
        _borderColors = Objects.requireNonNull(borderColors);
    }

    public Optional<Arc> topLeftArc() { return _topLeftArc.equals(Arc.none()) ? Optional.empty() : Optional.of(_topLeftArc); }

    public Optional<Arc> topRightArc() { return _topRightArc.equals(Arc.none()) ? Optional.empty() : Optional.of(_topRightArc); }

    public Optional<Arc> bottomLeftArc() { return _bottomLeftArc.equals(Arc.none()) ? Optional.empty() : Optional.of(_bottomLeftArc); }

    public Optional<Arc> bottomRightArc() { return _bottomRightArc.equals(Arc.none()) ? Optional.empty() : Optional.of(_bottomRightArc); }

    public boolean hasAnyNonZeroArcs() {
        return  ( !_topLeftArc    .equals( Arc.none() ) && _topLeftArc.width()     > 0 && _topLeftArc.height()     > 0 ) ||
                ( !_topRightArc   .equals( Arc.none() ) && _topRightArc.width()    > 0 && _topRightArc.height()    > 0 ) ||
                ( !_bottomLeftArc .equals( Arc.none() ) && _bottomLeftArc.width()  > 0 && _bottomLeftArc.height()  > 0 ) ||
                ( !_bottomRightArc.equals( Arc.none() ) && _bottomRightArc.width() > 0 && _bottomRightArc.height() > 0 );
    }

    public float topLeftRadius() { return !_topLeftArc.equals(Arc.none()) ? (_topLeftArc.width() + _topLeftArc.height()) / 2 : 0; }

    public float topRightRadius() { return !_topRightArc.equals(Arc.none()) ? (_topRightArc.width() + _topRightArc.height()) / 2 : 0; }

    public float bottomLeftRadius() { return !_bottomLeftArc.equals(Arc.none()) ? (_bottomLeftArc.width() + _bottomLeftArc.height()) / 2 : 0; }

    public float bottomRightRadius() { return !_bottomRightArc.equals(Arc.none()) ? (_bottomRightArc.width() + _bottomRightArc.height()) / 2 : 0; }

    public Outline widths() { return _borderWidths; }

    public Outline margin() { return _margin; }

    public Outline padding() { return _padding; }

    public BorderColorsConf colors() {
        return _borderColors;
    }

    BorderConf withWidths( Outline borderWidths ) {
        if ( borderWidths.equals(_borderWidths) )
            return this;
        return BorderConf.of(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, borderWidths, _margin, _padding, _borderColors);
    }

    BorderConf withMargin( Outline margin ) {
        if ( margin.equals(_margin) )
            return this;
        return BorderConf.of(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths, margin, _padding, _borderColors);
    }

    BorderConf withPadding( Outline padding ) {
        if ( padding.equals(_padding) )
            return this;
        return BorderConf.of(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths, _margin, padding, _borderColors);
    }

    BorderConf withArcWidthAt( UI.Corner corner, double borderArcWidth ) {
        if ( corner == UI.Corner.EVERY )
            return this.withArcWidth(borderArcWidth);
        float arcHeight;
        switch ( corner ) {
            case TOP_LEFT:
                arcHeight = !_topLeftArc.equals(Arc.none()) ? _topLeftArc.height() : 0;
                return BorderConf.of(Arc.of(borderArcWidth, arcHeight), _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths, _margin, _padding, _borderColors);
            case TOP_RIGHT:
                arcHeight = !_topRightArc.equals(Arc.none()) ? _topRightArc.height() : 0;
                return BorderConf.of(_topLeftArc, Arc.of(borderArcWidth, arcHeight), _bottomLeftArc, _bottomRightArc, _borderWidths, _margin, _padding, _borderColors);
            case BOTTOM_LEFT:
                arcHeight = !_bottomLeftArc.equals(Arc.none()) ? _bottomLeftArc.height() : 0;
                return BorderConf.of(_topLeftArc, _topRightArc, Arc.of(borderArcWidth, arcHeight), _bottomRightArc, _borderWidths, _margin, _padding, _borderColors);
            case BOTTOM_RIGHT:
                arcHeight = !_bottomRightArc.equals(Arc.none()) ? _bottomRightArc.height() : 0;
                return BorderConf.of(_topLeftArc, _topRightArc, _bottomLeftArc, Arc.of(borderArcWidth, arcHeight), _borderWidths, _margin, _padding, _borderColors);
            default:
                throw new IllegalArgumentException("Unknown corner: " + corner);
        }
    }

    BorderConf withArcWidth( double borderArcWidth ) {
        return this.withArcWidthAt(UI.Corner.TOP_LEFT,     borderArcWidth)
                   .withArcWidthAt(UI.Corner.TOP_RIGHT,    borderArcWidth)
                   .withArcWidthAt(UI.Corner.BOTTOM_LEFT,  borderArcWidth)
                   .withArcWidthAt(UI.Corner.BOTTOM_RIGHT, borderArcWidth);
    }

    BorderConf withArcHeightAt( UI.Corner corner, double borderArcHeight ) {
        if ( corner == UI.Corner.EVERY )
            return this.withArcHeight(borderArcHeight);
        float arcWidth;
        switch ( corner ) {
            case TOP_LEFT:
                arcWidth = !_topLeftArc.equals(Arc.none()) ? _topLeftArc.width() : 0;
                return BorderConf.of(Arc.of(arcWidth, borderArcHeight), _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths, _margin, _padding, _borderColors);
            case TOP_RIGHT:
                arcWidth = !_topRightArc.equals(Arc.none()) ? _topRightArc.width() : 0;
                return BorderConf.of(_topLeftArc, Arc.of(arcWidth, borderArcHeight), _bottomLeftArc, _bottomRightArc, _borderWidths, _margin, _padding, _borderColors);
            case BOTTOM_LEFT:
                arcWidth = !_bottomLeftArc.equals(Arc.none()) ? _bottomLeftArc.width() : 0;
                return BorderConf.of(_topLeftArc, _topRightArc, Arc.of(arcWidth, borderArcHeight), _bottomRightArc, _borderWidths, _margin, _padding, _borderColors);
            case BOTTOM_RIGHT:
                arcWidth = !_bottomRightArc.equals(Arc.none()) ? _bottomRightArc.width() : 0;
                return BorderConf.of(_topLeftArc, _topRightArc, _bottomLeftArc, Arc.of(arcWidth, borderArcHeight), _borderWidths, _margin, _padding, _borderColors);
            default:
                throw new IllegalArgumentException("Unknown corner: " + corner);
        }
    }

    BorderConf withArcHeight( double borderArcHeight ) {
        return this.withArcHeightAt(UI.Corner.TOP_LEFT,     borderArcHeight)
                   .withArcHeightAt(UI.Corner.TOP_RIGHT,    borderArcHeight)
                   .withArcHeightAt(UI.Corner.BOTTOM_LEFT,  borderArcHeight)
                   .withArcHeightAt(UI.Corner.BOTTOM_RIGHT, borderArcHeight);
    }

    BorderConf withWidthAt( UI.Edge edge, float borderWidth ) {
        if ( edge == UI.Edge.EVERY )
            return this.withWidth(borderWidth);
        switch (edge) {
            case TOP:    return BorderConf.of(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths.withTop(borderWidth), _margin, _padding, _borderColors);
            case RIGHT:  return BorderConf.of(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths.withRight(borderWidth), _margin, _padding, _borderColors);
            case BOTTOM: return BorderConf.of(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths.withBottom(borderWidth), _margin, _padding, _borderColors);
            case LEFT:   return BorderConf.of(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths.withLeft(borderWidth), _margin, _padding, _borderColors);
            default:
                throw new IllegalArgumentException("Unknown side: " + edge);
        }
    }

    BorderConf withWidth( double borderWidth ) {
        return this.withWidthAt(UI.Edge.TOP,    (float) borderWidth)
                   .withWidthAt(UI.Edge.RIGHT,  (float) borderWidth)
                   .withWidthAt(UI.Edge.BOTTOM, (float) borderWidth)
                   .withWidthAt(UI.Edge.LEFT,   (float) borderWidth);
    }

    BorderConf withColor( Color borderColor ) {
        if ( StyleUtil.isUndefinedColor(borderColor) )
            return this;
        return BorderConf.of(
                _topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc,
                _borderWidths, _margin, _padding,
                BorderColorsConf.of(borderColor)
            );
    }

    BorderConf withColors( Color top, Color right, Color bottom, Color left ) {
        return BorderConf.of(
                _topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc,
                _borderWidths, _margin, _padding,
                BorderColorsConf.of(top, right, bottom, left)
            );
    }

    BorderConf withColorAt( UI.Edge edge, Color borderColor ) {
        if ( edge == UI.Edge.EVERY )
            return this.withColor(borderColor);
        switch (edge) {
            case TOP:    return BorderConf.of(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths, _margin, _padding, _borderColors.withTop(borderColor));
            case RIGHT:  return BorderConf.of(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths, _margin, _padding, _borderColors.withRight(borderColor));
            case BOTTOM: return BorderConf.of(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths, _margin, _padding, _borderColors.withBottom(borderColor));
            case LEFT:   return BorderConf.of(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths, _margin, _padding, _borderColors.withLeft(borderColor));
            default:
                throw new IllegalArgumentException("Unknown side: " + edge);
        }
    }

    boolean allCornersShareTheSameArc() {
        return _topLeftArc.equals(_topRightArc) &&
               _topLeftArc.equals(_bottomLeftArc) &&
               _topLeftArc.equals(_bottomRightArc);
    }

    boolean allSidesShareTheSameWidth() {
        return Objects.equals(_borderWidths.top().orElse(null), _borderWidths.right().orElse(null)) &&
               Objects.equals(_borderWidths.top().orElse(null), _borderWidths.bottom().orElse(null)) &&
               Objects.equals(_borderWidths.top().orElse(null), _borderWidths.left().orElse(null));
    }

    boolean isVisible() {
        boolean hasAnyNonZeroArcs      = hasAnyNonZeroArcs();
        boolean hasAnyNonZeroWidths    = _borderWidths.isPositive();
        boolean hasAVisibleColor       = _borderColors.isAnyVisible();
        return hasAnyNonZeroArcs || hasAnyNonZeroWidths || hasAVisibleColor;
    }

    BorderConf _scale( double scale ) {
        if ( scale == 1.0 )
            return this;
        else if ( this.equals(_NONE) )
            return _NONE;
        else
            return BorderConf.of(
                    _topLeftArc.scale(scale),
                    _topRightArc.scale(scale),
                    _bottomLeftArc.scale(scale),
                    _bottomRightArc.scale(scale),
                    _borderWidths.scale(scale),
                    _margin.scale(scale),
                    _padding.scale(scale),
                    _borderColors
                );
    }

    BorderConf simplified() {
        if ( this.equals(_NONE) )
            return _NONE;

        Arc simplifiedTopLeftArc       = _topLeftArc.simplified();
        Arc simplifiedTopRightArc      = _topRightArc.simplified();
        Arc simplifiedBottomLeftArc    = _bottomLeftArc.simplified();
        Arc simplifiedBottomRightArc   = _bottomRightArc.simplified();
        Outline simplifiedBorderWidths = _borderWidths.simplified();
        Outline simplifiedMargin       = _margin.simplified();
        Outline simplifiedPadding      = _padding; // Allowing the user to set an all 0 padding is needed for overriding the default insets (from former border!)
        BorderColorsConf simplifiedBorderColor = _borderColors.isAnyVisible() ? _borderColors : BorderColorsConf.none();

        boolean hasNoBorderWidths = simplifiedBorderWidths.equals(Outline.none());

        if ( hasNoBorderWidths ) {
            simplifiedBorderColor = BorderColorsConf.none();
        }

        if (
            simplifiedTopLeftArc    .equals(_topLeftArc    ) &&
            simplifiedTopRightArc   .equals(_topRightArc   ) &&
            simplifiedBottomLeftArc .equals(_bottomLeftArc ) &&
            simplifiedBottomRightArc.equals(_bottomRightArc) &&
            simplifiedBorderWidths  .equals(_borderWidths  ) &&
            simplifiedMargin        .equals(_margin        ) &&
            simplifiedPadding       .equals(_padding       ) &&
            Objects.equals(simplifiedBorderColor, _borderColors)
        )
            return this;
        else
            return BorderConf.of(
                        simplifiedTopLeftArc,
                        simplifiedTopRightArc,
                        simplifiedBottomLeftArc,
                        simplifiedBottomRightArc,
                        simplifiedBorderWidths,
                        simplifiedMargin,
                        simplifiedPadding,
                        simplifiedBorderColor
                    );
    }

    BorderConf correctedForRounding() {
        Outline correction = _borderWidths.plus(_padding).plus(_margin)
                                .map( v -> v % 1 )
                                .map( v -> v > 0f ? 1f - v : 0f )
                                .map( v -> v == 0f ? null : v )
                                .simplified();

        return this.withMargin(_margin.plus(correction));
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
        hash = 97 * hash + ( _borderColors != null ? _borderColors.hashCode() : 0 );
        return hash;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        BorderConf rhs = (BorderConf) obj;
        return
            _topLeftArc    .equals(rhs._topLeftArc)     &&
            _topRightArc   .equals(rhs._topRightArc)    &&
            _bottomLeftArc .equals(rhs._bottomLeftArc)  &&
            _bottomRightArc.equals(rhs._bottomRightArc) &&
            _borderWidths  .equals(rhs._borderWidths)   &&
            _margin        .equals(rhs._margin)         &&
            _padding       .equals(rhs._padding)        &&
            Objects.equals(_borderColors,    rhs._borderColors);
    }

    @Override
    public String toString()
    {
        if ( this.equals(_NONE) )
            return this.getClass().getSimpleName() + "[NONE]";

        String arcsString;
        if ( allCornersShareTheSameArc() ) {
            boolean arcWidthEqualsHeight = _topLeftArc.equals(Arc.none()) || _topLeftArc.width() == _topLeftArc.height();
            arcsString = (
                        arcWidthEqualsHeight
                            ? "radius="   + ( _topLeftArc.equals(Arc.none()) ? "?" : Arc._toString(_topLeftArc.width()) )
                            : "arcWidth=" + Arc._toString(_topLeftArc.width()) + ", arcHeight=" + Arc._toString(_topLeftArc.height())
                    );
        } else {
            arcsString =
                    "topLeftArc="       + StyleUtil.toString(_topLeftArc)     +
                    ", topRightArc="    + StyleUtil.toString(_topRightArc)    +
                    ", bottomLeftArc="  + StyleUtil.toString(_bottomLeftArc)  +
                    ", bottomRightArc=" + StyleUtil.toString(_bottomRightArc);
        }

        String borderWidthsString;
        if ( allSidesShareTheSameWidth() ) {
            borderWidthsString = "width=" + _borderWidths.top().map(this::_toString).orElse("?");
        } else {
            borderWidthsString =
                    "topWidth="      + _borderWidths.top().map(this::_toString).orElse("?")    +
                    ", rightWidth="  + _borderWidths.right().map(this::_toString).orElse("?")  +
                    ", bottomWidth=" + _borderWidths.bottom().map(this::_toString).orElse("?") +
                    ", leftWidth="   + _borderWidths.left().map(this::_toString).orElse("?");
        }

        String colors;
        if ( _borderColors.equals(BorderColorsConf.none()) )
            colors = "color=?";
        else if ( _borderColors.isHomogeneous() )
            colors = "color=" + _borderColors.top().map(StyleUtil::toString).orElse("?");
        else
            colors = "colors=" + _borderColors;

        return this.getClass().getSimpleName() + "[" +
                    arcsString + ", " +
                    borderWidthsString + ", " +
                    "margin=" + _margin + ", " +
                    "padding=" + _padding + ", " +
                    colors +
                "]";
    }

    private String _toString( Float value ) {
        return String.valueOf(value).replace(".0", "");
    }
}
