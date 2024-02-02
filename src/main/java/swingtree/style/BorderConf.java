package swingtree.style;

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
                                                null
    );

    public static BorderConf none() { return _NONE; }

    static BorderConf of(
        Arc     topLeftArc,
        Arc     topRightArc,
        Arc     bottomLeftArc,
        Arc     bottomRightArc,
        Outline borderWidths,
        Outline margin,
        Outline padding,
        Color   borderColor
    ) {
        if ( topLeftArc      == Arc.none() &&
             topRightArc     == Arc.none() &&
             bottomLeftArc   == Arc.none() &&
             bottomRightArc  == Arc.none() &&
             borderWidths    == Outline.none() &&
             margin          == Outline.none() &&
             padding         == Outline.none() &&
             borderColor     == null
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

    private final Color _borderColor;


    private BorderConf(
        Arc     topLeftArc,
        Arc     topRightArc,
        Arc     bottomLeftArc,
        Arc     bottomRightArc,
        Outline borderWidths,
        Outline margin,
        Outline padding,
        Color   borderColor
    ) {
        _topLeftArc      = topLeftArc;
        _topRightArc     = topRightArc;
        _bottomLeftArc   = bottomLeftArc;
        _bottomRightArc  = bottomRightArc;
        _borderWidths    = Objects.requireNonNull(borderWidths);
        _margin          = Objects.requireNonNull(margin);
        _padding         = Objects.requireNonNull(padding);
        _borderColor     = borderColor;
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

    public Optional<Color> color() { return Optional.ofNullable(_borderColor); }

    BorderConf withWidths(Outline borderWidths ) {
        if ( borderWidths == _borderWidths )
            return this;
        return BorderConf.of(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, borderWidths, _margin, _padding, _borderColor);
    }

    BorderConf withMargin(Outline margin ) {
        if ( margin == _margin )
            return this;
        return BorderConf.of(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths, margin, _padding, _borderColor);
    }

    BorderConf withPadding(Outline padding ) {
        if ( padding == _padding )
            return this;
        return BorderConf.of(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths, _margin, padding, _borderColor);
    }

    BorderConf withArcWidthAt(UI.Corner corner, double borderArcWidth ) {
        if ( corner == UI.Corner.EVERY )
            return this.withArcWidth(borderArcWidth);
        float arcHeight;
        switch ( corner ) {
            case TOP_LEFT:
                arcHeight = _topLeftArc != Arc.none() ? _topLeftArc.height() : 0;
                return BorderConf.of(Arc.of(borderArcWidth, arcHeight), _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths, _margin, _padding, _borderColor);
            case TOP_RIGHT:
                arcHeight = _topRightArc != Arc.none() ? _topRightArc.height() : 0;
                return BorderConf.of(_topLeftArc, Arc.of(borderArcWidth, arcHeight), _bottomLeftArc, _bottomRightArc, _borderWidths, _margin, _padding, _borderColor);
            case BOTTOM_LEFT:
                arcHeight = _bottomLeftArc != Arc.none() ? _bottomLeftArc.height() : 0;
                return BorderConf.of(_topLeftArc, _topRightArc, Arc.of(borderArcWidth, arcHeight), _bottomRightArc, _borderWidths, _margin, _padding, _borderColor);
            case BOTTOM_RIGHT:
                arcHeight = _bottomRightArc != Arc.none() ? _bottomRightArc.height() : 0;
                return BorderConf.of(_topLeftArc, _topRightArc, _bottomLeftArc, Arc.of(borderArcWidth, arcHeight), _borderWidths, _margin, _padding, _borderColor);
            default:
                throw new IllegalArgumentException("Unknown corner: " + corner);
        }
    }

    BorderConf withArcWidth(double borderArcWidth ) {
        return this.withArcWidthAt(UI.Corner.TOP_LEFT,     borderArcWidth)
                   .withArcWidthAt(UI.Corner.TOP_RIGHT,    borderArcWidth)
                   .withArcWidthAt(UI.Corner.BOTTOM_LEFT,  borderArcWidth)
                   .withArcWidthAt(UI.Corner.BOTTOM_RIGHT, borderArcWidth);
    }

    BorderConf withArcHeightAt(UI.Corner corner, double borderArcHeight ) {
        if ( corner == UI.Corner.EVERY )
            return this.withArcHeight(borderArcHeight);
        float arcWidth;
        switch ( corner ) {
            case TOP_LEFT:
                arcWidth = _topLeftArc != Arc.none() ? _topLeftArc.width() : 0;
                return BorderConf.of(Arc.of(arcWidth, borderArcHeight), _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths, _margin, _padding, _borderColor);
            case TOP_RIGHT:
                arcWidth = _topRightArc != Arc.none() ? _topRightArc.width() : 0;
                return BorderConf.of(_topLeftArc, Arc.of(arcWidth, borderArcHeight), _bottomLeftArc, _bottomRightArc, _borderWidths, _margin, _padding, _borderColor);
            case BOTTOM_LEFT:
                arcWidth = _bottomLeftArc != Arc.none() ? _bottomLeftArc.width() : 0;
                return BorderConf.of(_topLeftArc, _topRightArc, Arc.of(arcWidth, borderArcHeight), _bottomRightArc, _borderWidths, _margin, _padding, _borderColor);
            case BOTTOM_RIGHT:
                arcWidth = _bottomRightArc != Arc.none() ? _bottomRightArc.width() : 0;
                return BorderConf.of(_topLeftArc, _topRightArc, _bottomLeftArc, Arc.of(arcWidth, borderArcHeight), _borderWidths, _margin, _padding, _borderColor);
            default:
                throw new IllegalArgumentException("Unknown corner: " + corner);
        }
    }

    BorderConf withArcHeight(double borderArcHeight ) {
        return this.withArcHeightAt(UI.Corner.TOP_LEFT,     borderArcHeight)
                   .withArcHeightAt(UI.Corner.TOP_RIGHT,    borderArcHeight)
                   .withArcHeightAt(UI.Corner.BOTTOM_LEFT,  borderArcHeight)
                   .withArcHeightAt(UI.Corner.BOTTOM_RIGHT, borderArcHeight);
    }

    BorderConf withWidthAt(UI.Edge edge, float borderWidth ) {
        if ( edge == UI.Edge.EVERY )
            return this.withWidth(borderWidth);
        switch (edge) {
            case TOP:    return BorderConf.of(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths.withTop(borderWidth), _margin, _padding, _borderColor);
            case RIGHT:  return BorderConf.of(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths.withRight(borderWidth), _margin, _padding, _borderColor);
            case BOTTOM: return BorderConf.of(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths.withBottom(borderWidth), _margin, _padding, _borderColor);
            case LEFT:   return BorderConf.of(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths.withLeft(borderWidth), _margin, _padding, _borderColor);
            default:
                throw new IllegalArgumentException("Unknown side: " + edge);
        }
    }

    BorderConf withWidth(double borderWidth ) {
        return this.withWidthAt(UI.Edge.TOP,    (float) borderWidth)
                   .withWidthAt(UI.Edge.RIGHT,  (float) borderWidth)
                   .withWidthAt(UI.Edge.BOTTOM, (float) borderWidth)
                   .withWidthAt(UI.Edge.LEFT,   (float) borderWidth);
    }

    BorderConf withColor(Color borderColor ) {
        return BorderConf.of(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths, _margin, _padding, borderColor);
    }

    boolean allCornersShareTheSameArc() {
        return Objects.equals(_topLeftArc, _topRightArc) &&
               Objects.equals(_topLeftArc, _bottomLeftArc) &&
               Objects.equals(_topLeftArc, _bottomRightArc);
    }

    boolean allSidesShareTheSameWidth() {
        return Objects.equals(_borderWidths.top().orElse(null), _borderWidths.right().orElse(null)) &&
               Objects.equals(_borderWidths.top().orElse(null), _borderWidths.bottom().orElse(null)) &&
               Objects.equals(_borderWidths.top().orElse(null), _borderWidths.left().orElse(null));
    }

    boolean isVisible() {
        boolean hasAnyNonZeroArcs      = hasAnyNonZeroArcs();
        boolean hasAnyNonZeroWidths    = _borderWidths.isPositive();
        boolean hasAVisibleColor       = _borderColor != null && _borderColor.getAlpha() > 0;
        return hasAnyNonZeroArcs || hasAnyNonZeroWidths || hasAVisibleColor;
    }

    BorderConf _scale(double scale ) {
        if ( scale == 1.0 )
            return this;
        else if ( this == _NONE )
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
                    _borderColor
                );
    }

    BorderConf simplified() {
        if ( this == _NONE )
            return _NONE;

        Arc simplifiedTopLeftArc       = _topLeftArc.simplified();
        Arc simplifiedTopRightArc      = _topRightArc.simplified();
        Arc simplifiedBottomLeftArc    = _bottomLeftArc.simplified();
        Arc simplifiedBottomRightArc   = _bottomRightArc.simplified();
        Outline simplifiedBorderWidths = _borderWidths.simplified();
        Outline simplifiedMargin       = _margin; // Allowing the user to set an all 0 margin is needed for overriding the default margin (from former border!)
        Outline simplifiedPadding      = _padding.simplified();
        Color simplifiedBorderColor    = _borderColor != null && _borderColor.getAlpha() > 0 ? _borderColor : null;

        if ( simplifiedBorderColor == UI.COLOR_UNDEFINED)
            simplifiedBorderColor = null;

        boolean hasNoBorderWidths = simplifiedBorderWidths.equals(Outline.none());

        if ( hasNoBorderWidths ) {
            simplifiedBorderColor = null;
        }

        if (
            simplifiedTopLeftArc     == _topLeftArc &&
            simplifiedTopRightArc    == _topRightArc &&
            simplifiedBottomLeftArc  == _bottomLeftArc &&
            simplifiedBottomRightArc == _bottomRightArc &&
            simplifiedBorderWidths   == _borderWidths &&
            simplifiedMargin         == _margin &&
            simplifiedPadding        == _padding &&
            simplifiedBorderColor    == _borderColor
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
        hash = 97 * hash + ( _borderColor != null ? _borderColor.hashCode()    : 0 );
        return hash;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        BorderConf rhs = (BorderConf) obj;
        return
            Objects.equals(_topLeftArc,     rhs._topLeftArc)     &&
            Objects.equals(_topRightArc,    rhs._topRightArc)    &&
            Objects.equals(_bottomLeftArc,  rhs._bottomLeftArc)  &&
            Objects.equals(_bottomRightArc, rhs._bottomRightArc) &&
            Objects.equals(_borderColor,    rhs._borderColor)    &&
            Objects.equals(_borderWidths,   rhs._borderWidths)   &&
            Objects.equals(_margin,         rhs._margin)         &&
            Objects.equals(_padding,        rhs._padding);
    }

    @Override
    public String toString()
    {
        if ( this == _NONE )
            return this.getClass().getSimpleName() + "[NONE]";

        String arcsString;
        if ( allCornersShareTheSameArc() ) {
            boolean arcWidthEqualsHeight = _topLeftArc == Arc.none() || _topLeftArc.width() == _topLeftArc.height();
            arcsString = (
                        arcWidthEqualsHeight
                            ? "radius="   + ( _topLeftArc == Arc.none() ? "?" : Arc._toString(_topLeftArc.width()) )
                            : "arcWidth=" + Arc._toString(_topLeftArc.width()) + ", arcHeight=" + Arc._toString(_topLeftArc.height())
                    );
        } else {
            arcsString =
                    "topLeftArc="       + StyleUtility.toString(_topLeftArc)     +
                    ", topRightArc="    + StyleUtility.toString(_topRightArc)    +
                    ", bottomLeftArc="  + StyleUtility.toString(_bottomLeftArc)  +
                    ", bottomRightArc=" + StyleUtility.toString(_bottomRightArc);
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

        return this.getClass().getSimpleName() + "[" +
                    arcsString + ", " +
                    borderWidthsString + ", " +
                    "margin=" + _margin + ", " +
                    "padding=" + _padding + ", " +
                    "color=" + StyleUtility.toString(_borderColor) +
                "]";
    }

    private String _toString( Float value ) {
        return String.valueOf(value).replace(".0", "");
    }
}
