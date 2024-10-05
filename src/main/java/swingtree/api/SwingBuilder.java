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
public interface SwingBuilder<C extends JComponent>
{
    /**
     *  Build the component. <br>
     *  Note that this method deliberately requires the handling of checked exceptions
     *  at its invocation sites because there may be any number of implementations
     *  hiding behind this interface and so it is unwise to assume that
     *  all of them will be able to execute gracefully without throwing exceptions.
     *
     * @return The built {@link JComponent} type.
     */
    C build() throws Exception;
}
