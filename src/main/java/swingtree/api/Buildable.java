package swingtree.api;

/**
 *  A generic interface for builder objects which is used by the Swing-Tree API to call
 *  the build methods of the builder objects for you.
 *
 * @param <T> The type of the object to build.
 */
public interface Buildable<T>
{
    T build();
}
