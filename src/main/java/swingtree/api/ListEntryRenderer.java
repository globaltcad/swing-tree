package swingtree.api;

import javax.swing.*;
import java.awt.*;

public interface ListEntryRenderer<E, L extends JList<E>> {

	Component render(ListEntryDelegate<E, L> delegate);

}
