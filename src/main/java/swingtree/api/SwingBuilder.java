package swingtree.api;

import swingtree.UI;

import javax.swing.*;

/**
 *  If you are using builders for your custom Swing components,
 *  implement this to allow the {@link UI} builder to call the {@link #build()}
 *  method for you!
 *
 * @param <C> The UI component type built by implementations of this.
 */
public interface SwingBuilder<C extends JComponent> {

    C build();

}
