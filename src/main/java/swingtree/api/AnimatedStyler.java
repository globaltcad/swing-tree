package swingtree.api;

import swingtree.animation.AnimationStatus;
import swingtree.animation.LifeTime;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * An {@link AnimatedStyler} is conceptually a union of the {@link swingtree.animation.Animation}
 * and {@link Styler} functions, which is to say that it takes both an {@link AnimationStatus} and a
 * {@link ComponentStyleDelegate} to produce a new {@link ComponentStyleDelegate}
 * with some style properties applied to it (usually based on the {@link AnimationStatus}). <br>
 * Note that both paramters are immutable value oriented objects, so the function is pure and
 * does not modify the original {@link ComponentStyleDelegate} or {@link AnimationStatus} objects. <br>
 * This design makes the underlying style engine of SwingTree very flexible and scalable
 * because it allows for the composition of styles and reuse of style logic across many components
 * (see {@link swingtree.style.StyleSheet} for more advanced usage).
 * <p>
 * This interface is typically used in {@link swingtree.ComponentDelegate#animateStyleFor(LifeTime, AnimatedStyler)}
 * and {@link swingtree.ComponentDelegate#animateStyleFor(double, TimeUnit, AnimatedStyler)}.
 *
 * @param <C> the type of the {@link JComponent} that the {@link ComponentStyleDelegate} is delegating to.
 */
@FunctionalInterface
public interface AnimatedStyler<C extends JComponent>
{
    /**
     * A {@link AnimatedStyler} that does nothing, meaning it simply returns the given {@link ComponentStyleDelegate}
     * without applying any style to it. Conceptually speaking, this returns the null object
     * of the {@link AnimatedStyler} type.
     *
     * @param <C> The type of the {@link JComponent} that the {@link ComponentStyleDelegate} is delegating to.
     * @return A {@link AnimatedStyler} that does nothing.
     */
    static <C extends JComponent> AnimatedStyler<C> none() {
        return (AnimatedStyler<C>) Constants.ANIMATED_STYLER_NONE;
    }

    /**
     * Applies some style to the given {@link ComponentStyleDelegate} and returns a new {@link ComponentStyleDelegate}
     * that has the style applied (if any). <br>
     *  Note that this method deliberately requires the handling of checked exceptions
     *  at its invocation sites because there may be any number of implementations
     *  hiding behind this interface and so it is unwise to assume that
     *  all of them will be able to execute gracefully without throwing exceptions.
     *
     * @param status The {@link AnimationStatus} which is used to configure the style
     *              (usually based on the {@link AnimationStatus#progress()}).
     * @param delegate The {@link ComponentStyleDelegate} to apply the style to.
     * @return A new {@link ComponentStyleDelegate} that has the style applied.
     */
    ComponentStyleDelegate<C> style( AnimationStatus status, ComponentStyleDelegate<C> delegate ) throws Exception;

    /**
     * Returns a new {@link AnimatedStyler} that applies the style of this {@link AnimatedStyler} and then applies the style
     * of the given {@link AnimatedStyler}. <br>
     * This method is conceptually equivalent to the
     * {@link java.util.function.Function#andThen(java.util.function.Function)}.
     *
     * @param other the {@link AnimatedStyler} to apply after this {@link AnimatedStyler}.
     * @return a new {@link AnimatedStyler} that applies the style of this {@link AnimatedStyler} and then applies the style
     * of the given {@link AnimatedStyler}.
     */
    default AnimatedStyler<C> andThen( AnimatedStyler<C> other ) {
        Objects.requireNonNull(other, "Use AnimatedStyler.none() instead of null.");
        if ( this == none() )
            return other;
        if ( other == none() )
            return this;

        return (state, delegate) -> {
            ComponentStyleDelegate<C> result = delegate;
            try {
                result = style( state, delegate );
            } catch ( Exception e ) {
                Constants.LOG.error("Failed to evaluate composed style", e);
            }
            return other.style( state, result );
        };
    }
}
