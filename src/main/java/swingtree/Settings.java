package swingtree;

class Settings {

	private EventProcessor _eventProcessor = EventProcessor.COUPLED;

	public EventProcessor getEventProcessor() {
		return _eventProcessor;
	}

	public void setEventProcessor(EventProcessor eventProcessor) {
		_eventProcessor = eventProcessor;
	}

}
