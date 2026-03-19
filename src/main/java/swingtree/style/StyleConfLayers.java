package swingtree.style;

import com.google.errorprone.annotations.Immutable;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import sprouts.Pair;
import swingtree.SwingTree;
import swingtree.UI;
import swingtree.api.Configurator;
import swingtree.layout.Bounds;
import swingtree.layout.Size;

import javax.swing.JComponent;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.BiPredicate;

@Immutable
final class StyleConfLayers
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(StyleConfLayers.class);

    private static final StyleConfLayers _EMPTY = new StyleConfLayers(
                                                    FilterConf.none(),
                                                    StyleConfLayer.empty(),
                                                    StyleConfLayer.empty(),
                                                    StyleConfLayer.empty(),
                                                    StyleConfLayer.empty(),
                                                    null
                                                );

    static StyleConfLayers empty() {
        return _EMPTY;
    }

    private final FilterConf               _filter;
    private final StyleConfLayer           _background;
    private final StyleConfLayer           _content;
    private final StyleConfLayer           _border;
    private final StyleConfLayer           _foreground;

    private final @Nullable StyleConfLayer _any;


    static StyleConfLayers of(
        FilterConf               filter,
        StyleConfLayer           background,
        StyleConfLayer           content,
        StyleConfLayer           border,
        StyleConfLayer           foreground,
        @Nullable StyleConfLayer any
    ) {
        StyleConfLayer empty = StyleConfLayer.empty();
        if (
            filter     .equals( FilterConf.none() ) &&
            background .equals( empty ) &&
            content    .equals( empty ) &&
            border     .equals( empty ) &&
            foreground .equals( empty ) &&
            any == null
        )
            return _EMPTY;

        return new StyleConfLayers( filter, background, content, border, foreground, any );
    }

    StyleConfLayers(
        FilterConf               filter,
        StyleConfLayer           background,
        StyleConfLayer           content,
        StyleConfLayer           border,
        StyleConfLayer           foreground,
        @Nullable StyleConfLayer any
    ) {
        _filter     = Objects.requireNonNull(filter);
        _background = Objects.requireNonNull(background);
        _content    = Objects.requireNonNull(content);
        _border     = Objects.requireNonNull(border);
        _foreground = Objects.requireNonNull(foreground);
        _any        = any;
    }

    FilterConf filter() { return _filter; }

    StyleConfLayer get( UI.Layer layer ) {
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

    StyleConfLayers filter( Configurator<FilterConf> f ) {
        FilterConf filter = _filter;
        try {
            filter = f.configure(_filter);
        } catch (Exception e) {
            log.error(SwingTree.get().logMarker(), "Error configuring filter settings for component background.", e);
        }
        return of(filter, _background, _content, _border, _foreground, _any);
    }

    StyleConfLayers with(UI.Layer layer, StyleConfLayer style) {
        switch (layer) {
            case BACKGROUND: return of(_filter, style,       _content, _border,  _foreground, _any);
            case CONTENT:    return of(_filter, _background,  style,    _border, _foreground, _any);
            case BORDER:     return of(_filter, _background, _content,  style,   _foreground, _any);
            case FOREGROUND: return of(_filter, _background, _content, _border,   style,      _any);
            default:
                throw new IllegalArgumentException("Unknown layer: " + layer);
        }
    }

    /**
     *  Tests if the predicate returns true for any of the {@link UI.Layer}s and their
     *  corresponding {@link StyleConfLayer}s in this {@link StyleConfLayers} object.
     *
     * @param predicate A predicate that takes a layer and a style configuration and returns true or false.
     * @return True if the predicate returns true for any of the layers.
     */
    boolean any( BiPredicate<UI.Layer, StyleConfLayer> predicate ) {
        if ( _any != null )
            return  predicate.test(UI.Layer.BACKGROUND,    _any)
                    || predicate.test(UI.Layer.CONTENT,    _any)
                    || predicate.test(UI.Layer.BORDER,     _any)
                    || predicate.test(UI.Layer.FOREGROUND, _any);

        return predicate.test(UI.Layer.BACKGROUND, _background)
            || predicate.test(UI.Layer.CONTENT,    _content)
            || predicate.test(UI.Layer.BORDER,     _border)
            || predicate.test(UI.Layer.FOREGROUND, _foreground);
    }

    StyleConfLayers map( Configurator<StyleConfLayer> f ) {
        try {
            return of(_filter, f.configure(_background), f.configure(_content), f.configure(_border), f.configure(_foreground), _any == null ? null : f.configure(_any));
        } catch (Exception e) {
            log.error(SwingTree.get().logMarker(), "Error configuring style settings for component background.", e);
            return this;
        }
    }

    StyleConfLayers _scale( double factor ) {
        if ( factor == 1 ) {
            return this;
        }
        if ( this.equals(_EMPTY) ) {
            return this;
        }
        return of(
            _filter    ._scale(factor),
            _background._scale(factor),
            _content   ._scale(factor),
            _border    ._scale(factor),
            _foreground._scale(factor),
            _any == null ? null : _any._scale(factor)
        );
    }

    @SuppressWarnings("ReferenceEquality")
    StyleConfLayers simplified() {
        if ( this == _EMPTY )
            return this;

        FilterConf     filter     = _filter.simplified();
        StyleConfLayer background = _background.simplified();
        StyleConfLayer content    = _content.simplified();
        StyleConfLayer border     = _border.simplified();
        StyleConfLayer foreground = _foreground.simplified();
        StyleConfLayer any        = ( _any == null ? null : _any.simplified() );

        if (
             filter     == _filter     &&
             background == _background &&
             content    == _content    &&
             border     == _border     &&
             foreground == _foreground &&
             any        == _any
        )
            return this;

        return of(filter, background, content, border, foreground, any);
    }

    OptionalDouble computePreferredHeightFromTextConfigs( JComponent owner, BoxModelConf predictedBoxModel ) {
        // We look for text configs with non-empty content and compute the preferred size from those:
        Double maxHeight = null;
        for ( UI.Layer layer : UI.Layer.values() ) {
            StyleConfLayer styleConfLayer = get(layer);
            double localMax =
                    styleConfLayer
                    .texts()
                    .stylesStream()
                    .filter(TextConf::autoPreferredHeight)
                    .mapToDouble( textConf -> {
                        final Outline insets = predictedBoxModel.insetsFor(textConf.placementBoundary());
                            if ( textConf.content().isEmpty() ) {
                                double totalHeight = 0;
                                totalHeight += insets.top().orElse(0f);
                                totalHeight += insets.bottom().orElse(0f);
                                return totalHeight;
                            }
                            final Bounds textBounds = StyleRenderer._computeTextBounds(textConf, predictedBoxModel);
                            final float availableWidth = textBounds.size().width().orElse(0f);
                            if ( availableWidth <= 0 )
                                return -1;
                            final boolean wrapLines = textConf.wrapLines();
                            Font font = Optional.ofNullable(owner.getFont()).orElse(new Font(Font.DIALOG, Font.PLAIN, UI.scale(12)));
                            font = textConf.fontConf().createDerivedFrom(font, predictedBoxModel).orElse(font);
                            BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
                            final Graphics2D g2d = img.createGraphics();
                            try {
                                final FontRenderContext frc = g2d.getFontRenderContext();
                                final Pair<Float, List<@Nullable TextLayout>> layoutResult =
                                        StyleRenderer._buildTextLayoutsAndPreferredHeight(
                                                font, frc, textConf.content(), availableWidth, wrapLines, predictedBoxModel
                                        );
                                double totalHeight = layoutResult.first().doubleValue();
                                totalHeight += textBounds.location().y();
                                totalHeight += insets.bottom().orElse(0f);
                                return totalHeight;
                            }
                            finally {
                                g2d.dispose();
                            }
                        })
                    .max()
                    .orElse(-1);

            if ( localMax >= 0 && ( maxHeight == null || localMax > maxHeight ) )
                maxHeight = localMax;
        }
        if ( maxHeight == null )
            return OptionalDouble.empty();
        else
            return OptionalDouble.of(Math.round(maxHeight));
    }

    @Override
    public String toString() {
        if ( _any != null )
            return String.format(
                this.getClass().getSimpleName() + "[any=%s]",
                _any
            );
        return String.format(
            this.getClass().getSimpleName() + "[filter=%s, background=%s, content=%s, border=%s, foreground=%s]",
            _filter, _background, _content, _border, _foreground
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(_filter, _background, _content, _border, _foreground, _any);
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == this ) return true;
        if ( !(obj instanceof StyleConfLayers) ) return false;

        StyleConfLayers other = (StyleConfLayers) obj;
        return Objects.equals(_filter,     other._filter)
            && Objects.equals(_background, other._background)
            && Objects.equals(_content,    other._content)
            && Objects.equals(_border,     other._border)
            && Objects.equals(_foreground, other._foreground)
            && Objects.equals(_any,        other._any);
    }
}
