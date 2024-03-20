package swingtree.style;

import org.slf4j.Logger;
import swingtree.SwingTree;
import swingtree.UI;
import swingtree.animation.LifeSpan;
import swingtree.api.Styler;

import javax.swing.*;
import java.util.*;

/**
 *  A style source is a container for a local styler, animation stylers and a style sheet
 *  which are all used to calculate the final {@link StyleConf} configuration of a component. <br>
 *  This object can be thought of as a function of lambdas that takes a {@link JComponent}
 *  and returns a {@link StyleConf} object. <br>
 *
 * @param <C> The type of the component that is being styled, animated or sized in a particular way...
 */
final class StyleSource<C extends JComponent>
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(StyleSource.class);

    static <C extends JComponent> StyleSource<C> create() {
        return new StyleSource<C>(
                        Styler.none(),
                        new Expirable[0],
                        SwingTree.get().getStyleSheet()
                    );
    }

    private final Styler<C> _localStyler;

    private final Expirable<Styler<C>>[] _animationStylers;

    private final StyleSheet _styleSheet;


    private StyleSource(
        Styler<C>              localStyler,
        Expirable<Styler<C>>[] animationStylers,
        StyleSheet             styleSheet
    ) {
        _localStyler      = Objects.requireNonNull(localStyler);
        _animationStylers = Objects.requireNonNull(animationStylers);
        _styleSheet       = Objects.requireNonNull(styleSheet);
    }

    public boolean hasNoAnimationStylers() {
        return _animationStylers.length == 0;
    }

    StyleSource<C> withLocalStyler( Styler<C> styler ) {
        Styler<C> compositeStyler = _localStyler.andThen(styler);
        return new StyleSource<>(compositeStyler, _animationStylers, _styleSheet);
    }

    StyleSource<C> withAnimationStyler(LifeSpan lifeSpan, Styler<C> animationStyler ) {
        List<Expirable<Styler<C>>> animationStylers = new ArrayList<>(Arrays.asList(_animationStylers));
        animationStylers.add(new Expirable<>(lifeSpan, animationStyler));
        return new StyleSource<>(_localStyler, animationStylers.toArray(new Expirable[0]), _styleSheet);
    }

    StyleSource<C> withoutAnimationStylers() {
        return new StyleSource<>(_localStyler, new Expirable[0], _styleSheet);
    }

    StyleConf gatherStyleFor(C owner )
    {
        StyleConf styleConf = Optional.ofNullable(owner.getParent())
                              .map( p -> p instanceof JComponent ? (JComponent) p : null )
                              .map(ComponentExtension::from)
                              .map(ComponentExtension::getStyle)
                              .map(StyleConf::font)
                              .filter( f -> !f.equals(FontConf.none()) )
                              .map( f -> StyleConf.none()._withFont(f) )
                              .orElse(StyleConf.none());

        try {
            styleConf = _styleSheet.applyTo( owner, styleConf );
        } catch (Exception e) {
            log.error("An exception occurred while applying the style sheet for component '"+owner+"'.", e);
            /*
                 If any exceptions happen in a StyleSheet implementation provided by a user,
                 then we don't want to prevent the other Stylers from doing their job,
                 which is why we catch any exceptions immediately!
            */
        }

        try {
            styleConf = _localStyler.style(new ComponentStyleDelegate<>(owner, styleConf)).style();
        } catch (Exception e) {
            log.error("An exception occurred while applying the local styler for component '"+owner+"'.", e);
            /*
                 If any exceptions happen in a Styler implementation provided by a user,
                 then we don't want to prevent the other Stylers from doing their job,
                 which is why we catch any exceptions immediately!
            */
        }

        // Animation styles are last: they override everything else:
        for ( Expirable<Styler<C>> expirableStyler : _animationStylers )
            if ( !expirableStyler.isExpired() )
                try {
                    styleConf = expirableStyler.get().style(new ComponentStyleDelegate<>(owner, styleConf)).style();
                } catch ( Exception e ) {
                    log.warn("An exception occurred while applying an animation styler!", e);
                    /*
                         If any exceptions happen in a Styler implementation provided by a user,
                         then we don't want to prevent the other Stylers from doing their job,
                         which is why we catch any exceptions immediately!

				         We log as warning because exceptions during
				         styling are not a big deal!

                         Hi there! If you are reading this, you are probably a developer using the SwingTree
                         library, thank you for using it! Good luck finding out what went wrong! :)
                    */
                }

        styleConf = styleConf.simplified();

        styleConf = _applyDPIScaling(styleConf);

        styleConf = styleConf.correctedForRounding();

        return styleConf;
    }

    private static StyleConf _applyDPIScaling(StyleConf styleConf) {
        if ( UI.scale() == 1f )
            return styleConf;

        return styleConf.scale( UI.scale() );
    }

}
