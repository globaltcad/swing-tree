package swingtree.api;

import javax.swing.*;
import java.util.Optional;

/**
 *  A context object providing {@link JList} entry specific context information
 *  for the functional {@link ListEntryRenderer} interface, which is used to
 *  render list entries.
 *
 * @param <E> The type of the entries in the {@link JList}.
 * @param <L> The type of the {@link JList} being rendered.
 */
public interface ListEntryDelegate<E, L extends JList<E>> {

	/**
	 * @return The list being rendered.
	 */
	L list();

	/**
	 * @return An {@link Optional} containing the value of the entry being rendered, or an empty {@link Optional}
	 * 		   if the entry is not present (null or out of bounds).
	 */
	Optional<E> entry();

	/**
	 * @return The index of the entry being rendered or -1 if out of bounds.
	 */
	int index();

	/**
	 * @return true if the entry is selected, false otherwise.
	 */
	boolean isSelected();

	/**
	 * @return true if the entry is focused, false otherwise.
	 */
	boolean hasFocus();

}
