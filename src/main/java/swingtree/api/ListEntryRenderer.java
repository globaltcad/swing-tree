package swingtree.api;

import javax.swing.*;
import java.awt.*;

/**
 *  A functional interface allowing you to define how a {@link JList} entry should
 *  be rendered in a declarative manner through method {@link swingtree.UIForList#withRenderer(ListEntryRenderer)}.
 *
 * @param <E>
 * @param <L>
 */
@FunctionalInterface
public interface ListEntryRenderer<E, L extends JList<E>> {

	Component render( ListEntryDelegate<E, L> delegate );

}
