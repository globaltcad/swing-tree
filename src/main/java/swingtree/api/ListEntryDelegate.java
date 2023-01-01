package swingtree.api;

import javax.swing.*;
import java.util.Optional;

public interface ListEntryDelegate<E, L extends JList<E>> {

	L list();
	Optional<E> entry();
	int index();
	boolean isSelected();
	boolean hasFocus();

}
