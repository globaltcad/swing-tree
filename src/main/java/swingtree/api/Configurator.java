package swingtree.api;

/**
 *  A configurator is a functional interface that takes a configuration object and
 *  returns a transformed configuration object.
 *  Typically, this configuration object is an immutable builder object.
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
    T apply(T config);
}
