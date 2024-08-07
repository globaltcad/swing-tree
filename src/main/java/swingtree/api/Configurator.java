package swingtree.api;

/**
 *  A configurator is a functional interface that takes a configuration object and
 *  returns a transformed configuration object.
 *  Typically, this configuration object is an immutable builder type.
 *  It is commonly used to configure table models or cell renderer,
 *  check out the following methods for these additional usage sites:
 *  <ul>
 *      <li>{@link swingtree.UIForTable#withModel(Configurator)}</li>
 *      <li>{@link swingtree.UIForTable#withModel(Class, Configurator)} </li>
 *      <li>{@link swingtree.UIForTable#withCells(Configurator)} </li>
 *      <li>{@link swingtree.UIForList#withCells(Configurator)} </li>
 *      <li>{@link swingtree.UIForCombo#withCells(Configurator)} </li>
 *  </ul>
 *  <p>
 *  Configurators are also heavily used for defining the <i>style</i>
 *  of components through the {@link swingtree.UIForAnySwing#withStyle(Styler)}
 *  method or when writing a custom {@link swingtree.style.StyleSheet}.
 *
 * @param <T> the type of the configuration object
 */
@FunctionalInterface
public interface Configurator<T>
{
    /**
     *  Configures the given configuration object and returns the transformed configuration object.
     *
     * @param config The configuration object, typically an immutable builder type
     *               which uses method chaining to for defining its properties.
     *
     * @return The fully transformed/updated configuration object.
     */
    T configure( T config );
}
