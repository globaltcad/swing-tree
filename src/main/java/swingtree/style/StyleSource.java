package swingtree.style;

import swingtree.SwingTree;
import swingtree.UI;
import swingtree.animation.LifeTime;
import swingtree.api.Styler;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *  A style source is a container for a local styler, animation stylers and a style sheet
 *  which are all used to calculate the final style of a component.
 *
 * @param <C> The type of the component that is being styled, animated or sized in a particular way...
 */
final class StyleSource<C extends JComponent>
{
    static <C extends JComponent> StyleSource<C> create() {
        return new StyleSource<>(
                    Styler.none(),
                    Collections.emptyMap(),
                    SwingTree.get().getStyleSheet().orElse(null)
                );
    }

    private final Styler<C> _localStyler;

    private final Map<LifeTime, Styler<C>> _animationStylers;

    private final StyleSheet _styleSheet;


    private StyleSource(
        Styler<C> localStyler,
        Map<LifeTime, Styler<C>> animationStylers,
        StyleSheet styleSheet
    ) {
        _localStyler = localStyler;
        Map<LifeTime, Styler<C>> newAnimationStylers = new LinkedHashMap<>(1 + (animationStylers.size()-1)*4/3);
        newAnimationStylers.putAll(animationStylers);
        _animationStylers = newAnimationStylers;
        _styleSheet = styleSheet;
    }

    public boolean hasNoAnimationStylers() {
        return _animationStylers.isEmpty();
    }

    StyleSource<C> withLocalStyler( Styler<C> styler, C owner ) {
        Styler<C> compositeStyler = _localStyler.andThen( s -> styler.style(new ComponentStyleDelegate<>(owner, s.style())) );
        return new StyleSource<>(compositeStyler, _animationStylers, _styleSheet);
    }

    StyleSource<C> withAnimationStyler(LifeTime lifeTime, Styler<C> animationStyler) {
        Map<LifeTime, Styler<C>> newAnimationStylers = new LinkedHashMap<>(1 + (_animationStylers.size()-1)*4/3);
        newAnimationStylers.putAll(_animationStylers);
        newAnimationStylers.put(lifeTime, animationStyler);
        return new StyleSource<>(_localStyler, newAnimationStylers, _styleSheet);
    }

    StyleSource<C> withoutAnimationStylers() {
        return new StyleSource<>(_localStyler, Collections.emptyMap(), _styleSheet);
    }


    Style calculateStyle( C owner ) {
        Style style = _styleSheet == null ? Style.none() : _styleSheet.applyTo( owner );
        style = _localStyler.style(new ComponentStyleDelegate<>(owner, style)).style();

        // Animations styles are last: they override everything else:
        for ( Map.Entry<LifeTime, Styler<C>> entry : new ArrayList<>(_animationStylers.entrySet()) )
            if ( entry.getKey().isExpired() )
                _animationStylers.remove(entry.getKey());
            else {
                try {
                    style = entry.getValue().style(new ComponentStyleDelegate<>(owner, style)).style();
                } catch ( Exception e ) {
                    e.printStackTrace();
                    // An exception inside a styler should not prevent other stylers from being applied!
                }
            }

        return _applyDPIScaling(style);
    }

    private static Style _applyDPIScaling( Style style ) {
        if ( UI.scale() == 1f )
            return style;

        return style.scale( UI.scale() );
    }

}
