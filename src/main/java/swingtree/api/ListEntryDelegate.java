package swingtree.api;

import javax.swing.*;

public interface ListEntryDelegate<E, L extends JList<E>> {

	L list();
	E entry();
	int index();
	boolean isSelected();
	boolean hasFocus();

}
