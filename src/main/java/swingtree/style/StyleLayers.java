package swingtree.style;

import swingtree.UI;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;

final class StyleLayers
{
    private static final StyleLayers _EMPTY = new StyleLayers(
                                                    StyleLayer.empty(),
                                                    StyleLayer.empty(),
                                                    StyleLayer.empty(),
                                                    StyleLayer.empty(),
                                                    null
                                                );

    static StyleLayers empty() {
        return _EMPTY;
    }

    private final StyleLayer _background;
    private final StyleLayer _content;
    private final StyleLayer _border;
    private final StyleLayer _foreground;

    private final StyleLayer _any;


    static StyleLayers of(
        StyleLayer background,
        StyleLayer content,
        StyleLayer border,
        StyleLayer foreground,
        StyleLayer any
    ) {
        StyleLayer empty = StyleLayer.empty();
        if (
            background == empty &&
            content    == empty &&
            border     == empty &&
            foreground == empty &&
            any        == null
        )
            return _EMPTY;

        return new StyleLayers( background, content, border, foreground, any );
    }

    StyleLayers(
        StyleLayer background,
        StyleLayer content,
        StyleLayer border,
        StyleLayer foreground,
        StyleLayer any
    ) {
        _background = Objects.requireNonNull(background);
        _content    = Objects.requireNonNull(content);
        _border     = Objects.requireNonNull(border);
        _foreground = Objects.requireNonNull(foreground);
        _any        = any;
    }

    StyleLayer get( UI.Layer layer ) {
        if ( _any != null )
            return _any;

        switch (layer) {
            case BACKGROUND: return _background;
            case CONTENT:    return _content;
            case BORDER:     return _border;
            case FOREGROUND: return _foreground;
            default:
                throw new IllegalArgumentException("Unknown layer: " + layer);
        }
    }

    StyleLayers with(UI.Layer layer, StyleLayer style) {
        switch (layer) {
            case BACKGROUND: return of(style,       _content, _border,  _foreground, _any);
            case CONTENT:    return of(_background,  style,    _border, _foreground, _any);
            case BORDER:     return of(_background, _content,  style,   _foreground, _any);
            case FOREGROUND: return of(_background, _content, _border,   style,      _any);
            default:
                throw new IllegalArgumentException("Unknown layer: " + layer);
        }
    }

    boolean everyNamedStyle( BiPredicate<UI.Layer, StyleLayer> predicate ) {
        if ( _any != null )
            return  predicate.test(UI.Layer.BACKGROUND,    _any)
                    && predicate.test(UI.Layer.CONTENT,    _any)
                    && predicate.test(UI.Layer.BORDER,     _any)
                    && predicate.test(UI.Layer.FOREGROUND, _any);

        return predicate.test(UI.Layer.BACKGROUND, _background)
            && predicate.test(UI.Layer.CONTENT,    _content)
            && predicate.test(UI.Layer.BORDER,     _border)
            && predicate.test(UI.Layer.FOREGROUND, _foreground);
    }

    public StyleLayers onlyRetainingAsUnnamedLayer( UI.Layer layer ){
        switch (layer) {
            case BACKGROUND: return of(StyleLayer.empty(), StyleLayer.empty(), StyleLayer.empty(), StyleLayer.empty(), _background);
            case CONTENT:    return of(StyleLayer.empty(), StyleLayer.empty(), StyleLayer.empty(), StyleLayer.empty(), _content);
            case BORDER:     return of(StyleLayer.empty(), StyleLayer.empty(), StyleLayer.empty(), StyleLayer.empty(), _border);
            case FOREGROUND: return of(StyleLayer.empty(), StyleLayer.empty(), StyleLayer.empty(), StyleLayer.empty(), _foreground);
            default:
                throw new IllegalArgumentException("Unknown layer: " + layer);
        }
    }

    StyleLayers map( Function<StyleLayer, StyleLayer> f ) {
        return of(f.apply(_background), f.apply(_content), f.apply(_border), f.apply(_foreground), _any == null ? null : f.apply(_any));
    }

    StyleLayers simplified() {
        if ( this == _EMPTY )
            return this;

        StyleLayer background = _background.simplified();
        StyleLayer content    = _content.simplified();
        StyleLayer border     = _border.simplified();
        StyleLayer foreground = _foreground.simplified();
        StyleLayer any        = ( _any == null ? null : _any.simplified() );

        if (
             background == _background &&
             content    == _content    &&
             border     == _border     &&
             foreground == _foreground &&
             any        == _any
        )
            return this;

        return of(background, content, border, foreground, any);
    }

    @Override
    public String toString() {
        if ( _any != null )
            return String.format(
                this.getClass().getSimpleName() + "[any=%s]",
                _any
            );
        return String.format(
            this.getClass().getSimpleName() + "[background=%s, content=%s, border=%s, foreground=%s]",
            _background, _content, _border, _foreground
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(_background, _content, _border, _foreground, _any);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof StyleLayers)) return false;

        StyleLayers other = (StyleLayers) obj;
        return Objects.equals(_background, other._background)
            && Objects.equals(_content,    other._content)
            && Objects.equals(_border,     other._border)
            && Objects.equals(_foreground, other._foreground)
            && Objects.equals(_any,        other._any);
    }
}
