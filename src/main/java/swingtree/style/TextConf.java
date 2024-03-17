package swingtree.style;

import swingtree.UI;

import java.awt.Font;
import java.util.Objects;
import java.util.function.Function;

/**
 *  An immutable configuration type which holds custom
 *  text as well as placement and font properties used for
 *  rendering text onto a Swing component.
 */
public final class TextConf implements Simplifiable<TextConf>
{
    public static UI.Layer DEFAULT_LAYER = UI.Layer.CONTENT;
    private static final TextConf _NONE = new TextConf(
                                                "",
                                                FontConf.none(),
                                                UI.ComponentArea.INTERIOR,
                                                UI.ComponentBoundary.INTERIOR_TO_CONTENT,
                                                UI.Placement.CENTER,
                                                Offset.none()
                                            );

    static final TextConf none() {
        return _NONE;
    }

    private final String               _text;
    private final FontConf             _fontConf;
    private final UI.ComponentArea     _clipArea;
    private final UI.ComponentBoundary _placementBoundary;
    private final UI.Placement         _placement;
    private final Offset               _offset;

    private TextConf(
        String               text,
        FontConf             fontConf,
        UI.ComponentArea     clipArea,
        UI.ComponentBoundary placementBoundary,
        UI.Placement         placement,
        Offset               offset
    )
    {
        _text               = Objects.requireNonNull(text);
        _fontConf           = Objects.requireNonNull(fontConf);
        _clipArea           = Objects.requireNonNull(clipArea);
        _placementBoundary  = Objects.requireNonNull(placementBoundary);
        _placement          = Objects.requireNonNull(placement);
        _offset             = Objects.requireNonNull(offset);
    }

    private static TextConf of(
        String               text,
        FontConf             fontConf,
        UI.ComponentArea     clipArea,
        UI.ComponentBoundary placementBoundary,
        UI.Placement         placement,
        Offset               offset
    )
    {
        if (
            text.isEmpty() &&
            fontConf.equals(_NONE._fontConf) &&
            clipArea.equals(_NONE._clipArea) &&
            placementBoundary.equals(_NONE._placementBoundary) &&
            placement.equals(_NONE._placement) &&
            offset.equals(_NONE._offset)
        ) {
            return _NONE;
        }
        return new TextConf(text, fontConf, clipArea, placementBoundary, placement, offset);
    }

    String text() {
        return _text;
    }

    FontConf fontConf() {
        return _fontConf;
    }

    UI.ComponentArea clipArea() {
        return _clipArea;
    }

    UI.ComponentBoundary placementBoundary() {
        return _placementBoundary;
    }

    UI.Placement placement() {
        return _placement;
    }

    Offset offset() {
        return _offset;
    }

    public TextConf text(String text) {
        return of(text, _fontConf, _clipArea, _placementBoundary, _placement, _offset);
    }

    private TextConf _fontConf(FontConf fontConf) {
        return of(_text, fontConf, _clipArea, _placementBoundary, _placement, _offset);
    }

    public TextConf font( Function<FontConf, FontConf> fontConfFunction ) {
        return _fontConf(fontConfFunction.apply(_fontConf));
    }

    public TextConf font( Font font ) {
        return _fontConf(_fontConf.withPropertiesFromFont(font));
    }

    public TextConf clipTo( UI.ComponentArea clipArea ) {
        return of(_text, _fontConf, clipArea, _placementBoundary, _placement, _offset);
    }

    public TextConf placementBoundary(UI.ComponentBoundary placementBoundary) {
        return of(_text, _fontConf, _clipArea, placementBoundary, _placement, _offset);
    }

    public TextConf placement(UI.Placement placement) {
        return of(_text, _fontConf, _clipArea, _placementBoundary, placement, _offset);
    }

    TextConf offset(Offset offset) {
        return of(_text, _fontConf, _clipArea, _placementBoundary, _placement, offset);
    }

    public TextConf offset(int x, int y) {
        return offset(Offset.of(x, y));
    }

    @Override
    public TextConf simplified() {
        if ( _text.isEmpty() )
            return _NONE;
        return this;
    }

    TextConf _scale(double scale) {
        return of(
            _text,
            _fontConf._scale(scale),
            _clipArea,
            _placementBoundary,
            _placement,
            _offset.scale(scale)
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TextConf)) {
            return false;
        }
        TextConf other = (TextConf) obj;
        return
            _text.equals(other._text) &&
            _fontConf.equals(other._fontConf) &&
            _clipArea.equals(other._clipArea) &&
            _placementBoundary.equals(other._placementBoundary) &&
            _placement.equals(other._placement) &&
            _offset.equals(other._offset);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_text, _fontConf, _clipArea, _placementBoundary, _placement, _offset);
    }

    @Override
    public String toString() {
        if ( this == _NONE )
            return this.getClass().getSimpleName() + "[NONE]";
        return this.getClass().getSimpleName() + "[" +
            "text=" + _text + ", " +
            "fontConf=" + _fontConf + ", " +
            "clipArea=" + _clipArea + ", " +
            "placementBoundary=" + _placementBoundary + ", " +
            "placement=" + _placement + ", " +
            "offset=" + _offset +
        "]";
    }

}
