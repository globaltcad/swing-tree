package swingtree.style;

import javax.swing.*;
import java.util.Objects;

/**
 * A {@link Styler} is a function that takes a {@link ComponentStyleDelegate}, applies some style to it, and then
 * returns a new {@link ComponentStyleDelegate} that has the style applied.
 * Note that all of this is done in a functional way, so the original {@link ComponentStyleDelegate}
 * as well as the delegated {@link Style} object is not modified.
 * @param <C> the type of the {@link JComponent} that the {@link ComponentStyleDelegate} is delegating to.
 */
@FunctionalInterface
public interface Styler<C extends JComponent>
{
    static <C extends JComponent> Styler<C> none() { return (Styler<C>) StyleUtility.STYLER_NONE; }

    /**
     * Applies some style to the given {@link ComponentStyleDelegate} and returns a new {@link ComponentStyleDelegate}
     * that has the style applied.
     * @param delegate the {@link ComponentStyleDelegate} to apply the style to.
     * @return a new {@link ComponentStyleDelegate} that has the style applied.
     */
    ComponentStyleDelegate<C> style(ComponentStyleDelegate<C> delegate );

    /**
     * Returns a new {@link Styler} that applies the style of this {@link Styler} and then applies the style
     * of the given {@link Styler}.
     * @param other the {@link Styler} to apply after this {@link Styler}.
     * @return a new {@link Styler} that applies the style of this {@link Styler} and then applies the style
     * of the given {@link Styler}.
     */
    default Styler<C> andThen( Styler<C> other ) {
        Objects.requireNonNull(other);
        return delegate -> {
            ComponentStyleDelegate<C> result = delegate;
            try {
                result = style( delegate );
            } catch ( Exception e ) {
                e.printStackTrace();
                // Exceptions inside a styler should not be fatal.
            }
            return other.style( result );
        };
    }
}
