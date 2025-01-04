package swingtree.api;

import swingtree.style.ComponentStyleDelegate;
import swingtree.style.StyleConf;

import javax.swing.JComponent;
import java.util.Objects;

/**
 * A {@link Styler} is a function that takes a {@link ComponentStyleDelegate} and then
 * transforms and returns it with some new style properties (see {@link swingtree.UIForAnySwing#withStyle(Styler)}). <br>
 * Note that all of this is done in a functional manner, so the original {@link ComponentStyleDelegate}
 * as well as the delegated {@link StyleConf} object is not modified
 * because {@link ComponentStyleDelegate} is an immutable object. <br>
 * This design makes the underlying style engine of SwingTree very flexible and scalable
 * because it allows for the composition of styles and reuse of style logic across many components
 * (see {@link swingtree.style.StyleSheet} for more advanced usage).
 *
 * @param <C> the type of the {@link JComponent} that the {@link ComponentStyleDelegate} is delegating to.
 */
@FunctionalInterface
public interface Styler<C extends JComponent>
{
    /**
     * A {@link Styler} that does nothing, meaning it simply returns the given {@link ComponentStyleDelegate}
     * without applying any style to it.
     *
     * @param <C> The type of the {@link JComponent} that the {@link ComponentStyleDelegate} is delegating to.
     * @return A {@link Styler} that does nothing.
     */
    static <C extends JComponent> Styler<C> none() {
        return (Styler<C>) Constants.STYLER_NONE;
    }

    /**
     * Applies some style to the given {@link ComponentStyleDelegate} and returns a new {@link ComponentStyleDelegate}
     * that has the style applied (if any). <br>
     *  Note that this method deliberately requires the handling of checked exceptions
     *  at its invocation sites because there may be any number of implementations
     *  hiding behind this interface and so it is unwise to assume that
     *  all of them will be able to execute gracefully without throwing exceptions.
     *
     * @param delegate The {@link ComponentStyleDelegate} to apply the style to.
     * @return A new {@link ComponentStyleDelegate} that has the style applied.
     * @throws Exception if the style could not be applied by the client code.
     */
    ComponentStyleDelegate<C> style( ComponentStyleDelegate<C> delegate ) throws Exception;

    /**
     * Returns a new {@link Styler} that applies the style of this {@link Styler} and then applies the style
     * of the given {@link Styler}. <br>
     * This method is conceptually equivalent to the
     * {@link java.util.function.Function#andThen(java.util.function.Function)}.
     *
     * @param other the {@link Styler} to apply after this {@link Styler}.
     * @return a new {@link Styler} that applies the style of this {@link Styler} and then applies the style
     * of the given {@link Styler}.
     */
    default Styler<C> andThen( Styler<C> other ) {
        Objects.requireNonNull(other);
        if ( this == none() )
            return other;
        if ( other == none() )
            return this;

        return delegate -> {
            ComponentStyleDelegate<C> result = delegate;
            try {
                result = style( delegate );
            } catch ( Exception e ) {
                // Exceptions inside a styler should not be fatal.
                Constants.LOG.error(
                    "Error trying to run '"+delegate+"' through styler '"+this+"'.",
                    e
                );
            }
            return other.style( result );
        };
    }
}
