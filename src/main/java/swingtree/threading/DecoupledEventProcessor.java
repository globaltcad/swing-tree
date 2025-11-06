package swingtree.threading;

import org.slf4j.Logger;
import swingtree.SwingTree;
import swingtree.UI;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This is a synchronized singleton wrapping a {@link BlockingQueue}.
 */
public final class DecoupledEventProcessor implements EventProcessor
{
	private static final Logger log = org.slf4j.LoggerFactory.getLogger(DecoupledEventProcessor.class);

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
			log.error(SwingTree.get().loggingMarker(), "Failed to register application event!", e);
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
			log.error(SwingTree.get().loggingMarker(), "Failed to register and run application event!", e);
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
			log.error(SwingTree.get().loggingMarker(), "Failed to register and run UI event!", e);
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
					log.error(SwingTree.get().loggingMarker(), "An exception occurred while processing an event!", e);
			}
		}
	}

	/**
	 *  A fully blocking call to the decoupled thread event processor
	 *  causing this thread to join its event queue
	 *  so that it can continuously process events produced by the UI.
	 *  <p>
	 *  This method wither be called by the main thread of the application
	 *  after the UI has been built and shown to the user, or alternatively
	 *  a new thread dedicated to processing events. (things like button clicks, etc.)
	 *  @throws IllegalStateException If this method is called from the UI thread.
	 */
	public void join() {
		if ( UI.thisIsUIThread() )
			throw new IllegalStateException("The UI thread cannot join the application event processing queue!");
		try {
			this.join(false);
		} catch (InterruptedException e) {
			log.error(SwingTree.get().loggingMarker(), "The application event processing queue was interrupted!", e);
		}
	}

	/**
	 *  A fully blocking call to the decoupled thread event processor
	 *  causing this thread to join its event queue
	 *  so that it can continuously process events produced by the UI.
	 *  <p>
	 *  This method should be called by the main thread of the application
	 *  after the UI has been built and shown to the user, or alternatively
	 *  a new thread dedicated to processing events. (things like button clicks, etc.)
	 *  <p>
	 *  This method will block until an exception is thrown by the event processor.
	 *  This is useful for debugging purposes.
	 *  @throws InterruptedException If the thread is interrupted while waiting for the event processor to join.
	 */
	public void joinUntilException() throws InterruptedException {
		this.join(true);
	}

	/**
	 *  A fully blocking call to the decoupled thread event processor
	 *  causing this thread to join its event queue
	 *  so that it can process the given number of events produced by the UI.
	 *  <p>
	 *  This method should be called by the main thread of the application
	 *  after the UI has been built and shown to the user, or alternatively
	 *  a new thread dedicated to processing events. (things like button clicks, etc.)
	 *  <p>
	 *  This method will block until the given number of events have been processed.
	 *  @param numberOfEvents The number of events to wait for.
	 */
	public void joinFor( long numberOfEvents ) {
		for ( long i = 0; i < numberOfEvents; i++ ) {
			try {
				this.rendererQueue.take().run();
			} catch (Exception e) {
				log.error(SwingTree.get().loggingMarker(), "An exception occurred while processing an event!", e);
			}
		}
	}

	/**
	 *  A temporarily blocking call to the decoupled thread event processor
	 *  causing this thread to join its event queue
	 *  so that it can process the given number of events produced by the UI.
	 *  <p>
	 *  This method should be called by the main thread of the application
	 *  after the UI has been built and shown to the user, or alternatively
	 *  a new thread dedicated to processing events. (things like button clicks, etc.)
	 *  <p>
	 *  This method will block until the given number of events have been processed
	 *  or an exception is thrown by the event processor.
	 *  @param numberOfEvents The number of events to wait for.
	 *  @throws InterruptedException If the thread is interrupted while waiting for the event processor to join.
	 */
	public void joinUntilExceptionFor( long numberOfEvents ) throws InterruptedException {
		for ( long i = 0; i < numberOfEvents; i++ )
			this.rendererQueue.take().run();
	}

	/**
	 *  A temporarily blocking call to the decoupled thread event processor
	 *  causing this thread to join its event queue
	 *  so that it can continuously process events produced by the UI
	 *  until all events have been processed or an exception is thrown by the event processor.
	 *  <p>
	 *  This method should be called by the main thread of the application
	 *  after the UI has been built and shown to the user, or alternatively
	 *  a new thread dedicated to processing events. (things like button clicks, etc.)
	 * @throws InterruptedException If the thread is interrupted while waiting.
	 */
	public void joinUntilDoneOrException() throws InterruptedException {
		while ( !this.rendererQueue.isEmpty() )
			this.rendererQueue.take().run();
	}

}
