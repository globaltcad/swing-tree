package swingtree.style;

import javax.swing.*;

/**
 * A {@link Styler} is a function that takes a {@link StyleDelegate}, applies some style to it, and then
 * returns a new {@link StyleDelegate} that has the style applied.
 * Note that all of this is done in a functional way, so the original {@link StyleDelegate}
 * as well as the delegated {@link Style} object is not modified.
 * @param <C> the type of the {@link JComponent} that the {@link StyleDelegate} is delegating to.
 */
@FunctionalInterface
public interface Styler<C extends JComponent>
{
    /**
     * Applies some style to the given {@link StyleDelegate} and returns a new {@link StyleDelegate}
     * that has the style applied.
     * @param delegate the {@link StyleDelegate} to apply the style to.
     * @return a new {@link StyleDelegate} that has the style applied.
     */
    StyleDelegate<C> style( StyleDelegate<C> delegate );

    /**
     * Returns a new {@link Styler} that applies the style of this {@link Styler} and then applies the style
     * of the given {@link Styler}.
     * @param other the {@link Styler} to apply after this {@link Styler}.
     * @return a new {@link Styler} that applies the style of this {@link Styler} and then applies the style
     * of the given {@link Styler}.
     */
    default Styler<C> andThen( Styler<C> other ) {
        return delegate -> other.style( style( delegate ) );
    }
}
