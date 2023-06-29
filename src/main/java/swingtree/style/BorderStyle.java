package swingtree.style;

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
                                                null,
                                                Collections.singletonMap(StyleUtility.DEFAULT_KEY, ShadeStyle.none())
                                            );

    public static BorderStyle none() { return _NONE; }

    private final Arc   _topLeftArc;
    private final Arc   _topRightArc;
    private final Arc   _bottomLeftArc;
    private final Arc   _bottomRightArc;

    private final Outline _borderWidths;
    private final Color _borderColor;
    private final Map<String, ShadeStyle> _shades = new TreeMap<>();


    private BorderStyle(
        Arc topLeftArc,
        Arc topRightArc,
        Arc bottomLeftArc,
        Arc bottomRightArc,
        Outline borderWidths,
        Color borderColor,
        Map<String, ShadeStyle> shades
    ) {
        _topLeftArc      = topLeftArc;
        _topRightArc     = topRightArc;
        _bottomLeftArc   = bottomLeftArc;
        _bottomRightArc  = bottomRightArc;
        _borderWidths    = Objects.requireNonNull(borderWidths);
        _borderColor     = borderColor;
        _shades.putAll(shades);
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

    public Optional<Color> color() { return Optional.ofNullable(_borderColor); }

    public List<ShadeStyle> shades() {
        return Collections.unmodifiableList(
                _shades.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(Map.Entry::getValue)
                        .collect(Collectors.toList())
            );
    }

    BorderStyle shade( Map<String, ShadeStyle> shades ) {
        Objects.requireNonNull(shades);
        return new BorderStyle(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths, _borderColor, shades);
    }

    public BorderStyle shade( String shadeName, Function<ShadeStyle, ShadeStyle> styler ) {
        Objects.requireNonNull(shadeName);
        Objects.requireNonNull(styler);
        ShadeStyle shadow = Optional.ofNullable(_shades.get(shadeName)).orElse(ShadeStyle.none());
        // We clone the shadow map:
        Map<String, ShadeStyle> newShadows = new HashMap<>(_shades);
        newShadows.put(shadeName, styler.apply(shadow));
        return shade(newShadows);
    }

    public ShadeStyle shade( String shadeName ) {
        Objects.requireNonNull(shadeName);
        return Optional.ofNullable(_shades.get(shadeName)).orElse(ShadeStyle.none());
    }

    public ShadeStyle shade() {
        return shade(StyleUtility.DEFAULT_KEY);
    }

    BorderStyle widths( Outline borderWidths ) {
        return new BorderStyle(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, borderWidths, _borderColor, _shades);
    }

    BorderStyle arcWidthAt( Corner corner, int borderArcWidth ) {
        if ( corner == Corner.EVERY )
            return this.arcWidth(borderArcWidth);
        int arcHeight;
        switch ( corner ) {
            case TOP_LEFT:
                arcHeight = _topLeftArc != null ? _topLeftArc.height() : 0;
                return new BorderStyle(new Arc(borderArcWidth, arcHeight), _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths, _borderColor, _shades);
            case TOP_RIGHT:
                arcHeight = _topRightArc != null ? _topRightArc.height() : 0;
                return new BorderStyle(_topLeftArc, new Arc(borderArcWidth, arcHeight), _bottomLeftArc, _bottomRightArc, _borderWidths, _borderColor, _shades);
            case BOTTOM_LEFT:
                arcHeight = _bottomLeftArc != null ? _bottomLeftArc.height() : 0;
                return new BorderStyle(_topLeftArc, _topRightArc, new Arc(borderArcWidth, arcHeight), _bottomRightArc, _borderWidths, _borderColor, _shades);
            case BOTTOM_RIGHT:
                arcHeight = _bottomRightArc != null ? _bottomRightArc.height() : 0;
                return new BorderStyle(_topLeftArc, _topRightArc, _bottomLeftArc, new Arc(borderArcWidth, arcHeight), _borderWidths, _borderColor, _shades);
            default:
                throw new IllegalArgumentException("Unknown corner: " + corner);
        }
    }

    BorderStyle arcWidth( int borderArcWidth ) {
        return this.arcWidthAt(Corner.TOP_LEFT,     borderArcWidth)
                   .arcWidthAt(Corner.TOP_RIGHT,    borderArcWidth)
                   .arcWidthAt(Corner.BOTTOM_LEFT,  borderArcWidth)
                   .arcWidthAt(Corner.BOTTOM_RIGHT, borderArcWidth);
    }

    BorderStyle arcHeightAt( Corner corner, int borderArcHeight ) {
        if ( corner == Corner.EVERY )
            return this.arcHeight(borderArcHeight);
        int arcWidth;
        switch ( corner ) {
            case TOP_LEFT:
                arcWidth = _topLeftArc != null ? _topLeftArc.width() : 0;
                return new BorderStyle(new Arc(arcWidth, borderArcHeight), _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths, _borderColor, _shades);
            case TOP_RIGHT:
                arcWidth = _topRightArc != null ? _topRightArc.width() : 0;
                return new BorderStyle(_topLeftArc, new Arc(arcWidth, borderArcHeight), _bottomLeftArc, _bottomRightArc, _borderWidths, _borderColor, _shades);
            case BOTTOM_LEFT:
                arcWidth = _bottomLeftArc != null ? _bottomLeftArc.width() : 0;
                return new BorderStyle(_topLeftArc, _topRightArc, new Arc(arcWidth, borderArcHeight), _bottomRightArc, _borderWidths, _borderColor, _shades);
            case BOTTOM_RIGHT:
                arcWidth = _bottomRightArc != null ? _bottomRightArc.width() : 0;
                return new BorderStyle(_topLeftArc, _topRightArc, _bottomLeftArc, new Arc(arcWidth, borderArcHeight), _borderWidths, _borderColor, _shades);
            default:
                throw new IllegalArgumentException("Unknown corner: " + corner);
        }
    }

    BorderStyle arcHeight( int borderArcHeight ) {
        return this.arcHeightAt(Corner.TOP_LEFT,     borderArcHeight)
                   .arcHeightAt(Corner.TOP_RIGHT,    borderArcHeight)
                   .arcHeightAt(Corner.BOTTOM_LEFT,  borderArcHeight)
                   .arcHeightAt(Corner.BOTTOM_RIGHT, borderArcHeight);
    }

    BorderStyle widthAt( Edge edge, int borderWidth ) {
        if ( edge == Edge.EVERY )
            return this.width(borderWidth);
        switch (edge) {
            case TOP:    return new BorderStyle(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths.top(borderWidth), _borderColor, _shades);
            case RIGHT:  return new BorderStyle(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths.right(borderWidth), _borderColor, _shades);
            case BOTTOM: return new BorderStyle(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths.bottom(borderWidth), _borderColor, _shades);
            case LEFT:   return new BorderStyle(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths.left(borderWidth), _borderColor, _shades);
            default:
                throw new IllegalArgumentException("Unknown side: " + edge);
        }
    }

    BorderStyle width( int borderWidth ) {
        return this.widthAt(Edge.TOP,    borderWidth)
                   .widthAt(Edge.RIGHT,  borderWidth)
                   .widthAt(Edge.BOTTOM, borderWidth)
                   .widthAt(Edge.LEFT,   borderWidth);
    }

    BorderStyle color( Color borderColor ) { return new BorderStyle(_topLeftArc, _topRightArc, _bottomLeftArc, _bottomRightArc, _borderWidths, borderColor, _shades); }

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

    BorderStyle _scale( double scale ) {
        return new BorderStyle(
                    _topLeftArc     == null ? null : _topLeftArc.scale(scale),
                    _topRightArc    == null ? null : _topRightArc.scale(scale),
                    _bottomLeftArc  == null ? null : _bottomLeftArc.scale(scale),
                    _bottomRightArc == null ? null : _bottomRightArc.scale(scale),
                    _borderWidths.scale(scale),
                    _borderColor,
                    _shades
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
        hash = 97 * hash + StyleUtility.mapHash(_shades);
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
            StyleUtility.mapEquals(_shades, rhs._shades);
    }

    @Override
    public String toString()
    {
        String arcsString;
        if ( allCornersShareTheSameArc() ) {
            boolean arcWidthEqualsHeight = _topLeftArc == null || _topLeftArc.width() == _topLeftArc.height();
            arcsString = (
                        arcWidthEqualsHeight
                            ? "radius="   + _topLeftArc.width()
                            : "arcWidth=" + _topLeftArc.width() + ", arcHeight=" + _topLeftArc.height()
                    );
        } else {
            arcsString =
                    "topLeftArc="    + StyleUtility.toString(_topLeftArc)     +
                    ", topRightArc="    + StyleUtility.toString(_topRightArc)    +
                    ", bottomLeftArc="  + StyleUtility.toString(_bottomLeftArc)  +
                    ", bottomRightArc=" + StyleUtility.toString(_bottomRightArc);
        }

        String borderWidthsString;
        if ( allSidesShareTheSameWidth() ) {
            borderWidthsString = "width=" + _borderWidths.top().orElse(null);
        } else {
            borderWidthsString =
                    "topWidth="    + _borderWidths.top().orElse(null)    +
                    ", rightWidth="  + _borderWidths.right().orElse(null)  +
                    ", bottomWidth=" + _borderWidths.bottom().orElse(null) +
                    ", leftWidth="   + _borderWidths.left().orElse(null);
        }

        String shadesString;
        if ( _shades.size() == 1 )
            shadesString = _shades.get(StyleUtility.DEFAULT_KEY).toString();
        else
            shadesString = _shades.entrySet()
                                    .stream()
                                    .map(e -> e.getKey() + ": " + e.getValue())
                                    .collect(Collectors.joining(", ", "shades=[", "]"));

        return "BorderStyle[" +
                    arcsString + ", " +
                    borderWidthsString + ", " +
                    "color=" + StyleUtility.toString(_borderColor) + ", " +
                    shadesString +
                "]";
    }
}
