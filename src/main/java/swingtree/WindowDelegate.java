package swingtree;

import org.jspecify.annotations.Nullable;

import java.awt.*;

/**
 *  A delegate for any kind of Swing window, usually a {@link javax.swing.JFrame} or a {@link javax.swing.JDialog},
 *  which is passed to the event handlers in the SwingTree API.
 *
 * @param <W> The type of the window.
 * @param <E> The type of the event.
 */
public interface WindowDelegate<W extends Window, E>
{
	/**
	 * @return The window which is wrapped by this delegate.
	 */
	W get();

	/**
	 * @return The event which is wrapped by this delegate.
	 */
	@Nullable E getEvent();
}
