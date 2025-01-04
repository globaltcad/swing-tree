package swingtree.api.mvvm;

import sprouts.Vals;
import swingtree.Tab;
import swingtree.UIForTabbedPane;

/**
 * Functional interface for dynamically creating tabs based on a provided model.
 * <p>
 * This interface is specifically designed for the {@link UIForTabbedPane#addAll(Vals, TabSupplier)} method
 * in the {@link  UIForTabbedPane} class.
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 *     UI.panel()
 *      .add(
 *          UI.tabbedPane().addAll(tabs, model ->
 *              switch(model.type()) {
 *                  case LOGIN -> UI.tab("Login").add(..);
 *                  case ABOUT -> UI.tab("About").add(..);
 *                  case SETTINGS -> UI.tab("Settings").add(..);
 *              }
 *          )
 *      )
 * }</pre>
 *  <p>
 *  See: <br>
 *  {@link swingtree.UIForTabbedPane#addAll(Vals, TabSupplier)},  <br>
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
