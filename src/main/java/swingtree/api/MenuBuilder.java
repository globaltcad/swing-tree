package swingtree.api;

import swingtree.UI;

import javax.swing.*;

/**
 *  If you are using builders for your custom Swing components,
 *  implement this to allow the {@link UI} builder to call the {@link #build()}
 *  method for you!
 * 	<p>
 * 	<b>Consider taking a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of Swing-Tree in general.</b>
 * <p>
 *
 * @param <M> The UI component type build by implementations of this.
 */
public interface MenuBuilder<M extends JMenuItem> {

	M build();

}
