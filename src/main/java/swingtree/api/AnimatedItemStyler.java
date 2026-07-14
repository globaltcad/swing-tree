package swingtree.api;

import swingtree.animation.AnimationStatus;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import java.util.Objects;

/**
 * An {@link AnimatedItemStyler} is conceptually a union of the {@link ItemStyler}
 * and {@link AnimatedStyler} functions, which is to say that it takes the current item
 * of a bound {@link sprouts.Val} property, an {@link AnimationStatus} and a
 * {@link ComponentStyleDelegate} to produce a new {@link ComponentStyleDelegate}
 * with some style properties applied to it (usually based on both the item and the
 * animation progress). <br>
 * All parameters are immutable value oriented objects, so the function is pure and
 * does not modify any of them. <br>
 * <p>
 * This interface is used by {@link swingtree.UIForAnySwing#withStyle(sprouts.Val, swingtree.animation.LifeTime, AnimatedItemStyler)},
 * where every change of the bound property item restarts a transition animation whose
 * {@link AnimationStatus#progress()} runs from {@code 0} to {@code 1} over the given
 * {@link swingtree.animation.LifeTime}. This allows a style to not only follow the
 * application state, but to <i>animate towards</i> it. <br>
 * Like the {@link ItemStyler}, this function receives the property item as an explicit
 * argument, captured from the property change event on the property's owning thread,
 * which is what makes it safe to evaluate on the UI thread in the decoupled threading
 * mode (see {@link swingtree.threading.EventProcessor#DECOUPLED}). Never read the
 * property itself inside this function!
 *
 * @param <T> the type of the item of the bound property, supplied to this function as the first argument.
 * @param <C> the type of the {@link JComponent} that the {@link ComponentStyleDelegate} is delegating to.
 */
@FunctionalInterface
public interface AnimatedItemStyler<T, C extends JComponent>
{
    /**
     * An {@link AnimatedItemStyler} that does nothing, meaning it simply returns the given
     * {@link ComponentStyleDelegate} without applying any style to it.
     * Conceptually speaking, this returns the null object of the {@link AnimatedItemStyler} type.
     *
     * @param <T> The type of the item of the bound property.
     * @param <C> The type of the {@link JComponent} that the {@link ComponentStyleDelegate} is delegating to.
     * @return An {@link AnimatedItemStyler} that does nothing.
     */
    static <T, C extends JComponent> AnimatedItemStyler<T, C> none() {
        return (AnimatedItemStyler<T, C>) Constants.ANIMATED_ITEM_STYLER_NONE;
    }

    /**
     * Applies some style to the given {@link ComponentStyleDelegate}, usually based on the
     * supplied property item and the progress of the supplied {@link AnimationStatus},
     * and returns a new {@link ComponentStyleDelegate} that has the style applied (if any). <br>
     * Note that this method deliberately requires the handling of checked exceptions
     * at its invocation sites because there may be any number of implementations
     * hiding behind this interface and so it is unwise to assume that
     * all of them will be able to execute gracefully without throwing exceptions.
     *
     * @param item The current item of the bound property, captured from the property change
     *             event on the property's owning thread. Style based on this argument only,
     *             never on the property itself!
     * @param status The {@link AnimationStatus} of the transition towards the supplied item,
     *               whose {@link AnimationStatus#progress()} runs from {@code 0} to {@code 1}.
     * @param delegate The {@link ComponentStyleDelegate} to apply the style to.
     * @return A new {@link ComponentStyleDelegate} that has the style applied.
     * @throws Exception if the style could not be applied by the client code.
     */
    ComponentStyleDelegate<C> style( T item, AnimationStatus status, ComponentStyleDelegate<C> delegate ) throws Exception;

    /**
     * Returns a new {@link AnimatedItemStyler} that applies the style of this {@link AnimatedItemStyler}
     * and then applies the style of the given {@link AnimatedItemStyler}.
     *
     * @param other the {@link AnimatedItemStyler} to apply after this one.
     * @return a new {@link AnimatedItemStyler} that applies the style of this and then the given one.
     */
    default AnimatedItemStyler<T, C> andThen( AnimatedItemStyler<T, C> other ) {
        Objects.requireNonNull(other);
        if ( this == none() )
            return other;
        if ( other == none() )
            return this;

        return ( item, status, delegate ) -> {
            ComponentStyleDelegate<C> result = delegate;
            try {
                result = style( item, status, delegate );
            } catch ( Exception e ) {
                // Exceptions inside a styler should not be fatal.
                Constants.LOG.error(
                    "Error trying to run '"+delegate+"' through animated item styler '"+this+"'.",
                    e
                );
            }
            return other.style( item, status, result );
        };
    }
}
