package swingtree;

public interface EventProcessor {

	EventProcessor COUPLED = new CoupledEventProcessor();
	EventProcessor DECOUPLED = EventQueue.INSTANCE();


	void processAppEvent(Runnable runnable);

	void processUIEvent(Runnable runnable);

}
