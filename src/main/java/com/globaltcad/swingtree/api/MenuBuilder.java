package com.globaltcad.swingtree.api;

import com.globaltcad.swingtree.UI;

import javax.swing.*;

/**
 *  If you are using builders for your custom Swing components,
 *  implement this to allow the {@link UI} builder to call the {@link #build()}
 *  method for you!
 *
 * @param <M> The UI component type build by implementations of this.
 */
public interface MenuBuilder<M extends JMenuItem> {

	M build();

}
