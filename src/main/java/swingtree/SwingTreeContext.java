package swingtree;

import swingtree.style.StyleSheet;
import swingtree.threading.EventProcessor;

import java.util.Objects;
import java.util.Optional;

/**
 *  A {@link SwingTreeContext} is a singleton that holds global state for the SwingTree library.
 *  This includes the {@link EventProcessor} that is used to process events, as well as the
 *  {@link StyleSheet} that is used to style components.
 */
public final class SwingTreeContext
{
	private static SwingTreeContext _INSTANCES = null;

	/**
	 * Returns the singleton instance of the {@link SwingTreeContext}.
	 * Note that this method will create the singleton if it does not exist.
	 * @return the singleton instance of the {@link SwingTreeContext}.
	 */
	public static SwingTreeContext get() {
		// We make sure this method is thread-safe by using the double-checked locking idiom.
		if ( _INSTANCES == null ) {
			synchronized ( SwingTreeContext.class ) {
				if ( _INSTANCES == null ) {
					_INSTANCES = new SwingTreeContext();
				}
			}
		}
		return _INSTANCES;
	}


	private EventProcessor _eventProcessor = EventProcessor.COUPLED_STRICT;
	private StyleSheet _styleSheet = null;

	private SwingTreeContext() {}

	/**
	 * @return The currently configured {@link EventProcessor} that is used to process
	 *         GUI and application events.
	 */
	public EventProcessor getEventProcessor() {
		return _eventProcessor;
	}

	/**
	 * Sets the {@link EventProcessor} that is used to process GUI and application events.
	 * @param eventProcessor the {@link EventProcessor} that is used to process GUI and application events.
	 */
	public void setEventProcessor( EventProcessor eventProcessor ) {
		_eventProcessor = Objects.requireNonNull(eventProcessor);
	}

	/**
	 * @return The currently configured {@link StyleSheet} that is used to style components.
	 */
	public Optional<StyleSheet> getStyleSheet() {
		return Optional.ofNullable(_styleSheet);
	}

	/**
	 * Sets the {@link StyleSheet} that is used to style components.
	 * @param styleSheet the {@link StyleSheet} that is used to style components.
	 */
	public void setStyleSheet(StyleSheet styleSheet) {
		_styleSheet = styleSheet;
	}

}
