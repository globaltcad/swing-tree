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
     *  Returns a configurator that does nothing, i.e. it returns the
     *  "null" object or "no-op" object for this interface.
     *  It is recommended to use the returned instance
     *  instead of null to avoid null pointer exceptions.
     *
     * @param <T> The type of the configuration object.
     *
     * @return A configurator that does nothing.
     */
    static <T> Configurator<T> none() {
        return (Configurator<T>) Constants.CONFIGURATOR_NONE;
    }

    /**
     *  Configures the given configuration object and returns the transformed configuration object. <br>
     *  Note that this method deliberately requires the handling of checked exceptions
     *  at its invocation sites because there may be any number of implementations
     *  hiding behind this interface and so it is unwise to assume that
     *  all of them will be able to execute gracefully without throwing exceptions.
     *
     * @param config The configuration object, typically an immutable builder type
     *               which uses method chaining to for defining its properties.
     *
     * @return The fully transformed/updated configuration object.
     * @throws Exception If the configuration encounters errors in the execution of its implementations.
     */
    T configure( T config ) throws Exception;

    /**
     *  Returns a new configurator that first configures the given configuration object
     *  and then configures the result of this configuration through the provided configurator.
     *
     * @param after The configurator that should be applied after this configurator.
     *
     * @return A new configurator that first configures the given configuration object
     *         and then configures the result of this configuration.
     */
    default Configurator<T> andThen( Configurator<T> after ) {
        return config -> after.configure( configure( config ) );
    }
}
