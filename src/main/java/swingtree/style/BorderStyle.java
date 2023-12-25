package swingtree.style;

import swingtree.UI;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *  An immutable config container for border styles that is part of
 *  a {@link Style} configuration object.
 *  The state of this object is updated through with-methods that return
 *  a new instance of this class with the updated state.
 */
final class BorderStyle
{
    private static final BorderStyle _NONE = new BorderStyle(
                                                Arc.none(),
                                                Arc.none(),
                                                Arc.none(),
                                                Arc.none(),
                                                Outline.none(),
                                                Outline.none(),
                                                Outline.none(),
                                                null,
                                                NamedStyles.of(NamedStyle.of(StyleUtility.DEFAULT_KEY, GradientStyle.none()))
                                            );

    public static BorderStyle none() { return _NONE; }

    private final Arc   _topLeftArc;
    private final Arc   _topRightArc;
    private final Arc   _bottomLeftArc;
    private final Arc   _bottomRightArc;

    private final Outline _borderWidths;
    private final Outline _margin;
    private final Outline _padding;

    private final Color _borderColor;
    private final NamedStyles<GradientStyle> _gradients;


    private BorderStyle(
        Arc     topLeftArc,
        Arc     topRightArc,
        Arc     bottomLeftArc,
        Arc     bottomRightArc,
        Outline borderWidths,
        Outline margin,
        Outline padding,
        Color   borderColor,
        NamedStyles<GradientStyle> gradients
    ) {
        _topLeftArc      = topLeftArc;
        _topRightArc     = topRightArc;
        _bottomLeftArc   = bottomLeftArc;
        _bottomRightArc  = bottomRightArc;
        _borderWidths    = Objects.requireNonNull(borderWidths);
        _margin          = Objects.requireNonNull(margin);
        _padding         = Objects.requireNonNull(padding);
        _borderColor     = borderColor;
        _gradients       = Objects.requireNonNull(gradients);
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

    public int topLeftRadius() { return _topLeftArc != Arc.none() ? (_topLeftArc.width() + _topLeftArc.height()) / 2 : 0; }

    public int topRightRadius() { return _topRightArc != Arc.none() ? (_topRightArc.width() + _topRightArc.height()) / 2 : 0; }

    public int bottomLeftRadius() { return _bottomLeftArc != Arc.none() ? (_bottomLeftArc.width() + _bottomLeftArc.height()) / 2 : 0; }

    public int bottomRightRadius() { return _bottomRightArc != Arc.none() ? (_bottomRightArc.width() + _bottomRightArc.height()) / 2 : 0; }

    public Outline widths() { return _borderWidths; }

    public Outline margin() { return _margin; }

    public Outline padding() { return _padding; }

    public Optional<Color> color() { return Optional.ofNullable(_borderColor); }

    /**
     * @return A list of all named gradient styles sorted by their name.
     */
    public List<GradientStyle> gradients() {
        return Collections.unmodifiableList(
                _gradients.namedStyles()
                            .stream()
                            .sorted(Comparator.comparing(NamedStyle::name))
                            .map(NamedStyle::style)
                            .collect(Collectors.toList())
            );
    }

    BorderStyle withGradients( NamedStyles<GradientStyle> gradients ) {
        Objects.requireNonNull(gradients);
        return new BorderStyle(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths, _margin, _padding, _borderColor, gradients);
    }

    public BorderStyle withGradient( String gradientName, Function<GradientStyle, GradientStyle> styler ) {
        Objects.requireNonNull(gradientName);
        Objects.requireNonNull(styler);
        GradientStyle shadow = Optional.ofNullable(_gradients.get(gradientName)).orElse(GradientStyle.none());
        return withGradients(_gradients.withNamedStyle(gradientName, styler.apply(shadow)));
    }

    BorderStyle withWidths( Outline borderWidths ) {
        return new BorderStyle(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, borderWidths, _margin, _padding, _borderColor, _gradients);
    }

    BorderStyle withMargin( Outline margin ) {
        return new BorderStyle(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths, margin, _padding, _borderColor, _gradients);
    }

    BorderStyle withPadding( Outline padding ) {
        return new BorderStyle(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths, _margin, padding, _borderColor, _gradients);
    }

    BorderStyle withArcWidthAt( UI.Corner corner, int borderArcWidth ) {
        if ( corner == UI.Corner.EVERY )
            return this.withArcWidth(borderArcWidth);
        int arcHeight;
        switch ( corner ) {
            case TOP_LEFT:
                arcHeight = _topLeftArc != Arc.none() ? _topLeftArc.height() : 0;
                return new BorderStyle(Arc.of(borderArcWidth, arcHeight), _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths, _margin, _padding, _borderColor, _gradients);
            case TOP_RIGHT:
                arcHeight = _topRightArc != Arc.none() ? _topRightArc.height() : 0;
                return new BorderStyle(_topLeftArc, Arc.of(borderArcWidth, arcHeight), _bottomLeftArc, _bottomRightArc, _borderWidths, _margin, _padding, _borderColor, _gradients);
            case BOTTOM_LEFT:
                arcHeight = _bottomLeftArc != Arc.none() ? _bottomLeftArc.height() : 0;
                return new BorderStyle(_topLeftArc, _topRightArc, Arc.of(borderArcWidth, arcHeight), _bottomRightArc, _borderWidths, _margin, _padding, _borderColor, _gradients);
            case BOTTOM_RIGHT:
                arcHeight = _bottomRightArc != Arc.none() ? _bottomRightArc.height() : 0;
                return new BorderStyle(_topLeftArc, _topRightArc, _bottomLeftArc, Arc.of(borderArcWidth, arcHeight), _borderWidths, _margin, _padding, _borderColor, _gradients);
            default:
                throw new IllegalArgumentException("Unknown corner: " + corner);
        }
    }

    BorderStyle withArcWidth( int borderArcWidth ) {
        return this.withArcWidthAt(UI.Corner.TOP_LEFT,     borderArcWidth)
                   .withArcWidthAt(UI.Corner.TOP_RIGHT,    borderArcWidth)
                   .withArcWidthAt(UI.Corner.BOTTOM_LEFT,  borderArcWidth)
                   .withArcWidthAt(UI.Corner.BOTTOM_RIGHT, borderArcWidth);
    }

    BorderStyle withArcHeightAt( UI.Corner corner, int borderArcHeight ) {
        if ( corner == UI.Corner.EVERY )
            return this.withArcHeight(borderArcHeight);
        int arcWidth;
        switch ( corner ) {
            case TOP_LEFT:
                arcWidth = _topLeftArc != Arc.none() ? _topLeftArc.width() : 0;
                return new BorderStyle(Arc.of(arcWidth, borderArcHeight), _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths, _margin, _padding, _borderColor, _gradients);
            case TOP_RIGHT:
                arcWidth = _topRightArc != Arc.none() ? _topRightArc.width() : 0;
                return new BorderStyle(_topLeftArc, Arc.of(arcWidth, borderArcHeight), _bottomLeftArc, _bottomRightArc, _borderWidths, _margin, _padding, _borderColor, _gradients);
            case BOTTOM_LEFT:
                arcWidth = _bottomLeftArc != Arc.none() ? _bottomLeftArc.width() : 0;
                return new BorderStyle(_topLeftArc, _topRightArc, Arc.of(arcWidth, borderArcHeight), _bottomRightArc, _borderWidths, _margin, _padding, _borderColor, _gradients);
            case BOTTOM_RIGHT:
                arcWidth = _bottomRightArc != Arc.none() ? _bottomRightArc.width() : 0;
                return new BorderStyle(_topLeftArc, _topRightArc, _bottomLeftArc, Arc.of(arcWidth, borderArcHeight), _borderWidths, _margin, _padding, _borderColor, _gradients);
            default:
                throw new IllegalArgumentException("Unknown corner: " + corner);
        }
    }

    BorderStyle withArcHeight( int borderArcHeight ) {
        return this.withArcHeightAt(UI.Corner.TOP_LEFT,     borderArcHeight)
                   .withArcHeightAt(UI.Corner.TOP_RIGHT,    borderArcHeight)
                   .withArcHeightAt(UI.Corner.BOTTOM_LEFT,  borderArcHeight)
                   .withArcHeightAt(UI.Corner.BOTTOM_RIGHT, borderArcHeight);
    }

    BorderStyle withWidthAt( UI.Edge edge, int borderWidth ) {
        if ( edge == UI.Edge.EVERY )
            return this.withWidth(borderWidth);
        switch (edge) {
            case TOP:    return new BorderStyle(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths.withTop(borderWidth), _margin, _padding, _borderColor, _gradients);
            case RIGHT:  return new BorderStyle(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths.withRight(borderWidth), _margin, _padding, _borderColor, _gradients);
            case BOTTOM: return new BorderStyle(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths.withBottom(borderWidth), _margin, _padding, _borderColor, _gradients);
            case LEFT:   return new BorderStyle(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths.withLeft(borderWidth), _margin, _padding, _borderColor, _gradients);
            default:
                throw new IllegalArgumentException("Unknown side: " + edge);
        }
    }

    BorderStyle withWidth( int borderWidth ) {
        return this.withWidthAt(UI.Edge.TOP,    borderWidth)
                   .withWidthAt(UI.Edge.RIGHT,  borderWidth)
                   .withWidthAt(UI.Edge.BOTTOM, borderWidth)
                   .withWidthAt(UI.Edge.LEFT,   borderWidth);
    }

    BorderStyle withColor( Color borderColor ) { return new BorderStyle(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths, _margin, _padding, borderColor, _gradients); }

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
        boolean hasAnyVisibleGradients = _gradients.stylesStream().anyMatch(gradient -> !gradient.equals(GradientStyle.none()) );
        return hasAnyNonZeroArcs || hasAnyNonZeroWidths || hasAVisibleColor || hasAnyVisibleGradients;
    }

    BorderStyle _scale( double scale ) {
        return new BorderStyle(
                    _topLeftArc.scale(scale),
                    _topRightArc.scale(scale),
                    _bottomLeftArc.scale(scale),
                    _bottomRightArc.scale(scale),
                    _borderWidths.scale(scale),
                    _margin.scale(scale),
                    _padding.scale(scale),
                    _borderColor,
                    _gradients
                );
    }

    BorderStyle simplified() {
        if ( this == _NONE )
            return _NONE;

        Arc simplifiedTopLeftArc       = _topLeftArc.simplified();
        Arc simplifiedTopRightArc      = _topRightArc.simplified();
        Arc simplifiedBottomLeftArc    = _bottomLeftArc.simplified();
        Arc simplifiedBottomRightArc   = _bottomRightArc.simplified();
        Outline simplifiedBorderWidths = _borderWidths.simplified();
        Outline simplifiedMargin       = _margin.simplified();
        Outline simplifiedPadding      = _padding.simplified();
        Color simplifiedBorderColor    = _borderColor != null && _borderColor.getAlpha() > 0 ? _borderColor : null;
        NamedStyles<GradientStyle> simplifiedGradients = _gradients.simplified();
        if (
             simplifiedTopLeftArc     == Arc.none() &&
             simplifiedTopRightArc    == Arc.none() &&
             simplifiedBottomLeftArc  == Arc.none() &&
             simplifiedBottomRightArc == Arc.none() &&
             simplifiedBorderWidths   == Outline.none() &&
             simplifiedMargin         == Outline.none() &&
             simplifiedPadding        == Outline.none() &&
             simplifiedBorderColor    == null &&
             simplifiedGradients.equals(NamedStyles.of(NamedStyle.of(StyleUtility.DEFAULT_KEY, GradientStyle.none())))
        )
            return _NONE;
        else if (
            simplifiedTopLeftArc     == _topLeftArc &&
            simplifiedTopRightArc    == _topRightArc &&
            simplifiedBottomLeftArc  == _bottomLeftArc &&
            simplifiedBottomRightArc == _bottomRightArc &&
            simplifiedBorderWidths   == _borderWidths &&
            simplifiedMargin         == _margin &&
            simplifiedPadding        == _padding &&
            simplifiedBorderColor    == _borderColor &&
            simplifiedGradients      == _gradients
        )
            return this;
        else
            return new BorderStyle(
                        simplifiedTopLeftArc,
                        simplifiedTopRightArc,
                        simplifiedBottomLeftArc,
                        simplifiedBottomRightArc,
                        simplifiedBorderWidths,
                        simplifiedMargin,
                        simplifiedPadding,
                        simplifiedBorderColor,
                        simplifiedGradients
                    );
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
        hash = 97 * hash + _gradients.hashCode();
        return hash;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        BorderStyle rhs = (BorderStyle) obj;
        return
            Objects.equals(_topLeftArc,     rhs._topLeftArc)     &&
            Objects.equals(_topRightArc,    rhs._topRightArc)    &&
            Objects.equals(_bottomLeftArc,  rhs._bottomLeftArc)  &&
            Objects.equals(_bottomRightArc, rhs._bottomRightArc) &&
            Objects.equals(_borderColor,    rhs._borderColor)    &&
            Objects.equals(_borderWidths,   rhs._borderWidths)   &&
            Objects.equals(_margin,         rhs._margin)         &&
            Objects.equals(_padding,        rhs._padding)        &&
            Objects.equals(_gradients,      rhs._gradients);
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
                            ? "radius="   + ( _topLeftArc == Arc.none() ? "?" : _topLeftArc.width() )
                            : "arcWidth=" + _topLeftArc.width() + ", arcHeight=" + _topLeftArc.height()
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
            borderWidthsString = "width=" + _borderWidths.top().map(Objects::toString).orElse("?");
        } else {
            borderWidthsString =
                    "topWidth="      + _borderWidths.top().map(Objects::toString).orElse("?")    +
                    ", rightWidth="  + _borderWidths.right().map(Objects::toString).orElse("?")  +
                    ", bottomWidth=" + _borderWidths.bottom().map(Objects::toString).orElse("?") +
                    ", leftWidth="   + _borderWidths.left().map(Objects::toString).orElse("?");
        }

        String shadesString = _gradients.toString(StyleUtility.DEFAULT_KEY, "gradients");

        return this.getClass().getSimpleName() + "[" +
                    arcsString + ", " +
                    borderWidthsString + ", " +
                    "margin=" + _margin + ", " +
                    "padding=" + _padding + ", " +
                    "color=" + StyleUtility.toString(_borderColor) + ", " +
                    shadesString +
                "]";
    }
}
