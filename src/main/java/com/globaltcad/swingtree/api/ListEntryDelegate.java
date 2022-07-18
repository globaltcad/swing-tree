package com.globaltcad.swingtree.api;

import javax.swing.*;
import java.util.List;

public interface ListEntryDelegate<E, L extends JList<E>> {

	L list();
	E entry();
	int index();
	boolean isSelected();
	boolean hasFocus();

}
