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
	 *  This method allows you to access the underlying window instance
	 *  of this delegate.
	 *  See {@link #getEvent()} for the event instance.
	 *
	 * @return The window which is wrapped by this delegate.
	 */
	W get();

	/**
	 * 	Allows you to access the delegated event instance.
	 * 	See {@link #get()} for the window instance.
	 *
	 * @return The event which is wrapped by this delegate.
	 */
	@Nullable E getEvent();
}
