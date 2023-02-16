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


	@Override public void registerAppEvent(Runnable task ) {
		try {
			rendererQueue.put(task);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void registerAndRunAppEventNow(Runnable runnable ) {
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
	public void registerUIEvent(Runnable runnable) {
		UI.run(runnable);
	}

	@Override
	public void registerAndRunUIEventNow(Runnable runnable) {
		try {
			UI.runNow(runnable);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method is called by a thread to process all GUI events, this should be the application's main thread.
	 * @param rethrow If true, any exception thrown by the event handler will be rethrown.
	 * @throws InterruptedException If the thread is interrupted while waiting for the event to be processed.
     *                              Only thrown if {@code rethrow} is true.
	 */
	void join( boolean rethrow ) throws InterruptedException {
		while ( true ) {
			try {
				this.rendererQueue.take().run();
			} catch (Exception e) {
				if (rethrow)
					throw e;
				else
					e.printStackTrace();
			}
		}
	}

	void join() {
		try {
			this.join(false);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	void joinUntilException() throws InterruptedException {
		this.join(true);
	}

	void joinFor( long numberOfEvents ) {
		for ( long i = 0; i < numberOfEvents; i++ ) {
			try {
				this.rendererQueue.take().run();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	void joinUntilExceptionFor( long numberOfEvents ) throws InterruptedException {
		for ( long i = 0; i < numberOfEvents; i++ ) {
			this.rendererQueue.take().run();
		}
	}

}
