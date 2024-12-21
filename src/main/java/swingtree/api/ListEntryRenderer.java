package swingtree.api;

import javax.swing.*;
import java.awt.*;

/**
 *  A functional interface allowing you to define how a {@link JList} entry should
 *  be rendered in a declarative manner through method {@link swingtree.UIForList#withRenderComponent(ListEntryRenderer)}.
 *
 * @param <E> The type of the list entry.
 * @param <L> The type of the list.
 */
@FunctionalInterface
public interface ListEntryRenderer<E, L extends JList<E>> {

    Component render( ListEntryDelegate<E, L> delegate );

}
