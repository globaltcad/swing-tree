package swingtree.api;

/**
 *  A generic interface for builder objects which is used by the SwingTree API to call
 *  the build methods of the builder objects for you and then use the built object
 *  as part of a SwingTree UI.
 *
 * @param <T> The type of the object to build.
 */
public interface Buildable<T>
{
    /**
     *  Builds the object that this builder is responsible for
     *  and returns it. This method is intended to be called by the
     *  internals of the SwingTree API. <br>
     *  Note that this method deliberately requires the handling of checked exceptions
     *  at its invocation sites because there may be any number of implementations
     *  hiding behind this interface and so it is unwise to assume that
     *  all of them will be able to execute gracefully without throwing exceptions.
     *
     * @return The object that this builder is responsible for.
     */
    T build() throws Exception;
}
