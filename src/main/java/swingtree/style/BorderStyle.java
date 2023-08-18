package swingtree.style;

import swingtree.UI;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *  An immutable, wither-like cloner method based settings container for border styles that is part of
 *  a {@link Style} configuration object.
 */
public final class BorderStyle
{
    private static final BorderStyle _NONE = new BorderStyle(
                                                null, null, null, null,
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
        Arc topLeftArc,
        Arc topRightArc,
        Arc bottomLeftArc,
        Arc bottomRightArc,
        Outline borderWidths,
        Outline margin,
        Outline padding,
        Color borderColor,
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

    public Optional<Arc> topLeftArc() { return Optional.ofNullable(_topLeftArc); }

    public Optional<Arc> topRightArc() { return Optional.ofNullable(_topRightArc); }

    public Optional<Arc> bottomLeftArc() { return Optional.ofNullable(_bottomLeftArc); }

    public Optional<Arc> bottomRightArc() { return Optional.ofNullable(_bottomRightArc); }

    public boolean hasAnyNonZeroArcs() {
        return _topLeftArc     != null && _topLeftArc.width()     > 0 && _topLeftArc.height()     > 0 ||
               _topRightArc    != null && _topRightArc.width()    > 0 && _topRightArc.height()    > 0 ||
               _bottomLeftArc  != null && _bottomLeftArc.width()  > 0 && _bottomLeftArc.height()  > 0 ||
               _bottomRightArc != null && _bottomRightArc.width() > 0 && _bottomRightArc.height() > 0;
    }

    public int topLeftRadius() { return _topLeftArc != null ? (_topLeftArc.width() + _topLeftArc.height()) / 2 : 0; }

    public int topRightRadius() { return _topRightArc != null ? (_topRightArc.width() + _topRightArc.height()) / 2 : 0; }

    public int bottomLeftRadius() { return _bottomLeftArc != null ? (_bottomLeftArc.width() + _bottomLeftArc.height()) / 2 : 0; }

    public int bottomRightRadius() { return _bottomRightArc != null ? (_bottomRightArc.width() + _bottomRightArc.height()) / 2 : 0; }

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

    BorderStyle gradient( NamedStyles<GradientStyle> shades ) {
        Objects.requireNonNull(shades);
        return new BorderStyle(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths, _margin, _padding, _borderColor, shades);
    }

    public BorderStyle gradient( String shadeName, Function<GradientStyle, GradientStyle> styler ) {
        Objects.requireNonNull(shadeName);
        Objects.requireNonNull(styler);
        GradientStyle shadow = Optional.ofNullable(_gradients.get(shadeName)).orElse(GradientStyle.none());
        return gradient(_gradients.withNamedStyle(shadeName, styler.apply(shadow)));
    }

    public GradientStyle gradient( String shadeName ) {
        Objects.requireNonNull(shadeName);
        return Optional.ofNullable(_gradients.get(shadeName)).orElse(GradientStyle.none());
    }

    public GradientStyle gradient() {
        return gradient(StyleUtility.DEFAULT_KEY);
    }

    BorderStyle widths( Outline borderWidths ) {
        return new BorderStyle(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, borderWidths, _margin, _padding, _borderColor, _gradients);
    }

    BorderStyle margin( Outline margin ) {
        return new BorderStyle(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths, margin, _padding, _borderColor, _gradients);
    }

    BorderStyle padding( Outline padding ) {
        return new BorderStyle(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths, _margin, padding, _borderColor, _gradients);
    }

    BorderStyle arcWidthAt(UI.Corner corner, int borderArcWidth ) {
        if ( corner == UI.Corner.EVERY )
            return this.arcWidth(borderArcWidth);
        int arcHeight;
        switch ( corner ) {
            case TOP_LEFT:
                arcHeight = _topLeftArc != null ? _topLeftArc.height() : 0;
                return new BorderStyle(Arc.of(borderArcWidth, arcHeight), _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths, _margin, _padding, _borderColor, _gradients);
            case TOP_RIGHT:
                arcHeight = _topRightArc != null ? _topRightArc.height() : 0;
                return new BorderStyle(_topLeftArc, Arc.of(borderArcWidth, arcHeight), _bottomLeftArc, _bottomRightArc, _borderWidths, _margin, _padding, _borderColor, _gradients);
            case BOTTOM_LEFT:
                arcHeight = _bottomLeftArc != null ? _bottomLeftArc.height() : 0;
                return new BorderStyle(_topLeftArc, _topRightArc, Arc.of(borderArcWidth, arcHeight), _bottomRightArc, _borderWidths, _margin, _padding, _borderColor, _gradients);
            case BOTTOM_RIGHT:
                arcHeight = _bottomRightArc != null ? _bottomRightArc.height() : 0;
                return new BorderStyle(_topLeftArc, _topRightArc, _bottomLeftArc, Arc.of(borderArcWidth, arcHeight), _borderWidths, _margin, _padding, _borderColor, _gradients);
            default:
                throw new IllegalArgumentException("Unknown corner: " + corner);
        }
    }

    BorderStyle arcWidth( int borderArcWidth ) {
        return this.arcWidthAt(UI.Corner.TOP_LEFT,     borderArcWidth)
                   .arcWidthAt(UI.Corner.TOP_RIGHT,    borderArcWidth)
                   .arcWidthAt(UI.Corner.BOTTOM_LEFT,  borderArcWidth)
                   .arcWidthAt(UI.Corner.BOTTOM_RIGHT, borderArcWidth);
    }

    BorderStyle arcHeightAt(UI.Corner corner, int borderArcHeight ) {
        if ( corner == UI.Corner.EVERY )
            return this.arcHeight(borderArcHeight);
        int arcWidth;
        switch ( corner ) {
            case TOP_LEFT:
                arcWidth = _topLeftArc != null ? _topLeftArc.width() : 0;
                return new BorderStyle(Arc.of(arcWidth, borderArcHeight), _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths, _margin, _padding, _borderColor, _gradients);
            case TOP_RIGHT:
                arcWidth = _topRightArc != null ? _topRightArc.width() : 0;
                return new BorderStyle(_topLeftArc, Arc.of(arcWidth, borderArcHeight), _bottomLeftArc, _bottomRightArc, _borderWidths, _margin, _padding, _borderColor, _gradients);
            case BOTTOM_LEFT:
                arcWidth = _bottomLeftArc != null ? _bottomLeftArc.width() : 0;
                return new BorderStyle(_topLeftArc, _topRightArc, Arc.of(arcWidth, borderArcHeight), _bottomRightArc, _borderWidths, _margin, _padding, _borderColor, _gradients);
            case BOTTOM_RIGHT:
                arcWidth = _bottomRightArc != null ? _bottomRightArc.width() : 0;
                return new BorderStyle(_topLeftArc, _topRightArc, _bottomLeftArc, Arc.of(arcWidth, borderArcHeight), _borderWidths, _margin, _padding, _borderColor, _gradients);
            default:
                throw new IllegalArgumentException("Unknown corner: " + corner);
        }
    }

    BorderStyle arcHeight( int borderArcHeight ) {
        return this.arcHeightAt(UI.Corner.TOP_LEFT,     borderArcHeight)
                   .arcHeightAt(UI.Corner.TOP_RIGHT,    borderArcHeight)
                   .arcHeightAt(UI.Corner.BOTTOM_LEFT,  borderArcHeight)
                   .arcHeightAt(UI.Corner.BOTTOM_RIGHT, borderArcHeight);
    }

    BorderStyle widthAt(UI.Edge edge, int borderWidth ) {
        if ( edge == UI.Edge.EVERY )
            return this.width(borderWidth);
        switch (edge) {
            case TOP:    return new BorderStyle(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths.top(borderWidth), _margin, _padding, _borderColor, _gradients);
            case RIGHT:  return new BorderStyle(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths.right(borderWidth), _margin, _padding, _borderColor, _gradients);
            case BOTTOM: return new BorderStyle(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths.bottom(borderWidth), _margin, _padding, _borderColor, _gradients);
            case LEFT:   return new BorderStyle(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths.left(borderWidth), _margin, _padding, _borderColor, _gradients);
            default:
                throw new IllegalArgumentException("Unknown side: " + edge);
        }
    }

    BorderStyle width( int borderWidth ) {
        return this.widthAt(UI.Edge.TOP,    borderWidth)
                   .widthAt(UI.Edge.RIGHT,  borderWidth)
                   .widthAt(UI.Edge.BOTTOM, borderWidth)
                   .widthAt(UI.Edge.LEFT,   borderWidth);
    }

    BorderStyle color( Color borderColor ) { return new BorderStyle(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths, _margin, _padding, borderColor, _gradients); }

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
                    _topLeftArc     == null ? null : _topLeftArc.scale(scale),
                    _topRightArc    == null ? null : _topRightArc.scale(scale),
                    _bottomLeftArc  == null ? null : _bottomLeftArc.scale(scale),
                    _bottomRightArc == null ? null : _bottomRightArc.scale(scale),
                    _borderWidths.scale(scale),
                    _margin.scale(scale),
                    _padding.scale(scale),
                    _borderColor,
                _gradients
            );
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + ( _topLeftArc     != null ? _topLeftArc.hashCode()     : 0 );
        hash = 97 * hash + ( _topRightArc    != null ? _topRightArc.hashCode()    : 0 );
        hash = 97 * hash + ( _bottomLeftArc  != null ? _bottomLeftArc.hashCode()  : 0 );
        hash = 97 * hash + ( _bottomRightArc != null ? _bottomRightArc.hashCode() : 0 );
        hash = 97 * hash + ( _borderColor    != null ? _borderColor.hashCode()    : 0 );
        hash = 97 * hash + _borderWidths.hashCode();
        hash = 97 * hash + _margin.hashCode();
        hash = 97 * hash + _padding.hashCode();
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
        String arcsString;
        if ( allCornersShareTheSameArc() ) {
            boolean arcWidthEqualsHeight = _topLeftArc == null || _topLeftArc.width() == _topLeftArc.height();
            arcsString = (
                        arcWidthEqualsHeight
                            ? "radius="   + ( _topLeftArc == null ? "?" : _topLeftArc.width() )
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

        return "BorderStyle[" +
                    arcsString + ", " +
                    borderWidthsString + ", " +
                    "margin=" + _margin + ", " +
                    "padding=" + _padding + ", " +
                    "color=" + StyleUtility.toString(_borderColor) + ", " +
                    shadesString +
                "]";
    }
}
