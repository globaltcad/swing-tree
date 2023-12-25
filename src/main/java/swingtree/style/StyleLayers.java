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
                                                    StyleLayer.empty()
                                                );

    static StyleLayers empty() {
        return _EMPTY;
    }

    private final StyleLayer _background;
    private final StyleLayer _content;
    private final StyleLayer _border;
    private final StyleLayer _foreground;


    StyleLayers(
        StyleLayer background,
        StyleLayer content,
        StyleLayer border,
        StyleLayer foreground
    ) {
        _background = Objects.requireNonNull(background);
        _content    = Objects.requireNonNull(content);
        _border     = Objects.requireNonNull(border);
        _foreground = Objects.requireNonNull(foreground);
    }

    StyleLayer get(UI.Layer layer) {
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
            case BACKGROUND: return new StyleLayers(style, _content, _border, _foreground);
            case CONTENT:    return new StyleLayers(_background, style, _border, _foreground);
            case BORDER:     return new StyleLayers(_background, _content, style, _foreground);
            case FOREGROUND: return new StyleLayers(_background, _content, _border, style);
            default:
                throw new IllegalArgumentException("Unknown layer: " + layer);
        }
    }

    boolean everyNamedStyle( BiPredicate<UI.Layer, StyleLayer> predicate ) {
        return predicate.test(UI.Layer.BACKGROUND, _background)
            && predicate.test(UI.Layer.CONTENT,    _content)
            && predicate.test(UI.Layer.BORDER,     _border)
            && predicate.test(UI.Layer.FOREGROUND, _foreground);
    }

    public StyleLayers onlyRetainingLayer( UI.Layer layer ){
        switch (layer) {
            case BACKGROUND: return new StyleLayers(_background, StyleLayer.empty(), StyleLayer.empty(), StyleLayer.empty());
            case CONTENT:    return new StyleLayers(StyleLayer.empty(), _content, StyleLayer.empty(), StyleLayer.empty());
            case BORDER:     return new StyleLayers(StyleLayer.empty(), StyleLayer.empty(), _border, StyleLayer.empty());
            case FOREGROUND: return new StyleLayers(StyleLayer.empty(), StyleLayer.empty(), StyleLayer.empty(), _foreground);
            default:
                throw new IllegalArgumentException("Unknown layer: " + layer);
        }
    }

    StyleLayers map( Function<StyleLayer, StyleLayer> f ) {
        return new StyleLayers(f.apply(_background), f.apply(_content), f.apply(_border), f.apply(_foreground));
    }

    StyleLayers simplified() {
        if ( this == _EMPTY )
            return this;

        StyleLayer background = _background.simplified();
        StyleLayer content    = _content.simplified();
        StyleLayer border     = _border.simplified();
        StyleLayer foreground = _foreground.simplified();

        if (
             background == _background &&
             content    == _content    &&
             border     == _border     &&
             foreground == _foreground
        )
            return this;

        return new StyleLayers(background, content, border, foreground);
    }

    @Override
    public String toString() {
        return String.format(
            this.getClass().getSimpleName() + "[background=%s, content=%s, border=%s, foreground=%s]",
            _background, _content, _border, _foreground
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(_background, _content, _border, _foreground);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof StyleLayers)) return false;

        StyleLayers other = (StyleLayers) obj;
        return Objects.equals(_background, other._background)
            && Objects.equals(_content, other._content)
            && Objects.equals(_border, other._border)
            && Objects.equals(_foreground, other._foreground);
    }
}
