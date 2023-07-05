package swingtree;

import swingtree.style.StyleSheet;
import swingtree.threading.EventProcessor;

import java.util.Objects;
import java.util.Optional;

public class SwingTreeContext
{
	private static final ThreadLocal<SwingTreeContext> _INSTANCES = new ThreadLocal<>();

	public static SwingTreeContext get() {
		SwingTreeContext swingTreeContext = _INSTANCES.get();
		if ( swingTreeContext == null ) {
			swingTreeContext = new SwingTreeContext();
			_INSTANCES.set(swingTreeContext);
		}
		return swingTreeContext;
	}


	private EventProcessor _eventProcessor = EventProcessor.COUPLED_STRICT;
	private StyleSheet _styleSheet = null;

	public EventProcessor getEventProcessor() {
		return _eventProcessor;
	}

	public void setEventProcessor( EventProcessor eventProcessor ) {
		_eventProcessor = Objects.requireNonNull(eventProcessor);
	}

	public Optional<StyleSheet> getStyleSheet() {
		return Optional.ofNullable(_styleSheet);
	}

	public void setStyleSheet(StyleSheet styleSheet) {
		_styleSheet = styleSheet;
	}

}
