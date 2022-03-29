package stml;

/**
 *  If you are using builders for your custom Swing components,
 *  implement this to allow the {@link UI} builder to call the {@link #build()}
 *  method for you!
 *
 * @param <C> The UI component type build by implementations of this.
 */
public interface SwingBuilder<C> {

    C build();

}
