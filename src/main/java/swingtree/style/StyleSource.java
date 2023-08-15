package swingtree.style;

import swingtree.SwingTree;
import swingtree.UI;
import swingtree.animation.LifeTime;
import swingtree.api.Styler;

import javax.swing.*;
import java.util.*;

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
                        new Expirable[0],
                        SwingTree.get().getStyleSheet().orElse(null)
                    );
    }

    private final Styler<C> _localStyler;

    private final Expirable<Styler<C>>[] _animationStylers;

    private final StyleSheet _styleSheet;


    private StyleSource(
        Styler<C> localStyler,
        Expirable<Styler<C>>[] animationStylers,
        StyleSheet styleSheet
    ) {
        _localStyler = localStyler;
        _animationStylers = animationStylers;
        _styleSheet = styleSheet;
    }

    public boolean hasNoAnimationStylers() {
        return _animationStylers.length == 0;
    }

    StyleSource<C> withLocalStyler( Styler<C> styler, C owner ) {
        Styler<C> compositeStyler = _localStyler.andThen( s -> styler.style(new ComponentStyleDelegate<>(owner, s.style())) );
        return new StyleSource<>(compositeStyler, _animationStylers, _styleSheet);
    }

    StyleSource<C> withAnimationStyler(LifeTime lifeTime, Styler<C> animationStyler) {
        List<Expirable<Styler<C>>> animationStylers = new ArrayList<>(Arrays.asList(_animationStylers));
        animationStylers.add(new Expirable<>(lifeTime, animationStyler));
        return new StyleSource<>(_localStyler, animationStylers.toArray(new Expirable[0]), _styleSheet);
    }

    StyleSource<C> withoutAnimationStylers() {
        return new StyleSource<>(_localStyler, new Expirable[0], _styleSheet);
    }


    Style calculateStyleFor( C owner ) {
        Style style = _styleSheet == null ? Style.none() : _styleSheet.applyTo( owner );
        style = _localStyler.style(new ComponentStyleDelegate<>(owner, style)).style();

        // Animation styles are last: they override everything else:
        for ( Expirable<Styler<C>> expirableStyler : _animationStylers )
            if ( !expirableStyler.isExpired() )
                try {
                    style = expirableStyler.get().style(new ComponentStyleDelegate<>(owner, style)).style();
                } catch ( Exception e ) {
                    e.printStackTrace();
                    // An exception inside a styler should not prevent other stylers from being applied!
                }

        return _applyDPIScaling(style);
    }

    private static Style _applyDPIScaling( Style style ) {
        if ( UI.scale() == 1f )
            return style;

        return style.scale( UI.scale() );
    }

    StyleSource<C> withoutExpiredAnimationStylers() {
        List<Expirable<Styler<C>>> animationStylers = new ArrayList<>(Arrays.asList(_animationStylers));
        animationStylers.removeIf(Expirable::isExpired);
        return new StyleSource<>(_localStyler, animationStylers.toArray(new Expirable[0]), _styleSheet);
    }

}
