package swingtree.api.mvvm;

import sprouts.Var;
import swingtree.JScrollPanels;

import javax.swing.*;

/**
 * The {@link JScrollPanels} class is similar to the {@link JList} class
 * which uses a {@link ListCellRenderer} to continuously fetch a live UI component
 * for dynamically rendering list entries.
 * Contrary to the {@link JList} however, entries of the {@link JScrollPanels}
 * will be able to receive user events like for example button clicks etc... <br>
 * This lambda defines the provider for the dynamically generated and rendered
 * list entries.
 *
 * @param <P> The panel type provided ba this provider.
 */
public interface ViewableEntry extends Viewable {

	Var<Boolean> isSelected();

	Var<Integer> position();
}
