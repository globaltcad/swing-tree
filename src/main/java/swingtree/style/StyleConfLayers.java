package swingtree.style;

import org.jspecify.annotations.Nullable;
import swingtree.UI;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;

final class StyleConfLayers
{
    private static final StyleConfLayers _EMPTY = new StyleConfLayers(
                                                    StyleConfLayer.empty(),
                                                    StyleConfLayer.empty(),
                                                    StyleConfLayer.empty(),
                                                    StyleConfLayer.empty(),
                                                    null
                                                );

    static StyleConfLayers empty() {
        return _EMPTY;
    }


    private final StyleConfLayer           _background;
    private final StyleConfLayer           _content;
    private final StyleConfLayer           _border;
    private final StyleConfLayer           _foreground;

    private final @Nullable StyleConfLayer _any;


    static StyleConfLayers of(
        StyleConfLayer           background,
        StyleConfLayer           content,
        StyleConfLayer           border,
        StyleConfLayer           foreground,
        @Nullable StyleConfLayer any
    ) {
        StyleConfLayer empty = StyleConfLayer.empty();
        if (
            background == empty &&
            content    == empty &&
            border     == empty &&
            foreground == empty &&
            any        == null
        )
            return _EMPTY;

        return new StyleConfLayers( background, content, border, foreground, any );
    }

    StyleConfLayers(
        StyleConfLayer           background,
        StyleConfLayer           content,
        StyleConfLayer           border,
        StyleConfLayer           foreground,
        @Nullable StyleConfLayer any
    ) {
        _background = Objects.requireNonNull(background);
        _content    = Objects.requireNonNull(content);
        _border     = Objects.requireNonNull(border);
        _foreground = Objects.requireNonNull(foreground);
        _any        = any;
    }

    StyleConfLayer get(UI.Layer layer ) {
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

    StyleConfLayers with(UI.Layer layer, StyleConfLayer style) {
        switch (layer) {
            case BACKGROUND: return of(style,       _content, _border,  _foreground, _any);
            case CONTENT:    return of(_background,  style,    _border, _foreground, _any);
            case BORDER:     return of(_background, _content,  style,   _foreground, _any);
            case FOREGROUND: return of(_background, _content, _border,   style,      _any);
            default:
                throw new IllegalArgumentException("Unknown layer: " + layer);
        }
    }

    boolean everyNamedStyle( BiPredicate<UI.Layer, StyleConfLayer> predicate ) {
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

    public StyleConfLayers onlyRetainingAsUnnamedLayer(UI.Layer layer ){
        switch (layer) {
            case BACKGROUND: return of(StyleConfLayer.empty(), StyleConfLayer.empty(), StyleConfLayer.empty(), StyleConfLayer.empty(), _background);
            case CONTENT:    return of(StyleConfLayer.empty(), StyleConfLayer.empty(), StyleConfLayer.empty(), StyleConfLayer.empty(), _content);
            case BORDER:     return of(StyleConfLayer.empty(), StyleConfLayer.empty(), StyleConfLayer.empty(), StyleConfLayer.empty(), _border);
            case FOREGROUND: return of(StyleConfLayer.empty(), StyleConfLayer.empty(), StyleConfLayer.empty(), StyleConfLayer.empty(), _foreground);
            default:
                throw new IllegalArgumentException("Unknown layer: " + layer);
        }
    }

    StyleConfLayers map(Function<StyleConfLayer, StyleConfLayer> f ) {
        return of(f.apply(_background), f.apply(_content), f.apply(_border), f.apply(_foreground), _any == null ? null : f.apply(_any));
    }

    StyleConfLayers simplified() {
        if ( this == _EMPTY )
            return this;

        StyleConfLayer background = _background.simplified();
        StyleConfLayer content    = _content.simplified();
        StyleConfLayer border     = _border.simplified();
        StyleConfLayer foreground = _foreground.simplified();
        StyleConfLayer any        = ( _any == null ? null : _any.simplified() );

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
        if (!(obj instanceof StyleConfLayers)) return false;

        StyleConfLayers other = (StyleConfLayers) obj;
        return Objects.equals(_background, other._background)
            && Objects.equals(_content,    other._content)
            && Objects.equals(_border,     other._border)
            && Objects.equals(_foreground, other._foreground)
            && Objects.equals(_any,        other._any);
    }
}
