package swingtree;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This is a synchronized singleton wrapping a {@link BlockingQueue}.
 */
class DecoupledEventProcessor implements EventProcessor
{
	private static final DecoupledEventProcessor _INSTANCE = new DecoupledEventProcessor();

	static DecoupledEventProcessor INSTANCE() { return _INSTANCE; }

	/**
	 * This is a simple queue for pending event calls which is thread safe and allows the
	 * GUI thread to register application events.
	 */
	private final BlockingQueue<Runnable> rendererQueue = new LinkedBlockingQueue<>();


	@Override public void processAppEvent( Runnable task ) {
		try {
			rendererQueue.put(task);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void processAppEventNow( Runnable runnable ) {
		// We add the task to the queue and then wait for it to be processed.
		// This is a blocking call.
		boolean[] done = new boolean[1];
		try {
			rendererQueue.put(() -> {
				runnable.run();
				synchronized (done) {
					done[0] = true;
					done.notifyAll();
					// notify the waiting thread, meaning the GUI/frontend thread,
					// which is waiting for the task to be processed.
				}
			});
			synchronized (done) {
				while ( !done[0] ) done.wait();
				// wait for the task to be processed. The wait is released by the notifyAll() call in the task.
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void processUIEvent(Runnable runnable) {
		UI.run(runnable);
	}

	@Override
	public void processUIEventNow(Runnable runnable) {
		try {
			UI.runNow(runnable);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void processAll( boolean rethrow ) throws InterruptedException {
		while ( !this.rendererQueue.isEmpty() )
			_process(this.rendererQueue.size(), rethrow);
	}

	private void _process( int numberOfEventsToBeProcessed, boolean rethrow ) throws InterruptedException {
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

}
