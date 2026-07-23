package swingtree.api;

import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import java.util.Objects;

/**
 * An {@link ItemStyler} is conceptually a union of a property observer and the
 * {@link Styler} function, which is to say that it takes both the current item
 * of a bound {@link sprouts.Val} property and a {@link ComponentStyleDelegate}
 * to produce a new {@link ComponentStyleDelegate} with some style properties
 * applied to it (usually based on the supplied item). <br>
 * Note that both parameters are immutable value oriented objects, so the function
 * is pure and does not modify the original {@link ComponentStyleDelegate} or item. <br>
 * <p>
 * This interface exists specifically for property driven styling through
 * {@link swingtree.UIForAnySwing#withStyle(sprouts.Val, ItemStyler)}, whose whole
 * purpose is thread safety in the decoupled threading mode
 * (see {@link swingtree.threading.EventProcessor#DECOUPLED}): <br>
 * Style gathering is owned by the UI thread, which means a plain {@link Styler}
 * lambda which reads a property from its enclosing scope (through {@code myProperty.get()})
 * leaks the UI thread into application thread owned state. An {@link ItemStyler}
 * closes that leak by receiving the item <b>as an explicit argument</b>, captured
 * from the property change event on the property's owning thread and published
 * to the UI thread, where this function is then evaluated safely. <br>
 * So instead of:
 * <pre>{@code
 *   UI.label("...")
 *   .withRepaintOn(person)
 *   .withStyle( it -> it.backgroundColor(person.get().color()) ) // <- live read on the UI thread!
 * }</pre>
 * ...you write:
 * <pre>{@code
 *   UI.label("...")
 *   .withStyle( person, (p, it) -> it.backgroundColor(p.color()) ) // <- captured item, no property access
 * }</pre>
 * ...which also repaints automatically whenever the property changes, making a
 * separate {@link swingtree.UIForAnySwing#withRepaintOn(sprouts.Observable)} binding unnecessary.
 * <p>
 * When a single {@link ItemStyler} should follow <b>several</b> properties at once, merge them
 * into one record with the composite view builder
 * {@link sprouts.Viewable#of(Object, java.util.function.Function)} (requires Sprouts 2.7.0 or above)
 * and bind that one merged property, instead of chaining a
 * {@link swingtree.UIForAnySwing#withStyle(sprouts.Val, ItemStyler)} per property.
 *
 * @param <T> the type of the item of the bound property, supplied to this function as the first argument.
 * @param <C> the type of the {@link JComponent} that the {@link ComponentStyleDelegate} is delegating to.
 * @see swingtree.UIForAnySwing#withStyle(sprouts.Val, ItemStyler) The builder method consuming this function.
 * @see Styler The plain, non property driven counterpart of this function.
 * @see AnimatedItemStyler The animated counterpart which also receives an animation status per repaint.
 * @see AnimatedStyler The animation status driven styler used for transitional and transitory styles.
 */
@FunctionalInterface
public interface ItemStyler<T, C extends JComponent>
{
    /**
     * An {@link ItemStyler} that does nothing, meaning it simply returns the given
     * {@link ComponentStyleDelegate} without applying any style to it.
     * Conceptually speaking, this returns the null object of the {@link ItemStyler} type.
     *
     * @param <T> The type of the item of the bound property.
     * @param <C> The type of the {@link JComponent} that the {@link ComponentStyleDelegate} is delegating to.
     * @return An {@link ItemStyler} that does nothing.
     */
    static <T, C extends JComponent> ItemStyler<T, C> none() {
        return (ItemStyler<T, C>) Constants.ITEM_STYLER_NONE;
    }

    /**
     * Applies some style to the given {@link ComponentStyleDelegate}, usually based on
     * the supplied property item, and returns a new {@link ComponentStyleDelegate}
     * that has the style applied (if any). <br>
     * Note that this method deliberately requires the handling of checked exceptions
     * at its invocation sites because there may be any number of implementations
     * hiding behind this interface and so it is unwise to assume that
     * all of them will be able to execute gracefully without throwing exceptions.
     *
     * @param item The current item of the bound property, captured from the property change
     *             event on the property's owning thread. Style based on this argument only,
     *             never on the property itself!
     * @param delegate The {@link ComponentStyleDelegate} to apply the style to.
     * @return A new {@link ComponentStyleDelegate} that has the style applied.
     * @throws Exception if the style could not be applied by the client code.
     */
    ComponentStyleDelegate<C> style( T item, ComponentStyleDelegate<C> delegate ) throws Exception;

    /**
     * Returns a new {@link ItemStyler} that applies the style of this {@link ItemStyler}
     * and then applies the style of the given {@link ItemStyler}.
     *
     * @param other the {@link ItemStyler} to apply after this one.
     * @return a new {@link ItemStyler} that applies the style of this and then the given one.
     */
    default ItemStyler<T, C> andThen( ItemStyler<T, C> other ) {
        Objects.requireNonNull(other);
        if ( this == none() )
            return other;
        if ( other == none() )
            return this;

        return ( item, delegate ) -> {
            ComponentStyleDelegate<C> result = delegate;
            try {
                result = style( item, delegate );
            } catch ( Exception e ) {
                // Exceptions inside a styler should not be fatal.
                Constants.LOG.error(
                    "Error trying to run '"+delegate+"' through item styler '"+this+"'.",
                    e
                );
            }
            return other.style( item, result );
        };
    }
}
