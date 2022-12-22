package swingtree;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This is a synchronized singleton wrapping a {@link BlockingQueue}.
 */
class EventQueue implements EventProcessor
{
	private static final EventQueue _INSTANCE = new EventQueue();

	static EventQueue INSTANCE() { return _INSTANCE; }

	/**
	 * This is a simple queue for pending event calls which is thread safe and allows the
	 * GUI thread to register application events.
	 */
	private final BlockingQueue<Runnable> rendererQueue = new LinkedBlockingQueue<>();

	@Override public void processAppEvent(Runnable task) {
		try {
			rendererQueue.put(task);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void processUIEvent(Runnable runnable) {
		UI.run(runnable);
	}

	public void process( int numberOfEventsToBeProcessed, boolean rethrow ) throws InterruptedException {
		int processed = 0;
		while ( processed < numberOfEventsToBeProcessed && !this.rendererQueue.isEmpty() ) {
			try {
				this.rendererQueue.take().run();
			}
			catch (Exception e) {
				if ( rethrow )
					throw e;
				else
					e.printStackTrace();
				processed--;
			}
			processed++;
		}
	}

	public void processAll( boolean rethrow ) throws InterruptedException {
		while ( !this.rendererQueue.isEmpty() )
			process(this.rendererQueue.size(), rethrow);
	}

}
