package swingtree;

import swingtree.style.StyleSheet;

import java.util.Objects;
import java.util.Optional;

class Settings {

	private EventProcessor _eventProcessor = EventProcessor.COUPLED;
	private StyleSheet _styleSheet = null;

	public EventProcessor getEventProcessor() {
		return _eventProcessor;
	}

	public void setEventProcessor(EventProcessor eventProcessor) {
		_eventProcessor = Objects.requireNonNull(eventProcessor);
	}

	public Optional<StyleSheet> getStyleSheet() {
		return Optional.ofNullable(_styleSheet);
	}

	public void setStyleSheet(StyleSheet styleSheet) {
		_styleSheet = styleSheet;
	}

}
