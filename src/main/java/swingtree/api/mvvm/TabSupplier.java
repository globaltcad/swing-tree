package swingtree.api.mvvm;

import swingtree.Tab;

/**
 * Functional interface for dynamically creating tabs based on a provided model.
 *
 * @param <M> the type of the model used to create the tab
 */
@FunctionalInterface
public interface TabSupplier<M> {

    /**
     * Creates a tab based on the given model.
     *
     * @param tabViewModel the model used to create the tab
     * @return the constructed tab
     * @throws Exception if an error occurs during tab creation
     */
    Tab createTabFor(M tabViewModel) throws Exception;

}
