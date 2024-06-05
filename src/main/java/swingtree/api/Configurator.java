package swingtree.api;

/**
 *  A configurator is a functional interface that takes a configuration object and
 *  returns a transformed configuration object.
 *  Typically, this configuration object is an immutable builder object.
 *  <p>
 *  This interface is primarily used to configure the style configuration objects
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
     * @param config the configuration object
     * @return the transformed configuration object
     */
    T configure( T config );
}
